package base_RayTracer;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

import processing.core.PImage;
import base_Utils_Objects.*;

//class to handle base texture functionality

public abstract class myTextureHandler {
	public myScene scene;
	public myObjShader shdr;
	
	protected static final int hashPrime1 = 1572869;
	protected static final int hashPrime2 = 6291469;
	//used for worley txtrs
	protected final int[][] nghbrHdCells = new int[][]{
		{ 0,  0,  0},{ 0,  0,  1},{ 0,  0, -1},{ 0,  1,  0},{ 0,  1,  1},{ 0,  1, -1},{ 0, -1,  0},{ 0, -1,  1},{ 0, -1, -1},
		{ 1,  0,  0},{ 1,  0,  1},{ 1,  0, -1},{ 1,  1,  0},{ 1,  1,  1},{ 1,  1, -1},{ 1, -1,  0},{ 1, -1,  1},{ 1, -1, -1},
		{-1,  0,  0},{-1,  0,  1},{-1,  0, -1},{-1,  1,  0},{-1,  1,  1},{-1,  1, -1},{-1, -1,  0},{-1, -1,  1},{-1, -1, -1}		
	};
	
	public boolean[] txtFlags;					//various state-related flags for this object
	public static final int 
			txtrdTopIDX			= 0,
			txtrdBtmIDX			= 1;			
	public static final int numFlags = 2;	

	//used as array indices
	public static final int R = 0, G = 1, B = 2;
	public static final double epsVal = myScene.epsVal;//.0000001;

	public myTextureHandler(myScene _scn, myObjShader _shdr) {
		scene = _scn;
		shdr = _shdr;
		initFlags();		
		initTextureVals();		
	}
	protected float fastAbs(float x) {return x>0?x:-x;}
	protected double fastAbs(double x) {return x>0?x:-x;}
	// This method is a *lot* faster than using (int)Math.floor(x)
	protected int fastfloor(float x) { return x>0 ? (int)x : (int)x-1;}
	protected int fastfloor(double x) { return x>0 ? (int)x : (int)x-1;}
	
	
	public void initFlags(){txtFlags = new boolean[numFlags];for(int i=0; i<numFlags;++i){txtFlags[i]=false;}}
	protected abstract void initTextureVals();
	public abstract double[] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst);  	
	public abstract String showUV();
	  	
	public String toString(){
		String res = "Shader Texture : ";
		return res;
	}
}//myTextureHandler

//class for non-textured objects
class myNonTexture extends myTextureHandler{

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

class myImageTexture extends myTextureHandler{
	//the image to be used as a texture to cover this object
	public PImage myTextureTop, myTextureBottom;

	public myImageTexture(myScene _scn, myObjShader _shdr) {
		super(_scn,_shdr);
	}
	//initialize constants used to store extremal texture map conversion coords
	@Override
	protected void initTextureVals(){
	    if (scene.scFlags[myScene.glblTxtrdTopIDX]){
	    	txtFlags[txtrdTopIDX] = scene.scFlags[myScene.glblTxtrdTopIDX];
	    	myTextureTop = scene.currTextureTop;
	    	myTextureTop.loadPixels();		    } 
	    else {					       myTextureTop = null;	    }
	    
	    if (scene.scFlags[myScene.glblTxtrdBtmIDX]){      	
	    	txtFlags[txtrdBtmIDX] =  scene.scFlags[myScene.glblTxtrdBtmIDX];
	    	myTextureBottom =  scene.currTextureBottom;		
	    	myTextureBottom.loadPixels();	     } 
	    else {	       					myTextureBottom = null;	    }
  	}
  	//interpolated via UV
	protected double[] getTextureColor(rayHit hit, PImage myTexture){
  		double [] texColor = new double[3];
  		
  		double[] tCoords = hit.obj.findTxtrCoords(hit.hitLoc, myTexture, hit.transRay.getTime());
  		double v = tCoords[1],u = tCoords[0];
  		//texColorRC - R is row(v), C is column(u)
  		
  		int uInt = (int)u, vInt = (int)v,
  		idx00 = vInt * myTexture.width + uInt, idx10 = idx00 + myTexture.width,idx01 = idx00 + 1,idx11 = idx10 + 1;
  		myColor texColorVal00 = new myColor(myTexture.pixels[idx00]), texColorVal10 = new myColor(myTexture.pixels[idx10]), 
  				texColorVal01 = new myColor(myTexture.pixels[idx01]), texColorVal11 = new myColor(myTexture.pixels[idx11]);
  	  
  		double uMFlU = u - uInt, vMFlV = v - vInt;  		
 		myColor texColorVal0 = texColorVal00.interpColor(uMFlU, texColorVal01), texColorVal1 = texColorVal10.interpColor(uMFlU,texColorVal11),texColorVal = texColorVal0.interpColor(vMFlV, texColorVal1);
  	  
  		texColor[R] = texColorVal.RGB.x;
  		texColor[G] = texColorVal.RGB.y;
  		texColor[B] = texColorVal.RGB.z;   
  		return texColor;   
  	}//get texture color at pixel
  	
