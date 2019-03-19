package graphProbExp_PKG;

import java.time.Instant;

public abstract class BaseProbExpMgr {
	//owning window for this experiment handler
	protected myDispWindow win;
	//papplet for visualization
	public static GraphProbExpMain pa;
	////////////////////////////////////////
	// gauss quadrature solver structures	
	//integral solvers for gaussian quadrature method used by this experiment
	protected myIntegrator[] quadSlvrs;	
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
	
	//types of random number generators implemented/supported so far
	public static final int 
		boundedRandGen			= 0,	//uses bounded pdf function (i.e. cosine)
		ziggRandGen 			= 1,	//ziggurat method to generate distribution
		fleishRandGen_UniVar	= 2,	//uses fleishman algorithm for univariate - needs first 4 moments
		linearTransformMap		= 3,	//just performs linear transformation mapping - does not actually represent a random function
		uniformTransformMap		= 4;	//just performs an order-based transformation from original grade to #/n where # is order in rank (from lowest to highest) and n is total # of grades
    public static final String[] randGenAlgNames = new String[] {"Bounded PDF Algorithm", "Ziggurat Algorithm", "Fleishman Univariate Polynomial Algorithm", "Linear Transformation Mapping", "Uniform Transformation Mapping"};
	
    //type of random variable function to use with ziggurat algorithm
    public static final int
    	normRandVarIDX			= 0,
    	gaussRandVarIDX			= 1,
    	cosRandVarIDX			= 2,
		cosCDFRandVarIDX 		= 3;
    public static final String[] randVarFuncNames = new String[] {"Normal Distribution", "Gaussian Distribution", "Cosine-PDF Distribution", "Cosine-PDF via sample-derived CDF"};
    	
    
	////////////////////////////////////////
	// internal functionality
	
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
	
	////////////////////////////////////////
	// visualization functionality
	
