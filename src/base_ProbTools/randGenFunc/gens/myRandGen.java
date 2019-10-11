package base_ProbTools.randGenFunc.gens;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import base_ProbTools.randGenFunc.RandGenDesc;
import base_ProbTools.randGenFunc.funcs.myFleishFunc_Uni;
import base_ProbTools.randGenFunc.funcs.myNormalFunc;
import base_ProbTools.randGenFunc.funcs.myRandVarFunc;
import base_ProbTools.randVisTools.myDistFuncHistVis;
import base_ProbTools.summary.myProbSummary_Dbls;
import base_UI_Objects.*;

/**
 * Provides generation of random variables from prob distributions given a uniform distribution
 */
public abstract class myRandGen implements Comparable<myRandGen> {
	public final int ObjID;
	private static int IDCnt = 0;
	//original data and analysis of it - fl polynomial needs to be built from a sample distribution or from a set of moments
	protected myProbSummary_Dbls summary;
    //random generator to use to generate uniform data - threadsafe
	public final String name;	
	//function name for this randgen
	protected String funcName;
	//descriptor of this random generator
	public RandGenDesc desc;		
	//function this rand gen uses
	protected final myRandVarFunc func;	
	//visualization tool for this random generator
	protected myDistFuncHistVis distVisObj; 
		
	//state flags - bits in array holding relevant info about this random variable function
	private int[] stFlags;						
	public static final int
			debugIDX 					= 0,
			funcSetIDX					= 1;		//whether or not this random variable will be used in a ziggurat solver		
	public static final int numFlags 	= 2;	
   
	public myRandGen(myRandVarFunc _func, String _name) {
		ObjID = IDCnt++;  
		name=_name;
		initFlags();
		func = _func;
		initRandGen();
    }//ctor
	//overriden by transforms
	protected void initRandGen() {
		setFlag(funcSetIDX, true);
		funcName= func.name; 
		desc = new RandGenDesc(func.getQuadSolverName(), funcName, this);
		//func built with summary data - allow for quick access
		summary = func.getSummary();	
		 _setFuncSummaryIndiv();
		distVisObj = null;
	}
	
	//set summary for this object and for function
	public void setFuncSummary(myProbSummary_Dbls _summary) {
		summary = _summary;	
		func.rebuildFuncs(_summary);
		 _setFuncSummaryIndiv();
	}//setFuncSummary
	
	//when new options are specified, rebuild functions as if new summary was specified
	public void setOptionFlags(int[][] _opts) {
		func.setOptionFlags( _opts[func.getRVFType()]);
		func.rebuildFuncs(summary);
		 _setFuncSummaryIndiv();
	}
	
	//called whenever summary object is set/reset
	public abstract void _setFuncSummaryIndiv();	
	
	public void buildDistVisObj(float[] _startRect) {		distVisObj = new myDistFuncHistVis(_startRect, this);	}
    //thread-safe queries for uniform values
    protected long getNextLong() {return ThreadLocalRandom.current().nextLong();}  
    protected int getNextInt() {return ThreadLocalRandom.current().nextInt();  }
    protected double getNextDouble() {return ThreadLocalRandom.current().nextDouble();}
    protected double getUniform01() {
    	long val = ThreadLocalRandom.current().nextLong();
    	return .5+ .5 * val/Long.MAX_VALUE;
    }
    //uniformly between [min,max)
    protected int getUniInt(int min, int max) {    	
    	return ThreadLocalRandom.current().nextInt(min,max);
    }
	
    public myProbSummary_Dbls getSummary() {return summary;}
    public myRandVarFunc getFunc() {return func;}
	public double getMean() {return summary.mean();}
	public double getStd() {return summary.std();}
	public double getVar() {return summary.var();}
	public double getSkew() {return summary.skew();}
	public double getKurt() {return summary.kurt();}    
    
    public abstract double[] getMultiSamples(int num);
    public abstract double[] getMultiFastSamples(int num);
    //return a sample based on func  - momments defined by myRandVarFunc
	public abstract double getSample();
	public abstract double getSampleFast();
	//mapping to go from distribution to uniform 0->1 (from val -> prob p(X<= val))
	public abstract double CDF(double _val);
	//mapping to go from uniform 0->1 to distribution (from p(X<= val) -> val) 
	public abstract double inverseCDF(double _val);
	//alias for CDF
	public double distToUniform(double _val) {return CDF(_val);}
	//alias for inverseCDF
	public double uniformToDist(double _val) {return inverseCDF(_val);}
	//test integral evaluation
	public double testInteg(double min, double max) {		return func.integral_f(min, max);	}
	
