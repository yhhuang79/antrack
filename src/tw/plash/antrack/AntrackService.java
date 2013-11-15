package tw.plash.antrack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class AntrackService extends Service implements LocationListener, ConnectionCallbacks,
		OnConnectionFailedListener {
	
	private static boolean serviceIsSharing = false;
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	
	private TripStatictics stats;
	
	private AntrackApp app;
	
	private ImageUploader imageUploader;
	private LocationUploader locationUploader;
	
	private Map<String, ActionHandler> handlers;
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(IPCMessages.LB_START_SHARING)){
				prepareToStartSharing();
			} else if(action.equals(IPCMessages.LB_STOP_SHARING)){
				prepareToStopSharing();
			} else {
				ActionHandler actionHandler = lookupHandlerBy(action);
				if(actionHandler != null){
					actionHandler.execute(intent);
				}
			}
		}
	};
	
	private ActionHandler lookupHandlerBy(String action){
		return handlers.get(action);
	}
	
	public void createHandlers(){
		handlers = new HashMap<String, ActionHandler>();
		handlers.put(IPCMessages.LB_IMAGE_CREATE, new ImageCreationHandler(app));
		handlers.put(IPCMessages.LB_IMAGE_CONFIRM, new ImageConfirmationHandler(app, imageUploader));
		handlers.put(IPCMessages.LB_IMAGE_CANCEL, new ImageCancellationHandler(app));
	}
	
	private Messenger mLocationSender;
	private final Messenger mReceiver = new Messenger(new IncomingHandler());
	
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
		List<Location> locations = app.getDbhelper().getAllDisplayableLocations();
		sendTrajectoryMessage(locations);
		List<ImageMarker> imagemarkers = app.getDbhelper().getImageMarkers();
		sendImageMarkerMessage(imagemarkers);
	}
	
	private void prepareToStartSharing() {
		resetVariables();
		showNotification();
		serviceIsSharing = true;
	}
	
	private void resetVariables() {
		app.getDbhelper().removeAll();
		stats.resetStats();
		app.setLatestLocation(null);
	}
	
	private void showNotification() {
		Notification notification = new Notification(R.drawable.ic_launcher, "sharing has started",
				System.currentTimeMillis());
		PendingIntent pendingIntent = PendingIntent.getActivity(AntrackService.this, 0, new Intent(AntrackService.this,
				AntrackMapActivity.class).setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT), PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(AntrackService.this, "AnTrack", "sharing...", pendingIntent);
		startForeground(1337, notification);
	}
	
	private void prepareToStopSharing() {
		serviceIsSharing = false;
		app.getStatsUpdater().updateStats(stats);
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
		
		app = AntrackApp.getInstance(this);
		
		imageUploader = new ImageUploader(app, new Handler(), PreferenceManager.getDefaultSharedPreferences(AntrackService.this));
		locationUploader = new LocationUploader(app, PreferenceManager.getDefaultSharedPreferences(AntrackService.this));
		
		createHandlers();
		
		stats = new TripStatictics();
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(IPCMessages.LB_START_SHARING);
		filter.addAction(IPCMessages.LB_STOP_SHARING);
		filter.addAction(IPCMessages.LB_IMAGE_CREATE);
		filter.addAction(IPCMessages.LB_IMAGE_CONFIRM);
		filter.addAction(IPCMessages.LB_IMAGE_CANCEL);
		LocalBroadcastManager.getInstance(AntrackService.this).registerReceiver(mBroadcastReceiver, filter);
	}
	
	private void setupLocationRequest() {
		locationRequest = LocationRequest.create();
		locationRequest.setInterval(Constants.LOCATION_INTERVAL);
		locationRequest.setFastestInterval(Constants.LOCATION_FASTEST_INTERVAL);
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
		
		imageUploader.stop();
		imageUploader = null;
		
		app.cancelAll();
		
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
		//1. check validity of location
		boolean toDisplay = shouldDisplayThisLocation(location);
		//2. sharing or not, send location to activity if valid
		if(toDisplay){
			sendLocationMessage(location);
		}
		//3. if sharing, save to db, upload, do stats
		if(isSharingLocation()){
			AntrackLocation aLocation = new AntrackLocation(location, toDisplay);
			//save to db
			app.getDbhelper().insertLocation(aLocation);
			//upload location
			locationUploader.upload(aLocation);
			if(toDisplay){
				//do stats
				stats.addLocation(location);
				//send stats
				app.getStatsUpdater().updateStats(stats);
			}
		}
	}
	
	private boolean shouldDisplayThisLocation(Location location) {
		boolean result = false;
		if (Utility.isValidLocation(location)) {
			if ((app.getLatestLocation() == null) || !Utility.isWithinAccuracyBound(app.getLatestLocation(), location)) {
				result = true;
			}
			app.setLatestLocation(location);
		}
		return result;
	}
}