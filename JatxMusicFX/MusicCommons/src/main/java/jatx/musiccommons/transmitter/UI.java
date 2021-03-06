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

import java.io.File;
import java.util.List;

public interface UI {
	public void setWifiStatus(boolean status);
	
	public void setPosition(int position);
	
	public void updateTrackList(List<TrackInfo> trackList, List<File> fileList);
	
	public void setCurrentTime(float currentMs, float trackLengthMs);
	
	public void forcePause();

	public void nextTrack();
}
