package graphProbExp_PKG;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * this class will manage probability-based graphical experiments
 * @author john
 *
 */
public class myProbExpMgr {
	//owning window for this experiment handler
	myDispWindow win;
	///////////
	// gauss quadrature solver structures
	
	//integral solvers for gaussian quadrature method used by this experiment
	protected myGaussQuad[] quadSlvrs;	
	//types of solvers for comparison
	public static final int
		GL_QuadSlvrIDX 		= 0;			//gaussian legendre solver
	public static final int numGaussSlvrs = 1;
	//types of random number generators
	public static final int 
		ziggRandGen 		= 0;
	
	
	
	//index of current solver
	private int curQuadSolverIDX = 0;	
	//# of points/wts used for gaussian quad
	public int numQuadPoints = 50;
	//convergence tolerance to use for iterative derivation methods
	public double quadConvTol = 1.0e-16;	
	//quadrature calculation big decimal scale to use
    public int quadBDScale = 18;
	
	//an instance of a fleischman solver
	protected myFleishSolver flSlvr;
	
	//random # generator
	protected myRandGen nrmlGen, gaussGen;

	
	//state flags - bits in array holding relevant process info
	private int[] stFlags;						
	public static final int
			debugIDX 					= 0;		
	public static final int numFlags = 1;	
	
	
	//time of current process start, from initial construction of mapmgr - TODO use this to monitor specific process time elapsed.  set to 0 at beginning of a particular process, then measure time elapsed in process
	private long curProcStartTime;
	//time mapMgr built, in millis - used as offset for instant to provide smaller values for timestamp
	private final long expMgrBuiltTime;
	
	public myProbExpMgr(myDispWindow _win) {
		win = _win;
		initFlags();
		//for display of time since experiment was built occur
		Instant now = Instant.now();
		expMgrBuiltTime = now.toEpochMilli();//milliseconds since 1/1/1970 when this exec was built.
		//run once
		buildSolvers();
		initExp();		
	}//ctor
	
	//build various quadrature solvers to experiment with different formulations
	private void buildSolvers() {
		//build quadrature solvers if not built already
		quadSlvrs = new  myGaussQuad[numGaussSlvrs];
		quadSlvrs[GL_QuadSlvrIDX] = new myGaussLegenQuad(this,numQuadPoints, quadConvTol, quadBDScale);
		curQuadSolverIDX = 0;
		flSlvr = new myFleishSolver();
	}//buildSolvers
	
	//initialize/reinitialize experiment
	private void initExp() {		
		nrmlGen = buildAndInitRandGen(ziggRandGen, GL_QuadSlvrIDX, 256, new double[] {0,1,0,0});	
		gaussGen = buildAndInitRandGen(ziggRandGen, GL_QuadSlvrIDX, 256, new double[] {3,2.4,0,0});	
	}//initExp

	public void setCurSolver(int idx) {
		if(curQuadSolverIDX == idx) {return;}
		curQuadSolverIDX = idx;
		quadSlvrs[curQuadSolverIDX].setSolverVals(numQuadPoints, quadConvTol, quadBDScale);
		
	}
	
	public void setSolverVals(int _numPoints, double _tol, int _BDScale) {
		numQuadPoints = _numPoints;
		quadConvTol = _tol;	
		quadBDScale = _BDScale;
	}//setSolverVals
	
	//must build rand gen through this method
	//momments will hold mean, std, and skew, and kurt, if set
	public myRandGen buildAndInitRandGen(int _type, int _quadSlvrIdx, int _numZigRects, double[] _mmnts) {
		myRandGen randGen;
		switch (_type) {
			case ziggRandGen : {//ziggurat alg solver
				//need to build a random variable generator function
				myRandVarFunc func = new myGaussianFunc(this, _mmnts[0], _mmnts[1]);
				func.setQuadSolver(quadSlvrs[_quadSlvrIdx]);
				func.setZigVals(_numZigRects);				
				randGen = new myZigRandGen(func);	
				
				return randGen;}
			default	:	{		
				dispMessage("myProbExpMgr","buildAndInitRandGen","Unknown random generator type : " + _type + ".  Aborting.");
				return null;
			}
		}//switch
		
	}//buildAndInitRandGen
	
