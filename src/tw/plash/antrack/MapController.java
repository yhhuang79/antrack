package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.AsyncTask;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MarkerOptionsCreator;
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
		pline.color(0xffdd0000);
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
	
	synchronized public void addImageMarkers(List<ImageMarker> imagemarkers){
		for(ImageMarker im : imagemarkers){
			LatLng latlng = new LatLng(im.getLatitude(), im.getLongitude());
			String path = im.getPath();
			new markerloader(latlng, path).execute();
		}
	}
	
	private class markerloader extends AsyncTask<Void, Void, Bitmap>{
		private LatLng latlng;
		private String path;
		public markerloader(LatLng latlng, String path) {
			this.latlng = latlng;
			this.path = path;
		}
		@Override
		protected Bitmap doInBackground(Void... params) {
			return Utility.getThumbnail(path, 72);
		}
		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if(gmap != null){
				gmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(result)).position(latlng).draggable(true));
			}
		}
	}
	
	private void animateToLocation(Location location){
		if(location != null){
			gmap.animateCamera(CameraUpdateFactory.newLatLng(location2LatLng(location)));
		}
	}
	
	synchronized public void centerAtMyLocation() {
		Location location = gmap.getMyLocation();
		if(location != null){
			animateToLocation(location);
		} else{
			throw new NullPointerException();
		}
	}
	
	synchronized public void moveToLocation(Location location){
		animateToLocation(location);
	}
	
	public boolean hasLocation(){
		if(latestLocation != null){
			return true;
		} else{
			return false;
		}
	}
	
	public LatLng getLatestLocation(){
		return latestLocation;
	}
	
	private LatLng location2LatLng(Location location){
		return new LatLng(location.getLatitude(), location.getLongitude());
	}
}