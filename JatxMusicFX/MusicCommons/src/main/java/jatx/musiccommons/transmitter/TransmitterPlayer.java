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

import jatx.musiccommons.util.Frame.WrongFrameException;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransmitterPlayer extends Thread {
	public static final String LOG_TAG_PLAYER = "transmitter player";
	
	public static final int CONNECT_PORT_PLAYER = 7171;
	
	volatile WeakReference<UI> ref;
	
	volatile int mCount;
	
	volatile List<File> mFileList;
	
	volatile int mPosition;
	volatile boolean isPlaying;
	
	volatile boolean mForceDisconnectFlag;

	volatile boolean microphoneOk;
	volatile boolean loopbackOk;

	volatile String mPath;
	
	volatile long t1;
	volatile long t2;
	volatile float dt;

	public TransmitterPlayer(List<File> fileList, UI ui, MusicDecoder decoder) {
		ref = new WeakReference(ui);
		setFileList(fileList);
		
		isPlaying = false;
		mForceDisconnectFlag = false;
		
		mPath = "";
	}

	public void setFileList(List<File> fileList) {
		mFileList = new ArrayList<File>(fileList);
		mCount = mFileList.size();
	}
	
	public void forceDisconnect() {
		mForceDisconnectFlag = true;
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
		
		final UI ui = ref.get();
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
	
		final UI ui = ref.get();
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
			final UI ui = ref.get();
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
		ServerSocket ss = null;
		OutputStream os = null;
		
		try {
			while(true) {
				Thread.sleep(100);
				
				ss = new ServerSocket(CONNECT_PORT_PLAYER);
				System.out.println("(player) new server socket");
				
				try {
					ss.setSoTimeout(Globals.SO_TIMEOUT);
					Socket s = ss.accept();
					os = s.getOutputStream();
					
					System.out.println("(player) socket connect");
					{
						final UI ui = ref.get();
						if (ui!=null) {
							ui.setWifiStatus(true);
						}
					}
					
					translateMusic(os);
				} catch (SocketTimeoutException e) {
					System.err.println("(player) socket timeout");
					
					mForceDisconnectFlag = false;
					MusicDecoder.disconnectResetTimeFlag = true;
				} catch (DisconnectException e){
					System.err.println("(player) socket force disconnect");
					System.err.println("(player) " + (new Date()).getTime()%10000);
					
					mForceDisconnectFlag = false;
					MusicDecoder.disconnectResetTimeFlag = true;
				} catch (IOException e) {
					System.err.println("(player) socket disconnect");
					System.err.println("(player) " + (new Date()).getTime()%10000);
					
					Globals.tc.forceDisconnect();
					
					Thread.sleep(250);
					
					mForceDisconnectFlag = false;
					MusicDecoder.disconnectResetTimeFlag = true;
				} finally {
					try {
						os.close();
						System.out.println("(player) outstream closed");
					} catch (Exception ex) {
						System.err.println("(player) cannot close outstream");
					}
					try {
						ss.close();
						System.out.println("(player) server socket closed");
					} catch (Exception ex) {
						System.err.println("(player) cannot close server socket");
					}
					
					final UI ui = ref.get();
					if (ui!=null) {
						ui.setWifiStatus(false);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			//Log.e(LOG_TAG_PLAYER, exceptionToString(e));
			System.err.println("(player) thread interrupted");
			try {
				os.close();
				System.out.println("(player) outstream closed");
			} catch (Exception ex) {
				System.err.println("(player) cannot close outstream");
			}
			try {
				ss.close();
				System.out.println("(player) server socket closed");
			} catch (Exception ex) {
				System.err.println("(player) cannot close server socket");
			}
		}  finally {
			System.out.println("(player) thread finished");
		}
	}
	
	private void translateMusic(OutputStream os) 
			throws InterruptedException, IOException, DisconnectException {
		byte[] data;
		
		t1 = (new Date()).getTime();
		t2 = t1;
		dt = 0f;
		
		while (true) {
			MusicDecoder musicDecoder = MusicDecoder.getInstance();
			if (musicDecoder != null) {
				if (isPlaying) {
					if (MusicDecoder.resetTimeFlag) {
						long t2_1 = t2;

						do {
							t2 = (new Date()).getTime();

							dt = musicDecoder.msTotal - (t2 - t1);

							Thread.sleep(10);
						} while (dt > 0);

						musicDecoder.msRead = 0f;
						musicDecoder.msTotal = 0f;
						t1 = (new Date()).getTime();
						t2 = t1;

						long t2_2 = t2;

						System.out.println("(player) force delay: " + (t2_2 - t2_1));

						MusicDecoder.resetTimeFlag = false;
					}

					if (MusicDecoder.disconnectResetTimeFlag) {
						t1 = (new Date()).getTime();
						t2 = t1;
						musicDecoder.msRead = 0f;
						musicDecoder.msTotal = 0f;
						MusicDecoder.disconnectResetTimeFlag = false;
					}

					try {
						if (TrackInfo.isMicPath(mPath)) {
							data = Microphone.readFrame(mPosition).toByteArray();
						} else if (TrackInfo.isLoopbackPath(mPath)) {
							data = Loopback.readFrame(mPosition).toByteArray();
						} else {
							data = musicDecoder.readFrame().toByteArray();
						}

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
						os.write(data);
						os.flush();
					}
				} else {
					Thread.sleep(10);
					musicDecoder.msRead = 0f;
					musicDecoder.msTotal = 0f;
					t1 = (new Date()).getTime();
					t2 = t1;
					dt = 0f;
				}

				if (mForceDisconnectFlag) {
					System.out.println("(player) disconnect flag: throwing DisconnectException");
					throw new DisconnectException();
				}

				if (musicDecoder.msRead > 300) {
					do {
						t2 = (new Date()).getTime();
						dt = musicDecoder.msTotal - (t2 - t1);

						Thread.sleep(10);
					} while (dt > 200);

					musicDecoder.msRead = 0f;
				}
			}
		}
	}
}
