package tw.plash.antrack;

import java.util.ArrayList;

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
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class Locator extends Service implements LocationListener, 
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener{
	
	private static boolean serviceIsRunning = false;
	private static boolean serviceIsSharingLocation = false;
	
	private final int NODISPLAY = 0;
	private final int DISPLAY = 1;
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	private SharedPreferences preference;
	private Location previousLocation;
	
	private DBHelper dbhelper;
	private TripStatictics stats;
	
	private Messenger outgoingMessenger;
	
	final Messenger incomingMessenger = new Messenger(new IncomingHandler());
	private class IncomingHandler extends Handler { 
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LocalBroadcastMessages.ACTIVITY_REGISTER:
				Log.e("Locator.handleMessage", "ACTIVITY_REGISTER");
				outgoingMessenger = msg.replyTo;
				if(isSharingLocation()){
					Log.e("Locator.handleMessage", "SYNC_CURRENT_TRAJECTORY");
					syncInformationWithActivity();
				}
				break;
			case LocalBroadcastMessages.ACTIVITY_DEREGISTER:
				Log.e("Locator.handleMessage", "ACTIVITY_DEREGISTER");
				if(outgoingMessenger == msg.replyTo){
					outgoingMessenger = null;
				}
				break;
			case LocalBroadcastMessages.START_SHARING:
				Log.e("Locator.handleMessage", "START_SHARING");
				prepareToStartSharing();
				break;
			case LocalBroadcastMessages.SHARING_SUMMARY_REQUEST:
				Log.e("Locator.handleMessage", "SHARING_SUMMARY_REQUEST");
				prepareToStopSharing();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private void syncInformationWithActivity(){
		ArrayList<Location> locations = (ArrayList<Location>) dbhelper.getAllDisplayableLocations();
		Bundle bundle = new Bundle();
		bundle.putSerializable("trajectory", locations);
		sendMessageToUI(LocalBroadcastMessages.SYNC_CURRENT_TRAJECTORY, bundle);
	}
	
	private void prepareToStartSharing(){
		resetVariables();
		showNotification();
		serviceIsSharingLocation = true;
	}
	
	private void resetVariables(){
		dbhelper.removeAllLocations();
		stats.resetStats();
		previousLocation = null;
	}
	
	private void showNotification(){
		Notification notification = new Notification(R.drawable.ic_launcher, "sharing has started", System.currentTimeMillis());
		PendingIntent pendingIntent = PendingIntent.getActivity(Locator.this, 0, new Intent(Locator.this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(Locator.this, "AnTrack", "sharing...", pendingIntent);
		startForeground(1337, notification);
	}
	
	private void prepareToStopSharing(){
		serviceIsSharingLocation = false;
		Bundle bundle = new Bundle();
		bundle.putSerializable("stats", stats);
		sendMessageToUI(LocalBroadcastMessages.SHARING_SUMMARY_REPLY, bundle);
		stopForeground(true);
	}
	
	synchronized private void sendMessageToUI(int what, Bundle data) {
		if(outgoingMessenger != null){
			try {
				Message msg = Message.obtain(null, what);
				msg.setData(data);
				outgoingMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else{
			Log.w("Locator", "null outgoingMessenger");
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		setupLocationRequest();
		setupLocationClient();
		
		serviceIsRunning = true;
		
		preference = PreferenceManager.getDefaultSharedPreferences(Locator.this);
		
		stats = new TripStatictics();
		
		dbhelper = new DBHelper(Locator.this);
	}
	
	private void setupLocationRequest(){
		locationRequest = LocationRequest.create();
		locationRequest.setInterval(5000);
		locationRequest.setFastestInterval(3000);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}
	
	private void setupLocationClient(){
		locationClient = new LocationClient(Locator.this, Locator.this, Locator.this);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return incomingMessenger.getBinder();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("MyService", "Received start id " + startId + ": " + intent);
		locationClient.connect();
		return START_STICKY; // run until explicitly stopped.
	}
	
	public static boolean isRunning() {
		return serviceIsRunning;
	}
	
	public static boolean isSharingLocation(){
		return serviceIsSharingLocation;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		serviceIsRunning = false;
		serviceIsSharingLocation = false;
		
		stats = null;
		
		dbhelper.closeDB();
		dbhelper = null;
		
		locationClient.removeLocationUpdates(Locator.this);
		locationClient.disconnect();
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e("Locator", "onConnectionFailed");
	}
	
	@Override
	public void onConnected(Bundle bundle) {
		Log.e("Locator", "onConnected");
		locationClient.requestLocationUpdates(locationRequest, Locator.this);
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
	
	private void handleNewLocation(Location location){
		if (shouldDisplayThisLocation(location)) {
			dbhelper.insert(location, DISPLAY);
			processLocationToBeDisplayed(location);
		} else {
			dbhelper.insert(location, NODISPLAY);
			//no going to display this location, no processing needed
		}
	}
	
	private boolean shouldDisplayThisLocation(Location location){
		boolean result = false;
		if(Utility.isValidLocation(location)){
			if((previousLocation == null) || !Utility.isWithinAccuracyBound(previousLocation, location)){
				result = true;
			}
			previousLocation = location;
		}
		return result;
	}
	
	private void processLocationToBeDisplayed(Location location){
		// send location to activity
		// send location to server
		Bundle bundle = new Bundle();
		bundle.putParcelable("location", location);
		if (serviceIsSharingLocation) {
			stats.addLocation(location);
			bundle.putSerializable("stats", stats);
			//pass through filter first...
			uploadLocationToServer(location, true);
		}
		sendMessageToUI(LocalBroadcastMessages.NEW_LOCATION_UPDATE, bundle);
	}
	
	private void uploadLocationToServer(Location location, boolean toDisplay){
		String token = preference.getString("token", null);
		try {
			AntrackSingleton.getInstance(getApplication()).getApi().upload(token, location, toDisplay, null, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
