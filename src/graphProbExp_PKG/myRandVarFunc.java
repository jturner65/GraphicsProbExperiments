package graphProbExp_PKG;

import java.math.*;
import java.util.TreeMap;
import java.util.function.Function;

import org.jblas.*;

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
	protected myIntegrator quadSlvr;
	
	//object to hold descriptive values and statistics for this distribution, and any source data/samples, if they exist
	protected myProbSummary summary;
	
	//convergence limit for iterative calcs
	public final double convLim=1e-6;
	
	//state flags - bits in array holding relevant info about this random variable function
	private int[] stFlags;						
	public static final int
			//whether to use zig alg for solving	
			useZigAlgIDX				= 0,		//whether or not this random variable will be used in a ziggurat solver		
			//quad solver set
			quadSlvrSetIDX				= 1;		//whether quadrature solver has been set or not
	public static final int numFlags 	= 2;	

	//functional representation of pdfs and inv pdfs, and normalized @ 0 for ziggurat calc
	protected Function<Double, Double>[] funcs;	
	//function idxs
	protected static final int 
		fIDX	 		= 0,
		fInvIDX 		= 1,
		//standardized results-> 0 mean, 1 std (i.e. functions specifically for ziggurat calc - expected to be 0-centered
		fStdIDX			= 2,
		fInvStdIDX		= 3,
		//derivative functions
		fDerivIDX		= 4,
		fStdDeriveIDX	= 5;

	protected static final int numFuncs = 6;
	//integral functions - take in 2 arguments as input, give 1 argument as out
	protected Function<Double[], Double>[] integrals;
	protected static final int 
		fIntegIDX		= 0,
		fStdIntegIDX	= 1;
	protected static final int numIntegrals  =2;
	
	//object used to perform ziggurat calcs for a particular function - contains pre-calced arrays
	//each instancing class will have a static map of these, and only build a new one if called for
	public zigConstVals zigVals;
	public int numZigRects = 256;
	
	//////////////////////////////////
	///useful constants
    //scale factor for normal N(0,1)
	protected static final double invSqrt2 = 1.0/Math.sqrt(2.0),
							ln2 = Math.log(2.0),
							halfPi =  Math.PI*.5, 
							twoPi = Math.PI*2.0;
	
	//types of functions to query
	public static final int
		queryFuncIDX = 0,
		queryCDFIDX = 1,
		queryInvCDFIDX = 2,
		queryIntegIDX = 3;
	
	public static final String[] queryFuncTypes = new String[] {"Function Eval", "CDF Eval", "Inv CDF Eval","Integral Eval"};	
	
	public myRandVarFunc(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj, String _name) {
		expMgr = _expMgr;name=_name;
		initFlags();
		setQuadSolver(_quadSlvr);
		rebuildFuncs(_summaryObj);
	}//ctor
	
	public void rebuildFuncs(myProbSummary _summaryObj) {
		summary=_summaryObj;
		rebuildFuncs_Indiv( );
		funcs= new Function[numFuncs];
		integrals = new Function[numIntegrals];
		buildFuncs();
	}//rebuildFuncs
	//instancing class should call buildFuncs from this function
	protected abstract void rebuildFuncs_Indiv();
	//build individual functions that describe pdf, inverse pdf and zigggurat (scaled to y==1 @ x==0) pdf and inv pdf, if necesssary
	protected abstract void buildFuncs();

	//set/get quadrature solver to be used to solve any integration for this RV func
	public void setQuadSolver(myIntegrator _quadSlvr) {
		quadSlvr = _quadSlvr;
		setFlag(quadSlvrSetIDX, quadSlvr!=null);
	}//setSolver	
	public myIntegrator getQuadSolver() {return quadSlvr;}
	public String getQuadSolverName() {
		if (getFlag(quadSlvrSetIDX)) { return quadSlvr.name;}
		return "None Set";
	}	
	//momments
	
	public double getMean() {return summary.mean();}
	public double getStd() {return summary.std();}
	public double getVar() {return summary.var();}
	public double getSkew() {return summary.skew();}
	public double getKurt() {return summary.kurt();}
	//for plotting results - this returns bounds
	public abstract double[] getPlotValBounds();
	
	//ignores x2 for all functions but integral
	public final double getFuncVal(int funcType, double x1, double x2) {
		switch(funcType) {
		case queryFuncIDX : {
			return f(x1);}
		case queryCDFIDX : {
			return CDF(x1);	}
		case queryInvCDFIDX: {
			return CDF_inv(x1);}
		case queryIntegIDX : {
			return integral_f(x1,x2);}		
		default : {
			expMgr.dispMessage("myRandVarFunc", "getFuncVal", "Attempting to evaluate unknown func type : " + funcType +" on value(s) : [" + x1 + ", "+ x2 + "] Aborting." , true);
			return x1;}
		}
	}//getFuncVal
	
	//calculate pdf function f
	public final double f(double x) {return funcs[fIDX].apply(x);}
	//calculate the inverse of f
	public final double f_inv(double xInv){return funcs[fInvIDX].apply(xInv);}	
	//calculate f normalized for ziggurate method, so that f(0) == 1;
	public final double fStd(double x){return funcs[fStdIDX].apply(x);}
	//calculate the inverse of f
	public final double f_invStd(double xInv){return funcs[fInvStdIDX].apply(xInv);}
	
	//calculate f normalized for ziggurate method, so that f(0) == 1;
	public final double fDeriv(double x){return funcs[fDerivIDX].apply(x);}
	//calculate the inverse of f
	public final double fStdDeriv(double xInv){return funcs[fStdDeriveIDX].apply(xInv);}
	
	//calculate the cdf
	public abstract double CDF(double x);
	//calculate inverse cdf of passed value 0->1
	public abstract double CDF_inv(double x);	
	
	//find inverse  value -> x value such that CDF(X<= x) == p
	//Lower bound should either be neg inf or the lower bound of the pdf, if it is bounded
	public double calcInvCDF(double p, Function<Double[], Double> integralFunc, Double lbnd) {
		double xVal = 0, calcPVal = 0, diff;
		Double[] args = new Double[] {lbnd, 0.0};
		//double lBndVal = a*(lbnd + Math.sin(freq*lbnd)/freq);
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			//calcPVal = a*(xVal + Math.sin(freq*(xVal-mu))/freq) - lBndVal;//solve for std value - ignore mu
			args[1]=xVal;
			calcPVal = integralFunc.apply(args);//solve for std value - ignore mu
			diff = p - calcPVal;
			//System.out.println("iter " + i + " diff : " + String.format("%3.8f", diff) + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " sinFreqS : "+ String.format("%3.8f", calcPVal));
			if(Math.abs(diff) < convLim) {				done=true;			}
			xVal += .2*diff;
			++i;
		}//
		//System.out.println("Final InvCDF val : iters " + i + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " prob(xVal) : " +  String.format("%3.8f", calcPVal)+ " lbnd : " +  String.format("%3.8f", lBndVal));
		return xVal;
	}//calcInvCDF
	
	//calculate integral of f between x1 and x2.  Use to calculate cumulative distribution by making x1==-inf, and x2 definite; qfunc by setting x1 to a value and x2 == +inf
	protected abstract double integral_f(Double x1, Double x2);	
	//calculate integral of normalized f (for ziggurat calc) between x1 and x2.  Use to calculate cumulative distribution by making x1==-inf, and x2 definite; qfunc by setting x1 to a value and x2 == +inf
	protected abstract double integral_fStd(Double x1, Double x2);	
		
	//process a result from a 0-centered, 1-std distribution to match stated moments of this distribution
	public abstract double processResValByMmnts(double val);	
	
	//if this rand var is going to be accessed via the ziggurat algorithm, this needs to be called w/# of rectangles to use
	//this must be called after an Quad solver has been set, since finding R and Vol for passed # of ziggurats requires such a solver
	public void setZigVals(int _nRect) {
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myRandVarFunc", "setZigVals", "No quadrature solver has been set, so cannot set ziggurat values for "+_nRect+" rectangles (incl tail).",true); return;}
		double checkRect = Math.log(_nRect)/ln2;
		int nRectCalc = (int)Math.pow(2.0, checkRect);//int drops all decimal values
		if (_nRect != nRectCalc) {	
			int numRectToUse = (int)Math.pow(2.0, (int)(checkRect) + 1);
			expMgr.dispMessage("myRandVarFunc", "setZigVals", "Number of ziggurat rectangles requested " + _nRect + " : " + nRectCalc + " must be an integral power of 2, so forcing requested " + _nRect + " to be " + numRectToUse,true);
			numZigRects = numRectToUse;
		}		
		zigVals = new zigConstVals(this,numZigRects);
		setFlag(useZigAlgIDX, true);
	}//setZigVals
	
	//test if newton iteration is done
	protected boolean isNewtonDone(DoubleMatrix f) {
		for(double x : f.data) {
			if(Math.abs(x) > convLim) {return false;}
		}		
		return true;
	}//isNewtonDone
	
	
	//temp testing function - returns R and Vol
	public double[] dbgTestCalcRVal(int nRect) {
		numZigRects = nRect;
		zigConstVals tmpZigVals = new zigConstVals(this,numZigRects);
		return tmpZigVals.calcRValAndVol();		
	}//testCalcRVal

	private void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag		
			case useZigAlgIDX 	: 	{break;}
			case quadSlvrSetIDX	: 	{break;}
		}
	}//setFlag		
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}	
	
	//describes data
	public String getFuncDataStr(){
		String res = "Type of distribution :  " + name +"|"+summary.getMoments();
		if(getFlag(useZigAlgIDX)) {	res += "\n\tUsing Ziggurat Algorithm : " + zigVals.toString();}		
		return res;
	}//getFuncDataStr
	
	public String getShortDesc() {
		String res = "Name : " +name + "|"+summary.getMinNumMmntsDesc();
		return res;
	}
	//get minimal string description, useful for key for map
	public String getMinDescString() {
		String res = name+summary.getMinNumMmnts();
		return res;
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
    //summary object needs to exist before ctor is called
	public myGaussianFunc(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj, String _name) {
		super(_expMgr,_quadSlvr, _summaryObj, _name);	
	}//ctor
	public myGaussianFunc(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj) {this(_expMgr, _quadSlvr,  _summaryObj, "Gaussian");}
	
	//rebuild function with new summary object - establish instance-class specific requirements before rebuilding
	@Override
	protected void rebuildFuncs_Indiv() {
		double mu = summary.mean(), std = summary.std();		
		gaussSclFact = (1.0/std) *normalSclFact;
		inGaussSclFactBD = new BigDecimal(1.0/gaussSclFact);
		meanStd = mu/std;
		invStdSclFact = (1.0/std) * invSqrt2;
	}//rebuildFunc
	
	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds() {
		double mu = summary.mean(), std = summary.std();
		// TODO Auto-generated method stub
		return new double[] {mu-(3.5*std), mu+(3.5*std)};
	}//getPlotValBounds
	

	
	@Override
	protected void buildFuncs() {
		errorFunc =  (x ->  ErfCoef * Math.exp(-(x*x)));
		double mu = summary.mean(), std = summary.std(), var = summary.var();
		//actual probablity functions
		funcs[fIDX] 		= (x -> (gaussSclFact  * Math.exp(-0.5 * ((x-mu)*(x-mu))/var)));
		funcs[fInvIDX] 		= (xinv -> (std*Math.sqrt(-2.0 * Math.log(xinv/gaussSclFact))) + meanStd);
		//zigurat uses standardized functions -> want pure normal distribution 
		funcs[fStdIDX]		= (x -> Math.exp(-0.5 *(x*x)));
		funcs[fInvStdIDX]	= (xinv -> (Math.sqrt(-2.0 * Math.log(xinv)))); 
		
		//derivative functions		
		funcs[fDerivIDX]	= (x -> (-(x-mu)/var) * (gaussSclFact  * Math.exp(-0.5 * ((x-mu)*(x-mu))/var)));	
		funcs[fStdDeriveIDX] = (x -> (-x * Math.exp(-0.5 *(x*x))));	
		//integrals
		integrals[fIntegIDX] = (x -> integral_f(x[0],x[1]));
		integrals[fStdIntegIDX] = (x -> integral_fStd(x[0],x[1]));
		
	}//buildFuncs
	
	//shift by mean, multiply by std
	@Override
	public double processResValByMmnts(double val) {	return summary.normToGaussTransform(val);}//public abstract double processResValByMmnts(double val);

	//calculate integral of f from x1 to x2.  Use to calculate cumulative distribution by making x1== -inf, and x2 definite val
	@Override
	public double integral_f(Double x1, Double x2) {
		double res = 0;
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myGaussianFunc", "integral_f", "No quadrature solver has been set, so cannot integrate f",true);return res;}
		//expMgr.dispMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2);
		
		//if x1 is -inf... gauss-legendre quad - use error function via gaussian quad - calculating cdf
		if(x1==Double.NEGATIVE_INFINITY) {				//cdf of x2 == .5 + .5 * error function x2/sqrt(2) 
			//expMgr.dispMessage("myGaussianFunc", "integral_f", "CDF : x1 : "+x1 + " and  x2 : " + x2 + " Using x2");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x2 - summary.mean())*invStdSclFact);
			//cdf == .5*(1+erf(x/sqrt(2))) 
			//res = halfVal.add(halfVal.multiply(erroFuncVal));		
			res = .5 + .5 * erroFuncVal.doubleValue();			
		} else if (x2==Double.POSITIVE_INFINITY) {		//pos inf -> this is 1- CDF == Q function
			//expMgr.dispMessage("myGaussianFunc", "integral_f", "Q func : x1 : "+x1 + " and  x2 : " + x2 + " Using x1");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x1 - summary.mean())*invStdSclFact);
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
	public double integral_fStd(Double x1, Double x2) {
		double res = 0;
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myGaussianFunc", "integral_fStd", "No quadrature solver has been set, so cannot integrate f",true);return res;}
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
			res = quadSlvr.evalIntegral(funcs[fStdIDX], x1, x2).doubleValue();
		}
		//expMgr.dispMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2 + " Res : \n" + res);
		return 1.0/normalSclFact * res;		//must have 1.0/normalSclFact to normalize integration results (i.e. scale CDF for function with p(0) == 1
		//return (1.0/gaussSclFact) *  integral_f(x1, x2);
		//return inGaussSclFactBD.multiply( integral_f(x1, x2));
	}//integral_f
	
	
	/**
	 * calculate an approximation of the probit function for a standard normal distribution
	 *	Lower tail quantile for standard normal distribution function. This function returns 
	 *	an approximation of the inverse cumulative standard normal distribution function.  
	 *		I.e., given P, it returns an approximation to the X satisfying P = Pr{Z <= X} 
	 *		where Z is a random variable from the standard normal distribution.
	 *	
	 *	The algorithm uses a minimax approximation by rational functions and the result has 
	 * 	a relative error whose absolute value is less than 1.15e-9.	
	 *  Author:      Peter J. Acklam
	 *  
	 * @param p probability
	 * @return value for which, using N(0,1), the p(x<= value) == p 
	 */
	protected static double calcProbitApprox(double p){	
		//p can't be 0 - if 0 then return min value possible
		if (p==0) {return Double.NEGATIVE_INFINITY;} else if (p==1) {return Double.POSITIVE_INFINITY;}
	    // Coefficients in rational approximations
	    double[] a = { -3.969683028665376e+01, 2.209460984245205e+02,-2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01,  2.506628277459239e+00},
	    		b = {-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02,  6.680131188771972e+01, -1.328068155288572e+01},
	    		c = {-7.784894002430293e-03,-3.223964580411365e-01,-2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00},
	    		d = {7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00};
	    // Define break-points/tails
	    double pLow = 0.02425,pHigh = 1-pLow, oneMp = 1-p, q,r, mult = 1.0;
	    //approx for middle region : 
	    if ((pLow <= p) && (p <= pHigh)) {
	    	q = p - 0.5f;
	    	r = q * q;
	        return (((((a[0]*r + a[1])*r + a[2])*r + a[3])*r + a[4])*r + a[5])*q /(((((b[0]*r + b[1])*r + b[2])*r + b[3])*r + b[4])*r + 1);	    	
	    }
	    if (p < pLow) {
	    	q = Math.sqrt(-2*Math.log(p));
	    	mult = 1.0;
	    } else {
	    	q = Math.sqrt(-2 * Math.log(oneMp));
	    	mult = -1.0;
	    }
        return mult * (((((c[0]*q + c[1])*q + c[2])*q + c[3])*q + c[4])*q + c[5]) / ((((d[0]*q + d[1])*q + d[2])*q + d[3])*q + 1);	    
	}//calcProbitApprox
	
	
	//get CDF of passed x value for this distribution - assume this value is from actual gaussian distribution (i.e. hasn't been normalized yet)
	@Override
	public double CDF(double x) {	return integral_f(Double.NEGATIVE_INFINITY, x);	}
	//calculate inverse cdf of passed value 0->1; this is probit function, related to inverse erf
	@Override
	public double CDF_inv(double x) {	
		double normRes = calcProbitApprox(x);
		//System.out.print("Raw probit val : " + normRes + " : ");
		return summary.normToGaussTransform(normRes);
	}//CDF_inv

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
	public myNormalFunc(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr) {
		super(_expMgr,_quadSlvr, new myProbSummary(new double[] {0.0, 1.0},2), "Normal");			
	}//ctor	
	
	//if this is a normal function, then these will not change
	@Override
	public double processResValByMmnts(double val) {	return val;}//public abstract double processResValByMmnts(double val);
	@Override
	public double CDF_inv(double x) {	
		double normRes = calcProbitApprox(x);
		//System.out.print("Raw probit val : " + normRes + " : ");
		return normRes;
	}//CDF_inv

}//class myNormalFunc


/**
 * instancing class for a univariate fleishman polynomial-based distribution function
 * 
 * When given moments of data, can build polynomial
 * @author john
 *
 */
class myFleishFunc_Uni extends myRandVarFunc{
	//polynomial coefficients
	private double[] coeffs;
	//whether this is ready to use or not - all values have been set and calculated
	private boolean ready;
	//maximum iterations
	private final int maxIter = 35;

	//summary object/

	//normal distribution for inverse calc
	private myNormalFunc normFunc;
	public myFleishFunc_Uni(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj, String _name) {
		super(_expMgr, _quadSlvr,_summaryObj, _name);		
	}//ctor

	@Override
	protected void rebuildFuncs_Indiv() {
		normFunc = new myNormalFunc(expMgr, quadSlvr);
		ready = false;
		coeffs = calcCoeffs();
		//set summary builds functions - need to specify required elements before it is called
	}//rebuildFunc
	
	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds() {
		double mu = summary.mean(), std = summary.std();
		// TODO Auto-generated method stub
		return new double[] {mu-(3.5*std), mu+(3.5*std)};
	}//getPlotValBounds
	

	
	//calculate the coefficients for the fleishman polynomial considering the given skew and excess kurtosis specified in summary object
	//will generate data with mean ==0 and std == 1; if ex kurtosis lies outside of feasible region will return all 0's for coefficients
	private double[] calcCoeffs() {		
		double[] coeffs = new double[summary.numMmntsGiven];
		double exKurt = summary.exKurt(), skew = summary.skew(), skewSQ = skew*skew;
        //first verify exkurt lies within feasible bound vs skew
        //this is fleish's bound from his paper - said to be wrong in subsequent 2010 paper
        //bound = -1.13168 + 1.58837 * skew**2
        double bound = -1.2264489 + 1.6410373*skewSQ;
        if (exKurt < bound) { 
        	expMgr.dispMessage("myFleishFunc_Uni", "calcCoeffs", "!!!! Coefficient error : ex kurt : " + String.format("%3.8f",exKurt)+ " is not feasible with skew :" + String.format("%3.8f",skew) +" | forcing exKurt to lower bound @ skew "+String.format("%3.8f",bound)+" DANGER this is not going to reflect the sample quantities",true);
        	summary.forceExKurt(bound);
        	exKurt = summary.exKurt();
        }
        double exKurtSq = exKurt * exKurt;
        //add -coeff[1] as coeff[0]
        //initial coeff estimates
        double c1 = 0.95357 - (0.05679*skew) + (0.03520*skewSQ) + (0.00133*exKurtSq);
        double c2 = (0.10007*skew) + (0.00844*skewSQ*skew);
        double c3 = 0.30978 - (0.31655 * c1);

        //solve with newton-raphson
        double[] tmpC = newton(c1, c2, c3, skew, exKurt);
		coeffs = new double[] {-tmpC[1], tmpC[0],tmpC[1],tmpC[2]};
		expMgr.dispMessage("myFleishFunc_Uni", "calcCoeffs", "Coeffs calculated :  ["+ String.format("%3.8f",coeffs[0])+","+ String.format("%3.8f",coeffs[1])+","+ String.format("%3.8f",coeffs[2])+","+ String.format("%3.8f",coeffs[3])+"]",true);		
		return coeffs;
	}//calcCoeffs
	
	//Given the fleishman coefficients, and a target skew and kurtois
    //this function will have a root if the coefficients give the desired skew and kurtosis
    //F = -c + bZ + cZ^2 + dZ^3, where Z ~ N(0,1)
	private DoubleMatrix flfunc(double b, double c, double d, double skew, double exKurt) {
        double b2 = b*b, c2 = c*c, d2 = d*d, bd = b*d;
        double _v = b2 + 6*bd + 2*c2 + 15*d2;
        double _s = 2 * c * (b2 + 24*bd + 105*d2 + 2);
        double _k = 24 * (bd + c2 * (1 + b2 + 28*bd) + 
                    d2 * (12 + 48*bd + 141*c2 + 225*d2));
        return new DoubleMatrix(new double[] {_v - 1, _s - skew, _k - exKurt});		
	}//flfunc

	//The deriviative of the flfunc above
    //returns jacobian
	private DoubleMatrix flDeriv(double b, double c, double d) {
		double b2 = b*b, c2 = c*c, d2 = d*d, d3 = d2*d, bd = b*d;
		double df1db = 2*b + 6*d, df1dc = 4*c, df1dd = 6*b + 30*d;
		double df2db = 4*c * (b + 12*d), df2dc = 2 * (b2 + 24*bd + 105*d2 + 2);
		double df2dd = 4 * c * (12*b + 105*d);
		double df3db = 24 * (d + c2 * (2*b + 28*d) + 48 * d3);
		double df3dc = 48 * c * (1 + b2 + 28*bd + 141*d2);
		double df3dd = 24 * (b + 28*b * c2 + 2 * d * (12 + 48*bd + 141*c2 + 225*d2) + d2 * (48*b + 450*d));
        return new DoubleMatrix(new double[][] {
        	{df1db, df1dc, df1dd},
        	{df2db, df2dc, df2dd},
            {df3db, df3dc, df3dd}});
	}//flDeriv

	
	//simple newton method solver
	private double[] newton(double a,double b,double c,double skew,double exKurtosis) {
        //Implements newtons method to find a root of flfunc
		DoubleMatrix f = flfunc(a, b, c, skew, exKurtosis), delta;
        DoubleMatrix Jacob;
        int i = 0;
        for (i=0; i<maxIter; ++i) {
            if (isNewtonDone(f)){          break;   }
            //get jacobian
            Jacob = flDeriv(a, b, c);
            //find delta amt
            delta = (Solve.solve(Jacob, f));
            a -= delta.data[0];
            b -= delta.data[1];
            c -= delta.data[2];
            f = flfunc(a, b, c, skew, exKurtosis);
        }
        System.out.println("newton iters to find coeffs : " + i);
        return new double[] {a,b,c};
	}//newton
	
	
	//find functional inverse - given specific y value, find x such that y = func(x) -> x = func^-1(y)
	//i.e. find x value that will give f(x)==y - note y should not be shifted by mean and std
	public double calcInvF(double y) {
		double res = y-10, diff, diffSq, fRes = 0, dfRes = 0;
		double convLimSq = convLim*convLim;
		//use newton method to find value
		int i = 0;
		int maxInvIter = 100;
		for (i=0; i<maxInvIter; ++i) {
			fRes = f(res);
			//dfRes = fDeriv(res);
			diff = (fRes - y);
			diffSq = diff * diff;
			if (diffSq < convLimSq) {          break;   }
			//System.out.println("iter " + i + " diff : " + String.format("%3.8f", diff) + "\t y :"+ y + " res : " + String.format("%3.8f", res) + " f(res) : "+ String.format("%3.8f", fRes) );//+ "\t f'(res) : " + String.format("%3.8f", dfRes));
			res += .2f *diff;
		}	
		System.out.println("myFleishFunc_Uni::calcInvF : iters to find inverse : " + i + " result : " + res + " y : " + y + " f(res) : "+ fRes);
		return res;
		
	}//calcInvF
	
	//this takes a normal input, not a uniform input - TODO change this to take uniform input (?)
	@Override
	protected void buildFuncs() {
		double mu = summary.mean(), std = summary.std();//, var = summary.var();
		//actual functions
		funcs[fIDX] 		= x ->  {return ((coeffs[0] + x*(coeffs[1] +x*(coeffs[2]+ x*coeffs[3])))*std +  mu);};
		//TODO find inverse of polynomial?  inverse of this function may not exist
		funcs[fInvIDX] 		= (xinv -> xinv);
		//zigurat functions -> want pure normal distribution
		funcs[fStdIDX]		= x -> {return (coeffs[0] + x*(coeffs[1] +x*(coeffs[2]+ x*coeffs[3])));};
		funcs[fInvStdIDX]	= (xinv -> xinv);
		//analytical derivatives
		funcs[fDerivIDX]	= x -> {return ((coeffs[1] +x*(coeffs[2]+ x*coeffs[3]))*std +  mu);};
		funcs[fStdDeriveIDX] = x -> {return (coeffs[1] +x*(coeffs[2]+ x*coeffs[3]));};                                                                   ;
		//integrals
		integrals[fIntegIDX] = (x -> integral_f(x[0],x[1]));
		integrals[fStdIntegIDX] = (x -> integral_fStd(x[0],x[1]));
		
	}//buildFuncs
	
	//find cumulative value 
	@Override
	public double CDF(double x) {	
		//must find underlying normal draw x' that yielded x once fed into fleishman polynomial
		
		//must find t @ f(t) where f is normal fed to poly, and f(t)==x.  in other words, need to know what t will give f(t) == x
		double xPrime = calcInvF(x);
		//find polynomial 
		//now we can find the CDF of transformed x by finding normFunc cdf of t and transforming it
		return normFunc.CDF(xPrime);
		
	}	//need to find most negative value of function corresponding to 0 probability => coeffs[0] 

	@Override
	public double CDF_inv(double prob) {
		//t is value of underlying normal that has given probability (p(x<=t) == prob)
		double t = normFunc.CDF_inv(prob);
		//this will be
		return f(t);
	}
	
	//evaluate integral
	@Override
	protected double integral_f(Double x1, Double x2) {
		//definite integral of polynomial along with normal pdf - should we use gauss-hermite? only for infinite	
		double res = 0; 
		
		
		if(x1==Double.NEGATIVE_INFINITY) {				//cdf of x2 == .5 + .5 * error function x2/sqrt(2) 
			
//			//expMgr.dispMessage("myGaussianFunc", "integral_f", "CDF : x1 : "+x1 + " and  x2 : " + x2 + " Using x2");
//			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x2 - summary.mean())*invStdSclFact);
//			//cdf == .5*(1+erf(x/sqrt(2))) 
//			//res = halfVal.add(halfVal.multiply(erroFuncVal));		
//			res = .5 + .5 * erroFuncVal.doubleValue();			
		} else if (x2==Double.POSITIVE_INFINITY) {		//pos inf -> this is 1- CDF == Q function
//			//expMgr.dispMessage("myGaussianFunc", "integral_f", "Q func : x1 : "+x1 + " and  x2 : " + x2 + " Using x1");
//			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x1 - summary.mean())*invStdSclFact);
//			//Q function is == 1 - (.5*(1+erf(x/sqrt(2))))
//			//res = BigDecimal.ONE.subtract(halfVal.add(halfVal.multiply(erroFuncVal)));		
//			res = 1.0 - (.5 + .5 * erroFuncVal.doubleValue());				
		} else {
			//find integral of f(normFunc.f_inv(x2)) - f(normFunc.f_inv(x1)) 
			//double
			
			//res = quadSlvr.evalIntegral(funcs[fIDX], x1, x2).doubleValue();
		}
		
		return res;
	}

	@Override
	protected double integral_fStd(Double x1, Double x2) {
		double res = 0;

		return res;
	}

	@Override
	public double processResValByMmnts(double val) {	return summary.normToGaussTransform(val);}//public abstract double processResValByMmnts(double val);	

}//class myFleishFunc

//class to model a pdf via a cosine
//mean is phase, std is function of frequency
class myCosFunc extends myRandVarFunc{
	//constants to modify cosine so that we represent desired moment behavior
	//area under pdf corresponding to x val @ 0,1,2,3 std from mean - used to determine appropriate frequency values
	private static final double[] stdAreaAra = new double[] {0.0,0.3413447460685429485852 ,0.4772498680518207927997 , 0.4986501019683699054734};
	//don't set this to 0!
	private static final int stdFreqMultToUse = 3;
	
	//frequency == 1/period; needs to be calculated so that stdArea is under curve from mean -> x @ 1 std - cos(x) has freq 1/2pi, so this value is actually 2pi*freq
	//xBnds == x value where function == 0 -> corresponds to +Pi for freq = 1
	private double freqMult, xBnd, actLBnd, actUBnd;
	//for standardized functions
	private static double freq1StdMult = -1, halfAmpl1Std, xBnd1Std;
	//half amplitude - needs to be freq/2pi; 
	//amplitude to maintain area == 1 under 1 period of cosine is actually freq/pi but we are using .5 + .5*cos, so make calc easier to use freq/2pi * ( 1 + cos)
	private double halfAmpl;

	public myCosFunc(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj) {
		super(_expMgr, _quadSlvr, _summaryObj, "Cosine PDF");
		if(freq1StdMult == -1) {
			freq1StdMult = calcFreq(1.0);
			halfAmpl1Std = freq1StdMult/twoPi;
			xBnd1Std = Math.PI/freq1StdMult;		//values need to be between mu - xBnd and mu + xBnd
		}
	}
	
	//this will calculate the freq val for a given std iteratively - calculates volume to be vol
	public double calcFreq(double std) {
		//qStd is std @ stdFreqMultToUse
		double stdArea = stdAreaAra[stdFreqMultToUse], twoPiStdArea = stdArea* twoPi;
		double res = std,sinFreqS,diff, qStd = stdFreqMultToUse * std ;
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			sinFreqS = Math.sin(res * qStd);//solve CDF for std value - ignore mu
			diff = res - ((twoPiStdArea - sinFreqS)/qStd); 
			//System.out.println("iter " + i + " diff : " + String.format("%3.8f", diff) + "\t std :"+ String.format("%3.8f", std) + " res : " + String.format("%3.8f", res) + " sinFreqS : "+ String.format("%3.8f", sinFreqS));
			if(Math.abs(diff) < convLim) {				done=true;			}
			res -= .2*diff;
			++i;
		}//
		//System.out.println("Final freq val : iters " + i + "\t std :"+ String.format("%3.8f", std) + " freq res : " + String.format("%3.8f", res));
		return res;
	}//calcFreq
	
	@Override
	protected void rebuildFuncs_Indiv() {
		double mu = summary.mean(), std = summary.std();//, var = summary.var();
		//setup before actual functions are built
		//TODO need to solve for freq based on area from 0->1 std being stdArea
		if(std==0) {
			freqMult = 1.0;//temp placeholder - NOT CORRECT
		} else {
			freqMult = calcFreq(std);	//solve based on std	
		}
		xBnd = Math.PI/freqMult;		//values need to be between mu - xBnd and mu + xBnd
		actLBnd = mu - xBnd;
		actUBnd = mu + xBnd;
		halfAmpl = freqMult/twoPi;		
	}//rebuildFuncs_Indiv
	
	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds() {
		double mu = summary.mean(), std = summary.std();
		// TODO Auto-generated method stub
		return new double[] {actLBnd, actUBnd};
	}//getPlotValBounds
	


	@Override
	protected void buildFuncs() {
		double mu = summary.mean();//, var = summary.var();
		//form should be freq/2pi * (1 + (cos(freq(x - mu)))); 
		//want to find appropriate freq so that area under curve [0,std] == 0.3413447460685429 		
		//actual functions
		funcs[fIDX] 		= x ->  {return (halfAmpl * (1 +  Math.cos(freqMult * (x - mu))));};
		funcs[fInvIDX] 		= xinv -> {return  (Math.acos(  (xinv/halfAmpl) - 1.0) + (freqMult*mu))/freqMult; };
//		//zigurat functions -> want 0 mean 1 std distribution
		funcs[fStdIDX]		= x -> {return (halfAmpl1Std * (1 +  Math.cos(freq1StdMult * x)));};
		funcs[fInvStdIDX]	= xinv -> {return  (Math.acos(  (xinv/halfAmpl1Std) - 1.0))/freq1StdMult; };
//		//analytical derivatives
		funcs[fDerivIDX]	= x -> {return (halfAmpl * (- freqMult * Math.sin(freqMult * x)));};    
		funcs[fStdDeriveIDX] = x -> {return (halfAmpl1Std * (- freq1StdMult * Math.sin(freq1StdMult * x)));};                                                         ;
		//integrals - solve analytically
		integrals[fIntegIDX] = x -> {return (halfAmpl * ((x[1]-x[0]) +  (Math.sin(freqMult * (x[1] - mu)) - Math.sin(freqMult * (x[0] - mu)))/freqMult));};
		integrals[fStdIntegIDX] = x -> {return (halfAmpl1Std * ((x[1]-x[0]) +  (Math.sin(freq1StdMult * x[1]) - Math.sin(freq1StdMult * x[0]))/freq1StdMult));};
	}//

	//find CDF value of x; x must be within bounds mu-xBnd to mu+xBnd
	//CDF is integral from -inf to x of pdf - can be solved analytically 
	@Override
	public double CDF(double x) {
		//expMgr.dispMessage("myRandVarFunc", "CDF", "Begin CDF calc for val : " + String.format("%3.8f", x) , true);
		double newX = forceInBounds(x,actLBnd, actUBnd);
		
		double  res = integrals[fIntegIDX].apply(new Double[] {actLBnd, newX});
		//double res = integral_f(actLBnd, x);		 
		//expMgr.dispMessage("myRandVarFunc", "CDF", "End CDF calc for val : " + String.format("%3.8f", x) , true);
		return res;
	}//CDF

	
	//given probability p find value x such that CDF(X<= x) == p
	@Override
	public double CDF_inv(double p) {
		//expMgr.dispMessage("myCosFunc", "CDF_inv", "Begin CDF_inv calc for prob : " + String.format("%3.8f", p), true);
		double res = calcInvCDF(p, integrals[fStdIntegIDX],  -xBnd1Std);		//iteratively finds inverse
		//double res = calcInvCDF(p, integrals[fIntegIDX],  actLBnd);
		//expMgr.dispMessage("myCosFunc", "CDF_inv", "Finish CDF_inv calc for prob : " + String.format("%3.8f", p) + "\t stdzd res : " + String.format("%3.8f",res)+ "\t low xBnd1Std : " + String.format("%3.8f", -xBnd1Std), true);			
		return processResValByMmnts(res);
		//return res;//processResValByMmnts(res);
	}//CDF_inv
	
	//private boolean checkInBnds(Double x, double mu) {return ((x>= mu - xBnd) && (x<= mu + xBnd));}
	
	private Double forceInBounds(Double x, double lBnd, double uBnd, boolean forceToBnd) {		
		if(forceToBnd) {
			return (x < lBnd ? lBnd : x > uBnd ? uBnd : x);
		} else {
			double pd =  uBnd - lBnd;//period is 2x bound
			if(x < lBnd) {				do {	x += pd;} while (x < lBnd);	} 
			else if(x > uBnd) {			do {	x -= pd;} while (x > uBnd);	} 
			return x;	
		}
	}//forceInBounds
	private Double forceInBounds(Double x, double lBnd, double uBnd) {return forceInBounds(x, lBnd, uBnd, true);}
	@Override
	protected double integral_f(Double x1, Double x2) {
		//expMgr.dispMessage("myRandVarFunc", "integral_f", "Begin integral_f calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2) , true);
		if(x1 == Double.NEGATIVE_INFINITY) {x1 = actLBnd;}
		if(x2 == Double.POSITIVE_INFINITY) {x2 = actUBnd;}
		
		double newX1 = forceInBounds(x1,actLBnd, actUBnd);
		double newX2 = forceInBounds(x2,actLBnd, actUBnd); 
		
		double  resEval = integrals[fIntegIDX].apply(new Double[] {newX1, newX2});
		//double res = quadSlvr.evalIntegral(funcs[fIDX], newX1, newX2).doubleValue();
		//expMgr.dispMessage("myRandVarFunc", "integral_f", "End integral_f calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2)+ " : res = " +  String.format("%3.8f", quadSlvr.evalIntegral(funcs[fIDX], newX1, newX2).doubleValue()) + " Analytic eval : " +  String.format("%3.8f", resEval) , true);
		return resEval;
	}

	@Override
	protected double integral_fStd(Double x1, Double x2) {
		//expMgr.dispMessage("myRandVarFunc", "integral_fStd", "Begin integral_fStd calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2) , true);
		if(x1 == Double.NEGATIVE_INFINITY) {x1 = -xBnd1Std;}
		if(x2 == Double.POSITIVE_INFINITY) {x2 = xBnd1Std;}		
		double newX1 = forceInBounds(x1,-xBnd1Std, xBnd1Std);
		double newX2 = forceInBounds(x2,-xBnd1Std, xBnd1Std); 	
		
		//expMgr.dispMessage("myRandVarFunc", "integral_fStd", "New Integral Bounds : " + String.format("%3.8f", newX1) +","+ String.format("%3.8f", newX2) , true);
		double resEval = integrals[fStdIntegIDX].apply(new Double[] {newX1, newX2});
		//double res = quadSlvr.evalIntegral(funcs[fStdIDX], newX1, newX2).doubleValue(); 
		//expMgr.dispMessage("myRandVarFunc", "integral_fStd", "End integral_fStd calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2)+ " : res = " +  String.format("%3.8f", quadSlvr.evalIntegral(funcs[fStdIDX], newX1, newX2).doubleValue()) + " Analytic eval : " +  String.format("%3.8f", resEval) , true);
		return resEval;
	}

	@Override
	//assmue we can modify value in similar way to transform by 1st 2 moments
	public double processResValByMmnts(double val) {	return summary.normToGaussTransform(val);}//public abstract double processResValByMmnts(double val);

}//myCosFunc

