package base_ProbTools.quadrature.base;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * class to manage an integration strategy on a functional object
 * @author john
 *
 */
public abstract class baseQuadrature {
	//# of wts/points used for quadrature or # of samples
	public int numPoints;
	//convergence tolerance to use for iterative derivation methods
	public double convTol;	
	//big decimal scale to use for big decimals in this integrator
    public int BDScale;		
	
	public final String name;
	public baseQuadrature(String _name) {name=_name;}//ctor
		
	/**
	 * evaluate a polynomial given by the passed coefficient matrix, at x
	 * @param lcoef
	 * @param n
	 * @param x
	 * @return
	 */
	protected BigDecimal evalPoly(BigDecimal [][] lcoef, int n, BigDecimal x) {
    	BigDecimal res = lcoef[n][n];
        for (int i = n; i > 0; --i) {res = (res.multiply(x)).add(lcoef[n][i - 1]);}		//((an * x + an-1) * x + an-2)...
        return res;
    }//evalPoly
	public abstract BigDecimal evalIntegral(Function<Double, Double> func, double min, double max);

	/**
	 * set the number of points/weights/samples, the convergence tolerance and the 
	 * scale for BigDecimals for this solver, and calculate any initial values and structures
	 * @param _numPoints
	 * @param _tol
	 * @param _BDScale
	 */
	public void setSolverVals(int _numPoints, double _tol, int _BDScale) {		
		numPoints = _numPoints;convTol=_tol; BDScale=_BDScale;
		//build precalced wts and abscissas for quadrature method
		preCalcIntegratorValues();
	}//setSolverVals

	/**
	 * any precalculated values used for this integrator
	 */
	protected abstract void preCalcIntegratorValues();
}//class baseQuadrature
