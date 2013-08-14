package tw.plash.antrack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.SupportMapFragment;

public class Map extends FragmentActivity {
	
	private enum ConnectionResult {NO_INTERNET, PARAMETER_ERROR, CONNECTION_ERROR, ALL_GOOD};
	
	private SharedPreferences preference;
	
	private GoogleMap gmap;
	private Button controlButton;
	private TextView latitudeField;
	private TextView longitudeField;
	private TextView durationField;
	private TextView distanceField;
	
	private OnLocationChangedListener onLocationChangedListener;
	
	private Messenger messengerToService = null;
	private boolean mIsBound;
	private final Messenger messengerFromService = new Messenger(new IncomingHandler());
	
	private final ArrayDeque<Location> locations = new ArrayDeque<Location>();
	
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Locator.SYNC_CURRENT_TRAJECTORY:
				//will be called right after binding with service
//				Object obj = msg.getData().getParcelable("locations");
//				if(obj instanceof Collection<?>){
//					locations.clear();
//					locations.addAll((Collection<? extends Location>) obj);
//				}
				
				break;
			case Locator.NEW_LOCATION_UPDATE:
				
				Location location = (Location) msg.obj;
				onLocationChangedListener.onLocationChanged(location);
				
				latitudeField.setText("latitude: " + location.getLatitude());
				longitudeField.setText("longitude: " + location.getLongitude());
				durationField.setText("timestamp: " + location.getTime());
				distanceField.setText("accuracy: " + location.getAccuracy());
				
