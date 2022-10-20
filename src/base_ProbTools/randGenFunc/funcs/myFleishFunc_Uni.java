package base_ProbTools.randGenFunc.funcs;

import org.jblas.DoubleMatrix;
import org.jblas.Solve;

import base_ProbTools.baseProbExpMgr;
import base_ProbTools.quadrature.base.baseQuadrature;
import base_ProbTools.randGenFunc.funcs.base.baseRandVarFunc;
import base_ProbTools.randGenFunc.gens.myZigRandGen;
import base_StatsTools.summary.myProbSummary_Dbls;

/**
 * instancing class for a univariate fleishman polynomial-based distribution function
 * 
 * When given moments of data, can build polynomial
 * @author john
 *
 */
public class myFleishFunc_Uni extends baseRandVarFunc{
	//polynomial coefficients
	private double[] coeffs;
	//quantities related to underlying cubic function
	//derivative quadratic coefficients : b + 2*cx + 3*dx^2 - find roots of deriv to determine min/maxs
	//[0][0] : lower x min/max, [0][1] : higher valued min/max
	//[1][-] : 2nd deriv test for idx0 and idx1 pts (>0 == min, <0 == max, ==0 may indicate inflection point
	//[2][-] : 3rd deriv test for sign
	//[3][-] : func eval for idx0 and idx1 pts 
	private double[][] minMax2ndDerivs;
	//roots of cubic - will always have at least 1 root (y==0)
	private double[] roots;
	//whether this is ready to use or not - all values have been set and calculated
	//private boolean ready;
	//maximum iterations
	private final int maxIter = 10000;
	//alpha
	private double[] lrnRateAra;

	//normal distribution for inverse calc
	private myZigRandGen zigNormGen;
	
	
	public myFleishFunc_Uni(baseQuadrature _quadSlvr, myProbSummary_Dbls _summaryObj, String _name) {
		super(_quadSlvr,_summaryObj, _name);		
	}//ctor

	@Override
	protected void rebuildFuncs_Indiv() {
		//build internal normal used to generate input to fleishman polynomial
		//normFunc = new myNormalFunc(expMgr, quadSlvr);
		if(zigNormGen == null) {	zigNormGen = new myZigRandGen(new myNormalFunc(quadSlvr), 256, "Ziggurat Algorithm");}	//don't rebuild if present
		//ready = false;
		lrnRateAra = new double[] {.1,.1,.1};
		coeffs = calcCoeffs();
		msgObj.dispInfoMessage("myFleishFunc_Uni", "rebuildFuncs_Indiv", "Coeffs calculated : ["+_getCoeffsStr() +"]");			
		//calculate mins and maxes of derivative of fl function
		minMax2ndDerivs = calcQuadDerivRoots();
		roots = calcAllRoots();		
		//set summary builds functions - need to specify required elements before it is called 
		if(getFlag(debugIDX)) {
			msgObj.dispInfoMessage("myFleishFunc_Uni", "rebuildFuncs_Indiv", "Roots calculated : ["+_getRootsStr() +"]");		
		}
	}//rebuildFunc
	
	@Override
	//for plotting - return min and max vals to plot between
	public double[] getPlotValBounds(int funcType) {
		if(funcType==queryInvCDFIDX) {	return  new double[] {probitBnd,1.0-probitBnd};	}
		double mu = summary.mean();//, std = summary.std();
		// TODO Auto-generated method stub
		//return new double[] {mu-(3.5*std), mu+(3.5*std)};
		//msgObj.dispMessage("myFleishFunc_Uni", "getPlotValBounds","Bounds eval @ -/+ 3.5 : [" + f(-3.5)+","+ f(3.5)+"]");		
		return new double[] {mu-5.0, mu+5.0};//{f(-3.5), f(3.5)};
	}//getPlotValBounds
	
