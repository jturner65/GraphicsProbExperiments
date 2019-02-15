package graphProbExp_PKG;

import java.math.*;
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
		//functions specifically for ziggurat calc - expected to be 0-centered
		fZigIDX			= 2,
		fInvZigIDX		= 3,
		//integral functions, for closed form integrals
		fIntegIDX 		= 4,
		fZigIntegIDX 	= 5;
	protected static final int numFuncs = 6;
	
	//object used to perform ziggurat calcs for a particular function - contains pre-calced arrays
	//each instancing class will have a static map of these, and only build a new one if called for
	public zigConstVals zigVals;
	public int numZigRects = 256;
	
	//////////////////////////////////
	///useful constants
    //scale factor for normal N(0,1)
	protected static double invSqrt2 = 1.0/Math.sqrt(2.0),
							ln2 = Math.log(2.0);
	
	public myRandVarFunc(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, String _name) {
		expMgr = _expMgr;name=_name;
		initFlags();
		setQuadSolver(_quadSlvr);
	}//ctor
	
	public abstract void rebuildFunc(myProbSummary _summary);
	
	
	//set new summary statistics for this function and rebuild functions 
	protected void setSummary(myProbSummary _summary) {
		summary=_summary;
		funcs= new Function[numFuncs];
		buildFuncs();
	}
		
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
	
	//build individual functions that describe pdf, inverse pdf and zigggurat (scaled to y==1 @ x==0) pdf and inv pdf, if necesssary
	protected abstract void buildFuncs();
	//calculate pdf function f
	public final double f(double x) {return funcs[fIDX].apply(x);}
	//calculate the inverse of f
	public final double f_inv(double xInv){return funcs[fInvIDX].apply(xInv);}	
	//calculate f normalized for ziggurate method, so that f(0) == 1;
	public final double fZig(double x){return funcs[fZigIDX].apply(x);}
	//calculate the inverse of f
	public final double f_invZig(double xInv){return funcs[fInvZigIDX].apply(xInv);}
	
	//calculate the cdf
	public abstract double CDF(double x);
	//calculate inverse cdf of passed value 0->1
	public abstract double CDF_inv(double x);	
	
	//calculate integral of f between x1 and x2.  Use to calculate cumulative distribution by making x1==-inf, and x2 definite; qfunc by setting x1 to a value and x2 == +inf
	protected abstract double integral_f(Double x1, Double x2);	
	//calculate integral of normalized f (for ziggurat calc) between x1 and x2.  Use to calculate cumulative distribution by making x1==-inf, and x2 definite; qfunc by setting x1 to a value and x2 == +inf
	protected abstract double integral_fZig(Double x1, Double x2);	
		
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
		super(_expMgr,_quadSlvr, _name);
		rebuildFunc(_summaryObj);
	}//ctor
	public myGaussianFunc(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj) {this(_expMgr, _quadSlvr,  _summaryObj, "Gaussian");}
	//rebuild function with new summary object
	@Override
	public void rebuildFunc(myProbSummary _summaryObj) {
		double mu = _summaryObj.mean(), std = _summaryObj.std();
		
		gaussSclFact = (1.0/std) *normalSclFact;
		inGaussSclFactBD = new BigDecimal(1.0/gaussSclFact);
		meanStd = mu/std;
		invStdSclFact = (1.0/std) * invSqrt2;
		//System.out.println("Mean : " + _mean + " std "+ _std + "| invStdSclFact : " +invStdSclFact);
		setSummary(_summaryObj);
	}//rebuildFunc
	
	@Override
	protected void buildFuncs() {
		errorFunc =  (x ->  ErfCoef * Math.exp(-(x*x)));
		double mu = summary.mean(), std = summary.std(), var = summary.var();
		//actual probablity functions
		funcs[fIDX] 		= (x -> (gaussSclFact  * Math.exp(-0.5 * ((x-mu)*(x-mu))/var)));
		funcs[fInvIDX] 		= (xinv -> (std*Math.sqrt(-2.0 * Math.log(xinv/gaussSclFact))) + meanStd);
		//zigurat functions -> want pure normal distribution
		funcs[fZigIDX]		= (x -> Math.exp(-0.5 *(x*x)));
		funcs[fInvZigIDX]	= (xinv -> (Math.sqrt(-2.0 * Math.log(xinv)))); 
		// no closed form integrals exist, so have to use quadrature
		funcs[fIntegIDX]		= (x -> x);
		funcs[fZigIntegIDX]		= (x -> x);
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
	public double integral_fZig(Double x1, Double x2) {
		double res = 0;
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myGaussianFunc", "integral_fZig", "No quadrature solver has been set, so cannot integrate f",true);return res;}
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
	
	
	//get CDF of passed x value for this distribution
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
	//convergence limit
	private final double convLim=1e-5;
	//summary object/
	public myFleishFunc_Uni(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj, String _name) {
		super(_expMgr, _quadSlvr, _name);
		rebuildFunc(_summaryObj);
	}//ctor

	@Override
	public void rebuildFunc(myProbSummary _summaryObj) {
		// TODO Auto-generated method stub
		ready = false;
		coeffs = calcCoeffs(_summaryObj);
		//set summary builds functions - need to specify required elements before it is called
		setSummary(_summaryObj);

	}
	
	
	//calculate the coefficients for the fleishman polynomial considering the given skew and excess kurtosis specified in summary object
	//will generate data with mean ==0 and std == 1; if ex kurtosis lies outside of feasible region will return all 0's for coefficients
	private double[] calcCoeffs(myProbSummary _summary) {		
		double[] coeffs = new double[_summary.numMmntsGiven];
		double exKurt = _summary.exKurt(), skew = _summary.skew(), skewSQ = skew*skew;
        //first verify exkurt lies within feasible bound vs skew
        //this is fleish's bound from his paper - said to be wrong in subsequent 2010 paper
        //bound = -1.13168 + 1.58837 * skew**2
        double bound = -1.2264489 + 1.6410373*skewSQ;
        if (exKurt < bound) { 
        	expMgr.dispMessage("myFleishFunc_Uni", "calcCoeffs", "!!!! Coefficient error : ex kurt : " + exKurt+ " is not feasible with skew :" + skew +" | forcing exKurt to lower bound @ skew DANGER this is not going to reflect the sample quantities :"+bound,true);
        	_summary.forceExKurt(bound);
        	exKurt = _summary.exKurt();
        }
        double exKurtSq = exKurt * exKurt;
        //add -coeff[1] as coeff[0]
        double c1 = 0.95357 - (0.05679*skew) + (0.03520*skewSQ) + (0.00133*exKurtSq);
        double c2 = (0.10007*skew) + (0.00844*skewSQ*skew);
        double c3 = 0.30978 - (0.31655 * c1);


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
	
	private boolean isNewtonDone(DoubleMatrix f) {
		for(double x : f.data) {
			if(Math.abs(x) < convLim) {return true;}
		}		
		return false;
	}
	//simple newton method solver
	private double[] newton(double a,double b,double c,double skew,double exKurtosis) {
        //Implements newtons method to find a root of flfunc
		DoubleMatrix f = flfunc(a, b, c, skew, exKurtosis), delta;
        DoubleMatrix Jacob;
        for (int i=0; i<maxIter; ++i) {
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
        return new double[] {a,b,c};
	}//newton
	
	//this takes a normal input, not a uniform input
	@Override
	protected void buildFuncs() {
		double mu = summary.mean(), std = summary.std();//, var = summary.var();
		//actual functions
		funcs[fIDX] 		= (x ->  ((coeffs[0] + x*(coeffs[1] +x*(coeffs[2]+ x*coeffs[3])))*std +  mu));
		//TODO find inverse of polynomial?  inverse of this function may not exist
		funcs[fInvIDX] 		= (xinv -> xinv);
		//zigurat functions -> want pure normal distribution
		funcs[fZigIDX]		= (x -> (coeffs[0] + x*(coeffs[1] +x*(coeffs[2]+ x*coeffs[3]))));
		funcs[fInvZigIDX]	= (xinv -> xinv);
		//easily integrated since we have coefficients of polynomial - should always be used in definite integral over a span, so coefficient will cancel
		//TODO need to verify this - should integral be of full equation or just underlying polynomial
		funcs[fIntegIDX]		= (x -> (((x*coeffs[0] + .5*x*x*coeffs[1] + (1.0/3.0)*x*x*x*coeffs[2]+ .25*x*x*x*x*coeffs[3])*std) +  mu));
		funcs[fZigIntegIDX]		= (x -> (x*coeffs[0] + .5*x*x*coeffs[1] + (1.0/3.0)*x*x*x*coeffs[2]+ .25*x*x*x*x*coeffs[3]));

	}//buildFuncs

	@Override
	public double CDF(double x) {	
		//this function is fed by normal distribution -> this means that bound of neg inf into source distribution == this being fed by 0 as lower bound
		//upper bound is inv_cdf of normal dist of original x
		return integral_f(Double.NEGATIVE_INFINITY, x);	
	}	//need to find most negative value of function corresponding to 0 probability => coeffs[0] 

	@Override
	public double CDF_inv(double x) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	//evaluate integral
	@Override
	protected double integral_f(Double x1, Double x2) {
		//definite integral of polynomial		
		double res; 
		//res = quadSlvr.evalIntegral(funcs[fIDX], x1, x2).doubleValue();
		res = funcs[fIntegIDX].apply(x2) - funcs[fIntegIDX].apply(x1);
		
		return res;
	}

	@Override
	protected double integral_fZig(Double x1, Double x2) {
		double res;
		//res = quadSlvr.evalIntegral(funcs[fZigIDX], x1, x2).doubleValue();
		res = funcs[fZigIntegIDX].apply(x2) - funcs[fZigIntegIDX].apply(x1);
		return res;
	}

	@Override
	public double processResValByMmnts(double val) {	return summary.normToGaussTransform(val);}//public abstract double processResValByMmnts(double val);	

}//class myFleishFunc




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
			xVals[i]=func.f_invZig(eval);
			fXVals[i]=func.fZig(xVals[i]);
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
		func.expMgr.dispMessage("zigConstVals", "calcRValAndVol",  func.getShortDesc()+ " | Done w/ " + Nrect + " rects, @ iter : " + iter + " rVal : " + String.format("%3.18f", rValGuess)+ " Gives Vol : " + String.format("%3.18f", zValAra[1]),true);
		return res;
	}//calcRVal
	
	
	//return important values for this ziggurat const struct
	public String toString() {
		String res = "Owning Func : " +func.getShortDesc()+" # Rects : " + Nrect + " R_Last : " + String.format("%3.16f", R_last) + " | Vol Per Zig : " + String.format("%3.16f", V_each) + "\n";
		return res;	
	}
	
}//class zigConstVals

