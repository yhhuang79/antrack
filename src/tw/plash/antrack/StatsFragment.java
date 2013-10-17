package tw.plash.antrack;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatsFragment extends Fragment {
	
	private SharedPreferences preference;
	
//	private View statsPanel;
	private StatsUpdater statsUpdater;
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(IPCMessages.LOCALBROADCAST_START_SHARING)){
				//set start time, reset and start chonometer
				statsUpdater.startSharing(preference, System.currentTimeMillis(), SystemClock.elapsedRealtime());
				bindToService();
			} else if(action.equals(IPCMessages.LOCALBROADCAST_STOP_SHARING)){
				//should anticipate stats summary
				statsUpdater.stopSharing(preference);
			}
		}
	};
	
	private Messenger mSender = null;
	private boolean mIsBound;
	private final Messenger mReceiver = new Messenger(new ImcomingHandler());
	
	private class ImcomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case IPCMessages.UPDATE_STATS:
				//just parse the stats object and show it
				statsUpdater.updateStats((TripStatictics) msg.obj);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mSender = new Messenger(service);
			sendMessageToService(IPCMessages.REGISTER, false);
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mSender = null;
		}
	};
	
	private void sendMessageToService(int what, boolean safetyCheck) {
		if (!safetyCheck || canSendMessageNow()) {
			try {
				Message msg = Message.obtain(null, what, IPCMessages.STATS_FRAGMENT, 0);
				msg.replyTo = mReceiver;
				mSender.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean canSendMessageNow() {
		if (mIsBound) {
			if (mSender != null) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		statsUpdater = new StatsUpdater();
		preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootview = inflater.inflate(R.layout.stats, container, false);
		View statsPanel = rootview.findViewById(R.id.stats_info);
		((TextView) statsPanel.findViewById(R.id.name)).setText(preference.getString("name", "AnTrack"));
		statsUpdater.initViews(statsPanel);
		return rootview;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		restoreState(AntrackService.isSharingLocation());
		if(AntrackService.isSharingLocation()){
			bindToService();
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction(IPCMessages.LOCALBROADCAST_START_SHARING);
		filter.addAction(IPCMessages.LOCALBROADCAST_STOP_SHARING);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, filter);
	}
	
	private void bindToService(){
		if(!mIsBound){
			getActivity();
			getActivity().bindService(new Intent(getActivity(), AntrackService.class), mConnection, Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}
	
	private void restoreState(boolean isSharing){
		if(isSharing){
			statsUpdater.restoreStats(preference);
		} else{
			statsUpdater.restoreNonSharingStats(preference);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
		if(AntrackService.isSharingLocation()){
			saveState();
		}
		unbindFromService();
	}
	
	private void saveState(){
		statsUpdater.saveStats(preference);
	}
	
	private void unbindFromService(){
		if(mIsBound){
			sendMessageToService(IPCMessages.DEREGISTER, true);
			getActivity().unbindService(mConnection);
			mIsBound = false;
		}
	}
}