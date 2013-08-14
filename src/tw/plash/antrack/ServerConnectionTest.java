package tw.plash.antrack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class ServerConnectionTest {
	
	private static String token;
	final private static String userid = "abcdefghijklmnopqratuvwxyz012345";
	
	public static void initialize(final Context context){
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				
					
					String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing?action=initialize&userid="
							+ userid;
					HttpClient client = null;
					
					try {
						
						client = Utility.getHttpsClient();
						Log.e(userid, "initialize: url=" + url);
						HttpGet request = new HttpGet(url);
						HttpResponse response = client.execute(request);
						
						if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
							
							BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
									.getContent()));
							
							String readInputLine = null;
							readInputLine = in.readLine();
							
							JSONObject result = new JSONObject(new JSONTokener(readInputLine));
							in.close();
							
							token = result.getString("token");
							
							Log.e(userid, "initialize: result=" + result.toString());
							Log.e(userid, "initialize: statuscode=" + result.getInt("status_code"));
							Log.e(userid, "initialize: message=" + result.getString("message"));
							Log.e(userid, "initialize: token=" + token);
							Log.e(userid, "initialize: userid=" + result.getString("userid"));
							Log.e(userid, "initialize: url=" + result.getString("url"));
						}
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					} finally {
						Log.e(userid, "initialize: finally");
						if (client != null) {
							client.getConnectionManager().shutdown();
						}
					}
				return null;
			}
		}.execute();
	}
	
	public static void upload(final Context context){
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				
					
					String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing";
					HttpClient client = null;
					
					try {
						
						client = Utility.getHttpsClient();
						HttpPost request = new HttpPost(url);
						
						JSONObject obj = new JSONObject();
						obj.put("latitude", 1.23456);
						obj.put("longitude", 12.3456);
						obj.put("altitude", 123.456);
						obj.put("accuracy", 1234.56);
						obj.put("speed", 12345.6);
						obj.put("bearing", 123456);
						obj.put("location_source", "testingonly");
						obj.put("timestamp", new Timestamp(new Date().getTime()).toString());
						JSONArray array = new JSONArray();
						array.put(obj);
						
						List<NameValuePair> param = new ArrayList<NameValuePair>();
						param.add(new BasicNameValuePair("action", "upload"));
						Log.e(userid, "upload: token=" + token);
						param.add(new BasicNameValuePair("token", token));
						param.add(new BasicNameValuePair("location", array.toString()));
						Log.e(userid, "upload: NameValuePair=" + param.toString());
						UrlEncodedFormEntity entity = new UrlEncodedFormEntity(param, HTTP.UTF_8);
						request.setEntity(entity);
						
						HttpResponse response = client.execute(request);
						
						if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
							
							BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
									.getContent()));
							
							String readInputLine = null;
							readInputLine = in.readLine();
							
							JSONObject result = new JSONObject(new JSONTokener(readInputLine));
							in.close();
							
							Log.e(userid, "upload: result=" + result.toString());
							Log.e(userid, "upload: statuscode=" + result.getInt("status_code"));
							Log.e(userid, "upload: message=" + result.getString("message"));
						}
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					} finally {
						Log.e(userid, "upload: finally");
						if (client != null) {
							client.getConnectionManager().shutdown();
						}
					}
				return null;
			}
		}.execute();
	}
	
	public static void stop(final Context context){
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				
					
					String url = "https://plash.iis.sinica.edu.tw:8080/LocationSharing?action=stop&token="
							+ token;
					HttpClient client = null;
					
					try {
						
						client = Utility.getHttpsClient();
						Log.e(userid, "stop: url=" + url);
						HttpGet request = new HttpGet(url);
						HttpResponse response = client.execute(request);
						
						if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
							
							BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
									.getContent()));
							
							String readInputLine = null;
							readInputLine = in.readLine();
							
							JSONObject result = new JSONObject(new JSONTokener(readInputLine));
							in.close();
							
							Log.e(userid, "stop: result=" + result.toString());
							Log.e(userid, "stop: statuscode=" + result.getInt("status_code"));
							Log.e(userid, "stop: message=" + result.getString("message"));
						}
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					} finally {
						Log.e(userid, "stop: finally");
						if (client != null) {
							client.getConnectionManager().shutdown();
						}
					}
				return null;
			}
		}.execute();
	}
}
