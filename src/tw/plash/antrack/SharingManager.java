package tw.plash.antrack;

import tw.plash.antrack.location.LocationUploader;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

public class SharingManager {
	
	private AntrackApp app;
	private LocationUploader locationUploader;
	
	public SharingManager(Context context) {
		app = AntrackApp.getInstance(context);
		locationUploader = new LocationUploader(app, PreferenceManager.getDefaultSharedPreferences(context));
	}
	
	public void startSharing(){
		Log.e("tw.sharingmanager", "start sharing");
		app.getLocationHub().addObserver(app.getStatsKeeper());
		app.getLocationHub().addObserver(app.getDbhelper());
		app.getLocationHub().addObserver(locationUploader);
	}
	
	public void stopSharing(){
		Log.e("tw.sharingmanager", "stop sharing");
		app.getLocationHub().deleteObserver(app.getStatsKeeper());
		app.getLocationHub().deleteObserver(app.getDbhelper());
		app.getLocationHub().deleteObserver(locationUploader);
	}
}
