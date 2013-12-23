package tw.plash.antrack;

import java.util.List;

import tw.plash.antrack.images.ImageMarker;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class PhotoFragment extends Fragment {
	
	private GridView gridview;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootview = inflater.inflate(R.layout.photos, container, false);
		gridview = (GridView) rootview.findViewById(R.id.photos);
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
				startActivity(intent);
			}
		});
	}
}
