package graphProbExp_PKG;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Provides generation of random variables from prob distributions given a uniform distribution
 */
public abstract class myRandGen implements Comparable<myRandGen> {
	public final int ObjID;
	private static int IDCnt = 0;
	//original data and analysis of it - fl polynomial needs to be built from a sample distribution or from a set of moments
	protected myProbSummary summary;
    //random generator to use to generate uniform data - threadsafe
	public final String name;	
	//descriptor of this random generator
	public RandGenDesc desc;		
	//function this rand gen uses
	protected myRandVarFunc func;	
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
	
	public void initRandGen() {
		setFlag(funcSetIDX, true);
		desc = new RandGenDesc(func.getQuadSolverName(), func.name, this);
		//func built with summary data - allow for quick access
		summary = func.summary;	
		 _setFuncSummaryIndiv();
		distVisObj = null;
	}
	
	//set summary for this object and for function
	public void setFuncSummary(myProbSummary _summary) {
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
		
		String histName = func.name+" PDF hist",
				gaussName = func.getDispFuncName(myRandVarFunc.queryPDFIDX),
				cosName = cosGen.func.getDispFuncName(myRandVarFunc.queryPDFIDX);
		
		//get min and max histogram values and get min/max/diff y values for larger of two dists, either cosine or gauss
		double[][] minMaxDiffHist = distVisObj.getSpecificMinMaxDiff(func.name+" PDF hist"),//use this for x values		
				minMaxDiffCos = distVisObj.getSpecificMinMaxDiff(cosName),
				minMaxDiffGauss = distVisObj.getSpecificMinMaxDiff(gaussName);
		double[][] minMaxDiff = new double[2][];
		minMaxDiff[0] = minMaxDiffHist[0];
		minMaxDiff[1] = new double[3];
		minMaxDiff[1][0] = (minMaxDiffCos[1][0] < minMaxDiffGauss[1][0]) ? minMaxDiffCos[1][0] : minMaxDiffGauss[1][0];
		minMaxDiff[1][1] = (minMaxDiffCos[1][1] > minMaxDiffGauss[1][1]) ? minMaxDiffCos[1][1] : minMaxDiffGauss[1][1];
		minMaxDiff[1][2] = minMaxDiff[1][1] - minMaxDiff[1][0];
		String[] dispMultiStrs = new String[] {func.name+" PDF hist", gaussName, cosName};
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
		
		myProbSummary summary = new myProbSummary(histVals);
		//build buckets : numBuckets+1 x 2 array; 2nd idxs : idx 0 is lower x value of bucket, y value is count; last entry should always have 0 count
		double[][] distBuckets = summary.calcBucketVals(numBuckets);
		
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
		
		distVisObj.setValuesHist(func.name+" PDF hist", new int[][] {new int[] {255,0,0,255}, new int[] {255,255,255,255}}, distBuckets, minMaxDiffXVals);
	}//calcDistValsForDisp
	
	//clear out any existing plot evaluations
	public void clearPlotEval() {	distVisObj.clearEvalVals();}
	//change width of visualization object
	public void dataVisSetDispWidth(float dispWidth) {	distVisObj.setDispWidth(dispWidth);}
	//update the visualization name
	public void updateVisName(String _newName) {distVisObj.updateName(_newName);}	
	
