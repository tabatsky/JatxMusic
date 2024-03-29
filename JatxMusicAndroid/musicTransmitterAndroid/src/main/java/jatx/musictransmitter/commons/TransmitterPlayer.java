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
package jatx.musictransmitter.commons;

import android.util.Log;

import jatx.debug.Debug;
import jatx.musiccommons.mp3.Frame.WrongFrameException;
import jatx.musictransmitter.interfaces.UIController;

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
	
	public static final int FRAME_HEADER_SIZE = 32;
	
	public static final int[] FRAME_RATES = {32000, 44100, 48000};
	
	volatile WeakReference<UIController> ref;
	
	volatile int mCount;
	
	volatile List<File> mFileList;
	
	volatile int mPosition;
	volatile boolean isPlaying;
	
	volatile boolean mForceDisconnectFlag;

	volatile boolean microphoneOk;

	volatile String mPath;
	volatile Mp3Decoder mDecoder;
	
	volatile long t1;
	volatile long t2;
	volatile float dt;

	volatile TransmitterController tc;
	
	public TransmitterPlayer(List<File> fileList, UIController uiController, Mp3Decoder decoder) {
		ref = new WeakReference<UIController>(uiController);
		setFileList(fileList);
		
		isPlaying = false;
		mForceDisconnectFlag = false;
		
		mPath = "";
		mDecoder = decoder;
	}

	public void setTransmitterController(TransmitterController tc) {
		this.tc = tc;
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
		if (mPath.equals(TrackInfo.MIC_PATH)) {
			try {
				Microphone.start();
				microphoneOk = true;
			} catch (Microphone.MicrophoneInitException e) {
				Log.e("play", Debug.exceptionToString(e));
				microphoneOk = false;
				UIController ui = ref.get();
				if (ui != null) {
					ui.errorMsg("Error: cannot init microphone");
				}
			}
		}
	}

	public void pause() {
		System.out.println("(player) pause");
		isPlaying = false;
        if (mPath.equals(TrackInfo.MIC_PATH)) {
			try {
				Microphone.stop();
			} catch (Throwable e) {
				Log.e("pause", Debug.exceptionToString(e));
			}
        }
	}

	public void seek(double progress) {
		boolean needToPlay = isPlaying;
		pause();
		try {
			mDecoder.seek(progress);
		} catch (Mp3Decoder.Mp3DecoderException e) {
			e.printStackTrace();
		}
		if (needToPlay) play();
	}

	private void forcePause() {
		System.out.println("(player) force pause");
		isPlaying = false;
		
		//Globals.tc.pause();
		tc.pause();
		
		final UIController uiController = ref.get();
		if (uiController !=null) {
			uiController.forcePause();
		}
	}

	public void setPosition(final int position) {		
		pause();
		
		if (position<0||mCount<=0) return;
		
		mPosition = position;
		if (mPosition>=mCount) mPosition = 0;
		
		System.out.println("(player) position: " + Integer.valueOf(position).toString());
		
		mPath = mFileList.get(position).getAbsolutePath();
		System.out.println("(player) path: " + mPath);
	
		final UIController uiController = ref.get();
		if (uiController !=null) {
			final String info = TrackInfo.getByPath(mPath).toString();
			System.out.println("(player) info: " + info);
			
			uiController.setPosition(mPosition);
		}
		
		try {
			mDecoder.setPath(mPath);
			mDecoder.setPosition(mPosition);
			//mDecoder.resetTimeFlag = true;
		} catch (Mp3Decoder.Mp3DecoderException e) {
			System.err.println("(player) " + e.getMessage());
		}

		play();
	}
	
	public void nextTrack() {
		try {
			final UIController uiController = ref.get();
			if (uiController!=null) {
				uiController.nextTrack();
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
						final UIController uiController = ref.get();
						if (uiController !=null) {
							uiController.setWifiStatus(true);
						} else {
							System.err.println("(player) uicontroller: null");
						}
					}
					
					translateMusic(os);
				} catch (SocketTimeoutException e) {
					System.err.println("(player) socket timeout");
					
					mForceDisconnectFlag = false;
					mDecoder.disconnectResetTimeFlag = true;
				} catch (DisconnectException e){
					System.err.println("(player) socket force disconnect");
					System.err.println("(player) " + (new Date()).getTime()%10000);
					
					mForceDisconnectFlag = false;
					mDecoder.disconnectResetTimeFlag = true;
				} catch (IOException e) {
					System.err.println("(player) socket disconnect");
					System.err.println("(player) " + (new Date()).getTime()%10000);
					
					//Globals.tc.forceDisconnect();
					tc.forceDisconnect();
					
					Thread.sleep(250);
					
					mForceDisconnectFlag = false;
					mDecoder.disconnectResetTimeFlag = true;
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
					
					final UIController uiController = ref.get();
					if (uiController !=null) {
						uiController.setWifiStatus(false);
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
		byte[] data = null;
		
		t1 = (new Date()).getTime();
		t2 = t1;
		dt = 0f;
		
		while (true) {
			if (isPlaying) {
				if (mDecoder.resetTimeFlag) {
					long t2_1 = t2;
					
					do {
						t2 = (new Date()).getTime();
						dt = mDecoder.msTotal - (t2-t1);
						
						Thread.sleep(10);
					} while (dt>0);
					
					mDecoder.msRead = 0f;
					mDecoder.msTotal = 0f;
					t1 = (new Date()).getTime();
					t2 = t1;
					
					long t2_2 = t2;
					
					System.out.println("(player) force delay: " + (t2_2-t2_1));
					
					mDecoder.resetTimeFlag = false;
				}
				
				if (mDecoder.disconnectResetTimeFlag) {
					t1 = (new Date()).getTime();
					t2 = t1;
					mDecoder.msRead = 0f;
					mDecoder.msTotal = 0f;
					mDecoder.disconnectResetTimeFlag = false;
				}
				
				try {
                    if (mPath.equals(TrackInfo.MIC_PATH)) {
                        data = Microphone.readFrame(mPosition).toByteArray();
                    } else {
                        data = mDecoder.readFrame().toByteArray();
                    }
				} catch (Mp3Decoder.Mp3DecoderException e) {
                    data = null;
                    e.printStackTrace();
                    Thread.sleep(200);
                } catch (Microphone.MicrophoneReadException e) {
                    data = null;
                    e.printStackTrace();
                    Thread.sleep(200);
				} catch (Mp3Decoder.TrackFinishException e) {
					data = null;
					System.out.println("(player) track finish");
					nextTrack();
				} catch (WrongFrameException e) {
					data = null;
					System.err.println("(player) wrong frame");
					System.err.println("(player) " + e.getMessage());
					nextTrack();
				}
				
				if (data!=null) {
					os.write(data);
					os.flush();
				}
			} else {
				Thread.sleep(10);
				mDecoder.msRead = 0f;
				mDecoder.msTotal = 0f;
				t1 = (new Date()).getTime();
				t2 = t1;
				dt = 0f;
			}
			
			if (mForceDisconnectFlag) {
				System.out.println("(player) disconnect flag: throwing DisconnectException");
				throw new DisconnectException();
			}		
			
			if (mDecoder.msRead>300) {
				do {
					t2 = (new Date()).getTime();
					dt = mDecoder.msTotal - (t2-t1);
					
					Thread.sleep(10);
				} while (dt>200);
				
				mDecoder.msRead = 0f;
			}
		}
	}
}
