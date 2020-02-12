package base_ProbTools.summary;

import java.util.TreeMap;

import base_ProbTools.summary.base.baseProbSummary;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;

/**
 * provides a summary of values' stats by using x,y,z summaries
 * @author john
 *
 */

public class myProbSummary_ptOrVec extends baseProbSummary {
	public final String name;
	/**
	 * arrays of float summaries either points, or vectors (With magnitude summary as well)
	 */
	protected myProbSummary_Flts[] fltSummaryAra;
	/**
	 * # of dimensions - either 3 for points or 4 for vectors
	 */
	protected int numDims;
	
	public static final int 
		x_Idx = 0,
		y_Idx = 1,
		z_Idx = 2,
		mag_Idx = 3;		//only used if vector values
	public static final String[] summaryNames = new String[] {"x","y","z","m"};
	/**
	 * 
	 * @param _ptVals
	 * @param _numDims 3 means data is points, 4 means data is vectors
	 */
	public myProbSummary_ptOrVec(String _name, myPointf[] _ptVals, int _numDims) {
		super();
		name=_name;
		numDims = _numDims;
		fltSummaryAra = new myProbSummary_Flts[numDims];
		float[][] valsToSmry;
		if(3==numDims) {valsToSmry = convPtAraToFloatAra(_ptVals);}
		else {			valsToSmry = convVecAraToFloatAra( _ptVals);}		
		
		for(int i=0;i<fltSummaryAra.length;++i) {
			fltSummaryAra[i] = new myProbSummary_Flts(name+"_"+i,valsToSmry[i]);
		}
		numVals = _ptVals.length;
	}
	
	/**
	 * momments are passed - no sample data exists numMmnts are # of moments actually specified
	 * @param _mmnts momments array of per-dim arrays
	 * @param _numMmnts # of moments actually specified
	 * @param isExKurt
	 * @param _numDims
	 */
	public myProbSummary_ptOrVec(String _name, float[][] _mmnts, int _numMmnts, boolean isExKurt, int _numDims) {
		super();
		name=_name;
		numDims = _numDims;
		fltSummaryAra = new myProbSummary_Flts[numDims];
		for(int i=0;i<fltSummaryAra.length;++i) {
			fltSummaryAra[i] = new myProbSummary_Flts(name+"_"+i,_mmnts[i], _numMmnts, isExKurt);
		}
		numVals = 0;	//no data values set
	}//ctor
	
	
	protected float[][] convPtAraToFloatAra(myPointf[] ptAra){
		float[][] res = new float[fltSummaryAra.length][ptAra.length];
		for(int j=0;j<res.length;++j) {res[j] = new float[ptAra.length];}
		float[] vals;
		for(int i=0;i<ptAra.length;++i) {
			vals=ptAra[i].asArray();			
			for(int j=0;j<vals.length;++j) {res[j][i]=vals[j];}
		}	
		return res;		
	}
	
	protected float[][] convVecAraToFloatAra(myPointf[] ptAra){
		float[][] res = new float[fltSummaryAra.length][ptAra.length];
		for(int j=0;j<res.length;++j) {res[j] = new float[ptAra.length];}
		float[] vals;
		for(int i=0;i<ptAra.length;++i) {
			vals=ptAra[i].asArray();			
			for(int j=0;j<vals.length;++j) {res[j][i]=vals[j];}
			res[vals.length][i] =((myVectorf) ptAra[i]).magn;
		}	
		return res;		
	}
	
	protected final boolean checkNotNullSummaryVals() {
		boolean res = true;
		for(int i=0;i<fltSummaryAra.length;++i) {if((fltSummaryAra[i].getDataVals() == null)) {return false;}}
		return res;
	}

	//build buckets of equally space buckets and counts in buckets
	//returns array of numBuckets, 2, where idx 0 is lower xVal of bucket, and idx 1 is count in bucket - last element has idx0 == max bucket limit, count 0 (none above max)
	public final float[][][] calcBucketVals(int numBuckets){
		//if no data, return empty array - need data to calculate this
		if ((!checkNotNullSummaryVals()) || (numVals == 0)) {return new float[0][0][0];}
		float[][][] results = new float[fltSummaryAra.length][numBuckets+1][2];
		for(int i=0;i<fltSummaryAra.length;++i) {	results[i]=fltSummaryAra[i].calcBucketVals(numBuckets);	}
		return results;
	}//calcBucketVals
	
