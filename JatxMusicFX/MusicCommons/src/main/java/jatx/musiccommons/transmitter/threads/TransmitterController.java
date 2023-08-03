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

import jatx.musiccommons.transmitter.Globals;
import jatx.musiccommons.transmitter.UI;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;

public class TransmitterController extends Thread {
	public static final int CONNECT_PORT_CONTROLLER = 7172;
	
	volatile WeakReference<UI> uiRef;

	private volatile int volume = 0;

	private volatile ConcurrentHashMap<String, TransmitterControllerWorker> workers =
			new ConcurrentHashMap<>();

	volatile boolean finishFlag;
	
	volatile boolean mForceDisconnectFlag;
	
	public TransmitterController(UI ui) {
		finishFlag = false;
		mForceDisconnectFlag = false;
		
		uiRef = new WeakReference<>(ui);
	}
	
	public void setFinishFlag() {
		finishFlag = true;
	}
	
	public void forceDisconnect() {
		mForceDisconnectFlag = true;
	}

	public void setVolume(int volume) {
		final String msg = "(controller) set volume: " + volume;
		System.out.println(msg);
		workers.forEachValue(0L, transmitterControllerWorker -> transmitterControllerWorker.setVolume(volume));
		this.volume = volume;
	}

	public void play() {
		System.out.println("(controller) play");
		workers.forEachValue(0L, transmitterControllerWorker -> transmitterControllerWorker.play());
	}

	public void pause() {
		System.out.println("(controller) pause");
		workers.forEachValue(0L, transmitterControllerWorker -> transmitterControllerWorker.pause());
	}
	
	@Override
	public void run() {
		ServerSocket ss = null;
		
		try {
			while(!finishFlag) {
				Thread.sleep(100);

				ss = new ServerSocket(CONNECT_PORT_CONTROLLER);
				System.out.println("(controller) new server socket");

				try {
					ss.setSoTimeout(Globals.SO_TIMEOUT);
					Socket s = ss.accept();
					System.out.println("(controller) server socket accept");
					String host = s.getInetAddress().getHostAddress();
					TransmitterControllerWorker worker = new TransmitterControllerWorker(s, host);
					worker.start();
					System.out.println("(controller) worker $it started");
					workers.put(host, worker);
					final String msg = "(controller) total workers: " + workers.size();
					System.out.println(msg);
					worker.onWorkerStopped = () -> {
						System.out.println("(controller) worker $host stopped");
						workers.remove(host);
						final String msg2 = "(player) total workers: " + workers.size();
						System.out.println(msg2);
					};
					setVolume(volume);
				} catch (SocketTimeoutException e) {
					System.out.println("(controller) socket timeout");
				} finally {
					try {
						ss.close();
						System.out.println("(controller) server socket closed");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.err.println("(controller) thread interrupted");
		} finally {
			workers.forEachValue(0L, transmitterControllerWorker -> transmitterControllerWorker.setFinishWorkerFlag());
			System.out.println("(controller) workers finished");
			try {
				ss.close();
				System.out.println("(controller) server socket closed");
			} catch (Exception ex) {
				System.err.println("(controller) cannot close server socket");
			}
			System.out.println("(controller) thread finished");
		}
	}
}

