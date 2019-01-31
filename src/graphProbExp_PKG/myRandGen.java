package graphProbExp_PKG;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Provides random generation of prob distributions given a uniform distribution
 */
public abstract class myRandGen {
    //random generator to use to generate uniform data - threadsafe
	
	//function this rand gen uses
	protected myRandVarFunc func;
	
	//state flags - bits in array holding relevant info about this random variable function
	private int[] stFlags;						
	public static final int
			debugIDX 					= 0,
			funcSetIDX					= 1;		//whether or not this random variable will be used in a ziggurat solver		
	public static final int numFlags 	= 2;	
   
	public myRandGen(myRandVarFunc _func) {
		initFlags();
		setRandomFunction(_func);
    }//ctor
	
	//set the function for this random generator.  The moments and other information necessary for the function to operate must have been set by here
	public void setRandomFunction(myRandVarFunc _func) {
		func = _func;
		setFlag(funcSetIDX, func!=null);
	}
    
    //thread-safe queries for uniform values
    protected long getNextLong() {return ThreadLocalRandom.current().nextLong();}  
    protected int getNextInt() {return ThreadLocalRandom.current().nextInt();  }
    protected double getNextDouble() {return ThreadLocalRandom.current().nextDouble();}
	
    //return gaussian  - momments defined by myRandVarFunc
	public abstract double getGaussian();
	public abstract double getGaussianFast();
	
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
	
	
}//class myRandGen

/**
 * implementation of ziggurat algorithm to build a gaussian distribution from a uniform sampling
 * @author john
 *
 */
class myZigRandGen extends myRandGen{
	//based on "An Improved Ziggurat Method to Generate Normal Random Samples";J. A. Doornik, 2005

	private zigConstVals zigVals;
	
	public myZigRandGen(myRandVarFunc _func) {
		super(_func);
		zigVals = func.zigVals;
	}//ctor
	
    
	public double getGaussian() {
		double res = nextNormal53();	
		return func.processResValByMmnts(res);
	}//getGaussian
	
