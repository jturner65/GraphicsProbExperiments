package graphProbExp_PKG;

import java.util.concurrent.ThreadLocalRandom;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_ProbTools.baseProbExpMgr;
import base_ProbTools.randGenFunc.gens.base.myRandGen;
import base_StatsTools.summary.myProbSummary_Dbls;
import base_Utils_Objects.io.messaging.MessageObject;
/**
 * this class will manage probability-based graphical experiments
 * @author john
 *
 */
public class myProbExpMgr extends baseProbExpMgr{	
	public IRenderInterface pa;
	//random # generator
	protected myRandGen nrmlGen, gaussGen;
	
	//experiment-specific state flag bits - bits in array holding relevant process info
	public static final int
			debugIDX 					= 0;		
	public static final int numFlags = 1;	
	
	
	public myProbExpMgr(IRenderInterface _pa, MessageObject _msgObj, float[] _curVisScrDims) {
		super(_msgObj, _curVisScrDims);
		pa=_pa;
	}//ctor

	//build solvers specific to this experiment - called after base class solver init
	@Override
	protected void buildSolvers_indiv() {

	}//buildSolvers
	
	//called at end of ctor and whenever experiment needs to be re-instanced
	@Override
	public final void initExp() {
		nrmlGen = buildAndInitRandGen(ziggRandGen, normRandVarIDX, new myProbSummary_Dbls(new double[] {0,1,0,0},2));	
		gaussGen = buildAndInitRandGen(ziggRandGen, gaussRandVarIDX, new myProbSummary_Dbls(new double[] {3,25.9,0,0},2));	
	}//initExp
	
	//update all rand gen objects for this function, including updating rand var funcs
	@Override
	protected void updateAllRandGens_Priv() {
		// TODO Auto-generated method stub
		
	}

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
	
	@Override
	public void drawExp() {		
	}

	public void testRandGen(int numVals) {
		msgObj.dispInfoMessage("myProbExpMgr","testRandGen","Start test of random normal gen of " +numVals + " vals from rand gen with momments : " + nrmlGen.getFuncDataStr());
		smplTestRandNumGen(nrmlGen, numVals);
		smplTestRandNumGen(gaussGen, numVals);
		msgObj.dispInfoMessage("myProbExpMgr","testRandGen","Start test of ThreadLocalRandom random gaussian gen of " +numVals + " vals.");
		double[] genVals = new double[numVals];
		//now test standard distribution of same # of values
		double mean = gaussGen.getMean(), std = gaussGen.getStd();
		for(int i=0;i<genVals.length;++i) {	genVals[i] = mean + (std*ThreadLocalRandom.current().nextGaussian());		}
		msgObj.dispInfoMessage("myProbExpMgr","testRandGen","Finished synthesizing " + numVals +" gaussian vals ~ N(" + mean + ","+std +") using ThreadLocalRandom random gaussian");
		myProbSummary_Dbls analysis = new myProbSummary_Dbls(genVals);
		msgObj.dispInfoMessage("myProbExpMgr","testRandGen","Analysis res of TLR.nextGauss : " + analysis.getMomentsVals());
	}//testRandGen
		
	
	/////////////////////////////
	//init and manage state flags
	@Override
	protected void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	@Override
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX : {break;}				
		}
	}//setFlag		

}//class myProbExpMgr



