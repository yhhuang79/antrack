package tw.plash.antrack;

import tw.plash.antrack.images.PhotoFragment;
import tw.plash.antrack.stats.StatsFragment;
import android.content.Context;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class PagerAdapter extends FragmentPagerAdapter {
	private Context mContext;
	public PagerAdapter(FragmentManager fm,Context c) {
		super(fm);
		mContext=c;
	}

	@Override
	public Fragment getItem(int position) {
		switch(position){
		case 0:
			return new StatsFragment();
		case 2:
			return new PhotoFragment();
		case 1:
		default:
			return new Fragment();
		}
	}
	
	@Override
	public int getCount() {
		return 3;
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		switch(position){
		
		case 0:
			return  mContext.getResources().getString(R.string.tab_stats);
		case 2:
			return  mContext.getResources().getString(R.string.tab_photos);
		case 1:
		default:
			return  mContext.getResources().getString(R.string.tab_map);
		}
	}
}