	@Override
	public int compareTo(myRandGen othr) {return desc.compareTo(othr.desc);}
	
	
	private final String[] dispMultiStrs = new String[] {"PDF hist",};
	//build dist and hist and also take passed cosine randgen and superimpose the values for its pdf
	public void buildFuncHistCosPlot(int numVals, int numBuckets, double low, double high, myBoundedRandGen cosGen) {
		//first build histogram
		calcHistValsForDisp(numVals, numBuckets);
		//build and set pdf function values
		func.buildFuncPlotVals(numVals, low, high, myRandVarFunc.queryPDFIDX, distVisObj);
		//now use passed cosGen but populate it into this object's distVisObj
		cosGen.func.buildFuncPlotVals(numVals, low, high, myRandVarFunc.queryPDFIDX, distVisObj);
		
		String histName = funcName+" PDF hist",
				gaussName = func.getDispFuncName(myRandVarFunc.queryPDFIDX),
				cosName = cosGen.func.getDispFuncName(myRandVarFunc.queryPDFIDX);
		
		//get min and max histogram values and get min/max/diff y values for larger of two dists, either cosine or gauss
		double[][] minMaxDiffHist = distVisObj.getSpecificMinMaxDiff(histName),//use this for x values		
				minMaxDiffCos = distVisObj.getSpecificMinMaxDiff(cosName),
				minMaxDiffGauss = distVisObj.getSpecificMinMaxDiff(gaussName);
		double[][] minMaxDiff = new double[2][];
		minMaxDiff[0] = minMaxDiffHist[0];
		minMaxDiff[1] = new double[3];
		minMaxDiff[1][0] = (minMaxDiffCos[1][0] < minMaxDiffGauss[1][0]) ? minMaxDiffCos[1][0] : minMaxDiffGauss[1][0];
		minMaxDiff[1][1] = (minMaxDiffCos[1][1] > minMaxDiffGauss[1][1]) ? minMaxDiffCos[1][1] : minMaxDiffGauss[1][1];
		minMaxDiff[1][2] = minMaxDiff[1][1] - minMaxDiff[1][0];
		String[] dispMultiStrs = new String[] {histName, gaussName, cosName};
		distVisObj.setCurMultiDispVis(dispMultiStrs,minMaxDiff);
		distVisObj.setColorVals(cosName,"stroke", new int[] {255,255,0,255});
	}//buildFuncHistCosPlot
	
	
	//synthesize numVals values from low to high to display 
	public void calcFuncValsForDisp(int numVals,double low, double high,  int funcType ) {
		func.buildFuncPlotVals(numVals, low, high, funcType, distVisObj);
	}//calcFValsForDisp
		
	//build display function for histogram
	//Num buckets should be << numVals
	public void calcHistValsForDisp(int numVals, int numBuckets) {
		//x val is distribution/max bucket value, y val is count
		double [] histVals = getMultiSamples(numVals);		
		myProbSummary_Dbls summary = new myProbSummary_Dbls(histVals);
		//build buckets : numBuckets+1 x 2 array; 2nd idxs : idx 0 is lower x value of bucket, y value is count; last entry should always have 0 count
		double[][] distBuckets = summary.calcBucketVals(numBuckets);
		System.out.println("calcHistValsForDisp : numVals : " + numVals + " | size of histVals :"+histVals.length + " | size of distBuckets : "+ distBuckets.length + " | distVisObj is null :  "+ (null==distVisObj));
		
		//min, max and diff values for x axis (rand val) and y axis (counts)
		double[][] minMaxDiffXVals = new double[2][3];
		minMaxDiffXVals[0][0] = summary.getMin();
		minMaxDiffXVals[0][1] = summary.getMax();
		minMaxDiffXVals[0][2] = minMaxDiffXVals[0][1] - minMaxDiffXVals[0][0];
		//bucket count min max diff - y axis
		minMaxDiffXVals[1][0] = 100000;
		minMaxDiffXVals[1][1] = -100000;
		for(int i=0;i<distBuckets.length;++i) {
			minMaxDiffXVals[1][0] = (minMaxDiffXVals[1][0] > distBuckets[i][1] ? distBuckets[i][1] : minMaxDiffXVals[1][0]);
			minMaxDiffXVals[1][1] = (minMaxDiffXVals[1][1] < distBuckets[i][1] ? distBuckets[i][1] : minMaxDiffXVals[1][1]);
		}		
		minMaxDiffXVals[1][2] = minMaxDiffXVals[1][1] - minMaxDiffXVals[1][0];
		
		distVisObj.setValuesHist(funcName+" PDF hist", new int[][] {new int[] {255,0,0,255}, new int[] {255,255,255,255}}, distBuckets, minMaxDiffXVals);
	}//calcDistValsForDisp
	
	//clear out any existing plot evaluations
	public void clearPlotEval() {	distVisObj.clearEvalVals();}
	//change width of visualization object
	public void dataVisSetDispWidth(float dispWidth) {	distVisObj.setDispWidth(dispWidth);}
	//update the visualization name
	public void updateVisName(String _newName) {distVisObj.updateName(_newName);}	
	
	//draw a represntation of this distribution
	public void drawDist(my_procApplet pa) {
		if(distVisObj == null) {			System.out.println("NO Vis Obj");		return;}
		distVisObj.drawVis(pa);
	}//drawDist
		
	//state flag management
	private void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX : 		{break;}	
			case funcSetIDX : 		{break;}	
		}
	}//setFlag		
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}	
	//return string description of rand function
	public String getFuncDataStr() {return func.getFuncDataStr();}
	
	//get short string suitable for key for map
	public String getTransformName() {
		String res = name+"_"+ desc.quadName+"_" + func.getMinDescString();
		return res;
	}
	
	//get short display string
	public String getDispTransName() {
		return name +" "+summary.getMinNumMmntsDesc();
	}

}//class myRandGen

////////////////////////////////////////////////////////////////////////////////////////
// child classes 




