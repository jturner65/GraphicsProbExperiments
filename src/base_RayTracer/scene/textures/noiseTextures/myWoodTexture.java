package base_RayTracer.scene.textures.noiseTextures;

import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.shaders.myObjShader;
import base_RayTracer.utils.myRTColor;
import base_Math_Objects.vectorObjs.doubles.myPoint;

//more complex wood txtr - uses turbulence for swirly swirls
public class myWoodTexture extends myNoiseTexture{
	
	public myWoodTexture(Base_Scene _scn, myObjShader _shdr, double _scl) {	super(_scn, _shdr,_scl);}	
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myRTColor diffuseColor, double diffConst) {
		myPoint hitVal = getHitLoc(hit);
		double res = getTurbVal(hitVal);
		double sqPtVal = sqPtVal(hitVal) + turbMult *res;
		double distVal = (Math.sin(sqPtVal * periodMult._mag()));
		distVal = 1 - (distVal < 0 ? 0 : distVal);
//		debugMinMaxVals(distVal);
		double[] texTopColor = getClrAra(distVal, hitVal,0,1, diffConst);
		return texTopColor;
	}//getDiffTxtrColor

	@Override
	public String showUV() {		return " Turb Wood Texture No UV";	}
	@Override
	public String toString() {		return "Wood "+super.toString();}

}//myWoodTexture
