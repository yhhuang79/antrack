package tw.plash.antrack;

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
		case 0: //friends' trips
			return new StatsFragment(); //XXX
		case 2: //public trips
			return new PhotoFragment(); //XXX
		case 1: //my trips
		default: //not sure when will this happen
			return new DummyFragment();
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
			return "Dummy";
		}
	}
}
