package tw.plash.antrack;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class PhotoFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootview = inflater.inflate(R.layout.dummy2, container, false);
		Log.i("ANTRACK", "Photo: onCreateView");
		return rootview;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
//		setHasOptionsMenu(true);
		Log.i("ANTRACK", "Photo: onActivityCreated");
//		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Log.i("ANTRACK", "photo: onPause");
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.i("ANTRACK", "photo: onResume");
	}
}
