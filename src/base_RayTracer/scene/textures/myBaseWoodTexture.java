package base_RayTracer.scene.textures;

import base_RayTracer.myColor;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;
import base_Math_Objects.vectorObjs.doubles.myVector;

//wood texture that looks like imag in proj 4 - based on only single iteration of noise
public class myBaseWoodTexture extends myNoiseTexture{ 	
	public myBaseWoodTexture(myScene _scn,myObjShader _shdr, double _scl) {		super(_scn, _shdr,_scl);	}	
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst) {
		myVector hitVal = getHitLoc(hit);
		double res = getNoiseVal(hitVal);
		double sqPtVal = sqPtVal(hitVal) + turbMult * res;
		//double sqPtVal = Math.sqrt((hit.hitLoc.x * hit.hitLoc.x)*periodMult.x + (hit.hitLoc.y * hit.hitLoc.y)*periodMult.y + (hit.hitLoc.z * hit.hitLoc.z)*periodMult.z) + turbMult *res;
		double distVal = Math.sin(sqPtVal * periodMult._mag());
		//map to slightly more and slightly less than 1 and 0, respectively, and then shelve at 0 and 1
		distVal *= 1.1;
		distVal += .5;	
		//debugMinMaxVals(distVal);
		distVal = (distVal < 0 ? 0 : (distVal > 1 ? 1 : distVal));
		double[] texTopColor = getClrAra(distVal,hit.hitLoc,0,1);
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		if(fastAbs(diffConst - 1.0) > epsVal){
			texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;}
		return texTopColor;
	}//getDiffTxtrColor

	@Override
	public String showUV() {		return " Basic Wood Texture No UV coords";	}
	@Override
	public String toString() {		return "Basic Wood "+super.toString();}
}//myBaseWoodTexture
