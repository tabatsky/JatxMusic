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
package jatx.musictransmitter.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import jatx.musictransmitter.commons.Globals;
import jatx.musictransmitter.commons.TrackInfo;
import jatx.musictransmitter.interfaces.ServiceInterface;
import jatx.musictransmitter.interfaces.UI;
import jatx.musictransmitter.interfaces.UIController;
import jatx.debug.Debug;
import jatx.musictransmitter.commons.FolderUtil;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.TextView;
import ar.com.daidalos.afiledialog.FileChooserActivity;

public class MusicTransmitterActivity extends AppCompatActivity implements UI, ServiceInterface {
	private static final String PREFS_NAME = "MusicTransmitterPreferences";
	private static final String LOG_TAG_ACTIVITY = "transmitterMainActivity";
	
	private static final int REQUEST_OPEN_FILE = 501;
	private static final int REQUEST_OPEN_DIR = 502;
	private static final int REQUEST_EXPORT_LIST = 503;
	private static final int REQUEST_IMPORT_LIST = 504;
	public static final int REQUEST_EDIT_TRACK = 505;

	public static final int PERMISSION_SDCARD_REQUEST_OPEN_MP3 = 1112;
	public static final int PERMISSION_SDCARD_REQUEST_OPEN_DIR = 1113;
	public static final int PERMISSION_SDCARD_REQUEST_IMPORT_M3U8 = 1114;
	public static final int PERMISSION_SDCARD_REQUEST_EXPORT_M3U8 = 1115;
	public static final int PERMISSION_SDCARD_REQUEST_LOAD_FILELIST = 1116;
	public static final int PERMISSION_SDCARD_REQUEST_SAVE_FILELIST = 1117;
	public static final int PERMISSION_MIC_REQUEST = 1118;
	public static final String PERMISSION_READ = Manifest.permission.READ_EXTERNAL_STORAGE;
	public static final String PERMISSION_WRITE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
	public static final String[] PERMISSIONS_SDCARD = new String[]{PERMISSION_READ, PERMISSION_WRITE};
	public static final String PERMISSION_MIC = Manifest.permission.RECORD_AUDIO;
	public static final String[] PERMISSIONS_MIC = new String[]{PERMISSION_MIC};

    boolean isPlaying;
	List<File> mFileList;
	private TrackListAdapter mAdapter;
	
	private File mCurrentMusicDir;
	private File mCurrentListDir;
	
	private ListView mListView;
	private ImageButton mPlayButton;
	private ImageButton mPauseButton;
	private ImageButton mRevButton;
	private ImageButton mFwdButton;
	private ImageButton mVolDownButton;
	private ImageButton mVolUpButton;
	private ImageButton mRepeatButton;
	private ImageButton mShuffleButton;
	private TextView mVolLabel;
	private ImageView mWifiOkIcon;
	private ImageView mWifiNoIcon;
	private SeekBar mProgressBar;
	
	private int mCurrentPosition = -1;

	private int hoursFromInstall;
	private boolean reviewOfferWasShown;

    private volatile boolean isShuffle = false;
    private static List<Integer> shuffledList;
    static {
        shuffledList = new ArrayList<Integer>();
        for (int i=0; i<10000; i++) {
            shuffledList.add(i);
        }
        Collections.shuffle(shuffledList);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_transmitter);
		
		Debug.setCustomExceptionHandler(getExternalFilesDir(null));
		
		Log.i(LOG_TAG_ACTIVITY, "on create");

		if (getShowManual()) {
			showManual(true);
		}
		
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		
		TrackInfo.setDBCache(new MusicInfoDBHelper(this));
		TrackInfo.setUI(this);
		
		loadFileList(null, false);
		loadSettings();

