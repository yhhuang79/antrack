package tw.plash.antrack;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class AntrackService extends Service implements LocationListener, ConnectionCallbacks,
		OnConnectionFailedListener {
	
	private final String simpleName = "AntrackService";
	
	private static boolean serviceIsSharing = false;
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	private SharedPreferences preference;
	private Location previousLocation;
	
	private TripStatictics stats;
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(IPCMessages.LOCALBROADCAST_START_SHARING)){
				prepareToStartSharing();
			} else if(action.equals(IPCMessages.LOCALBROADCAST_STOP_SHARING)){
				prepareToStopSharing();
			}
		}
	};
	
	private Messenger mLocationSender;
	final Messenger mReceiver = new Messenger(new IncomingHandler());
	
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case IPCMessages.REGISTER:
				switch(msg.arg1){
				case IPCMessages.MAP_ACTIVITY:
					mLocationSender = msg.replyTo;
					if (isSharingLocation()) {
						syncStuff();
					}
					break;
				}
				break;
			case IPCMessages.DEREGISTER:
				if (msg.replyTo == mLocationSender) {
					mLocationSender = null;
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private void syncStuff(){
		List<Location> locations = AntrackApp.getInstance(getApplicationContext()).getDbhelper().getAllDisplayableLocations();
		sendTrajectoryMessage(locations);
		List<ImageMarker> imagemarkers = AntrackApp.getInstance(getApplicationContext()).getDbhelper().getImageMarkers();
		sendImageMarkerMessage(imagemarkers);
		if(!locations.isEmpty()){
			Location latestloLocation = locations.get(locations.size() - 1);
			sendLocationMessage(latestloLocation);
		}
	}
	
	private void prepareToStartSharing() {
		resetVariables();
		showNotification();
		serviceIsSharing = true;
	}
	
	private void resetVariables() {
		AntrackApp.getInstance(getApplicationContext()).getDbhelper().removeAll();
		stats.resetStats();
		previousLocation = null;
	}
	
	private void showNotification() {
		Notification notification = new Notification(R.drawable.ic_launcher, "sharing has started",
				System.currentTimeMillis());
		PendingIntent pendingIntent = PendingIntent.getActivity(AntrackService.this, 0, new Intent(AntrackService.this,
				AntrackMapActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(AntrackService.this, "AnTrack", "sharing...", pendingIntent);
		startForeground(1337, notification);
	}
	
	private void prepareToStopSharing() {
		serviceIsSharing = false;
		AntrackApp.getInstance(AntrackService.this).getStatsUpdater().updateStats(stats);
		stopForeground(true);
	}
	
	synchronized private void sendLocationMessage(Location location){
		sendMessageToMapActivity(IPCMessages.UPDATE_NEW_LOCATION, location);
	}
	
	synchronized private void sendTrajectoryMessage(List<Location> locations){
		sendMessageToMapActivity(IPCMessages.SYNC_CURRENT_TRAJECTORY, locations);
	}
	
	synchronized private void sendImageMarkerMessage(List<ImageMarker> imagemarkers){
		sendMessageToMapActivity(IPCMessages.SYNC_CURRENT_IMAGE_MARKERS, imagemarkers);
	}
	
	synchronized private void sendMessageToMapActivity(int what, Object data) {
		if (mLocationSender != null) {
			try {
				Message msg = Message.obtain(null, what, data);
				mLocationSender.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		setupLocationRequest();
		setupLocationClient();
		
		serviceIsSharing = false;
		
		preference = PreferenceManager.getDefaultSharedPreferences(AntrackService.this);
		
		stats = new TripStatictics();
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(IPCMessages.LOCALBROADCAST_START_SHARING);
		filter.addAction(IPCMessages.LOCALBROADCAST_STOP_SHARING);
		LocalBroadcastManager.getInstance(AntrackService.this).registerReceiver(mBroadcastReceiver, filter);
	}
	
	private void setupLocationRequest() {
		locationRequest = LocationRequest.create();
		locationRequest.setInterval(5000);
		locationRequest.setFastestInterval(3000);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}
	
	private void setupLocationClient() {
		locationClient = new LocationClient(AntrackService.this, AntrackService.this, AntrackService.this);
		locationClient.connect();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mReceiver.getBinder();
	}
	
	public static boolean isSharingLocation() {
		return serviceIsSharing;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		LocalBroadcastManager.getInstance(AntrackService.this).unregisterReceiver(mBroadcastReceiver);
		
		serviceIsSharing = false;
		
		stats = null;
		
		if(locationClient.isConnected() || locationClient.isConnecting()){
			locationClient.removeLocationUpdates(AntrackService.this);
			locationClient.disconnect();
		}
		locationClient = null;
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		//send error to activity
	}
	
	@Override
	public void onConnected(Bundle bundle) {
		locationClient.requestLocationUpdates(locationRequest, AntrackService.this);
	}
	
	@Override
	public void onDisconnected() {
	}
	
	@Override
	public void onLocationChanged(Location location) {
		handleNewLocation(location);
	}
	
	private void handleNewLocation(Location location) {
		Utility.log(simpleName, "handleNewLocation");
		//1. check validity of location
		boolean toDisplay = shouldDisplayThisLocation(location);
		//2. sharing or not, send location to activity if valid
		if(toDisplay){
			Utility.log(simpleName, "handleNewLocation: sending location");
			sendLocationMessage(location);
		}
		//3. if sharing, save to db, upload, do stats
		if(isSharingLocation()){
			//save to db
			AntrackApp.getInstance(getApplicationContext()).getDbhelper().insert(location, toDisplay);
			//upload location
			uploadLocationToServer(location, toDisplay);
			if(toDisplay){
				//do stats
				stats.addLocation(location);
				//send stats
				AntrackApp.getInstance(AntrackService.this).getStatsUpdater().updateStats(stats);
			}
		}
	}
	
	private boolean shouldDisplayThisLocation(Location location) {
		Utility.log(simpleName, "shouldDisplayThisLocation");
		boolean result = false;
		if (Utility.isValidLocation(location)) {
			if ((previousLocation == null) || !Utility.isWithinAccuracyBound(previousLocation, location)) {
				result = true;
			}
			previousLocation = location;
		}
		return result;
	}
	
	private void uploadLocationToServer(Location location, boolean toDisplay) {
		String token = preference.getString("token", null);
		try {
			AntrackApp.getInstance(getApplication()).getApi()
				.upload(token, location, toDisplay, new Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject obj) {
						try {
							if(obj.getInt("status_code") == 200){
								int followers = obj.getInt("number_of_watcher");
								AntrackApp.getInstance(AntrackService.this).setFollowers(followers);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
					}
				}
			);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}