package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

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
	
//	private static boolean serviceIsRunning = false;
	private static boolean serviceIsSharing = false;
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	private SharedPreferences preference;
	private Location previousLocation;
	
	private DBHelper dbhelper;
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
	private Messenger mStatsSender;
	
	final Messenger mReceiver = new Messenger(new IncomingHandler());
	
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case IPCMessages.REGISTER:
				Utility.log(simpleName, "handle msg: register");
				switch(msg.arg1){
				case IPCMessages.MAP_ACTIVITY:
					Utility.log(simpleName, "handle msg: register: map");
					mLocationSender = msg.replyTo;
					if (isSharingLocation()) {
						//sync trajectory
						syncTrajectory();
					}
					break;
				case IPCMessages.STATS_FRAGMENT:
					Utility.log(simpleName, "handle msg: register: stats");
					mStatsSender = msg.replyTo;
					if(isSharingLocation()){
						syncStats();
					}
					break;
				}
				break;
			case IPCMessages.DEREGISTER:
				Utility.log(simpleName, "handle msg: deregister");
				if (msg.replyTo == mLocationSender) {
					Utility.log(simpleName, "handle msg: deregister: map");
					mLocationSender = null;
				} else if(msg.replyTo == mStatsSender) {
					Utility.log(simpleName, "handle msg: deregister: stats");
					mStatsSender = null;
				}
				break;
			case IPCMessages.START_SHARING:
				Utility.log(simpleName, "handle msg: start sharing");
//				prepareToStartSharing();
				break;
			case IPCMessages.STOP_SHARING:
				Utility.log(simpleName, "handle msg: stop sharing");
//				prepareToStopSharing();
				break;
			default:
				Utility.log(simpleName, "handle msg: default");
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
		sendStatsUpdate(stats); //XXX
		stopForeground(true);
	}
	
	synchronized private void sendLocationMessage(Location location){
		sendMessageToMapActivity(IPCMessages.UPDATE_NEW_LOCATION, location);
	}
	
	synchronized private void sendTrajectoryMessage(List<Location> locations){
		sendMessageToMapActivity(IPCMessages.SYNC_CURRENT_TRAJECTORY, locations);
	}
	
	synchronized private void sendMessageToMapActivity(int what, Object data) {
		Utility.log(simpleName, "sendMessageToMapActivity");
		if (mLocationSender != null) {
			try {
				Utility.log(simpleName, "sendMessageToMapActivity: message sent");
				Message msg = Message.obtain(null, what, data);
				mLocationSender.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			Utility.log(simpleName, "sendMessageToMapActivity: sender is null");
		}
	}
	
	synchronized private void sendStatsUpdate(TripStatictics stats){
		sendMessageToStatsFragment(IPCMessages.UPDATE_STATS, stats);
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
		Utility.log(simpleName, "onCreate: begin");
		
		setupLocationRequest();
		setupLocationClient();
		
//		serviceIsRunning = true;
		serviceIsSharing = false;
		
		preference = PreferenceManager.getDefaultSharedPreferences(AntrackService.this);
		
		stats = new TripStatictics();
		
		dbhelper = new DBHelper(AntrackService.this);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(IPCMessages.LOCALBROADCAST_START_SHARING);
		filter.addAction(IPCMessages.LOCALBROADCAST_STOP_SHARING);
		LocalBroadcastManager.getInstance(AntrackService.this).registerReceiver(mBroadcastReceiver, filter);
		Utility.log(simpleName, "onCreate: done");
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
	
//	public static boolean isRunning(){
//		return serviceIsRunning;
//	}
	
	public static boolean isSharingLocation() {
		return serviceIsSharing;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Utility.log(simpleName, "onDestroy: init");
		
		LocalBroadcastManager.getInstance(AntrackService.this).unregisterReceiver(mBroadcastReceiver);
		
//		serviceIsRunning = false;
		serviceIsSharing = false;
		
		stats = null;
		
		dbhelper.closeDB();
		dbhelper = null;
		
		if(locationClient.isConnected() || locationClient.isConnecting()){
			locationClient.removeLocationUpdates(AntrackService.this);
			locationClient.disconnect();
		}
		locationClient = null;
		
		Utility.log(simpleName, "onDestroy: done");
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Utility.log(simpleName, "onConnectionFailed");
		//send error to activity
	}
	
	@Override
	public void onConnected(Bundle bundle) {
		Utility.log(simpleName, "onConnected");
		locationClient.requestLocationUpdates(locationRequest, AntrackService.this);
	}
	
	@Override
	public void onDisconnected() {
		Utility.log(simpleName, "onDisconnected");
	}
	
	@Override
	public void onLocationChanged(Location location) {
		Log.i("Locator.onLocationChanged", location.toString());
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
			dbhelper.insert(location, toDisplay);
			//upload location
//			uploadLocationToServer(location, toDisplay);
			if(toDisplay){
				//do stats
				stats.addLocation(location);
				//send stats
				sendStatsUpdate(stats);
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
			AntrackApp.getInstance(getApplication()).getApi().upload(token, location, toDisplay, null, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
