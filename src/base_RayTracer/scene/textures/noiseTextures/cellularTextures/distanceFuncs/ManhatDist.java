package base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs;

import base_Math_Objects.vectorObjs.doubles.myVector;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs.base.Base_DistFunc;

public class ManhatDist extends Base_DistFunc {
	@Override
	public double calcDist(myVector v1, myVector v2) { return v1._L1Dist(v2);}
	@Override
	public String toString(){return "Manhattan distance function";}
}//manhatDist