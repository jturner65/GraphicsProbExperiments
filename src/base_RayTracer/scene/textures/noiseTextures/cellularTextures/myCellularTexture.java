package base_RayTracer.scene.textures.noiseTextures.cellularTextures;

import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;

import base_RayTracer.myColor;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;
import base_RayTracer.scene.textures.noiseTextures.myNoiseTexture;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs.EuclidDist;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs.ManhatDist;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.distanceFuncs.base.Base_DistFunc;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.*;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.regionOfInterest.base.Base_ROI;
import base_Math_Objects.MyMathUtils; 
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myCellularTexture extends myNoiseTexture{
	private double avgNumPerCell, mortarThresh;	
	private int maxMVal = 15, numPtsDist;		//max MVal+1 to calc dist for;# of points in neighborhood
	private int[] hitLocIDX;

	private ConcurrentSkipListMap<Double, Integer> pdfs;			//inits in cnstrctr - cumulative pdf - just lookup largest key less than rand #
	private ConcurrentSkipListMap<Double, Integer[]>distToPts;		//declaring so we don't reinit every ray - only a mechanism to quickly hold and sort distances
	private Random seededGen;
	
	private Base_DistFunc distFunc;			//function for dist calculation
	private Base_ROI roiFunc;					//function for region of interest calculation
	
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
			case 0 : {distFunc = new ManhatDist();break;}
			case 1 : {distFunc = new EuclidDist();break;}
			default : {distFunc = new EuclidDist();break;}
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
		if(fastAbs(diffConst - 1.0) > MyMathUtils.EPS){texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;}
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