	//int value
	public double getGaussianFast() {
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

//class myZigRandGen extends myRandGen{
//	//based on "An Improved Ziggurat Method to Generate Normal Random Samples";J. A. Doornik, 2005
//	
//	//consts used often - masks
//	private static final Long lBitMask = (1L<<53), bitMask = (1L<<31);
//	//used to keep values between [0,1] in nextDouble
//	private static final double invLBitMask = 1.0/lBitMask, invBitMask = 1.0/bitMask;
//	//Number of rectangles (bottom reclangle included) - all consts relate to 256 steps in ziggurat
//	private static final int Nrect = 256; 
//	
//	//X where the tail starts. - functionally dependent values
//	private static final double R_256 = 3.6541528853610088, invR_256 = 1.0/R_256;   
//	//Vol of zig rectangles - larger than vol of segment under curve (from paper)
//	private static final double V_256 = 0.00492867323399;  
//	//idxs for reg, nnorm
//    //Doornik's tables : Bottom reclangle has index 0 (but X_255 = R). X coordinates for equal area for each rectangle having same area; ratio of neighbors
//    private static final double[] eqAreaZigX = new double[Nrect+1], //eqAreaZigRatio = new double[Nrect],
//    		eqAreaZigX_NNorm = new double[Nrect],eqAreaZigX_NNormFast = new double[Nrect];
//    //For faster nextGaussian(Random)   
//   	private static final long[] eqAreaZigRatio_NNorm = new long[Nrect];
//   	///For faster nextGaussianFast(Random)
//   	private static final int[] eqAreaZigRatio_NNormFast = new int[Nrect];
//    //For faster rare cases.    
//    private static final double[] rareCaseEqAreaX = new double[eqAreaZigX.length];
//    //if static fields built yet
//    private static boolean built = false;
//	
//	public myZigRandGen(myRandVarFunc _func) {
//		super(_func);
//    	if(!built) {
//    		buildStaticVals();
//            built = true;
//    	}    
//	}//ctor
//	
//	   //this can be used for any decreasing function f
//    private void buildStaticVals() {
//        double f = f(R_256);
//        double[] eqAreaZigRatio = new double[Nrect];
//        eqAreaZigX[0] = V_256 / f;
//        eqAreaZigX[1] = R_256;
//        for (int i=2;i<Nrect;++i) {
//            double xi = f_inv(V_256/eqAreaZigX[i-1] + f);
//            eqAreaZigX[i] = xi;
//            f = f(xi);
//        }
//        eqAreaZigX[Nrect] = 0.0;
//        
//        for (int i=0;i<Nrect;++i) {      
//        	eqAreaZigRatio[i] = eqAreaZigX[i+1] / eqAreaZigX[i];  
//            eqAreaZigRatio_NNorm[i] = (long)Math.ceil(eqAreaZigRatio[i] * lBitMask);
//            eqAreaZigX_NNorm[i] = eqAreaZigX[i] * invLBitMask;
//            eqAreaZigRatio_NNormFast[i] = (int)Math.floor(-eqAreaZigRatio[i] * bitMask);
//            eqAreaZigX_NNormFast[i] = eqAreaZigX[i] * invBitMask;
//        }
//        for (int i=0;i<rareCaseEqAreaX.length;++i) {        
//        	rareCaseEqAreaX[i] = f(eqAreaZigX[i]);
//        	//System.out.println(""+String.format("%.7f",rareCaseEqAreaX[i]));
//        }    	
//    }//buildStaticVals	
//	
//    
//	public double getGaussian(double mu, double std) {
//		double res = nextNormal53();	
//		return (res*std) + mu;
//	}//getGaussian
//	
//	//int value
//	public double getGaussianFast(double mu, double std) {
//		double res = nextNormal32();		
//		return (res*std) + mu;
//	}//getGaussian
//	
//    /**
//     * @return A normal gaussian number.
//     */
//    private double nextNormal53() {
//    	long bits, uLong;
//    	int index;
//    	while (true) {
//	    	//single random 64 bit value
//	        bits = getNextLong();
//	        //uLong is using 54 Most Sig Bits (and 1st Most Sig Bits as sign bit).
//	        uLong = (bits>>10);
//	        //Using 8 Least sig Bits as index.
//	        index = ((int)bits) & 0xFF;        
//	        if (Math.abs(uLong) < eqAreaZigRatio_NNorm[index]) {   	return uLong * eqAreaZigX_NNorm[index]; }       
//	        if(index == 0) { 										return bottomCase((bits<<55) < 0);   }// Using 9th LSBit to decide +/-
//	        // u in [-1,1], using 54 Most Sig Bits, i.e. with 2^-53 granularity.
//	        double u = uLong * invLBitMask;
//	        // Using 9th LSBit to decide which side to go (has not been used yet).
//	        double x = rareCase(index, u);//, (bits<<55) < 0);
//	        if (x==x) {return x;    }		//Nan test -> NaN != NaN
//    	}
//    }//nextNormal
//
//    /**
//     * @return A normal gaussian number with 32 bit granularity
//     */
//    private double nextNormal32() {
//    	int bits, index;
//    	while (true) {
//	        bits = getNextInt();
//	        index = (bits & 0xFF);
//	        if (-Math.abs(bits) >= eqAreaZigRatio_NNormFast[index]) { 	return bits * eqAreaZigX_NNormFast[index]; }
//	        if(index == 0) { 											return bottomCase(bits < 0);   }//use most sig bit for +/-
//	        // u in [-1,1], using 32 bits, i.e. with 2^-31 granularity.
//	        double u = bits * invBitMask;
//	        double x = rareCase(index, u);//, bits < 0);
//	        if (x==x) {return x;    }		//Nan test -> NaN != NaN
//    	}
//    }//nextNormalFast
//    
//    //gives normal distribution curve
//    private double f(double x) {     return Math.exp(-0.5 * (x*x));  }
//    //gives inverse of normal distribution curve
//    private double f_inv(double xInv) { 	return Math.sqrt(-2.0 * Math.log(xInv));}
//
//    /**
//     * u and negSide are used exclusively, so it doesn't hurt
//     * randomness if same random bits were used to compute both.
//     * 
//     * @return Value to return, or nan if needing to retry
//     */
//    private double rareCase(int idx, double u) {
//        double x = u * eqAreaZigX[idx];
//        //verify under curve
//        if (rareCaseEqAreaX[idx+1] + (rareCaseEqAreaX[idx] - rareCaseEqAreaX[idx+1]) * getNextDouble() < f(x)) {          return x;    }
//        return Double.NaN;
//    }
//    
//    private double bottomCase(boolean negSide) {        
//        double x = -1.0, y = 0.0;
//        while (-(y + y) < x * x) {
//            x = Math.log(getNextDouble()+invLBitMask) * invR_256;
//            y = Math.log(getNextDouble()+invLBitMask);
//        } 
//        return negSide ? x - R_256 : R_256 - x;
//    }
//    
//    
//	
//}//class myZigRandGen
//
