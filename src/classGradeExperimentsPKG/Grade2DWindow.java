package classGradeExperimentsPKG;

import java.io.File;
import java.util.*;

import base_ProbTools.BaseProbExpMgr;
import base_ProbTools.randGenFunc.funcs.myRandVarFunc;
import base_UI_Objects.*;
import base_UI_Objects.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.windowUI.myDispWindow;
import base_UI_Objects.windowUI.myGUIObj;
import base_Utils_Objects.*;
import base_Utils_Objects.vectorObjs.myPoint;
import base_Utils_Objects.vectorObjs.myVector;
import graphProbExp_PKG.mySideBarMenu;

public class Grade2DWindow extends myDispWindow {
	
	/////////////
	// ui objects 
	////////////
	// # of students across all classes
	private int numStudents = 5;
	// # of classes to build final grade
	private int numClasses = 3;
	//eval func IDX
	private int funcEvalType = 1, funcEvalNumVals = 10000, funcEvalNumBuckets = 200;
	//lower and upper bound for functional evaluation 
	private double funcEvalLow = -.1f, funcEvalHigh = 1;
	//index in list of experiments that we are using - 0 corresponds to gaussian, default value
	private int expTypeIDX = 0;
	//index in list of experiments that we are using - 0 corresponds to gaussian, default value
	private int plotTypeIDX = 0;
	//final grade values - cannot be any function that derives pdf from samples 
	private int finalGradeNumMmnts = 2;	//if 2 then uses mean and var, zigg alg; if >2 then uses fleish
	//moments of final grade target
	private double[] finalGradeMmtns = new double[] {0.75,.2,0,0};
	
	//idxs - need one per object
	public final static int
		gIDX_NumStudents		= 0,
		gIDX_NumClasses			= 1,
		gIDX_ExpDistType		= 2,
		gIDX_FuncTypeEval		= 3,
		gIDX_FuncEvalLower		= 4,
		gIDX_FuncEvalHigher		= 5, 
		gIDX_FuncEvalNumVals	= 6,
		gIDX_FuncEvalNumBkts	= 7,
		gIDX_FinalGradeNumMmnts = 8,			//# of moments to use to descibe final grade, at least 2, no more than 4
		gIDX_FinalGradeMean		= 9,
		gIDX_FinalGradeSTD		= 10,
		gIDX_FinalGradeSkew		= 11,
		gIDX_FinalGradeExKurt	= 12;
		
	//initial values - need one per object
	public double[] uiVals = new double[]{
			numStudents,
			numClasses,
			expTypeIDX,
			funcEvalType,
			funcEvalLow,
			funcEvalHigh,
			funcEvalNumVals,
			funcEvalNumBuckets,
			finalGradeNumMmnts,
			finalGradeMmtns[0],
			finalGradeMmtns[1],
			finalGradeMmtns[2],
			finalGradeMmtns[3]
	};			//values of 8 ui-controlled quantities
	public final int numGUIObjs = uiVals.length;	
	/////////
	//ui button names -empty will do nothing, otherwise add custom labels for debug and custom functionality names
	//ddl 
	public static final String[] 
			gIDX_FuncTypeEvalList = myRandVarFunc.queryFuncTypes,
			gIDX_ExpDistTypeList = ClassGradeExperiment.expType;	
	
	private static final String[] noCosCompExpTypes = new String[] {"Linear", "Uniform Spaced"};

	//////////////
	// local/ui interactable boolean buttons
	//////////////
	//private child-class flags - window specific
	//for every class-wide boolean make an index, and increment numPrivFlags.
	//use getPrivFlags(idx) and setPrivFlags(idx,val) to consume
	//put idx-specific code in case statement in setPrivFlags
	public static final int 
			reCalcRandGradeSpread 		= 0,					//recalculate random grades given specified class and student # parameters	
			reBuildFinalGradeDist		= 1,
			setCurrGrades				= 2,					//use the current grades as the global grades, so they are preserved when the type of transformation is changed
			use1pSineCosCDF				= 3,					//whether to use 1 + sine x or x + sine x model for CosCDF distribution 
			useZScore					= 4,					//whether or not to use the z score calc to transform the uniform final grades
			rebuildDistOnMove			= 5,					//rebuild class distribution when value is moved
			//drawing function
			drawFuncEval				= 6,					//draw results of function evaluation
			drawHistEval				= 7,					//draw results of histogram evaluation
			drawMultiEval				= 8;					//draw overlay of multiple results 
	public static final int numPrivFlags = 9;
	//idxs of all plots
	private static final int[] showPlotIDXs = new int[] {drawFuncEval, drawHistEval, drawMultiEval};

