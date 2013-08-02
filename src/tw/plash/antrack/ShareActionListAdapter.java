package tw.plash.antrack;

import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ShareActionListAdapter extends BaseAdapter {
	
	private final List<ResolveInfo> list;
	private final LayoutInflater mInflater;
	private final PackageManager pm;
	
	public ShareActionListAdapter(Context c, List<ResolveInfo> list) {
		this.list = list;
		this.mInflater = LayoutInflater.from(c);
		this.pm = c.getPackageManager();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		
		if(convertView == null){
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.shareactionlistitem, null);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.label = (TextView) convertView.findViewById(R.id.label);
			convertView.setTag(holder);
		} else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		ResolveInfo info = list.get(position);
		holder.icon.setImageDrawable(info.loadIcon(pm));
		holder.label.setText(info.loadLabel(pm));
		
		return convertView;
	}
	
	class ViewHolder{
		ImageView icon;
		TextView label;
	}
	
	@Override
	public int getCount() {
		return list.size();
	}
	
	@Override
	public Object getItem(int position) {
		return list.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
}