	private void testGen(myRandGen gen, int numVals) {
		dispMessage("myProbExpMgr","testRandGen","Start Analysis of Gen : \n\t" + gen.getFuncDataStr());
		double[] genVals = new double[numVals];
		for(int i=0;i<genVals.length;++i) {		genVals[i] = gen.getGaussian();	}
		//now calculate mean value and 
		myProbAnalysis analysis = new myProbAnalysis(win.pa, genVals);
		dispMessage("myProbExpMgr","testRandGen","Analysis res of Zigg : " + analysis.getMoments());
	}//testGen
	
	
	public void testRandGen(int numVals) {
		dispMessage("myProbExpMgr","testRandGen","Start test of random normal gen of " +numVals + " vals from rand gen with momments : " + nrmlGen.getFuncDataStr());
		testGen(nrmlGen, numVals);
		testGen(gaussGen, numVals);
		
		double[] genVals = new double[numVals];
		//now test standard distribution of same # of values
		genVals = new double[numVals];
		for(int i=0;i<genVals.length;++i) {	genVals[i] = gaussGen.func.getMean() + (gaussGen.func.getStd()*ThreadLocalRandom.current().nextGaussian());		}
		myProbAnalysis analysis = new myProbAnalysis(win.pa, genVals);
		dispMessage("myProbExpMgr","testRandGen","Analysis res of TLR.nextGauss : " + analysis.getMoments());
	}//testRandGen
	
	
	//test method for calculating r for rand var function
	public void testRCalc() {
		dispMessage("myProbExpMgr","testRCalc","Start test of r var calc");
		myRandVarFunc randVar = new myNormalFunc(this);
		myRandVarFunc randGaussVar = new myGaussianFunc(this, 2.0, 3.0);
		randVar.setQuadSolver(quadSlvrs[GL_QuadSlvrIDX]);
		randVar.dbgTestCalcRVal(256);
		
		dispMessage("myProbExpMgr","testRCalc","End test of r var calc");
		
	}//testRCalc
	
	/////////////////////////////
	// utility : time stamp; display messages; state flags
	
	//get time from "start time" (ctor run for map manager)
	protected long getCurTime() {			
		Instant instant = Instant.now();
		return instant.toEpochMilli() - expMgrBuiltTime;//milliseconds since 1/1/1970, subtracting when mapmgr was built to keep millis low		
	}//getCurTime() 	
	//returns a positive int value in millis of current world time since sim start
	protected long getCurRunTimeForProc() {	return getCurTime() - curProcStartTime;}	
	protected String getTimeStrFromProcStart() {return  getTimeStrFromPassedMillis(getCurRunTimeForProc());}
	//get a decent display of passed milliseconds elapsed
	//	long msElapsed = getCurRunTimeForProc();
	protected String getTimeStrFromPassedMillis(long msElapsed) {
		long ms = msElapsed % 1000, sec = (msElapsed / 1000) % 60, min = (msElapsed / 60000) % 60, hr = (msElapsed / 3600000) % 24;	
		String res = String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
		return res;
	}//getTimeStrFromPassedMillis	
	
	//show array of strings, either just to console or to applet window
	public void dispMessageAra(String[] _sAra, String _callingClass, String _callingMethod, int _perLine) {
		String callingClassPrfx = getTimeStrFromProcStart() +"|" + _callingClass;		 
		for(int i=0;i<_sAra.length; i+=_perLine){
			String s = "";
			for(int j=0; j<_perLine; ++j){	
				if((i+j >= _sAra.length)) {continue;}
				s+= _sAra[i+j]+ "\t";}
			_dispMessage_base(callingClassPrfx,_callingMethod,s);
		}
	}//dispMessageAra	
	public void dispMessage(String srcClass, String srcMethod, String msgText) {_dispMessage_base(getTimeStrFromProcStart() +"|" + srcClass,srcMethod,msgText);	}	
	private void _dispMessage_base(String srcClass, String srcMethod, String msgText) {
		String msg = srcClass + "::" + srcMethod + " : " + msgText;
		win.pa.outStr2Scr(msg);
	}//dispMessage
	
	//init and manage state flags
	private void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX : {break;}	
			
		}
	}//setFlag		
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}		


}//class myProbExpMgr
