package graphProbExp_PKG;

import java.util.concurrent.ThreadLocalRandom;

/**
 * this class will manage probability-based graphical experiments
 * @author john
 *
 */
public class myProbExpMgr extends BaseProbExpMgr{	
	//random # generator
	protected myRandGen nrmlGen, gaussGen;

	//an instance of a fleischman solver
	protected myFleishSolver flSlvr;
	
	
	//experiment-specific state flag bits - bits in array holding relevant process info
	public static final int
			debugIDX 					= 0;		
	public static final int numFlags = 1;	
	
	
	public myProbExpMgr(myDispWindow _win) {
		super(_win);
		dispMessage("myProbExpMgr", "myProbExpMgr ctor", "Ctor");
		initExp();
	}//ctor

	//build solvers specific to this experiment - called after base class solver init
	@Override
	protected void buildSolvers_indiv() {
		flSlvr = new myFleishSolver();
	}//buildSolvers
	

	//called at end of ctor and whenever experiment needs to be re-instanced
	@Override
	public final void initExp() {		
		nrmlGen = buildAndInitRandGenFromMoments(ziggRandGen, GL_QuadSlvrIDX, 256, new double[] {0,1,0,0});	
		gaussGen = buildAndInitRandGenFromMoments(ziggRandGen, GL_QuadSlvrIDX, 256, new double[] {3,25.9,0,0});	
	}//initExp
	
	//this is called whenever screen width is changed - used to modify visualizations if necessary
	@Override
	protected void setVisWidth_Priv() {
		
	}//setVisWidth_Priv

	//check mouse over/click in 2d experiment - btn == -1 is mouse over
	@Override	
	public boolean checkMouseClickInExp2D(int msx, int msy, int btn) {
		return false;
	};
	
	//check mouse over/click in 2d experiment - btn == -1 is mouse over
	@Override	
	public boolean checkMouseDragMoveInExp2D(int msx, int msy, int btn) {
		return false;
	};
	
	//check mouse over/click in 2d experiment; if btn == -1 then mouse over
	@Override	
	public void setMouseReleaseInExp2D() {	
	}

	public void testRandGen(int numVals) {
		dispMessage("myProbExpMgr","testRandGen","Start test of random normal gen of " +numVals + " vals from rand gen with momments : " + nrmlGen.getFuncDataStr());
		smplTestRandNumGen(nrmlGen, numVals);
		smplTestRandNumGen(gaussGen, numVals);
		dispMessage("myProbExpMgr","testRandGen","Start test of ThreadLocalRandom random gaussian gen of " +numVals + " vals.");
		double[] genVals = new double[numVals];
		//now test standard distribution of same # of values
		genVals = new double[numVals];
		double mean = gaussGen.func.getMean(), std = gaussGen.func.getStd();
		for(int i=0;i<genVals.length;++i) {	genVals[i] = mean + (std*ThreadLocalRandom.current().nextGaussian());		}
		dispMessage("myProbExpMgr","testRandGen","Finished synthesizing " + numVals +" gaussian vals ~ N(" + mean + ","+std +") using ThreadLocalRandom random gaussian");
		myProbSummary analysis = new myProbSummary(genVals);
		dispMessage("myProbExpMgr","testRandGen","Analysis res of TLR.nextGauss : " + analysis.getMomentsVals());
	}//testRandGen
	
	
	//test method for calculating r for rand var function
	public void testRCalc() {
		dispMessage("myProbExpMgr","testRCalc","Start test of r var calc");
		myRandVarFunc randVar = new myNormalFunc(this, quadSlvrs[GL_QuadSlvrIDX]);
		myProbSummary analysis = new myProbSummary( new double[] {2.0, 3.0}, 2);
		myRandVarFunc randGaussVar = new myGaussianFunc(this, quadSlvrs[GL_QuadSlvrIDX], analysis);
		randVar.dbgTestCalcRVal(256);
		
		dispMessage("myProbExpMgr","testRCalc","End test of r var calc");
		
	}//testRCalc
	
	
	/////////////////////////////
	//init and manage state flags
	@Override
	protected void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	@Override
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	@Override
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX : {break;}				
		}
	}//setFlag		
	@Override
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}


}//class myProbExpMgr



