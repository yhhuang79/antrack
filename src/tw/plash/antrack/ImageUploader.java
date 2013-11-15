package tw.plash.antrack;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

public class ImageUploader {
	
	private boolean isUploading;
	private boolean canKeepRunning;
	
	private Handler mHandler;
	private AntrackApp app;
	private SharedPreferences preference;
	
	public ImageUploader(AntrackApp app, Handler handler, SharedPreferences preference) {
		this.app = app;
		this.mHandler = handler;
		this.preference = preference;
		this.isUploading = false;
		this.canKeepRunning = true;
	}
	
	private Runnable uploadImageTask = new Runnable() {
		@Override
		public void run() {
			Log.e("tw.uploadImageTask", "start");
			//get one marker and try upload
			ImageMarker imageMarker = app.getDbhelper().getPendingUploadImageMarker();
			if(imageMarker != null){
				final int code = imageMarker.getCode();
				app.getApi()
				.uploadImage(preference.getString("token", null), imageMarker, new Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject obj) {
						Log.e("tw.uploadImageTask", "upload image response " + obj.toString());
						try {
							int statusCode = obj.getInt(Constants.API_RES_KEY_STATUS_CODE);
							if(statusCode == 200 || statusCode == 409){
								//mark as uploaded
								app.getDbhelper().setImageMarkerState(code, Constants.IMAGE_MARKER_STATE.DONE);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						} finally{
							//do it hare to succeed both try and catch block
							startAgain();
						}
						Log.e("tw.uploadImageTask", "upload image response done");
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("tw.uploadImageTask", "upload image response: error= " + error.toString());
						startAgain();
					}
				});
			} else{
				isUploading = false;
			}
		}
		
		private void startAgain(){
			Log.e("tw.uploadImageTask", "start again");
			if(canKeepRunning){
				isUploading = true;
				mHandler.postDelayed(uploadImageTask, 1000);
				Log.e("tw.uploadImageTask", "start again delayed");
			} else{
				isUploading = false;
			}
		}
	};
	
	public void start(){
		if(!isUploading){
			mHandler.post(uploadImageTask);
			isUploading = true;
		}
	}
	
	public void stop(){
		canKeepRunning = false;
		mHandler.removeCallbacks(uploadImageTask);
		mHandler = null;
	}
}
