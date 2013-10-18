package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import tw.plash.antrack.connection.EncodedRequest;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

public class AntrackApp {
	
	private static AntrackApp instance;
	
	private AntrackApi mApi;
	private RequestQueue queue;
	private DBHelper dbhelper;
	private List<String> imagepaths;
	private StatsUpdater statsUpdater;
	private ImageMarker imagemarkerholder;
	private int followers;
	
	private AntrackApp(Context context) {
		queue = Volley.newRequestQueue(context, new HurlStack(null, getSSLSocketFactory(), DO_NOT_VERIFY));
		mApi = new AntrackApi(queue);
		dbhelper = new DBHelper(context);
		imagepaths = new ArrayList<String>();
		statsUpdater = new StatsUpdater();
		imagemarkerholder = null;
		followers = 0;
	}
	
	public AntrackApi getApi(){
		return mApi;
	}
	
	public DBHelper getDbhelper(){
		return dbhelper;
	}
	
	public StatsUpdater getStatsUpdater(){
		return statsUpdater;
	}
	
	public void setImageMarker(ImageMarker im){
		imagemarkerholder = im;
	}
	
	public ImageMarker getImageMarker(){
		return imagemarkerholder;
	}
	
	public void commitImageMarker(){
		Utility.log("singleton", imagemarkerholder.getPath() + ", " + imagemarkerholder.getLatitude() + ", " + imagemarkerholder.getLongitude());
		dbhelper.insertImageMarker(imagemarkerholder);
		imagemarkerholder = null;
	}
	
	public void resetImagePaths(){
		imagepaths.clear();
	}
	
	public void addImagePath(String path){
		if(!imagepaths.contains(path)){
			imagepaths.add(path);
		}
	}
	
	public List<String> getImagePaths(){
		return imagepaths;
	}
	
	public void setFollowers(int num){
		followers = num;
	}
	
	public int getFollowers(){
		return followers;
	}
	
	public static synchronized AntrackApp getInstance(Context context){
		if(instance == null){
			instance = new AntrackApp(context.getApplicationContext());
		}
		return instance;
	}
	
	public void cancelAll(Context context){
		queue.cancelAll(context);
	}
	
	private final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};
	
	private javax.net.ssl.SSLSocketFactory getSSLSocketFactory() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(sc != null){
			return sc.getSocketFactory();
		} else{
			return null;
		}
	}
}
