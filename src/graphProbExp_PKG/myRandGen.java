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
    }//ctor
	
	public void reSetSummary(myProbSummary _summary) {
		summary = _summary;
		func.setSummary(_summary);
	}//reSetSummary
	
    //thread-safe queries for uniform values
    protected long getNextLong() {return ThreadLocalRandom.current().nextLong();}  
    protected int getNextInt() {return ThreadLocalRandom.current().nextInt();  }
    protected double getNextDouble() {return ThreadLocalRandom.current().nextDouble();}
	
    //return a sample based on func  - momments defined by myRandVarFunc
	public abstract double getSample();
	public abstract double getSampleFast();
	
	//find inverse CDF value for passed val - val must be between 0->1; 
	//this is mapping from 0->1 to probability based on the random variable function definition
	public double inverseCDF(double _val) {
		return func.CDF_inv(_val);
	};
	
	//return string description of rand function
	public String getFuncDataStr() {return func.getFuncDataStr();}
	
	//get short string suitable for key for map
	public String getTransformName() {
		String res = name+"_"+ desc.quadName+"_" + func.getMinDescString();
		return res;
	}
	
	
	@Override
	public int compareTo(myRandGen othr) {return desc.compareTo(othr.desc);}

	
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
	
	//pass RV generating function
	public myZigRandGen(myRandVarFunc _func, int _numZigRects, String _name) {
		super(_func, _name);
		func.setZigVals(_numZigRects);			
		zigVals = func.zigVals;
	}//ctor
	    
	@Override
	public double getSample() {
		double res = nextNormal53();	
		return func.processResValByMmnts(res);
	}//getGaussian
	
	//int value
	@Override
	public double getSampleFast() {
		double res = nextNormal32();		
		return func.processResValByMmnts(res);
	}//getGaussian
	
    /**
     * @return A normal gaussian number.
     */
    private double nextNormal53() {
    	long bits, uLong;
    	int index;
    	while (true) {
	    	//single random 64 bit value
	        bits = getNextLong();
	        //uLong is using 54 Most Sig Bits (and 1st Most Sig Bits as sign bit).
	        uLong = (bits>>10);
	        //Using 8 Least sig Bits as index.
	        index = ((int)bits) & 0xFF;        
	        if (Math.abs(uLong) < zigVals.eqAreaZigRatio_NNorm[index]) {   	return uLong * zigVals.eqAreaZigX_NNorm[index]; }       
	        if(index == 0) { 										return bottomCase((bits<<55) < 0);   }// Using 9th LSBit to decide +/-
	        // u in [-1,1], using 54 Most Sig Bits, i.e. with 2^-53 granularity.
	        double u = uLong * zigVals.invLBitMask;
	        // Using 9th LSBit to decide which side to go (has not been used yet).
	        double x = rareCase(index, u);//, (bits<<55) < 0);
	        if (x==x) {return x;    }		//Nan test -> NaN != NaN
    	}
    }//nextNormal

    /**
     * @return A normal gaussian number with 32 bit granularity
     */
    private double nextNormal32() {
    	int bits, index;
    	while (true) {
	        bits = getNextInt();
	        index = (bits & 0xFF);
	        if (-Math.abs(bits) >= zigVals.eqAreaZigRatio_NNormFast[index]) { 	return bits * zigVals.eqAreaZigX_NNormFast[index]; }
	        if(index == 0) { 											return bottomCase(bits < 0);   }//use most sig bit for +/-
	        // u in [-1,1], using 32 bits, i.e. with 2^-31 granularity.
	        double u = bits * zigVals.invBitMask;
	        double x = rareCase(index, u);//, bits < 0);
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
	//min and max of synthesized
	public myFleishUniRandGen(myRandVarFunc _func, String _name) {
		super(_func, _name);
		
	}//ctor

	@Override
	public double getSample() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getSampleFast() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
	
}//class myFleishRandGen
