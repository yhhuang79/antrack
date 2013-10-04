package tw.plash.antrack;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

public class MapFragment extends SupportMapFragment {
	
	private GoogleMap googlemap;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View mapview = super.onCreateView(inflater, container, savedInstanceState);
		Log.e("ANTRACK", "Map: onCreateView");
		View layout = inflater.inflate(R.layout.mapfragment, container, false);
		RelativeLayout mapContainer = (RelativeLayout) layout.findViewById(R.id.map_container);
		mapContainer.addView(mapview, 0);
		
		//for compatibility issue with android 2.2 and 2.3
		FrameLayout frameLayout = new FrameLayout(getActivity());
		frameLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
		mapContainer.addView(frameLayout, 
			new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		return layout;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setHasOptionsMenu(true);
		Log.e("ANTRACK", "Map: onActivityCreated");
		
		switch (GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity())) {
		case ConnectionResult.SUCCESS:
			// init google map here
//			if (googlemap == null) {
//				googlemap = getMap();
//				googlemap.setMyLocationEnabled(true);
//				googlemap.getUiSettings().setMyLocationButtonEnabled(false);
//				googlemap.setIndoorEnabled(true);
//			}
			break;
		default:
			Toast.makeText(getActivity(), "Google Play Service unavailable...", Toast.LENGTH_SHORT).show();
			return;
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.camera:
			Toast.makeText(getActivity(), "camera", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.camera2:
			Toast.makeText(getActivity(), "camera2", Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Log.e("ANTRACK", "Map: onPause");
		// XXX unregister listener
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.e("ANTRACK", "Map: onResume");
		// XXX register listener
	}
}