				break;
			case Locator.STOP_SHARING_AND_SEND_SUMMARY:
				
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			messengerToService = new Messenger(service);
			try {
				Message msg = Message.obtain(null, Locator.ACTIVITY_REGISTER);
				msg.replyTo = messengerFromService;
				messengerToService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do
				// anything with it
			}
		}
		
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected - process crashed.
			messengerToService = null;
		}
	};
	
	private void sendMessageToService(int what) {
		if (mIsBound) {
			if (messengerToService != null) {
				try {
					Message msg = Message.obtain(null, what);
					msg.replyTo = messengerFromService;
					messengerToService.send(msg);
				} catch (RemoteException e) {
				}
			}
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.map);
		
		preference = PreferenceManager.getDefaultSharedPreferences(Map.this);
		
		setupMapIfNotAvailable();
		
		setupControlButton();
		
		setupStatsPanel();
	}
	
	private void setupMapIfNotAvailable() {
		if (gmap == null) {
			gmap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			setUpMap();
		}
	}
	
	private void setUpMap() {
		gmap.setLocationSource(new LocationSource() {
			@Override
			public void activate(OnLocationChangedListener listener) {
				onLocationChangedListener = listener;
			}
			@Override
			public void deactivate() {
				onLocationChangedListener = null;
			}
		});
		gmap.setMyLocationEnabled(true);
		gmap.getUiSettings().setAllGesturesEnabled(true);
		gmap.getUiSettings().setCompassEnabled(true);
	}
	
	private void setupControlButton(){
		controlButton = (Button) findViewById(R.id.controlbutton);
		if(Locator.isSharingLocation()){
			controlButton.setText("STOP sharing my location");
		} else{
			controlButton.setText("START sharing my location");
		}
		controlButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (shouldStopSharing()) {
					setupConfirmStopDialog();
				} else {
					setupStartSharingConnection();
				}
			}
		});
	}
	
	private boolean shouldStopSharing(){
		if(controlButton.getText().toString().contains("STOP")){
			return true;
		} else{
			return false;
		}
	}
	
	private void setupConfirmStopDialog(){
		new AlertDialog.Builder(Map.this)	
		.setMessage("are you sure you want to stop sharing?")
		.setPositiveButton("yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Toast.makeText(Map.this, "sharing is STOPPED", Toast.LENGTH_SHORT).show();
				controlButton.setText("START sharing my location");
				
				sendMessageToService(Locator.STOP_SHARING_AND_SEND_SUMMARY);
				
				new AsyncTask<Void, Void, ConnectionResult>(){
					
					private ProgressDialog diag;
					private String token;
					
					@Override
					protected void onPreExecute() {
						diag = new ProgressDialog(Map.this);
						diag.setMessage("stopping...");
						diag.setIndeterminate(true);
						diag.setCancelable(false);
						diag.show();
						token = preference.getString("token", "invalidtoken");
					};
					
					@Override
					protected ConnectionResult doInBackground(Void... params) {
						if(!Utility.isInternetAvailable(Map.this)){
							//no internet, prompt user to check internet service and try again later
							return ConnectionResult.NO_INTERNET;
						} else if(token.contains("invaliduserid")){
							return ConnectionResult.PARAMETER_ERROR;
						} else {
							//we have internet, now do the "initialize" connection
							String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing?action=stop&token="
									+ token;
							Log.e("antrack.map", "stop connection, url=" + url);
							
							HttpClient client = null;
							
							try {
								
								client = Utility.getHttpsClient();
								HttpGet request = new HttpGet(url);
								HttpResponse response = client.execute(request);
								
								if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
									
									BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
											.getContent()));
									
									String readInputLine = null;
									readInputLine = in.readLine();
									
									JSONObject result = new JSONObject(new JSONTokener(readInputLine));
									in.close();
									
									Log.e("antrack.map", "stop connection, result=" + result.toString());
									
									preference.edit().remove("token").remove("url").commit();
									
									return ConnectionResult.ALL_GOOD;
								}
							} catch (IOException e) {
								e.printStackTrace();
							} catch (JSONException e) {
								e.printStackTrace();
							} finally {
								
								Log.e("antrack.map", "stop connection, finally");
								
								if (client != null) {
									client.getConnectionManager().shutdown();
								}
							}
						}
						return null;
					}
					
					@Override
					protected void onPostExecute(ConnectionResult result) {
						diag.dismiss();
						switch(result){
						case NO_INTERNET:
							Toast.makeText(Map.this, "NO INTERNET", Toast.LENGTH_LONG).show();
							break;
						case ALL_GOOD:
							Toast.makeText(Map.this, "ALL GOOD", Toast.LENGTH_LONG).show();
							controlButton.setText("START sharing my location");
							break;
						case PARAMETER_ERROR:
							Toast.makeText(Map.this, "INVALID USERID", Toast.LENGTH_LONG).show();
							break;
						case CONNECTION_ERROR:
						default:
							Toast.makeText(Map.this, "CONNECTION ERROR", Toast.LENGTH_LONG).show();
							break;
						}
					};
				}.execute();
				
			}
		})
		.setNegativeButton("no", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Toast.makeText(Map.this, "sharing CONTINUES", Toast.LENGTH_SHORT).show();
			}
		})
		.show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.e("Map.onActivityResult", "request code: " + requestCode);
		Log.e("Map.onActivityResult", "result code: " + resultCode);
		if(requestCode == 12345){
			switch(resultCode){
			case RESULT_CANCELED:
				Toast.makeText(Map.this, "sharing canceled!", Toast.LENGTH_SHORT).show();
				sendMessageToService(Locator.STOP_SHARING_AND_SEND_SUMMARY);
				controlButton.setText("START sharing my location");
				Log.e("Map.onActivityResult", "RESULT_CANCELED");
				break;
			case RESULT_FIRST_USER:
				Log.e("Map.onActivityResult", "RESULT_FIRST_USER");
				break;
			case RESULT_OK:
				Log.e("Map.onActivityResult", "RESULT_OK");
				break;
			default:
				Log.e("Map.onActivityResult", "default");
				break;
			}
		}
	}
	
	private void setupStartSharingConnection(){
		
		new AsyncTask<Void, Void, ConnectionResult>(){
			
			private ProgressDialog diag;
			private String userid;
			
			@Override
			protected void onPreExecute() {
				diag = new ProgressDialog(Map.this);
				diag.setMessage("Contacting AnTrack location\nsharing service...");
				diag.setIndeterminate(true);
				diag.setCancelable(false);
				diag.show();
				userid = preference.getString("userid", "invaliduserid");
			};
			
			@Override
			protected ConnectionResult doInBackground(Void... params) {
				if(!Utility.isInternetAvailable(Map.this)){
					//no internet, prompt user to check internet service and try again later
					return ConnectionResult.NO_INTERNET;
				} else if(userid.contains("invaliduserid")){
					return ConnectionResult.PARAMETER_ERROR;
				} else {
					//we have internet, now do the "initialize" connection
					String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing?action=initialize&userid="
							+ userid;
					Log.e("antrack.map", "init connection, url=" + url);
					
					HttpClient client = null;
					
					try {
						
						client = Utility.getHttpsClient();
						HttpGet request = new HttpGet(url);
						HttpResponse response = client.execute(request);
						
						if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
							
							BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
									.getContent()));
							
							String readInputLine = null;
							readInputLine = in.readLine();
							
							JSONObject result = new JSONObject(new JSONTokener(readInputLine));
							in.close();
							
							Log.e("antrack.map", "init connection, result=" + result.toString());
							
							String token = result.getString("token");
							String sharingUrl = result.getString("url");
							
							preference.edit().putString("token", token).putString("url", sharingUrl).commit();
							
							return ConnectionResult.ALL_GOOD;
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					} finally {
						
						Log.e("antrack.map", "init connection, finally");
						
						if (client != null) {
							client.getConnectionManager().shutdown();
						}
					}
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(ConnectionResult result) {
				diag.dismiss();
				switch(result){
				case NO_INTERNET:
					Toast.makeText(Map.this, "NO INTERNET", Toast.LENGTH_LONG).show();
					break;
				case ALL_GOOD:
					Toast.makeText(Map.this, "ALL GOOD", Toast.LENGTH_LONG).show();
					controlButton.setText("STOP sharing my location");
					//should notify service to show notification and keep running...
					sendMessageToService(Locator.START_SHARING);
					
					Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Hey! it's me!");
					sendIntent.putExtra(Intent.EXTRA_TEXT, "Click on the link to follow my lead..." + preference.getString("url", ""));
					sendIntent.setType("text/plain");
					startActivityForResult(Intent.createChooser(sendIntent, "Share via..."), 12345);
					
					break;
				case PARAMETER_ERROR:
					Toast.makeText(Map.this, "INVALID USERID", Toast.LENGTH_LONG).show();
					break;
				case CONNECTION_ERROR:
				default:
					Toast.makeText(Map.this, "CONNECTION ERROR", Toast.LENGTH_LONG).show();
					break;
				}
			};
		}.execute();
	}
	
	private void setupStatsPanel(){
		latitudeField = (TextView) findViewById(R.id.latitude_field);
		longitudeField = (TextView) findViewById(R.id.longitude_field);
		durationField = (TextView) findViewById(R.id.duration_field);
		distanceField = (TextView) findViewById(R.id.distance_field);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startServiceIfNotAlreadyRunning();
		bindToService();
	}
	
	private void startServiceIfNotAlreadyRunning() {
		Log.e("map.onresume", "start service if neccessary");
		if (!Locator.isRunning()) {
			Log.e("map.onresume", "lets start the service");
			startService(new Intent(Map.this, Locator.class));
		}
	}
	
	void bindToService() {
		bindService(new Intent(Map.this, Locator.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unbindFromService();
		stopServiceIfNotSharing();
	}
	
	void unbindFromService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (messengerToService != null) {
				try {
					Message msg = Message.obtain(null, Locator.ACTIVITY_DEREGISTER);
					msg.replyTo = messengerFromService;
					messengerToService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has
					// crashed.
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	
	private void stopServiceIfNotSharing(){
		Log.e("map.onpause", "stop service if neccessary");
		if (!Locator.isSharingLocation()) {
			Log.e("map.onpause", "lets stop the service");
			stopService(new Intent(Map.this, Locator.class));
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.activity_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}