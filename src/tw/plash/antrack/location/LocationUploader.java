package tw.plash.antrack.location;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.json.JSONException;
import org.json.JSONObject;

import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.util.Constants;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

public class LocationUploader implements Observer{
	
	private AntrackApp app;
	private SharedPreferences preference;
	
	public LocationUploader(AntrackApp app, SharedPreferences preference) {
		this.app = app;
		this.preference = preference;
	}
	
	@Override
	public void update(Observable observable, Object data) {
		AntrackLocation antrackLocation = (AntrackLocation) data;
		upload(antrackLocation);
	}
	
	private void upload(AntrackLocation aLocation){
		List<AntrackLocation> locations = app.getDbhelper().getAllPendingUploadLocations();
		locations.add(aLocation);
		new Thread(new locationUploadTask(locations)).start();
	}
	
	private class locationUploadTask implements Runnable{
		
		private List<AntrackLocation> locations;
		
		public locationUploadTask(List<AntrackLocation> locations){
			this.locations = locations;
		}
		
		@Override
		public void run() {
			try {
				app.getApi().upload(preference.getString("token", null), locations, new Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject obj) {
						try {
							Log.d("tw.uploadlocation", "locationUploadTask: res= " + obj.toString());
							if(obj.getInt(Constants.API_RES_KEY_STATUS_CODE) == 200){
								Log.d("tw.uploadlocation", "locationUploadTask: success");
								int followers = obj.getInt("number_of_watcher");
								app.getStatsKeeper().getStats().setNumberOfFollowers(followers);
							
							} else{
								addToRetry();
							}
						} catch (JSONException e) {
							e.printStackTrace();
							addToRetry();
						}
					}
				}, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						addToRetry();
					}
				});
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		private void addToRetry(){
			app.getDbhelper().insertPendingUploadLocations(locations);
		}
	};
}