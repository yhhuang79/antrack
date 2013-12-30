package tw.plash.antrack.stats;

import java.util.Observable;
import java.util.Observer;

import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.AntrackService;
import tw.plash.antrack.R;
import tw.plash.antrack.util.Utility;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatsFragment extends Fragment implements Observer{
	
	private AntrackApp app;
	
	private TextView startTime;
	private TextView duration;
	private TextView numberOfPhotos;
	private TextView distance;
	private TextView followers;
	
	private Handler handler;
	private final int ONE_SECOND = 1000;
	
	private final Runnable updateDuration = new Runnable() {
		@Override
		public void run() {
			if(isResumed() && AntrackService.isSharingLocation()){
				updateDuration();
				handler.postDelayed(this, ONE_SECOND);
			}
		}
	};
	
	/**
	 * calculate duration by subtracting current time with start time
	 * and convert that difference from milliseconds to seconds to formatted string
	 */
	private void updateDuration(){
		duration.setText(Utility.getDurationInSecondsAsFormattedString((System.currentTimeMillis() - app.getStatsKeeper().getStats().getStartTime())/1000));
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = AntrackApp.getInstance(getActivity());
		handler = new Handler();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootview = inflater.inflate(R.layout.stats, container, false);
		
		startTime = (TextView) rootview.findViewById(R.id.start_time);
		duration = (TextView) rootview.findViewById(R.id.duration);
		numberOfPhotos = (TextView) rootview.findViewById(R.id.number_of_photos);
		distance = (TextView) rootview.findViewById(R.id.distance);
		followers = (TextView) rootview.findViewById(R.id.followers);
		
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
		((TextView) rootview.findViewById(R.id.name)).setText(preference.getString("name", "AnTrack"));
		return rootview;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.e("tw.statsfragment", "onResume");
		app.getStatsKeeper().addObserver(this);
		app.getStatsKeeper().sync();
		if(AntrackService.isSharingLocation()){
			handler.post(updateDuration);
		}
	}
	
	//TODO need to init stats and finalize stats of stats keeper in service
	//TODO make sure duration value when not recordign is set correctly
	//TODO make sure when recording, duration value is set correctly
	//TODO make sure when returning to a recording state, duration value is set correctly
	
	@Override
	public void onPause() {
		super.onPause();
		Log.e("tw.statsfragment", "onPause");
		app.getStatsKeeper().deleteObserver(this);
		handler.removeCallbacks(updateDuration);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.e("tw.statsfragment", "onDestroyView");
	}
	
	@Override
	public void update(Observable observable, Object data) {
		updateStatsPanel((Stats) data);
	}
	
	private void updateStatsPanel(Stats stats){
		Log.e("tw.statsfragment", "updateStatsPanel");
		startTime.setText(stats.getStartTimeAsString());
		if(stats.getDuration() > -1L){
			duration.setText(Utility.getDurationInSecondsAsFormattedString(stats.getDuration()/1000));
		}
		numberOfPhotos.setText(stats.getNumberOfPhotos());
		distance.setText(stats.getDistanceAsString());
		followers.setText(stats.getNumberOfFollowers());
	}
}