/**
 * this function will build a pdf/cdf model based on working backward from samples - building cdf from sample data, fitting sin-based CDF function to data, then differentiating to find pdf
 * @author john
 */
class myCosFuncFromCDF extends myRandVarFunc{


	public myCosFuncFromCDF(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj) {
		super(_expMgr, _quadSlvr, _summaryObj, "Cosine PDF");
	}//ctor

	
	private double calcF_Theta(DoubleMatrix Theta, double x) {
		return Theta.get(0) * (Math.sin(Theta.get(1) * x + Theta.get(2))) + Theta.get(3);
	}
	
	private DoubleMatrix calcRVal(DoubleMatrix yVals, DoubleMatrix xVals, DoubleMatrix Theta) {
		DoubleMatrix fXVals =DoubleMatrix.zeros(xVals.getRows(), 1);
		for (int i=0;i<xVals.getRows();++i) {	fXVals.put(i, calcF_Theta(Theta,xVals.get(i)));	}		
		return yVals.sub(fXVals);
	}
	
	//get deriv of func (sin) w/respect to each element of theta
	private DoubleMatrix getDerivTheta(double x, DoubleMatrix Theta) {
		double bXplusC = Theta.get(1) * x + Theta.get(2),
		A = Theta.get(0),
		cosVal = Math.cos(bXplusC),
		dA = Math.sin(bXplusC),  //sin(B * val + C)
		dB = A*x * cosVal, //A * val * cos(B * val + C) 
		dC = A * cosVal, //A * cos(B * val + C)
		dD = 1;
		//System.out.println("x : " + x +" | dA : " + dA + " dB : " + dB +" dC : " + dC + " dD :"+ dD);
		return new DoubleMatrix(new double[] { dA, dB, dC, dD});
	}//getDerivTheta
	
