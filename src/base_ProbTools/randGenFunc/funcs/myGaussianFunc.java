package base_ProbTools.randGenFunc.funcs;

import java.math.BigDecimal;
import java.util.function.Function;

import base_ProbTools.baseProbExpMgr;
import base_ProbTools.quadrature.base.baseQuadrature;
import base_ProbTools.randGenFunc.funcs.base.baseRandVarFunc;
import base_StatsTools.summary.myProbSummary_Dbls;

/**
 * instancing class for the function describing a gaussian random variable of specified mean and variance
 * @author john
 *
 */
public class myGaussianFunc extends baseRandVarFunc{	
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
	public myGaussianFunc(baseQuadrature _quadSlvr, myProbSummary_Dbls _summaryObj, String _name) {
		super(_quadSlvr, _summaryObj, _name);	
	}//ctor
	public myGaussianFunc(baseQuadrature _quadSlvr, myProbSummary_Dbls _summaryObj) {this(_quadSlvr,  _summaryObj, "Gaussian");}
	
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
	public double[] getPlotValBounds(int funcType) {
		if(funcType==queryInvCDFIDX) {	return  new double[] {probitBnd,1-probitBnd};	}
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
		if (!getFlag(quadSlvrSetIDX)) {	msgObj.dispWarningMessage("myGaussianFunc", "integral_f", "No quadrature solver has been set, so cannot integrate f");return res;}
		//expMgr.dispInfoMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2);
		
		//if x1 is -inf... gauss-legendre quad - use error function via gaussian quad - calculating cdf
		if(x1==Double.NEGATIVE_INFINITY) {				//cdf of x2 == .5 + .5 * error function x2/sqrt(2) 
			//expMgr.dispInfoMessage("myGaussianFunc", "integral_f", "CDF : x1 : "+x1 + " and  x2 : " + x2 + " Using x2");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x2 - summary.mean())*invStdSclFact);
			//cdf == .5*(1+erf(x/sqrt(2))) 
			//res = halfVal.add(halfVal.multiply(erroFuncVal));		
			res = .5 + .5 * erroFuncVal.doubleValue();			
		} else if (x2==Double.POSITIVE_INFINITY) {		//pos inf -> this is 1- CDF == Q function
			//expMgr.dispInfoMessage("myGaussianFunc", "integral_f", "Q func : x1 : "+x1 + " and  x2 : " + x2 + " Using x1");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x1 - summary.mean())*invStdSclFact);
			//Q function is == 1 - (.5*(1+erf(x/sqrt(2))))
			//res = BigDecimal.ONE.subtract(halfVal.add(halfVal.multiply(erroFuncVal)));		
			res = 1.0 - (.5 + .5 * erroFuncVal.doubleValue());				
		} else {
			res = quadSlvr.evalIntegral(funcs[fIDX], x1, x2).doubleValue();
		}
		//expMgr.dispInfoMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2 + " Res : \n" + res);
		return res;
	}//integral_f

	//calculate integral of f from x1 to x2.  Use to calculate cumulative distribution by making x1== -inf, and x2 definite val
	@Override
	public double integral_fStd(Double x1, Double x2) {
		double res = 0;
		if (!getFlag(quadSlvrSetIDX)) {	msgObj.dispWarningMessage("myGaussianFunc", "integral_fStd", "No quadrature solver has been set, so cannot integrate f");return res;}
		//expMgr.dispInfoMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2);		
		//if x1 is -inf... gauss-legendre quad - use error function via gaussian quad - calculating cdf
		if(x1==Double.NEGATIVE_INFINITY) {				//cdf of x2 == .5 + .5 * error function x2/sqrt(2) 
			//expMgr.dispInfoMessage("myGaussianFunc", "integral_f", "CDF : x1 : "+x1 + " and  x2 : " + x2 + " Using x2");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, x2*invSqrt2);
			//cdf == .5*(1+erf(x/sqrt(2))) 
			//res = halfVal.add(halfVal.multiply(erroFuncVal));		
			res = .5 + .5 * erroFuncVal.doubleValue();			
		} else if (x2==Double.POSITIVE_INFINITY) {		//pos inf -> this is 1- CDF == Q function
			//expMgr.dispInfoMessage("myGaussianFunc", "integral_f", "Q func : x1 : "+x1 + " and  x2 : " + x2 + " Using x1");
			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, x1*invSqrt2);
			//Q function is == 1 - (.5*(1+erf(x/sqrt(2))))
			//res = BigDecimal.ONE.subtract(halfVal.add(halfVal.multiply(erroFuncVal)));		
			res = 1.0 - (.5 + .5 * erroFuncVal.doubleValue());				
		} else {
			res = quadSlvr.evalIntegral(funcs[fStdIDX], x1, x2).doubleValue();
		}
		//expMgr.dispInfoMessage("myGaussianFunc", "integral_f", "Integrating for : x1 : "+x1 + " and  x2 : " + x2 + " Res : \n" + res);
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
	//pdf - described for this kind of rnd func by f
	@Override
	public double PDF(double x) {	return f(x);}

	//get CDF of passed x value for this distribution - assume this value is from actual gaussian distribution (i.e. hasn't been normalized yet)
	@Override
	public double CDF(double x) {	return integral_f(Double.NEGATIVE_INFINITY, x);	}
	//calculate inverse cdf of passed value 0->1; this is probit function, related to inverse erf
	@Override
	public double CDF_inv(double x) {	
		double normRes = calcProbitApprox(x);
		//System.out.println("Raw probit val : " + normRes + " : " + x);
		return summary.normToGaussTransform(normRes);
	}//CDF_inv
	@Override
	public void setOptionFlags(int[] _opts) {}
	@Override
	public int getRVFType() {return baseProbExpMgr.gaussRandVarIDX;}

}//class myGaussianFunc