	//this will force excess kurtosis to be passed value for fleishman polynomial
	//DANGER this may have unforseen effects, and should only be done with care.  this will require that sample vs. pop kurt values will be recalced, if data exists for them
	public void forceExKurt(myPointf _exKurt) {
		float[] exKurtVals = _exKurt.asArray();
		for(int i=0;i<fltSummaryAra.length;++i) {fltSummaryAra[i].forceExKurt(exKurtVals[i]);}
		setFlag(kurtIDX, true);
		setFlag(excKurtIDX, true);
		if(numVals > 0) {			calcSmpleMomentsForSampleSize();		} 
		else {							setFlag(smplValsCalcedIDX, false);		}			
	}//forceExKurt
	
	public boolean checkInBnds(float[] vals) {	
		for(int i=0;i<fltSummaryAra.length;++i) {			if(!fltSummaryAra[i].checkInBnds(vals[i])) {return false;}		}		
		return true;}
	
	/**
	 * set the min/max for a specific index in the data (x,y,z,mag(if vec))
	 * @param _min
	 * @param _max
	 * @param idx
	 */
	public final void setMinMax_Idx(float _min, float _max, int idx) {
		if((idx < 0)||(idx >= fltSummaryAra.length)) {return;}
		fltSummaryAra[idx].setMinMax(new float[] {_min,_max});
	}
	
	//set desired bounds on any distribution built from this data
	public final void setMinMax(float[] min, float[] max) {	for(int i=0;i<fltSummaryAra.length;++i) {setMinMax_Idx(min[i],max[i],i);}}
	public final void setMinMax(float[][] _minMax) {		for(int i=0;i<fltSummaryAra.length;++i) {setMinMax_Idx(_minMax[0][i], _minMax[1][i],i);}}
	
	public final void calcSmpleMomentsForSampleSize(){		for(int i=0;i<fltSummaryAra.length;++i) {fltSummaryAra[i].calcSmpleMomentsForSampleSize();}}
	//assuming calculated moments are population moments, modify popToSmpleMmntMults values so they can be used to calculate sample moments from pop momments
	public final void calcSmpleMomentsForSampleSize(int _smpleSize, float[][] _minMax) {
		for(int i=0;i<fltSummaryAra.length;++i) {			fltSummaryAra[i].calcSmpleMomentsForSampleSize(_smpleSize,new float[] {_minMax[0][i], _minMax[1][i]});}	
		sampleSize = _smpleSize;
		setFlag(smplValsCalcedIDX, true);
	}//calcSmpleMomentsForSampleSize
	
	
	//return CDF map of data, where key is p(X<=x) and value is x
	//idx 0 is biased; idx 1 is unbaised
	@SuppressWarnings("unchecked")
	public final TreeMap<Float,Float[]>[] getCDFOfData(){
		TreeMap<Float,Float>[][] summaryResults = getSummaryResults();
		
		TreeMap<Float,Float[]>[] res = new TreeMap[2];
		res[0] = new TreeMap<Float, Float[]>();
		res[1] = new TreeMap<Float, Float[]>();
		for(int i=0;i<summaryResults.length;++i) {//one per summary
			for(int j=0;j<summaryResults[i].length;++j) {//2
				for(Float key : summaryResults[i][j].keySet()) {
					res[j].put(key, new Float[summaryResults.length]);
				}					
			}
		}		
		for(int i=0;i<res.length;++i) {//2
			for (Float key : res[i].keySet()) {
				Float[] tmp = res[i].get(key);
				for(int j=0;j<tmp.length;++j) {			tmp[j]=summaryResults[j][i].floorKey(key);	}		
				res[i].put(key, tmp);
			}
		}	
		return res;
	}//getCDFOfData
	