	/////////
	//custom debug/function ui button names -empty will do nothing
	public String[][] menuBtnNames = new String[][] {	//each must have literals for every button defined in side bar menu, or ignored
		{},
		{"Func 01", "Func 02","Func 03","Func 04","Func 05"},				//row 1
		{"Test Inv Fl", "Test Fleish", "Tst Fl Range", "Test COS","Func 05"},	//row 2
		{"Test Zig Seq","DBG 2","DBG 3","DBG 4","DBG 5"}	
	};

	//class experiment
	private ClassGradeExperiment gradeAvgExperiment;
	
	public Grade2DWindow(my_procApplet _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		float stY = rectDim[1]+rectDim[3]-4*yOff,stYFlags = stY + 2*yOff;
		trajFillClrCnst = new int[] {0,120,120,255};
		trajStrkClrCnst = new int[] {0,255,255,255};
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
		
		//grade experiments
		gradeAvgExperiment = new ClassGradeExperiment(this);
		setGradeExp(true,true,true, false);
		
		//set visibility width and send to experiments - experiment must be built first
		setVisScreenDimsPriv();			
		//set offset to use for custom menu objects
		custMenuOffset = uiClkCoords[3];	
		//set this to default to moving distribution
		setPrivFlags(rebuildDistOnMove, true);
		setPrivFlags(use1pSineCosCDF, true);
		
		//moved from mapMgr ctor, to remove dependence on papplet in that object
		pa.setAllMenuBtnNames(menuBtnNames);	
	
	}//
	
	//initialize all UI buttons here
	@Override
	public void initAllPrivBtns() {
		//give true labels, false labels and specify the indexes of the booleans that should be tied to UI buttons
		truePrivFlagNames = new String[]{			//needs to be in order of privModFlgIdxs
				"Rebuilding/reloading Grades","Rebuilding Final Grade Dist","Setting Current Grades as Glbl",
				"CosCDF 1 + sine x","Rebuild Class dist on move","ZScore for final grades",
				"Eval/Draw Func on Bounds","Eval/Draw Hist of Dist","Showing Cos To Gauss Dist"
		};
		falsePrivFlagNames = new String[]{			//needs to be in order of flags
				"Rebuild/reload Grades","Rebuild Final Grade Dist","Set Current Grades as Glbl",
				"CosCDF x + sine x","Don't rebuild class dist on move","Specific Dist for final grades",
				"Eval/Draw Func on Bounds","Eval/Draw Hist of Dist"	,"Compare Cos To Gauss Dist"			
		};
		privModFlgIdxs = new int[]{					//idxs of buttons that are able to be interacted with
				reCalcRandGradeSpread,reBuildFinalGradeDist,setCurrGrades,
				use1pSineCosCDF, rebuildDistOnMove,useZScore,
				drawFuncEval,drawHistEval,drawMultiEval
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
					setGradeExp(false, false, true, false);
					addPrivBtnToClear(reCalcRandGradeSpread);
					setPrivFlags(drawHistEval, false);
					setPrivFlags(drawFuncEval, false);
				}
				break;}
			case rebuildDistOnMove : {
				gradeAvgExperiment.setRebuildDistOnGradeMod(val);
				break;}
			
			case reBuildFinalGradeDist :{		//rebuild final grade dist and mappings using current ui values
				if(val) {
					setFinalGradeVals();
					addPrivBtnToClear(reBuildFinalGradeDist);
				}
				break;}		
			
			case use1pSineCosCDF : {
				gradeAvgExperiment.setCosCDF_RVFOpts(0,(val ? 0 : 1));// 0==1 + sine; 1 == x + sine 
				break;}
			
			case useZScore : {
				gradeAvgExperiment.setUseZScore(val);
				break;}
			//evalPlotClassDists(boolean isHist, int funcType, int numVals, int numBuckets, double low, double high)
			case drawFuncEval : {
				//System.out.println("drawFuncEval : " + val + " getPrivFlags(drawHistEval) : " + getPrivFlags(drawHistEval)+ " getPrivFlags(drawFuncEval) : " + getPrivFlags(drawFuncEval));
				if (val) {
					if((expTypeIDX == 1) || (expTypeIDX == 2)) {
						gradeAvgExperiment.setShowPlots(false);
						setPrivFlags(idx, false);					
					} else {
						clearAllPlotsButMe(idx);
						//now evaluate new results
						gradeAvgExperiment.evalPlotClassFuncs(funcEvalType, funcEvalNumVals, funcEvalLow, funcEvalHigh);
					}
				} else {//turning off
					if(!isShowingPlots()) {gradeAvgExperiment.setShowPlots(false);}
				}
				break;}
			case drawHistEval : {
				//System.out.println("drawHistEval : " + val + " getPrivFlags(drawHistEval) : " + getPrivFlags(drawHistEval)+ " getPrivFlags(drawFuncEval) : " + getPrivFlags(drawFuncEval));
				if (val) {
					clearAllPlotsButMe(idx);
					//now evaluate new results
					gradeAvgExperiment.evalPlotClassHists(funcEvalNumVals,funcEvalNumBuckets,funcEvalLow, funcEvalHigh);					
				} else {//turning off					
					if(!isShowingPlots()) {gradeAvgExperiment.setShowPlots(false);}				}
				break;}		
			
