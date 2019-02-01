package graphProbExp_PKG;

import java.math.*;
import java.util.function.Function;

/**
 * classes to provide the functionality of a random variable to be consumed by the random number generators.
 * These classes will model the pdf and inv pdf of a particular random variable, as well as provide access to integration (to derive CDF)
 * 
 * @author john
 *
 */
public abstract class myRandVarFunc {
	public static BaseProbExpMgr expMgr;
	//descriptive name of function
	public final String name;
	
	//quadrature solver for this random variable/function
	protected myGaussQuad quadSlvr;
	
	//state flags - bits in array holding relevant info about this random variable function
	private int[] stFlags;						
	public static final int
			debugIDX 					= 0,
			useZigAlgIDX				= 1,		//whether or not this random variable will be used in a ziggurat solver		
			//momments set
			meanSetIDX					= 2,		//specific mean is set
			stdSetIDX					= 3,		//specific std is set
			skewSetIDX					= 4,		//specific skew is set
			kurtSetIDX					= 5,		//specific kurt is set
			//quad solver set
			quadSlvrSetIDX				= 6;		//whether quadrature solver has been set or not
	public static final int numFlags 	= 7;	
	
	//mean and std of this distribution.  mean, std, var may be null/undefined for certain distributions (i.e. cauchy)
	protected Double[] mmnts;
	public static final int 
		meanIDX			= 0, 
		stdIDX			= 1, 
		varIDX			= 2,
		skewIDX			= 3,
		kurtIDX			= 4;
	public static final int numMmnts = 5;

	//functional representation of pdfs and inv pdfs, and normalized @ 0 for ziggurat calc
	protected Function<Double, Double>[] funcs;
	//function idxs
	protected static final int 
		fIDX 		= 0,
		fInvIDX 	= 1,
		fZigIDX		= 2,
		fInvZigIDX	= 3;
	protected static final int numFuncs = 4;
	
	//object used to perform ziggurat calcs for a particular function - contains pre-calced arrays
	//each instancing class will have a static map of these, and only build a new one if called for
	public zigConstVals zigVals;
	public int numZigRects = 256;
	
	//////////////////////////////////
	///useful constants
    //scale factor for normal N(0,1)
	protected static double invSqrt2 = 1.0/Math.sqrt(2.0),
							ln2 = Math.log(2.0);
	
	public myRandVarFunc(BaseProbExpMgr _expMgr, myGaussQuad _quadSlvr, String _name) {
		expMgr = _expMgr;name=_name;
		initFlags();
		setQuadSolver(_quadSlvr);
		mmnts = new Double[numMmnts];
	}//ctor
	
	//call this to set desired values for mean and std - possibly change them on constructed object? - called from ctor
	protected void setMeanStd(Double _mu, Double _std){
		mmnts[meanIDX]=_mu;
		setFlag(meanSetIDX, true);
		mmnts[stdIDX]=_std; 
		setFlag(stdSetIDX, true);
		if(mmnts[stdIDX] != null) {mmnts[varIDX]=mmnts[stdIDX]*mmnts[stdIDX];} else {mmnts[varIDX]=null;}
		funcs = new Function[numFuncs];
		buildFuncs();
	}//setMeanStd
	
	//sets quadrature solver to be used to solve any integration for this RV func
	public void setQuadSolver(myGaussQuad _quadSlvr) {
		quadSlvr = _quadSlvr;
		setFlag(quadSlvrSetIDX, quadSlvr!=null);
	}//setSolver	
	
	public myGaussQuad getQuadSolver() {return quadSlvr;}
	public String getQuadSolverName() {
		if (getFlag(quadSlvrSetIDX)) { return quadSlvr.name;}
		return "None Set";
	}
	public Double getMean() {return mmnts[meanIDX];}
	public Double getStd() {return mmnts[stdIDX];}
	public Double getVar() {return mmnts[varIDX];}
	public Double getSkew() {return mmnts[skewIDX];}
	public Double getKurt() {return mmnts[kurtIDX];}
	
	//build individual functions that describe pdf, inverse pdf and ziggguart (scaled to 1 @ 0) pdf and inv pdf
	protected abstract void buildFuncs();
	
