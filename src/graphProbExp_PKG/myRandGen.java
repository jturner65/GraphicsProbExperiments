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
	protected myDistVis distVisObj; 
		
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
	
	//called whenever summary object is set/reset
	public abstract void _setFuncSummaryIndiv();	
	
	public void buildDistVisObj(float[] _startRect) {		distVisObj = new myDistVis(_startRect, this);	}
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
	
	//synthesize numVals values from low to high to display 
	public void calcFuncValsForDisp(int numVals,double low, double high,  int funcType ) {
		if(numVals < 2) {		numVals = 2;		}//minimum 2 values
//		if (low == high) {//ignore if same value
//			System.out.println("myRandGen : "+name+" :: calcFValsForDisp : Low == High : " +low +" : "+ high +" : Ignored, no values set/changed.");
//			return;			
//		} 
//		else if(low > high) {	double s = low;		low = high;		high = s;		}  //swap if necessary
		//get min/max values based on mean +/- 3.5 stds
		double[] minMaxVals = func.getPlotValBounds();

		double[][] funcVals = new double[numVals][2];
		double xdiff = minMaxVals[1]-minMaxVals[0];//high-low;
		for(int i=0;i<funcVals.length;++i) {		
			//funcVals[i][0] = low + (i * xdiff)/numVals;	
			funcVals[i][0] = minMaxVals[0] + (i * xdiff)/numVals;	
		}
		//evaluate specified function on funcVals
		double minY = Double.MAX_VALUE, maxY = -minY, ydiff;
		for(int i=0;i<funcVals.length-1;++i) {		
			funcVals[i][1] = func.getFuncVal(funcType,funcVals[i][0],funcVals[i+1][0]);
			minY = (minY > funcVals[i][1] ? funcVals[i][1] : minY);
			maxY = (maxY < funcVals[i][1] ? funcVals[i][1] : maxY);
		}
		//last argument is ignored except for integral calc 
		int i=funcVals.length-1;
		funcVals[i][1] = func.getFuncVal(funcType,funcVals[i][0],Double.POSITIVE_INFINITY);
		if(Math.abs(funcVals[i][1]) <= 10000000* Math.abs(funcVals[i-1][1])) {//- don't count this last value for min/max in case of divergence 
			minY = (minY > funcVals[i][1] ? funcVals[i][1] : minY);
			maxY = (maxY < funcVals[i][1] ? funcVals[i][1] : maxY);
		}
		ydiff = maxY - minY;
		//distVisObj.setValuesFunc(funcVals, new double[][]{{low, high, xdiff}, {minY, maxY, ydiff}});
		double minVal = (minMaxVals[0] < low ? minMaxVals[0] : low),
				maxVal = (minMaxVals[1] > high ? minMaxVals[1] : high);
		distVisObj.setValuesFunc(funcVals, new double[][]{{minVal, maxVal, (maxVal-minVal)}, {minY, maxY, ydiff}});
	}//calcFValsForDisp
	
	
	//build display function for distribution
	//Num buckets should be << numVals
	public void calcDistValsForDisp(int numVals, int numBuckets) {
		//x val is distribution/max bucket value, y val is count
		double [] distVals = getMultiSamples(numVals);		
		
		myProbSummary summary = new myProbSummary(distVals);
		//build buckets : numBuckets+1 x 2 array; 2nd idxs : idx 0 is lower x value of bucket, y value is count; last entry should always have 0 count
		double[][] distBuckets = summary.calcBucketVals(numBuckets);
		
		//min, max and diff values for x axis (rand val) and y axis (counts)
		double[][] minMaxDiffXVals = new double[2][3];
		minMaxDiffXVals[0][0] = summary.getMin();
		minMaxDiffXVals[0][1] = summary.getMax();
		minMaxDiffXVals[0][2] = minMaxDiffXVals[0][1] - minMaxDiffXVals[0][0];
		//bucket min max diff
		minMaxDiffXVals[1][0] = 100000;
		minMaxDiffXVals[1][1] = -100000;
		for(int i=0;i<distBuckets.length;++i) {
			minMaxDiffXVals[1][0] = (minMaxDiffXVals[1][0] > distBuckets[i][1] ? distBuckets[i][1] : minMaxDiffXVals[1][0]);
			minMaxDiffXVals[1][1] = (minMaxDiffXVals[1][1] < distBuckets[i][1] ? distBuckets[i][1] : minMaxDiffXVals[1][1]);
		}		
		minMaxDiffXVals[1][2] = minMaxDiffXVals[1][1] - minMaxDiffXVals[1][0];
		
		distVisObj.setValuesHist(distBuckets, minMaxDiffXVals);
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
	private final int numZigRects;
	
	//pass RV generating function
	public myZigRandGen(myRandVarFunc _func, int _numZigRects, String _name) {
		super(_func, _name);
		numZigRects=_numZigRects;
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
			do {		
				res = func.processResValByMmnts(nextNormal53());
			} while (!summary.checkInBnds(res));
		} else {
			res = func.processResValByMmnts(nextNormal53());
		}
		return res;
	}//getGaussian
	
	//int value
	@Override
	public double getSampleFast() {
		double res;
		if(summary.doClipAllSamples()) {
			do {		
				res = func.processResValByMmnts(nextNormal32());
			} while (!summary.checkInBnds(res));
		} else {
			res = func.processResValByMmnts(nextNormal32());
		}
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
	
	//takes sequential long value "bits", uses most sig bit as sign, next 8 sig bits as index, and last 54 bits as actual random value
	public double _getFuncValFromLong(long val) {
		//uLong is using 54 least Sig Bits (and 1st Most Sig Bits as sign bit).
		long uLong = ((long)(val<<10))>>10;
		//Using 9 least sig Bits as index and sign.
		int index = ((int)val>>55) & 0xFF;
		if (Math.abs(uLong) < zigVals.eqAreaZigRatio_NNorm[index]) {   	return uLong * zigVals.eqAreaZigX_NNorm[index]; }       
		//if(index == 0) { 										return bottomCase((bits<<55) < 0);   }// Using 9th LSBit to decide +/-
		if(index == 0) { 										return bottomCase(val < 0);   }// Using 1st MSSBit to decide +/-
		// uLong * zigVals.invLBitMask in [-1,1], using 54 L Sig Bits, i.e. with 2^-53 granularity.
		return rareCase(index, uLong * zigVals.invLBitMask);
	}//_getFuncValFromLong
	
	//takes sequential int value val, uses most sig bit as sign, next 8 sig bits as index, and entire value as rand val
	public double _getFuncValFromInt(int val) {
		int bits = val;
		int index = ((val>>16) & 0xFF);
		if (-Math.abs(bits) >= zigVals.eqAreaZigRatio_NNormFast[index]) { 	return bits * zigVals.eqAreaZigX_NNormFast[index]; }
		if(index == 0) { 											return bottomCase(bits < 0);   }//use most sig bit for +/-
		// bits * zigVals.invBitMask in [-1,1], using 32 bits, i.e. with 2^-31 granularity.
		return rareCase(index, bits * zigVals.invBitMask);
	}//_getFuncValFromLong
	
	
    /**
     * @return A normal gaussian number.
     */
	private double nextNormal53() {
    	long longVal;
    	double x;
    	while (true) {
			//single random 64 bit value
    		longVal = getNextLong();
			x = _getFuncValFromLong(longVal);
			if (x==x) {return x;    }		//Nan test -> NaN != NaN
    	}
    }//nextNormal

    /**
     * @return A normal gaussian number with 32 bit granularity
     */
	static int minVal = 1000000,maxVal = -10000000;
    private double nextNormal32() {
    	int val;
    	double x;
    	while (true) {
    		val = getNextInt();
    		x = _getFuncValFromInt(val);
			if (x==x) {return x;    }		//Nan test -> NaN != NaN
    	}
    }//nextNormalFast

    /**
     * u and negSide are used exclusively, so it doesn't hurt
     * randomness if same random bits were used to compute both.
     * 
     * @return Value to return, or nan if needing to retry
     */
    private double rareCase(int idx, double u) {
        double x = u * zigVals.eqAreaZigX[idx];
        //verify under curve
        if (zigVals.rareCaseEqAreaX[idx+1] + (zigVals.rareCaseEqAreaX[idx] - zigVals.rareCaseEqAreaX[idx+1]) * getNextDouble() < func.fStd(x)) {          return x;    }
        return Double.NaN;
    }
    
    private double bottomCase(boolean negSide) {        
        double x = -1.0, y = 0.0;
        while (-(y + y) < x * x) {
            x = Math.log(getNextDouble()+zigVals.invLBitMask) * zigVals.inv_R_last;
            y = Math.log(getNextDouble()+zigVals.invLBitMask);
        } 
        return negSide ? x - zigVals.R_last : zigVals.R_last - x;
    }

}//class myZigRandGen


