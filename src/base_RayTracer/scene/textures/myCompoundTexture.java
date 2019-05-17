package base_RayTracer.scene.textures;

import java.util.ArrayList;

import base_RayTracer.myColor;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;

//class holding multiple textures, to provide complex surfaces
public class myCompoundTexture extends myTextureHandler{
	//list of potential image textures
	private ArrayList<myImageTexture> imgTxtrs;
	//list of pcg noise-based textures
	private ArrayList<myNoiseTexture> noiseTxtrs;		
	
	//weights for each texture
	private double imgWt;			
	private ArrayList<Double> nseWts;	
	
	public myCompoundTexture(myScene _scn, myObjShader _shdr) {
		super(_scn, _shdr);
		imgTxtrs = new ArrayList<myImageTexture>();
		imgWt = 0;
		noiseTxtrs = new ArrayList<myNoiseTexture>();
		nseWts = new ArrayList<Double>();
	}

	@Override
	protected void initTextureVals() {}

	@Override
	public double[] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String showUV() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
}
