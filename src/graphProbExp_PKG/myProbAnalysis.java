package graphProbExp_PKG;
/**
 * instances of this class will analyze and display probablity results
 * @author john
 *
 */
public class myProbAnalysis{
	private GraphProbExpMain pa;
	//values to analyze
	private double[] vals;
	//mean, std
	private double mean, std, var;
	
	
	
	public myProbAnalysis(GraphProbExpMain _pa, double[] _vals) {
		pa=_pa;	setVals(_vals);
	}//ctor
	
	public void setVals(double[] _vals) {
		vals=_vals;
		mean = 0.0;
		double meanSq = 0.0;
		for(double x : vals) {	
			mean += x;
			meanSq += x*x;
		}
		mean /= vals.length;
		meanSq /= vals.length;
		var = meanSq - (mean*mean);
		std = Math.sqrt(var);
	}
	public String getMoments() {
		String res = "#vals : " +vals.length + "| Mean = "+String.format("%.7f",mean)+"| Var = " + String.format("%.7f",var) + "| Std  = " + String.format("%.7f",std);
		return res;
	}
	

}//class myProbAnalysis
