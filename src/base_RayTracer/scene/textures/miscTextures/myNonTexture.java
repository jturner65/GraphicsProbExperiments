package base_RayTracer.scene.textures.miscTextures;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.shaders.myObjShader;
import base_RayTracer.scene.textures.base.Base_TextureHandler;

//class for non-textured objects
public class myNonTexture extends Base_TextureHandler{

	public myNonTexture(Base_Scene _scn, myObjShader _shdr) {	super(_scn, _shdr);	}
	@Override
	protected void initTextureVals() {}
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myRTColor diffuseColor, double diffConst) {
		return new double[] { diffuseColor.x*diffConst, diffuseColor.y*diffConst,diffuseColor.z*diffConst};
	}

	@Override
	public String showUV() {return "Non-texture";}
	@Override
	public String toString() {return "Non-texture";}
	
}

