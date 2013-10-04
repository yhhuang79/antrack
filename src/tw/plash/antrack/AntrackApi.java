package tw.plash.antrack;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

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
	
	public Request<?> initialize(String userid, String username, String timestamp, Listener<JSONObject> listener, ErrorListener errorListener){
		String param = "?action=initialize&userid=" + Utility.encode(userid) + "&username=" + Utility.encode(username) + "&timestamp=" + Utility.encode(timestamp);
		String url = baseUrl + param; //make sure there's no whitespace or anything weird
		return mResutstQueue.add(new JsonObjectRequest(Method.GET, url, null, listener, errorListener));
	}
	
	public Request<?> upload(String token, Location location, boolean toDisplay, Listener<JSONObject> listener, ErrorListener errorListener) throws JSONException{
		String url = baseUrl;
		JSONObject postBody = new JSONObject();
		
		postBody.put("action", "upload");
		postBody.put("token", token);
		
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
		
		postBody.put("location", array.toString());
		
		return mResutstQueue.add(new JsonObjectRequest(Method.POST, url, postBody, listener, errorListener));
	}
	
//	public Request<?> upload(String token, Map<Location, Integer> locations, Listener<JSONObject> listener, ErrorListener errorListener) throws JSONException{
//		String url = baseUrl;
//		JSONObject postBody = new JSONObject();
//		
//		postBody.put("action", "upload");
//		postBody.put("token", token);
//		
//		JSONArray array = new JSONArray();
//		Iterator<Location> iterator = locations.keySet().iterator();
//		while(iterator.hasNext()){
//			Location location = iterator.next();
//			JSONObject obj = new JSONObject();
//			obj.put("latitude", location.getLatitude());
//			obj.put("longitude", location.getLongitude());
//			obj.put("altitude", location.getAltitude());
//			obj.put("accuracy", location.getAccuracy());
//			obj.put("speed", location.getSpeed());
//			obj.put("bearing", location.getBearing());
//			obj.put("location_source", location.getProvider());
//			obj.put("timestamp", new Timestamp(location.getTime()).toString());
//			obj.put("todisplay", locations.get(location));
//			array.put(obj);
//		}
//		postBody.put("location", array.toString());
//		
//		return mResutstQueue.add(new JsonObjectRequest(Method.POST, url, postBody, listener, errorListener));
//	}
	
	public Request<?> stop(String token, String timestamp, Listener<JSONObject> listener, ErrorListener errorListener){
		String param = "?action=stop&token=" + token;
		String url = baseUrl + param;
		return mResutstQueue.add(new JsonObjectRequest(Method.GET, url, null, listener, errorListener));
	}
}