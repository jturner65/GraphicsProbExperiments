package base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs;

import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs.base.Base_DistFunc;

public class ManhatDist extends Base_DistFunc {
	@Override
	public double calcDist(myPoint v1, myPoint v2) { return v1._L1Dist(v2);}
	@Override
	public String toString(){return "Manhattan distance function";}
}//manhatDist