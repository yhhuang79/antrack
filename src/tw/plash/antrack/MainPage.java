package tw.plash.antrack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * 
 * This class does four things 1. display initial welcome message and start
 * button 1.1 (future) display facebook/google login button for one click login
 * 
 * 2. display progress circle while connecting to server
 * 
 * 3. display error if connection to server failed 3.1 on successful connection,
 * switch to share targer selection page
 * 
 * 4. clicking on menu button will show a list of actions, including 4.1
 * settings page...not mushc to be done here 4.2 about this app and development
 * team 4.3 legal stuff
 * 
 * @author CSZU
 * 
 */
public class MainPage extends FragmentActivity {
	
	private Context context;
	private String userid;
	private SharedPreferences pref;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		context = this;
		
		pref = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (pref.getBoolean("firsttime", true)) {
			setScreenContent(R.layout.init);
		} else {
			setScreenContent(R.layout.intro);
		}
	}
	
	private void setScreenContent(int layoutID, int messageID) {
		setContentView(layoutID);
		switch (layoutID) {
		case R.layout.init:
			setInitPageContent();
			break;
		case R.layout.intro:
			setIntroPageContent();
			break;
		case R.layout.connect:
			setConnectPageContent();
			break;
		case R.layout.error:
			setErrorPageContent(messageID);
			break;
		case R.layout.share:
			setSharePageContent();
			break;
		}
	}
	
	private void setScreenContent(int layoutID) {
		setScreenContent(layoutID, -1);
	}
	
	private void setInitPageContent() {
		final EditText et = (EditText) findViewById(R.id.nameinput);
		
		Button commit = (Button) findViewById(R.id.submit);
		commit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = et.getEditableText().toString();
				Toast.makeText(context, "name=" + name, Toast.LENGTH_SHORT).show();
				if (name.isEmpty()) {
					et.setError("enter a name la");
				} else {
					pref.edit().putBoolean("firsttime", false).putString("name", name).commit();
					setScreenContent(R.layout.intro);
				}
			}
		});
	}
	
	private void setIntroPageContent() {
		TextView nameField = (TextView) findViewById(R.id.name);
		String name = pref.getString("name", null);
		if(name != null){
			nameField.setText(name);
			nameField.setVisibility(View.VISIBLE);
		}
		
		if (pref.contains("userid")) {
			userid = pref.getString("userid", "-1");
			Toast.makeText(context, "O:" + userid, Toast.LENGTH_LONG).show();
		} else {
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String imei = telephonyManager.getDeviceId();
			String prehash = imei + String.valueOf(System.currentTimeMillis());
			userid = Utility.getMD5(prehash);
			pref.edit().putString("userid", userid).commit();
			Toast.makeText(context, "X:" + userid, Toast.LENGTH_LONG).show();
		}
		
		Button start = (Button) findViewById(R.id.start);
		start.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.connect);
			}
		});
	}
	
	private boolean servicesConnected() {
		
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("antrack.mainpage", "google play service exist");
			
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			// Display an error dialog
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
			if (dialog != null) {
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				errorFragment.setDialog(dialog);
				errorFragment.setCancelable(false);
				errorFragment.show(getSupportFragmentManager(), "AnTrack.MainPage");
			}
			return false;
		}
	}
	
	/**
	 * Define a DialogFragment to display the error dialog generated in
	 * showErrorDialog.
	 */
	public static class ErrorDialogFragment extends DialogFragment {
		
		// Global field to contain the error dialog
		private Dialog mDialog;
		
		/**
		 * Default constructor. Sets the dialog field to null
		 */
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}
		
		/**
		 * Set the dialog to display
		 * 
		 * @param dialog
		 *            An error dialog
		 */
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}
		
		/*
		 * This method must return a Dialog to the DialogFragment.
		 */
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}
	
	private void setConnectPageContent() {
		TextView nameField = (TextView) findViewById(R.id.name);
		String name = pref.getString("name", null);
		if(name != null){
			nameField.setText(name);
			nameField.setVisibility(View.VISIBLE);
		}
		
		// if google play service is not available, show error page
		if (!servicesConnected()) {
			setScreenContent(R.layout.error, R.string.errormsg_google_play_service_unavailable);
		} else {
			// google play service available, send init request to server
			new AsyncTask<Void, Void, Integer>() {
				
				private final int INIT_SUCCESS = 0;
				private final int INIT_ERROR = 1;
				private final int NO_INTERNET = 2;
				
				@Override
				protected Integer doInBackground(Void... params) {
					
					if (!Utility.isInternetAvailable(context)) {
						return NO_INTERNET;
					} else {
//						userid = "-1";
						
						String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing?action=initialize&userid="
								+ userid;
						HttpClient client = null;
						
						try {
							client = Utility.getHttpsClient();
							HttpGet request = new HttpGet();
							request.setURI(new URI(url));
							HttpResponse response = client.execute(request);
							
							if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
								
								BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
										.getContent()));
								
								String readInputLine = null;
								readInputLine = in.readLine();
								
								JSONObject result = new JSONObject(new JSONTokener(readInputLine));
								in.close();
								
								if (result.getInt("status_code") == HttpStatus.SC_OK) {
									pref.edit().putString("token", result.getString("token"))
											.putString("url", result.getString("url")).commit();
									return INIT_SUCCESS;
								}
							}
							
						} catch (IOException e) {
							e.printStackTrace();
						} catch (JSONException e) {
							e.printStackTrace();
						} catch (URISyntaxException e) {
							e.printStackTrace();
						} finally {
							if (client != null) {
								client.getConnectionManager().shutdown();
							}
						}
					}
					return INIT_ERROR;
				}
				
				@Override
				protected void onPostExecute(Integer result) {
					switch (result) {
					case INIT_SUCCESS:
						setScreenContent(R.layout.share);
						break;
					case NO_INTERNET:
						setScreenContent(R.layout.error, R.string.errormsg_internet_not_available);
						break;
					case INIT_ERROR:
					default:
						setScreenContent(R.layout.error, R.string.errormsg_server_error);
						break;
					}
				};
			}.execute();
		}
	}
	
	private void setErrorPageContent(int stringID) {
		TextView nameField = (TextView) findViewById(R.id.name);
		String name = pref.getString("name", null);
		if(name != null){
			nameField.setText(name);
			nameField.setVisibility(View.VISIBLE);
		}
		
		TextView errorMsg = (TextView) findViewById(R.id.errorMessage);
		errorMsg.setText(stringID);
		
		Button tryagain = (Button) findViewById(R.id.start);
		tryagain.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.connect);
			}
		});
	}
	
	private void setSharePageContent() {
		// retrieve the share via action receivers and show them in a list view
		final ShareActionListAdapter adapter = new ShareActionListAdapter(context, getSendActionReceipient());
		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent sendIntent = new Intent(Intent.ACTION_SEND);
				
				ResolveInfo info = (ResolveInfo) adapter.getItem(position);
				String packageName = info.activityInfo.packageName;
				String className = info.activityInfo.name;
				sendIntent.setClassName(packageName, className);
				
				sendIntent.setType("text/plain");
				sendIntent.putExtra(Intent.EXTRA_TEXT,
						"Click on this link to follow my lead! " + pref.getString("url", ""));
				sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Hey, It's " + pref.getString("name", "me")
						+ "! Check this out!");
				sendIntent.putExtra("exit_on_sent", true);
				
				Intent intent = new Intent(context, Map.class);
				intent.putExtra("sendintent", sendIntent);
				
				startActivity(intent);
				finish();
			}
		});
	}
	
	private List<ResolveInfo> getSendActionReceipient() {
		PackageManager pm = context.getPackageManager();
		Intent send = new Intent(Intent.ACTION_SEND);
		String uriText = "mailto:" + Uri.encode("email@gmail.com") + "?subject=" + Uri.encode("the subject") + "&body="
				+ Uri.encode("the body of the message");
		Uri uri = Uri.parse(uriText);
		send.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
		send.setData(uri);
		send.setType("text/plain");
		
		return pm.queryIntentActivities(send, 0);
	}
}
