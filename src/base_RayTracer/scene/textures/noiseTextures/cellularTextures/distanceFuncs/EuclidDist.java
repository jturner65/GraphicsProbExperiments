package base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs;

import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs.base.Base_DistFunc;

public class EuclidDist extends Base_DistFunc {
	@Override
	public double calcDist(myPoint v1, myPoint v2) {return v1._dist(v2);}	
	@Override
	public String toString(){return "Euclidean distance function";}
}//euclidDist