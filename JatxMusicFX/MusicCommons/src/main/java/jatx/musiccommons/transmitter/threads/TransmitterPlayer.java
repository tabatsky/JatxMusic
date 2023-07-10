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
package jatx.musiccommons.transmitter.threads;

import jatx.musiccommons.transmitter.*;
import jatx.musiccommons.frame.Frame.WrongFrameException;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransmitterPlayer extends Thread {
	volatile WeakReference<UI> uiRef;
	
	volatile int mCount;
	
	volatile List<File> mFileList;
	
	volatile int mPosition;
	volatile boolean isPlaying;

	volatile boolean microphoneOk;
	volatile boolean loopbackOk;

	volatile String mPath;
	
	volatile long startTime;
	volatile long currentTime;
	volatile float deltaTimeExtraSentToReceiver;

	public TransmitterPlayer(List<File> fileList, UI ui) {
		uiRef = new WeakReference<>(ui);
		setFileList(fileList);
		
		isPlaying = false;
		
		mPath = "";
	}

	public void setFileList(List<File> fileList) {
		mFileList = new ArrayList<>(fileList);
		mCount = mFileList.size();
	}
	
	public void play() {
		System.out.println("(player) play");
		isPlaying = true;
		if (TrackInfo.isMicPath(mPath)) {
            try {
            	TrackInfo trackInfo = TrackInfo.getByPath(mPath);
            	int micIndex = Integer.parseInt(trackInfo.title);
                Microphone.start(micIndex);
                microphoneOk = true;
            } catch (LineUnavailableException e) {
                e.printStackTrace();
                microphoneOk = false;
            }
		} else if (TrackInfo.isLoopbackPath(mPath)) {
			try {
				TrackInfo trackInfo = TrackInfo.getByPath(mPath);
				int loopbackIndex = Integer.parseInt(trackInfo.title);
				Loopback.start(loopbackIndex);
				loopbackOk = true;
			} catch (Loopback.LoopbackStartException e) {
				e.printStackTrace();
				loopbackOk = false;
			}
		}
	}
	
	public void pause() {
		System.out.println("(player) pause");
		isPlaying = false;
		if (TrackInfo.isMicPath(mPath)) {
			Microphone.stop();
		} else if (TrackInfo.isLoopbackPath(mPath)) {
			Loopback.stop();
		}
	}

	public void seek(double progress) {
	    boolean needToPlay = isPlaying;
	    pause();
	    try {
			MusicDecoder musicDecoder = MusicDecoder.getInstance();
			if (musicDecoder != null) {
				musicDecoder.seek(progress);
			}
        } catch (MusicDecoder.MusicDecoderException e) {
	        e.printStackTrace();
        }
        if (needToPlay) play();
    }
	
	private void forcePause() {
		System.out.println("(player) force pause");
		isPlaying = false;
		
		Globals.tc.pause();
		
		final UI ui = uiRef.get();
		if (ui!=null) {
			ui.forcePause();
		}
	}

	public void setPosition(final int position) {		
		pause();
		
		if (position<0||mCount<=0) return;
		
		mPosition = position;
		if (mPosition>=mCount) mPosition = 0;

		System.out.println("(player) position: " + Integer.valueOf(mPosition).toString());
		
		mPath = mFileList.get(mPosition).getAbsolutePath();
		System.out.println("(player) path: " + mPath);
	
		final UI ui = uiRef.get();
		if (ui!=null) {
			final String info = TrackInfo.getByPath(mPath).toString();
			System.out.println("(player) info: " + info);
			
			ui.setPosition(mPosition);
		}

        try {
			MusicDecoder.setPath(mPath);
			MusicDecoder.setPosition(mPosition);
        } catch (MusicDecoder.MusicDecoderException e) {
            System.err.println("(player) " + e.getMessage());
        }

        play();
	}
	
	public void nextTrack() {
		try {
			final UI ui = uiRef.get();
			if (ui!=null) {
				ui.nextTrack();
			}
			Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
			forcePause();
		}
	}
	
	@Override
	public void run() {
		try {
			translateMusic();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.err.println("(player) thread interrupted");
		}  finally {
			System.out.println("(player) thread finished");
		}
	}
	
	private void translateMusic()
			throws InterruptedException, IOException {
		
		startTime = (new Date()).getTime();
		currentTime = startTime;
		deltaTimeExtraSentToReceiver = 0f;
		
		while (true) {
			MusicDecoder musicDecoder = MusicDecoder.getInstance();
			if (musicDecoder != null) {
				if (isPlaying) {
					if (MusicDecoder.resetTimeFlag) {
						resetTimeWithSynchronizing();
					}

					byte[] data;

					try {
						data = tryReadFrame();
					} catch (MusicDecoder.MusicDecoderException | Microphone.MicrophoneReadException e) {
						data = null;
						e.printStackTrace();
						Thread.sleep(200);
					} catch (Loopback.LoopbackReadException e) {
						data = null;
						System.out.println("(player) loopback read exception");
						Thread.sleep(200);
					} catch (MusicDecoder.TrackFinishException e) {
						data = null;
						System.out.println("(player) track finish");
						nextTrack();
					} catch (WrongFrameException e) {
						data = null;
						System.err.println("(player) wrong frame");
						System.err.println("(player) " + e.getMessage());
						nextTrack();
					}

					if (data != null) {
						Globals.tpck.writeData(data);
					}
				} else {
					Thread.sleep(10);
					resetTimeIfNotPlaying();
				}

				syncReadingFromFileWithSendingToReceiver();
			}
		}
	}

	private void resetTimeWithSynchronizing() throws InterruptedException {
		MusicDecoder musicDecoder = MusicDecoder.getInstance();
		if (musicDecoder != null) {
			do {
				currentTime = (new Date()).getTime();
				deltaTimeExtraSentToReceiver = musicDecoder.msSentToReceiver - (currentTime - startTime);
				Thread.sleep(10);
			} while (deltaTimeExtraSentToReceiver > 0);

			musicDecoder.msReadFromFile = 0f;
			musicDecoder.msSentToReceiver = 0f;
			startTime = (new Date()).getTime();
			currentTime = startTime;

			MusicDecoder.resetTimeFlag = false;
		}
	}

	private void resetTimeIfNotPlaying() {
		MusicDecoder musicDecoder = MusicDecoder.getInstance();
		if (musicDecoder != null) {
			musicDecoder.msReadFromFile = 0f;
			musicDecoder.msSentToReceiver = 0f;
			startTime = (new Date()).getTime();
			currentTime = startTime;
			deltaTimeExtraSentToReceiver = 0f;
		}
	}

	private void syncReadingFromFileWithSendingToReceiver() throws InterruptedException {
		MusicDecoder musicDecoder = MusicDecoder.getInstance();
		if (musicDecoder != null) {
			if (musicDecoder.msReadFromFile > 300) {
				do {
					currentTime = (new Date()).getTime();
					deltaTimeExtraSentToReceiver = musicDecoder.msSentToReceiver - (currentTime - startTime);

					Thread.sleep(10);
				} while (deltaTimeExtraSentToReceiver > 200);

				musicDecoder.msReadFromFile = 0f;
			}
		}
	}

	private byte[] tryReadFrame() throws Microphone.MicrophoneReadException, Loopback.LoopbackReadException, MusicDecoder.MusicDecoderException, WrongFrameException, MusicDecoder.TrackFinishException {
		if (TrackInfo.isMicPath(mPath)) {
			return Microphone.readFrame(mPosition).toByteArray();
		} else if (TrackInfo.isLoopbackPath(mPath)) {
			return Loopback.readFrame(mPosition).toByteArray();
		} else {
			MusicDecoder musicDecoder = MusicDecoder.getInstance();
			if (musicDecoder != null) {
				return musicDecoder.readFrame().toByteArray();
			}
		}
		return null;
	}
}
