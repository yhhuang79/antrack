package tw.plash.antrack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tw.plash.antrack.images.ImageCancellationHandler;
import tw.plash.antrack.images.ImageConfirmationHandler;
import tw.plash.antrack.images.ImageCreationHandler;
import tw.plash.antrack.images.ImageMarker;
import tw.plash.antrack.images.ImageUploader;
import tw.plash.antrack.location.LocationServiceConnector;
import tw.plash.antrack.util.IPCMessages;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

public class AntrackService extends Service {
	
	private static boolean serviceIsSharing = false;
	
	private AntrackApp app;
	
	private Context context;
	
	private LocationServiceConnector locationServiceConnector;
	private SharingManager sharingManager;
	
	private ImageUploader imageUploader;
	
	private Map<String, ActionHandler> handlers;
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(IPCMessages.LB_START_SHARING)){
				prepareToStartSharing();
			} else if(action.equals(IPCMessages.LB_STOP_SHARING)){
				prepareToStopSharing();
			} else {
				ActionHandler actionHandler = lookupHandlerBy(action);
				if(actionHandler != null){
					actionHandler.execute(intent);
				}
			}
		}
	};
	
	private ActionHandler lookupHandlerBy(String action){
		return handlers.get(action);
	}
	
	public void createHandlers(){
		handlers = new HashMap<String, ActionHandler>();
		handlers.put(IPCMessages.LB_IMAGE_CREATE, new ImageCreationHandler(app));
		handlers.put(IPCMessages.LB_IMAGE_CONFIRM, new ImageConfirmationHandler(app, imageUploader));
		handlers.put(IPCMessages.LB_IMAGE_CANCEL, new ImageCancellationHandler(app));
	}
	
	private Messenger mLocationSender;
	private final Messenger mReceiver = new Messenger(new IncomingHandler());
	
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case IPCMessages.REGISTER:
				switch(msg.arg1){
				case IPCMessages.MAP_ACTIVITY:
					mLocationSender = msg.replyTo;
					if (isSharingLocation()) {
						syncStuff();
					}
					break;
				}
				break;
			case IPCMessages.DEREGISTER:
				if (msg.replyTo == mLocationSender) {
					mLocationSender = null;
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private void syncStuff(){
		List<Location> locations = app.getDbhelper().getAllDisplayableLocations();
		sendTrajectoryMessage(locations);
		List<ImageMarker> imagemarkers = app.getDbhelper().getImageMarkers();
		sendImageMarkerMessage(imagemarkers);
	}
	
	private void prepareToStartSharing() {
		app.resetVariables();
		showNotification();
		serviceIsSharing = true;
		sharingManager.startSharing();
	}
	
	private void showNotification() {
		Notification notification = new Notification(R.drawable.ic_launcher, "sharing has started",
				System.currentTimeMillis());
		PendingIntent pendingIntent = PendingIntent.getActivity(AntrackService.this, 0, new Intent(AntrackService.this,
				MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT), PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(AntrackService.this, "AnTrack", "sharing...", pendingIntent);
		startForeground(1337, notification);
	}
	
	private void prepareToStopSharing() {
		sharingManager.stopSharing();
		serviceIsSharing = false;
		stopForeground(true);
	}
	
	synchronized private void sendTrajectoryMessage(List<Location> locations){
		sendMessageToMapActivity(IPCMessages.SYNC_CURRENT_TRAJECTORY, locations);
	}
	
	synchronized private void sendImageMarkerMessage(List<ImageMarker> imagemarkers){
		sendMessageToMapActivity(IPCMessages.SYNC_CURRENT_IMAGE_MARKERS, imagemarkers);
	}
	
	synchronized private void sendMessageToMapActivity(int what, Object data) {
		if (mLocationSender != null) {
			try {
				Message msg = Message.obtain(null, what, data);
				mLocationSender.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		serviceIsSharing = false;
		
		context = this;
		
		app = AntrackApp.getInstance(context);
		
		imageUploader = new ImageUploader(app, new Handler(), PreferenceManager.getDefaultSharedPreferences(context));
		
		createHandlers();
		
		setupBroadcastReceiver();
		
		locationServiceConnector = new LocationServiceConnector(context);
		sharingManager = new SharingManager(context);
	}
	
	private void setupBroadcastReceiver(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(IPCMessages.LB_START_SHARING);
		filter.addAction(IPCMessages.LB_STOP_SHARING);
		filter.addAction(IPCMessages.LB_IMAGE_CREATE);
		filter.addAction(IPCMessages.LB_IMAGE_CONFIRM);
		filter.addAction(IPCMessages.LB_IMAGE_CANCEL);
		LocalBroadcastManager.getInstance(AntrackService.this).registerReceiver(mBroadcastReceiver, filter);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mReceiver.getBinder();
	}
	
	public static boolean isSharingLocation() {
		return serviceIsSharing;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		locationServiceConnector.stop();
		locationServiceConnector = null;
		
		imageUploader.stop();
		imageUploader = null;
		
		app.cancelAll();
		
		LocalBroadcastManager.getInstance(AntrackService.this).unregisterReceiver(mBroadcastReceiver);
		
		serviceIsSharing = false;
	}
}