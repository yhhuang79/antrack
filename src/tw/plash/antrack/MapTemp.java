package tw.plash.antrack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;

public class MapTemp extends FragmentActivity implements LocationListener,
		GooglePlayServicesClient.ConnectionCallbacks, 
		GooglePlayServicesClient.OnConnectionFailedListener {
	
	private GoogleMap gmap;
	
	private LocationRequest locationRequest;
	private LocationClient locationClient = null;
	
	private String token;
	private SharedPreferences pref;
	private OnLocationChangedListener onLocationChangedListener;
	
	private String shareIntentIsSent = "share.intent.is.sent";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.map);
		
		pref = PreferenceManager.getDefaultSharedPreferences(MapTemp.this);
		token = pref.getString("token", "-1");
		
		if (!pref.getBoolean(shareIntentIsSent, false)) {
			final Intent sendIntent = getIntent().getExtras().getParcelable("sendintent");
			
			new AsyncTask<Void, Void, Void>() {
				
				private ProgressDialog diag;
				
				@Override
				protected void onPreExecute() {
					diag = new ProgressDialog(MapTemp.this);
					diag.setMessage("preparing your invitation message");
					diag.setIndeterminate(true);
					diag.setCancelable(false);
					diag.show();
				};
				
				@Override
				protected Void doInBackground(Void... params) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return null;
				}
				
				@Override
				protected void onPostExecute(Void result) {
					super.onPostExecute(result);
					diag.dismiss();
					diag = null;
					startActivity(sendIntent);
					pref.edit().putBoolean(shareIntentIsSent, true).commit();
				}
			}.execute();
		} else {
			
			isMapAvailable();
			
			locationRequest = LocationRequest.create();
			
			locationRequest.setInterval(5000); // XXX
			locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			locationRequest.setFastestInterval(1000); // XXX
			
			locationClient = new LocationClient(MapTemp.this, MapTemp.this, MapTemp.this);
			
			Button stop = (Button) findViewById(R.id.stop);
			stop.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Button btn = (Button) v;
					if (btn.getText().toString().contains("stop")) {
						Toast.makeText(MapTemp.this, "stoppp", Toast.LENGTH_SHORT).show();
						locationClient.removeLocationUpdates(MapTemp.this);
						btn.setText("start\nover");
					} else {
						Toast.makeText(MapTemp.this, "back to init", Toast.LENGTH_SHORT).show();
						startActivity(new Intent(MapTemp.this, MainPage.class));
						finish();
					}
				}
			});
		}
	}
	
	private void isMapAvailable() {
		if (gmap == null) {
			gmap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			if (gmap != null) {
				setUpMap();
			}
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
	
	@Override
	protected void onStart() {
		super.onStart();
		if (locationClient != null) {
			locationClient.connect();
		}
	}
	
	@Override
	protected void onStop() {
		
		if (locationClient != null) {
			if (locationClient.isConnected()) {
				// stop update
				locationClient.removeLocationUpdates(MapTemp.this);
			}
			locationClient.disconnect();
		}
		
		super.onStop();
	}
	
	@Override
	public void onConnected(Bundle arg0) {
		
		locationClient.requestLocationUpdates(locationRequest, MapTemp.this);
		
		Toast.makeText(MapTemp.this, "connected! request sent!", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onDisconnected() {
		
		Toast.makeText(MapTemp.this, "disconnected! byebye!", Toast.LENGTH_SHORT).show();
		
	}
	
	@Override
	public void onLocationChanged(Location location) {
		
		onLocationChangedListener.onLocationChanged(location);
		
		new AsyncTask<Location, Void, Integer>() {
			
			private final int INIT_SUCCESS = 0;
			private final int INIT_ERROR = 1;
			private final int NO_INTERNET = 2;
			
			@Override
			protected Integer doInBackground(Location... params) {
				
				if (!Utility.isInternetAvailable(MapTemp.this)) {
					return NO_INTERNET;
				} else {
					
					String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing?action=upload&token="
							+ token;
					HttpClient client = null;
					
					try {
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
						url = url + "&location=" + array.toString();
						
						client = Utility.getHttpsClient();
						HttpGet request = new HttpGet();
						request.setURI(new URI(url));
						HttpResponse response = client.execute(request);
						
						if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
							
							BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
									.getContent()));
							
							String readInputLine = null;
							readInputLine = in.readLine();
							
							JSONObject result = new JSONObject(new JSONTokener(readInputLine));
							in.close();
							
							if (result.getInt("status_code") == HttpStatus.SC_OK) {
								pref.edit().putString("token", result.getString("token"))
										.putString("url", result.getString("url")).commit();
								return INIT_SUCCESS;
							}
						}
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					} finally {
						if (client != null) {
							client.getConnectionManager().shutdown();
						}
					}
				}
				return INIT_ERROR;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				switch (result) {
				case INIT_SUCCESS:
					//good
					break;
				case NO_INTERNET:
					//save the point for future upload
					break;
				case INIT_ERROR:
				default:
					//need to save this point for future upload
					break;
				}
			};
		}.execute(location);
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		
		if (connectionResult.hasResolution()) {
			try {
				
				connectionResult.startResolutionForResult(this, 1732);
				
			} catch (IntentSender.SendIntentException e) {
				
				e.printStackTrace();
			}
		} else {
			
			showErrorDialog(connectionResult.getErrorCode());
		}
		
	}
	
	private void showErrorDialog(int errorCode) {
		
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, 1733);
		
		if (errorDialog != null) {
			
			ErrorDialogFragment errorFragment = new ErrorDialogFragment();
			
			errorFragment.setDialog(errorDialog);
			
			errorFragment.show(getSupportFragmentManager(), "lol error");
		}
	}
	
	public static class ErrorDialogFragment extends DialogFragment {
		
		private Dialog mDialog;
		
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}
		
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}
}