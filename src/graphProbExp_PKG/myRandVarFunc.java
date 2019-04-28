package graphProbExp_PKG;

import java.math.*;
import java.util.TreeMap;
import java.util.function.Function;
import base_Utils_Objects.*;
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
							twoPi = Math.PI*2.0,
							probitBnd = 1e-10;//so 0 or 1 probs are not used
	
	//types of functions to query
	public static final int
		queryFuncIDX = 0,
		queryPDFIDX = 1,
		queryCDFIDX = 2,
		queryInvCDFIDX = 3,
		queryIntegIDX = 4;
	
	public static final String[] queryFuncTypes = new String[] {"Function Eval", "PDF Eval", "CDF Eval", "Inv CDF Eval","Integral Eval"};	
	
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
	
	//set rv func-specific options
	public abstract void setOptionFlags(int[] _opts);
	//return what type, as specified in BaseProbExpMgr, this function is
	public abstract int getRVFType();

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
	//ignores x2 for all functions but integral
	private final double getDispFuncVal(int funcType, double x1, double x2) {
		switch(funcType) {
		case queryFuncIDX : {			return f(x1);}
		case queryPDFIDX : {			return PDF(x1);}
		case queryCDFIDX : {			return CDF(x1);	}
		case queryInvCDFIDX: {			return CDF_inv(x1);}
		case queryIntegIDX : {			return integral_f(x1,x2);}		
		default : {
			expMgr.dispMessage("myRandVarFunc", "getFuncVal", "Attempting to evaluate unknown func type : " + funcType +" on value(s) : [" + x1 + ", "+ x2 + "] Aborting.",MsgCodes.warning1 , true);
			return x1;}
		}
	}//getFuncVal
	
	public final String getDispFuncName(int funcType) {
		switch(funcType) {
		case queryFuncIDX : {			return name+ " Base Func";}
		case queryPDFIDX : {			return name+ " PDF";}
		case queryCDFIDX : {			return name+ " CDF";	}
		case queryInvCDFIDX: {			return name+ " Inverse CDF";}
		case queryIntegIDX : {			return name+ " Integral";}		
		default : {
			expMgr.dispMessage("myRandVarFunc", "getFuncVal", "Attempting to name unknown func type : " + funcType +". Aborting.",MsgCodes.warning1 , true);
			return name+ " ERROR";}
		}		
	}//

	//for plotting results - this returns bounds
	public abstract double[] getPlotValBounds(int funcType);
	
	//for plotting results, derive all values for plot.  pass pre-built array references from rand gen
	public void buildFuncPlotVals(int numVals, double low, double high,  int funcType, myDistFuncHistVis distVisObj ) {
		if(numVals < 2) {		numVals = 2;		}//minimum 2 values
//		if (low == high) {//ignore if same value
//			System.out.println("myRandGen : "+name+" :: calcFValsForDisp : Low == High : " +low +" : "+ high +" : Ignored, no values set/changed.");
//			return;			
//		} 
//		else if(low > high) {	double s = low;		low = high;		high = s;		}  //swap if necessary
		//get min/max values based on mean +/- 3.5 stds
		double[] minMaxVals = getPlotValBounds(funcType);
		String funcName = getDispFuncName(funcType);

		double[][] funcVals = new double[numVals][2];
		double xdiff = minMaxVals[1]-minMaxVals[0];//high-low;
		for(int i=0;i<funcVals.length;++i) {		
			//funcVals[i][0] = low + (i * xdiff)/numVals;	
			funcVals[i][0] = minMaxVals[0] + (i * xdiff)/numVals;	
		}
		//evaluate specified function on funcVals
		double minY = Double.MAX_VALUE, maxY = -minY, ydiff;
		for(int i=0;i<funcVals.length-1;++i) {		
			funcVals[i][1] = getDispFuncVal(funcType,funcVals[i][0],funcVals[i+1][0]);
			minY = (minY > funcVals[i][1] ? funcVals[i][1] : minY);
			maxY = (maxY < funcVals[i][1] ? funcVals[i][1] : maxY);
		}
		//last argument is ignored except for integral calc 
		int i=funcVals.length-1;
		funcVals[i][1] = getDispFuncVal(funcType,funcVals[i][0],Double.POSITIVE_INFINITY);
		if(Math.abs(funcVals[i][1]) <= 10000000* Math.abs(funcVals[i-1][1])) {//- don't count this last value for min/max in case of divergence 
			minY = (minY > funcVals[i][1] ? funcVals[i][1] : minY);
			maxY = (maxY < funcVals[i][1] ? funcVals[i][1] : maxY);
		}
		minY = (minY > 0 ? 0 : minY);
		ydiff = maxY - minY;
		//distVisObj.setValuesFunc(funcVals, new double[][]{{low, high, xdiff}, {minY, maxY, ydiff}});
		double minVal = (minMaxVals[0] < low ? minMaxVals[0] : low),
				maxVal = (minMaxVals[1] > high ? minMaxVals[1] : high);
		double[][] minMaxDiffFuncVals = new double[][]{{minVal, maxVal, (maxVal-minVal)}, {minY, maxY, ydiff}};
		
		distVisObj.setValuesFunc(funcName, new int[][] {new int[] {0,0,0,255}, new int[] {255,255,255,255}}, funcVals, minMaxDiffFuncVals);
		
	}//buildPlotVals
	
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
	
	//calculate the PDF - usually this will just be the functional description
	public abstract double PDF(double x);
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
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myRandVarFunc", "setZigVals", "No quadrature solver has been set, so cannot set ziggurat values for "+_nRect+" rectangles (incl tail).",MsgCodes.warning1,true); return;}
		double checkRect = Math.log(_nRect)/ln2;
		int nRectCalc = (int)Math.pow(2.0, checkRect);//int drops all decimal values
		if (_nRect != nRectCalc) {	
			int numRectToUse = (int)Math.pow(2.0, (int)(checkRect) + 1);
			expMgr.dispMessage("myRandVarFunc", "setZigVals", "Number of ziggurat rectangles requested " + _nRect + " : " + nRectCalc + " must be an integral power of 2, so forcing requested " + _nRect + " to be " + numRectToUse,MsgCodes.warning1,true);
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
	
	//display results from cdf map of values, where key is cdf and value is value
	protected void dbgDispCDF(TreeMap<Double,Double> map, String callingClass) {
		expMgr.dispMessage(callingClass,"dbgDispCDF","CDF Values : ",MsgCodes.warning1,true);
		for(Double key : map.keySet()) {expMgr.dispMessage(callingClass,"dbgDispCDF","\tKey : " + key +" | CDF Val : " + map.get(key),MsgCodes.warning1,true);}
		
	}//dbgDispCDF

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
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myGaussianFunc", "integral_f", "No quadrature solver has been set, so cannot integrate f",MsgCodes.warning1,true);return res;}
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
		if (!getFlag(quadSlvrSetIDX)) {	expMgr.dispMessage("myGaussianFunc", "integral_fStd", "No quadrature solver has been set, so cannot integrate f",MsgCodes.warning1,true);return res;}
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
	public int getRVFType() {return BaseProbExpMgr.gaussRandVarIDX;}

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
	
	@Override
	protected void buildFuncs() {
		errorFunc =  (x ->  ErfCoef * Math.exp(-(x*x)));

		//actual probablity functions
		funcs[fIDX] 		= (x -> (normalSclFact  * Math.exp(-0.5 * (x*x))));
		funcs[fInvIDX] 		= (xinv -> (Math.sqrt(-2.0 * Math.log(xinv/normalSclFact))) );
		//zigurat uses standardized functions -> want pure normal distribution 
		funcs[fStdIDX]		= (x -> Math.exp(-0.5 *(x*x)));
		funcs[fInvStdIDX]	= (xinv -> (Math.sqrt(-2.0 * Math.log(xinv)))); 
		
		//derivative functions		
		funcs[fDerivIDX]	= (x -> (-x * normalSclFact  * Math.exp(-0.5 * (x*x))));	
		funcs[fStdDeriveIDX] = (x -> (-x * Math.exp(-0.5 *(x*x))));	
		//integrals
		integrals[fIntegIDX] = (x -> integral_f(x[0],x[1]));
		integrals[fStdIntegIDX] = (x -> integral_fStd(x[0],x[1]));
		
	}//buildFuncs
	
	
	//if this is a normal function, then these will not change
	@Override
	public double processResValByMmnts(double val) {	return val;}
	@Override
	public double CDF_inv(double x) {	
		double normRes = calcProbitApprox(x);
		//System.out.print("Raw probit val : " + normRes + " : ");
		return normRes;
	}//CDF_inv
	@Override
	public int getRVFType() {return BaseProbExpMgr.normRandVarIDX;}
	@Override
	public void setOptionFlags(int[] _opts) {}

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
	//quantities related to underlying cubic function
	//derivative quadratic coefficients : b + 2*cx + 3*dx^2 - find roots of deriv to determine min/maxs
	//[0][0] : lower x min/max, [0][1] : higher valued min/max
	//[1][-] : 2nd deriv test for idx0 and idx1 pts (>0 == min, <0 == max, ==0 may indicate inflection point
	//[2][-] : 3rd deriv test for sign
	//[3][-] : func eval for idx0 and idx1 pts 
	private double[][] minMax2ndDerivs;
	//roots of cubic - will always have at least 1 root (y==0)
	private double[] roots;
	//whether this is ready to use or not - all values have been set and calculated
	private boolean ready;
	//maximum iterations
	private final int maxIter = 10000;
	//alpha
	private double[] lrnRateAra;

	//normal distribution for inverse calc
	private myZigRandGen zigNormGen;
	
	
	public myFleishFunc_Uni(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj, String _name) {
		super(_expMgr, _quadSlvr,_summaryObj, _name);		
	}//ctor

	@Override
	protected void rebuildFuncs_Indiv() {
		//build internal normal used to generate input to fleishman polynomial
		//normFunc = new myNormalFunc(expMgr, quadSlvr);
		if(zigNormGen == null) {	zigNormGen = new myZigRandGen(new myNormalFunc(expMgr, quadSlvr), 256, "Ziggurat Algorithm");}	//don't rebuild if present
		ready = false;
		lrnRateAra = new double[] {.1,.1,.1};
		coeffs = calcCoeffs();
		minMax2ndDerivs = calcQuadraticDerivRoots();
		roots = calcAllRoots();		
		expMgr.dispMessage("myFleishFunc_Uni", "calcCoeffs", "Coeffs calculated :  ["+getCoeffsStr() +"]",MsgCodes.info1,true);		
		
		//set summary builds functions - need to specify required elements before it is called
	}//rebuildFunc
	
	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds(int funcType) {
		if(funcType==queryInvCDFIDX) {	return  new double[] {probitBnd,1.0-probitBnd};	}
		double mu = summary.mean();//, std = summary.std();
		// TODO Auto-generated method stub
		//return new double[] {mu-(3.5*std), mu+(3.5*std)};
		//expMgr.dispMessage("myFleishFunc_Uni", "getPlotValBounds","Bounds eval @ -/+ 3.5 : [" + f(-3.5)+","+ f(3.5)+"]",MsgCodes.info1,true);		
		return new double[] {mu-5.0, mu+5.0};//{f(-3.5), f(3.5)};
	}//getPlotValBounds
	
	//calculate the coefficients for the fleishman polynomial considering the given skew and excess kurtosis specified in summary object
	//will generate data with mean ==0 and std == 1; if ex kurtosis lies outside of feasible region will return all 0's for coefficients
	private double[] calcCoeffs() {		
		expMgr.dispMessage("myFleishFunc_Uni", "calcCoeffs","Start calc coefficients.",MsgCodes.info1,true);
		double[] coeffs = new double[summary.numMmntsGiven];
		double exKurt = summary.exKurt(), skew = summary.skew(), skewSQ = skew*skew;
        //first verify exkurt lies within feasible bound vs skew
        //this is fleish's bound from his paper - said to be wrong in subsequent 2010 paper
        //bound = -1.13168 + 1.58837 * skew**2
        double bound = -1.2264489 + 1.6410373*skewSQ;
        if (exKurt < bound) { 
        	expMgr.dispMessage("myFleishFunc_Uni", "calcCoeffs", "!!!! Coefficient error : ex kurt : " + String.format("%3.8f",exKurt)+ " is not feasible with skew :" + String.format("%3.8f",skew) +" | forcing exKurt to lower bound @ skew "+String.format("%3.8f",bound)+" DANGER this is not going to reflect the sample quantities",MsgCodes.error1,true);
        	summary.forceExKurt(bound);
        	exKurt = summary.exKurt();
        }
        double exKurtSq = exKurt * exKurt;
        //initial coeff estimates
        double c1 = 0.95357 - (0.05679*skew) + (0.03520*skewSQ) + (0.00133*exKurtSq);
        double c2 = (0.10007*skew) + (0.00844*skewSQ*skew);
        double c3 = 0.30978 - (0.31655 * c1);

        //solve with newton-raphson
        double[] tmpC = newtonMethod(c1, c2, c3, skew, exKurt);
		coeffs = new double[] {-tmpC[1], tmpC[0],tmpC[1],tmpC[2]};
		expMgr.dispMessage("myFleishFunc_Uni", "calcCoeffs","Finished calc coefficients.",MsgCodes.info1,true);
		return coeffs;
	}//calcCoeffs
	
	//for when func has not yet been specified
	private double calcBaseEQ(double x) {return coeffs[0] + x *(coeffs[1] + x*(coeffs[2] + x *coeffs[3]));}
	
	//find roots described by ax^2 + bx + c ==0; if 2 roots are returned idx 0 is lowest root, idx 1 is max root
	private double[][] calcQuadraticDerivRoots() {	
		//double mu = summary.mean(), std = summary.std();
		//double a = coeffs[0]*std + mu, b=coeffs[1]*std,c = coeffs[2]*std,d=coeffs[3]*std;
		double a = coeffs[0], b=coeffs[1],c = coeffs[2],d=coeffs[3];
		//find derivative to find min/max points
		double derivC = b, derivB = 2*c, derivA = 3*d;
		
		double discr = derivB*derivB - 4 * derivA * derivC, ta = (2*derivA), axisOfSym = -derivB/ta;
		if(discr < 0) {//means imaginary roots and no mins/maxs in underlying cubic (?)
			expMgr.dispMessage("myFleishFunc_Uni", "calcQuadRoots", "Cubic deriv (quadratic) of " +getCoeffsStr() + " has negative discriminant : " + String.format("%3.8f",discr) + " -> means no mins/maxs.",MsgCodes.info1,true);
			return new double[][] {{},{},{},{}};
		} else if(discr == 0) {//double root
			expMgr.dispMessage("myFleishFunc_Uni", "calcQuadRoots", "Cubic deriv (quadratic) of " +getCoeffsStr() + " has 0 discriminant  : " + String.format("%3.8f",discr) + " -> means double root.",MsgCodes.info1,true);
			double tderivTest = ta*axisOfSym + b;
			//double val = calcBaseEQ(axisOfSym)*std + mu;
			double val = calcBaseEQ(axisOfSym);
			return new double[][] {{axisOfSym},{tderivTest},{2*a},{val}};
		}//else 2 real roots
		expMgr.dispMessage("myFleishFunc_Uni", "calcQuadRoots", "Cubic deriv (quadratic) of " +getCoeffsStr() + " has positive discriminant  : " + String.format("%3.8f",discr) + " means 2 roots.",MsgCodes.info1,true);
		double axisDist = Math.sqrt(discr)/ta;
		double zp = axisOfSym + axisDist, zm = axisOfSym - axisDist;
		//double valZP = calcBaseEQ(zp)*std + mu,valZM = calcBaseEQ(zm)*std + mu;
		double valZP = calcBaseEQ(zp),valZM = calcBaseEQ(zm);
		double tdZP = ta*zp + derivB, tdZM = ta*zm + derivB;
		return ((zp<zm) ? new double[][] {{zp, zm},{tdZP, tdZM},{ta,ta},{valZP, valZM}} : new double[][] {{zm, zp},{tdZM, tdZP},{ta,ta},{valZM, valZP}} );			
	}//calcQuadRoots
	
	//display the results of minMax2ndDerivs
	//quantities related to underlying cubic function
	//derivative quadratic coefficients : b + 2*cx + 3*dx^2 - find roots of deriv to determine min/maxs
	//[0][0] : lower x min/max, [0][1] : higher valued min/max
	//[1][-] : 2nd deriv test for idx0 and idx1 pts (>0 == min, <0 == max, ==0 may indicate inflection point
	//[2][-] : 3rd deriv test for sign
	//[3][-] : func eval for idx0 and idx1 pts 
	private void _dispDerivRootsVals() {
		double mu = summary.mean(), std = summary.std();	
		if(minMax2ndDerivs[0].length < 2) {return;}
		expMgr.dispMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "Values in minMax2ndDerivs : ",MsgCodes.info1,true);
		int idx = 0;
		expMgr.dispMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "\t min/max (deriv) : lower x minMax2ndDerivs["+idx+"][0] = "+minMax2ndDerivs[idx][0] + " | higher x minMax2ndDerivs["+idx+"][1] = "+minMax2ndDerivs[idx][1],MsgCodes.info1,true);
		++idx;
		expMgr.dispMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "\t 2nd deriv test : lower x minMax2ndDerivs["+idx+"][0] = "+minMax2ndDerivs[idx][0] + " | higher x minMax2ndDerivs["+idx+"][1] = "+minMax2ndDerivs[idx][1],MsgCodes.info1,true);
		++idx;
		expMgr.dispMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "\t 3rd deriv test : lower x minMax2ndDerivs["+idx+"][0] = "+minMax2ndDerivs[idx][0] + " | higher x minMax2ndDerivs["+idx+"][1] = "+minMax2ndDerivs[idx][1],MsgCodes.info1,true);
		++idx;
		expMgr.dispMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "\t func eval : lower x minMax2ndDerivs["+idx+"][0] = "+(minMax2ndDerivs[idx][0]*std + mu) + " | higher x minMax2ndDerivs["+idx+"][1] = "+(minMax2ndDerivs[idx][1]*std + mu),MsgCodes.info1,true);
		++idx;
	}//_dispDerivRootsVals
	
	//calculate roots of cubic via discriminant and using derive vals
	private double[] calcAllRoots() {
		double mu = summary.mean(), std = summary.std();
		//double a = coeffs[0]*std + mu, b=coeffs[1]*std, b3 = b*b*b,c = coeffs[2]*std,d=coeffs[3]*std,ad = a*d, bc = b*c, ad27 = 27*ad;
		double a = coeffs[0], b=coeffs[1], b3 = b*b*b,c = coeffs[2],d=coeffs[3],ad = a*d, bc = b*c, ad27 = 27*ad;
		//descriminant of cubic
		double discr = 18*ad*bc - 4*b3*d + bc*bc - 4*a*c*c*c - ad27*ad;
		//precalc to find roots 
		double del1 = 2 * b3 - 9*a*b*c + ad27*a, a27NegDiscr = -27*a*a*discr;
		//minMax2ndDerivs : check min/max values in function - if sign changes, then there is a zero between them
		//[0][0] : lower x min/max, [0][1] : higher valued min/max
		//[1][-] : 2nd deriv test for idx0 and idx1 pts (>0 == min, <0 == max, ==0 may indicate inflection point
		//[2][-] : 3rd deriv test for sign
		//[3][-] : func eval for idx0 and idx1 pts - if this changes signs then cubic root lies between points, otherwise root lies 
		//minMax2ndDerivs;
		//_dispDerivRootsVals();
	
		int numRoots = 0;
		if(discr < 0) {//1 real, 2 non-real complex-conjugate roots
			expMgr.dispMessage("myFleishFunc_Uni", "calcAllRoots", "Cubic Root calc : discr : " + String.format("%3.8f",discr) + " -> means 1 real and 2 non-real cmplx roots.",MsgCodes.info1,true);
			numRoots = 1;
		} 
		else if (discr > 0) {//has 3 distinct real roots
			expMgr.dispMessage("myFleishFunc_Uni", "calcAllRoots", "Cubic Root calc : discr : " + String.format("%3.8f",discr) + " -> means 3 distinct real roots.",MsgCodes.info1,true);
			numRoots = 3;
		} 
		else {//discr == 0 - has a multiple root and all roots are real
			expMgr.dispMessage("myFleishFunc_Uni", "calcAllRoots", "Cubic Root calc : discr : " + String.format("%3.8f",discr) + " -> means multiple root and all roots are real.",MsgCodes.info1,true);
			numRoots = 2;
		}
		double[] res = new double[numRoots];
		
		
		
		return res;
	}//calcAllRoots
	
	//Given the fleishman coefficients, and a target skew and kurtois 
    //this function will have a root if the coefficients give the desired skew and kurtosis
    //F = -c + bZ + cZ^2 + dZ^3, where Z ~ N(0,1)
	private DoubleMatrix flfunc(double b, double c, double d, double skew, double exKurt) {
        double b2 = b*b, c2 = c*c, d2 = d*d, bd = b*d, t4bd = 24*bd;
        double _v = b2 + 6*bd + 2*c2 + 15*d2;
        double _s = 2 * c * (b2 + t4bd + 105*d2 + 2);
        //24*b*d + 24*c^2 + 24*c^2*b^2 + 672*c^2*b*d +  
        double _k = t4bd + 24*(c2 * (1 + b2 + 28*bd) + d2 * (12 + 48*bd + 141*c2 + 225*d2));
        return new DoubleMatrix(new double[] {_v - 1, _s - skew, _k - exKurt});		
	}//flfunc

	//The deriviative of the flfunc above
    //returns jacobian
	private DoubleMatrix flDeriv(double b, double c, double d) {
		double b2 = b*b, c22 = 2.0*c*c, d2 = d*d, bd = b*d;
		//matrix coeffs
		// _v = b2 + 6*bd + 2*c2 + 15*d2
		double df1db = 2.0*b + 6.0*d, 
				df1dc = 4.0*c, 
				df1dd = 6.0*b + 30.0*d,
		//_s = 2 * c * (b2 + t4bd + 105*d2 + 2);
				df2db = df1dc * (b + 12.0*d),						//4bc + 48cd  
				df2dc = 2.0 *b2 + 48.0*bd + 210.0*d2 + 4.0,			//
				df2dd = df1dc * (12.0*b + 105*d),					//48bc + 420cd == 4c*(12b + 105d)
		//t4bd + 24*(c2 * (1 + b2 + 28*bd) + d2 * (12 + 48*bd + 141*c2 + 225*d2))
		//24(bd +   c^2 +   c^2b^2 +  28*bc^2d + 12d^2    + 48d^3b + 141c^2d^2 + 225d^4)
		//deriv b : 
		//24 * (d + 2c^2*b + 28*c^2*d + 48*d^3 
		//deriv c :
		//24 ( 2c + 2cb^2 + 56bcd + 282cd^2) == 48(c + cb^2 +28bd + 141d
		//deriv d : 
		//24(b   +   0    +  0    +    28bc^2   + 24d   +  144d^2b + 282c^2d +  900d^3)  
		//24(b  		       14*b * 2c^2 + 24*d + 
				df3db = 24.0 * (d + c22*(b + 14.0*d) + 48.0*d2*d),
				df3dc = 48.0 * c * (1.0 + b2 + 28*bd + 141.0*d2),
				df3dd = 24.0 * (b + 14.0*b *c22 + d*(24 + 144*bd + 141.0*c22 + 900*d2));
				
			//	df3dd = 24.0 * (b + 14.0*b * c22 + d*(24.0 + 96.0*bd + 141.0*c22 + 450.0*d2) + d2*(48.0*b + 450.0*d));
			//		df3db = 24 * (d + c2*(2*b + 28*d) + 48*d2*d),
			//		df3dc = 48 * c * (1 + b2 + 28*bd + 141*d2),
			//		//df3dd = 24 * (b + 28*b * c2 + 2*d*(12 + 48*bd + 141*c2 + 225*d2) + d2*(48*b + 450*d));
			//		df3dd = 24 * (b + 28*b * c2 + d*(24 + 96*bd + 282*c2 + 450*d2) + d2*(48*b + 450*d));
        return new DoubleMatrix(new double[][] {
        	{df1db, df1dc, df1dd},
        	{df2db, df2dc, df2dd},
            {df3db, df3dc, df3dd}});
	}//flDeriv
	
	
