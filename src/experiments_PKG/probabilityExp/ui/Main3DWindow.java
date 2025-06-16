package experiments_PKG.probabilityExp.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Params;
import base_Utils_Objects.io.messaging.MsgCodes;
import base_Utils_Objects.tools.flags.Base_BoolFlags;
import experiments_PKG.probabilityExp.experiment.myProbExpMgr;

public class Main3DWindow extends Base_DispWindow {
	
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
	public final int numGUIObjs = 3;											//# of gui objects for ui	
	
	/////////
	//custom debug/function ui button names -empty will do nothing
	
	//private child-class flags - window specific
	public static final int 
			resetSimIDX			= 1,						//whether or not to reset sim	
			drawVisIDX 			= 2,						//draw visualization - if false SIM exec and sim should ignore all processing/papplet stuff
			conductExpIDX		= 3;						//conduct experiment with current settings

	public static final int numPrivFlags = 4;
	
	/////////
	//custom debug/function ui button names -empty will do nothing
	public String[][] menuBtnNames = new String[][] {	//each must have literals for every button defined in side bar menu, or ignored
		{"Test Rand Gen", "Test R Calc","Func 3"},	//row 1
		{"Func 1", "Func 2", "Func 3", "Func 4"},	//row 1
		{"Dbg 1","Dbg 2","Dbg 3","Dbg 4","Dbg 5"}	
	};
	
	private myProbExpMgr tester;

