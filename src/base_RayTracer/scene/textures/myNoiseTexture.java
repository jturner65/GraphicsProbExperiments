package base_RayTracer.scene.textures;

import base_RayTracer.myColor;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;
import base_Utils_Objects.myVector;

//noise-based texture
public class myNoiseTexture extends myTextureHandler{
	public double scale;
	public myColor[] colors;		//light/dark colors - inited here so that can be overwritten in initTexturevals	
	public Double[] clrWts;			//weights of each color	
	
	public int numOctaves;		//# of octaves of turbulence
	public double turbMult;		//multiplier for turbulence effect
	//color multipliers for randomness
	public double colorScale, colorMult;
	public boolean rndColors, useFwdTrans;
	
	public myVector periodMult;	//multiplier in each of 3 dirs for periodic noise texture
	//debug quants
	double minVal = Double.MAX_VALUE,
			maxVal = -Double.MAX_VALUE;
	
	public myNoiseTexture(myScene _scn, myObjShader _shdr, double _scl) {	
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
		colors = new myColor[scene.noiseColors.length];
		for(int i =0; i<scene.noiseColors.length;++i ){colors[i] = new myColor(scene.noiseColors[i].RGB.x,scene.noiseColors[i].RGB.y,scene.noiseColors[i].RGB.z);}		
		clrWts = new Double[scene.clrWts.length];
		//normalize wts
		Double sum = 0.0, cnt = 0.0;
		for(Double wt : scene.clrWts){	sum+=wt;cnt++;}
		sum /=cnt;
		for(int i =0; i<clrWts.length;++i){	clrWts[i]=scene.clrWts[i]/sum;}
	}//set colors
	
	protected double getNoiseVal(double x, double y, double z){return scene.noise_3d((float)(x*scale), (float)(y*scale), (float)(z*scale));}	//gives vals between -1 and 1
		
	protected double getNoiseVal(myVector hitLoc){
		hitLoc._mult(scale);
		return scene.noise_3d(hitLoc);									//gives vals between -1 and 1
	}
	
	//turbulence is simulated by taking noise at increasing octaves - sort of like subdividing the noise manifold.
	protected double getTurbVal(myVector tmp){
		tmp._mult(scale);		
		double res = 0, _freqScale = 1.0, _ampScale = 1.0;	//frequency mult of noise, amplitude of 
		for(int i=0;i<numOctaves;++i){
			res += scene.noise_3d((float)(tmp.x*_freqScale), (float)(tmp.y*_freqScale), (float)(tmp.z*_freqScale)) * _ampScale;
			_ampScale *= .5;
			_freqScale *= 1.92;		//not 2 to avoid noise overlap
		}	
		return res;									//gives vals between -1 and 1
	}
	//summed octaves of abs noise - from perlin's 1999 GDC talk
	protected double getAbsTurbVal(myVector tmp){
		tmp._mult(scale);
		double res = 0, _freqScale = 1.0, _ampScale = 1.0;	//frequency mult of noise, amplitude of 
		for(int i=0;i<numOctaves;++i){
			res += fastAbs(scene.noise_3d((float)(tmp.x*_freqScale), (float)(tmp.y*_freqScale), (float)(tmp.z*_freqScale))) * _ampScale;
			_ampScale *= .5;
			_freqScale *= 1.92;		//not 2 to avoid noise overlap
		}	
		return res;									//gives vals between -1 and 1
	}//getAbsTurbVal
	
	//ugh. have to decide whether to use transformed hit loc(for meshes) or not (for prims) - need to build "mesh" prim
	protected myVector getHitLoc(rayHit hit){	return (useFwdTrans ? new myVector(hit.fwdTransHitLoc) : new myVector(hit.hitLoc));}
	
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst) {
		double res = turbMult * getNoiseVal(getHitLoc(hit));
		double val = .5 * res + .5;		//0->1
		
		double[] texTopColor = {val, val ,val};	
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		if(fastAbs(diffConst - 1.0) > epsVal){texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;}
		return texTopColor;
	}//getDiffTxtrColor
	
	//debug min & max vals 
	public void debugMinMaxVals(double val){
		minVal = (minVal > val ? val : minVal);
		maxVal = (maxVal < val ? val : maxVal);
		System.out.println("min and max debug val : " + minVal + " | " + maxVal);
	}
	
	public double linPtVal(myVector vec){return (vec.x*periodMult.x + vec.y*periodMult.y + vec.z*periodMult.z);	}
	public double sqPtVal(myVector vec){return  Math.sqrt((vec.x * vec.x)*periodMult.x + (vec.y * vec.y)*periodMult.y + (vec.z * vec.z)*periodMult.z);}
	//return rgb array from two colors, using passed interpolant, and with passed random value used to provide variability - do not use for cellular texture
	public double[] getClrAra(double distVal, myVector rawPt, int idx0, int idx1){//noise_3d((float)pt.x, (float)pt.y, (float)pt.z);
		myVector pt = new myVector(rawPt);
		pt._mult(colorScale);
		double mult = colorMult;

		double[] rndMult = rndColors ?  new double[]{
				1.0 + (mult *scene.noise_3d((float)pt.x, (float)pt.z, (float)pt.y)),
				1.0 + (mult *scene.noise_3d((float)pt.y, (float)pt.x, (float)pt.z)),
				1.0 + (mult *scene.noise_3d((float)pt.z, (float)pt.y, (float)pt.x))}
				:
				new double[]{1.0,1.0,1.0};
		double[] res = {
				Math.max(0,Math.min(1.0,(colors[idx0].RGB.x) + rndMult[0]*distVal * ((colors[idx1].RGB.x) - (colors[idx0].RGB.x)))),
				Math.max(0,Math.min(1.0,(colors[idx0].RGB.y) + rndMult[1]*distVal * ((colors[idx1].RGB.y) - (colors[idx0].RGB.y)))),
				Math.max(0,Math.min(1.0,(colors[idx0].RGB.z) + rndMult[2]*distVal * ((colors[idx1].RGB.z) - (colors[idx0].RGB.z)))),				
		};						
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