//uses standardized moments - screwy equations	
//	//Given the fleishman coefficients, and a target skew and kurtois - equations from thesis
//    //this function will have a root if the coefficients give the desired skew and kurtosis
//    //F = -c + bZ + cZ^2 + dZ^3, where Z ~ N(0,1)
//	private DoubleMatrix flfunc_alt(double b, double c, double d, double skew, double exKurt) {
//        double b2 = b*b, c2 = c*c, c3 = c2*c, d2 = d*d, bd = b*d, t4bd = 24*bd;
//        //a2 + 2ac + b2 + 6bd + 3c4 + 15d2 = 1
//        //c2 - 2c2 + b2 + 6bd + 3c4 + 15d2 = 1
//        double _v = -c2 + 3*c2*c2 + b2 + 6*bd + 15*d2;
//        //a3 + 3a2c + 3ab2 + 18abd + 9ac2 + 45ad2 + 9b2c + 90bcd + 15c3 + 315cd2 = skew
//        //-c3 + 3c3 - 3cb2 - 18cbd - 9c3  - 45cd2 + 9b2c + 90bcd + 15c3 + 315cd2
//        //8c3                                     + 6b2c + 72bcd        + 270cd2
//        double _s = 8.0*c3 + 6.0*c*b2 + 72.0*c*bd + 270.0*c*d2;
//        //a4 + 4a3c + 6a2b2 + 36a2bd + 18a2c2 + 90a2d2 + 36ab2c + 360abcd + 60ac3 + 1260acd2 + 3b4 + 60b3d + 90b2c2 + 630b2d2 + 1260bc2d + 3780bd3 + 105c4 + 5670c2d2 + 10395d4 = exKurt
//        //c4 - 4c4  + 6c2b2 + 36c2bd + 18c4   + 90c2d2 - 36b2c2 - 360bc2d - 60c4  - 1260c2d2 + 3b4 + 60b3d + 90b2c2 + 630b2d2 + 1260bc2d + 3780bd3 + 105c4 + 5670c2d2 + 10395d4 = exKurt
//        //60c4      +                                                                        + 3b4 + 60b3d + 60b2c2 + 630b2d2 + 926bc2d  + 3780bd3 +         4500c2d2 + 10395d4 = exKurt
//         //
//        double _k = 3.0*b2*b2 + 10395.0*d2*d2 + 60.0*c2*c2 + 60.0*b2*c2 + 926.0*c2*bd + 4500.0*c2*d2 + 60.0*b2*bd +  630.0*b2*d2 + 3780.0*bd*d2;
//        return new DoubleMatrix(new double[] {_v - 1, _s - skew, _k - exKurt});		
//	}//flfunc
//
//	
//	//The deriviative of the flfunc above - from thesis
//    //returns jacobian
//	private DoubleMatrix flDeriv_alt(double b, double c, double d) {
//		double b2 = b*b, b3=b*b2, c2 = c*c, c3 = c2*c, c22 = 2.0*c2, d2 = d*d, d3 = d*d2, bd = b*d, bc=b*c, cd=c*d;
//		//matrix coeffs
//		//_v = -c2 + 3*c2*c2 + b2 + 6*bd + 15*d2;
//		double df1db = 2.0*b + 6.0*d, 
//				df1dc = -1.0*c + 12.0*c3, 
//				df1dd = 6.0*b + 30.0*d,
//		// _s = 8.0*c3 + 6.0*c*b2 + 72.0*c*bd + 270.0*c*d2;
//				df2db = 12.0*bc + 72.0*cd,								//12bc + 72cd 
//				df2dc = 24.0*c2 + 6.0*b2 + 72.0*bd + 270.0*d2,		//24.0c2 + 6b2 + 72bd + 270d2
//				df2dd = 72.0*bc + 540.0*cd,							//72cb + 540cd
//		//_k = 3.0*b2*b2 + 10395.0*d2*d2 + 60.0*c2*c2 + 60.0*b2*c2 + 926.0*c2*bd + 4500.0*c2*d2 + 60.0*b2*bd +  630.0*b2*d2 + 3780.0*bd*d2;
//		//deriv b : 12b3 + 120bc2 + 926c2d + 180b2d + 1260bd2 + 3780d3
//				df3db = 12.0*b3 + 120.0*b*c2 + 926.0*c2*d + 180.0*b2*d + 1260.0*b*d2 + 3780.0*d3,
//		//deriv c : 240*c3 + 120*b2c + 1852*bcd + 9000*c*d2 
//				df3dc = 240*c3 + 120*b2*c + 1852*b*c*d + 9000*c*d2,
//		//deriv d : 41580 * d3 + 926*c2 + 180*b2 + 2520*bd + 11340*d2
//				df3dd = 41580.0*d3 + 926.0*c2 + 180.0*b2 + 2520.0*bd + 11340*d2;
//	        return new DoubleMatrix(new double[][] {
//        	{df1db, df1dc, df1dd},
//        	{df2db, df2dc, df2dd},
//            {df3db, df3dc, df3dd}});
//	}//flDeriv
	
	//simple newton method solver
	private double[] newtonMethod(double b,double c,double d,double skew,double exKurtosis) {
        //Implements newtons method to find a root of flfunc
		DoubleMatrix f = flfunc(b,c, d, skew, exKurtosis), delta, oldf = f.mul(2.0), olderF = oldf.mul(2.0);
        DoubleMatrix J;
        double mult = 1.0, oldfMag=0, fMag = 0;
        int i = 0;
        for (i=0; i<maxIter; ++i) {
            if (isNewtonDone(f)){          break;   }//want to minimize f
            //get jacobian
            J = flDeriv(b,c, d);
            //find delta amt that minimizes J * [b,c,d]T = df  where df (f here) is [f1-1, f2-skew, f3-exkurt]T
            delta = (Solve.solve(J, f));
            mult = 10.0;
            //adaptive time step
            while (mult > .0001) {
	            do {
		            b -= mult*lrnRateAra[0] * delta.data[0];
		            c -= mult*lrnRateAra[1] * delta.data[1];
		            d -= mult*lrnRateAra[2] * delta.data[2];
		            oldf = f;oldfMag = f.norm2();
		            f = flfunc(b,c, d, skew, exKurtosis); fMag = f.norm2();	
	            } while (oldfMag > fMag) ;
	            b += mult*lrnRateAra[0] * delta.data[0];
	            c += mult*lrnRateAra[1] * delta.data[1];
	            d += mult*lrnRateAra[2] * delta.data[2];
	            f = oldf;
	            mult *=.5;
            }	            
            //expMgr.dispMessage("myFleishFunc_Uni", "newtonMethod", "Newton iters to find coeffs : " + i + " : final f : " +getNewtonFStr(f) ,MsgCodes.info1,true);
        }
        //expMgr.dispMessage("myFleishFunc_Uni", "newtonMethod", "Newton iters to find coeffs : " + i + " : final f : " +getNewtonFStr(f) ,MsgCodes.info1,true);
        return new double[] {b,c, d};
	}//newton
	
	private String getCoeffsStr() {
		if(coeffs==null) {return "";}
		return String.format("%3.8f",coeffs[0])+","+ String.format("%3.8f",coeffs[1])+","+ String.format("%3.8f",coeffs[2])+","+ String.format("%3.8f",coeffs[3]);
	}
	
	private String getNewtonFStr(DoubleMatrix f) {
		String res = "[";
		for(int i=0;i<f.data.length-1;++i) {			res+= String.format("%3.8f", f.data[i])+", " ;		}
		res +=String.format("%3.8f", f.data[f.data.length-1])+"]" ;		
		return res;
	}
	
	//find functional inverse - given specific y value, find x such that y = func(x) -> x = func^-1(y)
	//i.e. find x value that will give f(x)==y - note y should not be shifted by mean and std
	public double calcInvF(double yRaw) {
		double mu = summary.mean(), std = summary.std();
		double y=(yRaw-mu)/std;
		double res = (y-coeffs[0])/coeffs[1], diff, diffSq, fRes = 0, dfRes = 0;
		//double convLimSq = convLim*convLim;
		//use newton method to find value
		int i = 0;
		int maxInvIter = 100;
		for (i=0; i<maxInvIter; ++i) {
			fRes = funcs[fStdIDX].apply(res);
			dfRes = funcs[fStdDeriveIDX].apply(res);
			diff = (fRes - y);
			diffSq = diff * diff;
			if(Math.abs(diff) < convLim) {break;}
			//if(Math.abs(oldDiff) < Math.abs(diff)) {System.out.println("iter " + i + " DIVERGING! : diff : " + String.format("%3.8f", diff) + " | oldDiff : " + String.format("%3.8f", oldDiff));	}
			//{System.out.println("iter " + i + " diff : " + String.format("%3.8f", diff) + "\ty :"+ y + " res : " + String.format("%3.8f", res) + " f(res) : "+ String.format("%3.8f", fRes)+ " dfRes : "+ String.format("%3.8f", dfRes) + "| coeffs :["+getCoeffsStr()+"]" );}//+ "\t f'(res) : " + String.format("%3.8f", dfRes));}
			res -= (fRes-y)/dfRes;

		}	
		//System.out.println("myFleishFunc_Uni::calcInvF : iters to find inverse : " + i + " result : " + res + " y : " + y + " f(res) : "+ fRes + "| coeffs :["+getCoeffsStr()+"]" );//+ "\t f'(res) : " + String.format("%3.8f", dfRes));
		return res;
		
	}//calcInvF
	
	//this takes a normal input, not a uniform input - TODO change this to take uniform input (??)
	@Override
	protected void buildFuncs() {
		double mu = summary.mean(), std = summary.std();//, var = summary.var();
		//actual functions
		funcs[fIDX] 		= x ->  {return ((coeffs[0] + x*(coeffs[1] +x*(coeffs[2]+ x*coeffs[3])))*std +  mu);		}; 
		//TODO find inverse of polynomial?  inverse of this function may not exist - need to use optimization process 
		funcs[fInvIDX] 		= (xinv -> null);
		//zigurat functions -> want pure normal distribution
		funcs[fStdIDX]		= x -> {return (coeffs[0] + x*(coeffs[1] +x*(coeffs[2]+ x*coeffs[3])));		};
		funcs[fInvStdIDX]	= (xinv -> null);
		//analytical derivatives
		funcs[fDerivIDX]	= x -> {return ((coeffs[1] +x*(2*coeffs[2]+ x*3*coeffs[3]))*std );}; //b + 2cx + 3dx^2 == b + x * (2c + 3dx)
		funcs[fStdDeriveIDX] = x -> {return (coeffs[1] +x*(2*coeffs[2]+ x*3*coeffs[3]));};                                                                   ;
		//integrals
		integrals[fIntegIDX] = (x -> integral_f(x[0],x[1]));
		integrals[fStdIntegIDX] = (x -> integral_fStd(x[0],x[1]));		
	}//buildFuncs
	
	//pdf - for fleishman distribution, this is not described by function
	@Override
	public double PDF(double x) {	
		double mu = summary.mean(), std = summary.std();
		//first find normal draw result x' that yielded x when fed to polynomial : fl(xprime) == x -> fl_inv(x) == xprime - inverting via newton method
		double xPrime = calcInvF(x);
		//now evaluate std derivative @ xPrime
		double derivVal = funcs[fStdDeriveIDX].apply(xPrime);
		
		return ((zigNormGen.func.fStd(xPrime)*std + mu)/derivVal);
	}//PDF
	
	//fleish has multi-step process.  first generate normal value (x'), then feed to polynomial to yield final value (x).  
	//in this case, we want to find find cumulative prob value p(X<=x) for fleishman. 
	//first must find x' value that, when fed to underlying normal distribution, yields x
	@Override
	public double CDF(double x) {	
		//first find normal draw result x' that yielded x when fed to polynomial : fl(xprime) == x -> fl_inv(x) == xprime
		double xPrime = calcInvF(x);
		
		//find polynomial 
		//now we can find the CDF of transformed x by finding normFunc cdf of t and transforming it
		return zigNormGen.func.CDF(xPrime);
		
	}	//need to find most negative value of function corresponding to 0 probability => coeffs[0] 

	@Override
	public double CDF_inv(double prob) {
		if(prob == 0) {prob+=probitBnd;} else if(prob == 1.0) {prob -= probitBnd;}
		//t is value of underlying normal that has given probability (p(x<=t) == prob)
		double t = zigNormGen.inverseCDF(prob);
		//this will be transformed value
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

	@Override
	public void setOptionFlags(int[] _opts) {
	}

	@Override
	public int getRVFType() {return BaseProbExpMgr.fleishRandVarIDX;}


}//class myFleishFunc

