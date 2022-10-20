package base_ProbTools.randGenFunc.funcs;

import java.util.TreeMap;
import java.util.function.Function;

import org.jblas.DoubleMatrix;
import org.jblas.Solve;

import base_ProbTools.baseProbExpMgr;
import base_ProbTools.quadrature.base.baseQuadrature;
import base_ProbTools.randGenFunc.funcs.base.baseRandVarFunc;
import base_StatsTools.summary.myProbSummary_Dbls;

/**
 * this function will build a pdf/cdf model based on working backward from samples - building cdf from sample data, fitting sin-based CDF function to data, then differentiating to find pdf
 * @author john
 */
public class myCosFuncFromCDF extends baseRandVarFunc{
	//TODO Doesn't work
	//variables that describe the underlying sin PDF : A*(sin(B*x + C) + x)
	private DoubleMatrix Theta;
	private double actLBnd, actUBnd;
	//whether to use 1 + sine or x + sine for CDF
	private int CDFToUse;
	//functions to solve optimization CDF - either a(1 + sine(b*(x - c))) or a(x + sine(b*(x-c)))
	//takes x, a,b,c as input, returns func eval
	private Function<Double[], Double>[] slvrFuncs;	
	//derivatives w/respect to each coefficient a,b,c...
	private Function<Double[], double[]>[] slvrDerivFuncs;	
	
	//idxs in slvrFuncs 
	private static final int 
		cdf1pSine = 0,
		cdfXpSine = 1;
	private static final int numSlvrFuncs = 2;
	public myCosFuncFromCDF(baseQuadrature _quadSlvr, myProbSummary_Dbls _summaryObj) {
		super(_quadSlvr, _summaryObj, "Cosine PDF");
		buildSlvrFuncs();		
	}//ctor
	
	@SuppressWarnings("unchecked")
	private void buildSlvrFuncs() {
		//funcs are CDF functions
		slvrFuncs = new Function[numSlvrFuncs];
		slvrDerivFuncs = new Function[numSlvrFuncs];
		slvrFuncs[0] = xAra -> {//1 + sine cdf model
			double a = xAra[0], b=xAra[1], c=xAra[2], x=xAra[3];
			return a*(1 + Math.sin(b * (x - c)));
		};		
		slvrDerivFuncs[0]= xAra -> {//1 + sine cdf model a*(1+sin(b*(x-c)))
			double a = xAra[0], b=xAra[1], c=xAra[2], x=xAra[3];
			double xmc = (x-c), bXmC = b*xmc, cosVal = Math.cos(bXmC);
			return new double[] {
					(1 + Math.sin(bXmC)),	//dA
					a*xmc*cosVal,			//dB
					-a*b*cosVal				//dC					
			};
		};		
		slvrFuncs[1] = xAra -> {//x + sine cdf model :  a *(Math.PI/b + (x-c) + (Math.sin(b * (x - c))/b));
			double a = xAra[0], b=xAra[1], c=xAra[2], x=xAra[3],xmc = (x-c);
			return a *(xmc + (Math.PI + Math.sin(b * xmc))/b);
		};		
		slvrDerivFuncs[1]= xAra -> {///CDF func : a *(Math.PI/b + (x-c) + (Math.sin(b * (x - c))/b));
			double a = xAra[0], b=xAra[1], c=xAra[2], x=xAra[3],xmc = (x-c), piOvB = Math.PI/b, bXmC = b*xmc, sinVal = Math.sin(bXmC), cosVal = Math.cos(bXmC);
			return new double[] {
					piOvB + sinVal/b + xmc,						//dA = sin(B * val + C)/B + x
					a * (-Math.PI + bXmC * cosVal - sinVal)/(b*b),	//dB 
					a*(-1 - cosVal)									//dC					
			};
		};		
	}//buildSlvrFuncs

	//private void printXYVals(DoubleMatrix xVals) {		System.out.println("xVals : " + xVals);	}
	

