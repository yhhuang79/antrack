package tw.plash.antrack.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import tw.plash.antrack.Utility;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import android.widget.Toast;

public class InitializeConnection extends AsyncTaskLoader<String> {
	
	private final int NO_INTERNET = 0;
	private final int PARAMETER_ERROR = 1;
	private final int CONNECTION_ERROR = 2;
	private final int ALL_GOOD = 3;
	
	private Context context;
	private SharedPreferences preference;
	private String userid;
	private String tokenurl;
	
	public InitializeConnection(Context context, SharedPreferences preference) {
		super(context);
		this.context = context;
		this.preference = preference;
		this.tokenurl = null;
		userid = preference.getString("userid", "invaliduserid");
	}
	
	
	@Override
	protected Integer doInBackground(Void... params) {
		if (!Utility.isInternetAvailable(context)) {
			// no internet, prompt user to check internet service and try again
			// later
			return NO_INTERNET;
		} else if (userid.contains("invaliduserid")) {
			return PARAMETER_ERROR;
		} else {
			// we have internet, now do the "initialize" connection
			String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing?action=initialize&userid=" + userid;
			Log.e("antrack.map", "init connection, url=" + url);
			
			HttpClient client = null;
			
			try {
				
				client = Utility.getHttpsClient();
				HttpGet request = new HttpGet(url);
				HttpResponse response = client.execute(request);
				
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					
					BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
					
					String readInputLine = null;
					readInputLine = in.readLine();
					
					JSONObject result = new JSONObject(new JSONTokener(readInputLine));
					in.close();
					
					Log.e("antrack.map", "init connection, result=" + result.toString());
					
					String token = result.getString("token");
					String sharingUrl = result.getString("url");
					
					preference.edit().putString("token", token).putString("url", sharingUrl).commit();
					
					return ALL_GOOD;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
				
				Log.e("antrack.map", "init connection, finally");
				
				if (client != null) {
					client.getConnectionManager().shutdown();
				}
			}
		}
		return CONNECTION_ERROR;
	}
	
	@Override
	public String loadInBackground() {
		// TODO Auto-generated method stub
		return null;
	};
	
	@Override
	public void deliverResult(String data) {
		if(isStarted()){
			super.deliverResult(data);
		}
	}
	
	@Override
	protected void onStartLoading() {
		if(tokenurl != null){
			deliverResult(tokenurl);
		}
		if(takeContentChanged() || tokenurl == null){
			forceLoad();
		}
	}
	
	@Override
	protected void onStopLoading() {
		cancelLoad();
	}
	
	@Override
	protected void onReset() {
		super.onReset();
		
		onStopLoading();
		
		if(tokenurl != null){
			tokenurl = null;
		}
	}
}