	private DoubleMatrix calcJacobian(DoubleMatrix xVals, DoubleMatrix Theta) {
		DoubleMatrix J = new DoubleMatrix(xVals.length, Theta.length);
		//System.out.println("xVals.getRows() : " +xVals.getRows());
		for (int i=0;i<xVals.getRows();++i) {		J.putRow(i, getDerivTheta(xVals.get(i),Theta));	}

		return J;
	}
	
	private void printXYVals(DoubleMatrix xVals) {		System.out.println("xVals : " + xVals);	}
	
	@Override
	protected void rebuildFuncs_Indiv() {
		int numIters = 100;
		//get CDF map of data  where key is p(X<=x) and value is x
		TreeMap<Double,Double> CDFMap = new TreeMap<Double,Double>();
		try {
			CDFMap = summary.getCDFOfData();
			if(CDFMap.size() < 2) {
				System.out.println("CDFMap.size() < 2 : " +CDFMap.size());
				throw new Exception();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
//		This is getting called with only 1 variable point from : 
//		at graphProbExp_PKG.myCosFuncFromCDF.rebuildFuncs_Indiv(myRandVarFunc.java:899)
//		at graphProbExp_PKG.myRandVarFunc.rebuildFuncs(myRandVarFunc.java:91)
//		at graphProbExp_PKG.myRandGen.setFuncSummary(myRandGen.java:54)
//		at graphProbExp_PKG.myFinalGradeRoster.calcTotalGrades(mySampleSet.java:427)
//		at graphProbExp_PKG.myClassRoster.updateFinalGrades(mySampleSet.java:217)
//		at graphProbExp_PKG.myClassRoster.transformStudentFromRawToUni(mySampleSet.java:205)
//		at graphProbExp_PKG.myClassRoster.transformStudentGradesToUniform(mySampleSet.java:187)
//		at graphProbExp_PKG.ClassGradeExperiment.setRandGensAndTransformGrades(ClassGradeExperiment.java:186)
//		at graphProbExp_PKG.ClassGradeExperiment.buildClassGradeExperiment(ClassGradeExperiment.java:138)
//		at graphProbExp_PKG.Grade2DWindow.setGradeExp(Grade2DWindow.java:120)
//		at graphProbExp_PKG.Grade2DWindow.setUIWinVals(Grade2DWindow.java:289)
//		at graphProbExp_PKG.myDispWindow.handleMouseRelease(myDispWindow.java:1111)
//		at graphProbExp_PKG.GraphProbExpMain.mouseReleased(GraphProbExpMain.java:289)
//		at processing.core.PApplet.mouseReleased(PApplet.java:2780)		
		//catch (Exception e) {			return;		}
		double alpha = .01;//learning rate
		DoubleMatrix rVals = new DoubleMatrix(CDFMap.size(), 1),rSqVals = new DoubleMatrix(CDFMap.size(), 1), yVals= new DoubleMatrix(CDFMap.size(), 1), xVals= new DoubleMatrix(CDFMap.size(), 1);
		DoubleMatrix Theta = new DoubleMatrix(new double[] {.5,.5,.5,.5}), dTheta, oldDTheta = new DoubleMatrix(new double[] {1,1,1,1});
		int row = 0;
		for(Double key : CDFMap.keySet()) {
			yVals.put(row, key);
			xVals.put(row, CDFMap.get(key));
			
			++row;
		}
		//printXYVals(xVals);
		//Theta == [A,B,C,D]^T
		//dTheta == [J^T * J]^-1 * J^T * r
		//newTheta = Theta + alpha * dTheta
		//r vals
		//jacobian
		boolean done = false;
		int iter = 0;
		while ((!done) && (iter < numIters)){
			//calc R Value - yVal - f(theta,xVal)
			rVals = calcRVal(yVals, xVals, Theta);
			//calculate Jacobians for each point
			DoubleMatrix J = calcJacobian(xVals, Theta);
			DoubleMatrix tmpVal = J.transpose().mmul(J);
			DoubleMatrix tmpVal2 = Solve.pinv(tmpVal).mmul(J.transpose());
			//System.out.println("iter : " + iter + " rVals : " +rVals.toString() +" rows : " + rVals.getRows()+ " J numrows : " + J.getRows() + " | J : " +J.toString() +" \ntmpVal : " +tmpVal.toString() +" \n tmpVal2 : " +tmpVal2.toString() +" \n tmpVal2 rows : " +tmpVal2.getRows()+ " \n ");
			//printXYVals(xVals);
			
			dTheta = tmpVal2.mmul(rVals);
			if (isNewtonDone(dTheta.sub(oldDTheta))) {
				done = true;
			}
			oldDTheta = dTheta;
			Theta.addi(dTheta.mul(alpha));
			//modify Theta
			//System.out.println("Final Iters :  " + iter + " Theta values : A : " + Theta.get(0) + " |B : " + Theta.get(1) + " |C : " + Theta.get(2) + " |D : " + Theta.get(3)+ "\t| dTheta values : A : " + dTheta.get(0) + " |B : " + dTheta.get(1) + " |C : " + dTheta.get(2) + " |D : " + dTheta.get(3) );
			
			++iter;
		}//iterative loop
		System.out.println("# vals : " +CDFMap.size() +" | Final Iters :  " + iter + " Theta values : A : " + Theta.get(0) + " |B : " + Theta.get(1) + " |C : " + Theta.get(2) + " |D : " + Theta.get(3) );
		
		
	}//rebuildFuncs_Indiv

	@Override
	protected void buildFuncs() {
		double mu = summary.mean();//, var = summary.var();
		//form should be freq/2pi * (1 + (cos(freq(x - mu)))); 
		//want to find appropriate freq so that area under curve [0,std] == 0.3413447460685429 		
		//actual functions
//		funcs[fIDX] 		= x ->  {return (halfAmpl * (1 +  Math.cos(freqMult * (x - mu))));};
//		funcs[fInvIDX] 		= xinv -> {return  (Math.acos(  (xinv/halfAmpl) - 1.0) + (freqMult*mu))/freqMult; };
////		//zigurat functions -> want 0 mean 1 std distribution
//		funcs[fStdIDX]		= x -> {return (halfAmpl1Std * (1 +  Math.cos(freq1StdMult * x)));};
//		funcs[fInvStdIDX]	= xinv -> {return  (Math.acos(  (xinv/halfAmpl1Std) - 1.0))/freq1StdMult; };
////		//analytical derivatives
//		funcs[fDerivIDX]	= x -> {return (halfAmpl * (- freqMult * Math.sin(freqMult * x)));};    
//		funcs[fStdDeriveIDX] = x -> {return (halfAmpl1Std * (- freq1StdMult * Math.sin(freq1StdMult * x)));};                                                         ;
//		//integrals - solve analytically
//		integrals[fIntegIDX] = x -> {return (halfAmpl * ((x[1]-x[0]) +  (Math.sin(freqMult * (x[1] - mu)) - Math.sin(freqMult * (x[0] - mu)))/freqMult));};
//		integrals[fStdIntegIDX] = x -> {return (halfAmpl1Std * ((x[1]-x[0]) +  (Math.sin(freq1StdMult * x[1]) - Math.sin(freq1StdMult * x[0]))/freq1StdMult));};
	}//
	
	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds() {
		double mu = summary.mean(), std = summary.std();
		// TODO Auto-generated method stub
		return new double[] {mu-(3.5*std), mu+(3.5*std)};
	}//getPlotValBounds
	


	//find CDF value of x; x must be within bounds mu-xBnd to mu+xBnd
	//CDF is integral from -inf to x of pdf - can be solved analytically 
	@Override
	public double CDF(double x) {
		//expMgr.dispMessage("myRandVarFunc", "CDF", "Begin CDF calc for val : " + String.format("%3.8f", x) , true);
		double res = 0;//integral_f(actLBnd, x);		 
		//expMgr.dispMessage("myRandVarFunc", "CDF", "End CDF calc for val : " + String.format("%3.8f", x) , true);
		return res;
	}//CDF
	
	//find inverse STD value -> x value such that CDF(X<= x) == p
	public double calcInvCDF(double p, double a, double freq, double lbnd, double mu) {
		double xVal = 0, calcPVal = 0, diff;
		double lBndVal = a*(lbnd + Math.sin(freq*lbnd)/freq);
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			calcPVal = a*(xVal + Math.sin(freq*(xVal-mu))/freq) - lBndVal;//solve for std value - ignore mu
			diff = p - calcPVal;
			//System.out.println("iter " + i + " diff : " + String.format("%3.8f", diff) + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " sinFreqS : "+ String.format("%3.8f", calcPVal));
			if(Math.abs(diff) < convLim) {				done=true;			}
			xVal += .2*diff;
			++i;
		}//
		//System.out.println("Final InvCDF val : iters " + i + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " prob(xVal) : " +  String.format("%3.8f", calcPVal)+ " lbnd : " +  String.format("%3.8f", lBndVal));
		return xVal;
	}//calcInvCDF
	
	//given probability p find value x such that CDF(X<= x) == p
	@Override
	public double CDF_inv(double p) {
		//expMgr.dispMessage("myCosFunc", "CDF_inv", "Begin CDF_inv calc for prob : " + String.format("%3.8f", p), true);
		//double res = calcInvCDF(p, halfAmpl1Std, freq1StdMult,  -xBnd1Std, 0);
		//double res = calcInvCDF(p, halfAmpl, freqMult,  actLBnd, summary.mean());
		//expMgr.dispMessage("myCosFunc", "CDF_inv", "Finish CDF_inv calc for prob : " + String.format("%3.8f", p) + "\t stdzd res : " + String.format("%3.8f",res)+ "\t low xBnd1Std : " + String.format("%3.8f", -xBnd1Std), true);			
		return 0;//processResValByMmnts(res);
		//return res;//processResValByMmnts(res);
	}//CDF_inv
	
	//private boolean checkInBnds(Double x, double mu) {return ((x>= mu - xBnd) && (x<= mu + xBnd));}
	
	private Double forceInBounds(Double x, double lBnd, double uBnd, boolean forceToBnd) {		
		if(forceToBnd) {
			return (x < lBnd ? lBnd : x > uBnd ? uBnd : x);
		} else {
			double pd =  uBnd - lBnd;//period is 2x bound
			if(x < lBnd) {				do {	x += pd;} while (x < lBnd);	} 
			else if(x > uBnd) {			do {	x -= pd;} while (x > uBnd);	} 
			return x;	
		}
	}//forceInBounds
	private Double forceInBounds(Double x, double lBnd, double uBnd) {return forceInBounds(x, lBnd, uBnd, true);}
	@Override
	protected double integral_f(Double x1, Double x2) {
//		//expMgr.dispMessage("myRandVarFunc", "integral_f", "Begin integral_f calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2) , true);
//		if(x1 == Double.NEGATIVE_INFINITY) {x1 = actLBnd;}
//		if(x2 == Double.POSITIVE_INFINITY) {x2 = actUBnd;}
//		
//		double newX1 = forceInBounds(x1,actLBnd, actUBnd);
//		double newX2 = forceInBounds(x2,actLBnd, actUBnd); 
//		
//		double  resEval = integrals[fIntegIDX].apply(new Double[] {newX1, newX2});
//		//double res = quadSlvr.evalIntegral(funcs[fIDX], newX1, newX2).doubleValue();
		//expMgr.dispMessage("myRandVarFunc", "integral_f", "End integral_f calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2)+ " : res = " +  String.format("%3.8f", quadSlvr.evalIntegral(funcs[fIDX], newX1, newX2).doubleValue()) + " Analytic eval : " +  String.format("%3.8f", resEval) , true);
		return 0;//resEval;
	}

	@Override
	protected double integral_fStd(Double x1, Double x2) {
//		//expMgr.dispMessage("myRandVarFunc", "integral_fStd", "Begin integral_fStd calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2) , true);
//		if(x1 == Double.NEGATIVE_INFINITY) {x1 = -xBnd1Std;}
//		if(x2 == Double.POSITIVE_INFINITY) {x2 = xBnd1Std;}
//		//must only use 
//		
//		double newX1 = forceInBounds(x1,-xBnd1Std, xBnd1Std);
//		double newX2 = forceInBounds(x2,-xBnd1Std, xBnd1Std); 
//		
//		
//		//expMgr.dispMessage("myRandVarFunc", "integral_fStd", "New Integral Bounds : " + String.format("%3.8f", newX1) +","+ String.format("%3.8f", newX2) , true);
//		double resEval = integrals[fStdIntegIDX].apply(new Double[] {newX1, newX2});
//		//double res = quadSlvr.evalIntegral(funcs[fStdIDX], newX1, newX2).doubleValue(); 
		//expMgr.dispMessage("myRandVarFunc", "integral_fStd", "End integral_fStd calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2)+ " : res = " +  String.format("%3.8f", quadSlvr.evalIntegral(funcs[fStdIDX], newX1, newX2).doubleValue()) + " Analytic eval : " +  String.format("%3.8f", resEval) , true);
		return 0;//resEval;
	}

	@Override
	//assmue we can modify value in similar way to transform by 1st 2 moments
	public double processResValByMmnts(double val) {	return summary.normToGaussTransform(val);}//public abstract double processResValByMmnts(double val);

		
}//myCosFunc


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
		//func.expMgr.dispMessage("myGaussianFunc", "z_R", "Starting z_R with: rVal : " + rVal,true);
		//this gives the volume at the tail - rectangle @ r + tail from r to end		
		double funcAtR = func.fStd(rVal);  							
		//vol = rF(r) + integral(r->+inf) (f_zig(x))
		double integralRes = func.integral_fStd(rVal, Double.POSITIVE_INFINITY);
		double vol = rVal* funcAtR + integralRes;//Q func == 1 - CDF
		if (vol < 0) {
			func.expMgr.dispMessage("zigConstVals", "z_R", func.getShortDesc()+ "| Initial Ziggurat R val chosen to be too high, causing integration to yield a negative volume due to error",true);
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
			//func.expMgr.dispMessage("myGaussianFunc", "z_R", "Inverse @ i=="+i+" =  " + xVals[i]  + " f(x[i]) : " + fXVals[i] + " eval : " + eval + " Vol : " + (xVals[i]* fXVals[i]),true);
		}
		//double retVal = vol - xVals[1] - xVals[1]*fXVals[1];
		//func.expMgr.dispMessage("myGaussianFunc", "z_R", "End : Passed rval : " + rVal + " f(rVal) : " + funcAtR + " Vol : " + vol + " xVals[1] :"+ xVals[1]+ " F(x[1]) :"+fXVals[1] + " Return val : " + retVal,true);
		return new double[] {retVal, vol};
	}//z_R
	   
	//3.6541528853610088; is value found for n == 256 
	private static final double RValAndVolzTol = 0.00000000000001;
	//x coordinate of final rectangle, such that v_each == r(f(r)) + integral(r->inf) f(x) dx
	protected double[] calcRValAndVol() {
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
				//func.expMgr.dispMessage("myRandVarFunc", "calcRVal", "Name : " + func.name+ "| For " + Nrect + " rectangles, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ "  oldGuess : " + String.format("%3.18f", oldGuess)+ " Gives zVal : " + String.format("%3.18f", zValAra[0]) + " Vol : " + String.format("%3.18f", zValAra[1]),true);
				while ((oldGuess < rValGuess) && (curLearnRate > minLearnRate)) {
					//func.expMgr.dispMessage("myRandVarFunc", "calcRVal", "\tFlip : curLearnRate : " + curLearnRate + " new learn rate : " + curLearnRate/2.0);
					curLearnRate /= 2.0;
					rValGuess -= zValAra[0] * curLearnRate;	//change mod to 1/2 last mod					
				}				
			}//not close enough, modifying guess			
			//func.expMgr.dispMessage("myRandVarFunc", "calcRVal", "Name : " + func.name+ "| For " + Nrect + " rectangles, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ " Gives zVal : " + String.format("%3.18f", zValAra[0]) + " Vol : " + String.format("%3.18f", zValAra[1]),true);
		}//while
		double[] res = new double[2];
		if(done) {
			res[0] = rValGuess;
			res[1] = zValAra[1];
		}
		//func.expMgr.dispMessage("zigConstVals", "calcRValAndVol",  func.getShortDesc()+ " | Done w/ " + Nrect + " rects, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ " Gives Vol : " + String.format("%3.18f", zValAra[1]),true);
		return res;
	}//calcRVal
	
	
	//return important values for this ziggurat const struct
	public String toString() {
		String res = "Owning Func : " +func.getShortDesc()+" # Rects : " + Nrect + " R_Last : " + String.format("%3.16f", R_last) + " | Vol Per Zig : " + String.format("%3.16f", V_each) + "\n";
		return res;	
	}
	
}//class zigConstVals

