package tw.plash.antrack;

import java.util.ArrayDeque;
import java.util.Collection;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.SupportMapFragment;

public class Map extends FragmentActivity {
	
	private GoogleMap gmap;
	private Button controlButton;

	private OnLocationChangedListener onLocationChangedListener;
	
	private Messenger messengerToService = null;
	private boolean mIsBound;
	private final Messenger messengerFromService = new Messenger(new IncomingHandler());
	
	private final ArrayDeque<Location> locations = new ArrayDeque<Location>();
	
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Locator.MSG_TRAJECTORY_SYNC:
				//will be called right after binding with service
				Object obj = msg.getData().getParcelable("locations");
				if(obj instanceof Collection<?>){
					locations.clear();
					locations.addAll((Collection<? extends Location>) obj);
				}
				break;
			case Locator.MSG_LOCATION_UPDATE:
				Location location = msg.getData().getParcelable("");
				if(onLocationChangedListener != null){
					onLocationChangedListener.onLocationChanged(location);
				}
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
				Message msg = Message.obtain(null, Locator.MSG_REGISTER_CLIENT);
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
		
		setupMapIfNotAvailable();
		
		setupControlButton();
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
				//send a stopping notification to server
				//request a trajectory summary from service, then stop it
//				sendMessageToService();
				//service should stop itself after replying the trajectory summary
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
	
	private void setupStartSharingConnection(){
		new AsyncTask<Void, Void, Integer>(){
			
			private final int NO_INTERNET_ERROR = 1;
			private final int CONNECTION_ERROR = 2;
			private final int ALL_GOOD = 3;
			
			private ProgressDialog diag;
			
			@Override
			protected void onPreExecute() {
				diag = new ProgressDialog(Map.this);
				diag.setMessage("Contacting AnTrack location\nsharing service...");
				diag.setIndeterminate(true);
				diag.setCancelable(false);
				diag.show();
			};
			
			@Override
			protected Integer doInBackground(Void... params) {
				if(!Utility.isInternetAvailable(Map.this)){
					//no internet, prompt user to check internet service and try again later
					return NO_INTERNET_ERROR;
				} else{
					
					//we have internet, now do the "initialize" connection
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				diag.dismiss();
				switch(result){
				case NO_INTERNET_ERROR:
					
					break;
				case ALL_GOOD:
					controlButton.setText("");
					break;
				case CONNECTION_ERROR:
				default:
					break;
				}
			};
		}.execute();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startServiceIfNotAlreadyRunning();
		bindToService();
	}
	
	private void startServiceIfNotAlreadyRunning() {
		if (!Locator.isRunning()) {
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
	}
	
	void unbindFromService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (messengerToService != null) {
				try {
					Message msg = Message.obtain(null, Locator.MSG_UNREGISTER_CLIENT);
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