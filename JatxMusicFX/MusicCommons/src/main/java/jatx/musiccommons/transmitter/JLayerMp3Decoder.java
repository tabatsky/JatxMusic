/*******************************************************************************
 * Copyright (c) 2015 Evgeny Tabatsky.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Evgeny Tabatsky - initial API and implementation
 ******************************************************************************/
package jatx.musiccommons.transmitter;

import jatx.musiccommons.util.Frame;
import jatx.musiccommons.util.Frame.WrongFrameException;

import java.io.*;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

public class JLayerMp3Decoder extends MusicDecoder {
	private Decoder mDecoder = null;
	private Bitstream mBitStream = null;
	private File mCurrentFile = null;
	
	private float msFrame = 0f;

	@Override
	public synchronized void setFile(File f) throws MusicDecoderException {
		if (f == null || !f.exists()) {
			throw new MusicDecoderException("File Read Error");
		}

		mCurrentFile = f;

		try {
			AudioFile af;
			af = AudioFileIO.read(f);

			trackLengthSec = af.getAudioHeader().getTrackLength();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		mDecoder = new Decoder();
		
		if (mBitStream!=null) {
			try {
				mBitStream.close();
			} catch (BitstreamException e) {
				throw new MusicDecoderException(e);
			}
			mBitStream = null;
		}
	
		try {
			InputStream inputStream = new BufferedInputStream(new FileInputStream(f), 32 * 1024);
			mBitStream = new Bitstream(inputStream);
		} catch (FileNotFoundException e) {
			throw new MusicDecoderException(e);
		}
		
		currentMs = 0;
		
		resetTimeFlag = true;

		System.out.println("set file: " + f.getName());
	}
	
	@Override
	public synchronized Frame readFrame() throws MusicDecoderException, WrongFrameException, TrackFinishException {
		if (isMicActive) return null;

		Frame f;
		
		try {			
			Header frameHeader;
			if (mBitStream!=null) {
				frameHeader = mBitStream.readFrame();
			} else {
				throw new MusicDecoderException("bitstream: null");
			}
				
			if (frameHeader==null) {
				throw new TrackFinishException();
			}
			
			msFrame = frameHeader.ms_per_frame();
			msRead += msFrame;
			msTotal += msFrame;
			currentMs += msFrame;

			SampleBuffer output;
			try {
				output = (SampleBuffer) mDecoder.decodeFrame(frameHeader, mBitStream);
			} catch (ArrayIndexOutOfBoundsException e) {
				//Log.e("readFrame", "ArrayIndexOutOfBounds");
				mBitStream.closeFrame();
				throw new TrackFinishException();
			}
			
			f = fromSampleBuffer(output, sPosition);
				
			mBitStream.closeFrame();
		} catch (WrongFrameException e) {
			throw e;
		} catch (BitstreamException e) {
			throw new MusicDecoderException(e);
		} catch (DecoderException e) {
			throw new MusicDecoderException(e);
		} finally {}
		
		return f;
	}

	private static Frame fromSampleBuffer(SampleBuffer sBuff, int position)
			throws WrongFrameException {

		Frame f = new Frame();
		f.position = position;

		ByteArrayOutputStream outStream = new ByteArrayOutputStream(10240);

		f.freq = sBuff.getSampleFrequency();
		f.channels = sBuff.getChannelCount();
		f.depth = 16;

		short[] pcm = sBuff.getBuffer();

		boolean wrongRate = true;

		for (int rate: Frame.FRAME_RATES) {
			if (rate==f.freq) wrongRate = false;
		}

		if (wrongRate) {
			throw new Frame.WrongFrameException("(player) wrong frame rate: " + Integer.toString(f.freq));
		}

		if (f.channels==2) {
			for (int i=0; i<pcm.length/2; i++) {
				short shrt1 = pcm[2*i];
				short shrt2 = pcm[2*i+1];

				outStream.write(shrt1 & 0xff);
				outStream.write((shrt1 >> 8 ) & 0xff);
				outStream.write(shrt2 & 0xff);
				outStream.write((shrt2 >> 8 ) & 0xff);
			}
		} else if (f.channels==1) {
			throw new WrongFrameException("(player) mono sound");
		} else {
			throw new WrongFrameException("(player) " + Integer.valueOf(f.channels).toString() + " channels");
		}

		f.data = outStream.toByteArray();
		f.size = f.data.length;

		return f;
	}

	@Override
	public void seek(double progress) throws MusicDecoderException {
		setFile(mCurrentFile);

		try {
			while (currentMs<trackLengthSec*1000.0*progress) {
				Header frameHeader = null;
				if (mBitStream != null) {
					frameHeader = mBitStream.readFrame();
				} else {
					throw new MusicDecoderException("bitstream: null");
				}

				msFrame = frameHeader.ms_per_frame();
				msRead += msFrame;
				//msTotal += msFrame;
				currentMs += msFrame;

				mBitStream.closeFrame();
			}
		} catch (BitstreamException e) {
			throw new MusicDecoderException(e);
		}

		resetTimeFlag = true;
	}
}