	public Main3DWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
		super(_p, _AppMgr, _winIdx);
		super.initThisWin(false);
	}//DancingBallWin
	/**
	 * Retrieve the total number of defined privFlags booleans (application-specific state bools and interactive buttons)
	 */
	@Override
	public int getTotalNumOfPrivBools() {return numPrivFlags;	}
	
	/**
	 * Initialize any UI control flags appropriate for all boids window application
	 */
	@Override
	protected final void initDispFlags() {
		//this window is runnable
		dispFlags.setIsRunnable(true);
		//this window uses a customizable camera
		dispFlags.setUseCustCam(true);
		// capable of using right side menu
		dispFlags.setHasRtSideMenu(true);
	}
		
	@Override
	protected void initMe() {//all ui objects set by here
	
		//called once
		//initPrivFlags(numPrivFlags);
		tester = new myProbExpMgr(this, curVisScrDims);

		custMenuOffset = uiMgr.getUIClkCoords()[3];	//495	
	}//initMe	

	@Override
	protected int[] getFlagIDXsToInitToTrue() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * This function would provide an instance of the override class for base_UpdateFromUIData, which would
	 * be used to communicate changes in UI settings directly to the value consumers.
	 */
	@Override
	protected UIDataUpdater buildUIDataUpdateObject() {
		return new GrapProbUIDataUpdater(this);
	}
	/**
	 * This function is called on ui value update, to pass new ui values on to window-owned consumers
	 */
	@Override
	protected final void updateCalcObjUIVals() {}
	/**
	 * UI code-level Debug mode functionality. Called only from flags structure
	 * @param val
	 */
	@Override
	protected final void handleDispFlagsDebugMode_Indiv(boolean val) {}
	
	/**
	 * Application-specific Debug mode functionality (application-specific). Called only from privflags structure
	 * @param val
	 */
	@Override
	protected final void handlePrivFlagsDebugMode_Indiv(boolean val) {	}
	
	/**
	 * Handle application-specific flag setting
	 */
	@Override
	public void handlePrivFlags_Indiv(int idx, boolean val, boolean oldVal){
		switch(idx){
			case Base_BoolFlags.debugIDX 			: {
				//simExec.setExecFlags(mySimExecutive.debugExecIDX,val);
				break;}
			case resetSimIDX			: {
				//if(val) {simExec.initSimExec(true); addPrivSwitchToClear(resetSimIDX);}
				break;}
			case drawVisIDX				:{
				//simExec.setExecFlags(mySimExecutive.drawVisIDX, val);
				break;}
			case conductExpIDX			: {
				//if wanting to conduct exp need to stop current experimet, reset environment, and then launch experiment
//				if(val) {
//					simExec.initializeTrials((int) uiVals[gIDX_ExpLength], (int) uiVals[gIDX_NumExpTrials], true);
//					ri.setFlags(ri.runSim, true);
//					addPrivSwitchToClear(conductExpIDX);
//				} 
				break;}
		
			default:					{}
		}		
	}//setPrivFlags	
		
	/**
	 * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param tmpUIObjMap : map of GUIObj_Params, keyed by unique string, with values describing the UI object
	 * 			- The object IDX                   
	 *          - A double array of min/max/mod values                                                   
	 *          - The starting value                                                                      
	 *          - The label for object                                                                       
	 *          - The object type (GUIObj_Type enum)
	 *          - A boolean array of behavior configuration values : (unspecified values default to false)
	 *           	idx 0: value is sent to owning window,  
	 *           	idx 1: value is sent on any modifications (while being modified, not just on release), 
	 *           	idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
	 *          - A boolean array of renderer format values :(unspecified values default to false)
	 *           	idx 0: whether multi-line(stacked) or not                                                  
	 *              idx 1: if true, build prefix ornament                                                      
	 *              idx 2: if true and prefix ornament is built, make it the same color as the text fill color.
	 */
	@Override
	protected final void setupGUIObjsAras(TreeMap<String, GUIObj_Params> tmpUIObjMap){	
		//msgObj.dispInfoMessage(className,"setupGUIObjsAras","start");
		tmpUIObjMap.put("gIDX_FrameTimeScale ", uiMgr.uiObjInitAra_Float(gIDX_FrameTimeScale, new double[]{1.0f,10000.0f,1.0f},	frameTimeScale, "Sim Speed Multiplier"));  	//time scaling - 1 is real time, 1000 is 1000x speedup           		gIDX_FrameTimeScale 
		tmpUIObjMap.put("gIDX_ExpLength", uiMgr.uiObjInitAra_Int(gIDX_ExpLength, new double[]{1.0f, 1440, 1.0f}, 				720.0, "Experiment Duration")); 				//experiment length
		tmpUIObjMap.put("gIDX_NumExpTrials	", uiMgr.uiObjInitAra_Int(gIDX_NumExpTrials, new double[]{1.0f, 100, 1.0f}, 			1.0, "# Experimental Trials")); 	  			//# of experimental trials
	}//setupGUIObjsAras
	/**
	 * Build UI button objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param firstIdx : the first index to use in the map/as the objIdx
	 * @param tmpUIBoolSwitchObjMap : map of GUIObj_Params to be built containing all flag-backed boolean switch definitions, keyed by sequential value == objId
	 * 				the first element is true label
	 * 				the second element is false label
	 * 				the third element is integer flag idx 
	 */
	@Override
	protected final void setupGUIBoolSwitchAras(int firstIdx, TreeMap<String, GUIObj_Params> tmpUIBoolSwitchObjMap) {
		int idx=firstIdx;
		tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.buildDebugButton(idx++,"Visualization Debug", "Enable Debug"));
		tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "button_"+idx, "Resetting Simulation", "Reset Simulation", resetSimIDX));  
		tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "button_"+idx, "Drawing Vis", "Render Visualization", drawVisIDX));  
		tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "button_"+idx, "Experimenting", "Conduct Experiment", conductExpIDX)); 	
	}//setupGUIObjsAras

	/**
	 * Called if int-handling guiObjs_Numeric[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_IntValsCustom(int UIidx, int ival, int oldVal) {
		switch(UIidx) {
		
			default : {
				msgObj.dispWarningMessage(className, "setUI_IntValsCustom", "No int-defined gui object mapped to idx :"+UIidx);
				break;}
		}	
	}
	
	/**
	 * Called if float-handling guiObjs_Numeric[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal float value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_FloatValsCustom(int UIidx, float val, float oldVal) {
		switch(UIidx) {
			case gIDX_FrameTimeScale 			:{
				this.frameTimeScale = val;
				//simExec.setTimeScale(val);
				break;}
			default : {
				msgObj.dispWarningMessage(className, "setUI_FloatValsCustom", "No int-defined gui object mapped to idx :"+UIidx);
				break;}
		}	
	}
	
	@Override
	public void initDrwnTraj_Indiv(){}

	@Override
	protected void setCamera_Indiv(float[] camVals) {
		// No custom camera handling
		setCameraBase(camVals);
	}//setCameraIndiv
	
	@Override
	//modAmtMillis is time passed per frame in milliseconds - returns if done or not
	protected boolean simMe(float modAmtMillis) {//run simulation
		//msgObj.dispInfoMessage(className,"simMe","took : " + (ri.millis() - stVal) + " millis to simulate");
		//call sim executive, return boolean of whether finished or not
		boolean done = true;//simExec.simMe(modAmtMillis);
		if(done) {uiMgr.setPrivFlag(conductExpIDX, false);}
		return done;	
	}//simMe
	
	@Override
	protected void drawOnScreenStuffPriv(float modAmtMillis) {}

	@Override
	//draw 2d constructs over 3d area on screen - draws behind left menu section
	//modAmtMillis is in milliseconds
	protected void drawRightSideInfoBarPriv(float modAmtMillis) {
		ri.pushMatState();
		//display current simulation variables - call sim world through sim exec
		//simExec.des.drawResultBar(pa, UIrectBox, yOff);
		ri.popMatState();					
	}//drawOnScreenStuff
	
	@Override
	//animTimeMod is in seconds.
	protected void drawMe(float animTimeMod) {
//		curMseLookVec = ri.c.getMse2DtoMse3DinWorld(ri.sceneOriginVals[ri.sceneIDX]);			//need to be here
//		curMseLoc3D = ri.c.getMseLoc(ri.sceneOriginVals[ri.sceneIDX]);
		
		//draw simulation - simExec should have drawMe(animTimeMod) method
		//simExec.drawMe(animTimeMod);
	}//drawMe	

	/**
	 * draw custom 2d constructs below interactive component of menu
	 */
	@Override
	public void drawCustMenuObjs(float animTimeMod){}//drawCustMenuObjs

	//manage any functionality specific to this window that needs to be recalced when the visibile dims of the window change
	@Override
	protected void setVisScreenDimsPriv() {		
		tester.setVisibleScreenDims(curVisScrDims);
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
	protected void setCustMenuBtnLabels() {
		AppMgr.setAllMenuBtnNames(menuBtnNames);	
	}
	
	@Override
	protected void showMe() {
		//things to do when swapping into this window - reinstance released objects, for example.
		setCustMenuBtnLabels();
		
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
		
	/**
	 * type is row of buttons (1st idx in curCustBtn array) 2nd idx is btn
	 * @param funcRow idx for button row
	 * @param btn idx for button within row (column)
	 * @param label label for this button (for display purposes)
	 */
	@Override
	protected final void launchMenuBtnHndlr(int funcRow, int btn, String label){
		switch(funcRow) {
		case 0 : {
			msgObj.dispInfoMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 1 | Btn : " + btn);
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
		case 1 : {
			msgObj.dispInfoMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 2 | Btn : " + btn);
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
		default : {
			msgObj.dispWarningMessage(className,"launchMenuBtnHndlr","Clicked Unknown Btn row : " + funcRow +" | Btn : " + btn);
			break;
		}
		}		
	}//launchMenuBtnHndlr
	@Override
	public void handleSideMenuMseOvrDispSel(int btn, boolean val) {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected final void handleSideMenuDebugSelEnable(int btn) {
		switch (btn) {
			case 0: {				break;			}
			case 1: {				break;			}
			case 2: {				break;			}
			case 3: {				break;			}
			case 4: {				break;			}
			case 5: {				break;			}
			default: {
				msgObj.dispMessage(className, "handleSideMenuDebugSelEnable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
				break;
			}
		}
	}
	
	@Override
	protected final void handleSideMenuDebugSelDisable(int btn) {
		switch (btn) {
			case 0: {				break;			}
			case 1: {				break;			}
			case 2: {				break;			}
			case 3: {				break;			}
			case 4: {				break;			}
			case 5: {				break;			}
		default: {
			msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
			break;
			}
		}
	}

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
	protected boolean hndlMouseMove_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}
	
	//alt key pressed handles trajectory
	
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClick_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {	
		
		return false;}//hndlMouseClickIndiv
	@Override
	protected boolean hndlMouseDrag_Indiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		return res;}	
	@Override
	protected void hndlMouseRel_Indiv() {	}
	@Override
	protected void endShiftKey_Indiv() {}
	@Override
	protected void endAltKey_Indiv() {}
	@Override
	protected void endCntlKey_Indiv() {}
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}	
	@Override
	protected void addSScrToWin_Indiv(int newWinKey){}
	@Override
	protected void addTrajToScr_Indiv(int subScrKey, String newTrajKey){}
	@Override
	protected void delSScrToWin_Indiv(int idx) {}	
	@Override
	protected void delTrajToScr_Indiv(int subScrKey, String newTrajKey) {}
	//resize drawn all trajectories
	@Override
	protected void resizeMe(float scale) { }

	@Override
	protected myPoint getMsePtAs3DPt(myPoint mseLoc) {return new myPoint(mseLoc);}

	@Override
	public void processTraj_Indiv(DrawnSimpleTraj drawnTraj) {
		// TODO Auto-generated method stub
		
	}

}//DESSimWindow

