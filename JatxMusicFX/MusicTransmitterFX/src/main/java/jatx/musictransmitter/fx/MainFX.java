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
package jatx.musictransmitter.fx;

import jatx.musiccommons.transmitter.*;
import jatx.musiccommons.transmitter.threads.TimeUpdater;
import jatx.musiccommons.transmitter.threads.TransmitterController;
import jatx.musiccommons.transmitter.threads.TransmitterPlayer;
import jatx.musiccommons.transmitter.threads.TransmitterPlayerConnectionKeeper;
import jatx.musiccommons.util.FolderUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import javax.sound.sampled.LineUnavailableException;

public class MainFX extends Application implements UI {
	public static final String SETTINGS_DIR_PATH;
	
	private static Image playImg;
	private static Image pauseImg;

	private static Image repeatImg;
	private static Image shuffleImg;

	static {
		String path = System.getProperty("user.home") + File.separator + ".jatxmusic_transmitter";
		File tmp = new File(path);
		tmp.mkdirs();
		
		if (tmp.exists()) {
			SETTINGS_DIR_PATH = path;
		} else {
			SETTINGS_DIR_PATH = ".";
		}
	}
	
	private volatile boolean isPlaying = false;
	private volatile boolean isWifiOk = false;
	private volatile int wifiReceiverCount = 0;
	private volatile boolean isShuffle = false;
	
	private MenuBar mMenuBar;
	
	private ListView<String> mListView;
	private Button mPlayPauseToggleButton;
	private Button mVolumeDownButton;
	private Button mVolumeUpButton;
	private Button mWifiButton;
	private Button mFwdButton;
	private Button mRevButton;
	private Label mVolLabel;
	private Label mReceiverCountLabel;
	private ProgressBar mProgressBar;
	private Button mShuffleToggleButton;

	private Stage mStage;
	
	private List<File> mFileList;
	
	private File mCurrentMusicDir;
	private File mCurrentListDir;
	
	private MyList mItems;
	
	private int mCurrentPosition = -1;

    private static final List<Integer> shuffledList;
    static {
        shuffledList = new ArrayList<>();
        for (int i=0; i<10000; i++) {
            shuffledList.add(i);
        }
        Collections.shuffle(shuffledList);
    }

