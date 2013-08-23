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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class StopConnection extends AsyncTask<Void, Void, Integer>{
	
	private final int NO_INTERNET = 0;
	private final int PARAMETER_ERROR = 1;
	private final int CONNECTION_ERROR = 2;
	private final int ALL_GOOD = 3;
	
	private ProgressDialog diag;
	private Context context;
	private SharedPreferences preference;
	private String token;
	
	public StopConnection(Context context, SharedPreferences preference) {
		this.context = context;
		this.preference = preference;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		diag = new ProgressDialog(context);
		diag.setMessage("stopping...");
		diag.setIndeterminate(true);
		diag.setCancelable(false);
		diag.show();
		token = preference.getString("token", "invalidtoken");
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		if (!Utility.isInternetAvailable(context)) {
			// no internet, prompt user to check internet service and
			// try again later
			return NO_INTERNET;
		} else if (token.contains("invalidtoken")) {
			return PARAMETER_ERROR;
		} else {
			// we have internet, now do the "initialize" connection
			String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing?action=stop&token=" + token;
			Log.e("antrack.map", "stop connection, url=" + url);
			
			HttpClient client = null;
			
			try {
				
				client = Utility.getHttpsClient();
				HttpGet request = new HttpGet(url);
				HttpResponse response = client.execute(request);
				
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					
					BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
							.getContent()));
					
					String readInputLine = null;
					readInputLine = in.readLine();
					
					JSONObject result = new JSONObject(new JSONTokener(readInputLine));
					in.close();
					
					Log.e("antrack.map", "stop connection, result=" + result.toString());
					
					preference.edit().remove("token").remove("url").commit();
					
					return ALL_GOOD;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
				
				Log.e("antrack.map", "stop connection, finally");
				
				if (client != null) {
					client.getConnectionManager().shutdown();
				}
			}
		}
		return CONNECTION_ERROR;
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		diag.dismiss();
		switch (result) {
		case NO_INTERNET:
			Toast.makeText(context, "NO INTERNET", Toast.LENGTH_LONG).show();
			break;
		case ALL_GOOD:
			Toast.makeText(context, "ALL GOOD", Toast.LENGTH_LONG).show();
			break;
		case PARAMETER_ERROR:
			Toast.makeText(context, "INVALID USERID", Toast.LENGTH_LONG).show();
			break;
		case CONNECTION_ERROR:
		default:
			Toast.makeText(context, "CONNECTION ERROR", Toast.LENGTH_LONG).show();
			break;
		}
	}
}