	//calculate the coefficients for the fleishman polynomial considering the given skew and excess kurtosis specified in summary object
	//will generate data with mean ==0 and std == 1; if ex kurtosis lies outside of feasible region will return all 0's for coefficients
	private double[] calcCoeffs() {		
		msgObj.dispInfoMessage("myFleishFunc_Uni", "calcCoeffs","Start calc coefficients.");
		double[] coeffs = new double[summary.numMmntsGiven];
		double exKurt = summary.exKurt(), skew = summary.skew(), skewSQ = skew*skew;
        //first verify exkurt lies within feasible bound vs skew
        //this is fleish's bound from his paper - said to be wrong in subsequent 2010 paper
        //bound = -1.13168 + 1.58837 * skew**2
        double bound = -1.2264489 + 1.6410373*skewSQ;
        if (exKurt < bound) { 
        	msgObj.dispErrorMessage("myFleishFunc_Uni", "calcCoeffs", "!!!! Coefficient error : ex kurt : " + String.format("%3.8f",exKurt)+ " is not feasible with skew :" + String.format("%3.8f",skew) +" | forcing exKurt to lower bound @ skew "+String.format("%3.8f",bound)+" DANGER this is not going to reflect the sample quantities");
        	summary.forceExKurt(bound);
        	exKurt = summary.exKurt();
        }
        double exKurtSq = exKurt * exKurt;
        //initial coeff estimates
        double c1 = 0.95357 - (0.05679*skew) + (0.03520*skewSQ) + (0.00133*exKurtSq);
        double c2 = (0.10007*skew) + (0.00844*skewSQ*skew);
        double c3 = 0.30978 - (0.31655 * c1);

        //solve with newton-raphson
        double[] tmpC = newtonMethod(c1, c2, c3, skew, exKurt);
		coeffs = new double[] {-tmpC[1], tmpC[0],tmpC[1],tmpC[2]};
		msgObj.dispInfoMessage("myFleishFunc_Uni", "calcCoeffs","Finished calc coefficients : [" +_getCoeffsStr() + "]");
		return coeffs;
	}//calcCoeffs
	
	/**
	 * for when func has not yet been specified
	 * @param x
	 * @return
	 */
	private double calcBaseEQ(double x) {return coeffs[0] + x *(coeffs[1] + x*(coeffs[2] + x *coeffs[3]));}
	
	/**
	 * find roots of flfunc(a + bx + cx2 + dx3)'s derivative, described by dx^2 + cx + b ==0; 
	 * if 2 roots are returned idx 0 is lowest root, idx 1 is max root
	 * @return
	 */
	private double[][] calcQuadDerivRoots() {	
		//double mu = summary.mean(), std = summary.std();
		//double a = coeffs[0]*std + mu, b=coeffs[1]*std,c = coeffs[2]*std,d=coeffs[3]*std;
		double a = coeffs[0], b=coeffs[1],c = coeffs[2],d=coeffs[3];
		//find derivative to find min/max points
		double derivC = b, derivB = 2*c, derivA = 3*d;
		
		double discr = derivB*derivB - 4 * derivA * derivC, ta = (2*derivA), axisOfSym = -derivB/ta;
		if(discr < 0) {//means imaginary roots and no mins/maxs in underlying cubic (?)
			msgObj.dispInfoMessage("myFleishFunc_Uni", "calcQuadDerivRoots", "Cubic deriv (quadratic) of " +_getCoeffsStr() + " has negative discriminant : " + String.format("%3.8f",discr) + " -> means no mins/maxs.");
			return new double[][] {{},{},{},{}};
		} else if(discr == 0) {//double root
			msgObj.dispInfoMessage("myFleishFunc_Uni", "calcQuadDerivRoots", "Cubic deriv (quadratic) of " +_getCoeffsStr() + " has 0 discriminant  : " + String.format("%3.8f",discr) + " -> means double root.");
			double tderivTest = ta*axisOfSym + b;
			//double val = calcBaseEQ(axisOfSym)*std + mu;
			double val = calcBaseEQ(axisOfSym);
			return new double[][] {{axisOfSym},{tderivTest},{2*a},{val}};
		}//else 2 real roots
		msgObj.dispInfoMessage("myFleishFunc_Uni", "calcQuadDerivRoots", "Cubic deriv (quadratic) of " +_getCoeffsStr() + " has positive discriminant  : " + String.format("%3.8f",discr) + " means 2 roots.");
		double axisDist = Math.sqrt(discr)/ta;
		double zp = axisOfSym + axisDist, zm = axisOfSym - axisDist;
		//double valZP = calcBaseEQ(zp)*std + mu,valZM = calcBaseEQ(zm)*std + mu;
		double valZP = calcBaseEQ(zp),valZM = calcBaseEQ(zm);
		double tdZP = ta*zp + derivB, tdZM = ta*zm + derivB;
		return ((zp<zm) ? new double[][] {{zp, zm},{tdZP, tdZM},{ta,ta},{valZP, valZM}} : new double[][] {{zm, zp},{tdZM, tdZP},{ta,ta},{valZM, valZP}} );			
	}//calcQuadDerivRoots
	
