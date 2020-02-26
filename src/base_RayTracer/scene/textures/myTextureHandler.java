package base_RayTracer.scene.textures;

import base_RayTracer.myColor;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;
import base_Math_Objects.vectorObjs.doubles.myVector;

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