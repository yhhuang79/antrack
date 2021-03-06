package tw.plash.antrack;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tw.plash.antrack.connection.EncodedRequest;
import tw.plash.antrack.connection.MultipartRequest;
import tw.plash.antrack.images.ImageMarker;
import tw.plash.antrack.location.AntrackLocation;
import tw.plash.antrack.util.Utility;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonObjectRequest;

public class AntrackApi {
	
	private final RequestQueue mResutstQueue;
	private final String baseUrl = "https://plash.iis.sinica.edu.tw:8080/LocationSharing";
	
	public AntrackApi(RequestQueue queue) {
		mResutstQueue = queue;
	}
	
	public Request<?> initialize(String uuid, String username, String timestamp, Listener<JSONObject> listener, ErrorListener errorListener){
		String param = "?action=initialize&uuid=" + uuid + "&username=" + Utility.encode(username) + "&timestamp=" + Utility.encode(timestamp);
		String url = baseUrl + param; //make sure there's no whitespace or anything weird
		
		JsonObjectRequest req = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);
		req.setRetryPolicy(new DefaultRetryPolicy(3000, 3, 0.5f));
		
		return mResutstQueue.add(req);
	}
	
	public Request<?> upload(String token, List<AntrackLocation> locations, Listener<JSONObject> listener, ErrorListener errorListener) throws JSONException{
		if(token == null){
			return null;
		}
		String url = baseUrl;
		
		List<NameValuePair> param = new ArrayList<NameValuePair>();
		param.add(new BasicNameValuePair("action", "upload"));
		param.add(new BasicNameValuePair("token", token));
		
		JSONArray array = new JSONArray();
		for(AntrackLocation location : locations){
			JSONObject obj = new JSONObject();
			obj.put("latitude", location.getLocation().getLatitude());
			obj.put("longitude", location.getLocation().getLongitude());
			obj.put("altitude", location.getLocation().getAltitude());
			obj.put("accuracy", location.getLocation().getAccuracy());
			obj.put("speed", location.getLocation().getSpeed());
			obj.put("bearing", location.getLocation().getBearing());
			obj.put("location_source", location.getLocation().getProvider());
			obj.put("timestamp", new Timestamp(location.getLocation().getTime()).toString());
			obj.put("todisplay", location.getToDisplay());
			array.put(obj);
		}
		
		param.add(new BasicNameValuePair("location", array.toString()));
		EncodedRequest req = new EncodedRequest(url, param, listener, errorListener);
		req.setRetryPolicy(new DefaultRetryPolicy(3000, 3, 0));
		
		return mResutstQueue.add(req);
	}
	
	public Request<?> uploadImage(String token, ImageMarker im, Listener<JSONObject> listener, ErrorListener errorListener){
		if(token == null){
			return null;
		}
		//new aynctask to do this...pass in the request queue as parameter
		new AsyncImageReqGenerator(mResutstQueue, token, im, listener, errorListener).execute();
		return null;
	}
	
	private class AsyncImageReqGenerator extends AsyncTask<Void, Void, Void>{
		
		private RequestQueue queue;
		private MultipartRequest req;
		private String token;
		private ImageMarker im;
		private Listener<JSONObject> listener;
		private ErrorListener errorListener;
		
		public AsyncImageReqGenerator(RequestQueue queue, String token, ImageMarker im, Listener<JSONObject> listener, ErrorListener errorListener) {
			this.queue = queue;
			this.req = null;
			this.token = token;
			this.im = im;
			this.listener = listener;
			this.errorListener = errorListener;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			String url = "http://plash2.iis.sinica.edu.tw/picture/uploadPicture.php";
			
			List<NameValuePair> param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("action", "uploadPicture"));
			param.add(new BasicNameValuePair("token", token));
//			param.add(new BasicNameValuePair("timestamp", new Timestamp(im.getTime()).toString()));
			param.add(new BasicNameValuePair("latitude",  String.valueOf(im.getLatitude())));
			param.add(new BasicNameValuePair("longitude", String.valueOf(im.getLongitude())));
			File file = new File(im.getPath());
			try {
				req = new MultipartRequest(url, param, file, listener, errorListener);
				req.setRetryPolicy(new DefaultRetryPolicy(60000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if(req != null){
				queue.add(req);
			} else{
				Log.e("tw.plash.antrack", "api: error loading image to upload");
			}
		}
	}
	
	public Request<?> stop(String token, String timestamp, Listener<JSONObject> listener, ErrorListener errorListener){
		if(token == null){
			return null;
		}
		String param = "?action=stop&token=" + token;
		String url = baseUrl + param;
		return mResutstQueue.add(new JsonObjectRequest(Method.GET, url, null, listener, errorListener));
	}
}