	/**
	 * display the results of minMax2ndDerivs quantities related to underlying cubic function derivative quadratic coefficients : b + 2*cx + 3*dx^2 - find roots of deriv to determine min/maxs
	 * [0][0] : lower x min/max, [0][1] : higher valued min/max
	 * [1][-] : 2nd deriv test for idx0 and idx1 pts (>0 == min, <0 == max, ==0 may indicate inflection point
	 * [2][-] : 3rd deriv test for sign
	 * [3][-] : func eval for idx0 and idx1 pts 
	 */
	private void _dispDerivRootsVals() {
		double mu = summary.mean(), std = summary.std();	
		if(minMax2ndDerivs[0].length < 2) {return;}
		msgObj.dispInfoMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "Values in minMax2ndDerivs : ");
		int idx = 0;
		msgObj.dispInfoMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "\t min/max (deriv) : lower x minMax2ndDerivs["+idx+"][0] = "+minMax2ndDerivs[idx][0] + " | higher x minMax2ndDerivs["+idx+"][1] = "+minMax2ndDerivs[idx][1]);
		++idx;
		msgObj.dispInfoMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "\t 2nd deriv test : lower x minMax2ndDerivs["+idx+"][0] = "+minMax2ndDerivs[idx][0] + " | higher x minMax2ndDerivs["+idx+"][1] = "+minMax2ndDerivs[idx][1]);
		++idx;
		msgObj.dispInfoMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "\t 3rd deriv test : lower x minMax2ndDerivs["+idx+"][0] = "+minMax2ndDerivs[idx][0] + " | higher x minMax2ndDerivs["+idx+"][1] = "+minMax2ndDerivs[idx][1]);
		++idx;
		msgObj.dispInfoMessage("myFleishFunc_Uni", "_dispDerivRootsVals", "\t func eval : lower x minMax2ndDerivs["+idx+"][0] = "+(minMax2ndDerivs[idx][0]*std + mu) + " | higher x minMax2ndDerivs["+idx+"][1] = "+(minMax2ndDerivs[idx][1]*std + mu));
		++idx;
	}//_dispDerivRootsVals
	
	/**
	 * calculate roots of cubic via discriminant and using derived vals
	 * @return
	 */
	private double[] calcAllRoots() {
		//double mu = summary.mean(), std = summary.std();
		//double a = coeffs[0]*std + mu, b=coeffs[1]*std, b3 = b*b*b,c = coeffs[2]*std,d=coeffs[3]*std,ad = a*d, bc = b*c, ad27 = 27*ad;
		double a = coeffs[0], b=coeffs[1], b3 = b*b*b,c = coeffs[2],d=coeffs[3],ad = a*d, bc = b*c, ad27 = 27*ad;
		//descriminant of cubic
		double discr = 18*ad*bc - 4*b3*d + bc*bc - 4*a*c*c*c - ad27*ad;
		//precalc to find roots 
		//double del1 = 2 * b3 - 9*a*b*c + ad27*a, a27NegDiscr = -27*a*a*discr;
		//minMax2ndDerivs : check min/max values in function - if sign changes, then there is a zero between them
		//[0][0] : lower x min/max, [0][1] : higher valued min/max
		//[1][-] : 2nd deriv test for idx0 and idx1 pts (>0 == min, <0 == max, ==0 may indicate inflection point
		//[2][-] : 3rd deriv test for sign
		//[3][-] : func eval for idx0 and idx1 pts - if this changes signs then cubic root lies between points, otherwise root lies 
		//minMax2ndDerivs;
		if(getFlag(debugIDX)) {_dispDerivRootsVals();}
		int numRoots = 0;
		if(discr < 0) {//1 real, 2 non-real complex-conjugate roots
			msgObj.dispInfoMessage("myFleishFunc_Uni", "calcAllRoots", "Cubic Root calc : discr : " + String.format("%3.8f",discr) + " -> means 1 real and 2 non-real cmplx roots.");
			numRoots = 1;
		} 
		else if (discr > 0) {//has 3 distinct real roots
			msgObj.dispInfoMessage("myFleishFunc_Uni", "calcAllRoots", "Cubic Root calc : discr : " + String.format("%3.8f",discr) + " -> means 3 distinct real roots.");
			numRoots = 3;
		} 
		else {//discr == 0 - has a multiple root and all roots are real
			msgObj.dispInfoMessage("myFleishFunc_Uni", "calcAllRoots", "Cubic Root calc : discr : " + String.format("%3.8f",discr) + " -> means multiple root and all roots are real.");
			numRoots = 2;
		}
		double[] res = new double[numRoots];
		//TODO Need to derive actual roots of cubic
		
		return res;
	}//calcAllRoots
	
	/**
	 * Given the fleishman coefficients, and a target skew and kurtois, this function will have a root if the 
	 * coefficients give the desired skew and kurtosis : F = -c + bZ + cZ^2 + dZ^3, where Z ~ N(0,1)
	 * @param b
	 * @param c
	 * @param d
	 * @param skew desired skew
	 * @param exKurt desired kurtosis
	 * @return jBlas double matrix holding v-1, s-skew, k-execess kurtosis
	 */
	private DoubleMatrix flfunc(double b, double c, double d, double skew, double exKurt) {
		//precalcs
        double b2 = b*b, c2 = c*c, d2 = d*d, bd = b*d, t4bd = 24*bd;
        
        double _v = b2 + 6*bd + 2*c2 + 15*d2;
        double _s = 2 * c * (b2 + t4bd + 105*d2 + 2);
        //24*b*d + 24*c^2 + 24*c^2*b^2 + 672*c^2*b*d +  
        double _k = t4bd + 24*(c2 * (1 + b2 + 28*bd) + d2 * (12 + 48*bd + 141*c2 + 225*d2));
        return new DoubleMatrix(new double[] {_v - 1, _s - skew, _k - exKurt});		
	}//flfunc

	/**
	 * The deriviative of flfunc above, used for Newton method calc; returns jacobian
	 * @param b
	 * @param c
	 * @param d
	 * @return 3x3 jacobian of flfunc
	 */
	private DoubleMatrix flDeriv(double b, double c, double d) {
		double b2 = b*b, c22 = 2.0*c*c, d2 = d*d, bd = b*d;
		//matrix coeffs
		// _v = b2 + 6*bd + 2*c2 + 15*d2
		double df1db = 2.0*b + 6.0*d, 
				df1dc = 4.0*c, 
				df1dd = 6.0*b + 30.0*d,
		//_s = 2 * c * (b2 + t4bd + 105*d2 + 2);
				df2db = df1dc * (b + 12.0*d),						//4bc + 48cd  
				df2dc = 2.0 *b2 + 48.0*bd + 210.0*d2 + 4.0,			//
				df2dd = df1dc * (12.0*b + 105*d),					//48bc + 420cd == 4c*(12b + 105d)
		//t4bd + 24*(c2 * (1 + b2 + 28*bd) + d2 * (12 + 48*bd + 141*c2 + 225*d2))
		//24(bd +   c^2 +   c^2b^2 +  28*bc^2d + 12d^2    + 48d^3b + 141c^2d^2 + 225d^4)
		//deriv b : 
		//24 * (d + 2c^2*b + 28*c^2*d + 48*d^3 
		//deriv c :
		//24 ( 2c + 2cb^2 + 56bcd + 282cd^2) == 48(c + cb^2 +28bd + 141d
		//deriv d : 
		//24(b   +   0    +  0    +    28bc^2   + 24d   +  144d^2b + 282c^2d +  900d^3)  
		//24(b  		       14*b * 2c^2 + 24*d + 
				df3db = 24.0 * (d + c22*(b + 14.0*d) + 48.0*d2*d),
				df3dc = 48.0 * c * (1.0 + b2 + 28*bd + 141.0*d2),
				df3dd = 24.0 * (b + 14.0*b *c22 + d*(24 + 144*bd + 141.0*c22 + 900*d2));
				
			//	df3dd = 24.0 * (b + 14.0*b * c22 + d*(24.0 + 96.0*bd + 141.0*c22 + 450.0*d2) + d2*(48.0*b + 450.0*d));
			//		df3db = 24 * (d + c2*(2*b + 28*d) + 48*d2*d),
			//		df3dc = 48 * c * (1 + b2 + 28*bd + 141*d2),
			//		//df3dd = 24 * (b + 28*b * c2 + 2*d*(12 + 48*bd + 141*c2 + 225*d2) + d2*(48*b + 450*d));
			//		df3dd = 24 * (b + 28*b * c2 + d*(24 + 96*bd + 282*c2 + 450*d2) + d2*(48*b + 450*d));
        return new DoubleMatrix(new double[][] {
        	{df1db, df1dc, df1dd},
        	{df2db, df2dc, df2dd},
            {df3db, df3dc, df3dd}});
	}//flDeriv
	
	
