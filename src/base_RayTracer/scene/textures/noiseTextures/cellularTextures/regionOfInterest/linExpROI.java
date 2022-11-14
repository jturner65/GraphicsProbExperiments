package base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest;

import base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.base.Base_ROI;

/**
 * linear exp ROI calc (exponential sum of dists)
 * @author 7strb
 *
 */
public class linExpROI extends Base_ROI{
	public linExpROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){			
			dist += Math.pow(dToPt,++i);
			if (i>=numPtsDist){break;}
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Linear Exponential sum ROI Calc";}	
}//linExpROI