		if (hoursFromInstall >= 72 && !reviewOfferWasShown) {
			reviewOfferWasShown = true;
			saveSettings();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.need_you_help));
			builder.setMessage(getString(R.string.do_you_like_this_app));
			builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					dialogInterface.dismiss();
				}
			});
			builder.setPositiveButton(getString(R.string.item_review_app), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					dialogInterface.dismiss();
					try {
						startActivity(new Intent(Intent.ACTION_VIEW,
								Uri.parse("market://details?id=jatx.musictransmitter.android")));
					} catch (android.content.ActivityNotFoundException anfe) {
						startActivity(new Intent(Intent.ACTION_VIEW,
								Uri.parse("https://play.google.com/store/apps/details?id=jatx.musictransmitter.android")));
					}
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
		}

		mAdapter = new TrackListAdapter(this);
		
	    mListView = (ListView)findViewById(R.id.list);
	    mPlayButton = (ImageButton)findViewById(R.id.play);
	    mPauseButton = (ImageButton)findViewById(R.id.pause);
	    mRevButton = (ImageButton)findViewById(R.id.rev);
	    mFwdButton = (ImageButton)findViewById(R.id.fwd);
	    mVolDownButton = (ImageButton) findViewById(R.id.vol_down);
	    mVolUpButton = (ImageButton) findViewById(R.id.vol_up);
	    mVolLabel = (TextView) findViewById(R.id.vol_label);
	    mWifiOkIcon = (ImageView) findViewById(R.id.wifi_ok);
	    mWifiNoIcon = (ImageView) findViewById(R.id.wifi_no);
	    mProgressBar = (SeekBar) findViewById(R.id.progress_bar);
	    mRepeatButton = (ImageButton)findViewById(R.id.repeat);
	    mShuffleButton = (ImageButton)findViewById(R.id.shuffle);
	    
	    mListView.setAdapter(mAdapter);
	    
	    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mCurrentPosition = position;
				mPlayButton.performClick();
				//Globals.tp.setPosition(position);
				tpSetPosition(position);
			}
		});
	    
	    mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				//removeTrack(position);
				
				TrackLongTapDialog dialog =
						TrackLongTapDialog.newInstance(MusicTransmitterActivity.this, position);
				dialog.show(getSupportFragmentManager(), "dialog");
				
				return true;
			}
		});
	    
	    mPlayButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mFileList.size()==0) {
				//if (Globals.fileList.size()==0) {
					return;
				} else if (mCurrentPosition==-1) {
					//Globals.tp.setPosition(0);
					mCurrentPosition = 0;
					tpSetPosition(0);
	        		setPosition(0);
				}

				isPlaying = true;
                TrackInfo trackInfo = mAdapter.getItem(mCurrentPosition);
                MusicTransmitterNotification.showNotification(MusicTransmitterActivity.this, trackInfo.artist, trackInfo.title, isPlaying);

				mPlayButton.setVisibility(View.GONE);
				mPauseButton.setVisibility(View.VISIBLE);
				//Globals.tp.play();
				tpPlay();
				//Globals.tc.play();
				tcPlay();
			}
		});
	    
	    mPauseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                isPlaying = false;
                TrackInfo trackInfo = mAdapter.getItem(mCurrentPosition);
                MusicTransmitterNotification.showNotification(MusicTransmitterActivity.this, trackInfo.artist, trackInfo.title, isPlaying);

				mPauseButton.setVisibility(View.GONE);
				mPlayButton.setVisibility(View.VISIBLE);
				//Globals.tp.pause();
				tpPause();
				//Globals.tc.pause();
				tcPause();
			}
		});

	    mRepeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isShuffle = true;

                mRepeatButton.setVisibility(View.GONE);
                mShuffleButton.setVisibility(View.VISIBLE);
            }
        });

	    mShuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isShuffle = false;

                mRepeatButton.setVisibility(View.VISIBLE);
                mShuffleButton.setVisibility(View.GONE);
            }
        });

	    mVolDownButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Globals.volume = Globals.volume>0?Globals.volume-5:0;
				mVolLabel.setText(Globals.volume.toString()+"%");
				//Globals.tc.setVolume(Globals.volume);
				tcSetVolume(Globals.volume);
				saveSettings();
			}
		});
	    
	    mFwdButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mFileList.size()==0) return;
				//if (Globals.fileList.size()==0) return;
				
				final int newPosition = (mCurrentPosition+1)%mFileList.size();
				//final int newPosition = (mCurrentPosition+1)%Globals.fileList.size();

				mCurrentPosition = newPosition;
				if (mCurrentPosition<0||mCurrentPosition>=mFileList.size()) {
					//if (position<0||position>=Globals.fileList.size()) {
					return;
				}

                final int realPosition;
                if (!isShuffle) {
                    realPosition = mCurrentPosition;
                } else {
                    realPosition = shuffledList.get(mCurrentPosition) % mFileList.size();
                }

                mPlayButton.performClick();
				
        		//Globals.tp.setPosition(newPosition);
				tpSetPosition(realPosition);
			}
		});
	    
	    mRevButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mFileList.size()==0) return;
				//if (Globals.fileList.size()==0) return;
				
				final int newPosition;
				
				if (mCurrentPosition>0) {
					newPosition = mCurrentPosition - 1;
				} else {
					newPosition = mFileList.size() - 1;
					//newPosition = Globals.fileList.size() - 1;
				}

                mCurrentPosition = newPosition;
                if (mCurrentPosition<0||mCurrentPosition>=mFileList.size()) {
                    //if (position<0||position>=Globals.fileList.size()) {
                    return;
                }

				mPlayButton.performClick();

                final int realPosition;
                if (!isShuffle) {
                    realPosition = mCurrentPosition;
                } else {
                    realPosition = shuffledList.get(mCurrentPosition) % mFileList.size();
                }

                //Globals.tp.play();
				tpPlay();
				//Globals.tc.play();
				tcPlay();
        		
        		//Globals.tp.setPosition(newPosition);
				tpSetPosition(realPosition);
			}
		});
	    
	    mVolUpButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Globals.volume = Globals.volume<100?Globals.volume+5:100;
				mVolLabel.setText(Globals.volume.toString()+"%");
				//Globals.tc.setVolume(Globals.volume);
				tcSetVolume(Globals.volume);
				saveSettings();
			}
		});
	    
	    mProgressBar.setMax(1000);
	    mProgressBar.setProgress(0);

	    mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    double progress = i / 1000.0;
                    tpSeek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

		//MusicTransmitterNotification.showNotification(this);

	    prepareAndStart();
	}

	/*
	@Override
	public void onNewIntent(Intent intent) {
		String action = intent.getStringExtra("action");
		if (action.equals("play")) {
			mPlayButton.performClick();
		} else if (action.equals("pause")) {
			mPauseButton.performClick();
		}
	}
	*/

	@Override
	protected void onStart() {
		super.onStart();
		
		Log.i(LOG_TAG_ACTIVITY, "on start");
	}
	
	@Override
	protected void onStop() {
		Log.i(LOG_TAG_ACTIVITY, "on stop");
		
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		Log.i(LOG_TAG_ACTIVITY, "on destroy");

		MusicTransmitterNotification.hideNotification(this);

        Intent intent = new Intent(MusicTransmitterService.STOP_SERVIVE);
        sendBroadcast(intent);

		//Globals.tu.interrupt();
		//Globals.tp.interrupt();
		//Globals.tc.setFinishFlag();
		//Intent intent = new Intent(MusicTransmitterService.STOP_SERVIVE);
		//sendBroadcast(intent);
		TrackInfo.destroy();
		
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_music_transmitter, menu);
	    return super.onCreateOptionsMenu(menu);
	} 
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		
		switch (item.getItemId()) {
	        case R.id.item_menu_add_track:
	        	openMP3File(null, false);
	        	return true;
	        
	        case R.id.item_menu_add_folder:
		        openDir(null, false);
	        	return true;

			case R.id.item_menu_add_mic:
				addMic(false);
				return true;
	        	
	        case R.id.item_menu_remove_track:
		        Toast.makeText(this, getString(R.string.toast_long_tap), Toast.LENGTH_LONG).show();
	        	return true;
	        
	        case R.id.item_menu_remove_all:
		        removeAllTracks();
	        	return true;
	        	
	        case R.id.item_menu_export_m3u8:
	        	exportM3U8(null, false);
	        	return true;
	        	
	        case R.id.item_menu_import_m3u8:	
	        	importM3U8(null, false);
	        	return true;
	        	
	        case R.id.item_review_app:
	        	try {
			    	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("market://details?id=jatx.musictransmitter.android")));
				} catch (android.content.ActivityNotFoundException anfe) {
			    	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("https://play.google.com/store/apps/details?id=jatx.musictransmitter.android")));
				}
	        	return true;
	        
	        case R.id.item_receiver_android:
	        	try {
			    	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("market://details?id=jatx.musicreceiver.android")));
				} catch (android.content.ActivityNotFoundException anfe) {
			    	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("https://play.google.com/store/apps/details?id=jatx.musicreceiver.android")));
				}
	        	return true;
	        	
	        /*
	        case R.id.item_javafx_version:
	        	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("https://yadi.sk/d/T2QKUqOGgxKR8")));
	        	return true;
	        */
	        	
	        case R.id.item_receiver_javafx:
	        	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("https://yadi.sk/d/mUHvCxcchFZ7s")));
	        	return true;
	        
	        case R.id.item_transmitter_javafx:
	        	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("https://yadi.sk/d/9vBoZFZVhFZ7D")));
	        	return true;
	        
	        case R.id.item_source_code:
	        	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("https://github.com/tabatsky/JatxMusic")));
	        	return true;
	        	
	        case R.id.item_dev_site:
	        	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("http://tabatsky.ru")));
	        	return true;

			case R.id.item_show_manual:
				showManual(false);
				return true;

			case R.id.item_show_my_ip:
				showIP();
				return true;

	        default:
	        	return false;
	    }
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(LOG_TAG_ACTIVITY, "on activity result");
		if (resultCode!=RESULT_OK) return;
		
		if (requestCode==REQUEST_EDIT_TRACK&&resultCode==RESULT_OK) {
			System.out.println("result edit track");
			
			refreshList();
			return;
		}
		
		String filePath = "";
        
        Bundle bundle = data.getExtras();
        if(bundle != null) {
            if(bundle.containsKey(FileChooserActivity.OUTPUT_NEW_FILE_NAME)) {
            	File folder = (File) bundle.get(FileChooserActivity.OUTPUT_FILE_OBJECT);
                String name = bundle.getString(FileChooserActivity.OUTPUT_NEW_FILE_NAME);
                filePath = folder.getAbsolutePath() + "/" + name;
            } else {
                File file = (File) bundle.get(FileChooserActivity.OUTPUT_FILE_OBJECT);
                filePath = file.getAbsolutePath();
            }
        }
        
        File f = new File(filePath);
		
		if (requestCode==REQUEST_OPEN_FILE&&resultCode==RESULT_OK) {
			openMP3File(f, true);
		} else if (requestCode==REQUEST_OPEN_DIR&&resultCode==RESULT_OK) {
			openDir(f, true);
		} else if (requestCode==REQUEST_EXPORT_LIST&&resultCode==RESULT_OK) {			
			exportM3U8(f, true);
		} else if (requestCode==REQUEST_IMPORT_LIST&&resultCode==RESULT_OK) {
			importM3U8(f, true);
		} 
		
	}
	
	@Override
	public void onBackPressed() {
		Log.i(LOG_TAG_ACTIVITY, "back pressed");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder
				.setMessage(getString(R.string.really_quit))
				.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
						finish();
					}
				})
				.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	@Override
	public void setWifiStatus(final boolean status) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				if (status) {
					mWifiOkIcon.setVisibility(View.VISIBLE);
					mWifiNoIcon.setVisibility(View.GONE);
				} else {
					mWifiNoIcon.setVisibility(View.VISIBLE);
					mWifiOkIcon.setVisibility(View.GONE);
				}
			}
		});
	}
	
	@Override
	public void setPosition(final int position) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				mAdapter.setCurrentPosition(position);
				mListView.setSelection(position);

				if (position > 0 && position < mAdapter.getCount()) {
					TrackInfo trackInfo = mAdapter.getItem(position);
					//Log.e("artist", trackInfo.artist == null ? "null" : trackInfo.artist);
					//Log.e("title", trackInfo.title == null ? "null" : trackInfo.title);

					MusicTransmitterNotification.showNotification(MusicTransmitterActivity.this, trackInfo.artist, trackInfo.title, isPlaying);
				}
			}
		});
	}
	
	@Override
	public void updateTrackList(final List<TrackInfo> trackList, final List<File> fileList) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				mAdapter.setTrackList(trackList);
				//Globals.tp.setFileList(fileList);
				tpSetFileList(fileList);
				setPosition(mCurrentPosition);
			}
		});
	}
	
	@Override
	public void setCurrentTime(final float currentMs, final float trackLengthMs) {
		if (trackLengthMs<=0) return;
		
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				int progress = (int)((currentMs*1000)/trackLengthMs);
				mProgressBar.setProgress(progress);
			}
		});
	}
	
	@Override
	public void forcePause() {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
                isPlaying = false;
                TrackInfo trackInfo = mAdapter.getItem(mCurrentPosition);
                MusicTransmitterNotification.showNotification(MusicTransmitterActivity.this, trackInfo.artist, trackInfo.title, isPlaying);
				mPauseButton.setVisibility(View.GONE);
				mPlayButton.setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	public void errorMsg(final String msg) {
		Toast.makeText(MusicTransmitterActivity.this, msg, Toast.LENGTH_LONG).show();
	}

	private void prepareAndStart() {
		if (!MusicTransmitterService.isInstanceRunning) {

            Intent intent = new Intent(this, MusicTransmitterService.class);
            String[] filePathArray = new String[mFileList.size()];
            for (int i=0; i<mFileList.size(); i++) {
                filePathArray[i] = mFileList.get(i).getAbsolutePath();
            }
            intent.putExtra("filePathArray", filePathArray);
            startService(intent);
		} else {
			Intent intent = new Intent(MusicTransmitterService.STATUS_REQUEST);
			sendBroadcast(intent);
		}

		//Mp3Decoder decoder = new JLayerMp3Decoder();
		
		//Globals.tu = new TimeUpdater(this, decoder);
		//Globals.tu.start();
		
		//Globals.tp = new TransmitterPlayer(mFileList, this, decoder);
	    //Globals.tp.start();
	    
	    //Globals.tc = new TransmitterController(this);
	    //Globals.tc.start();
	    
	    mVolLabel.setText(Globals.volume.toString()+"%");
		//Globals.tc.setVolume(Globals.volume);
		tcSetVolume(Globals.volume);
		
		refreshList();

		BroadcastReceiver setPositionReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int position = intent.getIntExtra("position", 0);
				setPosition(position);
			}
		};
		IntentFilter setPositionFilter = new IntentFilter(UIController.SET_POSITION);
		registerReceiver(setPositionReceiver, setPositionFilter);

		BroadcastReceiver setCurrentTimeReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				float currentMs = intent.getFloatExtra("currentMs", 0);
				float trackLengthMs = intent.getFloatExtra("trackLengthMs", 0);
				setCurrentTime(currentMs, trackLengthMs);
			}
		};
		IntentFilter setCurrentTimeFilter = new IntentFilter(UIController.SET_CURRENT_TIME);
		registerReceiver(setCurrentTimeReceiver, setCurrentTimeFilter);

		BroadcastReceiver setWifiStatusReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				boolean status = intent.getBooleanExtra("status", false);
				Log.e(LOG_TAG_ACTIVITY, "setting wifi status: " + status);
				setWifiStatus(status);
			}
		};
		IntentFilter setWifiStatusFilter = new IntentFilter(UIController.SET_WIFI_STATUS);
		registerReceiver(setWifiStatusReceiver, setWifiStatusFilter);

		BroadcastReceiver forcePauseReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				forcePause();
			}
		};
		IntentFilter forcePauseFilter = new IntentFilter(UIController.FORCE_PAUSE);
		registerReceiver(forcePauseReceiver, forcePauseFilter);

		BroadcastReceiver clickPlayReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mPlayButton.performClick();
			}
		};
		IntentFilter clickPlayFilter = new IntentFilter(MusicTransmitterNotification.CLICK_PLAY);
		registerReceiver(clickPlayReceiver, clickPlayFilter);

		BroadcastReceiver clickPauseReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mPauseButton.performClick();
			}
		};
		IntentFilter clickPauseFilter = new IntentFilter(MusicTransmitterNotification.CLICK_PAUSE);
		registerReceiver(clickPauseReceiver, clickPauseFilter);

		BroadcastReceiver clickRevReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mRevButton.performClick();
			}
		};
		IntentFilter clickRevFilter = new IntentFilter(MusicTransmitterNotification.CLICK_REV);
		registerReceiver(clickRevReceiver, clickRevFilter);

		BroadcastReceiver clickFwdReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mFwdButton.performClick();
			}
		};
		IntentFilter clickFwdFilter = new IntentFilter(MusicTransmitterNotification.CLICK_FWD);
		registerReceiver(clickFwdReceiver, clickFwdFilter);
		IntentFilter nextTrackFilter = new IntentFilter(UIController.NEXT_TRACK);
		registerReceiver(clickFwdReceiver, nextTrackFilter);

        BroadcastReceiver incomingCallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    mPauseButton.performClick();
                }
            }
        };
        IntentFilter incomingCallFilter = new IntentFilter("android.intent.action.PHONE_STATE");
        registerReceiver(incomingCallReceiver, incomingCallFilter);
	}
	
	private void refreshList() {
		saveFileList(null, false);
		
		TrackInfo.setFileList(mFileList);
		//TrackInfo.setFileList(Globals.fileList);
	}
	
	private void loadSettings() {
		mCurrentListDir = Environment.getExternalStorageDirectory();
		mCurrentMusicDir = Environment.getExternalStorageDirectory();
		
		SharedPreferences sp = getSharedPreferences(PREFS_NAME, 0);
		final String listDirPath = sp.getString("listDirPath", mCurrentListDir.getAbsolutePath());
		final String musicDirPath = sp.getString("musicDirPath", mCurrentMusicDir.getAbsolutePath());
		Globals.volume = sp.getInt("volume", 100);
		
		File tmp;
		
		tmp = new File(listDirPath);
		if (tmp.exists()) mCurrentListDir = tmp;
		tmp = new File(musicDirPath);
		if (tmp.exists()) mCurrentMusicDir = tmp;

		reviewOfferWasShown = sp.getBoolean("reviewOfferWasShown", false);
		long installTime = sp.getLong("installTime", System.currentTimeMillis());
		{
			SharedPreferences.Editor editor = sp.edit();
			editor.putLong("installTime", installTime);
			editor.commit();
		}
		hoursFromInstall = (int)((System.currentTimeMillis() - installTime)/(3600*1000));
	}
	
	private void saveSettings() {
		SharedPreferences sp = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("listDirPath", mCurrentListDir.getAbsolutePath());
		editor.putString("musicDirPath", mCurrentMusicDir.getAbsolutePath());
		editor.putInt("volume", Globals.volume);
		editor.putBoolean("reviewOfferWasShown", reviewOfferWasShown);
		editor.commit();
	}

	private boolean getShowManual() {
		SharedPreferences sp = getSharedPreferences(PREFS_NAME, 0);
		return sp.getBoolean("showManual", true);
	}

	private void setNotShowManual() {
		SharedPreferences sp = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = sp.edit();
		editor.putBoolean("showManual", false);
		editor.commit();
	}

	private void showManual(boolean dontShowButton) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder
				.setTitle(R.string.manual_title)
				.setMessage(R.string.manual_message)
				.setNegativeButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		if (dontShowButton) {
			builder.setPositiveButton(R.string.button_dont_shore_anymore, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					setNotShowManual();
					dialog.dismiss();
				}
			});
		}
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void showIP() {
		WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		String ipAddress = (ip>0)?Formatter.formatIpAddress(ip):getString(R.string.not_detected_message);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder
				.setTitle(R.string.show_ip_title)
				.setMessage(ipAddress)
				.setNegativeButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		if (grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

			switch (requestCode) {
				case PERMISSION_SDCARD_REQUEST_OPEN_MP3:
					openMP3File(null, true);
					return;
				case PERMISSION_SDCARD_REQUEST_OPEN_DIR:
					openDir(null, true);
					return;
				case PERMISSION_SDCARD_REQUEST_EXPORT_M3U8:
					exportM3U8(null, true);
					return;
				case PERMISSION_SDCARD_REQUEST_IMPORT_M3U8:
					importM3U8(null, true);
					return;
				case PERMISSION_SDCARD_REQUEST_LOAD_FILELIST:
					loadFileList(null, true);
					return;
				case PERMISSION_SDCARD_REQUEST_SAVE_FILELIST:
					saveFileList(null, true);
					return;
				case PERMISSION_MIC_REQUEST:
					addMic(true);
					return;
			}
		} else {
			switch (requestCode) {
				case PERMISSION_MIC_REQUEST:
					Toast.makeText(this, "No microphone access", Toast.LENGTH_SHORT).show();
					return;
				default:
					Toast.makeText(this, "No SDCard access", Toast.LENGTH_SHORT).show();
					return;
			}
		}
	}

	private void openMP3File(File selectedFile, boolean permissionsOk) {
		if (!permissionsOk) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				if (checkSelfPermission(PERMISSION_READ)
						== PackageManager.PERMISSION_DENIED) {
					requestPermissions(PERMISSIONS_SDCARD, PERMISSION_SDCARD_REQUEST_OPEN_MP3);
				} else {
					openMP3File(null, true);
				}
			} else {
				openMP3File(null, true);
			}
		} else if (selectedFile==null) {
			Intent intent = new Intent(this, FileChooserActivity.class);
		    intent.putExtra(FileChooserActivity.INPUT_START_FOLDER, mCurrentMusicDir.getAbsolutePath());
		    intent.putExtra(FileChooserActivity.INPUT_FOLDER_MODE, false);
		    intent.putExtra(FileChooserActivity.INPUT_REGEX_FILTER, ".*\\.mp3");
		    startActivityForResult(intent, REQUEST_OPEN_FILE);
		} else {
			Log.i(LOG_TAG_ACTIVITY, "file: " + selectedFile.getAbsolutePath());
			
			mFileList.add(selectedFile);
			//Globals.fileList.add(selectedFile);
			refreshList();
			
			mCurrentMusicDir = selectedFile.getParentFile();
			saveSettings();
		}
	}

	private void addMic(boolean permissionsOk) {
		if (!permissionsOk) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				if (checkSelfPermission(PERMISSION_MIC)
						== PackageManager.PERMISSION_DENIED) {
					requestPermissions(PERMISSIONS_MIC, PERMISSION_MIC_REQUEST);
				} else {
					addMic(true);
				}
			} else {
				addMic(true);
			}
		} else {
			mFileList.add(new File(TrackInfo.MIC_PATH));
			refreshList();
		}
	}

	private void openDir(File selectedDir, boolean permissionsOk) {
		if (!permissionsOk) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				if (checkSelfPermission(PERMISSION_READ)
						== PackageManager.PERMISSION_DENIED) {
					requestPermissions(PERMISSIONS_SDCARD, PERMISSION_SDCARD_REQUEST_OPEN_DIR);
				} else {
					openDir(null, true);
				}
			} else {
				openDir(null, true);
			}
		} else if (selectedDir==null) {
			Intent intent = new Intent(this, FileChooserActivity.class);
		    intent.putExtra(FileChooserActivity.INPUT_START_FOLDER, mCurrentMusicDir.getAbsolutePath());
		    intent.putExtra(FileChooserActivity.INPUT_FOLDER_MODE, true);
		    startActivityForResult(intent, REQUEST_OPEN_DIR);
		} else {
			Log.i(LOG_TAG_ACTIVITY, "dir: " + selectedDir.getAbsolutePath());
			
			List<File> fileList = FolderUtil.findFiles(selectedDir.getAbsolutePath(), ".*\\.mp3$");
			for (int i=0; i<fileList.size(); i++) {
				mFileList.add(fileList.get(i));
				//Globals.fileList.add(fileList.get(i));
			}
			refreshList();
			
			mCurrentMusicDir = selectedDir;
			saveSettings();
		}
	}
	
	void removeTrack(final int position) {
		/*
		 * final AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setTitle(getString(R.string.string_remove));
        dialog.setMessage(getString(R.string.question_remove_track));
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.yes), 
        		new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int buttonId) {
            	dialog.dismiss();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.no), 
        		new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int buttonId) {
            	dialog.dismiss();
            }
        });
        dialog.show();
        */
		
		mFileList.remove(position);
		//Globals.fileList.remove(position);
		if (position<mCurrentPosition) {
			mCurrentPosition -=1 ;
		} else if (position==mCurrentPosition) {
			mCurrentPosition = -1;
		}
		refreshList();
	}
	
	private void removeAllTracks() {
		final AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setTitle(getString(R.string.string_remove_all));
        dialog.setMessage(getString(R.string.question_remove_all));
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.yes), 
        		new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int buttonId) {
            	dialog.dismiss();
            	
        		mFileList.clear();
				//Globals.fileList.clear();
        		mCurrentPosition = -1;
        		refreshList();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.no), 
        		new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int buttonId) {
            	dialog.dismiss();
            }
        });
        dialog.show();
	}
	
	private void exportM3U8(File selectedFile, boolean permissionsOk) {
		if (!permissionsOk) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				if (checkSelfPermission(PERMISSION_READ)
						== PackageManager.PERMISSION_DENIED) {
					requestPermissions(PERMISSIONS_SDCARD, PERMISSION_SDCARD_REQUEST_EXPORT_M3U8);
				} else {
					exportM3U8(null, true);
				}
			} else {
				exportM3U8(null, true);
			}
		} else if (selectedFile==null) {
			Intent intent = new Intent(this, FileChooserActivity.class);
		    intent.putExtra(FileChooserActivity.INPUT_START_FOLDER, mCurrentListDir.getAbsolutePath());
		    intent.putExtra(FileChooserActivity.INPUT_FOLDER_MODE, false);
		    intent.putExtra(FileChooserActivity.INPUT_CAN_CREATE_FILES, true);
		    startActivityForResult(intent, REQUEST_EXPORT_LIST);
		} else {
			String fileName = selectedFile.getName();
			if (!fileName.endsWith(".m3u8")) {
				Toast.makeText(this, getString(R.string.toast_m3u8_ext), Toast.LENGTH_LONG).show();
				return;
			}
			
			mCurrentListDir = selectedFile.getParentFile();
			saveSettings();
			
			saveFileList(selectedFile.getAbsolutePath(), true);
		}
	}
	
	private void importM3U8(File selectedFile, boolean permissionsOk) {
		if (!permissionsOk) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				if (checkSelfPermission(PERMISSION_READ)
						== PackageManager.PERMISSION_DENIED) {
					requestPermissions(PERMISSIONS_SDCARD, PERMISSION_SDCARD_REQUEST_IMPORT_M3U8);
				} else {
					importM3U8(null, true);
				}
			} else {
				importM3U8(null, true);
			}
		} else if (selectedFile==null) {
			Intent intent = new Intent(this, FileChooserActivity.class);
		    intent.putExtra(FileChooserActivity.INPUT_START_FOLDER, mCurrentListDir.getAbsolutePath());
		    intent.putExtra(FileChooserActivity.INPUT_FOLDER_MODE, false);
		    intent.putExtra(FileChooserActivity.INPUT_REGEX_FILTER, ".*\\.m3u8");
		    startActivityForResult(intent, REQUEST_IMPORT_LIST);
		} else {
			mCurrentListDir = selectedFile.getParentFile();
			saveSettings();
			
			loadFileList(selectedFile.getAbsolutePath(), true);
			refreshList();
		}
	}
	
	private void loadFileList(String path, boolean permissionsOk) {
		if (!permissionsOk) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				if (checkSelfPermission(PERMISSION_READ)
						== PackageManager.PERMISSION_DENIED) {
					requestPermissions(PERMISSIONS_SDCARD, PERMISSION_SDCARD_REQUEST_LOAD_FILELIST);
				} else {
					loadFileList(null, true);
				}
			} else {
				loadFileList(null, true);
			}
		} else {
			mFileList = new ArrayList<File>();
			//Globals.fileList = new ArrayList<File>();

			File f;
			if (path == null) {
				File appDir = getExternalFilesDir(null);
				if (appDir == null) {
					appDir = getFilesDir();
				}
				if (appDir == null) {
					appDir = Environment.getExternalStorageDirectory();
				}
				if (appDir == null) {
					return;
				}

				path = appDir.getAbsolutePath() + File.separator + "current.m3u8";
				f = new File(path);
			} else {
				f = new File(path);
			}
			if (!f.exists()) return;

			try {
				Scanner sc = new Scanner(new FileInputStream(f));

				while (sc.hasNextLine()) {
					String trackPath = sc.nextLine();

					File track = new File(trackPath);
					if (track.exists() || track.getAbsolutePath().equals(TrackInfo.MIC_PATH)) {
						mFileList.add(track);
						//Globals.fileList.add(track);
					}
				}

				sc.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void saveFileList(String path, boolean permissionsOk) {
		if (!permissionsOk) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				if (checkSelfPermission(PERMISSION_READ)
						== PackageManager.PERMISSION_DENIED) {
					requestPermissions(PERMISSIONS_SDCARD, PERMISSION_SDCARD_REQUEST_SAVE_FILELIST);
				} else {
					saveFileList(null, true);
				}
			} else {
				saveFileList(null, true);
			}
		} else {
			File f;
			if (path == null) {
				File appDir = getExternalFilesDir(null);
				if (appDir == null) {
					appDir = getFilesDir();
				}
				if (appDir == null) {
					appDir = Environment.getExternalStorageDirectory();
				}
				if (appDir == null) {
					return;
				}

				path = appDir.getAbsolutePath() + File.separator + "current.m3u8";
				f = new File(path);
			} else {
				f = new File(path);
			}

			try {
				PrintWriter pw = new PrintWriter(new FileOutputStream(f));

				for (int i = 0; i < mFileList.size(); i++) {
					//for (int i=0; i<Globals.fileList.size(); i++) {
					pw.println(mFileList.get(i).getAbsolutePath());
					//pw.println(Globals.fileList.get(i).getAbsolutePath());
				}

				pw.flush();
				pw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void tpSetPosition(int position) {
		Intent intent = new Intent(ServiceInterface.TP_SET_POSITION);
		intent.putExtra("position", position);
		sendBroadcast(intent);
	}

	@Override
	public void tpPlay() {
		Intent intent = new Intent(ServiceInterface.TP_PLAY);
		sendBroadcast(intent);
	}

	@Override
	public void tpPause() {
		Intent intent = new Intent(ServiceInterface.TP_PAUSE);
		sendBroadcast(intent);
	}

	@Override
	public void tpSeek(double progress) {
		Intent intent = new Intent(ServiceInterface.TP_SEEK);
		intent.putExtra("progress", progress);
		sendBroadcast(intent);
	}

	@Override
	public void tpSetFileList(List<File> fileList) {
		Intent intent = new Intent(ServiceInterface.TP_SET_FILE_LIST);
		String[] filePathArray = new String[fileList.size()];
		for (int i=0; i<fileList.size(); i++) {
			filePathArray[i] = fileList.get(i).getAbsolutePath();
		}
		intent.putExtra("filePathArray", filePathArray);
		sendBroadcast(intent);
	}

	@Override
	public void tcPlay() {
		Intent intent = new Intent(ServiceInterface.TC_PLAY);
		sendBroadcast(intent);
	}

	@Override
	public void tcPause() {
		Intent intent = new Intent(ServiceInterface.TC_PAUSE);
		sendBroadcast(intent);
	}

	@Override
	public void tcSetVolume(int volume) {
		Intent intent = new Intent(ServiceInterface.TC_SET_VOLUME);
		intent.putExtra("volume", volume);
		sendBroadcast(intent);
	}

	@Override
    public void nextTrack() {
	    runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFwdButton.performClick();
            }
        });
    }
}
