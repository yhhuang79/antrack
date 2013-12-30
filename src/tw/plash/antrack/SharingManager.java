package tw.plash.antrack;

import tw.plash.antrack.location.LocationUploader;
import android.content.Context;
import android.preference.PreferenceManager;

public class SharingManager {
	
	private AntrackApp app;
	private LocationUploader locationUploader;
	
	public SharingManager(Context context) {
		app = AntrackApp.getInstance(context);
		locationUploader = new LocationUploader(app, PreferenceManager.getDefaultSharedPreferences(context));
	}
	
	public void startSharing(){
		app.getLocationHub().addObserver(app.getStatsKeeper());
		app.getLocationHub().addObserver(app.getDbhelper());
		app.getLocationHub().addObserver(locationUploader);
		app.getStatsKeeper().initStats();
	}
	
	public void stopSharing(){
		app.getLocationHub().deleteObserver(app.getStatsKeeper());
		app.getLocationHub().deleteObserver(app.getDbhelper());
		app.getLocationHub().deleteObserver(locationUploader);
		app.getStatsKeeper().finalizeStats();
	}
}