	/**
	 * override this for vectors
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected final TreeMap<Float,Float>[][] getSummaryResults(){
		TreeMap<Float,Float>[][] summaryResults = new TreeMap[fltSummaryAra.length][];
		//has prob for x, y and z
		for(int i=0;i<fltSummaryAra.length;++i) {		summaryResults[i] = fltSummaryAra[i].getCDFOfData();	}
		return summaryResults;	
	}
	
	public final float[] mean() {
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].mean();}	
		return res;
	}
	public final float[] var() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].var();}	
		return res;
	}
	public final float[] std() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].std();}	
		return res;
	}
	public final float[] skew() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].skew();}	
		return res;
	}
	public final float[] kurt() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].kurt();}	
		return res;
	}
	public final float[] exKurt() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].exKurt();}	
		return res;
	}	
	public final float[] getMin() {
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].getMin();}		
		return res;
	}
	public final float[] getMax() {
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].getMax();}			
		return res;
	}	
	public float[][] getDataVals() {
		float[][] smryVals = new float[fltSummaryAra.length][];
		for(int i=0;i<fltSummaryAra.length;++i) {smryVals[i] = fltSummaryAra[i].getDataVals();}
		return smryVals;
	}
	
	//get moments of sampled data, if specified that given moments are of underlying population
	public final float[] smpl_mean() {
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].smpl_mean();}
		return res;
	}
	public final float[] smpl_var() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].smpl_var();}	
		return res;
	}
	public final float[] smpl_std() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].smpl_std();}	
		return res;
	}
	public final float[] smpl_skew() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].smpl_skew();}
		return res;
	}
	public final float[] smpl_kurt() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].smpl_kurt();}	
		return res;
	}
	public final float[] smpl_exKurt() { 
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].smpl_exKurt();}
		return res;
	}
	
	//transform the given value via the 1st 2 moments of the distribution described by these statistics from a normal ~N(0,1) 
	public myPointf normToGaussTransform(myPointf val) {float[] res = normToGaussTransform(val.asArray());return new myPointf(res[0],res[1],res[2]);}	
	public final float[] normToGaussTransform(float[] vals) {		
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].normToGaussTransform(vals[i]);}	
		return res;
	}

	//transform the give value, assumed to be from the distribution described by this object, to a normal ~N(0,1) 
	public myPointf gaussToNormTransform(myPointf val) {float[] res = gaussToNormTransform(val.asArray());return new myPointf(res[0],res[1],res[2]);}
	public final float[] gaussToNormTransform(float[] vals) {
		float[] res = new float[fltSummaryAra.length];
		for(int i=0;i<fltSummaryAra.length;++i) {res[i]=fltSummaryAra[i].gaussToNormTransform(vals[i]);}	
		return res;
	}	
	
	public myProbSummary_Flts[] getFltSummaryAra() {return fltSummaryAra;}
	
	/**
	 * return an array of summary statistics to make displaying the results easier
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<String,String>[] summaryStringAra() {
		TreeMap<String,String>[] resMap = new TreeMap[fltSummaryAra.length];		
		for(int i=0;i<fltSummaryAra.length;++i) {
			resMap[i] = fltSummaryAra[i].summaryStringAra(summaryNames[i]);
		}
		return resMap;		
	}//summaryStringAra()

	/**
	 * return an array of values scaled to lie between observed min and max, in sequence of original data
	 * @return
	 */
	public float[][] getScaledVals() {
		float[][] res = new float[fltSummaryAra.length][];
		for(int i=0;i<fltSummaryAra.length;++i) {	res[i]=fltSummaryAra[i].getScaledVals();}
		return res;
	}
	
	public float[][] getMinMax(){
		float[][] res = new float[fltSummaryAra.length][];
		for(int i=0;i<fltSummaryAra.length;++i) {	res[i]=fltSummaryAra[i].getMinMax();}
		return res;
	}
	
	/**
	 * don't use this one, use the one above
	 */
	@Override
	public TreeMap<String, String> summaryStringAra(String _notUsed) {
		return new TreeMap<String, String>();
	}
	
//	/**
//	 * return a single string per row of values, determined by key ara holding keys to include
//	 * @param _statKeyAra
//	 * @return
//	 */
//	public String[] summaryStringAra_Simple(String[] _statKeyAra) {
//		TreeMap<String,String>[] resMap = summaryStringAra();
//		String[] res = new String[resMap.length];
//		for(int i=0; i<res.length;++i) {
//			String tmpRes = resMap[i].get("summaryName");
//			for(int j=0;j<_statKeyAra.length-1;++j) {
//				tmpRes += resMap[i].get(_statKeyAra[j]) + " | ";
//			}
//			tmpRes += resMap[i].get(_statKeyAra[_statKeyAra.length-1]); 
//			res[i] = tmpRes;
//		}		
//		return res;
//	}
//	
	@Override
	public String toString() {
		String res = "Name " + name+" : # values : " +numVals + " # summaries : " + this.numDims+"\n";
		for(int i=0;i<fltSummaryAra.length;++i) {
			res += "\t"+String.format("%-3s", summaryNames[i])+" : "+fltSummaryAra[i].getMoments()+"\n";
		}
		
		return res;
	}


}//class myProbSummary_myPointf
