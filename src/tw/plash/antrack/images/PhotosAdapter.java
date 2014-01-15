package tw.plash.antrack.images;

import java.io.File;
import java.util.List;

import tw.plash.antrack.R;
import tw.plash.antrack.Tutorial;
import tw.plash.antrack.R.drawable;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
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
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		ImageView view = (ImageView) convertView;
		if (view == null) {
			view = new ImageView(context);
		}
		String url = getItem(position);
		int dpi = pref.getInt("dpi", DisplayMetrics.DENSITY_DEFAULT);
		int px = 128, py = 128;
		switch(dpi) {
			case DisplayMetrics.DENSITY_LOW:
				px = (int) (px * 0.75);
				py = (int) (py * 0.75);
			case DisplayMetrics.DENSITY_HIGH:
			    px = (int) (px * 1.5);
			    py = (int) (py * 1.5);
			case DisplayMetrics.DENSITY_XHIGH:
				px = px * 2;
				py = py * 2;
				break;
			case DisplayMetrics.DENSITY_XXHIGH:
				px = px * 3;
				py = py * 3;
				break;
			default:
				px = px * 1;
				py = py * 1;
		}
		
		Picasso.with(context)
		.load(new File(url))
		.centerCrop()
		.resize(py, px)
		.error(R.drawable.ic_launcher)
		.placeholder(R.drawable.ic_launcher)
		.into(view);
		
		return view;
	}
}