	//if this rand var is going to be accessed via the ziggurat algorithm, this needs to be called w/# of rectangles to use
	//this must be called after an Quad solver has been set, since finding R and Vol for passed # of ziggurats requires such a solver
	public void setZigVals(int _nRect) {
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myRandVarFunc", "setZigVals", "No quadrature solver has been set, so cannot set ziggurat values for "+_nRect+" rectangles (incl tail)."); return;}
		double checkRect = Math.log(_nRect)/ln2;
		int nRectCalc = (int)Math.pow(2.0, checkRect);//int drops all decimal values
		if (_nRect != nRectCalc) {	
			int numRectToUse = (int)Math.pow(2.0, (int)(checkRect) + 1);
			expMgr.dispMessage("myRandVarFunc", "setZigVals", "Number of ziggurat rectangles requested " + _nRect + " : " + nRectCalc + " must be an integral power of 2, so forcing requested " + _nRect + " to be " + numRectToUse);
			numZigRects = numRectToUse;
		}		
		zigVals = new zigConstVals(this,numZigRects);
		setFlag(useZigAlgIDX, true);
	}//setZigVals
	
	public final double f(double x) {return funcs[fIDX].apply(x);}
	//calculate the inverse of f
	public final double f_inv(double xInv){return funcs[fInvIDX].apply(xInv);}	
	//calculate f normalized for ziggurate method, so that f(0) == 1;
	public final double fZig(double x){return funcs[fZigIDX].apply(x);}
	//calculate the inverse of f
	public final double f_invZig(double xInv){return funcs[fInvZigIDX].apply(xInv);}
	
	//calculate integral of f between x1 and x2.  Use to calculate cumulative distribution by making x1==-inf, and x2 definite; qfunc by setting x1 to a value and x2 == +inf
	public abstract double integral_f(Double x1, Double x2);
	//calculate integral of normalized f (for ziggurat calc) between x1 and x2.  Use to calculate cumulative distribution by making x1==-inf, and x2 definite; qfunc by setting x1 to a value and x2 == +inf
	public abstract double integral_fZig(Double x1, Double x2);	
	
	//temp testing function - returns R and Vol
	public double[] dbgTestCalcRVal(int nRect) {
		numZigRects = nRect;
		zigConstVals tmpZigVals = new zigConstVals(this,numZigRects);
		return tmpZigVals.calcRValAndVol();		
	}//testCalcRVal
	
	//process a result from a 0-centered, 1-std distribution to match stated moments of this distribution
	public abstract double processResValByMmnts(double val);

	private void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX 		: 	{break;}	
			case useZigAlgIDX 	: 	{break;}
			case meanSetIDX	 	: 	{break;} 
			case stdSetIDX	 	: 	{break;} 
			case skewSetIDX	 	: 	{break;} 
			case kurtSetIDX	 	: 	{break;} 						
			case quadSlvrSetIDX	: 	{break;}
		}
	}//setFlag		
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}	
	
	
	//describes data
	public String getFuncDataStr(){
		String res = "Type of distribution :  " + name +"\tMean:"+String.format("%3.8f",mmnts[meanIDX])+"\tSTD : "+ String.format("%3.8f",mmnts[stdIDX])+"\tVar:"+String.format("%3.8f",mmnts[varIDX]);
		if(getFlag(useZigAlgIDX)) {	res += "\n\tUsing Ziggurat Algorithm : " + zigVals.toString();}		
		return res;
	}//getFuncDataStr
	
	public String getShortDesc() {
		return "Name : " + name +"|Mean:"+String.format("%3.8f",mmnts[meanIDX])+"\tSTD:"+ String.format("%3.8f",mmnts[stdIDX]);
	}
	
}//class myRandVariable
/**
 * instancing class for the function describing a gaussian random variable of specified mean and variance
 * @author john
 *
 */

class myGaussianFunc extends myRandVarFunc{	
	//////////////////////////////
	//zig algorithm fields for scaled normal - all myRandVarFuncs need their own impelemtnations of this map, independent of base class
	//////////////////////////////
    //functional definition of error function to calculate CDF of normal distribution
    protected Function<Double, Double> errorFunc;
    //scl coefficient for mean 0/std = 1
	protected static final double normalSclFact = 1.0/Math.sqrt(2.0*Math.PI);
	//coefficient for error function
	protected static final double ErfCoef = 2.0/Math.sqrt(Math.PI);
	//for CDF and Q func calcs in BD realm
	public static BigDecimal inGaussSclFactBD;
	public static final BigDecimal halfVal = new BigDecimal(.5);
	
