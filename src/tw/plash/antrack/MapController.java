package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapController {
	
	private final GoogleMap gmap;
	private Polyline trajectory;
	private LatLng latestLocation;
	
	public MapController(GoogleMap gmap) {
		this.gmap = gmap;
		this.trajectory = null;
		this.latestLocation = null;
	}
	
	public void clearMap() {
		this.gmap.clear();
		this.trajectory = null;
		this.latestLocation = null;
	}
	
	synchronized public void addLocation(Location location) {
		latestLocation = location2LatLng(location);
		if (trajectory != null) {
			// already have locations
			List<LatLng> points = trajectory.getPoints();
			points.add(location2LatLng(location));
			trajectory.setPoints(points);
		} else {
			// first time adding locations
			ArrayList<Location> list = new ArrayList<Location>();
			list.add(location);
			drawLocations(list);
		}
	}
	
	synchronized public void drawLocations(List<Location> locations) {
		if (locations.isEmpty()) {
			return;
		}
		PolylineOptions pline = new PolylineOptions();
		for (Location location : locations) {
			pline.add(location2LatLng(location));
		}
		trajectory = gmap.addPolyline(pline);
		List<LatLng> points = trajectory.getPoints();
		LatLng firstPoint = points.get(0);
		latestLocation = points.get(points.size() - 1);
		drawMarker(firstPoint, "Start");
		gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestLocation, 10));
	}
	
	synchronized public void drawEndMarker() {
		//got nothing to draw
		if(trajectory != null){
			List<LatLng> points = trajectory.getPoints();
			LatLng lastPoint = points.get(points.size() - 1);
			drawMarker(lastPoint, "End");
		}
	}
	
	private void drawMarker(LatLng position, String title){
		gmap.addMarker(new MarkerOptions().position(position).title(title));
	}
	
	synchronized public void addPicture(String imagePath, LatLng location) {
		Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imagePath), 64, 64);
		gmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(thumbnail)).position(location));
		Log.e("tw.plash", "image marker at " + location);
	}
	
	private void animateToLocation(Location location){
		if(location != null){
			gmap.animateCamera(CameraUpdateFactory.newLatLng(location2LatLng(location)));
		}
	}
	
	synchronized public void centerAtMyLocation() {
		animateToLocation(gmap.getMyLocation());
	}
	
	synchronized public void moveToLocation(Location location){
		animateToLocation(location);
	}
	
	public LatLng getLatestLocation(){
		return latestLocation;
	}
	
	private LatLng location2LatLng(Location location){
		return new LatLng(location.getLatitude(), location.getLongitude());
	}
}