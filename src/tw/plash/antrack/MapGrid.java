package tw.plash.antrack;

public class MapGrid {
	/*
	 * NW  N  NE
	 *  W  C  E
	 * SW  S  SE
	 */
	//upper right
	private double latitudeNE;
	private double longitudeNE;
	//lower right
	private double latitudeSE;
	private double longitudeSE;
	//upper left
	private double latitudeNW;
	private double longitudeNW;
	//lower left
	private double latitudeSW;
	private double longitudeSW;
	//center coordinate
	private double latitudeCenter;
	private double longitudeCenter;
	
	public MapGrid() {
		
	}
	
	
	
	@Override
	public String toString() {
		return "NE:(" + latitudeNE + ", " + longitudeNE + ") " + 
		"SE:(" + latitudeSE + ", " + longitudeSE + ") " + 
		"NW:(" + latitudeNW + ", " + longitudeNW + ") " + 
		"SW:(" + latitudeSW + ", " + longitudeSW + ") " + 
		"Center:(" + latitudeCenter + ", " + longitudeCenter + ")";
	}
}
