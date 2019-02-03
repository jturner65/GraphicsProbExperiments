package graphProbExp_PKG;

import java.time.Instant;

public abstract class BaseProbExpMgr {
	//owning window for this experiment handler
	protected myDispWindow win;
	public static GraphProbExpMain pa;
	////////////////////////////////////////
	// gauss quadrature solver structures	
	//integral solvers for gaussian quadrature method used by this experiment
	protected myGaussQuad[] quadSlvrs;	
	//types of solvers for comparison
	public static final int
		GL_QuadSlvrIDX 		= 0;			//gaussian legendre solver
	public static final int numGaussSlvrs = 1;
	public static final String[] quadSlvrNames = new String[] {"Gauss-Legendre"};
	
	//index of current solver
	protected int curQuadSolverIDX = 0;	
	//# of points/wts used for gaussian quad
	public int numQuadPoints = 50;
	//convergence tolerance to use for iterative derivation methods
	public double quadConvTol = 1.0e-16;	
	//quadrature calculation big decimal scale to use
    public int quadBDScale = 18;
    
	////////////////////////////////////////
    // random var generators - take uniform input and return desired distribution
    //structure holding random number generators used in this experiment
    //public TreeMap<
	
	//types of random number generators implemented/supported so far
	public static final int 
		ziggRandGen 		= 0;	
	
    public static final String[] randGenAlgNames = new String[] {"Ziggurat Algorithm"};
	
	
	////////////////////////////////////////
	// internal functions
	
	//time of current process start, from initial construction of mapmgr - TODO use this to monitor specific process time elapsed.  set to 0 at beginning of a particular process, then measure time elapsed in process
	protected long curProcStartTime;
	//time mapMgr built, in millis - used as offset for instant to provide smaller values for timestamp
	protected final long expMgrBuiltTime;	
	//state flags used by instancing experiments
	protected int[] stFlags;					
	
	
	//internal to base class state flags - bits in array holding relevant process info restricted to base class
	private int[] _baseStFlags;						
	private static final int
			_BaseDebugIDX 				= 0;		
	private static final int _BaseNumStFlags = 1;	
	
	public BaseProbExpMgr(myDispWindow _win) {
		win = _win;
		pa=win.pa;
		//base class-specific flags, isolated to within this code only
		initBaseFlags();
		//init experiment-specific flags
		initFlags();
	
		//for display of time since experiment was built 
		Instant now = Instant.now();
		expMgrBuiltTime = now.toEpochMilli();//milliseconds since 1/1/1970 when this exec was built.
		//run once - all solvers for this experiment should be built
		buildSolvers();
	}//ctor
	
	//(re)init this experiment - specific functionality for each instance class
	public abstract void initExp();
	
	//build various quadrature solvers to experiment with different formulations
	private final void buildSolvers() {
		//build quadrature solvers if not built already
		quadSlvrs = new  myGaussQuad[numGaussSlvrs];
		quadSlvrs[GL_QuadSlvrIDX] = new myGaussLegenQuad(this,quadSlvrNames[GL_QuadSlvrIDX],numQuadPoints, quadConvTol, quadBDScale);
		curQuadSolverIDX = 0;
		//instance specific funcitionality
		buildSolvers_indiv();
	}//buildSolvers
	
	//experiment instance-specific solver building funcitionality
	protected abstract void buildSolvers_indiv();
	
	//must build rand gen through this method
	//momments will hold mean, std, and skew, and kurt, if set
	public myRandGen buildAndInitRandGen(int _type, int _quadSlvrIdx, int _numZigRects, double[] _mmnts) {
		myRandGen randGen;
		switch (_type) {
			case ziggRandGen : {//ziggurat alg solver - will use zigg algorithm to generate a gaussian of passed momments using a uniform source of RVs
				//need to build a random variable generator function
				myRandVarFunc func = new myGaussianFunc(this, quadSlvrs[GL_QuadSlvrIDX], _mmnts[0], _mmnts[1]);
				//_numZigRects must be pwr of 2 - is forced to be if is not.  Should be 256
				randGen = new myZigRandGen(func, _numZigRects, randGenAlgNames[GL_QuadSlvrIDX]);	
				
				return randGen;}
			default	:	{		
				dispMessage("BaseProbExpMgr","buildAndInitRandGen","Unknown random generator type : " + _type + ".  Aborting.");
				return null;
			}
		}//switch
		
	}//buildAndInitRandGen
	
	//set values specific to solver
	public void setSolverVals(int _numPoints, double _tol, int _BDScale) {
		numQuadPoints = _numPoints;
		quadConvTol = _tol;	
		quadBDScale = _BDScale;
		quadSlvrs[curQuadSolverIDX].setSolverVals(numQuadPoints, quadConvTol, quadBDScale);
		//set this solver for all random generators
		
	}//setSolverVals

	//check mouse over/click in 2d experiment; if btn == -1 then mouse over
	public abstract boolean checkMouseClickInExp2D(int msx, int msy, int btn);

	//check mouse over/click in 2d experiment; if btn == -1 then mouse over
	public abstract boolean checkMouseDragMoveInExp2D(int msx, int msy, int btn);

	//notify all exps that mouse has been released
	public abstract void setMouseReleaseInExp2D();

	//sets which solver will be used for integration for this experiment
	public void setCurSolver(int idx) {
		if(curQuadSolverIDX == idx) {return;}
		curQuadSolverIDX = idx;
		quadSlvrs[curQuadSolverIDX].setSolverVals(numQuadPoints, quadConvTol, quadBDScale);
		//set this solver for all random generators
	}
	
	//conduct a simple test on the passed random number generator
	protected void smplTestRandNumGen(myRandGen gen, int numVals) {
		dispMessage("BaseProbExpMgr","testRandGen","Start synthesizing " + numVals+ " values using Gen : \n\t" + gen.getFuncDataStr());
		double[] genVals = new double[numVals];
		for(int i=0;i<genVals.length;++i) {	
			//dispMessage("BaseProbExpMgr","testRandGen","Generating val : " + i);
			genVals[i] = gen.getGaussian();	
		}
		//now calculate mean value and
		dispMessage("BaseProbExpMgr","testRandGen","Finished synthesizing " + numVals+ " values using Gen : " + gen.name + " | Begin analysis of values.");
		myProbAnalysis analysis = new myProbAnalysis(win.pa, genVals, gen);
		dispMessage("BaseProbExpMgr","testRandGen","Analysis res of " + gen.name + " : " + analysis.getMoments());
	}//testGen
	
	
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
	
	/////////////////////////////
	// state flags specific to each experiment
	protected abstract void initFlags();
	public abstract void setAllFlags(int[] idxs, boolean val);
	public abstract void setFlag(int idx, boolean val);
	public abstract boolean getFlag(int idx);
	
	/////////////////////////////
	//init and manage state flags - internal to base class computation
	private void initBaseFlags(){_baseStFlags = new int[1 + _BaseNumStFlags/32]; for(int i = 0; i<_BaseNumStFlags; ++i){setBaseFlag(i,false);}}
	private void setAllBaseFlags(int[] idxs, boolean val) {for (int idx : idxs) {setBaseFlag(idx, val);}}
	private void setBaseFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		_baseStFlags[flIDX] = (val ?  _baseStFlags[flIDX] | mask : _baseStFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case _BaseDebugIDX : {break;}	
			
		}
	}//setFlag		
	private boolean getBaseFlag(int idx){int bitLoc = 1<<(idx%32);return (_baseStFlags[idx/32] & bitLoc) == bitLoc;}		


}//base class for probability experiments

