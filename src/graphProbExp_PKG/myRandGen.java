package graphProbExp_PKG;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Provides random generation of prob distributions given a uniform distribution
 */
public abstract class myRandGen implements Comparable<myRandGen> {
	public final int ObjID;
	private static int IDCnt = 0;
	//original data and analysis of it - fl polynomial needs to be built from a sample distribution or from a set of moments
	protected myProbSummary summary;

    //random generator to use to generate uniform data - threadsafe
	public final String name;
	
	//descriptor of this random generator
	public final RandGenDesc desc;	
	
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
		setFlag(funcSetIDX, true);
		desc = new RandGenDesc(func.getQuadSolverName(), func.name, this);
		//func built with summary data - allow for quick access
		summary = func.summary;	
		distVisObj = null;
    }//ctor
	
	public void setFuncSummary(myProbSummary _summary) {
		summary = _summary;	
		func.setSummary(_summary);
		 _setFuncSummaryIndiv();
	}//setFuncSummary
	
	public abstract void _setFuncSummaryIndiv();	
	
	public void setDistVisObj(myDistVis _distVisObj) {	distVisObj = _distVisObj;	}
	
	public void resetSummary(myProbSummary _summary) {
		summary = _summary;
		func.rebuildFunc(_summary);
	}//reSetSummary
	
    //thread-safe queries for uniform values
    protected long getNextLong() {return ThreadLocalRandom.current().nextLong();}  
    protected int getNextInt() {return ThreadLocalRandom.current().nextInt();  }
    protected double getNextDouble() {return ThreadLocalRandom.current().nextDouble();}
	
    public abstract double[] getMultiSamples(int num);
    public abstract double[] getMultiFastSamples(int num);
    //return a sample based on func  - momments defined by myRandVarFunc
	public abstract double getSample();
	public abstract double getSampleFast();
		
	//return string description of rand function
	public String getFuncDataStr() {return func.getFuncDataStr();}
	
	//get short string suitable for key for map
	public String getTransformName() {
		String res = name+"_"+ desc.quadName+"_" + func.getMinDescString();
		return res;
	}
	public abstract double inverseCDF(double _val);
	public abstract double CDF(double _val);
	
	@Override
	public int compareTo(myRandGen othr) {return desc.compareTo(othr.desc);}

	//synthesize numVals values from low to high to display 
	public void calcFValsForDisp(int numVals, double low, double high) {
		
	}//calcFValsForDisp
		
	public void drawDist(GraphProbExpMain pa) {
		if(distVisObj == null) {return;}
		distVisObj.drawVis(pa);
	}
	
	
	
	
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
	}
	
	
	@Override
	public String toString() {
		String res = "Rand Gen Alg Name : " + algName + " | Distribution : " + distName + " | Quadrature Alg Used : " + quadName;
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
		func.setZigVals(numZigRects);			
		zigVals = func.zigVals;		
	}
	
	@Override
	public double[] getMultiSamples(int num) {
		double[] res = new double[num];
		for(int i=0;i<res.length;++i) {	res[i]=getSample();}
		return res;
	}//getNumSamples

	@Override
	public double[] getMultiFastSamples(int num) {
		double[] res = new double[num];
		for(int i=0;i<res.length;++i) {	res[i]=getSampleFast();}
		return res;
	}//getNumFastSamples
	
    
	@Override
	public double getSample() {
		double res;
		if(func.summary.doClipAllSamples()) {
			do {		
				res = func.processResValByMmnts(nextNormal53());
			} while (!func.summary.checkInBnds(res));
		} else {
			res = func.processResValByMmnts(nextNormal53());
		}
		return res;
	}//getGaussian
	
	//int value
	@Override
	public double getSampleFast() {
		double res;
		if(func.summary.doClipAllSamples()) {
			do {		
				res = func.processResValByMmnts(nextNormal32());
			} while (!func.summary.checkInBnds(res));
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
	private double _getFuncValFromLong(long val) {
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
	private double _getFuncValFromInt(int val) {
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
        if (zigVals.rareCaseEqAreaX[idx+1] + (zigVals.rareCaseEqAreaX[idx] - zigVals.rareCaseEqAreaX[idx+1]) * getNextDouble() < func.fZig(x)) {          return x;    }
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
 * class that will model a distribution using first 4 moments via a polynomial transformation
 * @author john *
 */

class myFleishUniRandGen extends myRandGen{
	
	//generator to manage synthesizing normals to feed fleishman
	private myZigRandGen zigNormGen;
	
	
	//min and max of synthesized
	public myFleishUniRandGen(myRandVarFunc _func, String _name) {
		super(_func, _name);
		//need to build a source of normal random vars
		zigNormGen = new myZigRandGen(new myNormalFunc(func.expMgr, func.quadSlvr), 256, "Ziggurat Algorithm");		
	}//ctor

	@Override
	public void _setFuncSummaryIndiv() {	}

	@Override
	public double[] getMultiSamples(int num) {
		double[] res = new double[num];
		double val;
		boolean clipRes = func.summary.doClipAllSamples();
		//transformation via mean and std already performed as part of f function
		if (clipRes){
			int idx = 0;
			while (idx < num) {
				val = func.f(zigNormGen.getSample());	
				if(func.summary.checkInBnds(val)) {				res[idx++]=val;			}
			}
		} else {
			for(int i =0;i<res.length;++i) {		res[i]=func.f(zigNormGen.getSample());			}
		}
		return res;
	}//getMultiSamples

	@Override
	public double[] getMultiFastSamples(int num) {
		double[] res = new double[num];
		boolean clipRes = func.summary.doClipAllSamples();
		//transformation via mean and std already performed as part of f function
		if (clipRes){
			int idx = 0;
			while (idx < num) {
				double val = func.f(zigNormGen.getSampleFast());	
				if(func.summary.checkInBnds(val)) {				res[idx++]=val;			}
			}
		} else {
			for(int i =0;i<res.length;++i) {	res[i]=func.f(zigNormGen.getSampleFast());			}
		}
		return res;
	}//getMultiFastSamples
	
	@Override
	public double getSample() {
		double res;
		if(func.summary.doClipAllSamples()) {
			do {		
				res = func.f(zigNormGen.getSample());
			} while (!func.summary.checkInBnds(res));
		} else {
			res = func.f(zigNormGen.getSample());
		}
		return res;
//		double res;
//		do {
//			res = func.f(zigNormGen.getSample());		//needs to be fed from a normal distribution
//		} while (!func.summary.checkInBnds(res));
//		return res;
	}//getSample

	@Override
	public double getSampleFast() {
		double res;
		if(func.summary.doClipAllSamples()) {
			do {		
				res = func.f(zigNormGen.getSampleFast());
			} while (!func.summary.checkInBnds(res));
		} else {
			res = func.f(zigNormGen.getSampleFast());
		}
		return res;
		
//		double res = func.f(zigNormGen.getSampleFast());
//		return res;
	}//getSampleFast
	
	//will test that the volume under the function curve for this fleishman polynomial is 1
	public double testInteg(double min, double max) {
		return func.integral_f(min, max);
	}
	
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
