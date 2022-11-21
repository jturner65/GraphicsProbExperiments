package base_RayTracer.scene.textures.noiseTextures;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myVector;

//more complex wood txtr - uses turbulence for swirly swirls
public class myWoodTexture extends myNoiseTexture{
	
	public myWoodTexture(myScene _scn, myObjShader _shdr, double _scl) {	super(_scn, _shdr,_scl);}	
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myRTColor diffuseColor, double diffConst) {
		myVector hitVal = getHitLoc(hit);
		double res = getTurbVal(hitVal);
		double sqPtVal = sqPtVal(hitVal) + turbMult *res;
		double distVal = (Math.sin(sqPtVal * periodMult._mag()));
		distVal = 1 - (distVal < 0 ? 0 : distVal);
//		debugMinMaxVals(distVal);
		double[] texTopColor = getClrAra(distVal, hitVal,0,1);
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		if(fastAbs(diffConst - 1.0) > MyMathUtils.EPS){texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;}
		return texTopColor;
	}//getDiffTxtrColor

	@Override
	public String showUV() {		return " Turb Wood Texture No UV";	}
	@Override
	public String toString() {		return "Wood "+super.toString();}

}//myWoodTexture
