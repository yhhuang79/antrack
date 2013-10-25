package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
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
	
	private static boolean serviceIsSharing = false;
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	private SharedPreferences preference;
	private Location previousLocation;
	private BlockingQueue<AntsLocation> pendingRetryLocations;
	
	private TripStatictics stats;
	private boolean uploadTaskIsRunning;
	private boolean canKeepRunning;
	
	private Handler mHandler;
	private Runnable uploadImageTask = new Runnable() {
		@Override
		public void run() {
			Log.e("tw.uploadImageTask", "start");
			//get one marker and try upload
			ImageMarker imageMarker = AntrackApp.getInstance(getApplicationContext()).getDbhelper().getPendingUploadImageMarker();
			if(imageMarker != null){
				final int code = imageMarker.getCode();
				AntrackApp.getInstance(getApplicationContext()).getApi()
				.uploadImage(preference.getString("token", null), imageMarker, new Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject obj) {
						Log.e("tw.uploadImageTask", "upload image response " + obj.toString());
						try {
							if(obj.getInt(Constants.API_RES_KEY_STATUS_CODE) == 200){
								//mark as uploaded
								AntrackApp.getInstance(getApplicationContext()).getDbhelper().setImageMarkerState(code, Constants.IMAGE_MARKER_STATE.DONE);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						} finally{
							//do it hare to succeed both try and catch block
							startAgain();
						}
						Log.e("tw.uploadImageTask", "upload image response done");
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("tw.uploadImageTask", "upload image response: error= " + error.toString());
						startAgain();
					}
				});
			} else{
				uploadTaskIsRunning = false;
			}
		}
		
		private void startAgain(){
			Log.e("tw.uploadImageTask", "start again");
			if(canKeepRunning){
				uploadTaskIsRunning = true;
				mHandler.postDelayed(uploadImageTask, 1000);
				Log.e("tw.uploadImageTask", "start again delayed");
			} else{
				uploadTaskIsRunning = false;
			}
		}
	};
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(IPCMessages.LB_START_SHARING)){
				prepareToStartSharing();
			} else if(action.equals(IPCMessages.LB_STOP_SHARING)){
				prepareToStopSharing();
			} else if(action.equals(IPCMessages.LB_IMAGE_CREATE)){
				String path = intent.getStringExtra(IPCMessages.LB_EXTRA_IMAGE_PATH);
				int code = intent.getIntExtra(IPCMessages.LB_EXTRA_REQUEST_CODE, -1);
				if(code >= 0){
					long result = AntrackApp.getInstance(getApplicationContext()).getDbhelper().insertImageMarkerPath(code, path);
					Log.d("tw.service", "added image marker path into DB row no." + result);
				} else{
					Log.e("tw.service", "received invalid code: " + code + " at image creation");
				}
			} else if(action.equals(IPCMessages.LB_IMAGE_CONFIRM)){
				int code = intent.getIntExtra(IPCMessages.LB_EXTRA_REQUEST_CODE, -1);
				if(code >= 0){
					int result = AntrackApp.getInstance(getApplicationContext()).getDbhelper().insertImageMarkerLocation(code, previousLocation);
					Log.d("tw.service", "added " + result + " image marker location(s) into DB");
					if(uploadTaskIsRunning){
						//do nothing
					} else{
						//start a new thread and start upload
						uploadTaskIsRunning = true;
						mHandler.post(uploadImageTask);
					}
				} else{
					Log.e("tw.service", "received invalid code: " + code + " at image confirmation");
				}
			} else if(action.equals(IPCMessages.LB_IMAGE_CANCEL)){
				int code = intent.getIntExtra(IPCMessages.LB_EXTRA_REQUEST_CODE, -1);
				if(code >= 0){
					//remove it from DB
					int result = AntrackApp.getInstance(getApplicationContext()).getDbhelper().removeImageMarker(code);
					//result should be 1, if not, something is wrong
					Log.d("tw.service", "removed " + result + " image marker entries from DB");
				} else{
					Log.e("tw.service", "received invalid code: " + code + " at image cancellation");
				}
			}
		}
	};
	
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
		
		pendingRetryLocations = new LinkedBlockingQueue<AntsLocation>();
		
		mHandler = new Handler();
		uploadTaskIsRunning = false;
		canKeepRunning = true;
		
		setupLocationRequest();
		setupLocationClient();
		
		serviceIsSharing = false;
		
		preference = PreferenceManager.getDefaultSharedPreferences(AntrackService.this);
		
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
		
		canKeepRunning = false;
		mHandler.removeCallbacks(uploadImageTask);
		mHandler = null;
		
		AntrackApp.getInstance(getApplicationContext()).cancelAll();
		
		LocalBroadcastManager.getInstance(AntrackService.this).unregisterReceiver(mBroadcastReceiver);
		
		serviceIsSharing = false;
		
		stats = null;
		
		if(locationClient.isConnected() || locationClient.isConnecting()){
			locationClient.removeLocationUpdates(AntrackService.this);
			locationClient.disconnect();
		}
		locationClient = null;
		
		pendingRetryLocations = null;
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
			AntsLocation antsLocation = new AntsLocation(location, toDisplay);
			//save to db
			AntrackApp.getInstance(getApplicationContext()).getDbhelper().insertAntsLocation(antsLocation);
			//upload location
			uploadLocationToServer(antsLocation);
//			uploadLocationsToServer();
			if(toDisplay){
				//do stats
				stats.addLocation(location);
				//send stats
				AntrackApp.getInstance(AntrackService.this).getStatsUpdater().updateStats(stats);
			}
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
	
	synchronized private void uploadLocationToServer(AntsLocation antsLocation) {
		if(pendingRetryLocations.isEmpty()){
			//no pending locations, do current location only
			new Thread(new locationUploadTask(antsLocation)).start();
		} else{
			//there are pending locations
			List<AntsLocation> locations = new ArrayList<AntsLocation>();
			pendingRetryLocations.drainTo(locations); //get all pending locations
			locations.add(antsLocation); //and current location
			new Thread(new locationUploadTask(locations)).start();
		}
	}
	
	private class locationUploadTask implements Runnable{
		
		private List<AntsLocation> locations;
		
		public locationUploadTask(List<AntsLocation> locations){
			this.locations = locations;
		}
		
		public locationUploadTask(AntsLocation location) {
			this.locations = new ArrayList<AntsLocation>();
			this.locations.add(location);
		}
		
		@Override
		public void run() {
			String token = preference.getString("token", null);
			try {
				AntrackApp.getInstance(getApplication()).getApi().upload(token, locations, new Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject obj) {
						try {
							if(obj.getInt(Constants.API_RES_KEY_STATUS_CODE) == 200){
								int followers = obj.getInt("number_of_watcher");
								AntrackApp.getInstance(AntrackService.this).setFollowers(followers);
							} else{
								addToRetry();
							}
						} catch (JSONException e) {
							e.printStackTrace();
							addToRetry();
						}
					}
				}, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						addToRetry();
					}
				});
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		private void addToRetry(){
			synchronized (pendingRetryLocations) {
				for(AntsLocation location : locations){
					pendingRetryLocations.offer(location);
				}
			}
		}
	};
}