  	public double [] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst){
  		double[] texTopColor = {0, 0 ,0}, texBotColor = {0, 0, 0};	
		if (txtFlags[txtrdTopIDX]){	texTopColor = getTextureColor(hit,myTextureTop);	} //get color information from texture on top of object at specific point of intersection
		else {						texTopColor[R] = diffuseColor.RGB.x;texTopColor[G] = diffuseColor.RGB.y;texTopColor[B] = diffuseColor.RGB.z;}//add if checking for texturedBottom
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;

		if (txtFlags[txtrdBtmIDX]){//get color information from texture on b of object at specific point of intersection
			texBotColor = getTextureColor(hit,myTextureBottom);
			texBotColor[R] *= diffConst;texBotColor[G] *= diffConst;texBotColor[B] *= diffConst;    
		} 
		return texTopColor;  	
  	}//getDiffTxtrColor
  	
	public void setMyTextureTop(PImage myTexture){    myTextureTop = myTexture;      myTextureTop.loadPixels();  }
	public void setMyTextureBottom(PImage myTexture){    myTextureBottom = myTexture;      myTextureBottom.loadPixels();  }  
	
	public String showUV(){
		String result = "";
		if (myTextureTop != null){		result += " | texture w,h :" +  myTextureTop.width + ", " + myTextureTop.height;	}
		return result;
	}//showUV
	public String toString(){
		String result = "";
		if (myTextureTop != null){		result += " | texture w,h :" +  myTextureTop.width + ", " + myTextureTop.height;	}
		return result;
	}//toString

	
}//myImageTexture

//class holding multiple textures, to provide complex surfaces
class myCompoundTexture extends myTextureHandler{
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




//noise-based texture
class myNoiseTexture extends myTextureHandler{
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

//wood texture that looks like imag in proj 4 - based on only single iteration of noise
class myBaseWoodTexture extends myNoiseTexture{ 	
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

//more complex wood txtr - uses turbulence for swirly swirls
class myWoodTexture extends myNoiseTexture{
	
	public myWoodTexture(myScene _scn, myObjShader _shdr, double _scl) {	super(_scn, _shdr,_scl);}	
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst) {
		myVector hitVal = getHitLoc(hit);
		double res = getTurbVal(hitVal);
		double sqPtVal = sqPtVal(hitVal) + turbMult *res;
		double distVal = (Math.sin(sqPtVal * periodMult._mag()));
		distVal = 1 - (distVal < 0 ? 0 : distVal);
//		debugMinMaxVals(distVal);
		double[] texTopColor = getClrAra(distVal, hitVal,0,1);
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		if(fastAbs(diffConst - 1.0) > epsVal){texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;}
		return texTopColor;
	}//getDiffTxtrColor

