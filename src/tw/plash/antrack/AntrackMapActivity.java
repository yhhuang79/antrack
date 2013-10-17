package tw.plash.antrack;

import java.io.File;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.SupportMapFragment;

public class AntrackMapActivity extends ActionBarActivity implements TabListener, TouchableWrapperCallback {
	
	private tw.plash.antrack.AntrackViewPager myViewPager;
	private PagerAdapter pagerAdapter;
	
	private GoogleMap googlemap;
	private MapController mapController;
	private boolean fixToLocation;
	private SharedPreferences preference;
	
	private ImageButton fixLocationButton;
	private ImageButton settings;
	private ImageButton camera;
	private Button controlButton;
	
	private OnLocationChangedListener onLocationChangedListener;
	
	private Messenger mSender = null;
	private boolean mIsBound;
	private final Messenger mReceiver = new Messenger(new IncomingHandler());
	
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case IPCMessages.SYNC_CURRENT_TRAJECTORY: {
				List<Location> locations = (List<Location>) msg.obj;
				if (!locations.isEmpty()) {
					clearMapAndDrawSyncedTrajectory(locations);
				}
			}
				break;
			case IPCMessages.UPDATE_NEW_LOCATION: {
				Location location = (Location) msg.obj;
				onLocationChangedListener.onLocationChanged(location);
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
	
	private void clearMapAndDrawSyncedTrajectory(List<Location> locations) {
		mapController.clearMap();
		mapController.drawLocations(locations);
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
		
		preference = PreferenceManager.getDefaultSharedPreferences(AntrackMapActivity.this);
		getPreferences();
		
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
		
		// avoid setting up google map object more than once
		if (googlemap == null) {
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
			asmf.setCallback(AntrackMapActivity.this);
			googlemap = asmf.getMap();
			googlemap.setMyLocationEnabled(true);
			googlemap.getUiSettings().setMyLocationButtonEnabled(false);
			googlemap.getUiSettings().setZoomGesturesEnabled(true);
			googlemap.getUiSettings().setZoomControlsEnabled(false);
			googlemap.setIndoorEnabled(true);
			googlemap.setLocationSource(new LocationSource() {
				@Override
				public void activate(OnLocationChangedListener listener) {
					onLocationChangedListener = listener;
				}
				
				@Override
				public void deactivate() {
					onLocationChangedListener = null;
				}
			});
			googlemap.setOnMyLocationChangeListener(new OnMyLocationChangeListener() {
				@Override
				public void onMyLocationChange(Location location) {
					if (fixToLocation) {
						mapController.moveToLocation(location);
					}
				}
			});
			
			// XXX setup map drawer
			mapController = new MapController(googlemap);
			break;
		default:
			// XXX well...no map, no sharing
			break;
		}
	}
	
	private void setupButtons() {
		controlButton = (Button) findViewById(R.id.controlbutton);
		if (AntrackService.isSharingLocation()) {
			controlButton.setText(R.string.control_button_stop);
		} else {
			controlButton.setText(R.string.control_button_start);
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
				fixToLocation = true;
				mapController.centerAtMyLocation();
			}
		});
		
		settings = (ImageButton) findViewById(R.id.settings);
		settings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(AntrackMapActivity.this, "SETTINGS", Toast.LENGTH_SHORT).show();
			}
		});
		
		camera = (ImageButton) findViewById(R.id.camera);
		camera.setEnabled(AntrackService.isSharingLocation());
