package base_RayTracer.scene.textures;

import base_RayTracer.myColor;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;
import base_Math_Objects.vectorObjs.doubles.myVector;

//more complex marble
public class myMarbleTexture extends myNoiseTexture{

	public myMarbleTexture(myScene _scn,myObjShader _shdr, double _scl) {	super(_scn, _shdr,_scl);}
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst) {
		myVector hitVal = getHitLoc(hit);
		double res = getAbsTurbVal(hitVal);//gives vals between 0 and 1
		double sptVal = linPtVal(hitVal)/periodMult._mag() + turbMult * res;
		//double sptVal = hit.hitLoc.x + res;
		double distVal = .5*Math.sin(sptVal) + .5;

		double[] texTopColor = getClrAra(distVal, hitVal,0,1);
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		if(fastAbs(diffConst - 1.0) > myScene.epsVal){texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;}
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