	//for equation 1/2  + 1/2 sin(b*(x-phi)) - this doesn't integrate into an appropriate cosine
	private void deriveCoeffs_onePSine(TreeMap<Double,Double> CDFMap, TreeMap<Double,Double> CDFMapP1) {	
		TreeMap<Double,Double> mapToUse = CDFMap;
		//EQ is A*(1 + sin(b * (x - phi)))
		//can derive offest analytically since last value in cdf has cdf value 1 - how much we want to shift to get sine to be 1
		//wavelength == 
		double eps = .000000001;
		//double lastVal = mapToUse.get(mapToUse.lastKey());		//where sine is 1	
		double y1 = mapToUse.floorKey(mapToUse.lastKey() - eps), y2 = mapToUse.ceilingKey(mapToUse.firstKey() + eps), 
				x1 = mapToUse.get(y1), x2 = mapToUse.get(y2), asinY1 = Math.asin(2*y1-1); 			
		
//		double freqMult = (Math.asin((2*y2)-1.0) - halfPi)/(x2 - lastVal);
//		double phi = lastVal - (halfPi/freqMult);
		double freqMult = (Math.asin(2*y2-1) - asinY1)/(x2-x1);
		double phi = x1 - asinY1/freqMult;		
		
		DoubleMatrix thetaLcl = new DoubleMatrix(new double[] {.5,freqMult, phi});		
		actLBnd = (Math.asin(-1))/freqMult  + phi;
		actUBnd = (Math.asin(1))/freqMult  + phi;
		
		Theta = thetaLcl;
		Function<Double[], Double> func = slvrFuncs[cdf1pSine];
		double lbnd = calcActualBnd(func,Theta,0.0),
		ubnd = calcActualBnd(func,Theta,1.0);//CDFMap.get(CDFMap.lastKey());
		msgObj.dispInfoMessage("myCosFuncFromCDF","deriveCoeffs_onePSine","# vals : " +mapToUse.size() +" y2 : " + y2 + " x2 : " + x2 + " | Theta values : A : " + Theta.get(0) + " |B : " + Theta.get(1) + " |C : " + Theta.get(2) + " : bnds : act : [" +actLBnd +", "+actUBnd +"] | iter :  [" +lbnd +", "+ubnd +"]");				
	}//deriveCoeffs
	
	//calculate residual values
	private DoubleMatrix calcRVal(Function<Double[], Double> func, DoubleMatrix yVals, DoubleMatrix xVals, DoubleMatrix Theta) {
		DoubleMatrix fXVals =DoubleMatrix.zeros(xVals.getRows(), 1);
		Double[] inVals = new Double[] { Theta.get(0), Theta.get(1),Theta.get(2),0.0};
		for (int i=0;i<xVals.getRows();++i) {	
			inVals[3]=xVals.get(i);
			fXVals.put(i, func.apply(inVals));//	fXVals.put(i, calcF_Theta(Theta,xVals.get(i)));	
		}		
		return yVals.sub(fXVals);
	}//calcRVal
	
	private DoubleMatrix calcJacobian(Function<Double[], double[]> derivFunc, DoubleMatrix xVals, DoubleMatrix Theta) {
		DoubleMatrix J = new DoubleMatrix(xVals.length, Theta.length);
		Double[] inVals = new Double[] { Theta.get(0), Theta.get(1),Theta.get(2),0.0};
		for (int i=0;i<xVals.getRows();++i) {		
			inVals[3]=xVals.get(i);
			J.putRow(i, new DoubleMatrix(derivFunc.apply(inVals)));	
		}
		return J;
	}//calcJacobian
	
