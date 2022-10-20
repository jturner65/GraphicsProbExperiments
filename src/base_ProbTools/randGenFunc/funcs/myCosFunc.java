package base_ProbTools.randGenFunc.funcs;

import base_ProbTools.baseProbExpMgr;
import base_ProbTools.quadrature.base.baseQuadrature;
import base_ProbTools.randGenFunc.funcs.base.baseRandVarFunc;
import base_StatsTools.summary.myProbSummary_Dbls;

//class to model a pdf via a cosine
//mean is phase, std is function of frequency
public class myCosFunc extends baseRandVarFunc{
	//constants to modify cosine so that we represent desired moment behavior
	//area under pdf from mean-> x*std corresponding to x val @ 0,1,2,3 - used to determine appropriate frequency values 
	private static final double[] stdAreaAra = new double[] {0.0,0.3413447460685429485852 ,0.4772498680518207927997 , 0.4986501019683699054734};
	//don't set this to 0!
	private static final int stdFreqMultToUse = 1;
	
	//frequency == 1/period; needs to be calculated so that stdArea is under curve from mean -> x @ 1 std - cos(x) has freq 1/2pi, so this value is actually 2pi*freq
	//xBnds == x value where function == 0 -> corresponds to +Pi for freq = 1
	private double freqMult, xBnd, actLBnd, actUBnd;
	//for standardized functions : freq1StdMult found to be 1.20934655
	private static final double freq1StdMult = calcFreq(1.0), halfAmpl1Std = freq1StdMult/twoPi, xBnd1Std = Math.PI/freq1StdMult;
	//half amplitude - needs to be freq/2pi; 
	//amplitude to maintain area == 1 under 1 period of cosine is actually freq/pi but we are using .5 + .5*cos, so make calc easier to use freq/2pi * ( 1 + cos)
	private double halfAmpl;

	public myCosFunc(baseQuadrature _quadSlvr, myProbSummary_Dbls _summaryObj) {
		super(_quadSlvr, _summaryObj, "Cosine PDF");
	}
	