	@SuppressWarnings("unchecked")
	@Override
	public void start(Stage primaryStage) {
		mStage = primaryStage;
		mStage.setTitle("JatxMusic Transmitter");
		
		playImg = new Image("/icons/ic_play.png");
		pauseImg = new Image("/icons/ic_pause.png");

		repeatImg = new Image("/icons/ic_repeat.png");
		shuffleImg = new Image("/icons/ic_shuffle.png");

		TrackInfo.setDBCache(new SQLiteDBCache());
		TrackInfo.setUI(this);
		
		loadFileList(null);
		loadSettings();
		
		try {			
			Parent root = FXMLLoader.load(
					Objects.requireNonNull(
							getClass().getResource("/fxml/main.fxml")));
			Scene scene = new Scene(root);
			
			mMenuBar = (MenuBar) scene.lookup("#menu_pane_top");
			for (final Menu menu: mMenuBar.getMenus()) {
				for (final MenuItem item: menu.getItems()) {
					final String itemId = item.getId();
					
					item.setOnAction(event -> menuAction(itemId));
				}
			}
			
			mListView = (ListView<String>) scene.lookup("#my_list");
			mPlayPauseToggleButton = (Button) scene.lookup("#button_play_pause_toggle");
			mVolLabel = (Label) scene.lookup("#vol_label");
			mReceiverCountLabel = (Label) scene.lookup("#receiver_count_label");
			mVolumeUpButton = (Button) scene.lookup("#button_up");
			mVolumeDownButton = (Button) scene.lookup("#button_down");
			mWifiButton = (Button) scene.lookup("#button_wifi");
			mFwdButton = (Button) scene.lookup("#button_fwd");
			mRevButton = (Button) scene.lookup("#button_rev");
			mProgressBar = (ProgressBar) scene.lookup("#progress_bar");
			mShuffleToggleButton = (Button) scene.lookup("#button_shuffle_toggle");
			
			mListView.setOnMouseClicked(click -> {
				final int newPosition = mListView.getSelectionModel().getSelectedIndex();

				if (click.getClickCount()==2) {
					onListItemDoubleClick(newPosition);
				} else if (click.getClickCount()==1) {
					System.out.println("single clink on " + newPosition);
				}
			});
			
			mFwdButton.setOnAction(event -> onFwdClick());
			mRevButton.setOnAction(event -> onRevClick());
			mPlayPauseToggleButton.setOnAction(event -> onPlayPauseToggleClick());
			mVolumeUpButton.setOnAction(event -> onVolumeUpClick());
			mVolumeDownButton.setOnAction(event -> onVolumeDownClick());

			mShuffleToggleButton.setOnAction(event -> onShuffleToggleClick());
			if (isShuffle) {
				mShuffleToggleButton.setGraphic(new ImageView(shuffleImg));
			} else {
				mShuffleToggleButton.setGraphic(new ImageView(repeatImg));
			}
			
			mProgressBar.setProgress(0.0);
			mProgressBar.setOnMouseClicked(event -> onProgressBarClick(event));

			Platform.setImplicitExit(false);			
			mStage.setOnCloseRequest(event -> onCloseRequest());
			
			mStage.setScene(scene);
			mStage.show();
			
			prepareAndStart();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void updateWifiStatus(int count) {
		Platform.runLater(() -> {
			isWifiOk = count > 0;
			wifiReceiverCount = count;
			System.out.println("wifi: " + isWifiOk);

			final Image wifiNoImg = new Image("/icons/ic_wifi_no.png");
			final Image wifiOkImg = new Image("/icons/ic_wifi_ok.png");

			if (isWifiOk) {
				mWifiButton.setGraphic(new ImageView(wifiOkImg));
			} else {
				mWifiButton.setGraphic(new ImageView(wifiNoImg));
			}

			mReceiverCountLabel.setText(String.valueOf(wifiReceiverCount));

			if (!isWifiOk) {
				forcePause();
			}
		});
	}
	
	@Override
	public void setPosition(final int position) {
		System.out.println("current: " + position);

		Platform.runLater(() -> {
			mListView.getSelectionModel().select(position);
			mListView.scrollTo(position);
		});
	}
	
	@Override
	public void updateTrackList(List<TrackInfo> trackList, List<File> fileList) {
		Platform.runLater(() -> {
			mItems = new MyList(trackList);
			mListView.setItems(mItems);
			setPosition(mCurrentPosition);
			Globals.tp.setFileList(fileList);
		});
	}
	
	@Override
	public void setCurrentTime(final float currentMs, final float trackLengthMs) {
		
		if (trackLengthMs<=0) return;
		
		Platform.runLater(() -> mProgressBar.setProgress(currentMs/trackLengthMs));
	}
	
	@Override
	public void forcePause() {
		isPlaying = false;

		Globals.tp.pause();
		Globals.tc.pause();
		
		Platform.runLater(() -> mPlayPauseToggleButton.setGraphic(new ImageView(playImg)));
	}

	
	private void prepareAndStart() {
		try {
			Microphone.initMicList();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}

		Loopback.initLoopbackList();

		Globals.tpck = new TransmitterPlayerConnectionKeeper(this);
		Globals.tpck.start();
		
		Globals.tu = new TimeUpdater(this);
		Globals.tu.start();
		
		Globals.tp = new TransmitterPlayer(mFileList, this);
	    Globals.tp.start();
	    
	    Globals.tc = new TransmitterController(this);
	    Globals.tc.start();
	    
	    mVolLabel.setText(Globals.volume.toString()+"%");
		Globals.tc.setVolume(Globals.volume);
		
		refreshList();
	}
	
	private void menuAction(String itemId) {
		switch (itemId) {
			case "open_file_item":
				System.out.println("Open File");

				openMusicFile();
				break;
			case "open_folder_item":
				System.out.println("Open Folder");

				openDir();
				break;
			case "add_mic_item":
				System.out.println("Add Microphone");

				addMic();
				break;
			case "add_loopback_item":
				System.out.println("Add Loopback");

				addLoopback();
				break;
			case "remove_this_item":
				System.out.println("Remove Selected Tracks");

				removeSelectedTracks();
				break;
			case "remove_all_item":
				System.out.println("Remove All Tracks");

				removeAllTracks();
				break;
			case "m3u_import_item":
				System.out.println("Import M3U");

				importM3U();
				break;
			case "m3u_export_item":
				System.out.println("Export M3U");

				exportM3U();
				break;
		}
		
	}
	
	private void importM3U() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Import M3U");
		fileChooser.setInitialDirectory(mCurrentListDir);
		fileChooser.getExtensionFilters().add(
				new ExtensionFilter("M3U", "*.m3u"));
		final File selectedFile = fileChooser.showOpenDialog(mStage);
		
		mCurrentListDir = selectedFile.getParentFile();
		saveSettings();
		
		loadFileList(selectedFile.getAbsolutePath());
		refreshList();
		saveFileList(null);
	}
	
	private void exportM3U() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export M3U");
		fileChooser.setInitialDirectory(mCurrentListDir);
		fileChooser.getExtensionFilters().add(
				new ExtensionFilter("M3U", "*.m3u"));
		final File selectedFile = fileChooser.showSaveDialog(mStage);
	
		mCurrentListDir = selectedFile.getParentFile();
		saveSettings();
		
		saveFileList(selectedFile.getAbsolutePath());
	}
	
