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
	
	private Messenger mSender = null;
	private boolean mIsBound;
	private final Messenger mReceiver = new Messenger(new ImcomingHandler());
	
	private class ImcomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			//other cases
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mSender = new Messenger(service);
			Message msg = Message.obtain(null, IPCMessages.REGISTER, IPCMessages.STATS_FRAGMENT, 0);
			msg.replyTo = mReceiver;
			try {
				mSender.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mSender = null;
		}
	};
	
	private void sendMessageToService(int what){
		if(mIsBound){
			if(mSender != null){
				Message msg = Message.obtain(null, what, IPCMessages.STATS_FRAGMENT, 0);
				msg.replyTo = mReceiver;
				try {
					mSender.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootview = inflater.inflate(R.layout.stats, container, false);
		Log.w("ANTRACK", "Stats: onCraeteView");
		return rootview;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
//		setHasOptionsMenu(true);
		Log.w("ANTRACK", "Stats: onActivityCreated");
//		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.w("ANTRACK", "Stats: onResume");
		//bind to service
		bindToService();
	}
	
	private void bindToService(){
		if(!mIsBound){
			getActivity().bindService(new Intent(getActivity(), AntrackService.class), mConnection, getActivity().BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Log.w("ANTRACK", "Stats: onPause");
		//unbind from service
		
	}
	
	private void unbindFromService(){
		if(mIsBound){
			if(mSender != null){
				Message msg = Message.obtain(null, IPCMessages.DEREGISTER, IPCMessages.STATS_FRAGMENT, 0);
				msg.replyTo = mReceiver;
				try {
					mSender.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			getActivity().unbindService(mConnection);
			mIsBound = false;
		}
	}
}
