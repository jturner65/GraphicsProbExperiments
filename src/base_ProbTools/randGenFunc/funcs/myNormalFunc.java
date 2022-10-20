package base_ProbTools.randGenFunc.funcs;

import base_ProbTools.baseProbExpMgr;
import base_ProbTools.quadrature.base.baseQuadrature;
import base_StatsTools.summary.myProbSummary_Dbls;

/**
 * instancing class for the function describing a normal random variable - explicitly mean == 0, var==std==1
 * @author john
 *
 */
public class myNormalFunc extends myGaussianFunc{	
	//////////////////////////////
	//zig algorithm fields for scaled normal - all myRandVarFuncs need their own impelemtnations of this map, independent of base class
	//////////////////////////////	
	public myNormalFunc(baseQuadrature _quadSlvr) {
		super(_quadSlvr, new myProbSummary_Dbls(new double[] {0.0, 1.0},2), "Normal");			
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
	public int getRVFType() {return baseProbExpMgr.normRandVarIDX;}
	@Override
	public void setOptionFlags(int[] _opts) {}

}//class myNormalFunc
