package tw.plash.antrack.map;

import java.io.File;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.AntrackService;
import tw.plash.antrack.AntrackViewPager;
import tw.plash.antrack.PagerAdapter;
import tw.plash.antrack.R;
import tw.plash.antrack.R.color;
import tw.plash.antrack.R.drawable;
import tw.plash.antrack.R.id;
import tw.plash.antrack.R.layout;
import tw.plash.antrack.R.string;
import tw.plash.antrack.images.ImageMarker;
import tw.plash.antrack.util.Constants;
import tw.plash.antrack.util.IPCMessages;
import tw.plash.antrack.util.Utility;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class AntrackMapActivity extends ActionBarActivity implements TabListener {
	
	private tw.plash.antrack.AntrackViewPager myViewPager;
	private PagerAdapter pagerAdapter;
	
	private MapController mapController;
	private SharedPreferences preference;
	
	private AntrackApp app;
	
	private ImageButton fixLocationButton;
//	private ImageButton settings;
	private ImageButton camera;
	private Button controlButton;
	private ImageButton share;
	
	private Messenger mSender = null;
	private boolean mIsBound;
	private final Messenger mReceiver = new Messenger(new IncomingHandler());
	
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case IPCMessages.SYNC_CURRENT_TRAJECTORY: {
				List<Location> locations = (List<Location>) msg.obj;
				mapController.clearMap();
				mapController.drawLocations(locations);
			}
				break;
			case IPCMessages.SYNC_CURRENT_IMAGE_MARKERS: {
				List<ImageMarker> imagemarkers = (List<ImageMarker>) msg.obj;
				mapController.addImageMarkers(imagemarkers);
			}
				break;
			case IPCMessages.UPDATE_NEW_LOCATION: {
				Location location = (Location) msg.obj;
				mapController.setNewLocation(location);
				if (AntrackService.isSharingLocation()) {
					mapController.addLocation(location);
				}
			}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mSender = new Messenger(service);
			sendMessageToService(IPCMessages.REGISTER, false);
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mSender = null;
		}
	};
	
	private void sendMessageToService(int what, boolean safetyCheck) {
		if (!safetyCheck || canSendMessageNow()) {
			try {
				Message msg = Message.obtain(null, what, IPCMessages.MAP_ACTIVITY, 0);
				msg.replyTo = mReceiver;
				mSender.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean canSendMessageNow() {
		if (mIsBound) {
			if (mSender != null) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// do nothing...there are actually better ways to do this... XXX
		{
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			myViewPager.setWidth(dm.widthPixels);
			// myViewPager.setHeight(dm.heightPixels);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		app = AntrackApp.getInstance(this);
		
		preference = PreferenceManager.getDefaultSharedPreferences(AntrackMapActivity.this);
		
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		pagerAdapter = new PagerAdapter(getSupportFragmentManager());
		
		myViewPager = (AntrackViewPager) findViewById(R.id.pager);
		
		{
			// let view pager know the screen width, so it can setup bezel
			// gesture zone
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			myViewPager.setWidth(dm.widthPixels);
			// myViewPager.setHeight(dm.heightPixels);
		}
		
		myViewPager.setAdapter(pagerAdapter);
		myViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
				if (position == 1) {
					myViewPager.setPagingEnabled(false);
				} else {
					myViewPager.setPagingEnabled(true);
				}
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}
			
			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});
		
		if(mapController == null){
			setupGooglemap();
		}
		
		for (int i = 0; i < pagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab().setText(pagerAdapter.getPageTitle(i)).setTabListener(this));
		}
		
		actionBar.setSelectedNavigationItem(1);
		
		setupButtons();
	}
	
	private void setupGooglemap() {
		switch (GooglePlayServicesUtil.isGooglePlayServicesAvailable(AntrackMapActivity.this)) {
		case ConnectionResult.SUCCESS:
			AntrackSupportMapFragment asmf = ((AntrackSupportMapFragment) getSupportFragmentManager().findFragmentById(
					R.id.map));
			mapController = new MapController();
			asmf.setCallback(mapController);
			mapController.setMap(asmf.getMap());
			break;
		default:
			break;
		}
	}
	
	private void setupButtons() {
		controlButton = (Button) findViewById(R.id.controlbutton);
		if (AntrackService.isSharingLocation()) {
			controlButton.setText(R.string.control_button_stop);
			controlButton.setBackgroundResource(R.color.action_button_state_on);
		} else {
			controlButton.setText(R.string.control_button_start);
			controlButton.setBackgroundResource(R.color.action_button_state);
		}
		controlButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AntrackService.isSharingLocation()) {
					// stop sharing
					showConfirmStopSharingDialog();
				} else {
					// start sharing
					setupStartSharingConnection();
				}
			}
		});
		
		fixLocationButton = (ImageButton) findViewById(R.id.fixlocation);
		fixLocationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					mapController.centerAtMyLocation();
				} catch(NullPointerException e){
					Toast.makeText(AntrackMapActivity.this, "Waiting for location", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
//		settings = (ImageButton) findViewById(R.id.settings);
//		settings.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Toast.makeText(AntrackMapActivity.this, "Settings not available...", Toast.LENGTH_SHORT).show();
//			}
//		});
		
		share = (ImageButton) findViewById(R.id.share);
		if(AntrackService.isSharingLocation()){
			share.setEnabled(true);
			share.setBackgroundResource(R.color.action_button_state);
		} else{
			share.setEnabled(false);
			share.setBackgroundResource(R.drawable.camerabuttoninactivebg);
		}
		share.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showNotFirstTimeSharingDialog();
			}
		});
		
		camera = (ImageButton) findViewById(R.id.camera);
		if(AntrackService.isSharingLocation()){
			camera.setEnabled(true);
			camera.setBackgroundResource(R.color.action_button_state);
		} else{
			camera.setEnabled(false);
			camera.setBackgroundResource(R.drawable.camerabuttoninactivebg);
		}
		camera.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e("tw.map", "camera click");
				String imagename = String.format("%1$d.jpg", System.currentTimeMillis());
				File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AnTrack/" + preference.getString("token", ""));
				dir.mkdirs();
				if (dir.exists() && dir.isDirectory()) {
					File imagefile = new File(dir, imagename);
					if(mapController.hasLocation()){
						//use local broadcast to send file path to service
						int requestCode = generateNewRequestCode();
						String fullpath = imagefile.getAbsolutePath();
						notifyNewImageCreation(fullpath, requestCode);
						
						Uri imageUri = Uri.fromFile(imagefile);
						// intent to launch Android camera app to take pictures
						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						// input the desired filepath + filename
						intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
						startActivityForResult(intent, requestCode);
					} else{
						Toast.makeText(AntrackMapActivity.this, "Waiting for location", Toast.LENGTH_SHORT).show();
					}
				} else {
					Toast.makeText(AntrackMapActivity.this, "Cannot take picture", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	private int generateNewRequestCode(){
		long seed = System.currentTimeMillis();
		int code = (int) (seed % Constants.MAX_INT);
		Log.e("tw.map", "new reuqest code= " + code);
		return code;
	}
	
	private void notifyNewImageCreation(String path, int code){
		Intent intent = new Intent(IPCMessages.LB_IMAGE_CREATE);
		intent.putExtra(IPCMessages.LB_EXTRA_IMAGE_PATH, path);
		intent.putExtra(IPCMessages.LB_EXTRA_REQUEST_CODE, code);
		LocalBroadcastManager.getInstance(AntrackMapActivity.this).sendBroadcast(intent);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.e("tw.map", "onactivity result, request code= " + requestCode);
		switch(resultCode){
		case RESULT_OK:
			notifyNewImageConfirmation(requestCode);
			Toast.makeText(AntrackMapActivity.this, "New picture added", Toast.LENGTH_SHORT).show();
			break;
		case RESULT_CANCELED:
		case RESULT_FIRST_USER:
		default:
			notifyNewImageCancellation(requestCode);
			Toast.makeText(AntrackMapActivity.this, "No picture taken", Toast.LENGTH_SHORT).show();
			break;
		}
	}
	
	private void notifyNewImageConfirmation(int code){
		Intent intent = new Intent(IPCMessages.LB_IMAGE_CONFIRM);
		intent.putExtra(IPCMessages.LB_EXTRA_REQUEST_CODE, code);
		LocalBroadcastManager.getInstance(AntrackMapActivity.this).sendBroadcast(intent);
	}
	
	private void notifyNewImageCancellation(int code){
		Intent intent = new Intent(IPCMessages.LB_IMAGE_CANCEL);
		intent.putExtra(IPCMessages.LB_EXTRA_REQUEST_CODE, code);
		LocalBroadcastManager.getInstance(AntrackMapActivity.this).sendBroadcast(intent);
	}
	
	private void showConfirmStopSharingDialog() {
		new AlertDialog.Builder(AntrackMapActivity.this)
			.setMessage("Are you sure you want to stop sharing?")
			.setPositiveButton("yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					prepareToStopSharing();
				}
			})
			.setNegativeButton("no", null).show();
	}
	
	private void prepareToStopSharing() {
		mapController.drawEndMarker();
		sendLocalBroadcast(IPCMessages.LB_STOP_SHARING);
		stopService(new Intent(AntrackMapActivity.this, AntrackService.class));
		Toast.makeText(AntrackMapActivity.this, "Stop sharing", Toast.LENGTH_SHORT).show();
		prepareButtonsToStop();
		app.getStatsUpdater().stopSharing();
		executeStopSharingConnection();
	}
	
	private void prepareButtonsToStop(){
		controlButton.setText(R.string.control_button_start);
		controlButton.setBackgroundResource(R.color.action_button_state);
		camera.setEnabled(false);
		camera.setBackgroundResource(R.drawable.camerabuttoninactivebg);
		share.setEnabled(false);
		share.setBackgroundResource(R.drawable.camerabuttoninactivebg);
	}
	
	
	private void executeStopSharingConnection() {
		String token = preference.getString("token", null);
		String timestamp = new Timestamp(new Date().getTime()).toString();
		app.getApi().stop(token, timestamp, new Listener<JSONObject>() {
				@Override
				public void onResponse(JSONObject arg0) {
				}
			}, new ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError arg0) {
				}
			}
		);
	}
	
	private void sendLocalBroadcast(String action) {
		Intent intent = new Intent(action);
		LocalBroadcastManager.getInstance(AntrackMapActivity.this).sendBroadcast(intent);
	}
	
	private void setupStartSharingConnection() {
		String uuid = preference.getString("uuid", null);
		String username = preference.getString("name", null);
		String timestamp = new Timestamp(new Date().getTime()).toString();
		if (uuid == null || username == null) {
			Toast.makeText(AntrackMapActivity.this, "what", Toast.LENGTH_SHORT).show();
		} else {
			final ProgressDialog diag = new ProgressDialog(AntrackMapActivity.this);
			diag.setMessage("Connecting to AnTrack Service...");
			diag.setIndeterminate(true);
			diag.setCancelable(false);
			diag.setCanceledOnTouchOutside(false);
			diag.show();
			app.getApi()
				.initialize(uuid, username, timestamp, new Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject obj) {
						// parse the jsonobject returned from server
						// init share action intent
						Log.e("tw.activity", "init res= " + obj.toString());
						diag.dismiss();
						try {
							if (obj.getInt("status_code") == 200) {
								String token = obj.getString("token");
								String url = obj.getString("url");
								prepareToStartSharing(token, url);
							} else {
								showErrorMessage();
							}
						} catch (JSONException e) {
							e.printStackTrace();
							showErrorMessage();
						}
					}
				}, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						diag.dismiss();
						// let user know there is a error, try again later
						Utility.log(arg0.toString());
						showErrorMessage();
					}
				}
			);
		}
	}
	
	private void prepareToStartSharing(String token, String url) {
		app.getStatsUpdater().startSharing(System.currentTimeMillis(), SystemClock.elapsedRealtime());
		app.setFollowers(0);
		Log.e("tw.activity", "new token= " + token);
		preference.edit().putString("token", token).putString("url", url).commit();
		mapController.clearMap();
		prepareButtonsToStart();
		Toast.makeText(AntrackMapActivity.this, "Start sharing", Toast.LENGTH_SHORT).show();
		startService();
		// use local broadcast to start sharing instead
		sendLocalBroadcast(IPCMessages.LB_START_SHARING);
		
		showFirstTimeSharingDialog();
	}
	
	private void prepareButtonsToStart(){
		controlButton.setText(R.string.control_button_stop);
		controlButton.setBackgroundResource(R.color.action_button_state_on);
		camera.setEnabled(true);
		camera.setBackgroundResource(R.color.action_button_state);
		share.setEnabled(true);
		share.setBackgroundResource(R.color.action_button_state);
	}
	
	private void showFirstTimeSharingDialog(){
		showSharingSelector("Share via...");
	}
	
	private void showNotFirstTimeSharingDialog(){
		showSharingSelector("Share again via...");
	}
	
	private void showSharingSelector(String title){
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Hey! it's me!");
		sendIntent.putExtra(Intent.EXTRA_TEXT,
		"Click on the link to follow my lead " + preference.getString("url", ""));
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendIntent, title));
	}
	
	private void showErrorMessage() {
		new AlertDialog.Builder(AntrackMapActivity.this).setTitle("Error")
				.setMessage("Cannot connect to AnTrack service, check your internet settings and try again later")
				.setCancelable(true).setNeutralButton("Okay", null).show();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.e("tw.map", "onResume");
		// retrieve shared preference
		getPreferences();
		bindToService();
	}
	
	private void getPreferences() {
		mapController.setFixToLocation(preference.getBoolean(Constants.PREF_FIXTOLOCATION, true));
		mapController.setZoomLevel(preference.getFloat(Constants.PREF_LASTZOOMLEVEL, -1f));
	}
	
	private void startService() {
		startService(new Intent(AntrackMapActivity.this, AntrackService.class));
	}
	
	void bindToService() {
		// bind only when necessary
		if (!mIsBound) {
			bindService(new Intent(AntrackMapActivity.this, AntrackService.class), mConnection,
					Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.e("tw.map", "onPause");
		unbindFromService();
		// save shared preference
		savePreferences();
		// cancal all pending or ongoing connections if not sharing
		if (!AntrackService.isSharingLocation()) {
			app.cancelAll();
		}
	}
	
	void unbindFromService() {
		if (mIsBound) {
			sendMessageToService(IPCMessages.DEREGISTER, true);
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	
	private void savePreferences() {
		preference.edit()
		.putBoolean(Constants.PREF_FIXTOLOCATION, mapController.getFixToLocation())
		.putFloat(Constants.PREF_LASTZOOMLEVEL, mapController.getZoomLevel())
		.commit();
	}
	
	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
	}
	
	@Override
	public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
		myViewPager.setCurrentItem(arg0.getPosition());
	}
	
	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
	}
}