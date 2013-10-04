package tw.plash.antrack;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.LayoutParams;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements TabListener {
	
	private tw.plash.antrack.AntrackViewPager myViewPager;
	private PagerAdapter pagerAdapter;
	
	private GoogleMap googlemap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		final ActionBar actionBar = getSupportActionBar();
		
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle("AnTrack");
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		
		pagerAdapter = new PagerAdapter(getSupportFragmentManager());
		
		myViewPager = (AntrackViewPager) findViewById(R.id.pager);
		
		{
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			myViewPager.setWidth(dm.widthPixels);
		}
		
		myViewPager.setAdapter(pagerAdapter);
		myViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
//				actionBar.setSelectedNavigationItem(position);
				if (position == 1) {
					myViewPager.setPagingEnabled(false);
				} else {
					myViewPager.setPagingEnabled(true);
				}
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}
			
			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});
		
//		for (int i = 0; i < pagerAdapter.getCount(); i++) {
//			actionBar.addTab(actionBar.newTab().setText(pagerAdapter.getPageTitle(i)).setTabListener(this));
//		}
		
//		actionBar.setSelectedNavigationItem(1);
		
		if (googlemap == null) {
			switch (GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext())) {
			case ConnectionResult.SUCCESS:
				googlemap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
				googlemap.setMyLocationEnabled(true);
				googlemap.getUiSettings().setMyLocationButtonEnabled(false);
				googlemap.getUiSettings().setZoomGesturesEnabled(true);
				googlemap.getUiSettings().setZoomControlsEnabled(false);
				googlemap.setIndoorEnabled(true);
				break;
			}
		}
		
		myViewPager.setCurrentItem(1); //XXX
	}
	
	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
	}
	
	@Override
	public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
		myViewPager.setCurrentItem(arg0.getPosition());
	}
	
	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
}