	//need to derive coefficients through Newton Method
	private void deriveCoeffs(TreeMap<Double,Double> CDFMap, TreeMap<Double,Double> CDFMapP1, int slvrIDX) {		
		TreeMap<Double,Double> mapToUse = CDFMap;
		int numIters = 1000;
		double alpha = .02;//learning rate
		DoubleMatrix rVals = new DoubleMatrix(mapToUse.size(), 1),//rSqVals = new DoubleMatrix(CDFMap.size(), 1), 
				yVals= new DoubleMatrix(mapToUse.size(), 1), 
				xVals= new DoubleMatrix(mapToUse.size(), 1);
		//initial values
		DoubleMatrix thetaLcl = new DoubleMatrix(new double[] {.5,twoPi,.5}), 
				dTheta, oldDTheta = new DoubleMatrix(new double[] {0,1,1});
		int row = 0;
		for(Double key : mapToUse.keySet()) {		yVals.put(row, key);		xVals.put(row, mapToUse.get(key));		++row;	}
		//Theta == [A,B,C]^T; dTheta == [J^T * J]^-1 * J^T * r
		//newTheta = Theta + alpha * dTheta
		
		boolean done = false;
		int iter = 0;
		while ((!done) && (iter < numIters)){
			//calc Res Value : yVal - f(theta,xVal)
			rVals = calcRVal(slvrFuncs[slvrIDX],yVals, xVals, thetaLcl);
			//calculate Jacobians for each point
			DoubleMatrix J = calcJacobian(slvrDerivFuncs[slvrIDX],xVals, thetaLcl), //tmpVal = J.transpose().mmul(J), 
					tmpVal2 = Solve.pinv(J.transpose().mmul(J)).mmul(J.transpose());
			//System.out.println("iter : " + iter + " rVals : " +rVals.toString() +" rows : " + rVals.getRows()+ " J numrows : " + J.getRows() + " | J : " +J.toString() +" \ntmpVal : " +tmpVal.toString() +" \n tmpVal2 : " +tmpVal2.toString() +" \n tmpVal2 rows : " +tmpVal2.getRows()+ " \n ");
			//printXYVals(xVals);			
			dTheta = tmpVal2.mmul(rVals);
			if (isNewtonDone(dTheta.sub(oldDTheta))) {	done = true;}
			oldDTheta = dTheta;				
			thetaLcl.addi(dTheta.mul(alpha));	
			//modify Theta
			//System.out.println("Final Iters :  " + iter + " thetaLcl values : A : " + thetaLcl.get(0) + " |B : " + thetaLcl.get(1) + " |C : " + thetaLcl.get(2) + "\t| dTheta values : A : " + dTheta.get(0) + " |B : " + dTheta.get(1) + " |C : " + dTheta.get(2)  );
			
			++iter;
		}//iterative loop
		Theta = thetaLcl;
		
		actLBnd = calcActualBnd(slvrFuncs[slvrIDX],Theta,0.0);
		actUBnd = calcActualBnd(slvrFuncs[slvrIDX],Theta,1.0);//CDFMap.get(CDFMap.lastKey());
		if(actLBnd > actUBnd) {actLBnd -= twoPi/Theta.get(1);}
		msgObj.dispInfoMessage("myCosFuncFromCDF","deriveCoeffs","# vals : " +mapToUse.size() +" | Final Iters :  " + iter + " Theta values : A : " + Theta.get(0) + " |B : " + Theta.get(1) + " |C : " + Theta.get(2)+" | lbnd : " + actLBnd + " | ubnd : " + actUBnd);	
			
	}//deriveCoeffs
	
