package base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest;

import base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.base.Base_ROI;

/**
 * find nearest x pts and return their distance
 * @author 7strb
 *
 */
public class nearestROI extends Base_ROI{
	public nearestROI(int _numPts) {super(_numPts);}
	@Override
	public double calcROI(Double[] orderedKeys) {
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){	dist += dToPt;	++i;	if (i>=numPtsDist){break;}}
		return fixDist(dist);
	}
	@Override
	public String toString(){return "Nearest " +numPtsDist+" ROI Calc";}		
}