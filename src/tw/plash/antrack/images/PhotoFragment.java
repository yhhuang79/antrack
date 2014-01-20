package tw.plash.antrack.images;

import java.util.List;

import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class PhotoFragment extends Fragment {
	
	private GridView gridview;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootview = inflater.inflate(R.layout.photos, container, false);
		gridview = (GridView) rootview.findViewById(R.id.photos);
		rootview.setOnTouchListener(new OnTouchListener() {
	        @Override
	        public boolean onTouch(View v, MotionEvent event) {
	            Log.v(null, "TOUCH EVENT"); // handle your fragment number here
	            return false;
	        }
	    });
		return rootview;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		List<ImageMarker> imageMarkers = AntrackApp.getInstance(getActivity()).getDbhelper().getImageMarkers();
		// there are new images to be shown
		PhotosAdapter adapter = new PhotosAdapter(getActivity(), imageMarkers);
		gridview.setAdapter(adapter);
		gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse("file://" + gridview.getItemAtPosition(position)), "image/*");
				Log.d("PhotoFragment OnClick", "file://" + gridview.getItemAtPosition(position));
				startActivity(intent);
			}
		});
	}
}