	//find inverse CDF value -> x value such that CDF(X<= x) == p
	//prob - lbnd == 0; hbnd == 1
	private double calcActualBnd(Function<Double[], Double> func, DoubleMatrix theta, double prob) {
		double xVal = 1.0 - prob, calcPVal = 0, diff;
		Double[] inVals = new Double[] { theta.get(0), theta.get(1),theta.get(2),0.0};
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			inVals[3]=xVal;
			calcPVal = func.apply(inVals);
			diff = prob -calcPVal;
			if(Math.abs(diff) < convLim) {				done=true;			}
			xVal += .2*diff;
			++i;
		}//
		//System.out.println("Final InvCDF val : iters " + i + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " prob(xVal) : " +  String.format("%3.8f", calcPVal)+ " lbnd : " +  String.format("%3.8f", lBndVal));
		return xVal;
	}//calcInvCDF
	
	@Override
	protected void rebuildFuncs_Indiv() {
		if(slvrFuncs == null) {buildSlvrFuncs();}
		//get CDF map of data  where key is p(X<=x) and value is x
		TreeMap<Double,Double> CDFMap, CDFMapP1 = new TreeMap<Double,Double>();		
		try {
			TreeMap<Double,Double>[] res = summary.getCDFOfData();
			CDFMap = res[0];
			CDFMapP1 = res[1];
			if(CDFMap.size() < 2) {
				System.out.println("CDFMap.size() < 2 : " +CDFMap.size());
				throw new Exception();
			}
			dbgDispCDF(CDFMap,"myCosFuncFromCDF div n");
			//dbgDispCDF(CDFMapP1,"myCosFuncFromCDF div n+1");
		} catch (Exception e) {//not enough values to build a cdf here
			e.printStackTrace();
			return;
		}
		
		if(CDFToUse==0) {		deriveCoeffs_onePSine(CDFMap, CDFMapP1);} 
		else {
			deriveCoeffs(CDFMap, CDFMapP1,CDFToUse);
		}
	}//rebuildFuncs_Indiv

	//uses a(x + 1/b * sin(b*(x-c)) as CDF
	@Override
	protected void buildFuncs() {
		if(CDFToUse==cdfXpSine) {
			//actual functions  Theta.get(0) * (Math.sin(Theta.get(1) * x + Theta.get(2)) + 1);
			funcs[fIDX] 		= x ->  {
				//if(x<=actLBnd) {return 0.0;} else if(x>= actUBnd) {return 1.0;}
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return a * (1+Math.cos(b*(x - c))); };
			funcs[fInvIDX] 		= xinv -> {
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (Math.acos((xinv/a) - 1.0)/b + c);};
	//		//zigurat functions -> want 0 mean 1 std distribution
			funcs[fStdIDX]		= funcs[fIDX];
			funcs[fInvStdIDX]	= funcs[fInvIDX];
	//		//analytical derivatives
			funcs[fDerivIDX]	= x -> {
				//if(x<=actLBnd) {return 0.0;} else if(x>= actUBnd) {return 1.0;}
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (a *  (- b * Math.sin(b * (x - c))));};    
			funcs[fStdDeriveIDX] = funcs[fDerivIDX];                                                         ;
			//integrals - solve analytically
			// Theta.get(0) * (Math.sin(Theta.get(1) * x + Theta.get(2)) + x)
			//a *(Math.PI/b + (x-c) + (Math.sin(b * (x - c))/b));
			integrals[fIntegIDX] = x -> {
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (a * ( (x[1]-x[0]) +  (Math.sin(b * (x[1] - c)) - Math.sin(b * (x[0] - c)))/b ));};
		} else {
			//actual functions  Theta.get(0) * (Math.sin(Theta.get(1) * x + Theta.get(2)) + 1);
			funcs[fIDX] 		= x ->  {
				//if(x<=actLBnd) {return 0.0;} else if(x>= actUBnd) {return 1.0;}
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (a*b)*Math.cos(b*(x - c)); };
			funcs[fInvIDX] 		= xinv -> {
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return ((Math.acos(xinv/(a*b) ) + b*c)/b); };
//			//zigurat functions -> want 0 mean 1 std distribution
			funcs[fStdIDX]		= funcs[fIDX];
			funcs[fInvStdIDX]	= funcs[fInvIDX];
//			//analytical derivatives
			funcs[fDerivIDX]	= x -> {
				//if(x<=actLBnd) {return 0.0;} else if(x>= actUBnd) {return 1.0;}
				double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); 
				return (a *  (- b*b * Math.sin(b * (x - c))));};    
			funcs[fStdDeriveIDX] = funcs[fDerivIDX];                                                         ;
			//integrals - solve analytically
			// Theta.get(0) * (Math.sin(Theta.get(1) * x + Theta.get(2)) + x)
			integrals[fIntegIDX] = x -> {double a = Theta.get(0), b = Theta.get(1), c = Theta.get(2); return (a * (Math.sin(b * (x[1] - c)) - Math.sin(b * (x[0] - c))) );};
			
		}
		msgObj.dispInfoMessage("myCosFuncFromCDF","buildFuncs","LBnd : " + actLBnd + " UBnd : " + actUBnd);	
	}//

	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds(int funcType) {
		if(funcType==queryInvCDFIDX) {	return  new double[] {0.0,1.0};	}
		//double mu = summary.mean(), std = summary.std();
		// TODO Auto-generated method stub
		return new double[] {actLBnd, actUBnd};
	}//getPlotValBounds
	//pdf - described for this kind of rnd func by f
	@Override
	public double PDF(double x) {	return f(x);}
	
	//find CDF value of x; x must be within bounds mu-xBnd to mu+xBnd
	//CDF is integral from -inf to x of pdf - can be solved analytically 
	@Override
	public double CDF(double x) {
		//expMgr.dispInfoMessage("myRandVarFunc", "CDF", "Begin CDF calc for val : " + String.format("%3.8f", x));
		double newX = x;//forceInBounds(x,actLBnd, actUBnd);
		
		double  res = integrals[fIntegIDX].apply(new Double[] {actLBnd, newX});
		//double res = integral_f(actLBnd, x);		 
		//expMgr.dispInfoMessage("myRandVarFunc", "CDF", "End CDF calc for val : " + String.format("%3.8f", x));
		return res;
	}//CDF
	
	//find inverse CDF value -> x value such that CDF(X<= x) == p - func is internal
	private double calcInvCDF(Function<Double[], Double> func, double p, DoubleMatrix theta, double lbnd) {
		double xVal = 0, calcPVal = 0, diff;
		Double[] inVals = new Double[] { theta.get(0), theta.get(1),theta.get(2),lbnd};
		
		double lBndVal = func.apply(inVals);
		boolean done = false;
		int i = 0;
		while ((!done) && (i < 1000)){
			inVals[3]=xVal;
			calcPVal = func.apply(inVals) - lBndVal;//solve for std value - ignore mu
			diff = p - calcPVal;
			if(Math.abs(diff) < convLim) {				done=true;			}
			xVal += .2*diff;
			++i;
		}//
		//System.out.println("Final InvCDF val : iters " + i + "\t tar prob :"+ String.format("%3.8f", p) + " xVal : " + String.format("%3.8f", xVal) + " prob(xVal) : " +  String.format("%3.8f", calcPVal)+ " lbnd : " +  String.format("%3.8f", lBndVal));
		return xVal;
	}//calcInvCDF
	
	//given probability p find value x such that CDF(X<= x) == p
	@Override
	public double CDF_inv(double p) {
		//expMgr.dispInfoMessage("myCosFunc", "CDF_inv", "Begin CDF_inv calc for prob : " + String.format("%3.8f", p), true);
		double res = calcInvCDF(this.slvrFuncs[CDFToUse],p,Theta, actLBnd);
		//double res = calcInvCDF(p, halfAmpl, freqMult,  actLBnd, summary.mean());
		//expMgr.dispInfoMessage("myCosFunc", "CDF_inv", "Finish CDF_inv calc for prob : " + String.format("%3.8f", p) + "\t stdzd res : " + String.format("%3.8f",res)+ "\t low xBnd1Std : " + String.format("%3.8f", -xBnd1Std), true);			
		return res;//processResValByMmnts(res);
	}//CDF_inv
	
	//private boolean checkInBnds(Double x, double mu) {return ((x>= mu - xBnd) && (x<= mu + xBnd));}
	
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
		//expMgr.dispInfoMessage("myRandVarFunc", "integral_f", "Begin integral_f calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2));
		if(x1 == Double.NEGATIVE_INFINITY) {x1 = actLBnd;}
		if(x2 == Double.POSITIVE_INFINITY) {x2 = actUBnd;}
		
		double newX1 = forceInBounds(x1,actLBnd, actUBnd);
		double newX2 = forceInBounds(x2,actLBnd, actUBnd); 
		
		double  resEval = integrals[fIntegIDX].apply(new Double[] {newX1, newX2});
		//double res = quadSlvr.evalIntegral(funcs[fIDX], newX1, newX2).doubleValue();
		//expMgr.dispInfoMessage("myRandVarFunc", "integral_f", "End integral_f calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2)+ " : res = " +  String.format("%3.8f", quadSlvr.evalIntegral(funcs[fIDX], newX1, newX2).doubleValue()) + " Analytic eval : " +  String.format("%3.8f", resEval));
		return resEval;
	}

	@Override
	public double integral_fStd(Double x1, Double x2) {
//		//expMgr.dispInfoMessage("myRandVarFunc", "integral_fStd", "Begin integral_fStd calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2));
//		if(x1 == Double.NEGATIVE_INFINITY) {x1 = -xBnd1Std;}
//		if(x2 == Double.POSITIVE_INFINITY) {x2 = xBnd1Std;}
//		//must only use 
//		
//		double newX1 = forceInBounds(x1,-xBnd1Std, xBnd1Std);
//		double newX2 = forceInBounds(x2,-xBnd1Std, xBnd1Std); 
//		
//		
//		//expMgr.dispInfoMessage("myRandVarFunc", "integral_fStd", "New Integral Bounds : " + String.format("%3.8f", newX1) +","+ String.format("%3.8f", newX2));
//		double resEval = integrals[fStdIntegIDX].apply(new Double[] {newX1, newX2});
//		//double res = quadSlvr.evalIntegral(funcs[fStdIDX], newX1, newX2).doubleValue(); 
		//expMgr.dispInfoMessage("myRandVarFunc", "integral_fStd", "End integral_fStd calc for vals : " + String.format("%3.8f", x1) +","+ String.format("%3.8f", x2)+ " : res = " +  String.format("%3.8f", quadSlvr.evalIntegral(funcs[fStdIDX], newX1, newX2).doubleValue()) + " Analytic eval : " +  String.format("%3.8f", resEval));
		return 0;//resEval;
	}

	@Override
	//assume we can modify value in similar way to transform by 1st 2 moments
	public double processResValByMmnts(double val) {	return summary.normToGaussTransform(val);}//public abstract double processResValByMmnts(double val);
	
	//
	@Override
	public void setOptionFlags(int[] _opts) {
		CDFToUse = (_opts[0] == 0 ? 0 : 1);//restrict to be 0 or 1
	}//setOptionFlags
	@Override
	public int getRVFType() {return baseProbExpMgr.cosCDFRandVarIDX;}

		
}//myCosFuncFromCDF