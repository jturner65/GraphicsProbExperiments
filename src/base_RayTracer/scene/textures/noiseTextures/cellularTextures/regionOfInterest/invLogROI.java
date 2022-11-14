package base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest;

import base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.base.Base_ROI;

/**
 * inverse ROI calc ( 1/log sum of dists), should have even numPtsDist
 * @author 7strb
 *
 */
public class invLogROI extends Base_ROI{
	public invLogROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += 1.0/Math.log(1+dToPt);
			++i;
			if (i>=numPtsDist){break;}
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Inverse Log sum ROI Calc";}	
}//invLogROI