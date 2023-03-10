package experiments_PKG.rayTracerProbExp.experiment;

import base_Render_Interface.IRenderInterface;
import base_ProbTools.baseProbExpMgr;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.ui.base.Base_RayTracerWin;
import base_Utils_Objects.dataAdapter.Base_DataAdapter;
import experiments_PKG.rayTracerProbExp.ui.RayTracerExpUIUpdater;

public class RayTracerExperiment extends baseProbExpMgr {
	public IRenderInterface ri;
	
	public Base_RayTracerWin win;
	
	//current scene dims
	public static int sceneCols = 300;
	public static int sceneRows = 300;
	private int[] transLoc = new int[] {0,0};
	
	//experiment-specific state flag bits - bits in array holding relevant process info
	public static final int
			debugIDX 			= 0;		
	public static final int numFlags = 1;	
	
	public RayTracerExperiment(Base_RayTracerWin _win, float[] _curVisScrDims) {
		super(_win.getMsgObj(), _curVisScrDims);	
		win = _win;
		ri = Base_RayTracerWin.ri;		
	}//ctor

	@Override
	public void initExp() {}//initExp	
	

	@Override
	protected Base_DataAdapter initUIDataUpdater(Base_DataAdapter dataUpdate) {
		// TODO Auto-generated method stub
		return new RayTracerExpUIUpdater((RayTracerExpUIUpdater)dataUpdate);
	}

	@Override
	protected void updateUIDataValues_Priv() {
		RayTracerExpUIUpdater tmpUpdater = (RayTracerExpUIUpdater)uiDataValues;
		setRTSceneExpVals(tmpUpdater.getNumSceneCols(),tmpUpdater.getNumSceneRows());
	}

	

	//set values for RT scene experiment values
	protected void setRTSceneExpVals(int cols, int rows) {
		sceneCols = cols;
		sceneRows = rows;
		setTransLoc();		
	}//setRTSceneExpVals
	
	private void setTransLoc() {
		//layout is for 2 images side by side, first is standard dist calc, 2nd is using modified cos dist calcs
		//with 3rd image beneath showing difference (upsized by 2)
		int stLocX = (int) ((visScreenWidth- (2*sceneCols))/2.0f);
		transLoc[0] = (0>stLocX ? 0 : stLocX);
		int stLocY = (int) ((curVisScrDims[1]-sceneRows)/6.0f);	
		transLoc[1] = (0 > stLocY ? 0 : stLocY);
	}
	
	@Override
	protected void buildSolvers_Indiv() {}
	//update all rand gen objects for this function, including updating rand var funcs
	@Override
	protected void updateAllRandGens_Priv() {}
	@Override
	protected void setVisWidth_Priv() {
		if(transLoc != null) {	setTransLoc();	}
	}

	@Override
	public boolean checkMouseClickInExp2D(int msx, int msy, int btn) {	return false;}

	@Override
	public boolean checkMouseDragMoveInExp2D(int msx, int msy, int btn) {return false;}

	@Override
	public void setMouseReleaseInExp2D() {}
	
	@Override
	public void drawExp() {
		ri.pushMatState();
			ri.disableLights();
			ri.translate(transLoc[0],transLoc[1],0);
			Base_Scene s = win.getCurrScene();
			if(s!=null) {s.draw();}		
			ri.translate(sceneCols,0,0);
			//TODO needs to be alt scene, using reduced cosine dist
			if(s!=null) {s.draw();}
			ri.pushMatState();
			ri.translate(-sceneCols,sceneRows,0);
			//draw diff image
			ri.scale(2.0f);
			if(s!=null) {s.draw();}
			ri.popMatState();
		ri.popMatState();
	}

	/////////////////////////////
	//init and manage state flags
	@Override
	protected void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}

	@Override
	public void setFlag(int idx, boolean val) {
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX : {break;}	
			
		}//switch
	}//setFlag

}//class RayTracerExperiment
