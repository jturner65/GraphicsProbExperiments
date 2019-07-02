package graphProbExp_PKG;

import java.io.*;
import java.util.*;
import base_UI_Objects.*;
import base_UI_Objects.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.windowUI.myDispWindow;
import base_UI_Objects.windowUI.myGUIObj;
import base_Utils_Objects.*;
import base_Utils_Objects.vectorObjs.myPoint;
import base_Utils_Objects.vectorObjs.myVector;

public class Main3DWindow extends myDispWindow {
	
	///////////
	//ui vals
	//value to multiply delta t by per frame to speed up simulation
	private float frameTimeScale = 1.0f;
	//idxs - need one per object
	//add ui objects for controlling the task stdevmult and other criteria?
	//for tasks : opt size, optsize mean TTC, stdDev Mult
	//for lanes : lane speed
	public final static int
		gIDX_FrameTimeScale 		= 0,
		gIDX_ExpLength				= 1,			//length of time for experiment, in minutes
		gIDX_NumExpTrials			= 2; 
	//initial values - need one per object
	public float[] uiVals = new float[]{
			frameTimeScale,
			720,																				//720 minutes == 12 hours default value
			1
			//
	};			//values of 8 ui-controlled quantities
	public final int numGUIObjs = uiVals.length;											//# of gui objects for ui	
	
	/////////
	//custom debug/function ui button names -empty will do nothing
	
	//private child-class flags - window specific
	public static final int 
			debugAnimIDX 		= 0,						//debug
			resetSimIDX			= 1,						//whether or not to reset sim	
			drawVisIDX 			= 2,						//draw visualization - if false SIM exec and sim should ignore all processing/papplet stuff
			conductExpIDX		= 3;						//conduct experiment with current settings

	public static final int numPrivFlags = 4;
	
	/////////
	//custom debug/function ui button names -empty will do nothing
	public String[][] menuBtnNames = new String[][] {	//each must have literals for every button defined in side bar menu, or ignored
		{},
		{"Test Rand Gen", "Test R Calc","Func 3"},	//row 1
		{"Func 1", "Func 2", "Func 3", "Func 4"},	//row 1
		{"Dbg 1","Dbg 2","Dbg 3","Dbg 4","Dbg 5"}	
	};
	
	private myProbExpMgr tester;

	public Main3DWindow(my_procApplet _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed, String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		super.initThisWin(_canDrawTraj, true, false);
	}//DancingBallWin
	
	@Override
	//initialize all private-flag based UI buttons here - called by base class
	public void initAllPrivBtns(){
		truePrivFlagNames = new String[]{								//needs to be in order of privModFlgIdxs
				"Visualization Debug","Resetting Simulation", "Drawing Vis","Experimenting"
		};
		falsePrivFlagNames = new String[]{			//needs to be in order of flags
				"Enable Debug","Reset Simulation", "Render Visualization",  "Conduct Experiment"
		};
		
		privModFlgIdxs = new int[]{
				debugAnimIDX, resetSimIDX, drawVisIDX, conductExpIDX
		};
		numClickBools = privModFlgIdxs.length;	
		initPrivBtnRects(0,numClickBools);
	}//initAllPrivBtns
	//set labels of boolean buttons 
//	private void setLabel(int idx, String tLbl, String fLbl) {truePrivFlagNames[idx] = tLbl;falsePrivFlagNames[idx] = fLbl;}//	
	
	@Override
	protected void initMe() {//all ui objects set by here
		//this window is runnable
		setFlags(isRunnable, true);
		//this window uses a customizable camera
		setFlags(useCustCam, true);
		//this window uses right side info window
		setFlags(drawRightSideMenu, true);
		
		//called once
		initPrivFlags(numPrivFlags);
		//moved from mapMgr ctor, to remove dependence on papplet in that object
		pa.setAllMenuBtnNames(menuBtnNames);	
		tester = new myProbExpMgr(this);

		custMenuOffset = uiClkCoords[3];	//495	
	}//initMe	
		