//class to model a pdf via a cosine
//mean is phase, std is function of frequency
class myCosFunc extends myRandVarFunc{
	//constants to modify cosine so that we represent desired moment behavior
	//area under pdf from mean-> x*std corresponding to x val @ 0,1,2,3 - used to determine appropriate frequency values 
	private static final double[] stdAreaAra = new double[] {0.0,0.3413447460685429485852 ,0.4772498680518207927997 , 0.4986501019683699054734};
	//don't set this to 0!
	private static final int stdFreqMultToUse = 1;
	
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
	
	//this will calculate the freq val for a given std iteratively 
	protected double calcFreq(double std) {
		//qStd is std @ stdFreqMultToUse
		double stdArea = stdAreaAra[stdFreqMultToUse], twoPiStdArea = stdArea* twoPi;
		double res = std,sinFreqS,diff, qStd = stdFreqMultToUse * std  ,cosFreqS ;
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			sinFreqS = Math.sin(res * qStd);//solve CDF for std value - ignore mu
			//cosFreqS = Math.cos(res * qStd);//solve CDF for std value - ignore mu
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
			double freqMult2 = Math.PI/std;
			expMgr.dispMessage("myCosFuncFromCDF","rebuildFuncs_Indiv","Calced freqMult : " + String.format("%3.8f",freqMult) + " | pi/s : " + String.format("%3.8f",freqMult2) + " | s :"+String.format("%3.8f",std) + " | calc ov pi/s : " + String.format("%3.8f",freqMult/freqMult2),MsgCodes.info1,true);
		}
		xBnd = Math.PI/freqMult;		//values need to be between mu - xBnd and mu + xBnd
		actLBnd = mu - xBnd;
		actUBnd = mu + xBnd;
		halfAmpl = freqMult/twoPi;		
	}//rebuildFuncs_Indiv
	
	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds(int funcType) {
		if(funcType==queryInvCDFIDX) {	return  new double[] {0.0,1.0};	}
		//double mu = summary.mean(), std = summary.std();
		// TODO Auto-generated method stub
		return new double[] {actLBnd, actUBnd};
	}//getPlotValBounds
	
	@Override
	protected void buildFuncs() {
		double mu = summary.mean();//, var = summary.var();
		//form should be freq/2pi * (1 + (cos(freq*(x - mu)))); 
		//want to find appropriate freq so that area under curve [0,std] == 0.3413447460685429 		
		//actual functions
		funcs[fIDX] 		= x ->  {return (halfAmpl * (1 +  Math.cos(freqMult * (x - mu))));};
		funcs[fInvIDX] 		= xinv -> {return  (Math.acos(  (xinv/halfAmpl) - 1.0) + (freqMult*mu))/freqMult; };
//		//zigurat functions -> want 0 mean 1 std distribution
		funcs[fStdIDX]		= x -> {return (halfAmpl1Std * (1 +  Math.cos(freq1StdMult * x)));};
		funcs[fInvStdIDX]	= xinv -> {return  (Math.acos(  (xinv/halfAmpl1Std) - 1.0))/freq1StdMult; };
//		//analytical derivatives
		funcs[fDerivIDX]	= x -> {return (halfAmpl * (- freqMult * Math.sin(freqMult * (x - mu))));};    
		funcs[fStdDeriveIDX] = x -> {return (halfAmpl1Std * (- freq1StdMult * Math.sin(freq1StdMult * x)));};                                                         ;
		//integrals - solve analytically
		integrals[fIntegIDX] = x -> {return (halfAmpl * ((x[1]-x[0]) +  (Math.sin(freqMult * (x[1] - mu)) - Math.sin(freqMult * (x[0] - mu)))/freqMult));};
		integrals[fStdIntegIDX] = x -> {return (halfAmpl1Std * ((x[1]-x[0]) +  (Math.sin(freq1StdMult * x[1]) - Math.sin(freq1StdMult * x[0]))/freq1StdMult));};
	}//

	//pdf - described for this kind of rnd func by f
	@Override
	public double PDF(double x) {	return f(x);}
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

	@Override
	public void setOptionFlags(int[] _opts) {
	}

	@Override
	public int getRVFType() {return BaseProbExpMgr.raisedCosRandVarIDX;}

}//myCosFunc

