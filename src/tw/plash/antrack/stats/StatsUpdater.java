package tw.plash.antrack.stats;

import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.R;
import tw.plash.antrack.R.id;
import tw.plash.antrack.util.Utility;
import android.content.Context;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;

public class StatsUpdater {
	
	private Context mContext;
	
	private TextView startTime;
	private Chronometer duration;
	private TextView numberOfPhotos;
	private TextView distance;
	private TextView followers;
	
	private boolean canUpdate;
	private boolean isSharing;
	
	public void initViews(Context context, View panel){
		mContext = context;
		startTime = (TextView) panel.findViewById(R.id.start_time);
		duration = (Chronometer) panel.findViewById(R.id.duration);
		numberOfPhotos = (TextView) panel.findViewById(R.id.number_of_photos);
		distance = (TextView) panel.findViewById(R.id.distance);
		followers = (TextView) panel.findViewById(R.id.followers);
	}
	
	public void onResume(){
		canUpdate = true;
		
		if(isSharing){
			//resume state, start
			//get state from DB, and start timer
			Stats stats = AntrackApp.getInstance(mContext).getDbhelper().getStats();
			startTime.setText(stats.getStarttime());
			duration.setBase(stats.getDurationbase());
			numberOfPhotos.setText(String.valueOf(AntrackApp.getInstance(mContext).getDbhelper().getNumberOfImageMarkers()));
			distance.setText(stats.getDistance());
			duration.start();
			followers.setText(AntrackApp.getInstance(mContext).getFollowers() + " followers");
		} else{
			//resume state
			//get state from DB
			Stats stats = AntrackApp.getInstance(mContext).getDbhelper().getStats();
			startTime.setText(stats.getStarttime());
			duration.setText(stats.getDuration());
			numberOfPhotos.setText(String.valueOf(AntrackApp.getInstance(mContext).getDbhelper().getNumberOfImageMarkers()));
			followers.setText(AntrackApp.getInstance(mContext).getFollowers() + " followers");
			distance.setText(stats.getDistance());
		}
	}
	
	public void onPause(){
		canUpdate = false;
		if(isSharing){
			//save state, stop
			Stats stats = new Stats();
			stats.setStarttime(startTime.getText().toString());
			stats.setDuration(duration.getText().toString());
			stats.setDurationbase(duration.getBase());
			stats.setDistance(distance.getText().toString());
			AntrackApp.getInstance(mContext).getDbhelper().setStats(stats);
			
			duration.stop();
		} else{
			//do nothing
		}
	}
	
	public void startSharing(long currentTime, long timeBase){
		isSharing = true;
		
		if(canUpdate){
			//reset/save state, start
			AntrackApp.getInstance(mContext).getDbhelper().removeStats();
			
			startTime.setText(Utility.getHHMMSSTimeString(currentTime));
			duration.setText("00:00");
			duration.setBase(timeBase);
			numberOfPhotos.setText("0");
			distance.setText("0km");
			followers.setText("0 followers");
			
			Stats stats = new Stats();
			stats.setStarttime(startTime.getText().toString());
			stats.setDuration(duration.getText().toString());
			stats.setDurationbase(duration.getBase());
			stats.setDistance(distance.getText().toString());
			AntrackApp.getInstance(mContext).getDbhelper().setStats(stats);
			duration.start();
		} else{
			//reset/save state
			AntrackApp.getInstance(mContext).getDbhelper().removeStats();
			
			Stats stats = new Stats();
			stats.setStarttime(Utility.getHHMMSSTimeString(currentTime));
			stats.setDuration("00:00");
			stats.setDurationbase(timeBase);
			stats.setDistance("0km");
			AntrackApp.getInstance(mContext).getDbhelper().setStats(stats);
		}
	}
	
	public void stopSharing(){
		isSharing = false;
		
		if(canUpdate){
			//stop, save state
			duration.stop();
			Stats stats = new Stats();
			stats.setStarttime(startTime.getText().toString());
			stats.setDuration(duration.getText().toString());
			stats.setDurationbase(duration.getBase());
			stats.setDistance(distance.getText().toString());
			AntrackApp.getInstance(mContext).getDbhelper().setStats(stats);
		} else{
			//save state
			Stats stats = new Stats();
			stats.setStarttime(startTime.getText().toString());
			stats.setDuration(duration.getText().toString());
			stats.setDurationbase(duration.getBase());
			stats.setDistance(distance.getText().toString());
			AntrackApp.getInstance(mContext).getDbhelper().setStats(stats);
		}
	}
	
	public void updateStats(TripStatictics tstats){
		if(canUpdate){
			distance.setText(tstats.getDistanceString());
		} else{
			Stats stats = new Stats();
			stats.setStarttime(startTime.getText().toString());
			stats.setDuration(duration.getText().toString());
			stats.setDurationbase(duration.getBase());
			stats.setDistance(tstats.getDistanceString());
			AntrackApp.getInstance(mContext).getDbhelper().setStats(stats);
		}
	}
}
