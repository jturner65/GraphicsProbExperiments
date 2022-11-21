package base_RayTracer.scene.textures.miscTextures;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;
import base_RayTracer.scene.textures.base.Base_TextureHandler;

//class for non-textured objects
public class myNonTexture extends Base_TextureHandler{

	public myNonTexture(myScene _scn, myObjShader _shdr) {	super(_scn, _shdr);	}
	@Override
	protected void initTextureVals() {}
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myRTColor diffuseColor, double diffConst) {
		return new double[] { diffuseColor.RGB.x*diffConst, diffuseColor.RGB.y*diffConst,diffuseColor.RGB.z*diffConst};
	}

	@Override
	public String showUV() {return "Non-texture";}
	@Override
	public String toString() {return "Non-texture";}
	
}

