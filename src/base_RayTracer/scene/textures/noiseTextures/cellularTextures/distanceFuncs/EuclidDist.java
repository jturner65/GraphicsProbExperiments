package base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs;

import base_Math_Objects.vectorObjs.doubles.myVector;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs.base.Base_DistFunc;

public class EuclidDist extends Base_DistFunc {
	@Override
	public double calcDist(myVector v1, myVector v2) {return v1._dist(v2);}	
	@Override
	public String toString(){return "Euclidean distance function";}
}//euclidDist