	@Override
	public String showUV() {		return " Turb Wood Texture No UV";	}
	@Override
	public String toString() {		return "Wood "+super.toString();}

}//myWoodTexture

//more complex marble
class myMarbleTexture extends myNoiseTexture{

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

class myCellularTexture extends myNoiseTexture{
	private double avgNumPerCell, mortarThresh;	
	private int maxMVal = 15, numPtsDist;		//max MVal+1 to calc dist for;# of points in neighborhood
	private int[] hitLocIDX;

	private ConcurrentSkipListMap<Double, Integer> pdfs;			//inits in cnstrctr - cumulative pdf - just lookup largest key less than rand #
	private ConcurrentSkipListMap<Double, Integer[]>distToPts;		//declaring so we don't reinit every ray - only a mechanism to quickly hold and sort distances
	private Random seededGen;
	
	private myDistFunc distFunc;			//function for dist calculation
	private myROI roiFunc;					//function for region of interest calculation
	
	public myCellularTexture(myScene _scn,myObjShader _shdr, double _scl) {	
		super(_scn, _shdr,_scl);
		avgNumPerCell = scene.avgNumPerCell;
		mortarThresh = scene.mortarThresh;
		numPtsDist = scene.numPtsDist;
		
		switch (scene.roiFunc){
			case 0 : {roiFunc = new nearestROI(numPtsDist); break;}			//linear sum of x neighbor dists
			case 1 : {roiFunc = new altLinROI(numPtsDist); break;}			//alternating linear sum
			case 2 : {roiFunc = new altInvLinROI(numPtsDist); break;}		//alternating inverse linear sum of x neighbor dists
			case 3 : {roiFunc = new altExpROI(numPtsDist); break;}			//alternating Exp Sum of x neighbor dists
			case 4 : {roiFunc = new altLogROI(numPtsDist); break;}			//alternating log Sum of x neighbor dists
			case 5 : {roiFunc = new linExpROI(numPtsDist); break;}			//linear sum of exp of x neighbor dists
			case 6 : {roiFunc = new linLogROI(numPtsDist); break;}			//linear sum of log x neighbor dists
			case 7 : {roiFunc = new invExpROI(numPtsDist); break;}			//inverse exp sum of x neighbor dists
			case 8 : {roiFunc = new invLogROI(numPtsDist); break;}			//inv log sum of x neighbor dists
			default : {roiFunc = new altLinROI(numPtsDist); break;}			//alternating linear sum
		}
		switch (scene.distFunc){
			case 0 : {distFunc = new manhatDist();break;}
			case 1 : {distFunc = new euclidDist();break;}
			default : {distFunc = new euclidDist();break;}
		}
		
		pdfs = new ConcurrentSkipListMap<Double,Integer>();
		distToPts = new ConcurrentSkipListMap<Double,Integer[]>(); 
		double lastDist = 1.0/Math.pow(Math.E, avgNumPerCell), cumProb = lastDist;
		//System.out.println("i:"+0+" pdfs key:"+cumProb+" Val : "+0);
		for (int i = 1; i<maxMVal;++i){	
			lastDist *= (avgNumPerCell/(1.0*i));			//build poisson dist 
			cumProb += lastDist;							//build CPDF
			pdfs.put(cumProb, i);
		}	
		
		hitLocIDX = new int[]{0,0,0};
		seededGen = new Random(); 
	}//myCellularTexture
	
