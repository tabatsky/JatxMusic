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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.audio.wav.WavFileReader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.flac.FlacTag;

public class TrackInfo {

	public static String getMicPath(int micIndex) {
		return System.getProperty("os.name").toLowerCase().contains("win") ?
				"C:\\" + micIndex + ".mic" :
				"/" + micIndex + ".mic";
	}

	public static String getLoopbackPath(int loopbackIndex) {
		return System.getProperty("os.name").toLowerCase().contains("win") ?
				"C:\\" + loopbackIndex + ".loopback" :
				"/" + loopbackIndex + ".loopback";
	}

	public static boolean isMicPath(String path) {
		return path.endsWith(".mic");
	}
	public static boolean isLoopbackPath(String path) {
		return path.endsWith(".loopback");
	}

	private static volatile List<File> sFileList = null;
	private static volatile List<TrackInfo> sTrackInfoList = null;
	
	private static volatile TagWorker sTagWorker = null;
	private static volatile DBCache sDBCache = null;
	
	private static volatile WeakReference<UI> sUIRef = new WeakReference<UI>(null);
	
	private static volatile boolean pauseFlag = true;
	private static volatile boolean resetFlag = false;
	private static Object monitor = new Object();
	
	private static volatile int dbGetCounter = 0;
	private static volatile int fileGetCounter = 0;
	
	public String path;
	public String artist = "";
	public String album = "";
	public String title = "";
	public String year = "1900";
	public String length = "";
	public String number = "0";
	
	public static void setUI(UI ui) {
		sUIRef = new WeakReference<UI>(ui);
	}
	
