package jatx.musiccommons.transmitter.threads;

import jatx.musiccommons.transmitter.MusicDecoder;
import jatx.musiccommons.transmitter.UI;

import java.lang.ref.WeakReference;

public class TimeUpdater extends Thread {
	private volatile WeakReference<UI> uiRef;
	
	public TimeUpdater(UI ui) {
		uiRef = new WeakReference<>(ui);
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				UI ui = uiRef.get();

				MusicDecoder musicDecoder = MusicDecoder.getInstance();

				if (ui != null && musicDecoder != null) {
					ui.setCurrentTime(musicDecoder.currentMs, musicDecoder.trackLengthSec*1000f);
				}
				
				Thread.sleep(500);
			}
		} catch (InterruptedException e) {
			System.err.println("time updater interrupted");
		}
	}
}