	public int hashInts(int x, int y, int z){		return (x * hashPrime1 + y * hashPrime2 + z);}
	//get # of points for a particular cell given the passed seeded probability
	protected int getNumPoints(){
		double prob = seededGen.nextDouble();
		return pdfs.get((null == pdfs.lowerKey(prob) ? pdfs.firstKey() : pdfs.lowerKey(prob)));
	}	
	@Override
	public double[] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst) {
		distToPts.clear();
		myVector hitVal = getHitLoc(hit);
		hitVal._mult(scale);		//increasing scale here will proportionally decrease the size of the equal-hashed cubes
		hitLocIDX[0] = fastfloor(hitVal.x);	hitLocIDX[1] = fastfloor(hitVal.y);	hitLocIDX[2] = fastfloor(hitVal.z);
		//need to hash these ints, use resultant hash to be key in prob calc
		Integer[] cellLoc;
		int brickClrIDX=2;
		for(int i = 0; i<nghbrHdCells.length; ++i){
			cellLoc = new Integer[]{hitLocIDX[0] + nghbrHdCells[i][0],hitLocIDX[1]+ nghbrHdCells[i][1],hitLocIDX[2]+ nghbrHdCells[i][2]};
			int seed = hashInts(cellLoc[0],cellLoc[1],cellLoc[2]);
			seededGen.setSeed(seed);
			double prob = seededGen.nextDouble();		
			int numPoints = pdfs.get((null == pdfs.lowerKey(prob) ? pdfs.firstKey() : pdfs.lowerKey(prob)));			
			myVector pt;
			for(int j =0;j<numPoints;++j){
				pt = new myVector(cellLoc[0]+seededGen.nextDouble(),cellLoc[1]+seededGen.nextDouble(),cellLoc[2]+seededGen.nextDouble());
				distToPts.put(distFunc.calcDist(hitVal, pt), cellLoc);
			}//for each point
			//System.out.println("Hash : " + seed + " # points :  " + numPoints + " for vals "+ hitLocIDX[0]+"|"+hitLocIDX[1]+"|"+hitLocIDX[2]);
		}//for each cell
		//by  here we have sorted dist to pts values
		Double[] orderedKeys = distToPts.keySet().toArray(new Double[distToPts.size()]);
		double dist = roiFunc.calcROI(orderedKeys);
		dist = (dist < 0 ? 0 : dist > 1 ? 1 : dist);
		//this.debugMinMaxVals(dist);
		//based on dist values, choose color - below some threshold have mortar, above some threshold, choose color randomly (with cell seed for this cell)		
		if(dist < mortarThresh){		brickClrIDX = 0;	} //mortar
		else {
			double res;
			Integer[] cellLoc0 = distToPts.get(orderedKeys[0]);
					//,cellLoc1 = distToPts.get(orderedKeys[1]);
			int seed = hashInts(cellLoc0[0],cellLoc0[1],cellLoc0[2]);
			seededGen.setSeed(seed);
			res = seededGen.nextDouble();
			brickClrIDX = 2*(1+(fastfloor(((colors.length/2) - 1) * res)));
		}		
		double[] texTopColor = getClrAra(.65, hitVal, brickClrIDX,brickClrIDX+1);  // pass idxs of colors to interp between - want different colors for different "stones"
		
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		if(fastAbs(diffConst - 1.0) > epsVal){texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;}
		return texTopColor;
	}//getDiffTxtrColor	
	
