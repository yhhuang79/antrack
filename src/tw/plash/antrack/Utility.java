package tw.plash.antrack;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
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

import tw.plash.antrack.connection.AndroidSSLSocketFactory;
import android.content.Context;
import android.location.Location;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

/**
 * Methods for bit manipulation.
 * 
 * @author Joao Bispo
 */
public class Utility {
	
	private static final double earthRadiusInMeters = 6371008.7714;
	
	private static final long MASK_16_BITS = 0xFFFFL;
	private static final int MASK_BIT_1 = 0x1;
	
	/**
	 * Paul Hsieh's Hash Function.
	 * 
	 * @param data
	 *            data to hash
	 * @param dataLength
	 *            length of the data, in bytes
	 * @param hashedValue
	 *            previous value of the hash. If it is the start of the method,
	 *            used the length of the data (ex.: 8 bytes).
	 * @return
	 */
	public static int superFastHash(long data, int hash) {
		int tmp;
		// int rem;
		
		// if (len <= 0) {
		// return 0;
		// }
		
		// rem = len & 3;
		// len >>= 2;
		
		// Main Loop
		for (int i = 0; i < 4; i += 2) {
			// Get lower 16 bits
			hash += get16BitsAligned(data, i);
			// Calculate some random value with second-lower 16 bits
			tmp = (get16BitsAligned(data, i + 1) << 11) ^ hash;
			hash = (hash << 16) ^ tmp;
			// At this point, it would advance the data, but since it is
			// restricted
			// to longs (64-bit values), it is unnecessary).
			hash += hash >> 11;
		}
		
		// Handle end cases //
		// There are no end cases, main loop is done in chuncks of 32 bits.
		
		// Force "avalanching" of final 127 bits //
		hash ^= hash << 3;
		hash += hash >> 5;
		hash ^= hash << 4;
		hash += hash >> 17;
		hash ^= hash << 25;
		hash += hash >> 6;
		
		return hash;
	}
	
	/**
	 * Returns 16 bits from the long number.
	 * 
	 * @param data
	 * @param offset
	 *            one of 0 to 3
	 * @return
	 */
	public static int get16BitsAligned(long data, int offset) {
		// Normalize offset
		offset = offset % 4;
		// System.out.println("offset:"+offset);
		// Align the mask
		long mask = MASK_16_BITS << 16 * offset;
		// System.out.println("Mask:"+Long.toHexString(mask));
		// System.out.println("Data:"+Long.toHexString(data));
		
		// Get the bits
		long result = data & mask;
		
		// Put bits in position
		return (int) (result >>> (16 * offset));
	}
	
