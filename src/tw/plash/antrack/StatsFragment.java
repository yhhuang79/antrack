package tw.plash.antrack;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class StatsFragment extends Fragment {
	
	private final String simpleName = "StatsFragment";
	
	private Messenger mSender = null;
	private boolean mIsBound;
	private final Messenger mReceiver = new Messenger(new ImcomingHandler());
	
	private class ImcomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			//other cases
			case IPCMessages.UPDATE_STATS:
				//just parse the stats object and show it
				break;
			case IPCMessages.UPDATE_STATS_SUMMARY:
				//show average stats
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Utility.log(simpleName, "serviceocnnection: binded");
			mSender = new Messenger(service);
			Utility.log(simpleName, "serviceocnnection: now registering");
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootview = inflater.inflate(R.layout.stats, container, false);
		return rootview;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Utility.log(simpleName, "onresume");
		bindToService();
	}
	
	private void bindToService(){
		Utility.log(simpleName, "bindToService: check if necessary");
		if(!mIsBound){
			Utility.log(simpleName, "bindToService: actual binding");
			getActivity().bindService(new Intent(getActivity(), AntrackService.class), mConnection, getActivity().BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Utility.log(simpleName, "onPause");
		//unbind from service
		unbindFromService();
	}
	
	private void unbindFromService(){
		Utility.log(simpleName, "unbindFromService");
		if(mIsBound){
			Utility.log(simpleName, "unbindFromService: deregister");
			sendMessageToService(IPCMessages.DEREGISTER, true);
			Utility.log(simpleName, "unbindFromService: unbind");
			getActivity().unbindService(mConnection);
			mIsBound = false;
		}
	}
}
