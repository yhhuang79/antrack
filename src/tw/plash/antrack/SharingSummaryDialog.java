package tw.plash.antrack;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class SharingSummaryDialog extends Dialog{
	
	private TextView accuracy;
	private TextView speed;
	private TextView duration;
	private TextView distance;
	private TextView follower;
	
	private Button button;
	
	public SharingSummaryDialog(Context context) {
		super(context);
		
		dialogSetup();
		
		setupViews();
	}
	
	private void dialogSetup(){
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setCancelable(true);
		setCanceledOnTouchOutside(true);
		setContentView(R.layout.sharingsummary);
	}
	
	private void setupViews(){
		accuracy = (TextView) findViewById(R.id.accuracy);
		speed = (TextView) findViewById(R.id.speed);
		duration = (TextView) findViewById(R.id.duration);
		distance = (TextView) findViewById(R.id.distance);
		follower = (TextView) findViewById(R.id.number_of_watchers);
		
		button = (Button) findViewById(R.id.closedialog);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}
	
	public void setTripStatistics(TripStatictics stats){
		accuracy.setText(stats.getAverageAccuracy());
		speed.setText(stats.getAverageSpeed());
		duration.setText(stats.getFormattedElapsedTime());
		distance.setText(stats.getDistance());
		follower.setText(stats.getNumberOfWatcher());
	}
}