/**
 * this function will build a pdf/cdf model based on working backward from samples - building cdf from sample data, fitting sin-based CDF function to data, then differentiating to find pdf
 * @author john
 */
class myCosFuncFromCDF extends myRandVarFunc{
	//TODO Doesn't work
	//variables that describe the underlying sin PDF : A*(sin(B*x + C) + x)
	private DoubleMatrix Theta;
	private double actLBnd, actUBnd;
	//whether to use 1 + sine or x + sine for CDF
	private int CDFToUse;
	//functions to solve optimization CDF - either a(1 + sine(b*(x - c))) or a(x + sine(b*(x-c)))
	//takes x, a,b,c as input, returns func eval
	private Function<Double[], Double>[] slvrFuncs;	
	//derivatives w/respect to each coefficient a,b,c...
	private Function<Double[], double[]>[] slvrDerivFuncs;	
	
	//idxs in slvrFuncs 
	private static final int 
		cdf1pSine = 0,
		cdfXpSine = 1;
	private static final int numSlvrFuncs = 2;
	public myCosFuncFromCDF(BaseProbExpMgr _expMgr, myIntegrator _quadSlvr, myProbSummary _summaryObj) {
		super(_expMgr, _quadSlvr, _summaryObj, "Cosine PDF");
		buildSlvrFuncs();		
	}//ctor
	
