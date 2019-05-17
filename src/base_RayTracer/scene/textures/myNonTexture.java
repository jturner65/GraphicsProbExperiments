package base_RayTracer.scene.textures;

import base_RayTracer.myColor;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;

//class for non-textured objects
public class myNonTexture extends myTextureHandler{

	public myNonTexture(myScene _scn, myObjShader _shdr) {	super(_scn, _shdr);	}
	@Override
	protected void initTextureVals() {}
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst) {
		return new double[] { diffuseColor.RGB.x*diffConst, diffuseColor.RGB.y*diffConst,diffuseColor.RGB.z*diffConst};
	}

	@Override
	public String showUV() {return "Non-texture";}
	@Override
	public String toString() {return "Non-texture";}
	
}

