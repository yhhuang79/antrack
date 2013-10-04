package tw.plash.antrack;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import android.content.Context;

public class AntrackSingleton {
	
	private static AntrackSingleton instance;
	
	private AntrackApi mApi;
	
	private AntrackSingleton(Context context) {
		
		RequestQueue queue = Volley.newRequestQueue(context);
		mApi = new AntrackApi(queue);
		
	}
	
	public AntrackApi getApi(){
		return mApi;
	}
	
	public static synchronized AntrackSingleton getInstance(Context context){
		if(instance == null){
			instance = new AntrackSingleton(context.getApplicationContext());
		}
		return instance;
	}
}