    protected double gaussSclFact, meanStd, invStdSclFact;

	public myGaussianFunc(BaseProbExpMgr _expMgr, myGaussQuad _quadSlvr, double _mean, double _std, String _name) {
		super(_expMgr,_quadSlvr, _name);
		gaussSclFact = (1.0/_std) *normalSclFact;
		inGaussSclFactBD = new BigDecimal(1.0/gaussSclFact);
		meanStd = _mean/_std;
		invStdSclFact = (1.0/_std) * invSqrt2;
		//System.out.println("Mean : " + _mean + " std "+ _std + "| invStdSclFact : " +invStdSclFact);
		setMeanStd(_mean,_std);		
	}//ctor
	public myGaussianFunc(BaseProbExpMgr _expMgr, myGaussQuad _quadSlvr,  double _mean, double _std) {this(_expMgr, _quadSlvr, _mean,_std, "Gaussian");}
	
	@Override
	protected void buildFuncs() {
		errorFunc =  (x ->  ErfCoef * Math.exp(-(x*x)) );
		
		//actual probablity functions
		funcs[fIDX] 		= (x -> (gaussSclFact  * Math.exp(-0.5 * ((x-mmnts[meanIDX])*(x-mmnts[meanIDX]))/mmnts[varIDX]))   );
		funcs[fInvIDX] 		= (xinv -> (mmnts[stdIDX]*Math.sqrt(-2.0 * Math.log(xinv/gaussSclFact))) + meanStd);
		//zigurat functions -> want pure normal distribution
		funcs[fZigIDX]		= (x -> Math.exp(-0.5 *(x*x)));
		funcs[fInvZigIDX]	= (xinv -> (Math.sqrt(-2.0 * Math.log(xinv)))); 
	}//buildFuncs
	
	//shift by mean, multiply by std
	@Override
	public double processResValByMmnts(double val) {	return mmnts[stdIDX]*val + mmnts[meanIDX];}//public abstract double processResValByMmnts(double val);

	//calculate integral of f from x1 to x2.  Use to calculate cumulative distribution by making x1== -inf, and x2 definite val
	@Override
	public double integral_f(Double x1, Double x2) {
		double res = 0;
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myGaussianFunc", "integral_f", "No quadrature solver has been set, so cannot integrate f");return res;}
		//expMgr.dispMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2);
		
		//if x1 is -inf... gauss-legendre quad - use error function via gaussian quad - calculating cdf
		if(x1==Double.NEGATIVE_INFINITY) {				//cdf of x2 == .5 + .5 * error function x2/sqrt(2) 
			//expMgr.dispMessage("myGaussianFunc", "integral_f", "CDF : x1 : "+x1 + " and  x2 : " + x2 + " Using x2");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x2 - mmnts[meanIDX])*invStdSclFact);
			//cdf == .5*(1+erf(x/sqrt(2))) 
			//res = halfVal.add(halfVal.multiply(erroFuncVal));		
			res = .5 + .5 * erroFuncVal.doubleValue();			
		} else if (x2==Double.POSITIVE_INFINITY) {		//pos inf -> this is 1- CDF == Q function
			//expMgr.dispMessage("myGaussianFunc", "integral_f", "Q func : x1 : "+x1 + " and  x2 : " + x2 + " Using x1");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x1 - mmnts[meanIDX])*invStdSclFact);
			//Q function is == 1 - (.5*(1+erf(x/sqrt(2))))
			//res = BigDecimal.ONE.subtract(halfVal.add(halfVal.multiply(erroFuncVal)));		
			res = 1.0 - (.5 + .5 * erroFuncVal.doubleValue());				
		} else {
			res = quadSlvr.evalIntegral(funcs[fIDX], x1, x2).doubleValue();
		}
		//expMgr.dispMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2 + " Res : \n" + res);
		return res;
	}//integral_f

