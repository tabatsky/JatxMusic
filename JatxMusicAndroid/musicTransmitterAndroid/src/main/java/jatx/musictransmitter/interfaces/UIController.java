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
package jatx.musictransmitter.interfaces;

import java.io.File;
import java.util.List;

public interface UIController {
	String SET_WIFI_STATUS = "jatx.musictransmitter.android.setWifiStatus";
	String SET_POSITION = "jatx.musictransmitter.android.setPosition";
	String SET_CURRENT_TIME = "jatx.musictransmitter.android.setCurrentTime";
	String NEXT_TRACK = "jatx.musictransmitter.android.nextTrack";
	String FORCE_PAUSE = "jatx.musictransmitter.android.forcePause";

	void setWifiStatus(boolean status);
	void setPosition(int position);
	void setCurrentTime(float currentMs, float trackLengthMs);
	void forcePause();
	void errorMsg(String msg);
	void nextTrack();
}
