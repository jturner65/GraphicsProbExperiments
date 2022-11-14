package base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest;

import base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.base.Base_ROI;

/**
 * linear log ROI calc ( log sum of dists), should have even numPtsDist
 * @author 7strb
 *
 */
public class linLogROI extends Base_ROI{
	public linLogROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += Math.log(1+dToPt);
			++i;
			if (i>=numPtsDist){break;}
		}
		return dist;
	}//calcROI
	@Override
	public String toString(){return "Linear Log sum ROI Calc";}	
}//linLogROI