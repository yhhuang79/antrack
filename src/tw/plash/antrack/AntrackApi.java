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
import android.content.Context;
import android.location.Location;
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
		req.setRetryPolicy(new DefaultRetryPolicy(3000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		
		return mResutstQueue.add(req);
	}
	
	/*
	public Request<?> initialize(String userid, String username, String timestamp, Listener<JSONObject> listener, ErrorListener errorListener){
		String param = "?action=initialize&userid=" + Utility.encode(userid) + "&username=" + Utility.encode(username) + "&timestamp=" + Utility.encode(timestamp);
		String url = baseUrl + param; //make sure there's no whitespace or anything weird
		return mResutstQueue.add(new JsonObjectRequest(Method.GET, url, null, listener, errorListener));
	}
	*/
	
	public Request<?> upload(String token, Location location, boolean toDisplay, Listener<JSONObject> listener, ErrorListener errorListener) throws JSONException{
		if(token == null){
			return null;
		}
		String url = baseUrl;
		
		List<NameValuePair> param = new ArrayList<NameValuePair>();
		param.add(new BasicNameValuePair("action", "upload"));
		param.add(new BasicNameValuePair("token", token));
		
		JSONArray array = new JSONArray();
		JSONObject obj = new JSONObject();
		obj.put("latitude", location.getLatitude());
		obj.put("longitude", location.getLongitude());
		obj.put("altitude", location.getAltitude());
		obj.put("accuracy", location.getAccuracy());
		obj.put("speed", location.getSpeed());
		obj.put("bearing", location.getBearing());
		obj.put("location_source", location.getProvider());
		obj.put("timestamp", new Timestamp(location.getTime()).toString());
		obj.put("todisplay", toDisplay? 1 : 0);
		array.put(obj);
		
		param.add(new BasicNameValuePair("location", array.toString()));
		Log.w("tw.", "upload: " + param.toString());
		EncodedRequest req = new EncodedRequest(url, param, listener, errorListener);
		req.setRetryPolicy(new DefaultRetryPolicy(3000, 10, 1.5f));
		
		return mResutstQueue.add(req);
	}
	
	public Request<?> uploadImage(String token, ImageMarker im, Listener<JSONObject> listener, ErrorListener errorListener) throws JSONException{
		if(token == null){
			return null;
		}
		String url = "http://plash2.iis.sinica.edu.tw/picture/uploadPicture.php";
		
		List<NameValuePair> param = new ArrayList<NameValuePair>();
		param.add(new BasicNameValuePair("action", "uploadPicture"));
		param.add(new BasicNameValuePair("token", token));
//		param.add(new BasicNameValuePair("timestamp", new Timestamp(im.getTime()).toString()));
		param.add(new BasicNameValuePair("latitude",  String.valueOf(im.getLatitude())));
		param.add(new BasicNameValuePair("longitude", String.valueOf(im.getLongitude())));
		File file = new File(im.getPath());
		MultipartRequest req = null;
		try {
			req = new MultipartRequest(url, param, file, listener, errorListener);
			req.setRetryPolicy(new DefaultRetryPolicy(10000, 10, 1.5f));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return mResutstQueue.add(req);
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