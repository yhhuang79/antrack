package tw.plash.antrack;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class Locator extends Service implements LocationListener, 
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {
	
	private static boolean serviceIsRunning = false;
	private static boolean serviceIsSharingLocation = false;
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	
	private Messenger outgoingMessenger;
	
	public enum ServiceMessageType {
		ACTIVITY_REGISTER,
		ACTIVITY_DEREGISTER,
		START_SHARING,
		STOP_SHARING_AND_SEND_SUMMARY,
		NEW_LOCATION_UPDATE,
		SYNC_CURRENT_TRAJECTORY
	}
	
	public static final int ACTIVITY_REGISTER = 0;
	public static final int ACTIVITY_DEREGISTER = 1;
	public static final int START_SHARING = 2;
	public static final int STOP_SHARING_AND_SEND_SUMMARY = 3;
	public static final int NEW_LOCATION_UPDATE = 4;
	public static final int SYNC_CURRENT_TRAJECTORY = 5;
	
	
	final Messenger incomingMessenger = new Messenger(new IncomingHandler()); 
	class IncomingHandler extends Handler { 
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ACTIVITY_REGISTER:
				Log.e("Locator.handleMessage", "ACTIVITY_REGISTER");
				outgoingMessenger = msg.replyTo;
				//send complete trajectory if already recording XXX
				if(isSharingLocation()){
					Log.e("Locator.handleMessage", "SYNC_CURRENT_TRAJECTORY");
					sendMessageToUI(SYNC_CURRENT_TRAJECTORY, null);
				}
				break;
			case ACTIVITY_DEREGISTER:
				Log.e("Locator.handleMessage", "ACTIVITY_DEREGISTER");
				if(outgoingMessenger == msg.replyTo){
					outgoingMessenger = null;
				}
				break;
			case START_SHARING:
				Log.e("Locator.handleMessage", "START_SHARING");
				break;
			case STOP_SHARING_AND_SEND_SUMMARY:
				//reply with the summary of current trajectory and stop service
				//get summary
				Log.e("Locator.handleMessage", "STOP_SHARING_AND_SEND_SUMMARY");
				serviceIsSharingLocation = false;
				//send summary to ui
				sendMessageToUI(STOP_SHARING_AND_SEND_SUMMARY, null);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private void sendMessageToUI(int what, Object data) {
		if(outgoingMessenger != null){
			try {
				// Send data as an Integer
				Message msg = Message.obtain(null, what, data);
				outgoingMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else{
			//no one is attached to this service, don't send message
			Log.w("Locator", "null outgoingMessenger");
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		setupLocationRequest();
		setupLocationClient();
		
		locationClient.connect();
		
		serviceIsRunning = true;
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
		
		locationClient.removeLocationUpdates(Locator.this);
		locationClient.disconnect();
		
		stopForeground(true);
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
		//pass new location update through a filter first...then send it to server and UI
		Log.w("Locator.onLocationChanged", location.toString());
		sendMessageToUI(NEW_LOCATION_UPDATE, location);
	}
}
