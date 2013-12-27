package tw.plash.antrack.location;

import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.util.Constants;
import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

public class LocationServiceConnector implements ConnectionCallbacks, OnConnectionFailedListener{
	
	private LocationRequest locationRequest;
	private LocationClient locationClient;
	
	private Context context;
	
	public LocationServiceConnector(Context context) {
		this.context = context;
		setupLocationRequest();
		setupLocationClient();
	}
	
	private void setupLocationRequest() {
		locationRequest = LocationRequest.create();
		locationRequest.setInterval(Constants.LOCATION_INTERVAL);
		locationRequest.setFastestInterval(Constants.LOCATION_FASTEST_INTERVAL);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}
	
	private void setupLocationClient() {
		locationClient = new LocationClient(context, this, this);
		locationClient.connect();
	}
	
	public void stop(){
		if(locationClient.isConnected() || locationClient.isConnecting()){
			locationClient.removeLocationUpdates(AntrackApp.getInstance(context).getLocationHub());
			locationClient.disconnect();
		}
		locationClient = null;
	}
	
	@Override
	public void onConnected(Bundle arg0) {
		locationClient.requestLocationUpdates(locationRequest, AntrackApp.getInstance(context).getLocationHub());
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}
	
	@Override
	public void onDisconnected() {
	}
}