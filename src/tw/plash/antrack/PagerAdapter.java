package tw.plash.antrack;

import tw.plash.antrack.images.PhotoFragment;
import tw.plash.antrack.stats.StatsFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class PagerAdapter extends FragmentPagerAdapter {
	
	public PagerAdapter(FragmentManager fm) {
		super(fm);
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
			return "Stats";
		case 2:
			return "Photos";
		case 1:
		default:
			return "Map";
		}
	}
}
