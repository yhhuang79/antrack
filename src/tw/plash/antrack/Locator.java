package tw.plash.antrack;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

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

public class Locator extends Service implements LocationListener, 
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {
	
	private static boolean serviceIsRunning = false;
	
	static final int MSG_REGISTER_CLIENT = 0;
	static final int MSG_UNREGISTER_CLIENT = 1;
	static final int MSG_STOP_SERVICE = 2;
	static final int MSG_LOCATION_UPDATE = 3;
	static final int MSG_TRAJECTORY_SYNC = 4;
	
	private final int MSG_SEND_SUMMARY_TO_UI = 5;
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	
	private Messenger outgoingMessenger;
	
	final Messenger incomingMessenger = new Messenger(new IncomingHandler()); 
	class IncomingHandler extends Handler { 
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				outgoingMessenger = msg.replyTo;
				//send complete trajectory if already recording XXX
				sendMessageToUI(MSG_TRAJECTORY_SYNC, null);
				break;
			case MSG_UNREGISTER_CLIENT:
				if(outgoingMessenger == msg.replyTo){
					outgoingMessenger = null;
				}
				break;
			case MSG_STOP_SERVICE:
				//reply with the summary of current trajectory and stop service
				//get summary
				
				//send summary to ui
				sendMessageToUI(MSG_SEND_SUMMARY_TO_UI, null);
				//stop the service properly
				stopService();
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
				// The client is dead. Remove it from the list; we are going
				// through the list from back to front so this is safe to do
				// inside the loop.
				e.printStackTrace();
			}
		} else{
			//no one is attached to this service, don't send message
			Log.w("exampleservice", "null leMessenger");
		}
	}
	
	private void stopService(){
		//remove any notification first, then stop service
		stopForeground(true);
		stopSelf();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		
		serviceIsRunning = true;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("MyService", "Received start id " + startId + ": " + intent);
		return START_STICKY; // run until explicitly stopped.
	}
	
	public static boolean isRunning() {
		return serviceIsRunning;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		serviceIsRunning = false;
		
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onConnected(Bundle bundle) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onLocationChanged(Location location) {
		//pass new location update through a filter first...then send it to server and UI
	}
	
}
