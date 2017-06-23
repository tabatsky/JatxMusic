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
package jatx.musicreceiver.interfaces;

public interface ServiceController {
	String START_JOB = "jatx.musicreceiver.android.serviceStartJob";
	String STOP_JOB = "jatx.musicreceiver.android.serviceStopJob";

	void startJob();
	void stopJob();
	
	//public boolean isAutoConnect();
	
	void play();
	void pause();
	void setVolume(int vol);
}
