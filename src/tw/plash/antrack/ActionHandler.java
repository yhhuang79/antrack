package tw.plash.antrack;

import android.content.Context;
import android.content.Intent;

public abstract class ActionHandler {
	
	protected AntrackApp app;
	
	public ActionHandler(AntrackApp app) {
		this.app = app;
	}
	
	public abstract void execute(Intent intent);
}