	@Override
	//set flag values and execute special functionality for this sequencer
	//skipKnown will allow settings to be reset if passed redundantly
	public void setPrivFlags(int idx, boolean val){	
		boolean curVal = getPrivFlags(idx);
		if(val == curVal){return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		privFlags[flIDX] = (val ?  privFlags[flIDX] | mask : privFlags[flIDX] & ~mask);
		switch(idx){
			case debugAnimIDX 			: {
				//simExec.setExecFlags(mySimExecutive.debugExecIDX,val);
				break;}
			case resetSimIDX			: {
				//if(val) {simExec.initSimExec(true); addPrivBtnToClear(resetSimIDX);}
				break;}
			case drawVisIDX				:{
				//simExec.setExecFlags(mySimExecutive.drawVisIDX, val);
				break;}
			case conductExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
//				if(val) {
//					simExec.initializeTrials((int) uiVals[gIDX_ExpLength], (int) uiVals[gIDX_NumExpTrials], true);
//					pa.setFlags(pa.runSim, true);
//					addPrivBtnToClear(conductExpIDX);
//				} 
				break;}
		
			default:					{}
		}		
	}//setPrivFlags	
		
	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(){	
		//for list select objs
		TreeMap<Integer, String[]> tmpListObjVals = new TreeMap<Integer, String[]>();
		//pa.outStr2Scr("setupGUIObjsAras start");
		guiMinMaxModVals = new double [][]{
			{1.0f,10000.0f,1.0f},						//time scaling - 1 is real time, 1000 is 1000x speedup           		gIDX_FrameTimeScale 
			{1.0f, 1440, 1.0f},								//experiment length
			{1.0f, 100, 1.0f}								//# of experimental trials
		};		//min max mod values for each modifiable UI comp	

		guiStVals = new double[]{
			uiVals[gIDX_FrameTimeScale],
			uiVals[gIDX_ExpLength],
			uiVals[gIDX_NumExpTrials],
			
		};								//starting value
		
		guiObjNames = new String[]{
				"Sim Speed Multiplier",
				"Experiment Duration",
				"# Experimental Trials"
		};								//name/label of component	
		
		//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
		guiBoolVals = new boolean [][]{
			{false, false, true},	
			{true, false, true},
			{true, false, true}			
		};						//per-object  list of boolean flags
		
		//since horizontal row of UI comps, uiClkCoords[2] will be set in buildGUIObjs		
		guiObjs = new myGUIObj[numGUIObjs];			//list of modifiable gui objects
		if(numGUIObjs > 0){
			buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff},tmpListObjVals);			//builds a horizontal list of UI comps
		}
		
//		setupGUI_XtraObjs();
	}//setupGUIObjsAras
	
//	//setup UI object for song slider
//	private void setupGUI_XtraObjs() {
//		double stClkY = uiClkCoords[3], sizeClkY = 3*yOff;
//		guiObjs[songTransIDX] = new myGUIBar(pa, this, songTransIDX, "MP3 Transport for ", 
//				new myVector(0, stClkY,0), new myVector(uiClkCoords[2], stClkY+sizeClkY,0),
//				new double[] {0.0, 1.0,0.1}, 0.0, new boolean[]{false, false, true}, new double[]{xOff,yOff});	
//		
//		//setup space for ui interaction with song bar
//		stClkY += sizeClkY;				
//		uiClkCoords[3] = stClkY;
//	}

	
	@Override
	protected void setUIWinVals(int UIidx) {
		float val = (float)guiObjs[UIidx].getVal();
		if(val != uiVals[UIidx]){//if value has changed...
			uiVals[UIidx] = val;
			switch(UIidx){		
			case gIDX_FrameTimeScale 			:{
				this.frameTimeScale = val;
				//simExec.setTimeScale(val);
				break;}
			case gIDX_ExpLength : {//determines experiment length				
				break;}
			case gIDX_NumExpTrials : {//# of trials for experiments
				
			}

			default : {break;}
			}
		}
	}
	@Override
	public void initDrwnTrajIndiv(){}
	
