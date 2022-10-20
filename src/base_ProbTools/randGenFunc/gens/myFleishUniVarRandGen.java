package base_ProbTools.randGenFunc.gens;

import base_ProbTools.randGenFunc.funcs.myFleishFunc_Uni;
import base_ProbTools.randGenFunc.funcs.myNormalFunc;
import base_ProbTools.randGenFunc.funcs.base.baseRandVarFunc;
import base_ProbTools.randGenFunc.gens.base.myRandGen;

/**
 * class that will model a distribution using first 4 moments via a polynomial transformation
 * @author john *
 */
public class myFleishUniVarRandGen extends myRandGen{
	
	//generator to manage synthesizing normals to feed fleishman
	private myZigRandGen zigNormGen;
	
	
	//min and max of synthesized
	public myFleishUniVarRandGen(baseRandVarFunc _func, String _name) {
		super(_func, _name);
		//need to build a source of normal random vars
		zigNormGen = new myZigRandGen(new myNormalFunc(func.getQuadSlvr()), 256, "Ziggurat Algorithm");		
	}//ctor
	
	//if summary object is changed, new fleishman polynomial values need to be synthesized - this is done already in calling function 
	//when func.rebuildFuncs is called	
	@Override
	public void _setFuncSummaryIndiv() {	}
	//test function to test iterative method to derive fl inverse
	public double calcInvFuncVal(double y) {
		return ((myFleishFunc_Uni)func).calcInvF(y);
	}
	
	
	@Override
	public double[] getMultiSamples(int num) {
		double[] res = new double[num];
		double val;
		boolean clipRes = summary.doClipAllSamples();
		//transformation via mean and std already performed as part of f function
		if (clipRes){
			int idx = 0;
			while (idx < num) {		val = func.f(zigNormGen.getSample());		if(summary.checkInBnds(val)) {				res[idx++]=val;	}}
		} else {					for(int i =0;i<res.length;++i) {		res[i]=func.f(zigNormGen.getSample());			}		}
		return res;
	}//getMultiSamples

	@Override
	public double[] getMultiFastSamples(int num) {
		double[] res = new double[num];
		boolean clipRes = summary.doClipAllSamples();
		//transformation via mean and std already performed as part of f function
		if (clipRes){
			int idx = 0;
			while (idx < num) {			double val = func.f(zigNormGen.getSampleFast());	if(summary.checkInBnds(val)) {				res[idx++]=val;	}}
		} else {						for(int i =0;i<res.length;++i) {	res[i]=func.f(zigNormGen.getSampleFast());			}		}
		return res;
	}//getMultiFastSamples
	
	@Override
	public double getSample() {
		double res;
		if(summary.doClipAllSamples()) {
			do {				res = func.f(zigNormGen.getSample());			} while (!summary.checkInBnds(res));
		} else {				res = func.f(zigNormGen.getSample());			}
		return res;

	}//getSample

	@Override
	public double getSampleFast() {
		double res;
		if(func.getSummary().doClipAllSamples()) {
			do {				res = func.f(zigNormGen.getSampleFast());		} while (!summary.checkInBnds(res));
		} else {				res = func.f(zigNormGen.getSampleFast());		}
		return res;
		
//		double res = func.f(zigNormGen.getSampleFast());
//		return res;
	}//getSampleFast
	
	//find inverse CDF value for passed val - val must be between 0->1; value for which prob(x<=value) is _pval
	//this is mapping from 0->1 to probability based on the random variable function definition
	@Override
	public double inverseCDF(double _pval) {
		return func.CDF_inv(_pval);
	}
	//find the cdf value of the passed val == returns prob (x<= _val)
	//for fleishman polynomial, these use the opposite mapping from the normal distrubtion - 
	//so for CDF, we want normal dist's inverse cdf of passed value passed to fleish CDF calc
	@Override
	public double CDF(double _val) {
		return func.CDF(_val);		
	}//CDF
	
}//class myFleishRandGen_old (used external zigNormGen