	public static String getMD5(String input) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(input.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean isInternetAvailable(Context context){
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if(ni != null){ //in airplane mode, there are no "Active Network", networkInfo will be null
			if(ni.isConnected()){
				HttpURLConnection conn = null;
				try {
					URL url = new URL("http://www.google.com");
					conn = (HttpURLConnection) url.openConnection();
					conn.setConnectTimeout(2000);
					conn.getContent(); //if internet connection failed, this line will cause IOException
					return true;
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally{
					if(conn != null){
						conn.disconnect();
					}
				}
			}
		}
		return false;
	}
	
	public static HttpClient getHttpsClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);
			SSLSocketFactory sf = new AndroidSSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			HttpParams params = new BasicHttpParams();
			
			// set connection timeout
			int timeoutConnection = 3000;
			HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);
			// set socket timeout
			int timeoutSocket = 10000;
			HttpConnectionParams.setSoTimeout(params, timeoutSocket);
			
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));
			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return new DefaultHttpClient();
		}
	}
	
	public static String getDurationInSecondsAsFormattedString(long durationInSeconds){
		Double hours = ((double) durationInSeconds) / 60 / 60;
		Double minutes = (hours - hours.intValue()) * 60;
		Double seconds = (minutes - minutes.intValue()) * 60;
		return String.format("%d:%02d:%02d", hours.intValue(), minutes.intValue(), seconds.intValue());
	}
	
	public static boolean isValidLocation(Location location){
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		if(latitude > -90){
			if(latitude < 90){
				if(longitude > -180){
					if(longitude < 180){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static boolean isWithinAccuracyBound(Location previousLocation, Location currentlocation){
		double accuracy = currentlocation.getAccuracy();
		if(getDistance(previousLocation, currentlocation) < accuracy){
			return true;
		} else{
			return false;
		}
	}
	
	private static double getDistance(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
		double dlat = toRad(toLatitude - fromLatitude);
		double dlon = toRad(toLongitude - fromLongitude);
		double latone = toRad(fromLatitude);
		double lattwo = toRad(toLatitude);
		double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) + Math.sin(dlon / 2) * Math.sin(dlon / 2) * Math.cos(latone)
				* Math.cos(lattwo);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return earthRadiusInMeters * c;
	}
	
	public static double getDistance(LatLng fromLatlng, LatLng toLatlng) {
		return getDistance(fromLatlng.latitude, fromLatlng.longitude, toLatlng.latitude, toLatlng.longitude);
	}
	
	public static double getDistance(Location fromLocation, Location toLocation) {
		return getDistance(fromLocation.getLatitude(), fromLocation.getLongitude(), toLocation.getLatitude(),
				toLocation.getLongitude());
	}
	
	private static double toRad(Double degree) {
		return degree / 180 * Math.PI;
	}
	
	public static String encode(String input){
		String output = null;
		try {
			output = URLEncoder.encode(input, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			output = input;
		}
//		Log.e("correct url encoder", "outParam=" + output);
		return output;
	}
	
	public static String getHHMMSSTimeString(long time){
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String timestring = sdf.format(new Date(time));
		sdf = null;
		return timestring;
	}
	
	public static void log(String name, String msg){
		Log.d("tw.plash.antrack." + name, msg);
	}
	
	public static void log(String msg){
		log("", msg);
	}
	
	public static void geoTagPicture(String path, double latitude, double longitude, String timestamp) {
		ExifInterface exif;
		
		try {
			exif = new ExifInterface(path);
			
			//replace whatever is in this column with our standard GPS timestamp
			exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, timestamp);
			
			//get the width and length to calculate whether photo is landscape or portrait
			int width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1);
			int length = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1);
			//check if width/length are larger than the default null value
			if(width > -1 && length > -1){
				if(width >= length){
					//it's a square(rare!) or landscape photo
					exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
				} else{
					//XXX need to check the correct rotation value(90 or 270)
					//it's a portrait photo
					exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
					//not sure it's 90 or 270 degrees when shooting portrait photos
//					exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_270));
				}
			} else{
				//set the orientation to a default value, since we can't get the width/length value from the input photo
				exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
			}
			
			//reformat latitude from DD.DDDDD to DD/MM/SS
			int num1Lat = (int) Math.floor(latitude);
			int num2Lat = (int) Math.floor((latitude - num1Lat) * 60);
			double num3Lat = (latitude - ((double) num1Lat + ((double) num2Lat / 60))) * 3600000;
			//reformat longitude from DD.DDDDD to DD/MM/SS
			int num1Lon = (int) Math.floor(longitude);
			int num2Lon = (int) Math.floor((longitude - num1Lon) * 60);
			double num3Lon = (longitude - ((double) num1Lon + ((double) num2Lon / 60))) * 3600000;
			//set reformatted latitude/longitude values
			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, num1Lat + "/1," + num2Lat + "/1," + num3Lat + "/1000");
			exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, num1Lon + "/1," + num2Lon + "/1," + num3Lon + "/1000");
			//set N/S reference according to latitude value
			if (latitude > 0) {
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
			} else {
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
			}
			//set E/W reference according to longitude value
			if (longitude > 0) {
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
			} else {
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
			}
			//save all exif attributes
			exif.saveAttributes();
		} catch (IOException e) {
			Log.e("ExifEditor Error", "input path: " + path);
			e.printStackTrace();
		}
	}
}
