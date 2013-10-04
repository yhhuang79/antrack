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

public class StatsFragment extends Fragment {
	
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
	public void onPause() {
		super.onPause();
		Log.w("ANTRACK", "Stats: onPause");
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.w("ANTRACK", "Stats: onResume");
	}
}
