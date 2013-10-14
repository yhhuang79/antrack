package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapController {
	
	private final GoogleMap gmap;
	private Polyline trajectory;
	
	public MapController(GoogleMap gmap) {
		this.gmap = gmap;
		this.trajectory = null;
	}
	
	public void clearMap(){
		this.gmap.clear();
		this.trajectory = null;
	}
	
	synchronized public void addLocation(Location location){
		if(trajectory != null){
			//already have locations
			LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
			List<LatLng> points = trajectory.getPoints();
			points.add(latlng);
			trajectory.setPoints(points);
		} else{
			//first time adding locations
			ArrayList<Location> list = new ArrayList<Location>();
			list.add(location);
			drawLocations(list);
		}
	}
	
	synchronized public void drawLocations(List<Location> locations){
		if(locations.isEmpty()){
			return;
		}
		LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
		PolylineOptions pline = new PolylineOptions();
		for(Location location : locations){
			LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
			pline.add(latlng);
			boundBuilder.include(latlng);
		}
		trajectory = gmap.addPolyline(pline);
		LatLng firstPoint = trajectory.getPoints().get(0);
		gmap.addMarker(new MarkerOptions().position(firstPoint).title("Start"));
		//zoom out to show entire trajectory
		gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundBuilder.build(), 5));
	}
	
	synchronized public void moveToLocation(Location location){
		gmap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
	}
	
	synchronized public void drawEndMarker(){
		List<LatLng> points = trajectory.getPoints();
		LatLng lastPoint = points.get(points.size() - 1);
		gmap.addMarker(new MarkerOptions().position(lastPoint).title("End"));
	}
	
	synchronized public void addPicture(String pathToThumbnail){
		
	}
}