	//calculate integral of f from x1 to x2.  Use to calculate cumulative distribution by making x1== -inf, and x2 definite val
	@Override
	public double integral_fZig(Double x1, Double x2) {
		double res = 0;
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myGaussianFunc", "integral_fZig", "No quadrature solver has been set, so cannot integrate f");return res;}
		//expMgr.dispMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2);		
		//if x1 is -inf... gauss-legendre quad - use error function via gaussian quad - calculating cdf
		if(x1==Double.NEGATIVE_INFINITY) {				//cdf of x2 == .5 + .5 * error function x2/sqrt(2) 
			//expMgr.dispMessage("myGaussianFunc", "integral_f", "CDF : x1 : "+x1 + " and  x2 : " + x2 + " Using x2");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, x2*invSqrt2);
			//cdf == .5*(1+erf(x/sqrt(2))) 
			//res = halfVal.add(halfVal.multiply(erroFuncVal));		
			res = .5 + .5 * erroFuncVal.doubleValue();			
		} else if (x2==Double.POSITIVE_INFINITY) {		//pos inf -> this is 1- CDF == Q function
			//expMgr.dispMessage("myGaussianFunc", "integral_f", "Q func : x1 : "+x1 + " and  x2 : " + x2 + " Using x1");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, x1*invSqrt2);
			//Q function is == 1 - (.5*(1+erf(x/sqrt(2))))
			//res = BigDecimal.ONE.subtract(halfVal.add(halfVal.multiply(erroFuncVal)));		
			res = 1.0 - (.5 + .5 * erroFuncVal.doubleValue());				
		} else {
			res = quadSlvr.evalIntegral(funcs[fZigIDX], x1, x2).doubleValue();
		}
		//expMgr.dispMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2 + " Res : \n" + res);
		return 1.0/normalSclFact * res;		//must have 1.0/normalSclFact to normalize integration results (i.e. scale CDF for function with p(0) == 1
		//return (1.0/gaussSclFact) *  integral_f(x1, x2);
		//return inGaussSclFactBD.multiply( integral_f(x1, x2));
	}//integral_f
	

}//class myGaussianFunc


/**
 * instancing class for the function describing a normal random variable - explicitly mean == 0, var==std==1
 * @author john
 *
 */
class myNormalFunc extends myGaussianFunc{
	
	//////////////////////////////
	//zig algorithm fields for scaled normal - all myRandVarFuncs need their own impelemtnations of this map, independent of base class
	//////////////////////////////		
	public myNormalFunc(BaseProbExpMgr _expMgr, myGaussQuad _quadSlvr) {
		super(_expMgr,_quadSlvr, 0.0, 1.0, "Normal");			
	}//ctor	
	
}//class myNormalFunc



//class holding ziggurat pre-calced values (in tabular form) for a particular prob function and # of rectangles
//this needs to have no memory so multiple functions can use it
class zigConstVals{
	//based loosely on "An Improved Ziggurat Method to Generate Normal Random Samples";J. A. Doornik, 2005
	//consts used often - masks
	public static final Long lBitMask = (1L<<53), bitMask = (1L<<31);
	//used to keep values between [0,1] in nextDouble
	public static final double invLBitMask = 1.0/lBitMask, invBitMask = 1.0/bitMask;
	//owning random variable function
	private final myRandVarFunc func;
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
	public zigConstVals(myRandVarFunc _func, int _Nrect) {
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
		
		double f = func.fZig(R_last);
		double[] eqAreaZigRatio = new double[Nrect];
		//calculate X values for each equal area
		eqAreaZigX[0] = V_each / f;
		eqAreaZigX[1] = R_last;
		for (int i=2;i<Nrect;++i) {
			double xi = func.f_invZig(V_each/eqAreaZigX[i-1] + f);
			eqAreaZigX[i] = xi;
			f = func.fZig(xi);
		}
		eqAreaZigX[Nrect] = 0.0;
		
		for (int i=0;i<Nrect;++i) {      
			eqAreaZigRatio[i] = eqAreaZigX[i+1] / eqAreaZigX[i];  
			eqAreaZigRatio_NNorm[i] = (long)Math.ceil(eqAreaZigRatio[i] * lBitMask);
			eqAreaZigX_NNorm[i] = eqAreaZigX[i] * invLBitMask;
			eqAreaZigRatio_NNormFast[i] = (int)Math.floor(-eqAreaZigRatio[i] * bitMask);
			eqAreaZigX_NNormFast[i] = eqAreaZigX[i] * invBitMask;
		}
		for (int i=0;i<rareCaseEqAreaX.length;++i) {       	rareCaseEqAreaX[i] = func.fZig(eqAreaZigX[i]); }    	
	}//ctor
	
