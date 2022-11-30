package base_RayTracer.scene.textures.miscTextures;

import java.util.ArrayList;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.shaders.myObjShader;
import base_RayTracer.scene.textures.base.Base_TextureHandler;
import base_RayTracer.scene.textures.imageTextures.myImageTexture;
import base_RayTracer.scene.textures.noiseTextures.myNoiseTexture;

/**
 * class holding multiple textures, to provide complex surfaces
 * @author 7strb
 *
 */
public class myCompoundTexture extends Base_TextureHandler{
	//list of potential image textures
	private ArrayList<myImageTexture> imgTxtrs;
	//list of pcg noise-based textures
	private ArrayList<myNoiseTexture> noiseTxtrs;		
	
	//weights for each texture
	private ArrayList<Double> imgWts;			
	private ArrayList<Double> nseWts;	
	
	public myCompoundTexture(Base_Scene _scn, myObjShader _shdr) {
		super(_scn, _shdr);
		imgTxtrs = new ArrayList<myImageTexture>();
		imgWts = new ArrayList<Double>();
		noiseTxtrs = new ArrayList<myNoiseTexture>();
		nseWts = new ArrayList<Double>();
	}

	@Override
	protected void initTextureVals() {}

	@Override
	public double[] getDiffTxtrColor(rayHit hit, myRTColor diffuseColor, double diffConst) {
		//First calc all img-based colors
		double[] tmpColor, resColor= new double[3];
		for (int i=0;i<imgTxtrs.size();++i) {
			tmpColor = imgTxtrs.get(i).getDiffTxtrColor(hit, diffuseColor, diffConst);
			for(int j=0;j<resColor.length;++j) {resColor[i]+=tmpColor[i];}
		}
		//Then get all noise-based texture colors
		for (int i=0;i<noiseTxtrs.size();++i) {
			tmpColor = noiseTxtrs.get(i).getDiffTxtrColor(hit, diffuseColor, diffConst);
			for(int j=0;j<resColor.length;++j) {resColor[i]+=tmpColor[i];}			
		}
		
		// TODO Auto-generated method stub
		return resColor;
	}

	@Override
	public String showUV() {
		String res = "Compound Texture :\n\tImage Based ("+imgTxtrs.size() +") :\n";
		for (int i=0;i<imgTxtrs.size();++i) {
			res+="\tWt:"+imgWts.get(i) +" : "+imgTxtrs.get(i)+"\n";	
		}
		res +="\n\tTexture Based ("+noiseTxtrs.size() +") :\n";
		for (int i=0;i<noiseTxtrs.size();++i) {
			res+="\tWt:"+nseWts.get(i) +" : "+noiseTxtrs.get(i)+"\n";			
		}
		
		return res;
	}
	
	
	
}//class myCompoundTexture
