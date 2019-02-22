package graphProbExp_PKG;

import java.util.*;


public class Grade2DWindow extends myDispWindow {
	
	/////////////
	// ui objects 
	////////////
	// # of students across all classes
	private int numStudents = 5;
	// # of classes to build final grade
	private int numClasses = 3;
	//eval func IDX
	private int funcEvalType = 0, funcEvalNumVals = 10000, funcEvalNumBuckets = 10;
	//lower and upper bound for functional evaluation 
	private float funcEvalLow = 0, funcEvalHigh = 1;
	
	//idxs - need one per object
	public final static int
		gIDX_NumStudents		= 0,
		gIDX_NumClasses			= 1,
		gIDX_FuncTypeEval		= 2,
		gIDX_FuncEvalLower		= 3,
		gIDX_FuncEvalHigher		= 4, 
		gIDX_FuncEvalNumVals	= 5,
		gIDX_FuncEvalNumBkts	= 6;
	//initial values - need one per object
	public float[] uiVals = new float[]{
			numStudents,
			numClasses,
			funcEvalType,
			funcEvalLow,
			funcEvalHigh,
			funcEvalNumVals,
			funcEvalNumBuckets
	};			//values of 8 ui-controlled quantities
	public final int numGUIObjs = uiVals.length;	
	/////////
	//ui button names -empty will do nothing, otherwise add custom labels for debug and custom functionality names
	public static final String[] gIDX_FuncTypeEvalList = myRandVarFunc.queryFuncTypes;

	//////////////
	// local/ui interactable boolean buttons
	//////////////
	//private child-class flags - window specific
	//for every class-wide boolean make an index, and increment numPrivFlags.
	//use getPrivFlags(idx) and setPrivFlags(idx,val) to consume
	//put idx-specific code in case statement in setPrivFlags
	public static final int 
			reCalcRandGradeSpread 		= 0,					//recalculate random grades given specified class and student # parameters
			showScaledTransGrades 		= 1,					//display transformed grades from range 0->1 or in natural uniform scaled mapping from underlying distribution
			useZScore					= 2,					//whether or not to use the z score calc to transform the uniform final grades
			rebuildDistOnMove			= 3,					//rebuild class distribution when value is moved
			//drawing functions
			drawFuncEval				= 4,					//draw results of function evaluation
			drawHistEval				= 5;					//draw results of histogram evaluation
	public static final int numPrivFlags = 6;

	/////////
	//custom debug/function ui button names -empty will do nothing
	public String[][] menuBtnNames = new String[][] {	//each must have literals for every button defined in side bar menu, or ignored
		{},
		{"Test Rand Gen", "Test R Calc","Func 3"},				//row 1
		{"Func 1", "Test Fleish", "Tst Fl Range", "Func 4"},	//row 2
		{"Linear","Uniform","Linear w/Z","Fleish","Dbg 5"}	
	};

	private myProbExpMgr tester;
	
	//class experiment
	private ClassGradeExperiment gradeAvgExperiment;
	
	public Grade2DWindow(GraphProbExpMain _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		float stY = rectDim[1]+rectDim[3]-4*yOff,stYFlags = stY + 2*yOff;
		trajFillClrCnst = GraphProbExpMain.gui_DarkCyan;		//override this in the ctor of the instancing window class
		trajStrkClrCnst = GraphProbExpMain.gui_Cyan;
		super.initThisWin(_canDrawTraj, true, false);
	}//ctor
			
