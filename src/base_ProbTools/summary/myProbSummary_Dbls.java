package base_ProbTools.summary;

import java.util.*;

import base_ProbTools.summary.base.baseProbSummary;

/**
 * instances of this class will analyze and display statistical results relating to a data set;  this will also provide an easy-access container to manage moments
 * @author john
 *
 */
public class myProbSummary_Dbls extends baseProbSummary{
	//values to analyze
	private double[] vals;
	
	//(population) moments of this distribution.  mean, std, var may be null/undefined for certain distributions (i.e. cauchy)
	protected double[] mmnts;
	//min and max values of data
	protected double[] minMax;
	//multipliers to derive sample moments from population moments
	protected double[] popToSmplMmntMults;

	//dataset is passed
	public myProbSummary_Dbls(double[] _data) {
		super();
		setValsAndAnalyse(_data);
	}//ctor
	
	//momments are passed - no sample data exists
	//numMmnts are # of moments actually specified
	public myProbSummary_Dbls(double[] _mmnts, int _numMmnts, boolean isExKurt) {
		super();
		setMoments(_mmnts, _numMmnts, isExKurt);	
		numVals = 0;	//no data values set
	}//ctor
	//if not specified then assume kurtosis passed is not excess but rather actual 
	public myProbSummary_Dbls(double[] _mmnts, int _numMmnts) {this(_mmnts,_numMmnts, false);}

