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
package jatx.musicreceiver.commons;

import java.lang.ref.WeakReference;

import jatx.musicreceiver.interfaces.ServiceController;

public class AutoConnectThread extends Thread {	
	WeakReference<ServiceController> ref;

	public AutoConnectThread(ServiceController serviceController) {
		ref = new WeakReference<ServiceController>(serviceController);
	}
	
	public void run() {
		try {
			while(true) {
				final ServiceController serviceController = ref.get();

				if (Globals.isAutoConnect) {
					serviceController.startJob();
				}
				
				Thread.sleep(5000);
			}
		} catch (InterruptedException e) {
			System.out.println("(auto connect thread) interrupted");
		}
	}
}
