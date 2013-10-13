package tw.plash.antrack;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class AntrackViewPager extends ViewPager {
	
	private boolean isPagingEnabled;
	private boolean isBezelGesture;
//	private boolean isClickOnTabStrip;
	
	private float leftBezelZone;
	private float rightBezelZone;
//	private float topStripZone;
	
	public AntrackViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.isPagingEnabled = true;
		this.isBezelGesture = false;
//		this.isClickOnTabStrip = false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(this.isPagingEnabled || this.isBezelGesture){
//			Log.w("my view pager", "onTouchEvent: pager has control");
			return super.onTouchEvent(event);
		}
//		Log.d("my view pager", "onTouchEvent: map has control");
		return false;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		float x = event.getX();
//		float y = event.getY();
//		Log.e("my view pager", "onInterceptTouchEvent");
//		this.isClickOnTabStrip = isClickOnTabStrip(y);
		
		//if paging is enabled or user is clicking on tab strip
		//let viewpager handle the touch event
		//otherwise, check for bezel gesture
		//if non of the above, ignore touch event, let map handle it
//		if(this.isPagingEnabled || this.isClickOnTabStrip){
		if(this.isPagingEnabled){
//			Log.e("my view pager", "onInterceptTouchEvent: paging enabled/clicked on tab strip");
			return super.onInterceptTouchEvent(event);
		} else {
			this.isBezelGesture = isBezelGesture(x);
//			Log.i("my view pager", "onInterceptTouchEvent: paging disabled, check for bezel gesture, (x, y)=(" + x + ", " + y + ")");
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
