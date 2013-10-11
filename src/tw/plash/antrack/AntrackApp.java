package tw.plash.antrack;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import android.content.Context;

public class AntrackApp {
	
	private static AntrackApp instance;
	
	private AntrackApi mApi;
	
	private AntrackApp(Context context) {
		
		RequestQueue queue = Volley.newRequestQueue(context);
		mApi = new AntrackApi(queue);
		
	}
	
	public AntrackApi getApi(){
		return mApi;
	}
	
	public static synchronized AntrackApp getInstance(Context context){
		if(instance == null){
			instance = new AntrackApp(context.getApplicationContext());
		}
		return instance;
	}
}
