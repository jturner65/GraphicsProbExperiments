package base_ProbTools.quadrature;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

import base_ProbTools.quadrature.base.baseQuadrature;

/**
 * gauss legendre quadrature of definite integrals of function f(x)
 * @author john
 */
public class myGaussLegenQuad extends baseQuadrature{
    //idx 0 == xvals, idx 1 == wts for gaussian quadrature with legendre polynomials for the integrator for this function
	protected BigDecimal[][] gaussQuad;

	public myGaussLegenQuad(String _name, int _numPoints, double _tol, int _BDScale) { 
		super(_name);
		setSolverVals(_numPoints, _tol, _BDScale);
	}

	@Override
	public BigDecimal evalIntegral(Function<Double, Double> func, double min, double max) {
		//expMgr.dispMessage("myGaussLegenQuad", "evalIntegral", "Integral : min : "+min + " and  max : " + max);
    	BigDecimal c1 = new BigDecimal((max - min) / 2.0), 
        		c2 =  new BigDecimal((max + min) / 2.0); 
    	BigDecimal sum = new BigDecimal(0);
        for (int i = 0; i < gaussQuad[0].length; ++i) {  
        	sum = sum.add(gaussQuad[1][i].multiply(new BigDecimal(func.apply(c1.multiply(gaussQuad[0][i]).add(c2).doubleValue()))));
        }        
        BigDecimal res = c1.multiply(sum);
        //expMgr.dispMessage("myGaussLegenQuad", "evalIntegral", "Result : " + res.toString());
        return res;
	}//evalIntegral
	
	
	//build precalced wts and abscissas for quadrature method
	@Override
	protected void preCalcIntegratorValues() {
     	
    	BigDecimal[][] lcoef = new BigDecimal[numPoints + 1][numPoints + 1];
    	for(int i=0;i<lcoef.length;++i) {for(int j=0;j<lcoef[i].length;++j) {lcoef[i][j]=new BigDecimal(0.0);}}
    	//build coefficients of polynomials to then be used to determine abscissas and wt vals
        lcoef[0][0] = new BigDecimal(1.0); 
        lcoef[1][1] = new BigDecimal(1.0);
        BigDecimal negNm1BD, twoNm1BD, nBD;
        int twoNm1,nm1,nm2;
        for (int n = 2; n < lcoef.length; ++n) { 
            twoNm1 = (2*n-1);
            nm1 = n-1; 
            nm2 = n - 2;
            twoNm1BD = new BigDecimal(twoNm1);
            nBD = new BigDecimal(n);
            negNm1BD = new BigDecimal(-nm1);
            lcoef[n][0] = (negNm1BD.multiply(lcoef[nm2][0])).divide(nBD, BDScale, RoundingMode.HALF_UP); 
            for (int i = 1; i <= n; ++i) {lcoef[n][i] = ((twoNm1BD.multiply(lcoef[nm1][i-1])).add(negNm1BD.multiply(lcoef[nm2][i]))).divide(nBD, BDScale,RoundingMode.HALF_UP );}
        }

        BigDecimal[] xVals = new BigDecimal[numPoints],  wts = new BigDecimal[numPoints];

        //calculate weights and xVals
        BigDecimal x, xSq, x1, legEvalN, legEvalNm1, xDenom, numPointsBD= new BigDecimal(numPoints);
        double PiOvnp5=Math.PI/(numPoints + 0.5);
        for (int i = 1; i <= xVals.length; ++i) {
            x = new BigDecimal(Math.cos(PiOvnp5 * (i - 0.25)));
            do {//repeat until converges
            	x1 = new BigDecimal(x.toString());
                legEvalN = evalPoly(lcoef,numPoints, x);
                legEvalNm1 = evalPoly(lcoef,numPoints-1, x);
                xSq = x.multiply(x);
                xDenom = (((x.multiply(legEvalN)).subtract(legEvalNm1)).divide(xSq.subtract(BigDecimal.ONE), BDScale,RoundingMode.HALF_UP)).multiply(numPointsBD);
                x = x.subtract(legEvalN.divide(xDenom, BDScale, RoundingMode.HALF_UP));
            } while ((x1.subtract(x)).abs().doubleValue() > convTol);
            xSq = x.multiply(x);
            xVals[i-1] = new BigDecimal(x.toString());
            legEvalN = evalPoly(lcoef,numPoints, x);
            legEvalNm1 = evalPoly(lcoef,numPoints-1, x);            	
            x1 = (((x.multiply(legEvalN)).subtract(legEvalNm1)).divide(xSq.subtract(BigDecimal.ONE), BDScale,RoundingMode.HALF_UP)).multiply(numPointsBD);//legeDiff(lcoef,numPoints, x);
            BigDecimal denom = ((BigDecimal.ONE.subtract(xSq)).multiply(x1.multiply(x1)));
            wts[i-1] = new BigDecimal(2.0);
            wts[i-1] = wts[i-1].divide(denom, BDScale,RoundingMode.HALF_UP);
        }
        gaussQuad = new BigDecimal[][] {xVals, wts};	
	}//calcWtsAndAbscissas
	
	
}//class myGaussLegenQuad