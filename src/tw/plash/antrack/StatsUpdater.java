package tw.plash.antrack;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;

public class StatsUpdater {
	
	private TextView startTime;
	private Chronometer duration;
	private TextView numberOfPhotos;
	private TextView distance;
	
	public void initViews(View panel){
		numberOfPhotos = (TextView) panel.findViewById(R.id.number_of_photos);
		startTime = (TextView) panel.findViewById(R.id.start_time);
		duration = (Chronometer) panel.findViewById(R.id.duration);
		distance = (TextView) panel.findViewById(R.id.distance);
	}
	
	public void startSharing(SharedPreferences pref, long currentTime, long timeBase){
		pref.edit()
		.remove("STATS_PHOTOS")
		.remove("STATS_STARTTIME")
		.remove("STATS_TIMER")
		.remove("STATS_TIMER_STRING")
		.remove("STATS_DISTANCE")
		.commit();
		startTimer(currentTime, timeBase);
	}
	
	public void stopSharing(SharedPreferences pref){
		duration.stop();
		pref.edit()
		.putString("STATS_PHOTOS", numberOfPhotos.getText().toString())
		.putString("STATS_STARTTIME", startTime.getText().toString())
		.putString("STATS_TIMER_STRING", duration.getText().toString())
		.putString("STATS_DISTANCE", distance.getText().toString())
		.commit();
	}
	
	private void startTimer(long currentTime, long timeBase){
		startTime.setText(Utility.getHHMMSSTimeString(currentTime));
		duration.setBase(timeBase);
		duration.start();
	}
	
	public void updateStats(TripStatictics stats){
		distance.setText(stats.getDistanceString());
	}
	
	public void restoreNonSharingStats(SharedPreferences pref){
		numberOfPhotos.setText(pref.getString("STATS_PHOTOS", "0"));
		startTime.setText(pref.getString("STATS_STARTTIME", "00:00:00"));
		duration.setText(pref.getString("STATS_TIMER_STRING", "00:00"));
		distance.setText(pref.getString("STATS_DISTANCE", "0 m"));
	}
	
	public void restoreStats(SharedPreferences pref){
		numberOfPhotos.setText(pref.getString("STATS_PHOTOS", "0"));
		startTime.setText(pref.getString("STATS_STARTTIME", "00:00:00"));
		duration.setBase(pref.getLong("STATS_TIMER", -1L));
		duration.start();
		distance.setText(pref.getString("STATS_DISTANCE", "0 m"));
	}
	
	public void saveStats(SharedPreferences pref){
		pref.edit()
		.putString("STATS_PHOTOS", numberOfPhotos.getText().toString())
		.putString("STATS_STARTTIME", startTime.getText().toString())
		.putLong("STATS_TIMER", duration.getBase())
		.putString("STATS_DISTANCE", distance.getText().toString())
		.commit();
		duration.stop();
	}
}
