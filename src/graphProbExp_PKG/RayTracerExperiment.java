package graphProbExp_PKG;

import java.util.TreeMap;

import base_RayTracer.myRTFileReader;
import base_RayTracer.myScene;
import base_UI_Objects.*;
import base_Utils_Objects.*;

public class RayTracerExperiment extends BaseProbExpMgr {
	//holds references to all loaded scenes
	public TreeMap<String, myScene> loadedScenes;

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
		rdr = new myRTFileReader(this.win.pa,"txtrs");	
		loadedScenes = new TreeMap<String, myScene>();
	}//initExp	

	//set values for RT scene experiment values
	public void setRTSceneExpVals() {
		
		
	}
	
	public void startRayTrace() {
		
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
		// TODO Auto-generated method stub
		
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