	//draw a represntation of this distribution
	public void drawDist(GraphProbExpMain pa) {
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
/**
 * this class holds a description of a random number generator, including the momments and algorithms it uses
 * @author john
 */
class RandGenDesc implements Comparable<RandGenDesc>{
	//owning rand gen
	public final myRandGen randGen;
	//names of quadrature algorithm and random number distribution name and algorithm name 
	public final String quadName, distName, algName;
	
	public RandGenDesc(String _quadName, String _distName, myRandGen _randGen) {
		quadName = _quadName; 
		randGen = _randGen;		
		distName = _distName;
		algName = randGen.name;
	}//ctor

	@Override
	public int compareTo(RandGenDesc othr) {
		int res = distName.toLowerCase().compareTo(othr.distName.toLowerCase());
		res = (res == 0 ? quadName.toLowerCase().compareTo(othr.quadName.toLowerCase()) : res);
		res = (res == 0 ? algName.toLowerCase().compareTo(othr.algName.toLowerCase()) : res);
		return (res == 0 ? Integer.compare(randGen.ObjID, othr.randGen.ObjID) : res);
	}//compareTo	
	
	@Override
	public String toString() {
		String res = "Alg Name : " + algName + " | Dist : " + distName + " | Quad Alg : " + quadName;
		return res;
	}
}//class RandGenDesc

////////////////////////////////////////////////////////////////////////////////////////
// child classes

/**
 * implementation of ziggurat algorithm to build a distribution from a uniform sampling
 * @author john
 *
 */
class myZigRandGen extends myRandGen{
	//based on "An Improved Ziggurat Method to Generate Normal Random Samples";J. A. Doornik, 2005
	//reference to ziggurat values used by this random variable generator
	private zigConstVals zigVals;
	//# of rects used for zig
	private final int numZigRects, numZigIDXMask;
	
	//pass RV generating function
	public myZigRandGen(myRandVarFunc _func, int _numZigRects, String _name) {
		super(_func, _name);
		numZigRects=_numZigRects;
		numZigIDXMask = numZigRects-1;
		func.setZigVals(numZigRects);			
		zigVals = func.zigVals;
	}//ctor
	
	public myZigRandGen(myRandVarFunc _func, String _name) {
		super(_func, _name);
		numZigRects=256;		//if not specified use 256
		numZigIDXMask = numZigRects-1;
		func.setZigVals(numZigRects);			
		zigVals = func.zigVals;
	}//ctor
	
	@Override
	public void _setFuncSummaryIndiv() {
		//these lines shouldn't be needed - changing summary will not change underlying functional nature of func or zigvals - 
		//these are based on function and numZigRects, and have no bearing on moments, min/max or other underlying data, which is only thing that summary obj will have changed
		//func.setZigVals(numZigRects);			
		//zigVals = func.zigVals;		
	}
	
	@Override
	public double[] getMultiSamples(int num) {
		double[] res = new double[num];
		if(summary.doClipAllSamples()) {
			int idx = 0;
			double smpl;
			while (idx <= num){
				smpl=func.processResValByMmnts(nextNormal53());
				if (summary.checkInBnds(smpl)){					res[idx++]=smpl;			}
			}//while			
		} else {
			for(int i=0;i<res.length;++i) {	res[i]=func.processResValByMmnts(nextNormal53());}
		}
		return res;
	}//getNumSamples

	@Override
	public double[] getMultiFastSamples(int num) {
		double[] res = new double[num];
		if(summary.doClipAllSamples()) {			
			int idx = 0;
			double smpl;
			while (idx <= num){
				smpl=func.processResValByMmnts(nextNormal32());
				if (summary.checkInBnds(smpl)){					res[idx++]=smpl;			}
			}//while			
		} else {
			for(int i=0;i<res.length;++i) {	res[i]=func.processResValByMmnts(nextNormal32());}
		}		
		return res;
	}//getNumFastSamples	
    
	@Override
	public double getSample() {
		double res;
		if(summary.doClipAllSamples()) {
			do {			res = func.processResValByMmnts(nextNormal53());} while (!summary.checkInBnds(res));
		} else {			res = func.processResValByMmnts(nextNormal53());}
		return res;
	}//getGaussian
	
	//int value
	@Override
	public double getSampleFast() {
		double res;
		if(summary.doClipAllSamples()) {
			do {			res = func.processResValByMmnts(nextNormal32());} while (!summary.checkInBnds(res));
		} else {			res = func.processResValByMmnts(nextNormal32());}
		return res;
//		double res = nextNormal32();		
//		return func.processResValByMmnts(res);
	}//getGaussian
		
	//find inverse CDF value for passed val - val must be between 0->1 - value @ which p(x<=value) == val
	//this is mapping from 0->1 to probability based on the random variable function definition
	@Override
	public double inverseCDF(double _val) {
		//probit value
		return func.CDF_inv(_val);
	}
	//find the cdf value of the passed val -> prob (x<= _val)
	@Override
	public double CDF(double _val) {
		return func.CDF(_val);		
	}//CDF
	
	public double getFuncValFromLong(long val) {
		//uLong is using 54 least Sig Bits (and 1st Most Sig Bits as sign bit). - >> preserves sign
		//this method is closest to using magnitude of val as CDF-ish mapping, although not consistent
		long uLong = (val>>10); 
		int index = (int)(val & numZigIDXMask);
//alternate method - does not preserve order of val
//		long uLong = ((long)(val<<10))>>10;
//		int index = (int)((val>>55) & numZigIDXMask);		
		if (Math.abs(uLong) < zigVals.eqAreaZigRatio_NNorm[index]) {   	return uLong * zigVals.eqAreaZigX_NNorm[index]; }       
		if(index == 0) { 										return bottomCase((val<<55) < 0);   }// Using 9th LSBit to decide +/-
		//if(index == 0) { 										return bottomCase(val < 0);   }//alt method : Using 1st MSSBit to decide +/-
		// uLong * zigVals.invLBitMask in [-1,1], using 54 L Sig Bits, i.e. with 2^-53 granularity.
		return rareCase(index, uLong * zigConstVals.invLBitMask);
	}//_getFuncValFromLong
	
	//takes sequential int value val, uses most sig bit as sign, next 8 sig bits as index, and entire value as rand val
	public double getFuncValFromInt(int val) {
		int index = ((val>>16) & numZigIDXMask);
		if (-Math.abs(val) >= zigVals.eqAreaZigRatio_NNormFast[index]) { 	return val * zigVals.eqAreaZigX_NNormFast[index]; }
		if(index == 0) { 											return bottomCase(val < 0);   }//use most sig bit for +/-
		// bits * zigVals.invBitMask in [-1,1], using 32 bits, i.e. with 2^-31 granularity.
		return rareCase(index, val * zigConstVals.invBitMask);
	}//_getFuncValFromLong
		
    /**
     * @return A normal gaussian number.
     */
	private int numNanRes = 0;
	private double nextNormal53() {
    	double x;
    	while (true) {
			x = getFuncValFromLong(getNextLong());
			if (x==x) {return x;    }		//Nan test -> NaN != NaN
			//else {System.out.println("Nan res : " + numNanRes++);}
    	}
    }//nextNormal

    /**
     * @return A normal gaussian number with 32 bit granularity
     */
    private double nextNormal32() {
    	double x;
    	while (true) {
    		x = getFuncValFromInt(getNextInt());
			if (x==x) {return x;    }		//Nan test -> NaN != NaN
			//else {System.out.println("Nan res : " + numNanRes++);}
    	}
    }//nextNormalFast

    /**
     * u and negSide are used exclusively, so it doesn't hurt randomness if same random bits were used to compute both.
     * @return Value to return, or nan if needing to retry
     */
    private double rareCase(int idx, double u) {
        double x = u * zigVals.eqAreaZigX[idx];
        //verify under curve
        if (zigVals.rareCaseEqAreaX[idx+1] + (zigVals.rareCaseEqAreaX[idx] - zigVals.rareCaseEqAreaX[idx+1]) * getNextDouble() < func.fStd(x)) {          return x;    }
        //overflowing when outside to cause re-samnple
        return Double.NaN;
    }
    
    private double bottomCase(boolean negSide) {        
        double x = -1.0, y = 0.0;
        while (-(y + y) < x * x) {
            x = Math.log(getNextDouble()+zigConstVals.invLBitMask) * zigVals.inv_R_last;//adding zigConstVals.invLBitMask to avoid log of 0 - getNextDouble might possibly return a 0
            y = Math.log(getNextDouble()+zigConstVals.invLBitMask);
        } 
        return negSide ? x - zigVals.R_last : zigVals.R_last - x;
    }

}//class myZigRandGen


/**
 * class that will model a distribution using first 4 moments via a polynomial transformation
 * @author john *
 */
//class myFleishUniVarRandGen_new extends myRandGen{
//	
//	//min and max of synthesized
//	public myFleishUniVarRandGen_new(myRandVarFunc _func, String _name) {
//		super(_func, _name);
//	}//ctor
//	
//	//if summary object is changed, new fleishman polynomial values need to be synthesized - this is done already in calling function 
//	//when func.rebuildFuncs is called	
//	@Override
//	public void _setFuncSummaryIndiv() {	}
//	//test function to test iterative method to derive fl inverse
//	public double calcInvFuncVal(double y) {	return ((myFleishFunc_Uni)func).calcInvF(y);}
//	
//	@Override
//	//synthesize numVals values from low to high to display 
//	//overridden to get apprpriate values - feed low->high into zi
//	public void calcFuncValsForDisp(int numVals,double low, double high,  int funcType ) {
//		if(numVals < 2) {		numVals = 2;		}//minimum 2 values
////		if (low == high) {//ignore if same value
////			System.out.println("myRandGen : "+name+" :: calcFValsForDisp : Low == High : " +low +" : "+ high +" : Ignored, no values set/changed.");
////			return;			
////		} 
////		else if(low > high) {	double s = low;		low = high;		high = s;		}  //swap if necessary
//		double[] minMaxVals = func.getPlotValBounds(funcType);
//
//		double[][] funcVals = new double[numVals][2];
//		double xdiff = minMaxVals[1]-minMaxVals[0];//high-low;
//		for(int i=0;i<funcVals.length;++i) {		
//			//funcVals[i][0] = low + (i * xdiff)/numVals;	
//			funcVals[i][0] = minMaxVals[0] + (i * xdiff)/numVals;	
//		}
//		
//		//evaluate specified function on funcVals
//		double minY = Double.MAX_VALUE, maxY = -minY, ydiff;
//		for(int i=0;i<funcVals.length-1;++i) {	
//			double lowVal = zigNormGen.func.f(funcVals[i][0]), highVal = zigNormGen.func.f(funcVals[i+1][0]);
//			funcVals[i][1] = func.getFuncVal(funcType,lowVal,highVal);
//			minY = (minY > funcVals[i][1] ? funcVals[i][1] : minY);
//			maxY = (maxY < funcVals[i][1] ? funcVals[i][1] : maxY);
//		}
//		//last argument is ignored except for integral calc 
//		int i=funcVals.length-1;
//		double lowVal = zigNormGen.func.f(funcVals[i][0]);
//		funcVals[i][1] = func.getFuncVal(funcType,lowVal,Double.POSITIVE_INFINITY);
//		if(Math.abs(funcVals[i][1]) <= 10000000* Math.abs(funcVals[i-1][1])) {//- don't count this last value for min/max in case of divergence 
//			minY = (minY > funcVals[i][1] ? funcVals[i][1] : minY);
//			maxY = (maxY < funcVals[i][1] ? funcVals[i][1] : maxY);
//		}
//		minY = (minY > 0 ? 0 : minY);
//		ydiff = maxY - minY;
//		//distVisObj.setValuesFunc(funcVals, new double[][]{{minMaxVals[0], minMaxVals[1], xdiff}, {minY, maxY, ydiff}});
//		double minVal = (minMaxVals[0] < low ? minMaxVals[0] : low),
//				maxVal = (minMaxVals[1] > high ? minMaxVals[1] : high);
//		
//		double[][] minMaxDiffFuncVals = new double[][]{{minVal, maxVal, (maxVal-minVal)}, {minY, maxY, ydiff}};
//		
//		
//		distVisObj.setValuesFunc(funcVals, minMaxDiffFuncVals);
//	}//calcFValsForDisp
//	
//	@Override
//	public double[] getMultiSamples(int num) {
//		double[] res = new double[num];
//		double val;
//		boolean clipRes = summary.doClipAllSamples();
//		//transformation via mean and std already performed as part of f function
//		if (clipRes){
//			int idx = 0;
//			while (idx < num) {		val = func.f(zigNormGen.getSample());		if(summary.checkInBnds(val)) {				res[idx++]=val;	}}
//		} else {					for(int i =0;i<res.length;++i) {		res[i]=func.f(zigNormGen.getSample());			}		}
//		return res;
//	}//getMultiSamples
//
//	@Override
//	public double[] getMultiFastSamples(int num) {
//		double[] res = new double[num];
//		boolean clipRes = summary.doClipAllSamples();
//		//transformation via mean and std already performed as part of f function
//		if (clipRes){
//			int idx = 0;
//			while (idx < num) {			double val = func.f(zigNormGen.getSampleFast());	if(summary.checkInBnds(val)) {				res[idx++]=val;	}}
//		} else {						for(int i =0;i<res.length;++i) {	res[i]=func.f(zigNormGen.getSampleFast());			}		}
//		return res;
//	}//getMultiFastSamples
//	
//	@Override
//	public double getSample() {
//		double res;
//		if(summary.doClipAllSamples()) {
//			do {				res = func.f(zigNormGen.getSample());			} while (!summary.checkInBnds(res));
//		} else {				res = func.f(zigNormGen.getSample());			}
//		return res;
//
//	}//getSample
//
//	@Override
//	public double getSampleFast() {
//		double res;
//		if(func.summary.doClipAllSamples()) {
//			do {				res = func.f(zigNormGen.getSampleFast());		} while (!summary.checkInBnds(res));
//		} else {				res = func.f(zigNormGen.getSampleFast());		}
//		return res;
//		
////		double res = func.f(zigNormGen.getSampleFast());
////		return res;
//	}//getSampleFast
//	
//	//find inverse CDF value for passed val - val must be between 0->1; value for which prob(x<=value) is _pval
//	//this is mapping from 0->1 to probability based on the random variable function definition
//	@Override
//	public double inverseCDF(double _pval) {
//		return func.CDF_inv(_pval);
//	}
//	//find the cdf value of the passed val == returns prob (x<= _val)
//	//for fleishman polynomial, these use the opposite mapping from the normal distrubtion - 
//	//so for CDF, we want normal dist's inverse cdf of passed value passed to fleish CDF calc
//	@Override
//	public double CDF(double _val) {
//		return func.CDF(_val);	
//	}//CDF
//	
//}//class myFleishRandGen

class myFleishUniVarRandGen extends myRandGen{
	
	//generator to manage synthesizing normals to feed fleishman
	private myZigRandGen zigNormGen;
	
	
	//min and max of synthesized
	public myFleishUniVarRandGen(myRandVarFunc _func, String _name) {
		super(_func, _name);
		//need to build a source of normal random vars
		zigNormGen = new myZigRandGen(new myNormalFunc(func.expMgr, func.quadSlvr), 256, "Ziggurat Algorithm");		
	}//ctor
	
	//if summary object is changed, new fleishman polynomial values need to be synthesized - this is done already in calling function 
	//when func.rebuildFuncs is called	
	@Override
	public void _setFuncSummaryIndiv() {	}
	//test function to test iterative method to derive fl inverse
	public double calcInvFuncVal(double y) {
		return ((myFleishFunc_Uni)func).calcInvF(y);
	}
	
	
	@Override
	public double[] getMultiSamples(int num) {
		double[] res = new double[num];
		double val;
		boolean clipRes = summary.doClipAllSamples();
		//transformation via mean and std already performed as part of f function
		if (clipRes){
			int idx = 0;
			while (idx < num) {		val = func.f(zigNormGen.getSample());		if(summary.checkInBnds(val)) {				res[idx++]=val;	}}
		} else {					for(int i =0;i<res.length;++i) {		res[i]=func.f(zigNormGen.getSample());			}		}
		return res;
	}//getMultiSamples

	@Override
	public double[] getMultiFastSamples(int num) {
		double[] res = new double[num];
		boolean clipRes = summary.doClipAllSamples();
		//transformation via mean and std already performed as part of f function
		if (clipRes){
			int idx = 0;
			while (idx < num) {			double val = func.f(zigNormGen.getSampleFast());	if(summary.checkInBnds(val)) {				res[idx++]=val;	}}
		} else {						for(int i =0;i<res.length;++i) {	res[i]=func.f(zigNormGen.getSampleFast());			}		}
		return res;
	}//getMultiFastSamples
	
	@Override
	public double getSample() {
		double res;
		if(summary.doClipAllSamples()) {
			do {				res = func.f(zigNormGen.getSample());			} while (!summary.checkInBnds(res));
		} else {				res = func.f(zigNormGen.getSample());			}
		return res;

	}//getSample

	@Override
	public double getSampleFast() {
		double res;
		if(func.summary.doClipAllSamples()) {
			do {				res = func.f(zigNormGen.getSampleFast());		} while (!summary.checkInBnds(res));
		} else {				res = func.f(zigNormGen.getSampleFast());		}
		return res;
		
//		double res = func.f(zigNormGen.getSampleFast());
//		return res;
	}//getSampleFast
	
	//find inverse CDF value for passed val - val must be between 0->1; value for which prob(x<=value) is _pval
	//this is mapping from 0->1 to probability based on the random variable function definition
	@Override
	public double inverseCDF(double _pval) {
		return func.CDF_inv(_pval);
	}
	//find the cdf value of the passed val == returns prob (x<= _val)
	//for fleishman polynomial, these use the opposite mapping from the normal distrubtion - 
	//so for CDF, we want normal dist's inverse cdf of passed value passed to fleish CDF calc
	@Override
	public double CDF(double _val) {
		return func.CDF(_val);		
	}//CDF
	
}//class myFleishRandGen_old (used external zigNormGen



/**
 * rand gen class for bounded pdfs (like cosine) - perhaps use variant of zigguarat instead of iterative convergence method to find inv-CDF?
 */

class myBoundedRandGen extends myRandGen{

	public myBoundedRandGen(myRandVarFunc _func, String _name) {
		super(_func, _name);
	}

	@Override
	public void _setFuncSummaryIndiv() {//no extra settings required		
	}

	@Override
	public double[] getMultiSamples(int num) {
		double[] res = new double[num];
		if(summary.doClipAllSamples()) {
			int idx = 0;
			double smpl;
			while (idx <= num){
				smpl=nextRandCosVal();
				if (summary.checkInBnds(smpl)){					res[idx++]=smpl;			}
			}//while			
		} else {
			for(int i=0;i<res.length;++i) {	res[i]=nextRandCosVal();}
		}
		return res;
	}//getNumSamples

	@Override
	public double[] getMultiFastSamples(int num) {return getMultiSamples(num);}//getNumFastSamples
	
    
	@Override
	public double getSample() {
		double res;
		if(summary.doClipAllSamples()) {
			do {		res = nextRandCosVal();	} while (!summary.checkInBnds(res));
		} else {
			res = nextRandCosVal();
		}
		return res;
	}//getGaussian
	
	//get a random value based on cosine pdf
	private double nextRandCosVal() {
		double res = func.CDF_inv(getNextDouble());
		return res;		
	}
	
	//int value
	@Override
	public double getSampleFast() {

		return getSample();
//		double res = nextNormal32();		
//		return func.processResValByMmnts(res);
	}//getGaussian

	@Override
	public double inverseCDF(double _val) {
		//probit value
		return func.CDF_inv(_val);
	}
	//find the cdf value of the passed val -> prob (x<= _val)
	@Override
	public double CDF(double _val) {
		return func.CDF(_val);		
	}//CDF
	
}//class myCosVarRandGen

//////////////////////////////////
// linear and uniform transformation classes
//	these are just mappers and will not be used to synthesize random values

abstract class transform extends myRandGen{
	//func will be null for these, so all functionality that is dependent on func variable needs to be overridden
	public transform(String _name, myProbSummary _summary) {
		super(null, _name);
		setFuncSummary(_summary);
	}
	//overrding base class verison to remove refs to func
	@Override
	public void initRandGen() {
		setFlag(funcSetIDX, func!=null);
		desc = new RandGenDesc("No Quad Solver", "No Rand Func", this);
		distVisObj = null;
	}//initRandGen
	
	//override base class version to remove ref to func, which will be null
	@Override
	public void setFuncSummary(myProbSummary _summary) {
		summary = _summary;	
		 _setFuncSummaryIndiv();
	}//setFuncSummary
	
	//for a transform this is meaningless - transforms just remap given data to affine transformations, they don't model them
	public void calcDistValsForDisp(int numVals, int numBuckets) {}
	public void calcFuncValsForDisp(int numVals, double low, double high, int funcType ) {}
	
	//return string description of rand function
	@Override
	public String getFuncDataStr() {return "No Function for Transform RandGen - only has mapping";}

	//transform "randGen" objects are actually intended only as a mappers,so never going to ever generate any values
	@Override
	public double[] getMultiSamples(int num) {	return new double[0];}
	@Override
	public double[] getMultiFastSamples(int num) {return new double[0];}
	@Override
	public double getSample() {return 0;}
	@Override
	public double getSampleFast() {return 0;}
	@Override
	public String getTransformName() {		return name+"_"+ _getTransformNameIndiv();	}
	
	public abstract String _getTransformNameIndiv();


}//class transform

//min/max to 0->1 -> using this method to facilitate implementing structure for trivial examples - 
//will never generate values, nor will it ever access a random variable function.
//only maps values via affine transformation to 0->1
class linearTransform extends transform{
	//func must not be null, but doesn't matter for this transforming rand gen
	double min, max, diff;
	//summary must have min and max
	public linearTransform( myProbSummary _summary) {
		super( "Linear Transform Mapping", _summary);		
	}//ctor
	//called whenever summary object is set/reset
	@Override
	public void _setFuncSummaryIndiv() {	
		min = summary.getMin();
		max = summary.getMax();
		diff = max - min;
		if(diff == 0) {//should never happen - give error if it does
			System.out.println("The linear transform " + name + " must have min != max.  Min and max being set to 0 and 1");
			min = 0;
			max = 1.0;
		}
	}
	//really just provides mapping from 0->1 to original span 
	@Override
	public double inverseCDF(double _val) {return (diff*_val)+min;}

	@Override
	public double CDF(double _val) {		return (_val - min)/diff;}

	@Override
	public String _getTransformNameIndiv() {		return "|Linear Transform | Min : "+ String.format("%3.8f", min) + " | Max : "+ String.format("%3.8f", max);	}
	
}//class linearTransform

//maps each grade to a specific location based on its order - does not care about original grade value, just uses rank
class uniformCountTransform extends transform{
	//# of -unique- grades
	int count;
	//grades sorted in ascending order
	TreeMap<Double, Integer> sortedGrades;
	//ranks and actual grade values
	TreeMap<Integer, Double> rankedGrades;
	
	//summary must be built by data and have data vals
	public uniformCountTransform(myProbSummary _summary) {
		super("Uniform Count Transform Mapping", _summary);
	}

	//This object MUST have vals, so that grades can be sorted;
	//for final grade roster, this object must have updated summary object
	@Override
	public void _setFuncSummaryIndiv() {
		//when summary is set, need to add all grades in ascending order to sortedGrades
		sortedGrades = new TreeMap<Double, Integer>();
		rankedGrades = new TreeMap<Integer, Double>();
		double [] vals = summary.getDataVals();
		//place in grade map
		for (double val : vals) {			sortedGrades.put(val, 0);		}
		//find count
		count = sortedGrades.size();
		//System.out.println("uniformCountTransform : This object has :"+ count+" elements");
		//place count in sorted map - treats grades of same value as same grade
		int idx =0;
		for(double val : sortedGrades.keySet()) {		rankedGrades.put(idx, val);	sortedGrades.put(val, idx++);	}//start with 1
	}
	//provides mapping from rank/n to original grade
	@Override
	public double inverseCDF(double _val) {	
		int desKey = (int)(((_val) * count) - 1.0/count);
//		System.out.println("Wanting inv cdf of _val == " + _val + " des key : " + desKey+ " currently contains : ");
//		for(int key : rankedGrades.keySet()) {
//			System.out.println("Key : " + key + " | Val :  "+ rankedGrades.get(key));
//		}
		//update every time?
		_setFuncSummaryIndiv();
		return rankedGrades.get(desKey);	}
	//provides mapping from original grade to rank/n (0->1)
	@Override
	public double CDF(double _val) {		
		//update every time?
		_setFuncSummaryIndiv();
		
		return (1.0 * sortedGrades.get(_val)+1)/count;	}
	
	@Override
	public String _getTransformNameIndiv() {return "Uniformly Ranked | # of unique grades : " + count;	}	
	
}//class uniformCountTransform