			case drawMultiEval : {
				if (val) {
					if((expTypeIDX == 1) || (expTypeIDX == 2)) {
						gradeAvgExperiment.setShowPlots(false);
						setPrivFlags(idx, false);					
					} else {
						clearAllPlotsButMe(idx);						
						//now evaluate new results for selected options
						gradeAvgExperiment.evalCosAndNormWithHist(funcEvalNumVals,funcEvalNumBuckets,funcEvalLow, funcEvalHigh);		
					}	
				} else {//turning off					
					if(!isShowingPlots()) {gradeAvgExperiment.setShowPlots(false);}				
				}
				break;}		
				
			case setCurrGrades : {
				if(val) {
					gradeAvgExperiment.updateGlblGrades();
					addPrivBtnToClear(setCurrGrades);
				}
				break;}
		}//switch
	}//setPrivFlags
	
	private void clearAllPlotsButMe(int meIDX) {for(int idx : showPlotIDXs) {if(idx==meIDX) continue;setPrivFlags(idx, false);}}//clearAllPlotsButMe		
	private boolean isShowingPlots() {for(int idx : showPlotIDXs) {if(getPrivFlags(idx)) return true;}	return false;}//isShowingPlots
	
	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(){	
		//pa.outStr2Scr("setupGUIObjsAras start");
		guiMinMaxModVals = new double [][]{
			{2,100,1},						//# students
			{1,9,1},						//# classes
			{0,gIDX_ExpDistTypeList.length-1,1},
			{0,gIDX_FuncTypeEvalList.length-1,1}, //gIDX_FuncTypeEval	
			{-10.0, 10.0,.01},				//gIDX_FuncEvalLower
			{-10.0, 10.0,.01},				//gIDX_FuncEvalHigher
			{10000,1000000,1000},           // gIDX_FuncEvalNumVals 
			{10,1000,1},                    // gIDX_FuncEvalNumBkts 			
			{2, 4, .1},						//gIDX_FinalGradeNumMmnts 	= 8,			//# of moments to use to descibe final grade, at least 2, no more than 4
			{0.0, 1.0,.01},					//gIDX_FinalGradeMean		= 9,
			{0.0, 1.0,.01},					//gIDX_FinalGradeSTD		= 10,
			{-5.0,5.0,.01},					//gIDX_FinalGradeSkew		= 11,
			{0.0, 5.0,.01}					//gIDX_FinalGradeExKurt		= 12;					
		};		//min max modify values for each modifiable UI comp	

		guiStVals = new double[]{
				uiVals[gIDX_NumStudents],			//# students
				uiVals[gIDX_NumClasses],			//# classes
				uiVals[gIDX_ExpDistType],			//gIDX_ExpDistType
				uiVals[gIDX_FuncTypeEval],			//gIDX_FuncTypeEval	
				uiVals[gIDX_FuncEvalLower],			//gIDX_FuncEvalLower
				uiVals[gIDX_FuncEvalHigher],		//gIDX_FuncEvalHigher				
				uiVals[gIDX_FuncEvalNumVals],    	// gIDX_FuncEvalNumVals
				uiVals[gIDX_FuncEvalNumBkts],    	// gIDX_FuncEvalNumBkts				
				uiVals[gIDX_FinalGradeNumMmnts],	// gIDX_FinalGradeNumMmnts
				uiVals[gIDX_FinalGradeMean],    	// gIDX_FinalGradeMean	
				uiVals[gIDX_FinalGradeSTD],    		// gIDX_FinalGradeSTD	
				uiVals[gIDX_FinalGradeSkew],    	// gIDX_FinalGradeSkew	
				uiVals[gIDX_FinalGradeExKurt],    	// gIDX_FinalGradeExKurt	
		};								//starting value
		
		guiObjNames = new String[]{
				"Number of Students : ",
				"Number of Classes : ",
				"Exp Mapping Type : ",				//gIDX_ExpDistType
				"Plot Eval Func Type : ",			//gIDX_FuncTypeEval	
				"Plot Eval Func Low : ",			//gIDX_FuncEvalLower
				"Plot Eval Func High : ",			//gIDX_FuncEvalHigher
				"Plot Eval Func # Vals : ", 		// gIDX_FuncEvalNumVals        
				"Plot Eval Func # Bkts (dist) : ",	// gIDX_FuncEvalNumBkts   
				"Final Grade # Momments (2-4) : ",  // gIDX_FinalGradeNumMmnts  
				"Final Grade Mean : ",              // gIDX_FinalGradeMean	    
				"Final Grade Std Dev : ",           // gIDX_FinalGradeSTD	    
				"Final Grade Skew : ",              // gIDX_FinalGradeSkew	    
				"Final Grade Ex Kurt : ",          // gIDX_FinalGradeExKurt	    
				
		};								//name/label of component	
		
		//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
		guiBoolVals = new boolean [][]{
			{true, false, true},	//# students
			{true, false, true},	//# classes
			{true, true, true},		//gIDX_ExpDistType
			{true, true, true},		//gIDX_FuncTypeEval	
			{false, false, true},	//gIDX_FuncEvalLower
			{false, false, true},	//gIDX_FuncEvalHigher
			{true, false, true}, 	// gIDX_FuncEvalNumVals        
			{true, false, true}, 	// gIDX_FuncEvalNumBkts      
			{true, false, true},    // gIDX_FinalGradeNumMmnts  
			{false,false,true},     // gIDX_FinalGradeMean	    
			{false,false,true},     // gIDX_FinalGradeSTD	    
			{false,false,true},     // gIDX_FinalGradeSkew	    
			{false,false,true},     // gIDX_FinalGradeExKurt	    
			
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
		double val = (float)guiObjs[UIidx].getVal();
		double oldVal = (float)uiVals[UIidx];
		int ival = (int)val;
		if(val != uiVals[UIidx]){//if value has changed...
			uiVals[UIidx] = val;
			switch(UIidx){		
			case gIDX_NumStudents 			:{
				if(ival != numStudents){
					numStudents = ival;
					//(boolean rebuildStudents, boolean rebuildClasses,  boolean rebuildGrades, boolean loadFromRosters)
					setGradeExp(true, false, false, false);
				}	break;}			
			case gIDX_NumClasses 			:{
				if(ival != numClasses){
					numClasses = ival;
					setGradeExp(false, true, false, false);
				}	break;}
			case gIDX_ExpDistType : {
				if (ival != expTypeIDX) {				
					expTypeIDX = ival;
					setGradeExp(false, false, false, false);
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
			case gIDX_FinalGradeNumMmnts  : 	{
				if(ival != finalGradeNumMmnts) {		finalGradeNumMmnts = ival;			}
				break;}
			case gIDX_FinalGradeMean	  : 	{
				if (val !=finalGradeMmtns[0]) {			finalGradeMmtns[0] = val;	setFinalGradeVals();		}
				break;}    
			case gIDX_FinalGradeSTD	      : 	{
				if (val !=finalGradeMmtns[1]) {			finalGradeMmtns[1] = val;	setFinalGradeVals();		}				
				break;}
			case gIDX_FinalGradeSkew	  : 	{
				if (val !=finalGradeMmtns[2]) {			
					finalGradeMmtns[2] = val;	
					double skewSQ = finalGradeMmtns[2] * finalGradeMmtns[2], bound = -1.2264489 + 1.6410373*skewSQ; 
					guiObjs[gIDX_FinalGradeExKurt].setNewMin((bound > 0 ? bound : 0));
					guiObjs[gIDX_FinalGradeExKurt].setNewMax((guiObjs[gIDX_FinalGradeExKurt].getMinVal()+1)*10);
				}				
				break;}   
			case gIDX_FinalGradeExKurt	  : 	{		//Exkurtosis is  skewness vals for fleishman polynomial
				if (val !=finalGradeMmtns[3]) {	
					//verify legitimate kurt for fleishman dist
					double skewSQ = finalGradeMmtns[2] * finalGradeMmtns[2], bound = -1.2264489 + 1.6410373*skewSQ; 
					finalGradeMmtns[3] = (val < bound ? bound : val) ;
					guiObjs[UIidx].setVal(finalGradeMmtns[3]);
				}
				break;}
			}
		}//if val is different
	}//setUIWinVals
	
	
	//called when only final grade mapping changes
	private void setFinalGradeVals() {
		//System.out.println("Rebuilding final grade values");
		double[][] _finalGradeMappings = new double[2][];
		int [] _finalGradeDescs = new int[3];
		_buildFinalGradeArrays(_finalGradeMappings,_finalGradeDescs);	
		gradeAvgExperiment.rebuildDesFinalGradeMapping(_finalGradeMappings,_finalGradeDescs);			
	}//setFinalGradeVals()
	//build array that describes desired final grade mapping - pre-inited arrays
	//_mmntsAndMinMax : idx 0 : up to first 4 moments of final grade target mapping; idx 1 : min/max of target mapping
	//_descVals : idx 0 : # of moments final grade mapping dist should use, idx 1 : type of myRandGen ; idx 2 : type of myRandFunc to use
	private void _buildFinalGradeArrays(double [][] _mmntsAndMinMax, int[] _descVals) {
		_descVals[0]= finalGradeNumMmnts; 
		_descVals[1]= (finalGradeNumMmnts == 2 ? BaseProbExpMgr.ziggRandGen : BaseProbExpMgr.fleishRandGen_UniVar);		
		_descVals[2]= (finalGradeNumMmnts == 2 ? BaseProbExpMgr.gaussRandVarIDX : BaseProbExpMgr.fleishRandVarIDX);
		_mmntsAndMinMax[0] = new double[] {finalGradeMmtns[0],finalGradeMmtns[1],finalGradeMmtns[2],finalGradeMmtns[3]};
		_mmntsAndMinMax[1] = new double[] {0.0,1.0};
	}//buildFinalGradeArrays
	
	//set values and generate grade-based experiments - need to specify whether random or using grade files with pre-set grades
	//whether or not to load from rosters or 
	private void setGradeExp(boolean rebuildStudents, boolean rebuildClasses,  boolean rebuildGrades, boolean loadFromRosters) {
		double[][] _finalGradeMappings = new double[2][];
		int [] _finalGradeDescs = new int[] {0,0,0};
		_buildFinalGradeArrays(_finalGradeMappings,_finalGradeDescs);
		if(loadFromRosters) {//rebuild all for this
			//TODO need to manage this
			String _fileNameOfStudents = "";
			String[] _fileNamesOfGrades = new String[] {""};
			gradeAvgExperiment.buildFileBasedGradeExp(_fileNameOfStudents, _fileNamesOfGrades, expTypeIDX,_finalGradeMappings,_finalGradeDescs);
		} else {				
			boolean[] flags = new boolean[] {rebuildStudents, rebuildClasses, rebuildGrades};
			int[] vals = new int[] {numStudents, numClasses};			
			gradeAvgExperiment.buildRandGradeExp(flags, vals, expTypeIDX,_finalGradeMappings,_finalGradeDescs);
		}
	}//setGradeExp
	
	//handle list ui components - return display value for list-based UI object
	@Override
	protected String getUIListValStr(int UIidx, int validx) {
		switch(UIidx){
		case gIDX_FuncTypeEval : {return gIDX_FuncTypeEvalList[validx % gIDX_FuncTypeEvalList.length];}
		case gIDX_ExpDistType : {return gIDX_ExpDistTypeList[validx % gIDX_ExpDistTypeList.length];}
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
		case mySideBarMenu.btnAuxFunc2Idx : {
			pa.outStr2Scr("Clicked Btn row : Aux Func 2 | Btn : " + btn);
			switch(btn){
				case 0 : {	
					//test calculation of inverse fleish function -> derive x such that y = f(x) for fleishman polynomial.  This x is then the value from normal dist that yields y from fleish dist
					double xDesired = -2.0;
					gradeAvgExperiment.testInvFleishCalc(xDesired);
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
					//test cosine function
					gradeAvgExperiment.testCosFunction();
					resetButtonState();
					break;}
				default : {
					break;}	
			}
			break;}//row 2 of menu side bar buttons
		case mySideBarMenu.btnDBGSelCmpIdx : {
			pa.outStr2Scr("Clicked Btn row : Debug | Btn : " + btn);
			switch(btn){
				case 0 : {	
					//test sequential ziggurat values
					gradeAvgExperiment.testSeqZigGen();
					resetButtonState();
					break;}
				case 1 : {	
					gradeAvgExperiment.dbgTestStuff();
					resetButtonState();
					break;}
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
	public void hndlFileLoad(File file, String[] vals, int[] stIdx) {
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileSave 
		//hndlFileLoad_GUI(vals, stIdx);
		//loading in grade data from grade file - vals holds array of strings, expected to be comma sep values, for a single class, with student names and grades
		
		
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
	protected myPoint getMsePtAs3DPt(myPoint mseLoc){return new myPoint(mseLoc.x,mseLoc.y,0);}

}
