package experiments_PKG.probabilityExp;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.exp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public class TestGauss {
    
    static BigDecimal evalPoly(BigDecimal [][] lcoef, int n, BigDecimal x) {
        BigDecimal  res = lcoef[n][n];
        for (int i = n; i > 0; --i) {res = (res.multiply(x)).add(lcoef[n][i - 1]);}
        return res;
    }
 
    static BigDecimal[][] computeLegendreWeightsXvals(int numPoints, double tol) {
        int legScale = 18;
        BigDecimal[] xVals = new BigDecimal[numPoints],  wts = new BigDecimal[numPoints];
        BigDecimal[][] lcoef = new BigDecimal[numPoints + 1][numPoints + 1];
        for(int i=0;i<lcoef.length;++i) {for(int j=0;j<lcoef[i].length;++j) {lcoef[i][j]=new BigDecimal(0.0);}}
        //build coefficients of polynomials
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
            lcoef[n][0] = (negNm1BD.multiply(lcoef[nm2][0])).divide(nBD, legScale,RoundingMode.HALF_UP); 
            for (int i = 1; i <= n; ++i) {
                //System.out.println("n:"+n+" i:"+i);
                lcoef[n][i] = ((twoNm1BD.multiply(lcoef[nm1][i-1])).add(negNm1BD.multiply(lcoef[nm2][i]))).divide(nBD, legScale,RoundingMode.HALF_UP );
            }
        }
        //calculate weights and xVals
        BigDecimal x, xSq, x1, legEvalN,legEvalNm1, xDenom, numPointsBD= new BigDecimal(numPoints);
        double PiOvnp5=PI/(numPoints + 0.5);
        for (int i = 1; i <= xVals.length; ++i) {
            x = new BigDecimal(cos(PiOvnp5 * (i - 0.25)));
            do {//repeat until converges
                x1 = new BigDecimal(x.toString());
                legEvalN = evalPoly(lcoef,numPoints, x);
                legEvalNm1 = evalPoly(lcoef,numPoints-1, x);
                xSq = x.multiply(x);
                xDenom = (((x.multiply(legEvalN)).subtract(legEvalNm1)).divide(xSq.subtract(BigDecimal.ONE))).multiply(numPointsBD);
                x = x.subtract(legEvalN.divide(xDenom, legScale,RoundingMode.HALF_UP));
            } while ((x1.subtract(x)).abs().doubleValue() > tol);
            xSq = x.multiply(x);
            xVals[i-1] = new BigDecimal(x.toString());
            legEvalN = evalPoly(lcoef,numPoints, x);
            legEvalNm1 = evalPoly(lcoef,numPoints-1, x);                
            x1 = (((x.multiply(legEvalN)).subtract(legEvalNm1)).divide(xSq.subtract(BigDecimal.ONE), legScale,RoundingMode.HALF_UP)).multiply(numPointsBD);//legeDiff(lcoef,numPoints, x);
            BigDecimal denom = ((BigDecimal.ONE.subtract(xSq)).multiply(x1.multiply(x1)));
            wts[i-1] = new BigDecimal(2.0);
            wts[i-1] = wts[i-1].divide(denom, legScale,RoundingMode.HALF_UP);
        }
        return (new BigDecimal[][] {xVals, wts});
    }
 
    static BigDecimal legeInte(Function<Double, Double> f, double a, double b, BigDecimal[][] vals) {
        BigDecimal c1 = new BigDecimal((b - a) / 2.0), 
                c2 =  new BigDecimal((b + a) / 2.0); 
        BigDecimal sum = new BigDecimal(0);
        for (int i = 0; i < vals[0].length; ++i) {  
            sum = sum.add(vals[1][i].multiply(new BigDecimal(f.apply(c1.multiply(vals[0][i]).add(c2).doubleValue()))));
        }
        
        return c1.multiply(sum);
    }
 
    public static void main(String[] args) {
        double xLow = -10, xHigh = 1.0, tol=1.0e-16;
        int N = 40;//(int)((xHigh - xLow) * 1.2)+1;
        if (N<10) {N=10;}
        BigDecimal[][] vals = computeLegendreWeightsXvals(N,tol);
        System.out.print(""+N+" Roots found ");
        //double sclFact = 1.0/Math.sqrt(2.0*Math.PI);//for normal
        double ErfCoef = 2.0/Math.sqrt(Math.PI);//for error function
        //Function<Double, Double> func = (x -> sclFact * exp(-(x*x)/2.0)  );
        //Function<Double, Double> func = (x ->   exp(x)  );
        Function<Double, Double> func = (x ->  exp(-x)  );
        Function<Double, Double> ErrorFunc = (x ->  ErfCoef * exp(-(x*x)) );
        
        System.out.print(""+N+" Roots: ");       
        for (int i = 0; i < N; i++) {            System.out.printf(" %f", vals[0][i]);}
 
        System.out.print("\nWeights:");
        for (int i = 0; i < N; i++) {            System.out.printf(" %f", vals[1][i]);}
        
        BigDecimal half = new BigDecimal(.5);
        BigDecimal resBD = legeInte(func, xLow, xHigh, vals);
        BigDecimal resBDErf = legeInte(ErrorFunc, xLow, xHigh, vals);
        BigDecimal CDF_BD = resBDErf.multiply(half).add(half);
        
        double res = resBD.doubleValue();
        double resErf = resBDErf.doubleValue();
        double CDF = CDF_BD.doubleValue();
        double CDFCalc = .5 + .5*resErf;
        double calc = -exp(-xHigh) + exp(-xLow);
        double ratio = res/calc;
        
        
        
        System.out.printf("%nIntegrating Exp(x) over ["+xLow+", "+xHigh+"]:%n\t%10.18f,%ncompared to actual%n\t%10.18f%nRatio : %n\t%10.18f%n", res, calc,ratio);//only works because integral of e^x ==e^x 
        System.out.printf("%nIntegrating errorFunction(x) over ["+xLow+", "+xHigh+"]:%n\t%10.18f%nCDF as BigDecimal : \t%10.18f%nCDF as Double : \t%10.18f%n", resErf,CDF,CDFCalc); 
    }
}