	public static void setFileList(List<File> fileList) {
		pauseFlag = true;
		
		fileGetCounter = 0;
		dbGetCounter = 0;
		
		if (sTagWorker==null) {
			sTagWorker = new TagWorker();
			sTagWorker.start();
		}
		
		try {
			synchronized(monitor) {
				monitor.wait(1000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		sFileList = new ArrayList<File>(fileList);
		sTrackInfoList = new ArrayList<TrackInfo>();
		
		pauseFlag = false;
		resetFlag = true;
	}
	
	public static void destroy() {
		if (sTagWorker!=null) {
			sTagWorker.interrupt();
			sTagWorker = null;
		}
		
		if (sDBCache!=null) {
			sDBCache = null;
		}
	}
	
	public static void setDBCache(DBCache cache) {
		sDBCache = cache;
	}
	
	public String toString() {
		final File f = new File(path);
		final String fileName = f.getName().replace(".mp3", ""); 
		
		String result = fileName;
		
		if (title.equals("")) {
			result = result + " | " + length;
		} else if (artist.equals("")) {
			result = title + " | " + length;
		} else {
			result = title + " (" + artist + ") | " + length;
		}
		
		return result;
	}
	
	public static TrackInfo getByPath(String path) {
		return getFromFile(new File(path));
	}

	public static TrackInfo getMicInfo(int micIndex) {
		TrackInfo info = new TrackInfo();
		info.path = getMicPath(micIndex);
		info.artist = "Microphone";
		info.title = String.valueOf(micIndex);
		info.album = "Microphone";
		info.year = "1970";
		info.number = "0";
		info.length = "00:00";
		return info;
	}

	public static TrackInfo getLoopbackInfo(int loopbackIndex) {
		TrackInfo info = new TrackInfo();
		info.path = getLoopbackPath(loopbackIndex);
		info.artist = "Loopback";
		info.title = String.valueOf(loopbackIndex);
		info.album = "Loopback";
		info.year = "1970";
		info.number = "0";
		info.length = "00:00";
		return info;
	}

	public static TrackInfo getFromFile(File f) {
		if (f!=null && isMicPath(f.getAbsolutePath())) {
			final String micNumber;
			if (System.getProperty("os.name").toLowerCase().contains("win")) {
				micNumber = f.getAbsolutePath().replace("C:\\", "").replace(".mic", "");
			} else {
				micNumber = f.getAbsolutePath().replace("/", "").replace(".mic", "");
			}
			return getMicInfo(Integer.parseInt(micNumber));
		}

		if (f!=null && isLoopbackPath(f.getAbsolutePath())) {
			final String loopbackNumber;
			if (System.getProperty("os.name").toLowerCase().contains("win")) {
				loopbackNumber = f.getAbsolutePath().replace("C:\\", "").replace(".loopback", "");
			} else {
				loopbackNumber = f.getAbsolutePath().replace("/", "").replace(".loopback", "");
			}
			return getLoopbackInfo(Integer.parseInt(loopbackNumber));
		}

		if (f==null || !f.exists()) return null;
		
		final String path = f.getAbsolutePath();
		final long lastModified = f.lastModified();
		
		TrackInfo info;
		if (sDBCache!=null) {
			info = sDBCache.get(path, lastModified);
			if (info!=null) {
				dbGetCounter++;
				if (dbGetCounter%20==0) {
					fileGetCounter++;
				}

				return info;
			}
		}
		
		info = new TrackInfo();
		info.path = path;

		try {
			AudioFile af;
			af = AudioFileIO.read(f);
			int len = af.getAudioHeader().getTrackLength();
			int sec = len % 60;
			int min = (len - sec) / 60;
			info.length = String.format("%02d:%02d", min, sec);

			if (f.getName().endsWith(".mp3")) {
				MP3File mp3f = new MP3File(f);

				Tag tag = mp3f.getTag();
				info.artist = tag.getFirst(FieldKey.ARTIST).trim();
				info.album = tag.getFirst(FieldKey.ALBUM).trim();
				info.title = tag.getFirst(FieldKey.TITLE).trim();
				info.year = tag.getFirst(FieldKey.YEAR);
				info.number = tag.getFirst(FieldKey.TRACK);
				if (!info.number.equals("")) {
					Integer num = Integer.parseInt(info.number);
					if (num < 10) {
						info.number = "00" + num;
					} else if (num < 100) {
						info.number = "0" + num;
					}
				}
			} else if (f.getName().endsWith(".flac")) {
				FlacTag tag = (FlacTag) af.getTag();
				info.artist = tag.getFirst(FieldKey.ARTIST).trim();
				info.album = tag.getFirst(FieldKey.ALBUM).trim();
				info.title = tag.getFirst(FieldKey.TITLE).trim();
				info.year = tag.getFirst(FieldKey.YEAR);
				info.number = tag.getFirst(FieldKey.TRACK);
			}

			if (sDBCache != null) {
				sDBCache.put(info, lastModified);
			}
			fileGetCounter++;
		} catch (Exception e) {
		}

		return info;
	}
	
	private static class TagWorker extends Thread {		
		@Override
		public void run() {
			try {
				int current = -1;
				
				while (true) {
					synchronized(monitor) {
						monitor.notifyAll();
					}
					
					Thread.sleep(5);
					
					if (resetFlag) {
						current = -1;
						pauseFlag = false;
						resetFlag = false;
					}
					
					if (pauseFlag) continue;
					
					current++;
					
					if (sFileList==null||sFileList.size()<=current) {
						current = -1;
						pauseFlag = true;
						resetFlag = false;
						
						UI ui = sUIRef.get();
						if (ui!=null) {
							ui.updateTrackList(sTrackInfoList, sFileList);
						}
						
						continue;
					}
					
					if (fileGetCounter%20==0) {
						UI ui = sUIRef.get();
						if (ui!=null) {
							ui.updateTrackList(sTrackInfoList, sFileList);
						}
					}
					
					System.out.println(current);
					
					File f = sFileList.get(current);
					
					sTrackInfoList.add(getFromFile(f));
				}
			} catch (InterruptedException e) {
				System.err.println("Tag Worker interrupted");
			}
		}
	}
	
	public interface DBCache {
		TrackInfo get(String path, long lastModified);
		void put(TrackInfo info, long lastModified);
	}
}
