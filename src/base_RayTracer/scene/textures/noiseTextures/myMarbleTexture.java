package base_RayTracer.scene.textures.noiseTextures;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.shaders.myObjShader;
import base_Math_Objects.vectorObjs.doubles.myPoint;

//more complex marble
public class myMarbleTexture extends myNoiseTexture{

	public myMarbleTexture(Base_Scene _scn,myObjShader _shdr, double _scl) {	super(_scn, _shdr,_scl);}
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myRTColor diffuseColor, double diffConst) {
		myPoint hitVal = getHitLoc(hit);
		double res = getAbsTurbVal(hitVal);//gives vals between 0 and 1
		double sptVal = linPtVal(hitVal)/periodMult._mag() + turbMult * res;
		//double sptVal = hit.hitLoc.x + res;
		double distVal = .5*Math.sin(sptVal) + .5;

		double[] texTopColor = getClrAra(distVal, hitVal,0,1, diffConst);
		return texTopColor;
	}//getDiffTxtrColor
	
	@Override
	public String showUV() {		return " Marble Texture No UV";	}
	@Override
	public String toString() {
		String res = "Marble "+super.toString();
		return res;
	}
}//myMarbleTexture