//		camera.setEnabled(true);
		camera.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String imagename = String.format("%1$d.jpg", System.currentTimeMillis());
				Utility.log("camera", "imagename=" + imagename);
				File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AnTrack/" + preference.getString("token", ""));
				dir.mkdirs();
				Utility.log("camera", "path=" + dir.getAbsolutePath());
				if (dir.exists() && dir.isDirectory()) {
					File imagefile = new File(dir, imagename);
					Uri imageUri = Uri.fromFile(imagefile);
					// intent to launch Android camera app to take pictures
					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					// input the desired filepath + filename
					intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
					// launch the intent with code
					// startActivityForResult(intent,
					// REQUEST_CODE_TAKE_PICTURE);
					startActivityForResult(intent, 12345);
				} else {
					Toast.makeText(AntrackMapActivity.this, "cannot take picture", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Utility.log("map", "onactivityresult");
		switch(requestCode){
		case 12345:
			Utility.log("map", "request code is 12345");
			switch(resultCode){
			case RESULT_OK:
				Utility.log("map", "RESULT_OK");
				
				break;
			case RESULT_FIRST_USER:
			case RESULT_CANCELED:
				Utility.log("map", "RESULT_CANCELED");
			default:
				Utility.log("map", "default");
				
				break;
			}
			break;
		}
	}
	
	private void showConfirmStopSharingDialog() {
		new AlertDialog.Builder(AntrackMapActivity.this).setMessage("are you sure you want to stop sharing?")
				.setPositiveButton("yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						prepareToStopSharing();
					}
				}).setNegativeButton("no", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Toast.makeText(AntrackMapActivity.this, "sharing CONTINUES", Toast.LENGTH_SHORT).show();
					}
				}).show();
	}
	
	private void prepareToStopSharing() {
		mapController.drawEndMarker();
		// sendMessageToService(IPCMessages.STOP_SHARING, true);
		localbroadcast(IPCMessages.LOCALBROADCAST_STOP_SHARING);
		stopService(new Intent(AntrackMapActivity.this, AntrackService.class));
		Toast.makeText(AntrackMapActivity.this, "STOPPED sharing", Toast.LENGTH_SHORT).show();
		controlButton.setText(R.string.control_button_start);
		camera.setEnabled(false);
		// executeStopSharingConnection();
	}
	
	private void executeStopSharingConnection() {
		String token = preference.getString("token", null);
		String timestamp = new Timestamp(new Date().getTime()).toString();
		// doesn't care about the server response...thus the null response and
		// error listener
		AntrackApp.getInstance(AntrackMapActivity.this).getApi().stop(token, timestamp, null, null);
	}
	
	private void localbroadcast(String action) {
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
			AntrackApp.getInstance(AntrackMapActivity.this).getApi()
					.initialize(uuid, username, timestamp, new Listener<JSONObject>() {
						@Override
						public void onResponse(JSONObject obj) {
							// parse the jsonobject returned from server
							// init share action intent
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
							// let user know there is a error, try again later
							Utility.log(arg0.toString());
							showErrorMessage();
						}
					});
		}
	}
	
	private void prepareToStartSharing(String token, String url) {
		preference.edit().putString("token", token).putString("url", url).commit();
		mapController.clearMap();
		controlButton.setText(R.string.control_button_stop);
		camera.setEnabled(true);
		Toast.makeText(AntrackMapActivity.this, "START sharing", Toast.LENGTH_SHORT).show();
		startService();
		// use local broadcast to start sharing instead
		localbroadcast(IPCMessages.LOCALBROADCAST_START_SHARING);
	}
	
	private void showErrorMessage() {
		new AlertDialog.Builder(AntrackMapActivity.this).setTitle("Error")
				.setMessage("Cannot connect to AnTrack service, check your internet settings and try again later")
				.setCancelable(true).setNeutralButton("Okay", null).show();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// retrieve shared preference
		getPreferences();
		bindToService();
	}
	
	private void getPreferences() {
		fixToLocation = preference.getBoolean(Constants.PREF_FIXTOLOCATION, true);
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
		unbindFromService();
		// save shared preference
		savePreferences();
		// cancal all pending or ongoing connections if not sharing
		if (!AntrackService.isSharingLocation()) {
			AntrackApp.getInstance(AntrackMapActivity.this).cancelAll(AntrackMapActivity.this);
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
		preference.edit().putBoolean(Constants.PREF_FIXTOLOCATION, fixToLocation).commit();
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
	
	@Override
	public void setIsTouched() {
		fixToLocation = false;
	}
}