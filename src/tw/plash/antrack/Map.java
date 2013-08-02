package tw.plash.antrack;

import java.util.ArrayDeque;
import java.util.Collection;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;

public class Map extends FragmentActivity {
	
	private Context mContext;
	private GoogleMap gmap;
	
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
		
		mContext = this;
		
		setContentView(R.layout.map);
		
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		if(!pref.getBoolean("shareintentsent", false)){
			final Intent sendIntent = getIntent().getExtras().getParcelable("sendintent");
			
			new AsyncTask<Void, Void, Void>(){
				
				private ProgressDialog diag;
				
				@Override
				protected void onPreExecute() {
					diag = new ProgressDialog(mContext);
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
					pref.edit().putBoolean("shareintentsent", true).commit();
				}
			}.execute();
		}
		
		isMapAvailable();
		
		Button stop = (Button) findViewById(R.id.stop);
		stop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Button btn = (Button) v;
				if (btn.getText().toString().contains("stop")) {
					Toast.makeText(Map.this, "stoppp", Toast.LENGTH_SHORT).show();
					btn.setText("start\nover");
					//send a stopping notification to server
					//request a trajectory summary from service, then stop it
					sendMessageToService();
					//service should stop itself after replying the trajectory summary
				} else {
					Toast.makeText(Map.this, "back to init", Toast.LENGTH_SHORT).show();
					startActivity(new Intent(Map.this, MainPage.class));
					finish();
				}
			}
		});
		stop.setVisibility(View.VISIBLE);
	}
	
	private void isMapAvailable() {
		if (gmap == null) {
			gmap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			if (gmap != null) {
				setUpMap();
			}
		}
	}
	
	private OnLocationChangedListener onLocationChangedListener;
	
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
	protected void onResume() {
		super.onResume();
		CheckIfServiceIsRunning();
	}
	
	private void CheckIfServiceIsRunning() {
		// If the service is running when the activity starts, we want to
		// automatically bind to it.
		if (!Locator.isRunning()) {
			startService(new Intent(mContext, Locator.class));
		}
		doBindService();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		doUnbindService();
	}
	
	void doBindService() {
		bindService(new Intent(this, Locator.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}
	
	void doUnbindService() {
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
}