	@Override
	public String showUV() {		return " Cellular Texture No UV";	}
	@Override
	public String toString() {	
		String res = "Cellular "+super.toString() ;
		res += "\t# of pts considered in " + distFunc.toString() +" Calc's " + roiFunc.toString() + " : " + numPtsDist + "\n";
		res += "\tAverage # of random points per cell " + String.format("%.2f", avgNumPerCell) + " | Dist threshold for 'mortar' color between cells :" + String.format("%.3f", mortarThresh)+"\n";				
		return res;
	}
}//myCellularTexture

//distance function pointers
abstract class myDistFunc {public abstract double calcDist(myVector v1, myVector v2); }
class euclidDist extends myDistFunc {
	@Override
	public double calcDist(myVector v1, myVector v2) {return v1._dist(v2);}	
	@Override
	public String toString(){return "Euclidean distance function";}
}//euclidDist
class manhatDist extends myDistFunc {
	@Override
	public double calcDist(myVector v1, myVector v2) { return v1._L1Dist(v2);}
	@Override
	public String toString(){return "Manhattan distance function";}
}//manhatDist

//ROI calculation - how to distinguish points and build regions
abstract class myROI { 
	public int numPtsDist;			//# of points to consider in ROI calc				
	public myROI(int _numPts){numPtsDist = _numPts;}
	public abstract double calcROI(Double[] orderedKeys);
	public double fixDist(double dist){
		if(dist < 0){dist *= -1;}
		if(dist > 1.0){dist = 1.0/dist;}
		return dist;
	}
}
//find nearest x pts and return their distance
class nearestROI extends myROI{
	public nearestROI(int _numPts) {super(_numPts);}
	@Override
	public double calcROI(Double[] orderedKeys) {
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){	dist += dToPt;	i++;	if (i>=numPtsDist){break;}}
		return fixDist(dist);
	}
	@Override
	public String toString(){return "Nearest " +numPtsDist+" ROI Calc";}		
}
//alternating ROI calc (negative/positive sum of dists), should have even numPtsDist
class altLinROI extends myROI{
	public altLinROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0, modVal = -1;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += (modVal * dToPt);
			i++;
			if (i>=numPtsDist){break;}
			modVal *= -1;
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Alternating Linear sum/diff ROI Calc";}	
}//altLinROI

//alternating inv ROI calc (negative/positive sum of dists), should have even numPtsDist
class altInvLinROI extends myROI{
	public altInvLinROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0, modVal = -1;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += 1.0/(modVal * dToPt);
			i++;
			if (i>=numPtsDist){break;}
			modVal *= -1;
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Alternating Linear sum/diff ROI Calc";}	
}//altLinROI

//alternating exp ROI calc (negative/positive exponential sum of dists), should have even numPtsDist
class altExpROI extends myROI{
	public altExpROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0, modVal = -1;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += (modVal * Math.pow(dToPt,++i));
			if (i>=numPtsDist){break;}
			modVal *= -1;
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Alternating Exponential sum/diff ROI Calc";}	
}//altExpROI

//alternating log ROI calc (negative/positive log sum of dists), should have even numPtsDist
class altLogROI extends myROI{
	public altLogROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0, modVal = -1;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += (modVal * Math.log(1+dToPt));
			i++;
			if (i>=numPtsDist){break;}
			modVal *= -1;
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Alternating Log sum/diff ROI Calc";}	
}//altLogROI
//linear exp ROI calc (exponential sum of dists)
class linExpROI extends myROI{
	public linExpROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){			
			dist += Math.pow(dToPt,++i);
			if (i>=numPtsDist){break;}
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Linear Exponential sum ROI Calc";}	
}//linExpROI

//linear log ROI calc ( log sum of dists), should have even numPtsDist
class linLogROI extends myROI{
	public linLogROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += Math.log(1+dToPt);
			i++;
			if (i>=numPtsDist){break;}
		}
		return dist;
	}//calcROI
	@Override
	public String toString(){return "Linear Log sum ROI Calc";}	
}//linLogROI
//inverse exp ROI calc (1/exponential sum of dists)
class invExpROI extends myROI{
	public invExpROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += Math.pow(dToPt,-(++i));
			if (i>=numPtsDist){break;}
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Inverse Exponential sum ROI Calc";}	
}//invExpROI

//inverse ROI calc ( 1/log sum of dists), should have even numPtsDist
class invLogROI extends myROI{
	public invLogROI(int _numPts) {super(_numPts);	}
	@Override
	public double calcROI(Double[] orderedKeys) {		
		int i =0;
		double dist = 0;
		for(Double dToPt : orderedKeys){
			dist += 1.0/Math.log(1+dToPt);
			i++;
			if (i>=numPtsDist){break;}
		}
		return fixDist(dist);
	}//calcROI
	@Override
	public String toString(){return "Inverse Log sum ROI Calc";}	
}//invLogROI