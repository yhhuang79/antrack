package tw.plash.antrack.stats;

import java.util.Observable;
import java.util.Observer;

import tw.plash.antrack.AntrackApp;
import tw.plash.antrack.AntrackService;
import tw.plash.antrack.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TextView;

public class StatsFragment extends Fragment implements Observer{
	
	private AntrackApp app;
	
	private TextView startTime;
	private Chronometer duration;
	private TextView numberOfPhotos;
	private TextView distance;
	private TextView followers;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = AntrackApp.getInstance(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootview = inflater.inflate(R.layout.stats, container, false);
		
		startTime = (TextView) rootview.findViewById(R.id.start_time);
		duration = (Chronometer) rootview.findViewById(R.id.duration);
		numberOfPhotos = (TextView) rootview.findViewById(R.id.number_of_photos);
		distance = (TextView) rootview.findViewById(R.id.distance);
		followers = (TextView) rootview.findViewById(R.id.followers);
		
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
		((TextView) rootview.findViewById(R.id.name)).setText(preference.getString("name", "AnTrack"));
		return rootview;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getStatsKeeper().addObserver(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		app.getStatsKeeper().deleteObserver(this);
	}
	
	@Override
	public void update(Observable observable, Object data) {
		updateStatsPanel((Stats) data);
	}
	
	private void updateStatsPanel(Stats stats){
		if(AntrackService.isSharingLocation()){
			
		} else{
			
		}
	}
}