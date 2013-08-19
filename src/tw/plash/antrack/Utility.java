package tw.plash.antrack;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyStore;

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

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Methods for bit manipulation.
 * 
 * @author Joao Bispo
 */
public class Utility {
	
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
	
	
}
