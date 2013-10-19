package tw.plash.antrack;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatsFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootview = inflater.inflate(R.layout.stats, container, false);
		View statsPanel = rootview.findViewById(R.id.stats_info);
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
		((TextView) statsPanel.findViewById(R.id.name)).setText(preference.getString("name", "AnTrack"));
		AntrackApp.getInstance(getActivity()).getStatsUpdater().initViews(getActivity(), statsPanel);
		return rootview;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		AntrackApp.getInstance(getActivity()).getStatsUpdater().onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		AntrackApp.getInstance(getActivity()).getStatsUpdater().onPause();
	}
}