	private void buildSlvrFuncs() {
		//funcs are CDF functions
		slvrFuncs = new Function[numSlvrFuncs];
		slvrDerivFuncs = new Function[numSlvrFuncs];
		slvrFuncs[0] = xAra -> {//1 + sine cdf model
			double a = xAra[0], b=xAra[1], c=xAra[2], x=xAra[3];
			return a*(1 + Math.sin(b * (x - c)));
		};		
		slvrDerivFuncs[0]= xAra -> {//1 + sine cdf model a*(1+sin(b*(x-c)))
			double a = xAra[0], b=xAra[1], c=xAra[2], x=xAra[3];
			double xmc = (x-c), bXmC = b*xmc, cosVal = Math.cos(bXmC);
			return new double[] {
					(1 + Math.sin(bXmC)),	//dA
					a*xmc*cosVal,			//dB
					-a*b*cosVal				//dC					
			};
		};		
		slvrFuncs[1] = xAra -> {//x + sine cdf model :  a *(Math.PI/b + (x-c) + (Math.sin(b * (x - c))/b));
			double a = xAra[0], b=xAra[1], c=xAra[2], x=xAra[3],xmc = (x-c);
			return a *(xmc + (Math.PI + Math.sin(b * xmc))/b);
		};		
		slvrDerivFuncs[1]= xAra -> {///CDF func : a *(Math.PI/b + (x-c) + (Math.sin(b * (x - c))/b));
			double a = xAra[0], b=xAra[1], c=xAra[2], x=xAra[3],xmc = (x-c), piOvB = Math.PI/b, bXmC = b*xmc, sinVal = Math.sin(bXmC), cosVal = Math.cos(bXmC);
			return new double[] {
					piOvB + sinVal/b + xmc,						//dA = sin(B * val + C)/B + x
					a * (-Math.PI + bXmC * cosVal - sinVal)/(b*b),	//dB 
					a*(-1 - cosVal)									//dC					
			};
		};		
	}//buildSlvrFuncs

