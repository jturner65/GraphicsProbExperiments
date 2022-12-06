package base_RayTracer.scene.textures.noiseTextures;

import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.shaders.myObjShader;
import base_RayTracer.scene.textures.base.Base_TextureHandler;
import base_RayTracer.utils.myRTColor;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

//noise-based texture
public class myNoiseTexture extends Base_TextureHandler{
	protected double scale;
	protected myRTColor[] colors;		//light/dark colors - inited here so that can be overwritten in initTexturevals	
	protected Double[] clrWts;			//weights of each color	
	
	protected int numOctaves;		//# of octaves of turbulence
	protected double turbMult;		//multiplier for turbulence effect
	//color multipliers for randomness
	protected double colorScale, colorMult;
	protected boolean rndColors, useFwdTrans;
	
	protected myVector periodMult;	//multiplier in each of 3 dirs for periodic noise texture

	public myNoiseTexture(Base_Scene _scn, myObjShader _shdr, double _scl) {	
		super(_scn,_shdr);	
		scale = _scl;
	}

	@Override
	protected void initTextureVals() {
		setColorsAndWts();
		numOctaves = scene.numOctaves;
		turbMult = scene.turbMult;
		periodMult = new myVector(scene.pdMult);
		colorScale = scene.colorScale;
		colorMult = scene.colorMult;
		rndColors = scene.rndColors;
		useFwdTrans = scene.useFwdTrans;
	}
	
	//set colors
	protected void setColorsAndWts(){
		colors = new myRTColor[scene.noiseColors.length];
		for(int i =0; i<scene.noiseColors.length;++i ){colors[i] = new myRTColor(scene.noiseColors[i].x,scene.noiseColors[i].y,scene.noiseColors[i].z);}		
		clrWts = new Double[scene.clrWts.length];
		//normalize wts
		Double sum = 0.0, cnt = 0.0;
		for(Double wt : scene.clrWts){	sum+=wt;cnt++;}
		sum /=cnt;
		for(int i =0; i<clrWts.length;++i){	clrWts[i]=scene.clrWts[i]/sum;}
	}//set colors
	
	protected double getNoiseVal(double x, double y, double z){return scene.perlinNoise3D((float)(x*scale), (float)(y*scale), (float)(z*scale));}	//gives vals between -1 and 1
		
	protected double getNoiseVal(myPoint hitLoc){
		hitLoc._mult(scale);
		return scene.perlinNoise3D(hitLoc);									//gives vals between -1 and 1
	}
	
	//turbulence is simulated by taking noise at increasing octaves - sort of like subdividing the noise manifold.
	protected double getTurbVal(myPoint tmp){
		tmp._mult(scale);		
		double res = 0, _freqScale = 1.0, _ampScale = 1.0;	//frequency mult of noise, amplitude of 
		for(int i=0;i<numOctaves;++i){
			res += scene.perlinNoise3D((float)(tmp.x*_freqScale), (float)(tmp.y*_freqScale), (float)(tmp.z*_freqScale)) * _ampScale;
			_ampScale *= .5;
			_freqScale *= 1.92;		//not 2 to avoid noise overlap
		}	
		return res;									//gives vals between -1 and 1
	}
	//summed octaves of abs noise - from perlin's 1999 GDC talk
	protected double getAbsTurbVal(myPoint tmp){
		tmp._mult(scale);
		double res = 0, _freqScale = 1.0, _ampScale = 1.0;	//frequency mult of noise, amplitude of 
		for(int i=0;i<numOctaves;++i){
			res += Math.abs(scene.perlinNoise3D((float)(tmp.x*_freqScale), (float)(tmp.y*_freqScale), (float)(tmp.z*_freqScale))) * _ampScale;
			_ampScale *= .5;
			_freqScale *= 1.92;		//not 2 to avoid noise overlap
		}	
		return res;									//gives vals between -1 and 1
	}//getAbsTurbVal
	
	//ugh. have to decide whether to use transformed hit loc(for meshes) or not (for prims) - need to build "mesh" prim
	protected myPoint getHitLoc(rayHit hit){	return (useFwdTrans ? new myPoint(hit.fwdTransHitLoc) : new myPoint(hit.hitLoc));}
	
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myRTColor diffuseColor, double diffConst) {
		double res = turbMult * getNoiseVal(getHitLoc(hit));
		double val = .5 * res + .5;		//0->1
		
		double[] texTopColor = {val, val ,val};	
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		if(Math.abs(diffConst - 1.0) > MyMathUtils.EPS){texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;}
		return texTopColor;
	}//getDiffTxtrColor
	
	public double linPtVal(myPoint vec){return (vec.x*periodMult.x + vec.y*periodMult.y + vec.z*periodMult.z);	}
	public double sqPtVal(myPoint vec){return  Math.sqrt((vec.x * vec.x)*periodMult.x + (vec.y * vec.y)*periodMult.y + (vec.z * vec.z)*periodMult.z);}
	/**
	 * return rgb array from two colors, using passed interpolant, and with passed random value used to provide variability. Scale by diffConst
	 * @param distVal
	 * @param rawPt
	 * @param idx0
	 * @param idx1
	 * @param diffConst
	 * @return
	 */
	public double[] getClrAra(double distVal, myPoint rawPt, int idx0, int idx1, double diffConst){//noise_3d((float)pt.x, (float)pt.y, (float)pt.z);
		myPoint pt = new myPoint(rawPt);
		pt._mult(colorScale);
		double mult = colorMult;

		double[] rndMult = rndColors ?  new double[]{
				1.0 + (mult *scene.perlinNoise3D((float)pt.x, (float)pt.z, (float)pt.y)),
				1.0 + (mult *scene.perlinNoise3D((float)pt.y, (float)pt.x, (float)pt.z)),
				1.0 + (mult *scene.perlinNoise3D((float)pt.z, (float)pt.y, (float)pt.x))}
				:
				new double[]{1.0,1.0,1.0};
		double[] res = {
				MyMathUtils.max(0,MyMathUtils.min(1.0,(colors[idx0].x) + rndMult[0]*distVal * ((colors[idx1].x) - (colors[idx0].x)))),
				MyMathUtils.max(0,MyMathUtils.min(1.0,(colors[idx0].y) + rndMult[1]*distVal * ((colors[idx1].y) - (colors[idx0].y)))),
				MyMathUtils.max(0,MyMathUtils.min(1.0,(colors[idx0].z) + rndMult[2]*distVal * ((colors[idx1].z) - (colors[idx0].z)))),				
		};		
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		if(Math.abs(diffConst - 1.0) > MyMathUtils.EPS){res[R] *= diffConst;res[G] *= diffConst;res[B] *= diffConst;}
		return res;
	}//getClrAra

	@Override
	public String showUV() {		return "Noise Texture No UV";}
	@Override
	public String toString(){		
		String res = "Noise Texture: Scale : " + String.format("%.2f", scale)+" Colors : \n";
		for(int i = 0; i<colors.length;++i){	res+= "\t"+(i+1) +" : " + colors[i].toString() + "\n";}
		res += "# Octaves : " + numOctaves + "| Turbulence Multiplier : " +  String.format("%.2f", turbMult) + " | Period Mult : " + periodMult + "\n"; 
				
		return res;
	}
}//myNoiseTexture