package base_ProbTools.randGenFunc.transform;

import java.util.TreeMap;

import base_ProbTools.randGenFunc.transform.base.baseTransform;
import base_StatsTools.summary.myProbSummary_Dbls;

/**
 * maps each grade to a specific location based on its order - does not care about original grade value, just uses rank
 * @author 7strb
 *
 */
public class uniformCountTransform extends baseTransform{
	//# of -unique- grades
	int count;
	//grades sorted in ascending order
	TreeMap<Double, Integer> sortedGrades;
	//ranks and actual grade values
	TreeMap<Integer, Double> rankedGrades;	
	//summary must be built by data and have data vals
	public uniformCountTransform(myProbSummary_Dbls _summary) {
		super("Uniform Count Transform Mapping", _summary);
	}

	//This object MUST have vals, so that grades can be sorted;
	//for final grade roster, this object must have updated summary object
	@Override
	public void _setFuncSummaryIndiv() {
		//when summary is set, need to add all grades in ascending order to sortedGrades
		sortedGrades = new TreeMap<Double, Integer>();
		rankedGrades = new TreeMap<Integer, Double>();
		double [] vals = summary.getDataVals();
		//place in grade map
		for (double val : vals) {			sortedGrades.put(val, 0);		}
		//find count
		count = sortedGrades.size();
		//System.out.println("Vals size : " + vals.length+" uniformCountTransform : This object has :"+ count+" elements");
		//for (double val : vals) {System.out.println("\t"+val);}
		//place count in sorted map - treats grades of same value as same grade
		int idx =0;
		for(double val : sortedGrades.keySet()) {		rankedGrades.put(idx, val);	sortedGrades.put(val, idx++);	}//start with 1
	}
	
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
	public double getSample() {	
		if(count==0) {return 0;}
		int rank = getUniInt(0,count);
		double val = rank;//rankedGrades.get(rank);		
		return val;}
	@Override
	public double getSampleFast() {return getSample();}
	
	//provides mapping from rank/n to original grade
	@Override
	public double inverseCDF(double _val) {	
		int desKey = (int)(((_val) * count) - 1.0/count);
//		System.out.println("Wanting inv cdf of _val == " + _val + " des key : " + desKey+ " currently contains : ");
//		for(int key : rankedGrades.keySet()) {
//			System.out.println("Key : " + key + " | Val :  "+ rankedGrades.get(key));
//		}
		//update every time?
		_setFuncSummaryIndiv();
		return rankedGrades.get(desKey);	
	}
	//provides mapping from original grade to rank/n (0->1)
	
	@Override
	public double CDF(double _val) {		
		//update every time?
		_setFuncSummaryIndiv();
		Integer retRank = sortedGrades.get(_val);
		if(null==retRank) {	retRank = sortedGrades.get(sortedGrades.ceilingKey(_val));	}
		return (1.0 * retRank+1)/count;	
	}
	
	@Override
	public String _getTransformNameIndiv() {return "Uniformly Ranked | # of unique grades : " + count;	}	
	
}//class uniformCountTransform