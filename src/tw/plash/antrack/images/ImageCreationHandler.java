package tw.plash.antrack.images;

import tw.plash.antrack.ActionHandler;
import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.util.IPCMessages;
import android.content.Intent;
import android.util.Log;

public class ImageCreationHandler extends ActionHandler{
	
	public ImageCreationHandler(AntrackApp app) {
		super(app);
	}
	
	public void execute(Intent intent){
		String path = intent.getStringExtra(IPCMessages.LB_EXTRA_IMAGE_PATH);
		int code = intent.getIntExtra(IPCMessages.LB_EXTRA_REQUEST_CODE, -1);
		if(code >= 0){
			long result = app.getDbhelper().insertImageMarkerPath(code, path);
			Log.d("tw.service", "added image marker path into DB row no." + result);
		} else{
			Log.e("tw.service", "received invalid code: " + code + " at image creation");
		}
	}
}