	private void printXYVals(DoubleMatrix xVals) {		System.out.println("xVals : " + xVals);	}
	

	//for equation 1/2  + 1/2 sin(b*(x-phi)) - this doesn't integrate into an appropriate cosine
	private void deriveCoeffs_onePSine(TreeMap<Double,Double> CDFMap, TreeMap<Double,Double> CDFMapP1) {	
		TreeMap<Double,Double> mapToUse = CDFMap;
		//EQ is A*(1 + sin(b * (x - phi)))
		//can derive offest analytically since last value in cdf has cdf value 1 - how much we want to shift to get sine to be 1
		//wavelength == 
		double eps = .000000001;
		//double lastVal = mapToUse.get(mapToUse.lastKey());		//where sine is 1	
		double y1 = mapToUse.floorKey(mapToUse.lastKey() - eps), y2 = mapToUse.ceilingKey(mapToUse.firstKey() + eps), 
				x1 = mapToUse.get(y1), x2 = mapToUse.get(y2), asinY1 = Math.asin(2*y1-1); 			
		
//		double freqMult = (Math.asin((2*y2)-1.0) - halfPi)/(x2 - lastVal);
//		double phi = lastVal - (halfPi/freqMult);
		double freqMult = (Math.asin(2*y2-1) - asinY1)/(x2-x1);
		double phi = x1 - asinY1/freqMult;		
		
		DoubleMatrix thetaLcl = new DoubleMatrix(new double[] {.5,freqMult, phi});		
		actLBnd = (Math.asin(-1))/freqMult  + phi;
		actUBnd = (Math.asin(1))/freqMult  + phi;
		
		Theta = thetaLcl;
		Function func = slvrFuncs[cdf1pSine];
		double lbnd = calcActualBnd(func,Theta,0.0),
		ubnd = calcActualBnd(func,Theta,1.0);//CDFMap.get(CDFMap.lastKey());
		expMgr.dispMessage("myCosFuncFromCDF","deriveCoeffs_onePSine","# vals : " +mapToUse.size() +" y2 : " + y2 + " x2 : " + x2 + " | Theta values : A : " + Theta.get(0) + " |B : " + Theta.get(1) + " |C : " + Theta.get(2) + " : bnds : act : [" +actLBnd +", "+actUBnd +"] | iter :  [" +lbnd +", "+ubnd +"]",MsgCodes.info1,true);				
	}//deriveCoeffs
	
