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
package jatx.musicreceiver.android;

import java.util.ArrayList;
import java.util.List;

import jatx.musicreceiver.commons.Globals;
import jatx.musicreceiver.interfaces.ServiceController;
import jatx.debug.Debug;
import jatx.musicreceiver.interfaces.UIController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class MusicReceiverActivity extends AppCompatActivity implements UIController {
	final static String PREFS_NAME = "MusicReceiverPrefsFile";

	final static String DELIM = "#13731#";
	
	public static final String LOG_TAG_ACTIVITY = "receiver main activity";

	private boolean isRunning;

	//public boolean autoConnect;
	public String host;
	public List<String> allHosts;
	int hostIndex;
	
	private EditText hostField;
	private Button mToogleButton;
	private CheckBox mAutoCheckBox;

	private int hoursFromInstall;
	private boolean reviewOfferWasShown;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_receiver);
		
		Debug.setCustomExceptionHandler(getExternalFilesDir(null));
				
		isRunning = false;
		
		hostField = (EditText) findViewById(R.id.hostname);
		mToogleButton = (Button)findViewById(R.id.toogle);
		mAutoCheckBox = (CheckBox)findViewById(R.id.auto_connect);
		
		mToogleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isRunning) {
					uiStartJob();
					serviceStartJob();
				} else {
					uiStopJob();
					serviceStopJob();
				}
			}
		});
		
		mAutoCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Globals.isAutoConnect = mAutoCheckBox.isChecked();
				saveSettings();
			}
		});
		
		//prepareAndStart();
		
		SelectHostDialog dialog = SelectHostDialog.newInstance(this);
		dialog.show(getSupportFragmentManager(), "select-host-dialog");
	}
	
	@Override
	protected void onDestroy() {
		Log.i(LOG_TAG_ACTIVITY, "on destroy");

		Intent intent = new Intent(MusicReceiverService.STOP_SERVIVE);
		sendBroadcast(intent);


		try {
			final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			audioManager.setMode(AudioManager.MODE_NORMAL);
			audioManager.stopBluetoothSco();
			audioManager.setBluetoothScoOn(false);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		super.onDestroy();
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
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_music_receiver, menu);
	    return super.onCreateOptionsMenu(menu);
	} 
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		
		switch (item.getItemId()) {
	        case R.id.item_review_app:
	        	try {
			    	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("market://details?id=jatx.musicreceiver.android")));
				} catch (android.content.ActivityNotFoundException anfe) {
			    	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("https://play.google.com/store/apps/details?id=jatx.musicreceiver.android")));
				}
	        	return true;
	        
	        case R.id.item_transmitter_android:
	        	try {
			    	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("market://details?id=jatx.musictransmitter.android")));
				} catch (android.content.ActivityNotFoundException anfe) {
			    	startActivity(new Intent(Intent.ACTION_VIEW,
			    			Uri.parse("https://play.google.com/store/apps/details?id=jatx.musictransmitter.android")));
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
			    		Uri.parse("https://github.com/tabatsky/jatx/tree/master/JatxMusic")));
	        	return true;
	        	
	        case R.id.item_dev_site:
	        	startActivity(new Intent(Intent.ACTION_VIEW, 
			    		Uri.parse("http://tabatsky.ru")));
	        	return true;
	        
	        default:
	        	return false;
	    }
	}

	@Override
	public void uiStartJob() {
		if (isRunning) return;
		isRunning = true;
		Log.i(LOG_TAG_ACTIVITY, "start job");
		mToogleButton.setText(getString(R.string.string_stop));
		host = hostField.getText().toString();
		saveSettings();
		//rp = new ReceiverPlayer(host, self, new AndroidSoundOut());
		//rc = new ReceiverController(host, self);
		//rp.start();
		//rc.start();
	}

	@Override
	public void uiStopJob() {
		if (!isRunning) return;
		isRunning = false;
		//rp.setFinishFlag();
		//rc.setFinishFlag();
		mToogleButton.setText(getString(R.string.string_start));
		Log.i(LOG_TAG_ACTIVITY, "stop job");
		Toast.makeText(MusicReceiverActivity.this, getString(R.string.toast_disconnect), Toast.LENGTH_SHORT).show();
	}
	
	public void prepareAndStart() {
		BroadcastReceiver uiStartJobReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				uiStartJob();
			}
		};
		registerReceiver(uiStartJobReceiver, new IntentFilter(UIController.START_JOB));

		BroadcastReceiver uiStopJobReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				uiStopJob();
			}
		};
		registerReceiver(uiStopJobReceiver,  new IntentFilter(UIController.STOP_JOB));

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
								Uri.parse("market://details?id=jatx.musicreceiver.android")));
					} catch (android.content.ActivityNotFoundException anfe) {
						startActivity(new Intent(Intent.ACTION_VIEW,
								Uri.parse("https://play.google.com/store/apps/details?id=jatx.musicreceiver.android")));
					}
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
		}

		hostField.setText(host);
		mAutoCheckBox.setChecked(Globals.isAutoConnect);

		BroadcastReceiver statusResponseReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				host = intent.getStringExtra("host");
				isRunning = intent.getBooleanExtra("isRunning", false);
				if (host != null) hostField.setText(host);
				mToogleButton.setText(getString(isRunning ? R.string.string_stop: R.string.string_start));
			}
		};
		registerReceiver(statusResponseReceiver, new IntentFilter(MusicReceiverService.STATUS_RESPONSE));

		if (!MusicReceiverService.isInstanceRunning) {
			Intent intent = new Intent(this, MusicReceiverService.class);
			intent.putExtra("host", host);
			startService(intent);
		} else {
			Intent intent = new Intent(MusicReceiverService.STATUS_REQUEST);
			sendBroadcast(intent);
		}

		//act = new AutoConnectThread(this);
		//act.start();

		final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		final CheckBox bluetoothCheckBox = (CheckBox) findViewById(R.id.bluetooth);
		bluetoothCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (bluetoothCheckBox.isChecked()) {
					try {
                        audioManager.setBluetoothScoOn(true);
						audioManager.startBluetoothSco();
					} catch (Exception e) {
						Toast.makeText(MusicReceiverActivity.this,
								getString(R.string.toast_bluetooth_error),
								Toast.LENGTH_SHORT).show();
						bluetoothCheckBox.setChecked(false);
					}
				} else {
					try {
						audioManager.setMode(AudioManager.MODE_NORMAL);
						audioManager.stopBluetoothSco();
						audioManager.setBluetoothScoOn(false);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}
		});

		BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
			int prevState = -223355;

			@Override
			public void onReceive(Context context, Intent intent) {
				int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
				if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
					audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
					Log.e(LOG_TAG_ACTIVITY, "mode in call");
				} else {
					audioManager.setMode(AudioManager.MODE_NORMAL);
					//audioManager.stopBluetoothSco();
					//audioManager.setBluetoothScoOn(false);
					Log.e(LOG_TAG_ACTIVITY, "mode normal");
				}
				/*
				if (mToogleButton.getText().equals(getString(R.string.string_stop)) && state!=prevState) {
					mToogleButton.performClick();
					prevState = state;
					Log.e("state changed", "" + state);
				}
				*/
			}
		};
		if (Build.VERSION.SDK_INT < 14) {
			registerReceiver(bluetoothReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED));
		} else {
			registerReceiver(bluetoothReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
		}
	}
	
	public void loadSettings() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	
		String allHostsStr = settings.getString("all_IP", "New Transmitter" + DELIM + "127.0.0.1");
		String[] allHostsArr = allHostsStr.split(DELIM);
		
		allHosts = new ArrayList<String>();
		for (int i=0; i<allHostsArr.length; i++) {
			allHosts.add(allHostsArr[i]);
		}
		
		host = settings.getString("IP", "127.0.0.1");
		
		hostIndex = allHosts.indexOf(host);
		
		Log.i("all hosts", allHostsStr);
		Log.i("host index", Integer.toString(hostIndex));
		
		Globals.isAutoConnect = settings.getBoolean("autoConnect", false);

		reviewOfferWasShown = settings.getBoolean("reviewOfferWasShown", false);
		long installTime = settings.getLong("installTime", System.currentTimeMillis());
		{
			SharedPreferences.Editor editor = settings.edit();
			editor.putLong("installTime", installTime);
			editor.commit();
		}
		hoursFromInstall = (int)((System.currentTimeMillis() - installTime)/(3600*1000));
	}
	
	public void saveSettings() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		
		editor.putBoolean("autoConnect", Globals.isAutoConnect);
		editor.putString("IP", host);
		
		StringBuilder allHostsBuilder = new StringBuilder();
		allHostsBuilder.append(allHosts.get(0));
		for (int i=1; i<allHosts.size(); i++) {
			allHostsBuilder.append(DELIM);
			allHostsBuilder.append(allHosts.get(i));
		}
		editor.putString("all_IP", allHostsBuilder.toString());

		editor.putBoolean("reviewOfferWasShown", reviewOfferWasShown);
		
		editor.commit();
	}

	private void serviceStartJob() {
		Intent intent = new Intent(ServiceController.START_JOB);
		//intent.putExtra("host", host);
		sendBroadcast(intent);
	}

	private void serviceStopJob() {
		Intent intent = new Intent(ServiceController.STOP_JOB);
		sendBroadcast(intent);
	}

	/*
	@Override
	public boolean isAutoConnect() {
		return autoConnect;
	}
	*/
}
