package tw.plash.antrack.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import tw.plash.antrack.Utility;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

public class UploadConnection extends AsyncTask<Location, Void, Void> {
	
	private Context context;
	private SharedPreferences preference;
	private String token;
	
	public UploadConnection(Context context, SharedPreferences preference) {
		this.context = context;
		this.preference = preference;
	}
	
	@Override
	protected void onPreExecute() {
		Log.e("Locator.onLocationChanged", "upload: token=" + token);
		token = preference.getString("token", "invalidtoken");
	};
	
	@Override
	protected Void doInBackground(Location... params) {
		
		if(!Utility.isInternetAvailable(context)){
			return null;
		}
		
		if (token.equals("invalidtoken")) {
			return null;
		}
		
		String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing";
		HttpClient client = null;
		
		try {
			
			client = Utility.getHttpsClient();
			HttpPost request = new HttpPost(url);
			
			JSONObject obj = buildJSONObjectFromLocation(params[0]);
			
			JSONArray array = new JSONArray();
			array.put(obj);
			
			List<NameValuePair> param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("action", "upload"));
			param.add(new BasicNameValuePair("token", token));
			param.add(new BasicNameValuePair("location", array.toString()));
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(param, HTTP.UTF_8);
			request.setEntity(entity);
			
			HttpResponse response = client.execute(request);
			
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				
				String readInputLine = null;
				readInputLine = in.readLine();
				
				JSONObject result = new JSONObject(new JSONTokener(readInputLine));
				in.close();
				
				int numberOfWatchers = result.getInt("number_of_watcher");
				
				Log.e("Locator.onLocationChanged", "upload: result=" + result.toString());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			Log.e("Locator.onLocationChanged", "upload: finally");
			if (client != null) {
				client.getConnectionManager().shutdown();
			}
		}
		return null;
	}
	
	private JSONObject buildJSONObjectFromLocation(Location location) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("latitude", location.getLatitude());
		obj.put("longitude", location.getLongitude());
		obj.put("altitude", location.getAltitude());
		obj.put("accuracy", location.getAccuracy());
		obj.put("speed", location.getSpeed());
		obj.put("bearing", location.getBearing());
		obj.put("location_source", location.getProvider());
		obj.put("timestamp", new Timestamp(location.getTime()).toString());
		return obj;
	}
}