	//function described in zig paper to find appropriate r value - need to find r to make this funct == 0 
	private double[] z_R(double rVal) {
		//expMgr.dispMessage("myGaussianFunc", "z_R", "Start : rVal : " + rVal + " nRect " + nRect);
		//this gives the volume at the tail - rectangle @ r + tail from r to end		
		double funcAtR = func.fZig(rVal);  							
		//vol = rF(r) + integral(r->+inf) (f_zig(x))
		double integralRes = func.integral_fZig(rVal, Double.POSITIVE_INFINITY);
		double vol = rVal* funcAtR + integralRes;//Q func == 1 - CDF
		if (vol < 0 ) {
			func.expMgr.dispMessage("zigConstVals", "z_R", func.getShortDesc()+ "| Initial Ziggurat R val chosen to be too high, causing integration to yield a negative volume due to error");
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
			xVals[i]=func.f_invZig(eval);
			fXVals[i]=func.fZig(xVals[i]);
			retVal = vol - xVals[i+1] + xVals[i+1]*fXVals[i+1];//area vol - vol of top block
			//func.expMgr.dispMessage("myGaussianFunc", "z_R", "Inverse @ i=="+i+" =  " + xVals[i]  + " f(x[i]) : " + fXVals[i] + " eval : " + eval + " Vol : " + (xVals[i]* fXVals[i]));
		}
		//double retVal = vol - xVals[1] - xVals[1]*fXVals[1];
		//func.expMgr.dispMessage("myGaussianFunc", "z_R", "End : Passed rval : " + rVal + " f(rVal) : " + funcAtR + " Vol : " + vol + " xVals[1] :"+ xVals[1]+ " F(x[1]) :"+fXVals[1] + " Return val : " + retVal );
		return new double[] {retVal, vol};
	}//z_R
	   
	//3.6541528853610088; is value found for n == 256 
	private static final double RValAndVolzTol = 0.00000000000001;
	//x coordinate of final rectangle, such that v_each == r(f(r)) + integral(r->inf) f(x) dx
	protected double[] calcRValAndVol() {
		//find an r that will make z_r function == 0
		double rValGuess = 20.0;
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
				//func.expMgr.dispMessage("myRandVarFunc", "calcRVal", "Name : " + func.name+ "| For " + Nrect + " rectangles, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ "  oldGuess : " + String.format("%3.18f", oldGuess)+ " Gives zVal : " + String.format("%3.18f", zValAra[0]) + " Vol : " + String.format("%3.18f", zValAra[1]));
				while ((oldGuess < rValGuess) && (curLearnRate > minLearnRate)) {
					//func.expMgr.dispMessage("myRandVarFunc", "calcRVal", "\tFlip : curLearnRate : " + curLearnRate + " new learn rate : " + curLearnRate/2.0);
					curLearnRate /= 2.0;
					rValGuess -= zValAra[0] * curLearnRate;	//change mod to 1/2 last mod					
				}				
			}//not close enough, modifying guess			
			//func.expMgr.dispMessage("myRandVarFunc", "calcRVal", "Name : " + func.name+ "| For " + Nrect + " rectangles, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ " Gives zVal : " + String.format("%3.18f", zValAra[0]) + " Vol : " + String.format("%3.18f", zValAra[1]));
		}//while
		double[] res = new double[2];
		if(done) {
			res[0] = rValGuess;
			res[1] = zValAra[1];
		}
		func.expMgr.dispMessage("myRandVarFunc", "calcRVal",  func.getShortDesc()+ " | Finished with " + Nrect + " rectangles, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ " Gives Vol : " + String.format("%3.18f", zValAra[1]));
		return res;
	}//calcRVal
	
	
	//return important values for this ziggurat const struct
	public String toString() {
		String res = "Owning Func : " +func.getShortDesc()+" # Rectangles : " + Nrect + " R_Last : " + String.format("%3.16f", R_last) + " | Vol Per Zig : " + String.format("%3.16f", V_each) + "\n";
		return res;	
	}
	
}//class zigConstVals