/**
 * rand gen class for cosine function - cannot use ziggurat alg because algorithm is predicated on having unbounded x - instead use inverse function
 */

class myCosVarRandGen extends myRandGen{

	public myCosVarRandGen(myRandVarFunc _func, String _name) {
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
				smpl=func.processResValByMmnts(nextRandCosVal());
				if (summary.checkInBnds(smpl)){					res[idx++]=smpl;			}
			}//while			
		} else {
			for(int i=0;i<res.length;++i) {	res[i]=func.processResValByMmnts(nextRandCosVal());}
		}
		return res;
	}//getNumSamples

	@Override
	public double[] getMultiFastSamples(int num) {return getMultiSamples(num);}//getNumFastSamples
	
    
	@Override
	public double getSample() {
		double res;
		if(summary.doClipAllSamples()) {
			do {		
				res = func.processResValByMmnts(nextRandCosVal());
			} while (!summary.checkInBnds(res));
		} else {
			res = func.processResValByMmnts(nextRandCosVal());
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

/**
 * class that will model a distribution using first 4 moments via a polynomial transformation
 * @author john *
 */
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
	public void calcInvFuncVal(double y) {
		double x = ((myFleishFunc_Uni)func).calcInvF(y);
	}
	
	@Override
	//synthesize numVals values from low to high to display 
	//overridden to get apprpriate values - feed low->high into zi
	public void calcFuncValsForDisp(int numVals,double low, double high,  int funcType ) {
		if(numVals < 2) {		numVals = 2;		}//minimum 2 values
//		if (low == high) {//ignore if same value
//			System.out.println("myRandGen : "+name+" :: calcFValsForDisp : Low == High : " +low +" : "+ high +" : Ignored, no values set/changed.");
//			return;			
//		} 
//		else if(low > high) {	double s = low;		low = high;		high = s;		}  //swap if necessary
		double[] minMaxVals = func.getPlotValBounds();

		double[][] funcVals = new double[numVals][2];
		double xdiff = minMaxVals[1]-minMaxVals[0];//high-low;
		for(int i=0;i<funcVals.length;++i) {		
			//funcVals[i][0] = low + (i * xdiff)/numVals;	
			funcVals[i][0] = minMaxVals[0] + (i * xdiff)/numVals;	
		}
		
		//evaluate specified function on funcVals
		double minY = Double.MAX_VALUE, maxY = -minY, ydiff;
		for(int i=0;i<funcVals.length-1;++i) {	
			double lowVal = zigNormGen.func.f(funcVals[i][0]), highVal = zigNormGen.func.f(funcVals[i+1][0]);
			funcVals[i][1] = func.getFuncVal(funcType,lowVal,highVal);
			minY = (minY > funcVals[i][1] ? funcVals[i][1] : minY);
			maxY = (maxY < funcVals[i][1] ? funcVals[i][1] : maxY);
		}
		//last argument is ignored except for integral calc 
		int i=funcVals.length-1;
		double lowVal = zigNormGen.func.f(funcVals[i][0]);
		funcVals[i][1] = func.getFuncVal(funcType,lowVal,Double.POSITIVE_INFINITY);
		if(Math.abs(funcVals[i][1]) <= 10000000* Math.abs(funcVals[i-1][1])) {//- don't count this last value for min/max in case of divergence 
			minY = (minY > funcVals[i][1] ? funcVals[i][1] : minY);
			maxY = (maxY < funcVals[i][1] ? funcVals[i][1] : maxY);
		}
		ydiff = maxY - minY;
		//distVisObj.setValuesFunc(funcVals, new double[][]{{minMaxVals[0], minMaxVals[1], xdiff}, {minY, maxY, ydiff}});
		double minVal = (minMaxVals[0] < low ? minMaxVals[0] : low),
				maxVal = (minMaxVals[1] > high ? minMaxVals[1] : high);
		distVisObj.setValuesFunc(funcVals, new double[][]{{minVal, maxVal, (maxVal-minVal)}, {minY, maxY, ydiff}});

	}//calcFValsForDisp
	
	
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
		return func.f(zigNormGen.inverseCDF(_pval));
	}
	//find the cdf value of the passed val == returns prob (x<= _val)
	//for fleishman polynomial, these use the opposite mapping from the normal distrubtion - 
	//so for CDF, we want normal dist's inverse cdf of passed value passed to fleish CDF calc
	@Override
	public double CDF(double _val) {
		return func.f(zigNormGen.CDF(_val));		
	}//CDF
	
}//class myFleishRandGen

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