	@Override
	protected void initMe() {
		//called once
		initPrivFlags(numPrivFlags);
		//this window is runnable
		setFlags(isRunnable, true);
		//this window uses a customizable camera
		setFlags(useCustCam, true);
		//this window uses right side info window
		setFlags(drawRightSideMenu, true);
		
		//generic tester
		tester = new myProbExpMgr(this);
		
		//grade experiments
		gradeAvgExperiment = new ClassGradeExperiment(this);
		gradeAvgExperiment.buildStudentsClassesRandGrades(numStudents, numClasses);
		//set visibility width and send to experiments
		setVisScreenDimsPriv();			
		//set offset to use for custom menu objects
		custMenuOffset = uiClkCoords[3];	
		//moved from mapMgr ctor, to remove dependence on papplet in that object
		pa.setAllMenuBtnNames(menuBtnNames);	
	}//
	//initialize all UI buttons here
	@Override
	public void initAllPrivBtns() {
		//give true labels, false labels and specify the indexes of the booleans that should be tied to UI buttons
		truePrivFlagNames = new String[]{			//needs to be in order of privModFlgIdxs
				"Recalculating Grades",
				"Showing Scaled Uni Grades [0->1]",
				"ZScore for final grades",
				"Rebuild Class dist on move",
				"Eval/Draw Func on Bounds",
				"Eval/Draw  Distribution"
		};
		falsePrivFlagNames = new String[]{			//needs to be in order of flags
				"Recalculate Grades",
				"Showing Unscaled Uni Grades",
				"Specific Dist for final grades",
				"Don't rebuild class dist on move",
				"Eval/Draw Func on Bounds",
				"Eval/Draw Distribution"				
		};
		privModFlgIdxs = new int[]{					//idxs of buttons that are able to be interacted with
				reCalcRandGradeSpread,
				showScaledTransGrades,
				useZScore,
				rebuildDistOnMove,
				drawFuncEval,
				drawHistEval
		};
		numClickBools = privModFlgIdxs.length;	
		initPrivBtnRects(0,numClickBools);
	}
	
