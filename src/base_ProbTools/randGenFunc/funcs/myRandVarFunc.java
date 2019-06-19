package base_ProbTools.randGenFunc.funcs;

import java.math.*;
import java.util.TreeMap;
import java.util.function.Function;
import base_Utils_Objects.*;

import org.jblas.*;

import base_ProbTools.BaseProbExpMgr;
import base_ProbTools.myIntegrator;
import base_ProbTools.myProbSummary;
import base_ProbTools.randGenFunc.zigConstVals;
import base_ProbTools.randVisTools.myDistFuncHistVis;

/**
 * classes to provide the functionality of a random variable to be consumed by the random number generators.
 * These classes will model the pdf and inv pdf of a particular random variable, as well as provide access to integration (to derive CDF)
 * 
 * @author john
 *
 */
public abstract class myRandVarFunc {
	public static MessageObject msgObj;
	//public static BaseProbExpMgr expMgr;
	//descriptive name of function
	public final String name;	
	//quadrature solver for this random variable/function
	protected myIntegrator quadSlvr;	
	//object to hold descriptive values and statistics for this distribution, and any source data/samples, if they exist
	protected myProbSummary summary;	
	//convergence limit for iterative calcs
	public static final double convLim=1e-6;	
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
	
	public myRandVarFunc(myIntegrator _quadSlvr, myProbSummary _summaryObj, String _name) {
		if(null==msgObj) {msgObj = MessageObject.buildMe();}
		
		name=_name;
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
	
	public myProbSummary getSummary() {return summary;}
	public myIntegrator getQuadSlvr() {return quadSlvr;}
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
			msgObj.dispMessage("myRandVarFunc", "getFuncVal", "Attempting to evaluate unknown func type : " + funcType +" on value(s) : [" + x1 + ", "+ x2 + "] Aborting.",MsgCodes.warning1 , true);
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
			msgObj.dispMessage("myRandVarFunc", "getFuncVal", "Attempting to name unknown func type : " + funcType +". Aborting.",MsgCodes.warning1 , true);
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
	
	//calculate integral of f between x1 and x2.  Use to calculate cumulative distribution by making x1==-inf, and x2 definite; qfunc by setting x1 to a value and x2 == +inf
	public abstract double integral_f(Double x1, Double x2);	
	//calculate integral of normalized f (for ziggurat calc) between x1 and x2.  Use to calculate cumulative distribution by making x1==-inf, and x2 definite; qfunc by setting x1 to a value and x2 == +inf
	public abstract double integral_fStd(Double x1, Double x2);	

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
		//System.out.println("Final InvCDF val : iters " + i + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " prob(xVal) : " +  String.format("%3.8f", calcPVal)+ " lbnd : " +  String.format("%3.8f", lbnd));
		return xVal;
	}//calcInvCDF
	
	//find inverse  value -> x value such that CDF(X<= x) == p
	//Lower bound should either be neg inf or the lower bound of the pdf, if it is bounded
	//include derivative function
	public double calcInvCDF_Newton(double p, Function<Double[], Double> integralFunc,  Function<Double, Double> derivFunc, Double lbnd) {
		double xVal = 0, xVal1 = 0, calcPVal = 0, calcDPVal = 0, del,diff;
		Double[] args = new Double[] {lbnd, xVal};
		//double lBndVal = a*(lbnd + Math.sin(freq*lbnd)/freq);
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			//calcPVal = a*(xVal + Math.sin(freq*(xVal-mu))/freq) - lBndVal;//solve for std value - ignore mu
			args[1]=xVal;
			calcPVal = integralFunc.apply(args) - p;//solve for std value - ignore mu
			calcDPVal = derivFunc.apply(xVal);
			del = calcPVal/calcDPVal;
			xVal1 = xVal - del;
			diff = xVal - xVal1;
			//System.out.println("iter " + i + " diff : " + String.format("%3.8f", diff) + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " sinFreqS : "+ String.format("%3.8f", calcPVal)+ " deriv : "+ String.format("%3.8f", calcDPVal));
			if(Math.abs(diff) < convLim) {				done=true;			}
			xVal = xVal1;
			++i;
		}//
		//System.out.println("Final InvCDF val : iters " + i + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " prob(xVal) : " +  String.format("%3.8f", calcPVal)+ " lbnd : " +  String.format("%3.8f", lbnd));
		return xVal;
	}//calcInvCDF
	
	//process a result from a 0-centered, 1-std distribution to match stated moments of this distribution
	public abstract double processResValByMmnts(double val);	
	
	//if this rand var is going to be accessed via the ziggurat algorithm, this needs to be called w/# of rectangles to use
	//this must be called after an Quad solver has been set, since finding R and Vol for passed # of ziggurats requires such a solver
	public void setZigVals(int _nRect) {
		if (!getFlag(quadSlvrSetIDX)) {	msgObj.dispMessage("myRandVarFunc", "setZigVals", "No quadrature solver has been set, so cannot set ziggurat values for "+_nRect+" rectangles (incl tail).",MsgCodes.warning1,true); return;}
		double checkRect = Math.log(_nRect)/ln2;
		int nRectCalc = (int)Math.pow(2.0, checkRect);//int drops all decimal values
		if (_nRect != nRectCalc) {	
			int numRectToUse = (int)Math.pow(2.0, (int)(checkRect) + 1);
			msgObj.dispMessage("myRandVarFunc", "setZigVals", "Number of ziggurat rectangles requested " + _nRect + " : " + nRectCalc + " must be an integral power of 2, so forcing requested " + _nRect + " to be " + numRectToUse,MsgCodes.warning1,true);
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
		msgObj.dispMessage(callingClass,"dbgDispCDF","CDF Values : ",MsgCodes.warning1,true);
		for(Double key : map.keySet()) {msgObj.dispMessage(callingClass,"dbgDispCDF","\tKey : " + key +" | CDF Val : " + map.get(key),MsgCodes.warning1,true);}
		
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






