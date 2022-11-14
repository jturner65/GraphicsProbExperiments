package base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.base;

/**
 * ROI calculation - how to distinguish points and build regions
 * @author 7strb
 *
 */
public abstract class Base_ROI { 
	public int numPtsDist;			//# of points to consider in ROI calc				
	public Base_ROI(int _numPts){numPtsDist = _numPts;}
	public abstract double calcROI(Double[] orderedKeys);
	public double fixDist(double dist){
		if(dist < 0){dist *= -1;}
		if(dist > 1.0){dist = 1.0/dist;}
		return dist;
	}
}//class Base_ROI