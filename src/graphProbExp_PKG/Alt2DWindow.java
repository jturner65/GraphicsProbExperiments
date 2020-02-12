package graphProbExp_PKG;

import java.io.File;
import java.util.*;
import base_UI_Objects.*;
import base_UI_Objects.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.windowUI.base.base_UpdateFromUIData;
import base_UI_Objects.windowUI.base.myDispWindow;
import base_UI_Objects.windowUI.myGUIObj;
import base_Utils_Objects.*;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class Alt2DWindow extends myDispWindow {
	
	/////////////
	// ui objects 
	////////////

	//idxs - need one per object
	public final static int
		gIDX_TempIDX		= 0;

	/////////
	//ui button names -empty will do nothing, otherwise add custom labels for debug and custom functionality names

	//////////////
	// local/ui interactable boolean buttons
	//////////////
	//private child-class flags - window specific
	//for every class-wide boolean make an index, and increment numPrivFlags.
	//use getPrivFlags(idx) and setPrivFlags(idx,val) to consume
	//put idx-specific code in case statement in setPrivFlags
	public static final int 
			debugAnimIDX 		= 0;					//show debug
	public static final int numPrivFlags = 1;

	/////////
	//custom debug/function ui button names -empty will do nothing
	public String[][] menuBtnNames = new String[][] {	//each must have literals for every button defined in side bar menu, or ignored
		{"Test Rand Gen", "Test R Calc","Func 3"},	//row 1
		{"Func 1", "Func 2", "Func 3", "Func 4"},	//row 1
		{"Dbg 1","Dbg 2","Dbg 3","Dbg 4","Dbg 5"}	
	};

	private myProbExpMgr tester;
	
	public Alt2DWindow(my_procApplet _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt);
		super.initThisWin(false);
	}
			
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
		tester = new myProbExpMgr(this);
		
		//set offset to use for custom menu objects
		custMenuOffset = uiClkCoords[3];	
		//moved from mapMgr ctor, to remove dependence on papplet in that object
		pa.setAllMenuBtnNames(menuBtnNames);	
	}//
	//initialize all UI buttons here
	@Override
	public int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray) {
		//give true labels, false labels and specify the indexes of the booleans that should be tied to UI buttons
		tmpBtnNamesArray.add(new Object[] { "Debugging", "Debug", debugAnimIDX });
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
			case debugAnimIDX 			: {
				break;}
		}
	}
	
	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){	

	}//setupGUIObjsAras
	
	//all ui objects should have an entry here to show how they should interact
	@Override
	protected void setUIWinVals(int UIidx) {
		float val = (float)guiObjs[UIidx].getVal();
		int ival = (int)val;
		switch(UIidx){		
		case gIDX_TempIDX 			:{break;}
		}
		
	}//setUIWinVals
	
	//check whether the mouse is over a legitimate map location
	public boolean chkMouseClick2D(int mouseX, int mouseY, int btn){		
		return false;
	}//chkMouseOvr

	
	//check whether the mouse is over a legitimate map location
	public boolean chkMouseMoveDragState2D(int mouseX, int mouseY, int btn){		
		return false;
	}//chkMouseOvr
	
	//check whether the mouse is over a legitimate map location
	public void setMouseReleaseState2D(){	
	}//chkMouseOvr

	
	@Override
	protected void drawMe(float animTimeMod) {
		pa.pushMatrix();pa.pushStyle();
		pa.translate(this.rectDim[0],0,0);

		pa.popStyle();pa.popMatrix();
	}

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
	public void drawCustMenuObjs() {
		pa.pushMatrix();				pa.pushStyle();		
		//all sub menu drawing within push mat call
		pa.translate(5,custMenuOffset+yOff);
		//draw any custom menu stuff here
		
		
		pa.popStyle();					pa.popMatrix();		
	}//drawCustMenuObjs
	
	//manage any functionality specific to this window that needs to be recalced when the visibile dims of the window change
	@Override
	protected void setVisScreenDimsPriv() {

		
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
	protected void launchMenuBtnHndlr(int funcRow, int btn) {
		switch(funcRow) {
		case 0 : {
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
		case 1 : {
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
		pa.camera(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
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
		
	}
	@Override
	public ArrayList<String> hndlFileSave(File file) {
		ArrayList<String> res = new ArrayList<String>();
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileLoad 
		//res = hndlFileSave_GUI();

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
		boolean res =chkMouseClick2D(mouseX, mouseY, mseBtn);
		
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
	protected myPoint getMsePtAs3DPt(myPoint mseLoc) {
		// TODO Auto-generated method stub
		return null;
	}


}
