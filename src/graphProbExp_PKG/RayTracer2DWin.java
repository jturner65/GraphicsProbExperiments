package graphProbExp_PKG;

import java.io.*;
import java.util.*;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_UI_Objects.*;
import base_UI_Objects.windowUI.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.windowUI.base.base_UpdateFromUIData;
import base_UI_Objects.windowUI.base.myDispWindow;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * class to hold 2-D ray tracer experiment - bunch of circles, shoot rays and plot their traversal
 * @author john
 *
 */

public class RayTracer2DWin extends myDispWindow {
	
	/////////////
	// ui objects 
	////////////
	//scene dimensions
	private int sceneCols = RayTracerExperiment.sceneCols;
	private int sceneRows = RayTracerExperiment.sceneRows;
	//cli file describing current scene
	private int curSceneCliFileIDX = 0;

	//idxs - need one per object
	public final static int
		gIDX_SceneCols		= 0,
		gIDX_SceneRows		= 1,
		gIDX_CurrSceneCLI	= 2;
	public final int numGUIObjs = 3;	
	/////////
	
	public String[] gIDX_CurrSceneCLIList = new String[] {
			"trTrans.cli","t01.cli","t02.cli","t03.cli","t04.cli","t05.cli","t06.cli","t07.cli","t08.cli","t09.cli","t10.cli","t11.cli",
			"p4_st01.cli","p4_st02.cli","p4_st03.cli","p4_st04.cli","p4_st05.cli","p4_st06.cli","p4_st07.cli","p4_st08.cli","p4_st09.cli",
			"p4_t05.cli","p4_t06.cli","p4_t07.cli","p4_t08.cli","p4_t09.cli","plnts3ColsBunnies.cli","p3_t02_sierp.cli",
			"p2_t01.cli", "p2_t02.cli", "p2_t03.cli", "p2_t04.cli", "p2_t05.cli","p2_t06.cli", "p2_t07.cli", "p2_t08.cli", "p2_t09.cli", 			
			"old_t07c.cli","earthAA1.cli","earthAA2.cli","earthAA3.cli","c2clear.cli","c3shinyBall.cli","c4InSphere.cli","c6.cli", "c6Fish.cli","c2torus.cli",
			"old_t02.cli","old_t03.cli","old_t04.cli","old_t05.cli","old_t06.cli","old_t07.cli","old_t08.cli","old_t09.cli","old_t10.cli",
			"planets.cli","planets2.cli","planets3.cli","planets3columns.cli","planets3Ortho.cli",
			"c0.cli", "c1.cli","c2.cli","c3.cli","c4.cli","c5.cli",
			"p3_t01.cli","p3_t02.cli","p3_t03.cli","p3_t04.cli","p3_t05.cli","p3_t06.cli","p3_t07.cli","p3_t11_sierp.cli", 	
			"p4_t06_2.cli", "p4_t09.cli","cylinder1.cli", "tr0.cli","c0Square.cli",  "c1octo.cli",  			
			"old_t0rotate.cli","old_t03a.cli", "old_t04a.cli", "old_t05a.cli", "old_t06a.cli", 	"old_t07a.cli",		
	};

	//////////////
	// local/ui interactable boolean buttons
	//////////////
	//private child-class flags - window specific
	//for every class-wide boolean make an index, and increment numPrivFlags.
	//use getPrivFlags(idx) and setPrivFlags(idx,val) to consume
	//put idx-specific code in case statement in setPrivFlags
	public static final int 
			shootRays			 		= 0,					//shoot rays
			flipNorms					= 1;
	public static final int numPrivFlags = 2;

	private RayTracerExperiment RTExp;
	
	/////////
	//custom debug/function ui button names -empty will do nothing
	public String[][] menuBtnNames = new String[][] {	//each must have literals for every button defined in side bar menu, or ignored
		{"Func 00", "Func 01", "Func 02"},				//row 1
		{"Func 10", "Func 11", "Func 12", "Func 13"},	//row 2
		{"Func 20", "Func 21", "Func 22", "Func 23","Func 24"}	
	};

		
	public RayTracer2DWin(IRenderInterface _p, GUI_AppManager _AppMgr, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt) {
		super(_p, _AppMgr, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt);
		super.initThisWin(false);
	}//ctor
			
