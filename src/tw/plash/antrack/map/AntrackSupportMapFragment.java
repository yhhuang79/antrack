package tw.plash.antrack.map;

import tw.plash.antrack.util.TouchableWrapper;
import tw.plash.antrack.util.TouchableWrapperCallback;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.SupportMapFragment;

public class AntrackSupportMapFragment extends SupportMapFragment {
	
	private View mOriginalContentView;
	private TouchableWrapper mTouchView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mOriginalContentView = super.onCreateView(inflater, container, savedInstanceState);
		mTouchView = new TouchableWrapper(getActivity());
		mTouchView.addView(mOriginalContentView);
		return mTouchView;
	}
	
	@Override
	public View getView() {
		return mOriginalContentView;
	}
	
	public void setCallback(TouchableWrapperCallback twc){
		mTouchView.setCallback(twc);
	}
}
