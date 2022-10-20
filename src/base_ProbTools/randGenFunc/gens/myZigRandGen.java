package base_ProbTools.randGenFunc.gens;

import base_ProbTools.randGenFunc.zigConstVals;
import base_ProbTools.randGenFunc.funcs.base.baseRandVarFunc;
import base_ProbTools.randGenFunc.gens.base.myRandGen;

/**
 * implementation of ziggurat algorithm to build a distribution from a uniform sampling
 * @author john
 *
 */
public class myZigRandGen extends myRandGen{
	//based on "An Improved Ziggurat Method to Generate Normal Random Samples";J. A. Doornik, 2005
	//reference to ziggurat values used by this random variable generator
	private zigConstVals zigVals;
	//# of rects used for zig
	private final int numZigRects, numZigIDXMask;
	
	//pass RV generating function
	public myZigRandGen(baseRandVarFunc _func, int _numZigRects, String _name) {
		super(_func, _name);
		numZigRects=_numZigRects;
		numZigIDXMask = numZigRects-1;
		func.setZigVals(numZigRects);			
		zigVals = func.zigVals;
	}//ctor
	
	public myZigRandGen(baseRandVarFunc _func, String _name) {
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
	//private int numNanRes = 0;
	private double nextNormal53() {
    	double x;
    	while (true) {
			x = getFuncValFromLong(getNextLong());
			if (x==x) {return x;    }		//Nan test -> NaN != NaN
			//else {System.out.println("Nan res : " + ++numNanRes);}
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