	//add reference here to all button IDX's 
	@Override
	public void setPrivFlags(int idx, boolean val) {
		boolean curVal = getPrivFlags(idx);
		if(val == curVal) {return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		privFlags[flIDX] = (val ?  privFlags[flIDX] | mask : privFlags[flIDX] & ~mask);
		switch(idx){
			case reCalcRandGradeSpread : {//build new grade distribution
				if (val) {
					gradeAvgExperiment.buildStudentsClassesRandGrades(numStudents, numClasses);
					addPrivBtnToClear(reCalcRandGradeSpread);
					setPrivFlags(drawHistEval, false);
					setPrivFlags(drawFuncEval, false);
				}
				break;}
			case showScaledTransGrades : {
				//TODO need to change what type of grades are shown
				break;}			
			case useZScore : {
				gradeAvgExperiment.setUseZScore(val);
				break;}
			case rebuildDistOnMove : {
				gradeAvgExperiment.setRebuildDistOnGradeMod(val);
				break;}
			//evalPlotClassDists(boolean isHist, int funcType, int numVals, int numBuckets, double low, double high)
			case drawFuncEval : {
				if (val) {
					setPrivFlags(drawHistEval, false);
					//first clear existing results					
					gradeAvgExperiment.clearAllPlotEval();
					//now evaluate new results
					gradeAvgExperiment.evalPlotClassDists(false, funcEvalType, funcEvalNumVals,funcEvalNumBuckets,funcEvalLow, funcEvalHigh);
				} else {//turning off
					if(!getFlags(drawHistEval)) {//if both off then set class experiment draw evals to off
						gradeAvgExperiment.setShowPlots(false);
					}
				}
				break;}
			case drawHistEval : {
				if (val) {
					setPrivFlags(drawFuncEval, false);	
					//first clear existing results					
					gradeAvgExperiment.clearAllPlotEval();
					//now evaluate new results
					gradeAvgExperiment.evalPlotClassDists(true, funcEvalType, funcEvalNumVals,funcEvalNumBuckets,funcEvalLow, funcEvalHigh);					
				} else {//turning off					
					if(!getFlags(drawFuncEval)) {//if both off then set class experiment draw evals to off
						gradeAvgExperiment.setShowPlots(false);
					}
				}
				break;}
			
		}
	}
	
	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(){	
		//pa.outStr2Scr("setupGUIObjsAras start");
		guiMinMaxModVals = new double [][]{
			{2,100,1},						//# students
			{1,9,1},						//# classes
			{0,gIDX_FuncTypeEvalList.length-1,1}, //gIDX_FuncTypeEval	
			{-10.0, 10.0,.01},				//gIDX_FuncEvalLower
			{-10.0, 10.0,.01},				//gIDX_FuncEvalHigher
			{10000,1000000,1000},           // gIDX_FuncEvalNumVals 
			{10,1000,1}                     // gIDX_FuncEvalNumBkts 
			
		};		//min max modify values for each modifiable UI comp	

		guiStVals = new double[]{
				uiVals[gIDX_NumStudents],		//# students
				uiVals[gIDX_NumClasses],		//# classes
				uiVals[gIDX_FuncTypeEval],		//gIDX_FuncTypeEval	
				uiVals[gIDX_FuncEvalLower],		//gIDX_FuncEvalLower
				uiVals[gIDX_FuncEvalHigher],	//gIDX_FuncEvalHigher				
				uiVals[gIDX_FuncEvalNumVals],    // gIDX_FuncEvalNumVals
				uiVals[gIDX_FuncEvalNumBkts],    // gIDX_FuncEvalNumBkts
		};								//starting value
		
		guiObjNames = new String[]{
				"Number of Students",
				"Number of Classes",
				"Eval Func Type : ",		//gIDX_FuncTypeEval	
				"Eval Func Low : ",			//gIDX_FuncEvalLower
				"Eval Func High : ",		//gIDX_FuncEvalHigher
				"Eval Func # Vals : ", 			// gIDX_FuncEvalNumVals        
				"Eval Func # Bkts (dist) : ",	// gIDX_FuncEvalNumBkts     			
		};								//name/label of component	
		
		//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
		guiBoolVals = new boolean [][]{
			{true, false, true},	//# students
			{true, false, true},	//# classes
			{true, true, true},		//gIDX_FuncTypeEval	
			{false, false, true},	//gIDX_FuncEvalLower
			{false, false, true},	//gIDX_FuncEvalHigher
			{true, false, true}, 	// gIDX_FuncEvalNumVals        
			{true, false, true}, 	// gIDX_FuncEvalNumBkts        
			
		};						//per-object  list of boolean flags
		
		//since horizontal row of UI comps, uiClkCoords[2] will be set in buildGUIObjs		
		guiObjs = new myGUIObj[numGUIObjs];			//list of modifiable gui objects
		if(numGUIObjs > 0){
			buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff});			//builds a horizontal list of UI comps
		}
//		setupGUI_XtraObjs();
	}//setupGUIObjsAras
	
	//all ui objects should have an entry here to show how they should interact
	@Override
	protected void setUIWinVals(int UIidx) {
		float val = (float)guiObjs[UIidx].getVal();
		float oldVal = uiVals[UIidx];
		int ival = (int)val;
		if(val != uiVals[UIidx]){//if value has changed...
			uiVals[UIidx] = val;
			switch(UIidx){		
			case gIDX_NumStudents 			:{
				if(ival != numStudents){
					numStudents = ival;
					gradeAvgExperiment.buildStudentsClassesRandGrades(numStudents, numClasses);
				}	break;}			
			case gIDX_NumClasses 			:{
				if(ival != numClasses){
					numClasses = ival;
					gradeAvgExperiment.buildStudentsClassesRandGrades(numStudents, numClasses);
				}	break;}
			case gIDX_FuncTypeEval : 	{
				if (ival != funcEvalType) {				funcEvalType = ival;}		
				break;	}
			case gIDX_FuncEvalLower : 	{						
				if (val != funcEvalLow) {				funcEvalLow = val;	}				
				break;	}
			case gIDX_FuncEvalHigher : 	{
				if (val != funcEvalHigh) {				funcEvalHigh = val;	}				
				break;	}
			case gIDX_FuncEvalNumVals : 	{						
				if (ival != funcEvalNumVals) {			funcEvalNumVals = ival;	}				
				break;	}
			case gIDX_FuncEvalNumBkts : 	{						
				if (ival != funcEvalNumBuckets) {		funcEvalNumBuckets = ival;	}				
				break;	}
			}
		}//if val is different
	}//setUIWinVals
	
	//handle list ui components - return display value for list-based UI object
	@Override
	protected String getUIListValStr(int UIidx, int validx) {
		switch(UIidx){
		case gIDX_FuncTypeEval : {return gIDX_FuncTypeEvalList[validx % gIDX_FuncTypeEvalList.length];}
		default : {break;}
	}
	return "";	}


	//check whether the mouse is over a legitimate map location
	public boolean chkMouseClick2D(int mouseX, int mouseY, int btn){		
		return gradeAvgExperiment.checkMouseClickInExp2D( mouseX-(int)this.rectDim[0], mouseY, btn);
	}//chkMouseOvr

	
	//check whether the mouse is over a legitimate map location
	public boolean chkMouseMoveDragState2D(int mouseX, int mouseY, int btn){		
		return gradeAvgExperiment.checkMouseDragMoveInExp2D( mouseX-(int)this.rectDim[0], mouseY, btn);
	}//chkMouseOvr
	
	//check whether the mouse is over a legitimate map location
	public void setMouseReleaseState2D(){	gradeAvgExperiment.setMouseReleaseInExp2D();}//chkMouseOvr

	
	@Override
	protected void drawMe(float animTimeMod) {
		pa.pushMatrix();pa.pushStyle();
		pa.translate(this.rectDim[0],0,0);
		//all drawing stuff goes here
		gradeAvgExperiment.drawExp();
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
		gradeAvgExperiment.setVisibleScreenWidth();
		
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
	private void setCustMenuBtnNames() {
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
					//gradeAvgExperiment.calcInverseMapping();
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
					gradeAvgExperiment.testFleishTransform();
					resetButtonState();
					break;}
				case 2 : {	
					gradeAvgExperiment.testFleishRangeOfVals();
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
					setPrivFlags(useZScore, false);
					setPrivFlags(rebuildDistOnMove, true);
					gradeAvgExperiment.linearTransformExperiment(numStudents, numClasses);
					resetButtonState();
					break;}
				case 1 : {	
					setPrivFlags(rebuildDistOnMove, true);
					gradeAvgExperiment.uniformTransformExperiment(numStudents, numClasses);
					resetButtonState();
					break;}
				case 2 : {	
					setPrivFlags(useZScore, true);
					setPrivFlags(rebuildDistOnMove, true);
					gradeAvgExperiment.linearTransformExperiment(numStudents, numClasses);
					resetButtonState();
					break;}
				case 3 : {	
					gradeAvgExperiment.fleishModelExperiment(numStudents, numClasses);
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
	
	private void toggleDbgBtn(int idx, boolean val) {
		setPrivFlags(idx, !getPrivFlags(idx));
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
	public void hndlFileLoad(String[] vals, int[] stIdx) {
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileSave 
		//hndlFileLoad_GUI(vals, stIdx);
		//loading in grade data from grade file - vals holds array of strings, expected to be comma sep values, for a single class, with student names and grades
		
		
	}
	@Override
	public ArrayList<String> hndlFileSave() {
		ArrayList<String> res = new ArrayList<String>();
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileLoad 
		//res = hndlFileSave_GUI();
		//saving student grades to a file for a single class - vals holds array of strings, expected to be comma sep values, for a single class, with student names and grades
		
		return res;
	}

	@Override
	protected void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){	}
	
	
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		boolean res = chkMouseMoveDragState2D(mouseX, mouseY, -1);
		return res;
	}
	//alt key pressed handles trajectory
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {	
		boolean res = checkUIButtons(mouseX, mouseY);	
		if(!res) {
			res = chkMouseClick2D(mouseX, mouseY, mseBtn);
		}
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
	protected myPoint getMsePtAs3DPt(int mouseX, int mouseY) {
		// TODO Auto-generated method stub
		return null;
	}

}
