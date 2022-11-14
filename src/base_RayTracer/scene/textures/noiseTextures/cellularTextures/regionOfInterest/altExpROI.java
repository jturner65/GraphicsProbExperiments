package base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest;

import base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.base.Base_ROI;

/**
 * alternating exp ROI calc (negative/positive exponential sum of dists), should have even numPtsDist
 * @author 7strb
 *
 */
public class altExpROI extends Base_ROI{
	public altExpROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0, modVal = -1;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += (modVal * Math.pow(dToPt,++i));
			if (i>=numPtsDist){break;}
			modVal *= -1;
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Alternating Exponential sum/diff ROI Calc";}	
}//altExpROI