	//using Kahan summation to minimize errors from large differences in value magnitude
	private void setValsAndAnalyse(double[] _vals) {
		setFlag(setByDataIDX, true);
		numVals = _vals.length;
		mmnts = new double[numTrackedMmnts];  
		popToSmplMmntMults = new double[mmnts.length];
		//array of values
		vals = _vals;
		
		for(int i=0;i<mmnts.length;++i) {mmnts[i]=0.0;}
		
		
		if(numVals ==0 ) {return;}
		///calculate mean while minimizing float error
		double sumMu = _vals[0], min = _vals[0], max = _vals[0];
		double cMu = 0.0, y, t;
		for(int i=1;i<numVals;++i) {
			min=(_vals[i]<min?_vals[i]:min);
			max=(_vals[i]>max?_vals[i]:max);
			
			y = _vals[i] - cMu;
			t = sumMu + y;
			cMu = (t-sumMu) - y;
			sumMu = t;
		}//		
		mmnts[meanIDX] = sumMu/numVals;
		//calculate variance/std while minimizing float error
		//double sumVar = (vals[0] - mmnts[meanIDX])*(vals[0] - mmnts[meanIDX]);
		double tDiff, tDiffSq;
		//initialize for Kahan summation method
		double valMMean = (_vals[0] - mmnts[meanIDX]);
		double [] sumAndCSq = new double[] {valMMean*valMMean, 0.0},
				sumAndCCu = new double[] {(sumAndCSq[0])*valMMean, 0.0},
				sumAndCQu = new double[] {(sumAndCSq[0])*(sumAndCSq[0]), 0.0};
		//kahan summation to address magnitude issues in adding 2 values of largely different magnitudes
		for(int i=1;i<numVals;++i) {
			tDiff = _vals[i] - mmnts[meanIDX];
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
		
		for(int i=0;i<mmnts.length;++i) {
			setFlag(i,true);
		}
		numMmntsGiven = 4;	
		//System.out.println("Data vals :  " + numVals + " var : " + mmnts[varIDX] + " std : " + mmnts[stdIDX] + " min : " + min + " | Max : " + max);
		calcSmpleMomentsForSampleSize(numVals,new double[] {min,max});
	}//setVals
	//convenience for kahan
	private void calcSumAndC(double[] sumAndC, double y) {
		double t = sumAndC[0] + y;
		sumAndC[1] = (t-sumAndC[0]) - y;
		sumAndC[0] = t;
	}//calcSumAndC
	
	//build buckets of equally space buckets and counts in buckets
	//returns array of numBuckets, 2, where idx 0 is lower xVal of bucket, and idx 1 is count in bucket - last element has idx0 == max bucket limit, count 0 (none above max)
	public double[][] calcBucketVals(int numBuckets){
		//if no data, return empty array - need data to calculate this
		if ((vals == null) || (numVals == 0)) {return new double[0][0];}
		double[][] results = new double[numBuckets+1][2];
		//split min->max into numBuckets partitions
		double diff = minMax[1] - minMax[0];
		double bucketDiff = diff/numBuckets;
		for(int i=0; i<results.length;++i) {
			results[i][0] = minMax[0] + i * bucketDiff;
		}
		for (int i=0;i<vals.length;++i) {
			double bktVal = (vals[i]-minMax[0])/bucketDiff;
			int idx = (int)(bktVal);
			//System.out.println("IDX found for # bkts : " +numBuckets + " bktVal : " + bktVal +" idx : " + idx +" orig val : " + vals[i] + " min : " + minMax[0] +" max : "+ minMax[1]);
			++results[idx][1];	
		}		
		return results;
	}//calcBucketVals
	
	//this will force excess kurtosis to be passed value for fleishman polynomial
	//DANGER this may have unforseen effects, and should only be done with care.  this will require that sample vs. pop kurt values will be recalced, if data exists for them
	public void forceExKurt(double _exKurt) {
		mmnts[excKurtIDX] = _exKurt;
		mmnts[kurtIDX] = mmnts[excKurtIDX] + 3;
		setFlag(kurtIDX, true);
		setFlag(excKurtIDX, true);
		if(numVals > 0) {			calcSmpleMomentsForSampleSize();		} 
		else {							setFlag(smplValsCalcedIDX, false);		}			
	}//forceExKurt
	
	public boolean checkInBnds(double x) {	return ((minMax[0] <= x) && (minMax[1] >= x));}
		
	//take passed moments (only canonical - no separate variance and ex kurtosis values) and set the internal moments of this analysis object
	//moments given are assumed to be of population, so sample multiplier is dependent on size of sample
	private void setMoments(double[] _mmnts, int _numMmntsGiven, boolean isExKurt) {
		vals = new double[0];
		setFlag(setByDataIDX, false);
		mmnts = new double[numTrackedMmnts];  
		setMinMax(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);		
		popToSmplMmntMults = new double[mmnts.length];
		numMmntsGiven = _numMmntsGiven;
		mmnts[meanIDX] = _mmnts[meanIDX];
		setFlag(meanIDX, true);
		if(numMmntsGiven > 1) {//has std
			mmnts[stdIDX] = _mmnts[stdIDX];
			setFlag(stdIDX, true);
			mmnts[varIDX]=mmnts[stdIDX]*mmnts[stdIDX];
			setFlag(varIDX, true);
			if(numMmntsGiven > 2) {//has skew
				mmnts[skewIDX] = _mmnts[skewIDX];
				setFlag(skewIDX, true);
				if(numMmntsGiven > 3) {//has kurtosis
					if (isExKurt) {
						mmnts[excKurtIDX] = _mmnts[kurtIDX];
						mmnts[kurtIDX] = mmnts[excKurtIDX] + 3;
						setFlag(kurtIDX, true);
						setFlag(excKurtIDX, true);
					} else {
						mmnts[kurtIDX] = _mmnts[kurtIDX];
						mmnts[excKurtIDX] = mmnts[kurtIDX]-3.0;
						setFlag(kurtIDX, true);
						setFlag(excKurtIDX, true);
					}
				}					
			}		
		}	
		setFlag(smplValsCalcedIDX, false);
	}//setMoments	
	
	//set desired bounds on any distribution built from this data
	public void setMinMax(double min, double max) {setMinMax(new double[] {min,max});}
	public void setMinMax(double[] _minMax) {	minMax = _minMax;}
	public double[] getMinMax() {return new double[] {minMax[0],minMax[1]};}

	public void calcSmpleMomentsForSampleSize(){calcSmpleMomentsForSampleSize(numVals, minMax);}
	//assuming calculated moments are population moments, modify popToSmpleMmntMults values so they can be used to calculate sample moments from pop momments
	public void calcSmpleMomentsForSampleSize(int _smpleSize, double[] _minMax) {
		sampleSize = _smpleSize;
		popToSmplMmntMults = new double[numTrackedMmnts];
		setMinMax(_minMax[0],_minMax[1]);			
		popToSmplMmntMults[meanIDX] = 1.0;
		if(sampleSize <= 1) {return;}
		popToSmplMmntMults[varIDX] = sampleSize/(sampleSize-1);
		popToSmplMmntMults[stdIDX] =  Math.sqrt(popToSmplMmntMults[varIDX]);
		if(sampleSize <= 2) {return;}
		double smplSz2x = (sampleSize*sampleSize), sz1msz2 = (sampleSize-1)*(sampleSize-2);
		popToSmplMmntMults[skewIDX] = (smplSz2x/sz1msz2) / (popToSmplMmntMults[varIDX] * popToSmplMmntMults[stdIDX]); 
		if(sampleSize <= 3) {return;}
		popToSmplMmntMults[kurtIDX] = ((smplSz2x*(sampleSize+1))/(sz1msz2*(sampleSize-3))) / (popToSmplMmntMults[varIDX] * popToSmplMmntMults[varIDX]);
		popToSmplMmntMults[excKurtIDX] = popToSmplMmntMults[kurtIDX];
		setFlag(smplValsCalcedIDX, true);
	}//calcSmpleMomentsForSampleSize
	
	public double mean() {return getFlag(meanIDX) ? mmnts[meanIDX] : 0;}
	public double var() { return getFlag(varIDX) ? mmnts[varIDX] : 0;}
	public double std() { return getFlag(stdIDX) ? mmnts[stdIDX] : 0;}
	public double skew() { return getFlag(skewIDX) ? mmnts[skewIDX] : 0;}
	public double kurt() { return getFlag(kurtIDX) ? mmnts[kurtIDX] : 0;}
	public double exKurt() { return getFlag(excKurtIDX) ? mmnts[excKurtIDX] : 0;}
	
	public double getMin() {return minMax[0];}
	public double getMax() {return minMax[1];}
	
	public double[] getDataVals() {return vals;}
	
	//return CDF map of data, where key is p(X<=x) and value is x
	//idx 0 is biased; idx 1 is unbaised
	@SuppressWarnings("unchecked")
	public TreeMap<Double,Double>[] getCDFOfData(){
		//keyed by cum prob, value is sample value
		TreeMap<Double, Double>[] res = new TreeMap[2];
		res[0] = new TreeMap<Double, Double>();
		res[1] = new TreeMap<Double, Double>();
		
		if (vals != null) {			
			//map of values and counts - use this to build CDF
			TreeMap<Double, Integer> valsCount = new TreeMap<Double, Integer>();
			Integer count;
			for (int i=0;i<vals.length;++i) {
				count = valsCount.get(vals[i]);
				if(null==count) {count=0;};
				valsCount.put(vals[i], ++count);
			}
			Double cumProb = 0.0, cumProbP1 = 0.0, np = 1.0 * vals.length, np1 = np + 1.0;
			Integer val;
			//now build result map based on prob for every value - # of elements <= val/# of elements
			for (Double key : valsCount.keySet()) {
				val = valsCount.get(key);
				cumProb += val/np;
				cumProbP1 += val/np1;
				res[0].put(cumProb, key);				
				res[1].put(cumProbP1, key);		
			}			
		}//if vals		
		return res;
	}//getCDFOfData
	
	
	//get moments of sampled data, if specified that given moments are of underlying population
	public double smpl_mean() {return (getFlag(meanIDX) & getFlag(smplValsCalcedIDX)) ? mmnts[meanIDX] * popToSmplMmntMults[meanIDX]: 0;}
	public double smpl_var() { return (getFlag(varIDX) & getFlag(smplValsCalcedIDX))  ? mmnts[varIDX] * popToSmplMmntMults[varIDX]  : 0;}
	public double smpl_std() { return (getFlag(stdIDX) & getFlag(smplValsCalcedIDX))  ? mmnts[stdIDX] * popToSmplMmntMults[stdIDX] : 0;}
	public double smpl_skew() { return (getFlag(skewIDX) & getFlag(smplValsCalcedIDX))  ? mmnts[skewIDX] * popToSmplMmntMults[skewIDX] : 0;}
	public double smpl_kurt() { return (getFlag(kurtIDX) & getFlag(smplValsCalcedIDX))  ? mmnts[kurtIDX] * popToSmplMmntMults[kurtIDX] : 0;}
	public double smpl_exKurt() { return (getFlag(excKurtIDX) & getFlag(smplValsCalcedIDX))  ? mmnts[excKurtIDX] * popToSmplMmntMults[excKurtIDX] : 0;}
	
	//transform the given value via the 1st 2 moments of the distribution described by these statistics from a normal ~N(0,1) 
	public double normToGaussTransform(double val) {return (val * mmnts[stdIDX]) +  mmnts[meanIDX];}
	//transform the give value, assumed to be from the distribution described by this object, to a normal ~N(0,1) 
	public double gaussToNormTransform(double val) {return (val - mmnts[meanIDX])/ mmnts[stdIDX];}
	@Override
	public TreeMap<String,String> summaryStringAra(String smryName) {
		TreeMap<String,String> resMap = new TreeMap<String,String>();
		resMap.put("summaryName",smryName);
		//{"ME,"STD","SKEW","KURT", "MIN","MAX"};
		resMap.put("MEAN",String.format(frmtStr,mean()));
		resMap.put("STD",String.format(frmtStr,std()));
		resMap.put("VAR",String.format(frmtStr,var()));
		resMap.put("SKEW",String.format(frmtStr,skew()));
		resMap.put("KURT",String.format(frmtStr,kurt()));
		resMap.put("MIN",String.format(frmtStr,getMin()));
		resMap.put("MAX",String.format(frmtStr,getMax()));
		return resMap;		
	}//summaryStringAra()

	/**
	 * return an array of values scaled to lie between observed min and max, in sequence of original data
	 * @return
	 */
	public double[] getScaledVals() {
		double[] res = new double[vals.length];
		for(int i=0;i<vals.length;++i) {	res[i]=scaleVal(vals[i],this.minMax[0],this.minMax[1]);}
		return res;
	}
		

	public String getMomentsVals() {
		String res = "# vals : " +numVals + " | Set By Data : " + getFlag(setByDataIDX) + " | "  + getMoments();
		return res;
	}
	
	public String getMoments() {
		String res = "";
		for (int i=0;i<mmntLabels.length-1;++i) {	res +=  mmntLabels[i] + " = "+String.format("%.8f",mmnts[i]) + " | ";	}
		res+=mmntLabels[mmntLabels.length-1] + " = "+String.format("%.8f",mmnts[mmntLabels.length-1]);
		return res;
	}
	
	public String getMinNumMmntsDesc() {
		String res = "";
		for (int i=0;i<numMmntsGiven-1;++i) {	res +=  mmntLabels[i] + " = "+String.format("%.8f",mmnts[i]) + " | ";	}
		res+=mmntLabels[numMmntsGiven-1] + " = "+String.format("%.8f",mmnts[numMmntsGiven-1]);
		return res;
	}
	
	//get base moments with no descriptor strings
	public String getMinNumMmnts() {
		String res = "";
		for(int i=0;i<numMmntsGiven;++i) { 			res += "_"+String.format("%.5f",mmnts[i]);		}
		return res;
	}	
	
	public String toString() {
		String res ="";
		res+=getMomentsVals() + " | Min = " +String.format("%.8f",minMax[0])+ " | Max = " +String.format("%.8f",minMax[1]);
		
		return res;
	}

}//class myProbAnalysis
