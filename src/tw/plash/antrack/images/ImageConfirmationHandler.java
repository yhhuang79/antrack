package tw.plash.antrack.images;

import tw.plash.antrack.ActionHandler;
import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.util.IPCMessages;
import android.content.Intent;
import android.util.Log;

public class ImageConfirmationHandler extends ActionHandler{
	private ImageUploader imageUploader;
	
	public ImageConfirmationHandler(AntrackApp app, ImageUploader imageUploader) {
		super(app);
		this.imageUploader = imageUploader;
	}
	
	public void execute(Intent intent){
		int code = intent.getIntExtra(IPCMessages.LB_EXTRA_REQUEST_CODE, -1);
		if(code >= 0){
			int result = app.getDbhelper().insertImageMarkerLocation(code, app.getLocationHub().getLatestLocation());
			Log.d("tw.service", "added " + result + " image marker location(s) into DB");
			imageUploader.start();
		} else{
			Log.e("tw.service", "received invalid code: " + code + " at image confirmation");
		}
	}
}