	private void openMusicFile() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open MP3 File");
		fileChooser.setInitialDirectory(mCurrentMusicDir);
		fileChooser.getExtensionFilters().add(
				new ExtensionFilter("MP3 and Flac Files", "*.mp3", "*.flac"));
		final File selectedFile = fileChooser.showOpenDialog(mStage);
		
		if (selectedFile!=null) {
			System.out.println("file: " + selectedFile.getAbsolutePath());
			
			mFileList.add(selectedFile);
			refreshList();
			
			mCurrentMusicDir = selectedFile.getParentFile();
			saveSettings();
		} else {
			System.out.println("file: null");
		}
	}

	private void addMic() {
    	try {
    		Microphone.initMicList();
    		for (int i = 0; i < Microphone.getLineCount(); i++) {
				mFileList.add(new File(TrackInfo.getMicPath(i)));
			}
			refreshList();
		} catch (Throwable t) {
    		t.printStackTrace();
		}
	}

	private void addLoopback() {
    	try {
    	    Loopback.initLoopbackList();
            for (int i = 0; i < Loopback.getDeviceCount(); i++) {
                mFileList.add(new File(TrackInfo.getLoopbackPath(i)));
            }
            refreshList();
		} catch (Throwable t) {
    		t.printStackTrace();
		}
	}

	private void openDir() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Open Dir With Music");
		dirChooser.setInitialDirectory(mCurrentMusicDir);
		File selectedDir = dirChooser.showDialog(mStage);
		
		if (selectedDir!=null) {
			System.out.println("dir: " + selectedDir.getAbsolutePath());
			
			List<File> fileList = FolderUtil.findFiles(selectedDir.getAbsolutePath(), ".*mp3$|.*flac$");
			mFileList.addAll(fileList);
			refreshList();
			
			mCurrentMusicDir = selectedDir;
			saveSettings();
		} else {
			System.out.println("dir: null");
		}
	}
	
	private void removeSelectedTracks() {
		int pos = mListView.getSelectionModel().getSelectedIndex();
		
		if (pos>=0 && pos<mFileList.size()) {
			mFileList.remove(pos);
			refreshList();
		}
		
		if (pos<mCurrentPosition) {
			setPosition(mCurrentPosition-1);
		} else if (pos==mCurrentPosition) {
			setPosition(-1);
		}
 	}
	
	private void removeAllTracks() {
		mFileList.clear();
		setPosition(-1);
		refreshList();
	}
	
	private void refreshList() {		
		saveFileList(null);
		
		TrackInfo.setFileList(mFileList);
	}
	
	private void loadFileList(String path) {
		mFileList = new ArrayList<>();
		
		File f;
		if (path==null) {
			f = new File(SETTINGS_DIR_PATH + File.separator + "current.m3u");
		} else {
			f = new File(path);
		}
		if (!f.exists()) return;
		
		try {
			Scanner sc = new Scanner(new FileInputStream(f));
			
			while(sc.hasNextLine()) {
				String trackPath = sc.nextLine();
				
				File track = new File(trackPath);
				if (track.exists() || TrackInfo.isMicPath(track.getAbsolutePath()) || TrackInfo.isLoopbackPath(track.getAbsolutePath())) {
					mFileList.add(track);
				}
			}
			
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void saveFileList(String path) {
		File f;
		if (path==null) {
			f = new File(SETTINGS_DIR_PATH + File.separator + "current.m3u");
		} else {
			f = new File(path);
		}
		
		try {
			PrintWriter pw = new PrintWriter(new FileOutputStream(f));

			for (File file : mFileList) {
				pw.println(file.getAbsolutePath());
			}
			
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static final String KEY_CURRENT_MUSIC_DIR = "CURRENT_MUSIC_DIR=";
	private static final String KEY_CURRENT_LIST_DIR = "CURRENT_LIST_DIR=";
	private static final String KEY_VOLUME = "VOLUME=";
	private static final String KEY_IS_SHUFFLE = "IS_SHUFFLE=";

	private void loadSettings() {
		mCurrentMusicDir = new File(System.getProperty("user.home"));
		mCurrentListDir = new File(System.getProperty("user.home"));
		Globals.volume = 100;
		
		File f = new File(SETTINGS_DIR_PATH + File.separator + "settings.txt");
		if (!f.exists()) return;
		
		try {
			Scanner sc = new Scanner(f);
			
			while(sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				
				if (line.startsWith(KEY_CURRENT_MUSIC_DIR)) {
					String path = line.replace(KEY_CURRENT_MUSIC_DIR, "");
					File fmd = new File(path);
					if (fmd.exists()) mCurrentMusicDir = fmd;
				}
				
				if (line.startsWith(KEY_CURRENT_LIST_DIR)) {
					String path = line.replace(KEY_CURRENT_LIST_DIR, "");
					File fld = new File(path);
					if (fld.exists()) mCurrentListDir = fld;
				}
				
				if (line.startsWith(KEY_VOLUME)) {
					String volStr = line.replace(KEY_VOLUME, "");
					try {
						Globals.volume = Integer.parseInt(volStr);
					} catch (NumberFormatException e) {
						Globals.volume = 100;
					}
				}

				if (line.startsWith(KEY_IS_SHUFFLE)) {
					String isShuffleStr = line.replace(KEY_IS_SHUFFLE, "");
					try {
						isShuffle = Boolean.parseBoolean(isShuffleStr);
					} catch (Exception e) {
						isShuffle = false;
					}
				}
			}
			
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void saveSettings() {
		File f = new File(SETTINGS_DIR_PATH + File.separator + "settings.txt");
		
		try {
			PrintWriter pw = new PrintWriter(f);
			
			pw.println(KEY_CURRENT_MUSIC_DIR + mCurrentMusicDir.getAbsolutePath());
			pw.println(KEY_CURRENT_LIST_DIR + mCurrentListDir.getAbsolutePath());
			pw.println(KEY_VOLUME + Globals.volume);
			pw.println(KEY_IS_SHUFFLE + isShuffle);
			
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private int getRealPosition() {
		if (mCurrentPosition < 0) {
			return mCurrentPosition;
		} else if (isShuffle) {
			return shuffledList.get(mCurrentPosition % shuffledList.size()) % mFileList.size();
		} else {
			return (mCurrentPosition % shuffledList.size()) % mFileList.size();
		}
	}

	private void onShuffleToggleClick() {
		isShuffle = !isShuffle;
		if (isShuffle) {
			mCurrentPosition = shuffledList.indexOf(mCurrentPosition);
			mShuffleToggleButton.setGraphic(new ImageView(shuffleImg));
		} else {
			if (mCurrentPosition >= 0) {
				mCurrentPosition = shuffledList.get(mCurrentPosition);
			}
			mShuffleToggleButton.setGraphic(new ImageView(repeatImg));
		}
		saveSettings();
	}

	private void onPlayPauseToggleClick() {
		isPlaying = !isPlaying;
		System.out.println("toogle: " + isPlaying);

		if (isPlaying) {
			if (mFileList.size()==0) {
				isPlaying = false;
				return;
			} else if (mCurrentPosition==-1) {
				Globals.tp.setPosition(0);
				setPosition(0);
			}

			mPlayPauseToggleButton.setGraphic(new ImageView(pauseImg));

			setVolumeAndPlay();
		} else {
			mPlayPauseToggleButton.setGraphic(new ImageView(playImg));

			pause();
		}
	}

	private void onRevClick() {
		if (mFileList.size()==0) return;

		final int newPosition;

		if (mCurrentPosition > 0) {
			newPosition = mCurrentPosition - 1;
		} else if (isShuffle) {
			newPosition = shuffledList.size() - 1;
		} else {
			newPosition = mFileList.size() - 1;
		}

		mCurrentPosition = newPosition;

		isPlaying = true;

		mPlayPauseToggleButton.setGraphic(new ImageView(pauseImg));

		setVolumeAndPlay();

		Globals.tp.setPosition(getRealPosition());
	}

	private void onFwdClick() {
		if (mFileList.size()==0) return;

		mCurrentPosition += 1;

		isPlaying = true;

		mPlayPauseToggleButton.setGraphic(new ImageView(pauseImg));

		setVolumeAndPlay();

		Globals.tp.setPosition(getRealPosition());
	}

	private void onListItemDoubleClick(int newPosition) {
		System.out.println("double click on " + newPosition);

		mCurrentPosition = newPosition;

		mCurrentPosition = isShuffle ? shuffledList.indexOf(newPosition) : newPosition;

		isPlaying = true;

		mPlayPauseToggleButton.setGraphic(new ImageView(pauseImg));

		pause();
		setVolumeAndPlay();

		Globals.tp.setPosition(newPosition);
	}

	private void setVolumeAndPlay() {
		Globals.tc.setVolume(Globals.volume);
		Globals.tp.play();
		Globals.tc.play();
	}

	private void pause() {
		Globals.tp.pause();
		Globals.tc.pause();
	}

	private void onVolumeUpClick() {
		Globals.volume = Globals.volume < 100 ? Globals.volume + 5 : 100;
		mVolLabel.setText(Globals.volume.toString() + "%");
		Globals.tc.setVolume(Globals.volume);
		saveSettings();
	}

	private void onVolumeDownClick() {
		Globals.volume = Globals.volume > 0 ? Globals.volume - 5 : 0;
		mVolLabel.setText(Globals.volume.toString() + "%");
		Globals.tc.setVolume(Globals.volume);
		saveSettings();
	}

	private void onProgressBarClick(MouseEvent event) {
		if (event.getButton() == MouseButton.PRIMARY){
			Bounds b1 = mProgressBar.getLayoutBounds();
			double mouseX = event.getSceneX();
			double percent = (((b1.getMinX() + mouseX ) * 100) / b1.getMaxX());
			mProgressBar.setProgress((percent) / 100.0);
			Globals.tp.seek((percent) / 100.0);
		}
	}

	private void onCloseRequest() {
		Globals.tu.interrupt();
		Globals.tp.interrupt();
		Globals.tc.setFinishFlag();
		TrackInfo.destroy();

		System.out.println("close app");

		Platform.exit();
	}

	public void nextTrack() {
		mFwdButton.fire();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
