package tw.plash.antrack;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class AntrackViewPager extends ViewPager {
	
	private boolean isPagingEnabled;
	private boolean isBezelGesture;
	
	private float leftBezelZone;
	private float rightBezelZone;
	
	public AntrackViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.isPagingEnabled = true;
		this.isBezelGesture = false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(this.isPagingEnabled || this.isBezelGesture){
			//pager has control
			return super.onTouchEvent(event);
		}
		//map has control
		return false;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		float x = event.getX();
		if(this.isPagingEnabled){
			return super.onInterceptTouchEvent(event);
		} else {
			this.isBezelGesture = isBezelGesture(x);
			//paging disabled, check for bezel gesture
		} 
		return true;
	}
	
	public void setPagingEnabled(boolean b){
		this.isPagingEnabled = b;
	}
	
	private boolean isBezelGesture(float x){
		if(x < leftBezelZone || x > rightBezelZone){
			return true;
		}
		return false;
	}
	
//	private boolean isClickOnTabStrip(float y){
//		if(y < topStripZone){
//			return true;
//		}
//		return false;
//	}
	
//	public void setHeight(float height){
//		this.topStripZone = height * 0.05f;
//		Log.e("my view pager", "height= " + height + ", top= " + topStripZone);
//	}
	
	public void setWidth(float width){
		this.leftBezelZone = width * 0.05f;
		this.rightBezelZone = width * 0.95f;
//		Log.e("my view pager", "width= " + width + ", left= " + leftBezelZone + ", right= " + rightBezelZone);
	}
}
