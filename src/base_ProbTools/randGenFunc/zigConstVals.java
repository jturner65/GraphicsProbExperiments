package base_ProbTools.randGenFunc;

import base_ProbTools.randGenFunc.funcs.base.baseRandVarFunc;

//class holding ziggurat pre-calced values (in tabular form) for a particular prob function and # of rectangles
//this needs to have no memory so multiple functions can use it
public class zigConstVals{
	//based loosely on "An Improved Ziggurat Method to Generate Normal Random Samples";J. A. Doornik, 2005
	//consts used often - masks
	public static final Long lBitMask = (1L<<53), bitMask = (1L<<31);
	//used to keep values between [0,1] in nextDouble
	public static final double invLBitMask = 1.0/lBitMask, invBitMask = 1.0/bitMask;
	//owning random variable function
	private final baseRandVarFunc func;
	//Doornik's tables : Bottom reclangle has index 0 (but X_255 = R). X coordinates for equal area for each rectangle having same area; ratio of neighbors
	public final double[] eqAreaZigX, eqAreaZigX_NNorm,eqAreaZigX_NNormFast;
	//For faster nextGaussian(Random)   
	public final long[] eqAreaZigRatio_NNorm;
	///For faster nextGaussianFast(Random)
	public final int[] eqAreaZigRatio_NNormFast;
	//For faster rare cases.    
	public final double[] rareCaseEqAreaX;
	//specific values for this zig construct
	//# of equal-volume rectangles + 1 for infinite tail of same volume
	public final int Nrect;
	//volume of each ziggurat rectangle and tail
	public final double V_each;
	//x coordinate of final rectangle, such that v_each == r(f(r)) + integral(r->inf) f(x) dx
	public final double R_last, inv_R_last;
		
	//make sure all access to func's function and inv function use ziggurat versions - these have range 0->1
	public zigConstVals(baseRandVarFunc _func, int _Nrect) {
		func=_func;
		Nrect = _Nrect;
		double[] rValAndVol = calcRValAndVol();
		R_last = rValAndVol[0];
		V_each = rValAndVol[1];
		inv_R_last = 1.0/R_last;
		//build structures and est values
		eqAreaZigX = new double[Nrect+1];
		eqAreaZigX_NNorm = new double[Nrect];
		eqAreaZigX_NNormFast = new double[Nrect];
		eqAreaZigRatio_NNorm = new long[Nrect];
		eqAreaZigRatio_NNormFast = new int[Nrect];
		rareCaseEqAreaX = new double[eqAreaZigX.length];
		
		double f = func.fStd(R_last);
		double[] eqAreaZigRatio = new double[Nrect];
		//calculate X values for each equal area
		eqAreaZigX[0] = V_each / f;
		eqAreaZigX[1] = R_last;
		for (int i=2;i<Nrect;++i) {
			double xi = func.f_invStd(V_each/eqAreaZigX[i-1] + f);
			eqAreaZigX[i] = xi;
			f = func.fStd(xi);
		}
		eqAreaZigX[Nrect] = 0.0;
		
		for (int i=0;i<Nrect;++i) {      
			eqAreaZigRatio[i] = eqAreaZigX[i+1] / eqAreaZigX[i];  
			eqAreaZigRatio_NNorm[i] = (long)Math.ceil(eqAreaZigRatio[i] * lBitMask);
			eqAreaZigX_NNorm[i] = eqAreaZigX[i] * invLBitMask;
			eqAreaZigRatio_NNormFast[i] = (int)Math.floor(-eqAreaZigRatio[i] * bitMask);
			eqAreaZigX_NNormFast[i] = eqAreaZigX[i] * invBitMask;
		}
		for (int i=0;i<rareCaseEqAreaX.length;++i) {       	rareCaseEqAreaX[i] = func.fStd(eqAreaZigX[i]); }    	
	}//ctor
	
