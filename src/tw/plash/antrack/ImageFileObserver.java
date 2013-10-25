package tw.plash.antrack;

import android.os.FileObserver;
import android.util.Log;

public class ImageFileObserver extends FileObserver {
	
	private final static int mask = (FileObserver.CREATE | FileObserver.DELETE | FileObserver.MODIFY);
	private final static int testMask = FileObserver.ALL_EVENTS;
	
	private final static String path = "/mnt/sdcard/LINEcamera/";
	
	public ImageFileObserver() {
		super(path, testMask);
	}
	
	@Override
	public void onEvent(int event, String path) {
		switch(event){
		case FileObserver.ACCESS:
			Log.i("tw.fileobsvr", "ACCESS: " + path);
			break;
		case FileObserver.ALL_EVENTS:
			Log.i("tw.fileobsvr", "ALL_EVENTS: " + path);
			break;
		case FileObserver.ATTRIB:
			Log.i("tw.fileobsvr", "ATTRIB: " + path);
			break;
		case FileObserver.CLOSE_NOWRITE:
			Log.i("tw.fileobsvr", "CLOSE_NOWRITE: " + path);
			break;
		case FileObserver.CLOSE_WRITE:
			Log.i("tw.fileobsvr", "CLOSE_WRITE: " + path);
			break;
		case FileObserver.CREATE:
			Log.i("tw.fileobsvr", "CREATE: " + path);
			break;
		case FileObserver.DELETE:
			Log.i("tw.fileobsvr", "DELETE: " + path);
			break;
		case FileObserver.DELETE_SELF:
			Log.i("tw.fileobsvr", "DELETE_SELF: " + path);
			break;
		case FileObserver.MODIFY:
			Log.i("tw.fileobsvr", "MODIFY: " + path);
			break;
		case FileObserver.MOVE_SELF:
			Log.i("tw.fileobsvr", "MOVE_SELF: " + path);
			break;
		case FileObserver.MOVED_FROM:
			Log.i("tw.fileobsvr", "MOVED_FROM: " + path);
			break;
		case FileObserver.MOVED_TO:
			Log.i("tw.fileobsvr", "MOVED_TO: " + path);
			break;
		case FileObserver.OPEN:
			Log.i("tw.fileobsvr", "OPEN: " + path);
			break;
		default:
			Log.i("tw.fileobsvr", "default: " + path);
			break;
		}
	}
	
	@Override
	public void startWatching() {
		super.startWatching();
		Log.i("tw.fileobsvr", "startWatching");
	}
	
	@Override
	public void stopWatching() {
		super.stopWatching();
		Log.i("tw.fileobsvr", "stopWatching");
	}
}
