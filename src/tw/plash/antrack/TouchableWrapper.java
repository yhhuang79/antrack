package tw.plash.antrack;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class TouchableWrapper extends FrameLayout {
	
	private TouchableWrapperCallback twc;
	
	public TouchableWrapper(Context context) {
		super(context);
		this.twc = null;
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(twc != null){
			switch(ev.getAction()){
			case MotionEvent.ACTION_DOWN:
				//set callback to touch is true
				twc.setIsTouched();
				break;
			}
		}
		return super.dispatchTouchEvent(ev);
	}
	
	public void setCallback(TouchableWrapperCallback twc){
		this.twc = twc;
	}
}