//	public void setLights(){
//		pa.ambientLight(102, 102, 102);
//		pa.lightSpecular(204, 204, 204);
//		pa.directionalLight(180, 180, 180, 0, 1, -1);	
//	}	
	//overrides function in base class mseClkDisp
	@Override
	public void drawTraj3D(float animTimeMod,myPoint trans){}//drawTraj3D	
	//set camera to either be global or from pov of one of the boids
	@Override
	protected void setCameraIndiv(float[] camVals){		
		//, float rx, float ry, float dz are now member variables of every window
		pa.camera(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
		// puts origin of all drawn objects at screen center and moves forward/away by dz
		pa.translate(camVals[0],camVals[1],(float)dz); 
	    setCamOrient();	
	}//setCameraIndiv

	
	@Override
	//modAmtMillis is time passed per frame in milliseconds - returns if done or not
	protected boolean simMe(float modAmtMillis) {//run simulation
		//pa.outStr2Scr("took : " + (pa.millis() - stVal) + " millis to simulate");
		//call sim executive, return boolean of whether finished or not
		boolean done = true;//simExec.simMe(modAmtMillis);
		if(done) {setPrivFlags(conductExpIDX, false);}
		return done;	
	}//simMe
	
	
	@Override
	protected void drawOnScreenStuffPriv(float modAmtMillis) {
		
	}


	@Override
	//draw 2d constructs over 3d area on screen - draws behind left menu section
	//modAmtMillis is in milliseconds
	protected void drawRightSideInfoBarPriv(float modAmtMillis) {
		pa.pushMatrix();pa.pushStyle();
		//display current simulation variables - call sim world through sim exec
		//simExec.des.drawResultBar(pa, UIrectBox,  yOff);
		pa.popStyle();pa.popMatrix();					
	}//drawOnScreenStuff
	
	@Override
	//animTimeMod is in seconds.
	protected void drawMe(float animTimeMod) {
//		curMseLookVec = pa.c.getMse2DtoMse3DinWorld(pa.sceneCtrVals[pa.sceneIDX]);			//need to be here
//		curMseLoc3D = pa.c.getMseLoc(pa.sceneCtrVals[pa.sceneIDX]);
		
		//draw simulation - simExec should have drawMe(animTimeMod) method
		//simExec.drawMe(animTimeMod);
	}//drawMe	

	//draw custom 2d constructs below interactive component of menu
	@Override
	public void drawCustMenuObjs(){
		pa.pushMatrix();				pa.pushStyle();		
		//all sub menu drawing within push mat call
		pa.translate(0,custMenuOffset+yOff);		
		//draw any custom menu stuff here
		pa.popStyle();					pa.popMatrix();		
	}//drawCustMenuObjs

	//manage any functionality specific to this window that needs to be recalced when the visibile dims of the window change
	@Override
	protected void setVisScreenDimsPriv() {
		
		
	}//setVisScreenDimsPriv


	@Override
	protected void closeMe() {
		//things to do when swapping this window out for another window - release objects that take up a lot of memory, for example.
	}	
	
	
	@Override
	//stopping simulation
	protected void stopMe() {
		System.out.println("Simulation Finished");	
	}
	
	//modify menu buttons to display whether using CSV or SQL to access raw data
	@Override
	protected void setCustMenuBtnNames() {
		//menuBtnNames[mySideBarMenu.btnAuxFunc1Idx][loadRawBtnIDX]=menuLdRawFuncBtnNames[(rawDataSource % menuLdRawFuncBtnNames.length) ];
		pa.setAllMenuBtnNames(menuBtnNames);	
	}
	
	@Override
	protected void showMe() {
		//things to do when swapping into this window - reinstance released objects, for example.
		setCustMenuBtnNames();
		
	}
	//return strings for directory names and for individual file names that describe the data being saved.  used for screenshots, and potentially other file saving
	//first index is directory suffix - should have identifying tags based on major/archtypical component of sim run
	//2nd index is file name, should have parameters encoded
	@Override
	protected String[] getSaveFileDirNamesPriv() {
		String dirString="", fileString ="";
		//for(int i=0;i<uiAbbrevList.length;++i) {fileString += uiAbbrevList[i]+"_"+ (uiVals[i] > 1 ? ((int)uiVals[i]) : uiVals[i] < .0001 ? String.format("%6.3e", uiVals[i]) : String.format("%3.3f", uiVals[i]))+"_";}
		return new String[]{dirString,fileString};	
	}
		
	//if launching threads for custom functions or debug, need to remove resetButtonState call in function below and call resetButtonState (with slow proc==true) when thread ends
	@Override
	protected void launchMenuBtnHndlr() {
		int btn = curCustBtn[curCustBtnType];
		switch(curCustBtnType) {
		case mySideBarMenu.btnAuxFunc1Idx : {
			pa.outStr2Scr("Clicked Btn row : Aux Func 1 | Btn : " + btn);
			switch(btn){
				case 0 : {	
					tester.testRandGen(10000000);
					resetButtonState();
					break;}
				case 1 : {	
					tester.testRCalc(); 
					resetButtonState();
					break;}
				case 2 : {	
					resetButtonState();
					break;}
				default : {
					break;}
			}	
			break;}//row 1 of menu side bar buttons
		case mySideBarMenu.btnAuxFunc2Idx : {
			pa.outStr2Scr("Clicked Btn row : Aux Func 2 | Btn : " + btn);
			switch(btn){
				case 0 : {	
					resetButtonState();
					break;}
				case 1 : {	
					resetButtonState();
					break;}
				case 2 : {	
					resetButtonState();
					break;}
				case 3 : {	
					break;}
				default : {
					break;}	
			}
			break;}//row 2 of menu side bar buttons
		case mySideBarMenu.btnDBGSelCmpIdx : {
			pa.outStr2Scr("Clicked Btn row : Debug | Btn : " + btn);
			switch(btn){
				case 0 : {	
					resetButtonState();
					break;}//verify priority queue functionality
				case 1 : {	

					resetButtonState();
					break;}//verify FEL pq integrity
				case 2 : {	
					resetButtonState();
					break;}
				case 3 : {	
					resetButtonState();
					break;}
				case 4 : {						
					resetButtonState();
					break;}
				default : {
					break;}
			}				
			break;}//row 3 of menu side bar buttons (debug)			
		}		
	}//launchMenuBtnHndlr
	
	
	@Override
	public void hndlFileLoad(File file, String[] vals, int[] stIdx) {
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileSave 
		//hndlFileLoad_GUI(vals, stIdx);
		
	}
	@Override
	public ArrayList<String> hndlFileSave(File file) {
		ArrayList<String> res = new ArrayList<String>();
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileLoad 
		//res = hndlFileSave_GUI();

		return res;
	}

	@Override
	protected void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){	}
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}
	
	//alt key pressed handles trajectory
	
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {	
		boolean res = checkUIButtons(mouseX, mouseY);	
		return res;}//hndlMouseClickIndiv
	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		return res;}	
	@Override
	protected void hndlMouseRelIndiv() {	}
	@Override
	protected void endShiftKeyI() {}
	@Override
	protected void endAltKeyI() {}
	@Override
	protected void endCntlKeyI() {}
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}	
	@Override
	protected void addSScrToWinIndiv(int newWinKey){}
	@Override
	protected void addTrajToScrIndiv(int subScrKey, String newTrajKey){}
	@Override
	protected void delSScrToWinIndiv(int idx) {}	
	@Override
	protected void delTrajToScrIndiv(int subScrKey, String newTrajKey) {}
	//resize drawn all trajectories
	@Override
	protected void resizeMe(float scale) { }

	@Override
	protected myPoint getMsePtAs3DPt(myPoint mseLoc) {return new myPoint(mseLoc);}
}//DESSimWindow

