package graphProbExp_PKG;
/**
 * instances of this class will analyze and display probability results
 * @author john
 *
 */
public class myProbAnalysis{
	private GraphProbExpMain pa;
	//values to analyze
	private double[] vals;
	//generator ref
	private myRandGen gen; // if null then means we're using threadlocalrandom
	//mean and std of this distribution.  mean, std, var may be null/undefined for certain distributions (i.e. cauchy)
	protected Double[] mmnts;
	public static final int 
		meanIDX			= 0, 
		stdIDX			= 1, 
		varIDX			= 2,
		skewIDX			= 3,
		kurtIDX			= 4,
		excKurtIDX		= 5;
	public static final int numMmnts = 6;
	public static final String[] mmntLabels = new String[] {"Mean","STD","Variance","Skew","Kurtosis", "Excess Kurtosis"};
	
	public myProbAnalysis(GraphProbExpMain _pa, double[] _vals, myRandGen _gen) {
		pa=_pa;	setValsAndAnalyse(_vals); gen=_gen;
	}//ctor
	
	//using Kahan summation to minimize errors
	public void setValsAndAnalyse(double[] _vals) {
		mmnts = new Double[numMmnts];  
		for(int i=0;i<numMmnts;++i) {mmnts[i]=0.0;}
		vals=_vals;
		int numVals = vals.length;
		if(numVals ==0 ) {return;}
		///calculate mean while minimizing float error
		double sumMu = vals[0];
		double cMu = 0.0, y, t;
		for(int i=1;i<vals.length;++i) {
			y = vals[i] - cMu;
			t = sumMu + y;
			cMu = (t-sumMu) - y;
			sumMu = t;
		}//		
		mmnts[meanIDX] = sumMu/numVals;
		//calculate variance/std while minimizing float error
		//double sumVar = (vals[0] - mmnts[meanIDX])*(vals[0] - mmnts[meanIDX]);
		double tDiff, tDiffSq;
		double valMMean = (vals[0] - mmnts[meanIDX]);
		double [] sumAndCSq = new double[] {valMMean*valMMean, 0.0};
		double [] sumAndCCu = new double[] {(sumAndCSq[0])*valMMean, 0.0};
		double [] sumAndCQu = new double[] {(sumAndCSq[0])*(sumAndCSq[0]), 0.0};
		for(int i=1;i<vals.length;++i) {
			tDiff = vals[i] - mmnts[meanIDX];
			tDiffSq = (tDiff*tDiff);
			calcSumAndC(sumAndCSq,tDiffSq - sumAndCSq[1]);
			calcSumAndC(sumAndCCu,(tDiffSq*tDiff) - sumAndCCu[1]);
			calcSumAndC(sumAndCQu,(tDiffSq*tDiffSq) - sumAndCQu[1]);
		}//		
		mmnts[varIDX] = sumAndCSq[0] / numVals;
		mmnts[stdIDX] = Math.sqrt(mmnts[varIDX]);
		mmnts[skewIDX] = (sumAndCCu[0] / numVals)/(mmnts[stdIDX]*mmnts[varIDX]);
		mmnts[kurtIDX] = (sumAndCQu[0] / numVals)/(mmnts[varIDX]*mmnts[varIDX]);
		mmnts[excKurtIDX] = mmnts[kurtIDX]-3.0;
		
	}//setVals
	
	private void calcSumAndC(double[] sumAndC, double y) {
		double t = sumAndC[0] + y;
		sumAndC[1] = (t-sumAndC[0]) - y;
		sumAndC[0] = t;
	}
	
	public String getMoments() {
		String res = "# vals : " +vals.length;
		for (int i=0;i<mmntLabels.length;++i) {	res += " | " + mmntLabels[i] + " = "+String.format("%.8f",mmnts[i]);	}
		return res;
	}
	

}//class myProbAnalysis
