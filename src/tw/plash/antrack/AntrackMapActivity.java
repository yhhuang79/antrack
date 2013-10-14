package tw.plash.antrack;

import java.util.List;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class AntrackMapActivity extends ActionBarActivity implements TabListener, FixLocationCallback {
	
	private final String simpleName = "AntrackMapActivity";
	
	private tw.plash.antrack.AntrackViewPager myViewPager;
	private PagerAdapter pagerAdapter;
	
	private GoogleMap googlemap;
	private MapController mapController;
	private boolean fixToLocation;
	private SharedPreferences preference;
	
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
			Utility.log(simpleName, "serviceocnnection: binded");
			mSender = new Messenger(service);
			Utility.log(simpleName, "serviceocnnection: now registering");
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
		
		if (googlemap == null) {
			setupGooglemap();
		}
		
		for (int i = 0; i < pagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab().setText(pagerAdapter.getPageTitle(i)).setTabListener(this));
		}
		
		actionBar.setSelectedNavigationItem(1);
		
		// myViewPager.setCurrentItem(1); // XXX
		
		setupControlButton();
		
	}
	
	private void setupGooglemap() {
		switch (GooglePlayServicesUtil.isGooglePlayServicesAvailable(AntrackMapActivity.this)) {
		case ConnectionResult.SUCCESS:
			googlemap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
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
					if(fixToLocation){
						mapController.moveToLocation(location);
					}
				}
			});
			// XXX setup map drawer
			mapController = new MapController(googlemap);
			break;
		default:
			//XXX well...no map, no sharing
			break;
		}
	}
	
	private void setupControlButton() {
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
					//stop sharing
					showConfirmStopSharingDialog();
				} else {
					//start sharing
					prepareToStartSharing();
				}
			}
		});
	}
	
	private void showConfirmStopSharingDialog() {
		new AlertDialog.Builder(AntrackMapActivity.this)
			.setMessage("are you sure you want to stop sharing?")
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
		sendMessageToService(IPCMessages.STOP_SHARING, true);
		stopService(new Intent(AntrackMapActivity.this, AntrackService.class));
		Toast.makeText(AntrackMapActivity.this, "STOPPED sharing", Toast.LENGTH_SHORT).show();
		controlButton.setText(R.string.control_button_start);
//		executeStopSharingConnection();
	}
	
	private void executeStopSharingConnection() {
		// new StopConnection(MapActivity.this, preference).execute();
		AntrackApp.getInstance(AntrackMapActivity.this).getApi().stop("", "", null, null);
	}
	
	private void prepareToStartSharing() {
		mapController.clearMap();
		controlButton.setText(R.string.control_button_stop);
		Toast.makeText(AntrackMapActivity.this, "START sharing", Toast.LENGTH_SHORT).show();
		startService(new Intent(AntrackMapActivity.this, AntrackService.class));
		sendMessageToService(IPCMessages.START_SHARING, true);
//		setupStartSharingConnection();
	}
	
	private void setupStartSharingConnection() {
		// new InitializeConnection(Map.this, preference).execute();
		AntrackApp.getInstance(AntrackMapActivity.this).getApi().initialize("", "", "", 
			new Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject obj) {
				//parse the jsonobject returned from server
				//init share action intent
			}
		}, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				//let user know there is a error, try again later
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Utility.log(simpleName, "onresume");
		//retrieve shared preference
		getPreferences();
//		startService();
//		checkIfServiceIsRunning();
		bindToService();
	}
	
	private void getPreferences(){
		fixToLocation = preference.getBoolean(Constants.PREF_FIXTOLOCATION, true);
	}
	
	private void startService() {
		Utility.log(simpleName, "onresume: startservice");
		startService(new Intent(AntrackMapActivity.this, AntrackService.class));
	}
	
	void bindToService() {
		// bind only when necessary
		Utility.log(simpleName, "bindToService: check if necessary");
		if(!mIsBound){
			Utility.log(simpleName, "bindToService: actual binding");
			bindService(new Intent(AntrackMapActivity.this, AntrackService.class), mConnection, Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Utility.log(simpleName, "onPause");
		unbindFromService();
		//save shared preference
		savePreferences();
	}
	
	void unbindFromService() {
		Utility.log(simpleName, "unbindFromService");
		if (mIsBound) {
			Utility.log(simpleName, "unbindFromService: deregister");
			sendMessageToService(IPCMessages.DEREGISTER, true);
			// Detach our existing connection.
			Utility.log(simpleName, "unbindFromService: unbind");
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	
	private void savePreferences(){
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
	public void enableFix() {
		// TODO Auto-generated method stub
		fixToLocation = true;
	}

	@Override
	public void disableFix() {
		// TODO Auto-generated method stub
		fixToLocation = false;
	}
}