	//calculate residual values
	private DoubleMatrix calcRVal(Function<Double[], Double> func, DoubleMatrix yVals, DoubleMatrix xVals, DoubleMatrix Theta) {
		DoubleMatrix fXVals =DoubleMatrix.zeros(xVals.getRows(), 1);
		Double[] inVals = new Double[] { Theta.get(0), Theta.get(1),Theta.get(2),0.0};
		for (int i=0;i<xVals.getRows();++i) {	
			inVals[3]=xVals.get(i);
			fXVals.put(i, func.apply(inVals));//	fXVals.put(i, calcF_Theta(Theta,xVals.get(i)));	
		}		
		return yVals.sub(fXVals);
	}//calcRVal
	
	private DoubleMatrix calcJacobian(Function<Double[], double[]> derivFunc, DoubleMatrix xVals, DoubleMatrix Theta) {
		DoubleMatrix J = new DoubleMatrix(xVals.length, Theta.length);
		Double[] inVals = new Double[] { Theta.get(0), Theta.get(1),Theta.get(2),0.0};
		for (int i=0;i<xVals.getRows();++i) {		
			inVals[3]=xVals.get(i);
			J.putRow(i, new DoubleMatrix(derivFunc.apply(inVals)));	
		}
		return J;
	}//calcJacobian
	
	//need to derive coefficients through Newton Method
	private void deriveCoeffs(TreeMap<Double,Double> CDFMap, TreeMap<Double,Double> CDFMapP1, int slvrIDX) {		
		TreeMap<Double,Double> mapToUse = CDFMap;
		int numIters = 1000;
		double alpha = .02;//learning rate
		DoubleMatrix rVals = new DoubleMatrix(mapToUse.size(), 1),//rSqVals = new DoubleMatrix(CDFMap.size(), 1), 
				yVals= new DoubleMatrix(mapToUse.size(), 1), 
				xVals= new DoubleMatrix(mapToUse.size(), 1);
		//initial values
		DoubleMatrix thetaLcl = new DoubleMatrix(new double[] {.5,twoPi,.5}), 
				dTheta, oldDTheta = new DoubleMatrix(new double[] {0,1,1});
		int row = 0;
		for(Double key : mapToUse.keySet()) {		yVals.put(row, key);		xVals.put(row, mapToUse.get(key));		++row;	}
		//Theta == [A,B,C]^T; dTheta == [J^T * J]^-1 * J^T * r
		//newTheta = Theta + alpha * dTheta
		
		boolean done = false;
		int iter = 0;
		while ((!done) && (iter < numIters)){
			//calc Res Value : yVal - f(theta,xVal)
			rVals = calcRVal(slvrFuncs[slvrIDX],yVals, xVals, thetaLcl);
			//calculate Jacobians for each point
			DoubleMatrix J = calcJacobian(slvrDerivFuncs[slvrIDX],xVals, thetaLcl), //tmpVal = J.transpose().mmul(J), 
					tmpVal2 = Solve.pinv(J.transpose().mmul(J)).mmul(J.transpose());
			//System.out.println("iter : " + iter + " rVals : " +rVals.toString() +" rows : " + rVals.getRows()+ " J numrows : " + J.getRows() + " | J : " +J.toString() +" \ntmpVal : " +tmpVal.toString() +" \n tmpVal2 : " +tmpVal2.toString() +" \n tmpVal2 rows : " +tmpVal2.getRows()+ " \n ");
			//printXYVals(xVals);			
			dTheta = tmpVal2.mmul(rVals);
			if (isNewtonDone(dTheta.sub(oldDTheta))) {	done = true;}
			oldDTheta = dTheta;				
			thetaLcl.addi(dTheta.mul(alpha));	
			//modify Theta
			//System.out.println("Final Iters :  " + iter + " thetaLcl values : A : " + thetaLcl.get(0) + " |B : " + thetaLcl.get(1) + " |C : " + thetaLcl.get(2) + "\t| dTheta values : A : " + dTheta.get(0) + " |B : " + dTheta.get(1) + " |C : " + dTheta.get(2)  );
			
			++iter;
		}//iterative loop
		Theta = thetaLcl;
		
		actLBnd = calcActualBnd(slvrFuncs[slvrIDX],Theta,0.0);
		actUBnd = calcActualBnd(slvrFuncs[slvrIDX],Theta,1.0);//CDFMap.get(CDFMap.lastKey());
		if(actLBnd > actUBnd) {actLBnd -= twoPi/Theta.get(1);}
		expMgr.dispMessage("myCosFuncFromCDF","deriveCoeffs","# vals : " +mapToUse.size() +" | Final Iters :  " + iter + " Theta values : A : " + Theta.get(0) + " |B : " + Theta.get(1) + " |C : " + Theta.get(2)+" | lbnd : " + actLBnd + " | ubnd : " + actUBnd,MsgCodes.info1,true);	
			
	}//deriveCoeffs
	
