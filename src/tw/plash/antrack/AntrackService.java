package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class AntrackService extends Service implements LocationListener, ConnectionCallbacks,
		OnConnectionFailedListener {
	
	private final String simpleName = "AntrackService";
	
	private static boolean serviceIsRunning = false;
	private static boolean serviceIsSharing = false;
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	private SharedPreferences preference;
	private Location previousLocation;
	
	private DBHelper dbhelper;
	private TripStatictics stats;
	
	private Messenger mLocationSender;
	private Messenger mStatsSender;
	
	final Messenger mReceiver = new Messenger(new IncomingHandler());
	
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case IPCMessages.REGISTER:
				Log.d("Locator.handleMessage", "ACTIVITY_REGISTER");
				switch(msg.arg1){
				case IPCMessages.MAP_ACTIVITY:
					mLocationSender = msg.replyTo;
					if (isSharingLocation()) {
						//sync trajectory
						syncTrajectory();
					}
					break;
				case IPCMessages.STATS_FRAGMENT:
					mStatsSender = msg.replyTo;
					if(isSharingLocation()){
						syncStats();
					}
					break;
				}
				break;
			case IPCMessages.DEREGISTER:
				Log.d("Locator.handleMessage", "ACTIVITY_DEREGISTER");
				if (msg.replyTo == mLocationSender) {
					mLocationSender = null;
				} else if(msg.replyTo == mStatsSender) {
					mStatsSender = null;
				}
				break;
			case IPCMessages.START_SHARING:
				Utility.log(simpleName, "start sharing");
				prepareToStartSharing();
				break;
			case IPCMessages.STOP_SHARING:
				Utility.log(simpleName, "stop sharing");
				prepareToStopSharing();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private void syncTrajectory() {
		ArrayList<Location> locations = (ArrayList<Location>) dbhelper.getAllDisplayableLocations();
		sendTrajectoryMessage(locations);
	}
	
	private void syncStats(){
		sendStatsUpdate(stats);
	}
	
	private void prepareToStartSharing() {
		resetVariables();
		showNotification();
		serviceIsSharing = true;
	}
	
	private void resetVariables() {
		dbhelper.removeAllLocations();
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
		sendStatsSummary(stats); //XXX
		stopForeground(true);
	}
	
	synchronized private void sendLocationMessage(Location location){
		sendMessageToMapActivity(IPCMessages.UPDATE_NEW_LOCATION, location);
	}
	
	synchronized private void sendTrajectoryMessage(List<Location> locations){
		sendMessageToMapActivity(IPCMessages.SYNC_CURRENT_TRAJECTORY, locations);
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
			Log.w("Locator", "null outgoingMessenger");
		}
	}
	
	synchronized private void sendStatsUpdate(TripStatictics stats){
		sendMessageToStatsFragment(IPCMessages.UPDATE_STATS, stats);
	}
	
	synchronized private void sendStatsSummary(TripStatictics stats){
		sendMessageToStatsFragment(IPCMessages.UPDATE_STATS_SUMMARY, stats);
	}
	
	synchronized private void sendMessageToStatsFragment(int what, Object data){
		if (mStatsSender != null) {
			try {
				Message msg = Message.obtain(null, what, data);
				mStatsSender.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			Log.w("Locator", "null outgoingMessenger");
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		setupLocationRequest();
		setupLocationClient();
		
		serviceIsRunning = true;
		serviceIsSharing = false;
		
		preference = PreferenceManager.getDefaultSharedPreferences(AntrackService.this);
		
		stats = new TripStatictics();
		
		dbhelper = new DBHelper(AntrackService.this);
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
	
	public static boolean isRunning(){
		return serviceIsRunning;
	}
	
	public static boolean isSharingLocation() {
		return serviceIsSharing;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		serviceIsRunning = false;
		serviceIsSharing = false;
		
		stats = null;
		
		dbhelper.closeDB();
		dbhelper = null;
		
		locationClient.removeLocationUpdates(AntrackService.this);
		locationClient.disconnect();
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e("Locator", "onConnectionFailed");
		//send error to activity
	}
	
	@Override
	public void onConnected(Bundle bundle) {
		Log.e("Locator", "onConnected");
		locationClient.requestLocationUpdates(locationRequest, AntrackService.this);
	}
	
	@Override
	public void onDisconnected() {
		Log.e("Locator", "onDisconnected");
	}
	
	@Override
	public void onLocationChanged(Location location) {
		Log.i("Locator.onLocationChanged", location.toString());
		handleNewLocation(location);
	}
	
	private void handleNewLocation(Location location) {
		//1. check validity of location
		boolean toDisplay = shouldDisplayThisLocation(location);
		//2. sharing or not, send location to activity if valid
		if(toDisplay){
			sendLocationMessage(location);
		}
		//3. if sharing, save to db, upload, do stats
		if(isSharingLocation()){
			//save to db
			dbhelper.insert(location, toDisplay);
			//upload location
			uploadLocationToServer(location, toDisplay);
			//do stats
			stats.addLocation(location);
			//send stats
			sendStatsUpdate(stats);
		}
	}
	
	private boolean shouldDisplayThisLocation(Location location) {
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
			AntrackApp.getInstance(getApplication()).getApi().upload(token, location, toDisplay, null, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
