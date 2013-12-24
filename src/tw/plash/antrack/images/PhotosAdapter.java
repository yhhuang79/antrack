package tw.plash.antrack.images;

import java.io.File;
import java.util.List;

import tw.plash.antrack.R;
import tw.plash.antrack.R.drawable;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class PhotosAdapter extends BaseAdapter {
	
	private Context context;
	private List<ImageMarker> imagepaths;
	
	public PhotosAdapter(Context context, List<ImageMarker> imagepaths) {
		this.context = context;
		this.imagepaths = imagepaths;
	}
	
	@Override
	public int getCount() {
		return imagepaths.size();
	}
	
	@Override
	public String getItem(int position) {
		return imagepaths.get(position).getPath();
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView view = (ImageView) convertView;
		if (view == null) {
			view = new ImageView(context);
		}
		String url = getItem(position);
		
		Picasso.with(context)
		.load(new File(url))
		.centerCrop()
		.resize(128, 128)
		.error(R.drawable.ic_launcher)
		.placeholder(R.drawable.ic_launcher)
		.into(view);
		
		return view;
	}
}
