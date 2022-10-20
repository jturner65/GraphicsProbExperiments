package base_ProbTools.randGenFunc.transform;

import base_ProbTools.randGenFunc.transform.base.baseTransform;
import base_StatsTools.summary.myProbSummary_Dbls;

//min/max to 0->1 -> using this method to facilitate implementing structure for trivial examples - 
//will never generate values, nor will it ever access a random variable function.
//only maps values via affine transformation to 0->1
public class linearTransform extends baseTransform{
	//these are of given data
	double min, max, diff;
	//summary must have min and max
	public linearTransform( myProbSummary_Dbls _summary) {
		super( "Linear Transform Mapping", _summary);		
	}//ctor
	//called whenever summary object is set/reset
	@Override
	public void _setFuncSummaryIndiv() {	
		min = summary.getMin();
		max = summary.getMax();
		diff = max - min;
		if(diff == 0) {//should never happen - give error if it does
			System.out.println("The linear transform " + name + " must have min != max.  Min and max being set to 0 and 1");
			min = 0;
			max = 1.0;
		}
	}
	//really just provides mapping from 0->1 to original span 
	@Override
	public double inverseCDF(double _val) {return (diff*_val)+min;}

	@Override
	public double CDF(double _val) {		return (_val - min)/diff;}
	
	//transform "randGen" objects are actually intended only as a mappers,so never going to ever generate any values
	@Override
	public double[] getMultiSamples(int num) {	
		double[] res = new double[num];
		for(int i=0;i<num;++i) {res[i] = getSample();}		
		return res;		
	}
	@Override
	public double[] getMultiFastSamples(int num) {return getMultiSamples(num);}
	@Override
	public double getSample() {	return getUniform01();}
	@Override
	public double getSampleFast() {return getSample();}

	@Override
	public String _getTransformNameIndiv() {		return "|Linear Transform | Min : "+ String.format("%3.8f", min) + " | Max : "+ String.format("%3.8f", max);	}
	
}//class linearTransform