	//this will calculate the freq val for a given std iteratively 
	protected static double calcFreq(double std) {
		//qStd is std @ stdFreqMultToUse
		double stdArea = stdAreaAra[stdFreqMultToUse], twoPiStdArea = stdArea* twoPi;
		double res = std,sinFreqS,diff, qStd = stdFreqMultToUse * std;//  ,cosFreqS ;
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			sinFreqS = Math.sin(res * qStd);//solve CDF for std value - ignore mu
			//cosFreqS = Math.cos(res * qStd);//solve CDF for std value - ignore mu
			diff = res - ((twoPiStdArea - sinFreqS)/qStd); 
			//System.out.println("iter " + i + " diff : " + String.format("%3.8f", diff) + "\t std :"+ String.format("%3.8f", std) + " res : " + String.format("%3.8f", res) + " sinFreqS : "+ String.format("%3.8f", sinFreqS));
			if(Math.abs(diff) < convLim) {				done=true;			}
			res -= .2*diff;
			++i;
		}//
		//expMgr.dispMessage("myCosFunc","calcFreq","Final freq val : iters " + i + "\t std :"+ String.format("%3.8f", std) + " freq res : " + String.format("%3.8f", res),MsgCodes.info1,true);
		return res;
	}//calcFreq
	
	@Override
	protected void rebuildFuncs_Indiv() {
		double mu = summary.mean(), std = summary.std();//, var = summary.var();
		//setup before actual functions are built
		//TODO need to solve for freq based on area from 0->1 std being stdArea
		if(std==0) {
			freqMult = 1.0;//temp placeholder - NOT CORRECT
		} else {
			freqMult = calcFreq(std);	//solve based on std	
			double freqMult2 = Math.PI/std;
			msgObj.dispInfoMessage("myCosFunc","rebuildFuncs_Indiv","freq1StdMult : " + String.format("%3.8f",freq1StdMult)+ " | Calced freqMult : " + String.format("%3.8f",freqMult) + " | pi/s : " + String.format("%3.8f",freqMult2) + " | s :"+String.format("%3.8f",std) + " | calc ov pi/s : " + String.format("%3.8f",freqMult/freqMult2));
		}
		xBnd = Math.PI/freqMult;		//values need to be between mu - xBnd and mu + xBnd
		actLBnd = mu - xBnd;
		actUBnd = mu + xBnd;
		halfAmpl = freqMult/twoPi;		
	}//rebuildFuncs_Indiv
	
	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds(int funcType) {
		if(funcType==queryInvCDFIDX) {	return  new double[] {0.0,1.0};	}
		//double mu = summary.mean(), std = summary.std();
		// TODO Auto-generated method stub
		return new double[] {actLBnd, actUBnd};
	}//getPlotValBounds
	
	@Override
	protected void buildFuncs() {
		double mu = summary.mean();//, var = summary.var();
		//form should be freq/2pi * (1 + (cos(freq*(x - mu)))); 
		//want to find appropriate freq so that area under curve [0,std] == 0.3413447460685429 		
		//actual functions
		funcs[fIDX] 		= x ->  {return (halfAmpl * (1 +  Math.cos(freqMult * (x - mu))));};
		funcs[fInvIDX] 		= xinv -> {return  (Math.acos(  (xinv/halfAmpl) - 1.0) + (freqMult*mu))/freqMult; };
//		//zigurat functions -> want 0 mean 1 std distribution
		funcs[fStdIDX]		= x -> {return (halfAmpl1Std * (1 +  Math.cos(freq1StdMult * x)));};
		funcs[fInvStdIDX]	= xinv -> {return  (Math.acos(  (xinv/halfAmpl1Std) - 1.0))/freq1StdMult; };
//		//analytical derivatives
		funcs[fDerivIDX]	= x -> {return (halfAmpl * (- freqMult * Math.sin(freqMult * (x - mu))));};    
		funcs[fStdDeriveIDX] = x -> {return (halfAmpl1Std * (- freq1StdMult * Math.sin(freq1StdMult * x)));};                                                         ;
		//integrals - solve analytically
		integrals[fIntegIDX] = x -> {return (halfAmpl * ((x[1]-x[0]) +  (Math.sin(freqMult * (x[1] - mu)) - Math.sin(freqMult * (x[0] - mu)))/freqMult));};
		integrals[fStdIntegIDX] = x -> {return (halfAmpl1Std * ((x[1]-x[0]) +  (Math.sin(freq1StdMult * x[1]) - Math.sin(freq1StdMult * x[0]))/freq1StdMult));};
	}//

	//pdf - described for this kind of rnd func by f
	@Override
	public double PDF(double x) {	return f(x);}
	//find CDF value of x; x must be within bounds mu-xBnd to mu+xBnd
	//CDF is integral from -inf to x of pdf - can be solved analytically 
	@Override
	public double CDF(double x) {
		//expMgr.dispMessage("myRandVarFunc", "CDF", "Begin CDF calc for val : " + String.format("%3.8f", x) , true);
		double newX = forceInBounds(x,actLBnd, actUBnd);
		
		double  res = integrals[fIntegIDX].apply(new Double[] {actLBnd, newX});
		//double res = integral_f(actLBnd, x);		 
		//expMgr.dispMessage("myRandVarFunc", "CDF", "End CDF calc for val : " + String.format("%3.8f", x) , true);
		return res;
	}//CDF
	
	//given probability p find value x such that CDF(X<= x) == p
	@Override
	public double CDF_inv(double p) {
		//expMgr.dispMessage("myCosFunc", "CDF_inv", "Begin CDF_inv calc for prob : " + String.format("%3.8f", p), MsgCodes.info1,true);
		//double res = calcInvCDF(p, integrals[fStdIntegIDX],  -xBnd1Std);		//iteratively finds inverse
		
		double res2 = calcInvCDF_Newton(p, integrals[fStdIntegIDX], funcs[fStdIDX], -xBnd1Std);
		//double res = calcInvCDF(p, integrals[fIntegIDX],  actLBnd);
		//expMgr.dispMessage("myCosFunc", "CDF_inv", "Finish CDF_inv calc for prob : " + String.format("%3.8f", p) + "\t stdzd res : " + String.format("%3.8f",res)+ "\t newton stdzd res : " + String.format("%3.8f",res2)+ "\t low xBnd1Std : " + String.format("%3.8f", -xBnd1Std),MsgCodes.info1,true);
		return processResValByMmnts(res2);
		//return res;//processResValByMmnts(res);
	}//CDF_inv
	
	private Double forceInBounds(Double x, double lBnd, double uBnd, boolean forceToBnd) {		
		if(forceToBnd) {
			return (x < lBnd ? lBnd : x > uBnd ? uBnd : x);
		} else {
			double pd =  uBnd - lBnd;//period is 2x bound
			if(x < lBnd) {				do {	x += pd;} while (x < lBnd);	} 
			else if(x > uBnd) {			do {	x -= pd;} while (x > uBnd);	} 
			return x;	
		}
	}//forceInBounds
	private Double forceInBounds(Double x, double lBnd, double uBnd) {return forceInBounds(x, lBnd, uBnd, true);}
	@Override
	public double integral_f(Double x1, Double x2) {
		//expMgr.dispMessage("myRandVarFunc", "integral_f", "Begin integral_f calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2) , true);
		if(x1 == Double.NEGATIVE_INFINITY) {x1 = actLBnd;}
		if(x2 == Double.POSITIVE_INFINITY) {x2 = actUBnd;}
		
		double newX1 = forceInBounds(x1,actLBnd, actUBnd);
		double newX2 = forceInBounds(x2,actLBnd, actUBnd); 
		
		double  resEval = integrals[fIntegIDX].apply(new Double[] {newX1, newX2});
		//double res = quadSlvr.evalIntegral(funcs[fIDX], newX1, newX2).doubleValue();
		//expMgr.dispMessage("myRandVarFunc", "integral_f", "End integral_f calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2)+ " : res = " +  String.format("%3.8f", quadSlvr.evalIntegral(funcs[fIDX], newX1, newX2).doubleValue()) + " Analytic eval : " +  String.format("%3.8f", resEval) , true);
		return resEval;
	}

	@Override
	public double integral_fStd(Double x1, Double x2) {
		//expMgr.dispMessage("myRandVarFunc", "integral_fStd", "Begin integral_fStd calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2) , true);
		if(x1 == Double.NEGATIVE_INFINITY) {x1 = -xBnd1Std;}
		if(x2 == Double.POSITIVE_INFINITY) {x2 = xBnd1Std;}		
		double newX1 = forceInBounds(x1,-xBnd1Std, xBnd1Std);
		double newX2 = forceInBounds(x2,-xBnd1Std, xBnd1Std); 	
		
		//expMgr.dispMessage("myRandVarFunc", "integral_fStd", "New Integral Bounds : " + String.format("%3.8f", newX1) +","+ String.format("%3.8f", newX2) , true);
		double resEval = integrals[fStdIntegIDX].apply(new Double[] {newX1, newX2});
		//double res = quadSlvr.evalIntegral(funcs[fStdIDX], newX1, newX2).doubleValue(); 
		//expMgr.dispMessage("myRandVarFunc", "integral_fStd", "End integral_fStd calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2)+ " : res = " +  String.format("%3.8f", quadSlvr.evalIntegral(funcs[fStdIDX], newX1, newX2).doubleValue()) + " Analytic eval : " +  String.format("%3.8f", resEval) , true);
		return resEval;
	}

	@Override
	//assmue we can modify value in similar way to transform by 1st 2 moments
	public double processResValByMmnts(double val) {	return summary.normToGaussTransform(val);}//public abstract double processResValByMmnts(double val);

	@Override
	public void setOptionFlags(int[] _opts) {
	}

	@Override
	public int getRVFType() {return baseProbExpMgr.raisedCosRandVarIDX;}

}//myCosFunc
