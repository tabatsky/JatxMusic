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

import jatx.musiccommons.frame.Frame;
import jatx.musiccommons.frame.Frame.WrongFrameException;

import java.io.File;

public abstract class MusicDecoder {
	public static volatile boolean resetTimeFlag = false;

	protected static volatile int sPosition = 0;

	private static volatile MusicDecoder _instance = null;

	protected static volatile boolean isMicActive = false;

	public float msReadFromFile = 0f;
	public float msSentToReceiver = 0f;
	
	public float currentMs = 0f;
	public int trackLengthSec = 0;

	public static void setPosition(int position) {
		sPosition = position;
	}

	public static MusicDecoder getInstance() {
		return _instance;
	}
	
	public static void setPath(String path) throws MusicDecoderException {
		if (!TrackInfo.isMicPath(path)) {
			File file = new File(path);
			if (file.getName().endsWith("mp3")) {
				_instance = new JLayerMp3Decoder();
			} else if (file.getName().endsWith("flac")) {
				_instance = new FlacMusicDecoder();
			}
			System.out.println("set path: " + file.getName());
			_instance.setFile(file);
			isMicActive = false;
		} else {
			isMicActive = true;
		}
	}
	
	public abstract void setFile(File f) throws MusicDecoderException;

	public abstract Frame readFrame() throws MusicDecoderException, WrongFrameException, TrackFinishException;

	public abstract void seek(double progress) throws MusicDecoderException;

	public static class MusicDecoderException extends Exception {
		private static final long serialVersionUID = -5257283039222447187L;

		MusicDecoderException(Exception cause) {
			super(cause);
		}
		
		MusicDecoderException(String msg) {
			super(msg);
		}
	}
	
	public static class TrackFinishException extends Exception {
		private static final long serialVersionUID = 6563356718258555416L;
	}
}
