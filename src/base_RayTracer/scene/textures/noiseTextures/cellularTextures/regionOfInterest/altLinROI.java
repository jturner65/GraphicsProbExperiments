package base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest;

import base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.base.Base_ROI;

/**
 * alternating ROI calc (negative/positive sum of dists), should have even numPtsDist
 * @author 7strb
 *
 */
public class altLinROI extends Base_ROI{
	public altLinROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0, modVal = -1;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += (modVal * dToPt);
			++i;
			if (i>=numPtsDist){break;}
			modVal *= -1;
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Alternating Linear sum/diff ROI Calc";}	
}//altLinROI