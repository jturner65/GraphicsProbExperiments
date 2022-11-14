package base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest;

import base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.base.Base_ROI;

/**
 * inverse exp ROI calc (1/exponential sum of dists)
 * @author 7strb
 *
 */
public class invExpROI extends Base_ROI{
	public invExpROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += Math.pow(dToPt,-(++i));
			if (i>=numPtsDist){break;}
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Inverse Exponential sum ROI Calc";}	
}//invExpROI