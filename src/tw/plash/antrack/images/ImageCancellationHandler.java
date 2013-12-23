package tw.plash.antrack.images;

import tw.plash.antrack.ActionHandler;
import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.util.IPCMessages;
import android.content.Intent;
import android.util.Log;

public class ImageCancellationHandler extends ActionHandler{
	private AntrackApp app;
	
	public ImageCancellationHandler(AntrackApp app) {
		super(app);
	}
	
	public void execute(Intent intent){
		int code = intent.getIntExtra(IPCMessages.LB_EXTRA_REQUEST_CODE, -1);
		if(code >= 0){
			//remove it from DB
			int result = app.getDbhelper().removeImageMarker(code);
			//result should be 1, if not, something is wrong
			Log.d("tw.service", "removed " + result + " image marker entries from DB");
		} else{
			Log.e("tw.service", "received invalid code: " + code + " at image cancellation");
		}
	}
}