	//function described in zig paper to find appropriate r value - need to find r to make this funct == 0 
	private double[] z_R(double rVal) {
		//func.msgObj.dispMessage("myGaussianFunc", "z_R", "Starting z_R with: rVal : " + rVal,true);
		//this gives the volume at the tail - rectangle @ r + tail from r to end		
		double funcAtR = func.fStd(rVal);  							
		//vol = rF(r) + integral(r->+inf) (f_zig(x))
		double integralRes = func.integral_fStd(rVal, Double.POSITIVE_INFINITY);
		double vol = rVal* funcAtR + integralRes;//Q func == 1 - CDF
		if (vol < 0) {
			baseRandVarFunc.msgObj.dispErrorMessage("zigConstVals", "z_R", func.getShortDesc()+ "| Initial Ziggurat R val chosen to be too high, causing integration to yield a negative volume due to error");
			return new double[] {-rVal*9, 0};
		}
		//x values and functional eval of x vals
		double[] xVals = new double[Nrect], fXVals = new double[Nrect];
		xVals[Nrect-1]=rVal;
		fXVals[Nrect-1]=funcAtR;	
		double retVal = vol - xVals[Nrect-1] - xVals[Nrect-1]*fXVals[Nrect-1];
		for(int i=Nrect-2;i>=0;--i) {
			double eval = (vol/xVals[i+1]) + fXVals[i+1];
			//if eval > 1 then this is going to break ---V - this is wrong, changes the curve being fitted
			eval = (eval > 1 ? 1 : eval);
			xVals[i]=func.f_invStd(eval);
			fXVals[i]=func.fStd(xVals[i]);
			retVal = vol - xVals[i+1] + xVals[i+1]*fXVals[i+1];//area vol - vol of top block
			//func.msgObj.dispMessage("myGaussianFunc", "z_R", "Inverse @ i=="+i+" =  " + xVals[i]  + " f(x[i]) : " + fXVals[i] + " eval : " + eval + " Vol : " + (xVals[i]* fXVals[i]),true);
		}
		//double retVal = vol - xVals[1] - xVals[1]*fXVals[1];
		//func.msgObj.dispMessage("myGaussianFunc", "z_R", "End : Passed rval : " + rVal + " f(rVal) : " + funcAtR + " Vol : " + vol + " xVals[1] :"+ xVals[1]+ " F(x[1]) :"+fXVals[1] + " Return val : " + retVal,true);
		return new double[] {retVal, vol};
	}//z_R
	   
	//3.6541528853610088; is value found for n == 256 
	private static final double RValAndVolzTol = 0.00000000000001;
	//x coordinate of final rectangle, such that v_each == r(f(r)) + integral(r->inf) f(x) dx
	public double[] calcRValAndVol() {
		//find an r that will make z_r function == 0
		double rValGuess = 20;
		boolean done = false;
		int iter = 0;
		double [] zValAra = new double[] {-100,0};
		double learnRate = .1, minLearnRate = .000001, curLearnRate = learnRate;
		double oldGuess;
		while ((!done) && (iter < 1000)) {
			oldGuess = rValGuess;
			zValAra = z_R(rValGuess);
			iter++;
			if(Math.abs(zValAra[0]) < RValAndVolzTol) {done=true;} 
			else {//modify guess appropriately							
				rValGuess += zValAra[0] * learnRate;
				curLearnRate = learnRate;
				//func.msgObj.dispMessage("myRandVarFunc", "calcRVal", "Name : " + func.name+ "| For " + Nrect + " rectangles, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ "  oldGuess : " + String.format("%3.18f", oldGuess)+ " Gives zVal : " + String.format("%3.18f", zValAra[0]) + " Vol : " + String.format("%3.18f", zValAra[1]),true);
				while ((oldGuess < rValGuess) && (curLearnRate > minLearnRate)) {
					//func.msgObj.dispMessage("myRandVarFunc", "calcRVal", "\tFlip : curLearnRate : " + curLearnRate + " new learn rate : " + curLearnRate/2.0);
					curLearnRate /= 2.0;
					rValGuess -= zValAra[0] * curLearnRate;	//change mod to 1/2 last mod					
				}				
			}//not close enough, modifying guess			
			//func.msgObj.dispMessage("myRandVarFunc", "calcRVal", "Name : " + func.name+ "| For " + Nrect + " rectangles, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ " Gives zVal : " + String.format("%3.18f", zValAra[0]) + " Vol : " + String.format("%3.18f", zValAra[1]),true);
		}//while
		double[] res = new double[2];
		if(done) {
			res[0] = rValGuess;
			res[1] = zValAra[1];
		}
		//func.msgObj.dispMessage("zigConstVals", "calcRValAndVol",  func.getShortDesc()+ " | Done w/ " + Nrect + " rects, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ " Gives Vol : " + String.format("%3.18f", zValAra[1]),true);
		return res;
	}//calcRVal
	
	
	//return important values for this ziggurat const struct
	public String toString() {
		String res = "Owning Func : " +func.getShortDesc()+" # Rects : " + Nrect + " R_Last : " + String.format("%3.16f", R_last) + " | Vol Per Zig : " + String.format("%3.16f", V_each) + "\n";
		return res;	
	}
	
}//class zigConstVals

