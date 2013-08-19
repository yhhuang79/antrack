package tw.plash.antrack;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;

public class SharingSummaryDialog extends Dialog{
	
	public SharingSummaryDialog(Context context) {
		super(context);
		
		dialogSetup();
		
		
	}
	
	private void dialogSetup(){
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setCancelable(true);
		setCanceledOnTouchOutside(true);
		setContentView(0);
	}
	
	
}