	//width of display space - used to shrink things if necessary when right sidebar menu is added
	protected float visScreenWidth;
	
	
	public BaseProbExpMgr(myDispWindow _win) {
		win = _win;
		pa=win.pa;
		setVisibleScreenWidth();
		//base class-specific flags, isolated to within this code only
		initBaseFlags();
		//init experiment-specific flags
		initFlags();
	
		//for display of time since experiment was built 
		Instant now = Instant.now();
		expMgrBuiltTime = now.toEpochMilli();//milliseconds since 1/1/1970 when this exec was built.
		//run once - all solvers for this experiment should be built
		buildSolvers();
		//initialize exp - in instance class
		initExp();
		
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
	
	
	public myRandVarFunc buildRandVarType (int _pdfType,  int _quadSlvrIdx, myProbSummary _summaryObj) {
		System.out.println("buildRandVarType : " + _pdfType);
		switch (_pdfType) {		
		   	case normRandVarIDX		: { return new myNormalFunc(this, quadSlvrs[_quadSlvrIdx]);}
	    	case gaussRandVarIDX	: { return new myGaussianFunc(this, quadSlvrs[_quadSlvrIdx], _summaryObj);}
	    	case cosRandVarIDX		: { return new myCosFunc(this, quadSlvrs[_quadSlvrIdx], _summaryObj);}			
	    	case cosCDFRandVarIDX		: { return new myCosFuncFromCDF(this, quadSlvrs[_quadSlvrIdx], _summaryObj);}			
			default : {return null;}		
		}
	}//myRandVarFunc
	
	//using default GaussLengendre and 256 zig's for ziggurat alg
	public myRandGen buildAndInitRandGen(int _type, int _pdfType, myProbSummary _summaryObj) {return buildAndInitRandGen(_type, _pdfType, GL_QuadSlvrIDX, 256, _summaryObj);}
	//must build rand gen through this method
	public myRandGen buildAndInitRandGen(int _type, int _pdfType, int _quadSlvrIdx, int _numZigRects, myProbSummary _summaryObj) {

		switch (_type) {
			case boundedRandGen : {
				//need to build a random variable generator function
				myRandVarFunc func = buildRandVarType(_pdfType, _quadSlvrIdx, _summaryObj);
				
				return new myBoundedRandGen(func, randGenAlgNames[_type]);}
			case ziggRandGen : {//ziggurat alg solver - will use zigg algorithm to generate a gaussian of passed momments using a uniform source of RVs
				//need to build a random variable generator function
				myRandVarFunc func = buildRandVarType(_pdfType, _quadSlvrIdx, _summaryObj);				
				//_numZigRects must be pwr of 2 - is forced to be if is not.  Should be 256
				return new myZigRandGen(func, _numZigRects, randGenAlgNames[_type]);}
			
			case fleishRandGen_UniVar : {
				//specify fleishman rand function with either moments or data - if only moments given, then need to provide hull as well
				myRandVarFunc func = new myFleishFunc_Uni(this, quadSlvrs[_quadSlvrIdx], _summaryObj, randGenAlgNames[_type]);
				return new myFleishUniVarRandGen(func,  randGenAlgNames[_type]);	}
			
			case linearTransformMap : {	
				//does not actually represent a random function generator, just represents a transformation, to be used by class rosters in ClassGradeExperiment				
				return new linearTransform(_summaryObj);}
			
			case uniformTransformMap : {
				//does not represent random func generator/model, just represents a mapping from grade to order-based 1/n..n/n equal partition of grade space
				return new uniformCountTransform(_summaryObj);}
			
			
			default	:	{		
				dispMessage("BaseProbExpMgr","buildAndInitRandGen","Unknown random generator type : " + _type + ".  Aborting.", true);
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
	
	//called whenever screen width changes due to showing/hiding the right side menu
	public void setVisibleScreenWidth() {
		visScreenWidth = win.curVisScrDims[0];
		setVisWidth_Priv();
	}//setVisibleScreenWidth
	public float getVisibleSreenWidth() {return visScreenWidth;}
	
	//set the experiment-specific quantities dependent on visible width of the display area - use this to shrink visualizations if necessary
	protected abstract void setVisWidth_Priv();

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
	
	//conduct a simple test on the passed random number generator - it will get a sample based on the pdf function given to generator
	//uses 64 bit random uniform val
	protected void smplTestRandNumGen(myRandGen gen, int numVals) {
		dispMessage("BaseProbExpMgr","testRandGen","Start synthesizing " + numVals+ " values using Gen : \n\t" + gen.getFuncDataStr(), true);
		double[] genVals = new double[numVals];
		for(int i=0;i<genVals.length;++i) {	
			//dispMessage("BaseProbExpMgr","testRandGen","Generating val : " + i);
			genVals[i] = gen.getSample();	
		}
		//now calculate mean value and
		dispMessage("BaseProbExpMgr","testRandGen","Finished synthesizing " + numVals+ " values using Gen : " + gen.name + " | Begin analysis of values.", true);
		myProbSummary analysis = new myProbSummary(genVals);
		dispMessage("BaseProbExpMgr","testRandGen","Analysis res of " + gen.name + " : " + analysis.getMomentsVals(), true);
	}//testGen
	
	//conduct a simple test on the passed random number generator - it will get a sample based on the pdf function given to generator
	//uses the "fast" implementation - 32 bit random uniform value
	protected void smplTestFastRandNumGen(myRandGen gen, int numVals) {
		dispMessage("BaseProbExpMgr","testRandGen","Start synthesizing " + numVals+ " values using Gen : \n\t" + gen.getFuncDataStr(), true);
		double[] genVals = new double[numVals];
		for(int i=0;i<genVals.length;++i) {	
			//dispMessage("BaseProbExpMgr","testRandGen","Generating val : " + i);
			genVals[i] = gen.getSampleFast();	
		}
		//now calculate mean value and
		dispMessage("BaseProbExpMgr","testRandGen","Finished synthesizing " + numVals+ " values using Gen : " + gen.name + " | Begin analysis of values.", true);
		myProbSummary analysis = new myProbSummary(genVals);
		dispMessage("BaseProbExpMgr","testRandGen","Analysis res of " + gen.name + " : " + analysis.getMomentsVals(), true);
	}//testGen
	
	
	//test method for calculating r for rand var function (used in ziggurat algorithm)
	public void testRCalc() {
		dispMessage("BaseProbExpMgr","testRCalc","Start test of r var calc",true);
		myRandVarFunc randVar = new myNormalFunc(this, quadSlvrs[GL_QuadSlvrIDX]);
		myProbSummary analysis = new myProbSummary( new double[] {2.0, 3.0}, 2);
		myRandVarFunc randGaussVar = new myGaussianFunc(this, quadSlvrs[GL_QuadSlvrIDX], analysis);
		randVar.dbgTestCalcRVal(256);
		
		dispMessage("BaseProbExpMgr","testRCalc","End test of r var calc",true);
		
	}//testRCalc
	
	//////////////////////////////
	// drawing
	
	public abstract void drawExp();
	
	
	
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
	public void dispMessageAra(String[] _sAra, String _callingClass, String _callingMethod, int _perLine, boolean onlyConsole) {
		String callingClassPrfx = getTimeStrFromProcStart() +"|" + _callingClass;		 
		for(int i=0;i<_sAra.length; i+=_perLine){
			String s = "";
			for(int j=0; j<_perLine; ++j){	
				if((i+j >= _sAra.length)) {continue;}
				s+= _sAra[i+j]+ "\t";}
			_dispMessage_base(callingClassPrfx,_callingMethod,s,onlyConsole);
		}
	}//dispMessageAra	
	public void dispMessage(String srcClass, String srcMethod, String msgText, boolean onlyConsole) {_dispMessage_base(getTimeStrFromProcStart() +"|" + srcClass,srcMethod,msgText,onlyConsole);	}	
	private void _dispMessage_base(String srcClass, String srcMethod, String msgText, boolean onlyConsole) {
		String msg = srcClass + "::" + srcMethod + " : " + msgText;
		if(onlyConsole) {
			System.out.println(msg);
		} else {
			win.pa.outStr2Scr(msg);
		}
	}//dispMessage
	
	/////////////////////////////
	// state flags specific to each experiment
	protected abstract void initFlags();
	public abstract void setFlag(int idx, boolean val);
	public final void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	public final boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}
	
	/////////////////////////////
	//init and manage state flags internal to base class computation
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

