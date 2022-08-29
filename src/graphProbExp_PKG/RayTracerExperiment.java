package graphProbExp_PKG;

import java.io.File;
import java.util.TreeMap;

import base_ProbTools.BaseProbExpMgr;
import base_RayTracer.myRTFileReader;
import base_RayTracer.scene.myScene;
import base_UI_Objects.windowUI.base.myDispWindow;
import base_Utils_Objects.io.MsgCodes;

public class RayTracerExperiment extends BaseProbExpMgr {
	//holds references to all loaded scenes
	public TreeMap<String, myScene> loadedScenes;
	
	//current scene dims
	public static int sceneCols = 300;
	public static int sceneRows = 300;
	private int[] transLoc = new int[] {0,0};
	//current scene name
	private String currSceneName = "", currDispSceneName = "";
	
	//file reader/interpreter
	public myRTFileReader rdr; 	

	
	//experiment-specific state flag bits - bits in array holding relevant process info
	public static final int
			debugIDX 			= 0;		
	public static final int numFlags = 1;	
	
	public RayTracerExperiment(myDispWindow _win) {
		super(_win);		
	}//ctor

	@Override
	public void initExp() {
		rdr = new myRTFileReader(myDispWindow.pa,".."+File.separator+"data"+File.separator+"txtrs"+File.separator);	
		loadedScenes = new TreeMap<String, myScene>();		
		
	}//initExp	

	//set values for RT scene experiment values
	public void setRTSceneExpVals(int cols, int rows, String _currFileName) {
		sceneCols = cols;
		sceneRows = rows;
		currSceneName = _currFileName;
		setTransLoc();		
	}//setRTSceneExpVals
	
	private void setTransLoc() {
		//layout is for 2 images side by side, first is standard dist calc, 2nd is using modified cos dist calcs
		//with 3rd image beneath showing difference (upsized by 2)
		int stLocX = (int) ((visScreenWidth- (2*sceneCols))/2.0f);
		transLoc[0] = (0>stLocX ? 0 : stLocX);
		int stLocY = (int) ((win.rectDim[3]-sceneRows)/6.0f);	
		transLoc[1] = (0 > stLocY ? 0 : stLocY);
	}
	
	public void setFlipNorms() {
		myScene s = loadedScenes.get(currSceneName);
		if(s!=null) {s.flipNormal();}
	}
	
	public void startRayTrace() {		
		myScene tmp = rdr.readRTFile(loadedScenes, currSceneName, null, sceneCols, sceneRows);//pass null as scene so that we don't add to an existing scene
		msgObj.dispMessage("RayTracerExperiment", "startRayTrace", "Done with readRTFile", MsgCodes.info1);
		//returns null means not found
		if(null==tmp) {currSceneName = "";}
		currDispSceneName = currSceneName;
	}
	
	@Override
	protected void buildSolvers_indiv() {
	}
	//update all rand gen objects for this function, including updating rand var funcs
	@Override
	protected void updateAllRandGens_Priv() {
	}
	@Override
	protected void setVisWidth_Priv() {
		if(transLoc != null) {	setTransLoc();	}
	}

	@Override
	public boolean checkMouseClickInExp2D(int msx, int msy, int btn) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkMouseDragMoveInExp2D(int msx, int msy, int btn) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setMouseReleaseInExp2D() {
		// TODO Auto-generated method stub
	}
	@Override
	public void drawExp() {
		pa.pushMatState();
			pa.disableLights();
			pa.translate(transLoc[0],transLoc[1],0);
			myScene s = loadedScenes.get(currDispSceneName);
			if(s!=null) {s.draw();}		
			pa.translate(sceneCols,0,0);
			//TODO needs to be alt scene, using reduced cosine dist
			if(s!=null) {s.draw();}
			pa.pushMatState();
			pa.translate(-sceneCols,sceneRows,0);
			//draw diff image
			pa.scale(2.0f);
			if(s!=null) {s.draw();}
			pa.popMatState();
		pa.popMatState();
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