	//find inverse CDF value -> x value such that CDF(X<= x) == p
	//prob - lbnd == 0; hbnd == 1
	private double calcActualBnd(Function<Double[], Double> func, DoubleMatrix theta, double prob) {
		double xVal = 1.0 - prob, calcPVal = 0, diff;
		Double[] inVals = new Double[] { theta.get(0), theta.get(1),theta.get(2),0.0};
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			inVals[3]=xVal;
			calcPVal = func.apply(inVals);
			diff = prob -calcPVal;
			if(Math.abs(diff) < convLim) {				done=true;			}
			xVal += .2*diff;
			++i;
		}//
		//System.out.println("Final InvCDF val : iters " + i + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " prob(xVal) : " +  String.format("%3.8f", calcPVal)+ " lbnd : " +  String.format("%3.8f", lBndVal));
		return xVal;
	}//calcInvCDF
	
	@Override
	protected void rebuildFuncs_Indiv() {
		if(slvrFuncs == null) {buildSlvrFuncs();}
		//get CDF map of data  where key is p(X<=x) and value is x
		TreeMap<Double,Double> CDFMap, CDFMapP1 = new TreeMap<Double,Double>();		
		try {
			TreeMap<Double,Double>[] res = summary.getCDFOfData();
			CDFMap = res[0];
			CDFMapP1 = res[1];
			if(CDFMap.size() < 2) {
				System.out.println("CDFMap.size() < 2 : " +CDFMap.size());
				throw new Exception();
			}
			dbgDispCDF(CDFMap,"myCosFuncFromCDF div n");
			//dbgDispCDF(CDFMapP1,"myCosFuncFromCDF div n+1");
		} catch (Exception e) {//not enough values to build a cdf here
			e.printStackTrace();
			return;
		}
		
		if(CDFToUse==0) {		deriveCoeffs_onePSine(CDFMap, CDFMapP1);} 
		else {
			deriveCoeffs(CDFMap, CDFMapP1,CDFToUse);
		}
	}//rebuildFuncs_Indiv

	//uses a(x + 1/b * sin(b*(x-c)) as CDF
	@Override
	protected void buildFuncs() {
		if(CDFToUse==cdfXpSine) {
			//actual functions  Theta.get(0) * (Math.sin(Theta.get(1) * x + Theta.get(2)) + 1);
			funcs[fIDX] 		= x ->  {
				//if(x<=actLBnd) {return 0.0;} else if(x>= actUBnd) {return 1.0;}
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return a * (1+Math.cos(b*(x - c))); };
			funcs[fInvIDX] 		= xinv -> {
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (Math.acos((xinv/a) - 1.0)/b + c);};
	//		//zigurat functions -> want 0 mean 1 std distribution
			funcs[fStdIDX]		= funcs[fIDX];
			funcs[fInvStdIDX]	= funcs[fInvIDX];
	//		//analytical derivatives
			funcs[fDerivIDX]	= x -> {
				//if(x<=actLBnd) {return 0.0;} else if(x>= actUBnd) {return 1.0;}
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (a *  (- b * Math.sin(b * (x - c))));};    
			funcs[fStdDeriveIDX] = funcs[fDerivIDX];                                                         ;
			//integrals - solve analytically
			// Theta.get(0) * (Math.sin(Theta.get(1) * x + Theta.get(2)) + x)
			//a *(Math.PI/b + (x-c) + (Math.sin(b * (x - c))/b));
			integrals[fIntegIDX] = x -> {
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (a * ( (x[1]-x[0]) +  (Math.sin(b * (x[1] - c)) - Math.sin(b * (x[0] - c)))/b ));};
		} else {
			//actual functions  Theta.get(0) * (Math.sin(Theta.get(1) * x + Theta.get(2)) + 1);
			funcs[fIDX] 		= x ->  {
				//if(x<=actLBnd) {return 0.0;} else if(x>= actUBnd) {return 1.0;}
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (a*b)*Math.cos(b*(x - c)); };
			funcs[fInvIDX] 		= xinv -> {
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return ((Math.acos(xinv/(a*b) ) + b*c)/b); };
//			//zigurat functions -> want 0 mean 1 std distribution
			funcs[fStdIDX]		= funcs[fIDX];
			funcs[fInvStdIDX]	= funcs[fInvIDX];
//			//analytical derivatives
			funcs[fDerivIDX]	= x -> {
				//if(x<=actLBnd) {return 0.0;} else if(x>= actUBnd) {return 1.0;}
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (a *  (- b*b * Math.sin(b * (x - c))));};    
			funcs[fStdDeriveIDX] = funcs[fDerivIDX];                                                         ;
			//integrals - solve analytically
			// Theta.get(0) * (Math.sin(Theta.get(1) * x + Theta.get(2)) + x)
			integrals[fIntegIDX] = x -> {double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); return (a * (Math.sin(b * (x[1] - c)) - Math.sin(b * (x[0] - c))) );};
			
		}
		expMgr.dispMessage("myCosFuncFromCDF","buildFuncs","LBnd : " + actLBnd + " UBnd : " + actUBnd,MsgCodes.info1,true);	
	}//

	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds(int funcType) {
		if(funcType==queryInvCDFIDX) {	return  new double[] {0.0,1.0};	}
		//double mu = summary.mean(), std = summary.std();
		// TODO Auto-generated method stub
		return new double[] {actLBnd, actUBnd};
	}//getPlotValBounds
	//pdf - described for this kind of rnd func by f
	@Override
	public double PDF(double x) {	return f(x);}
	
	//find CDF value of x; x must be within bounds mu-xBnd to mu+xBnd
	//CDF is integral from -inf to x of pdf - can be solved analytically 
	@Override
	public double CDF(double x) {
		//expMgr.dispMessage("myRandVarFunc", "CDF", "Begin CDF calc for val : " + String.format("%3.8f", x) , true);
		double newX = x;//forceInBounds(x,actLBnd, actUBnd);
		
		double  res = integrals[fIntegIDX].apply(new Double[] {actLBnd, newX});
		//double res = integral_f(actLBnd, x);		 
		//expMgr.dispMessage("myRandVarFunc", "CDF", "End CDF calc for val : " + String.format("%3.8f", x) , true);
		return res;
	}//CDF
	
	//find inverse CDF value -> x value such that CDF(X<= x) == p - func is internal
	private double calcInvCDF(Function<Double[], Double> func, double p, DoubleMatrix theta, double lbnd) {
		double xVal = 0, calcPVal = 0, diff;
		Double[] inVals = new Double[] { theta.get(0), theta.get(1),theta.get(2),lbnd};
		
		double lBndVal = func.apply(inVals);
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			inVals[3]=xVal;
			calcPVal = func.apply(inVals) - lBndVal;//solve for std value - ignore mu
			diff = p - calcPVal;
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
		double res = calcInvCDF(this.slvrFuncs[CDFToUse],p,Theta, actLBnd);
		//double res = calcInvCDF(p, halfAmpl, freqMult,  actLBnd, summary.mean());
		//expMgr.dispMessage("myCosFunc", "CDF_inv", "Finish CDF_inv calc for prob : " + String.format("%3.8f", p) + "\t stdzd res : " + String.format("%3.8f",res)+ "\t low xBnd1Std : " + String.format("%3.8f", -xBnd1Std), true);			
		return res;//processResValByMmnts(res);
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
	//assume we can modify value in similar way to transform by 1st 2 moments
	public double processResValByMmnts(double val) {	return summary.normToGaussTransform(val);}//public abstract double processResValByMmnts(double val);
	
	//
	@Override
	public void setOptionFlags(int[] _opts) {
		CDFToUse = (_opts[0] == 0 ? 0 : 1);//restrict to be 0 or 1
	}//setOptionFlags
	@Override
	public int getRVFType() {return BaseProbExpMgr.cosCDFRandVarIDX;}

		
}//myCosFuncFromCDF


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
			func.expMgr.dispMessage("zigConstVals", "z_R", func.getShortDesc()+ "| Initial Ziggurat R val chosen to be too high, causing integration to yield a negative volume due to error",MsgCodes.error1,true);
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

