package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import tw.plash.antrack.connection.InitializeConnection;
import tw.plash.antrack.connection.StopConnection;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class Map extends FragmentActivity implements ConnectionResultCallback{
	
	private SharedPreferences preference;
	
	private GoogleMap gmap;
	private MapDrawer mapDrawer;
	
	private Button controlButton;
	private TextView latitudeField;
	private TextView longitudeField;
	private Chronometer durationTimer;
	private TextView distanceField;
	private TextView accuracyField;
	private TextView speedField;
	private TextView numberOfWatcherField;
	
	private OnLocationChangedListener onLocationChangedListener;
	
	private Messenger messengerToService = null;
	private boolean mIsBound;
	private final Messenger messengerFromService = new Messenger(new IncomingHandler());
	
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			Location location = bundle.getParcelable("location");
			TripStatictics stats = (TripStatictics) bundle.getSerializable("stats");
			ArrayList<Location> locations = (ArrayList<Location>) msg.getData().getSerializable("trajectory");
			switch (msg.what) {
			case IPCMessages.SYNC_CURRENT_TRAJECTORY:
				Log.w("map.incominghandler", "SYNC_CURRENT_TRAJECTORY");
				if(!locations.isEmpty()){
					clearMapAndDrawSyncedTrajectory(locations);
				}
				break;
			case IPCMessages.NEW_LOCATION_UPDATE:
				Log.w("map.incominghandler", "NEW_LOCATION_UPDATE");
				onLocationChangedListener.onLocationChanged(location);
				updateDashboard(location, stats);
				if(Locator.isSharingLocation()){
					mapDrawer.addLocation(location);
				}
				break;
			case IPCMessages.SHARING_SUMMARY_REPLY:
				Log.w("map.incominghandler", "SHARING_SUMMARY_REPLY");
				setupSharingSummaryDialog(stats);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private void clearMapAndDrawSyncedTrajectory(List<Location> locations){
		mapDrawer.clearMap();
		mapDrawer.drawLocations(locations);
	}
	
	private void updateDashboard(Location location, TripStatictics stats){
		latitudeField.setText(String.format("%9.5f", location.getLatitude()));
		longitudeField.setText(String.format("%9.5f", location.getLongitude()));
		if(stats != null){
			distanceField.setText(stats.getDistance());
			numberOfWatcherField.setText(stats.getNumberOfWatcher());
		}
		accuracyField.setText(String.format("%.1f", location.getAccuracy()));
		speedField.setText(String.format("%.2f", location.getSpeed()));
	}
	
	private void setupSharingSummaryDialog(TripStatictics statsToShow){
		SharingSummaryDialog summary = new SharingSummaryDialog(Map.this);
		statsToShow.setBaseTimeFromChronometer(durationTimer.getBase());
		summary.setTripStatistics(statsToShow);
		summary.show();
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			messengerToService = new Messenger(service);
			try {
				Message msg = Message.obtain(null, IPCMessages.ACTIVITY_REGISTER);
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
		
		setupDashboard();
	}
	
	private void setupMapIfNotAvailable() {
		if (gmap == null) {
			gmap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			mapDrawer = new MapDrawer(gmap);
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
			controlButton.setText(R.string.control_button_stop);
		} else{
			controlButton.setText(R.string.control_button_start);
		}
		controlButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (shouldStopSharing()) {
					showConfirmStopSharingDialog();
				} else {
					prepareToStartSharing();
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
	
	private void showConfirmStopSharingDialog(){
		new AlertDialog.Builder(Map.this)	
		.setMessage("are you sure you want to stop sharing?")
		.setPositiveButton("yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				prepareToStopSharing();
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
	
	private void prepareToStopSharing(){
		Toast.makeText(Map.this, "sharing is STOPPED", Toast.LENGTH_SHORT).show();
		durationTimer.stop();
		controlButton.setText(R.string.control_button_start);
		
		sendMessageToService(IPCMessages.SHARING_SUMMARY_REQUEST);
		
		executeStopSharingConnection();
	}
	
	private void executeStopSharingConnection() {
		new StopConnection(Map.this, preference).execute();
	}
	
	private void prepareToStartSharing(){
		mapDrawer.clearMap();
		setupStartSharingConnection();
	}
	
	private void setupStartSharingConnection(){
		new InitializeConnection(Map.this, preference, Map.this).execute();
	}
	
	private void setupDashboard(){
		latitudeField = (TextView) findViewById(R.id.latitude_field);
		longitudeField = (TextView) findViewById(R.id.longitude_field);
		durationTimer = (Chronometer) findViewById(R.id.duration_field);
		distanceField = (TextView) findViewById(R.id.distance_field);
		accuracyField = (TextView) findViewById(R.id.accuracy_field);
		speedField = (TextView) findViewById(R.id.speed_field);
		numberOfWatcherField = (TextView) findViewById(R.id.number_of_watchers);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startServiceIfNotAlreadyRunning();
		bindToService();
		if(Locator.isSharingLocation()){
			durationTimer.setBase(preference.getLong("CHRONOMETER", 0L));
			durationTimer.start();
		}
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
		if(Locator.isSharingLocation()){
			preference.edit().putLong("CHRONOMETER", durationTimer.getBase()).commit();
			durationTimer.stop();
		}
	}
	
	void unbindFromService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (messengerToService != null) {
				try {
					Message msg = Message.obtain(null, IPCMessages.ACTIVITY_DEREGISTER);
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

	@Override
	public void allGood() {
		durationTimer.setBase(SystemClock.elapsedRealtime());
		durationTimer.start();
		controlButton.setText(R.string.control_button_stop);
		// should notify service to show notification and keep running...
		sendMessageToService(IPCMessages.START_SHARING);
		
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Hey! it's me!");
		sendIntent.putExtra(Intent.EXTRA_TEXT,
				"Click on the link to follow my lead..." + preference.getString("url", ""));
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendIntent, "Share via..."));
	}
	
	@Override
	public void setFollowerCount(int count) {
	}
}