	@Override
	protected void initMe() {
		//called once
		//initPrivFlags(numPrivFlags);
		//this window is runnable
		setFlags(isRunnable, true);
		//this window uses a customizable camera
		setFlags(useCustCam, true);
		//this window uses right side info window
		setFlags(drawRightSideMenu, true);
		
		//build exps before visible screen with set
		RTExp = new RayTracerExperiment(this);
		//set visibility width and send to experiments
		setVisScreenDimsPriv();			
		//set offset to use for custom menu objects
		custMenuOffset = uiClkCoords[3];	
		//set initial values
		RTExp.setRTSceneExpVals(sceneCols, sceneRows, gIDX_CurrSceneCLIList[curSceneCliFileIDX % gIDX_CurrSceneCLIList.length]);
		RTExp.startRayTrace();
		//moved from mapMgr ctor, to remove dependence on papplet in that object
		AppMgr.setAllMenuBtnNames(menuBtnNames);		
	}//
	
	//initialize all UI buttons here
	@Override
	public int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray) {
		//give true labels, false labels and specify the indexes of the booleans that should be tied to UI buttons
		tmpBtnNamesArray.add(new Object[]{"Shooting Rays", "Shoot Rays", shootRays});  
		tmpBtnNamesArray.add(new Object[]{"Norms are Flipped", "Flip Normals", flipNorms});  
		return numPrivFlags;
	
	}
	

	@Override
	protected base_UpdateFromUIData buildUIDataUpdateObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void buildUIUpdateStruct_Indiv(TreeMap<Integer, Integer> intValues, TreeMap<Integer, Float> floatValues,
			TreeMap<Integer, Boolean> boolValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected int[] getFlagIDXsToInitToTrue() {
		// TODO Auto-generated method stub
		return null;
	}
	
	//add reference here to all button IDX's 
	@Override
	public void setPrivFlags(int idx, boolean val) {
		boolean curVal = getPrivFlags(idx);
		if(val == curVal) {return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		privFlags[flIDX] = (val ?  privFlags[flIDX] | mask : privFlags[flIDX] & ~mask);
		switch(idx){
			case shootRays : {//build new grade distribution
				if (val) {
					RTExp.startRayTrace();
					addPrivBtnToClear(idx);
				}
				break;}
			case flipNorms : {
				RTExp.setFlipNorms();
				break;}
		}
	}//setPrivFlags
	
	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){		
	
		tmpListObjVals.put(gIDX_CurrSceneCLI, gIDX_CurrSceneCLIList);


		tmpUIObjArray.put(gIDX_SceneCols, new Object[] {new double[]{200,(int)(rectDim[2]/2),10}, 1.0*sceneCols, "Image Width (pxls)", new boolean[]{true, false, true}});
		tmpUIObjArray.put(gIDX_SceneRows, new Object[] {new double[]{200,(int)(rectDim[3]/2),10}, 1.0*sceneRows, "Image Height (pxls)", new boolean[]{true, false, true}});
		tmpUIObjArray.put(gIDX_CurrSceneCLI, new Object[] {new double[]{0,tmpListObjVals.get(gIDX_CurrSceneCLI).length-1,1}, 0.0, "Scene to Display",	new boolean[]{true, true, true}});
	

//		setupGUI_XtraObjs();
	}//setupGUIObjsAras
	
	//all ui objects should have an entry here to show how they should interact
	@Override
	protected void setUIWinVals(int UIidx) {
		float val = (float)guiObjs[UIidx].getVal();
		int ival = (int)val;
		switch(UIidx){		
			case gIDX_SceneCols		:{
				if(ival != sceneCols){
					sceneCols = ival;		
					RTExp.setRTSceneExpVals(sceneCols, sceneRows, gIDX_CurrSceneCLIList[curSceneCliFileIDX % gIDX_CurrSceneCLIList.length]);
				}	
				break;}			
			case gIDX_SceneRows		:{
				if(ival != sceneRows){
					sceneRows = ival;
					RTExp.setRTSceneExpVals(sceneCols, sceneRows, gIDX_CurrSceneCLIList[curSceneCliFileIDX % gIDX_CurrSceneCLIList.length]);
				}	
				break;}	
			case gIDX_CurrSceneCLI :{
				if(ival != curSceneCliFileIDX) {
					curSceneCliFileIDX = ival;
					RTExp.setRTSceneExpVals(sceneCols, sceneRows, gIDX_CurrSceneCLIList[curSceneCliFileIDX % gIDX_CurrSceneCLIList.length]);
				}
				break;}
		}//switch

	}//setUIWinVals
	
	//check whether the mouse is over a legitimate map location
	public boolean chkMouseClick2D(int mouseX, int mouseY, int btn){		
		return RTExp.checkMouseClickInExp2D( mouseX-(int)this.rectDim[0], mouseY, btn);
	}//chkMouseOvr

	
	//check whether the mouse is over a legitimate map location
	public boolean chkMouseMoveDragState2D(int mouseX, int mouseY, int btn){		
		return RTExp.checkMouseDragMoveInExp2D( mouseX-(int)this.rectDim[0], mouseY, btn);
	}//chkMouseOvr
	
	//check whether the mouse is over a legitimate map location
	public void setMouseReleaseState2D(){	RTExp.setMouseReleaseInExp2D();}//chkMouseOvr

	
	@Override
	protected void drawMe(float animTimeMod) {
		pa.pushMatState();
		pa.translate(this.rectDim[0],0,0);
		//all drawing stuff goes here
		RTExp.drawExp();
		pa.popMatState();
	}

	@Override
	protected void drawOnScreenStuffPriv(float modAmtMillis) {
	}

	@Override
	//draw 2d constructs over 3d area on screen - draws behind left menu section
	//modAmtMillis is in milliseconds
	protected void drawRightSideInfoBarPriv(float modAmtMillis) {
		pa.pushMatState();
		//display current simulation variables - call sim world through sim exec
		//simExec.des.drawResultBar(pa, UIrectBox,  yOff);
		pa.popMatState();					
	}//drawOnScreenStuff
	@Override
	public void drawCustMenuObjs() {
		pa.pushMatState();
		//all sub menu drawing within push mat call
		pa.translate(5,custMenuOffset+yOff);
		//draw any custom menu stuff here
		
		
		pa.popMatState();		
	}//drawCustMenuObjs
	
	//manage any functionality specific to this window that needs to be recalced when the visibile dims of the window change
	@Override
	protected void setVisScreenDimsPriv() {	
		RTExp.setVisibleScreenWidth();
		
	}//setVisScreenDimsPriv

	
	//any simulation stuff - executes before draw on every draw cycle
	@Override
	//modAmtMillis is time passed per frame in milliseconds - returns if done or not
	protected boolean simMe(float modAmtSec) {
		// TODO Auto-generated method stub

		return true;
	}
	
	@Override
	protected void closeMe() {
		//things to do when swapping this window out for another window - release objects that take up a lot of memory, for example.
	}
	
	
	//modify menu buttons to display whether using CSV or SQL to access raw data
	@Override
	protected void setCustMenuBtnNames() {
		//menuBtnNames[mySideBarMenu.btnAuxFunc1Idx][loadRawBtnIDX]=menuLdRawFuncBtnNames[(rawDataSource % menuLdRawFuncBtnNames.length) ];
		AppMgr.setAllMenuBtnNames(menuBtnNames);	
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
	protected void launchMenuBtnHndlr(int funcRow, int btn) {
		switch(funcRow) {
		case 0 : {
			pa.outStr2Scr("Clicked Btn row : Aux Func 1 | Btn : " + btn);
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
				default : {
					break;}
			}	
			break;}//row 1 of menu side bar buttons
		case 1 : {
			pa.outStr2Scr("Clicked Btn row : Aux Func 2 | Btn : " + btn);
			switch(btn){
				case 0 : {	
					//test calculation of inverse fleish function -> derive x such that y = f(x) for fleishman polynomial.  This x is then the value from normal dist that yields y from fleish dist
					//double xDesired = -2.0;
					resetButtonState();
					break;}
				case 1 : {	
					resetButtonState();
					break;}
				case 2 : {	
					resetButtonState();
					break;}
				case 3 : {	
					//test cosine function
					resetButtonState();
					break;}
				default : {
					break;}	
			}
			break;}//row 2 of menu side bar buttons

		}		
	}//launchMenuBtnHndlr		
	
	@Override
	public void handleSideMenuMseOvrDispSel(int btn, boolean val) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleSideMenuDebugSel(int btn, int val) {
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
	}
	@Override
	protected void setCameraIndiv(float[] camVals){		
		//, float rx, float ry, float dz are now member variables of every window
		pa.setCameraWinVals(camVals);//(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
		// puts origin of all drawn objects at screen center and moves forward/away by dz
		pa.translate(camVals[0],camVals[1],(float)dz); 
	    setCamOrient();	
	}//setCameraIndiv

	@Override
	protected void stopMe() {pa.outStr2Scr("Stop");}	
	
	@Override
	public void hndlFileLoad(File file, String[] vals, int[] stIdx) {
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileSave 
		//hndlFileLoad_GUI(vals, stIdx);
		//loading in grade data from grade file - vals holds array of strings, expected to be comma sep values, for a single class, with student names and grades
		for(String s : vals) {			pa.outStr2Scr(s);}	
		String fileName = file.getName();
		TreeMap<String, String> tmpAra = new TreeMap<String, String>(), valsToIDX = new TreeMap<String, String>();
		for(String s : gIDX_CurrSceneCLIList) {tmpAra.put(s, "");	}
		tmpAra.put(fileName, "");
		int idx = 0;
		int fileIDX = 0;
		for(String s : tmpAra.keySet()){
			if (s.equals(fileName)) {fileIDX=idx; break;}
			++idx;
		}		
		gIDX_CurrSceneCLIList = tmpAra.keySet().toArray(new String[0]);		
		curSceneCliFileIDX = fileIDX;
		this.guiObjs[gIDX_CurrSceneCLI].setNewMax(gIDX_CurrSceneCLIList.length-1);
		RTExp.setRTSceneExpVals(sceneCols, sceneRows, gIDX_CurrSceneCLIList[curSceneCliFileIDX % gIDX_CurrSceneCLIList.length]);
	}
	@Override
	public ArrayList<String> hndlFileSave(File file) {
		ArrayList<String> res = new ArrayList<String>();
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileLoad 
		//res = hndlFileSave_GUI();
		//saving student grades to a file for a single class - vals holds array of strings, expected to be comma sep values, for a single class, with student names and grades
		
		return res;
	}

	@Override
	public void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){	}
	
	
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		boolean res = chkMouseMoveDragState2D(mouseX, mouseY, -1);
		return res;
	}
	//alt key pressed handles trajectory
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {	
		boolean res =  chkMouseClick2D(mouseX, mouseY, mseBtn);
		
		return res;}//hndlMouseClickIndiv
	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		if(!res) {
			res = chkMouseMoveDragState2D(mouseX, mouseY, mseBtn);
		}		
		return res;}	
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}	
	@Override
	protected void hndlMouseRelIndiv() {
		setMouseReleaseState2D();
	}
	@Override
	protected void endShiftKeyI() {}
	@Override
	protected void endAltKeyI() {}
	@Override
	protected void endCntlKeyI() {}
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
	protected void resizeMe(float scale) {	}

	@Override
	protected void initDrwnTrajIndiv() {}
	@Override
	protected myPoint getMsePtAs3DPt(myPoint mseLoc){return new myPoint(mseLoc.x,mseLoc.y,0);}


}
