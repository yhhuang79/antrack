package tw.plash.antrack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
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
		GooglePlayServicesClient.OnConnectionFailedListener {
	
	private static boolean serviceIsRunning = false;
	private static boolean serviceIsSharingLocation = false;
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	private SharedPreferences preference;
	
	private TripStatictics stats;
	
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
	public static final int STOP_SHARING = 3;
	public static final int STOP_SHARING_AND_SEND_SUMMARY = 4;
	public static final int NEW_LOCATION_UPDATE = 5;
	public static final int SYNC_CURRENT_TRAJECTORY = 6;
	
	
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
				showNotification();
				serviceIsSharingLocation = true;
				stats.resetStats();
				break;
			case STOP_SHARING_AND_SEND_SUMMARY:
				//reply with the summary of current trajectory and stop service
				//get summary
				Log.e("Locator.handleMessage", "STOP_SHARING_AND_SEND_SUMMARY");
				serviceIsSharingLocation = false;
				//send summary to ui
				Bundle bundle = new Bundle();
				bundle.putSerializable("stats", stats);
				sendMessageToUI(STOP_SHARING_AND_SEND_SUMMARY, bundle);
				stopForeground(true);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private void showNotification(){
		Notification notification = new Notification(R.drawable.ic_launcher, "sharing has started", System.currentTimeMillis());
		PendingIntent pendingIntent = PendingIntent.getActivity(Locator.this, 0, new Intent(Locator.this, Map.class), PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(Locator.this, "AnTrack", "sharing...", pendingIntent);
		startForeground(1337, notification);
	}
	
	private void sendMessageToUI(int what, Bundle data) {
		if(outgoingMessenger != null){
			try {
				// Send data as an Integer
				Message msg = Message.obtain(null, what);
				msg.setData(data);
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
		
		preference = PreferenceManager.getDefaultSharedPreferences(Locator.this);
		
		stats = new TripStatictics();
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
		serviceIsSharingLocation = false;
		
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
		//pass new location update through a filter first...then send it to server and UI
		Log.w("Locator.onLocationChanged", location.toString());
		Bundle bundle = new Bundle();
		bundle.putParcelable("location", location);
		if(serviceIsSharingLocation){
			stats.addLocation(location);
			bundle.putSerializable("stats", stats);
			new AsyncTask<Location, Void, Void>() {
				
				private String token;
				
				@Override
				protected void onPreExecute() {
					token = preference.getString("token", "invalidtoken");
					Log.e("Locator.onLocationChanged", "upload: token=" + token);
				};
				
				@Override
				protected Void doInBackground(Location... params) {
					
					if(token.equals("invalidtoken")){
						return null;
					}
						
						String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing";
						HttpClient client = null;
						
						try {
							
							client = Utility.getHttpsClient();
							HttpPost request = new HttpPost(url);
							
							JSONObject obj = new JSONObject();
							obj.put("latitude", params[0].getLatitude());
							obj.put("longitude", params[0].getLongitude());
							obj.put("altitude", params[0].getAltitude());
							obj.put("accuracy", params[0].getAccuracy());
							obj.put("speed", params[0].getSpeed());
							obj.put("bearing", params[0].getBearing());
							obj.put("location_source", params[0].getProvider());
							obj.put("timestamp", new Timestamp(params[0].getTime()).toString());
							JSONArray array = new JSONArray();
							array.put(obj);
							
							List<NameValuePair> param = new ArrayList<NameValuePair>();
							param.add(new BasicNameValuePair("action", "upload"));
							param.add(new BasicNameValuePair("token", token));
							param.add(new BasicNameValuePair("location", array.toString()));
							UrlEncodedFormEntity entity = new UrlEncodedFormEntity(param, HTTP.UTF_8);
							request.setEntity(entity);
							
							HttpResponse response = client.execute(request);
							
							if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
								
								BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
										.getContent()));
								
								String readInputLine = null;
								readInputLine = in.readLine();
								
								JSONObject result = new JSONObject(new JSONTokener(readInputLine));
								in.close();
								
								int numberOfWatchers = result.getInt("number_of_watcher");
								
								stats.setNumberOfWatcher(numberOfWatchers);
								
								Log.e("Locator.onLocationChanged", "upload: result=" + result.toString());
							}
							
						} catch (IOException e) {
							e.printStackTrace();
						} catch (JSONException e) {
							e.printStackTrace();
						} finally {
							Log.e("Locator.onLocationChanged", "upload: finally");
							if (client != null) {
								client.getConnectionManager().shutdown();
							}
						}
					return null;
				}
			}.execute(location);
		}
		sendMessageToUI(NEW_LOCATION_UPDATE, bundle);
	}
}
