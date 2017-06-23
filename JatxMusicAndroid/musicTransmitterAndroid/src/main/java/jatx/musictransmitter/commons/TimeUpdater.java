package jatx.musictransmitter.commons;

import java.lang.ref.WeakReference;

import jatx.musictransmitter.interfaces.UIController;

public class TimeUpdater extends Thread {
	private volatile WeakReference<UIController> ref;
	private volatile Mp3Decoder mDecoder;
	
	public TimeUpdater(UIController uiController, Mp3Decoder decoder) {
		ref = new WeakReference<UIController>(uiController);
		mDecoder = decoder;
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				UIController uiController = ref.get();
				
				if (uiController !=null) {
					uiController.setCurrentTime(mDecoder.currentMs, mDecoder.trackLengthSec*1000f);
				}
				
				Thread.sleep(500);
			}
		} catch (InterruptedException e) {
			System.err.println("time updater interrupted");
		}
	}
}
