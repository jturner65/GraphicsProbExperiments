package base_ProbTools.randGenFunc.gens;

import base_ProbTools.randGenFunc.funcs.base.baseRandVarFunc;
import base_ProbTools.randGenFunc.gens.base.myRandGen;

/**
 * rand gen class for bounded pdfs (like cosine) - perhaps use variant of zigguarat instead of iterative convergence method to find inv-CDF?
 */
public class myBoundedRandGen extends myRandGen{

	public myBoundedRandGen(baseRandVarFunc _func, String _name) {
		super(_func, _name);
	}

	@Override
	public void _setFuncSummaryIndiv() {//no extra settings required		
	}

	@Override
	public double[] getMultiSamples(int num) {
		double[] res = new double[num];
		if(summary.doClipAllSamples()) {
			int idx = 0;
			double smpl;
			while (idx <= num){
				smpl=nextRandCosVal();
				if (summary.checkInBnds(smpl)){					res[idx++]=smpl;			}
			}//while			
		} else {
			for(int i=0;i<res.length;++i) {	res[i]=nextRandCosVal();}
		}
		return res;
	}//getNumSamples

	@Override
	public double[] getMultiFastSamples(int num) {return getMultiSamples(num);}//getNumFastSamples
	
    
	@Override
	public double getSample() {
		double res;
		if(summary.doClipAllSamples()) {
			do {		res = nextRandCosVal();	} while (!summary.checkInBnds(res));
		} else {
			res = nextRandCosVal();
		}
		return res;
	}//getGaussian
	
	//get a random value based on cosine pdf
	private double nextRandCosVal() {
		double res = func.CDF_inv(getNextDouble());
		return res;		
	}
	
	//int value
	@Override
	public double getSampleFast() {

		return getSample();
//		double res = nextNormal32();		
//		return func.processResValByMmnts(res);
	}//getGaussian

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
	
}//class myCosVarRandGen