//uses standardized moments - screwy equations	
//	//Given the fleishman coefficients, and a target skew and kurtois - equations from thesis
//    //this function will have a root if the coefficients give the desired skew and kurtosis
//    //F = -c + bZ + cZ^2 + dZ^3, where Z ~ N(0,1)
//	private DoubleMatrix flfunc_alt(double b, double c, double d, double skew, double exKurt) {
//        double b2 = b*b, c2 = c*c, c3 = c2*c, d2 = d*d, bd = b*d, t4bd = 24*bd;
//        //a2 + 2ac + b2 + 6bd + 3c4 + 15d2 = 1
//        //c2 - 2c2 + b2 + 6bd + 3c4 + 15d2 = 1
//        double _v = -c2 + 3*c2*c2 + b2 + 6*bd + 15*d2;
//        //a3 + 3a2c + 3ab2 + 18abd + 9ac2 + 45ad2 + 9b2c + 90bcd + 15c3 + 315cd2 = skew
//        //-c3 + 3c3 - 3cb2 - 18cbd - 9c3  - 45cd2 + 9b2c + 90bcd + 15c3 + 315cd2
//        //8c3                                     + 6b2c + 72bcd        + 270cd2
//        double _s = 8.0*c3 + 6.0*c*b2 + 72.0*c*bd + 270.0*c*d2;
//        //a4 + 4a3c + 6a2b2 + 36a2bd + 18a2c2 + 90a2d2 + 36ab2c + 360abcd + 60ac3 + 1260acd2 + 3b4 + 60b3d + 90b2c2 + 630b2d2 + 1260bc2d + 3780bd3 + 105c4 + 5670c2d2 + 10395d4 = exKurt
//        //c4 - 4c4  + 6c2b2 + 36c2bd + 18c4   + 90c2d2 - 36b2c2 - 360bc2d - 60c4  - 1260c2d2 + 3b4 + 60b3d + 90b2c2 + 630b2d2 + 1260bc2d + 3780bd3 + 105c4 + 5670c2d2 + 10395d4 = exKurt
//        //60c4      +                                                                        + 3b4 + 60b3d + 60b2c2 + 630b2d2 + 926bc2d  + 3780bd3 +         4500c2d2 + 10395d4 = exKurt
//         //
//        double _k = 3.0*b2*b2 + 10395.0*d2*d2 + 60.0*c2*c2 + 60.0*b2*c2 + 926.0*c2*bd + 4500.0*c2*d2 + 60.0*b2*bd +  630.0*b2*d2 + 3780.0*bd*d2;
//        return new DoubleMatrix(new double[] {_v - 1, _s - skew, _k - exKurt});		
//	}//flfunc
//
//	
//	//The deriviative of the flfunc above - from thesis
//    //returns jacobian
//	private DoubleMatrix flDeriv_alt(double b, double c, double d) {
//		double b2 = b*b, b3=b*b2, c2 = c*c, c3 = c2*c, c22 = 2.0*c2, d2 = d*d, d3 = d*d2, bd = b*d, bc=b*c, cd=c*d;
//		//matrix coeffs
//		//_v = -c2 + 3*c2*c2 + b2 + 6*bd + 15*d2;
//		double df1db = 2.0*b + 6.0*d, 
//				df1dc = -1.0*c + 12.0*c3, 
//				df1dd = 6.0*b + 30.0*d,
//		// _s = 8.0*c3 + 6.0*c*b2 + 72.0*c*bd + 270.0*c*d2;
//				df2db = 12.0*bc + 72.0*cd,								//12bc + 72cd 
//				df2dc = 24.0*c2 + 6.0*b2 + 72.0*bd + 270.0*d2,		//24.0c2 + 6b2 + 72bd + 270d2
//				df2dd = 72.0*bc + 540.0*cd,							//72cb + 540cd
//		//_k = 3.0*b2*b2 + 10395.0*d2*d2 + 60.0*c2*c2 + 60.0*b2*c2 + 926.0*c2*bd + 4500.0*c2*d2 + 60.0*b2*bd +  630.0*b2*d2 + 3780.0*bd*d2;
//		//deriv b : 12b3 + 120bc2 + 926c2d + 180b2d + 1260bd2 + 3780d3
//				df3db = 12.0*b3 + 120.0*b*c2 + 926.0*c2*d + 180.0*b2*d + 1260.0*b*d2 + 3780.0*d3,
//		//deriv c : 240*c3 + 120*b2c + 1852*bcd + 9000*c*d2 
//				df3dc = 240*c3 + 120*b2*c + 1852*b*c*d + 9000*c*d2,
//		//deriv d : 41580 * d3 + 926*c2 + 180*b2 + 2520*bd + 11340*d2
//				df3dd = 41580.0*d3 + 926.0*c2 + 180.0*b2 + 2520.0*bd + 11340*d2;
//	        return new DoubleMatrix(new double[][] {
//        	{df1db, df1dc, df1dd},
//        	{df2db, df2dc, df2dd},
//            {df3db, df3dc, df3dd}});
//	}//flDeriv
	
	//simple newton method solver
	private double[] newtonMethod(double b,double c,double d,double skew,double exKurtosis) {
        //Implements Newton's method to find a root of flfunc
		DoubleMatrix f = flfunc(b,c, d, skew, exKurtosis), delta, oldf = f.mul(2.0);
		//DoubleMatrix olderF = oldf.mul(2.0);
        DoubleMatrix J;
        double mult = 1.0, oldfMag=0, fMag = 0;
        int i = 0;
        for (i=0; i<maxIter; ++i) {
            if (isNewtonDone(f)){          break;   }//want to minimize f
            //get jacobian
            J = flDeriv(b,c, d);
            //find delta amt that minimizes J * [b,c,d]T = df  where df (f here) is [f1-1, f2-skew, f3-exkurt]T
            delta = (Solve.solve(J, f));
            mult = 10.0;
            //adaptive time step
            while (mult > .0001) {
	            do {
		            b -= mult*lrnRateAra[0] * delta.data[0];
		            c -= mult*lrnRateAra[1] * delta.data[1];
		            d -= mult*lrnRateAra[2] * delta.data[2];
		            oldf = f;oldfMag = f.norm2();
		            f = flfunc(b,c, d, skew, exKurtosis); fMag = f.norm2();	
	            } while (oldfMag > fMag) ;
	            b += mult*lrnRateAra[0] * delta.data[0];
	            c += mult*lrnRateAra[1] * delta.data[1];
	            d += mult*lrnRateAra[2] * delta.data[2];
	            f = oldf;
	            mult *=.5;
            }
            if(getFlag(debugIDX)) {msgObj.dispInfoMessage("myFleishFunc_Uni", "newtonMethod", "Newton iters to find coeffs : " + i + " : final f : " +_getNewtonFStr(f));}
        }
        if(getFlag(debugIDX)) {msgObj.dispInfoMessage("myFleishFunc_Uni", "newtonMethod", "Newton iters to find coeffs : " + i + " : final f : " +_getNewtonFStr(f));}
        return new double[] {b,c, d};
	}//newton
	
	private String _getCoeffsStr() {
		if(coeffs==null) {return "";}
		return String.format("%3.8f",coeffs[0])+","+ String.format("%3.8f",coeffs[1])+","+ String.format("%3.8f",coeffs[2])+","+ String.format("%3.8f",coeffs[3]);
	}//_getCoeffsStr
	private String _getRootsStr() {
		if((roots == null)||(roots.length == 0)){return "";}
		String res = "";
		for(int i=0;i<roots.length-1;++i) {	res+=String.format("%3.8f",roots[i])+",";}
		res+=String.format("%3.8f",roots[roots.length-1]);
		return res;
	}//_getRootsStr	
	private String _getNewtonFStr(DoubleMatrix f) {
		String res = "[";
		for(int i=0;i<f.data.length-1;++i) {			res+= String.format("%3.8f", f.data[i])+", " ;		}
		res +=String.format("%3.8f", f.data[f.data.length-1])+"]" ;		
		return res;
	}//_getNewtonFStr
	
	/**
	 * find functional inverse - given specific y value, find x such that y = func(x) -> x = func^-1(y) 
	 * i.e. find x value that will give f(x)==y - note y should not be shifted by mean and std
	 * @param yRaw
	 * @return
	 */
	public double calcInvF(double yRaw) {
		double mu = summary.mean(), std = summary.std();
		double y=(yRaw-mu)/std;
		double res = (y-coeffs[0])/coeffs[1], diff, fRes = 0, dfRes = 0;
		//double diffSq, convLimSq = convLim*convLim;
		//use newton method to find value
		int i = 0;
		int maxInvIter = 100;
		for (i=0; i<maxInvIter; ++i) {
			fRes = funcs[fStdIDX].apply(res);
			dfRes = funcs[fStdDeriveIDX].apply(res);
			diff = (fRes - y);
			//diffSq = diff * diff;
			if(Math.abs(diff) < convLim) {
			//if(Math.abs(oldDiff) < Math.abs(diff)) {System.out.println("iter " + i + " DIVERGING! : diff : " + String.format("%3.8f", diff) + " | oldDiff : " + String.format("%3.8f", oldDiff));	}
				if(getFlag(debugIDX)){
					msgObj.dispInfoMessage("myFleishFunc_Uni", "calcInvF", "iter " + i + " diff : " + String.format("%3.8f", diff) + "\ty :"+ y + " res : " + String.format("%3.8f", res) + " f(res) : "+ String.format("%3.8f", fRes)+ " dfRes : "+ String.format("%3.8f", dfRes) + "| coeffs :["+_getCoeffsStr()+"]" );//+ "\t f'(res) : " + String.format("%3.8f", dfRes));}
				}
				break;
			}
			res -= (fRes-y)/dfRes;
		}	
		//msgObj.dispMessage("myFleishFunc_Uni", "calcInvF", "iters to find inverse : " + i + " result : " + res + " y : " + y + " f(res) : "+ fRes + "| coeffs :["+_getCoeffsStr()+"]" );//+ "\t f'(res) : " + String.format("%3.8f", dfRes));
		return res;
		
	}//calcInvF
	
	//this takes a normal input, not a uniform input - TODO change this to take uniform input (??)
	@Override
	protected void buildFuncs() {
		double mu = summary.mean(), std = summary.std();//, var = summary.var();
		//actual functions
		funcs[fIDX] 		= x ->  {return ((coeffs[0] + x*(coeffs[1] +x*(coeffs[2]+ x*coeffs[3])))*std +  mu);		}; 
		//TODO find inverse of polynomial?  inverse of this function may not exist - need to use optimization process 
		funcs[fInvIDX] 		= (xinv -> null);
		//zigurat functions -> want pure normal distribution
		funcs[fStdIDX]		= x -> {return (coeffs[0] + x*(coeffs[1] +x*(coeffs[2]+ x*coeffs[3])));		};
		funcs[fInvStdIDX]	= (xinv -> null);
		//analytical derivatives
		funcs[fDerivIDX]	= x -> {return ((coeffs[1] +x*(2*coeffs[2]+ x*3*coeffs[3]))*std );}; //b + 2cx + 3dx^2 == b + x * (2c + 3dx)
		funcs[fStdDeriveIDX] = x -> {return (coeffs[1] +x*(2*coeffs[2]+ x*3*coeffs[3]));};                                                                   ;
		//integrals
		integrals[fIntegIDX] = (x -> integral_f(x[0],x[1]));
		integrals[fStdIntegIDX] = (x -> integral_fStd(x[0],x[1]));		
	}//buildFuncs
	
	//pdf - for fleishman distribution, this is not described by function
	@Override
	public double PDF(double x) {	
		double mu = summary.mean(), std = summary.std();
		//first find normal draw result x' that yielded x when fed to polynomial : fl(xprime) == x -> fl_inv(x) == xprime - inverting via newton method
		double xPrime = calcInvF(x);
		//now evaluate std derivative @ xPrime
		double derivVal = funcs[fStdDeriveIDX].apply(xPrime);
		
		return ((zigNormGen.getFunc().fStd(xPrime)*std + mu)/derivVal);
	}//PDF
	
	//fleish has multi-step process.  first generate normal value (x'), then feed to polynomial to yield final value (x).  
	//in this case, we want to find find cumulative prob value p(X<=x) for fleishman. 
	//first must find x' value that, when fed to underlying normal distribution, yields x
	@Override
	public double CDF(double x) {	
		//first find normal draw result x' that yielded x when fed to polynomial : fl(xprime) == x -> fl_inv(x) == xprime
		double xPrime = calcInvF(x);
		
		//find polynomial 
		//now we can find the CDF of transformed x by finding normFunc cdf of t and transforming it
		return zigNormGen.getFunc().CDF(xPrime);
		
	}	//need to find most negative value of function corresponding to 0 probability => coeffs[0] 

	@Override
	public double CDF_inv(double prob) {
		if(prob == 0) {prob+=probitBnd;} else if(prob == 1.0) {prob -= probitBnd;}
		//t is value of underlying normal that has given probability (p(x<=t) == prob)
		double t = zigNormGen.inverseCDF(prob);
		//this will be transformed value
		return f(t);
	}
	
	//evaluate integral
	@Override
	public double integral_f(Double x1, Double x2) {
		//TODO
		//definite integral of polynomial along with normal pdf - should we use gauss-hermite? only for infinite	
		double res = 0; 
		
		
		if(x1==Double.NEGATIVE_INFINITY) {				//cdf of x2 == .5 + .5 * error function x2/sqrt(2) 
			
//			//msgObj.dispMessage("myGaussianFunc", "integral_f", "CDF : x1 : "+x1 + " and  x2 : " + x2 + " Using x2");
//			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x2 - summary.mean())*invStdSclFact);
//			//cdf == .5*(1+erf(x/sqrt(2))) 
//			//res = halfVal.add(halfVal.multiply(erroFuncVal));		
//			res = .5 + .5 * erroFuncVal.doubleValue();			
		} else if (x2==Double.POSITIVE_INFINITY) {		//pos inf -> this is 1- CDF == Q function
//			//msgObj.dispMessage("myGaussianFunc", "integral_f", "Q func : x1 : "+x1 + " and  x2 : " + x2 + " Using x1");
//			BigDecimal erroFuncVal = quadSlvr.evalIntegral(errorFunc, 0.0, (x1 - summary.mean())*invStdSclFact);
//			//Q function is == 1 - (.5*(1+erf(x/sqrt(2))))
//			//res = BigDecimal.ONE.subtract(halfVal.add(halfVal.multiply(erroFuncVal)));		
//			res = 1.0 - (.5 + .5 * erroFuncVal.doubleValue());				
		} else {
			//find integral of f(normFunc.f_inv(x2)) - f(normFunc.f_inv(x1)) 
			//double
			
			//res = quadSlvr.evalIntegral(funcs[fIDX], x1, x2).doubleValue();
		}
		
		return res;
	}

	@Override
	public double integral_fStd(Double x1, Double x2) {
		double res = 0;

		return res;
	}

	@Override
	public double processResValByMmnts(double val) {	return summary.normToGaussTransform(val);}//public abstract double processResValByMmnts(double val);	

	@Override
	public void setOptionFlags(int[] _opts) {
	}

	@Override
	public int getRVFType() {return baseProbExpMgr.fleishRandVarIDX;}


}//class myFleishFunc

