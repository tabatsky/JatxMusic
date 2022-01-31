package jatx.musiccommons.transmitter;

import java.lang.ref.WeakReference;

public class TimeUpdater extends Thread {
	private volatile WeakReference<UI> ref;
	
	public TimeUpdater(UI ui, MusicDecoder decoder) {
		ref = new WeakReference(ui);
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				UI ui = ref.get();

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
