package experiments_PKG.classGradeExp.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_ProbTools.baseProbExpMgr;
import base_ProbTools.randGenFunc.funcs.base.Base_RandVarFunc;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Type;
import base_Utils_Objects.io.messaging.MsgCodes;
import experiments_PKG.classGradeExp.experiment.ClassGradeExperiment;

public class Grade2DWindow extends Base_DispWindow {
	
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
	//private int plotTypeIDX = 0;
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

	public final int numGUIObjs = 13;	
	/////////
	//ui button names -empty will do nothing, otherwise add custom labels for debug and custom functionality names
	//ddl 
	public static final String[] 
			gIDX_FuncTypeEvalList = Base_RandVarFunc.queryFuncTypes,
			gIDX_ExpDistTypeList = ClassGradeExperiment.expType;	
	
	//private static final String[] noCosCompExpTypes = new String[] {"Linear", "Uniform Spaced"};

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
	private static final int numPrivFlags = 9;
	//idxs of all plots
	private static final int[] showPlotIDXs = new int[] {drawFuncEval, drawHistEval, drawMultiEval};

	/////////
	//custom debug/function ui button names -empty will do nothing
	public String[][] menuBtnNames = new String[][] {	//each must have literals for every button defined in side bar menu, or ignored
		{"Func 01", "Func 02","Func 03","Func 04","Func 05"},				//row 1
		{"Test Inv Fl", "Test Fleish", "Tst Fl Range", "Test COS","Func 05"},	//row 2
		{"Test Zig Seq","DBG 2","DBG 3","DBG 4","DBG 5"}	
	};

	//class experiment
	private ClassGradeExperiment gradeAvgExperiment;
	
	public Grade2DWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx, int _flagIdx) {
		super(_p, _AppMgr, _winIdx, _flagIdx);
		super.initThisWin(false);
	}//ctor
			
	@Override
	protected void initMe() {
		//called once
		//this window is runnable
		dispFlags.setIsRunnable(true);
		//this window uses a customizable camera
		dispFlags.setUseCustCam(true);
		// capable of using right side menu
		dispFlags.setDrawRtSideMenu(true);
		//grade experiments
		gradeAvgExperiment = new ClassGradeExperiment(pa, this, curVisScrDims);
		setGradeExp(true,true,true, false);
		
		//set visibility width and send to experiments - experiment must be built first
		setVisScreenDimsPriv();			
		//set offset to use for custom menu objects
		custMenuOffset = uiClkCoords[3];	
//		//set this to default to moving distribution
//		setPrivFlags(rebuildDistOnMove, true);
//		setPrivFlags(use1pSineCosCDF, true);
	
	}//
	
	//initialize all UI buttons here
	@Override
	public int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray) {
		//give true labels, false labels and specify the indexes of the booleans that should be tied to UI buttons
		
		tmpBtnNamesArray.add(new Object[]{"Rebuilding/reloading Grades",     "Rebuild/reload Grades",            reCalcRandGradeSpread});       
		tmpBtnNamesArray.add(new Object[]{"Rebuilding Final Grade Dist",     "Rebuild Final Grade Dist",         reBuildFinalGradeDist});         
		tmpBtnNamesArray.add(new Object[]{"Setting Current Grades as Glbl",  "Set Current Grades as Glbl",       setCurrGrades});                 
		tmpBtnNamesArray.add(new Object[]{"CosCDF 1 + sine x",               "CosCDF x + sine x",                use1pSineCosCDF});                 
		tmpBtnNamesArray.add(new Object[]{"Rebuild Class dist on move",      "Don't rebuild class dist on move", rebuildDistOnMove});              
		tmpBtnNamesArray.add(new Object[]{"ZScore for final grades",         "Specific Dist for final grades",   useZScore});                     
		tmpBtnNamesArray.add(new Object[]{"Eval/Draw Func on Bounds",        "Eval/Draw Func on Bounds",         drawFuncEval});                    
		tmpBtnNamesArray.add(new Object[]{"Eval/Draw Hist of Dist",          "Eval/Draw Hist of Dist"	,        drawHistEval});                  
		tmpBtnNamesArray.add(new Object[]{"Showing Cos To Gauss Dist",        "Compare Cos To Gauss Dist",			drawMultiEval});   
		
		return numPrivFlags;
	}
	
	/**
	 * This function would provide an instance of the override class for base_UpdateFromUIData, which would
	 * be used to communicate changes in UI settings directly to the value consumers.
	 */
	@Override
	protected UIDataUpdater buildUIDataUpdateObject() {
		return new Grade2DUIDataUpdater(this);
	}
	/**
	 * This function is called on ui value update, to pass new ui values on to window-owned consumers
	 */
	@Override
	protected final void updateCalcObjUIVals() {
		gradeAvgExperiment.updateUIDataValues(uiUpdateData);
	}

	@Override
	protected int[] getFlagIDXsToInitToTrue() {
		// TODO Auto-generated method stub
		return new int[] {rebuildDistOnMove,use1pSineCosCDF};
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
	
	/**
	 * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param tmpUIObjArray : map of object data, keyed by UI object idx, with array values being :                    
	 *           the first element double array of min/max/mod values                                                   
	 *           the 2nd element is starting value                                                                      
	 *           the 3rd elem is label for object                                                                       
	 *           the 4th element is object type (GUIObj_Type enum)
	 *           the 5th element is boolean array of : (unspecified values default to false)
	 *           	{value is sent to owning window, 
	 *           	value is sent on any modifications (while being modified, not just on release), 
	 *           	changes to value must be explicitly sent to consumer (are not automatically sent)}    
	 * @param tmpListObjVals : map of list object possible selection values
	 */
	@Override
	protected void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){	
		
		
		tmpListObjVals.put(gIDX_FuncTypeEval,gIDX_FuncTypeEvalList);
		tmpListObjVals.put(gIDX_ExpDistType,gIDX_ExpDistTypeList);

		tmpUIObjArray.put(gIDX_NumStudents, new Object[] {new double[]{2,100,1},										     1.0*numStudents,        "Number of Students : ", GUIObj_Type.IntVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_NumClasses, new Object[] {new double[]{1,9,1},												 1.0*numClasses,         "Number of Classes : ", GUIObj_Type.IntVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_ExpDistType, new Object[] {new double[]{0,tmpListObjVals.get(gIDX_ExpDistType).length-1,1},   1.0*expTypeIDX,         "Exp Mapping Type : ", GUIObj_Type.ListVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_FuncTypeEval, new Object[] {new double[]{0,tmpListObjVals.get(gIDX_FuncTypeEval).length-1,1}, 1.0*funcEvalType,       "Plot Eval Func Type : ", GUIObj_Type.ListVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_FuncEvalLower, new Object[] {new double[]{-10.0, 10.0,.01},			                         1.0*funcEvalLow,        "Plot Eval Func Low : ", GUIObj_Type.FloatVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_FuncEvalHigher, new Object[] {new double[]{-10.0, 10.0,.01},			                         1.0*funcEvalHigh,       "Plot Eval Func High : ", GUIObj_Type.FloatVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_FuncEvalNumVals, new Object[] {new double[]{10000,1000000,1000},                              1.0*funcEvalNumVals,    "Plot Eval Func # Vals : ", GUIObj_Type.IntVal, new boolean[]{true}}); 
		tmpUIObjArray.put(gIDX_FuncEvalNumBkts, new Object[] {new double[]{10,1000,1},                                       1.0*funcEvalNumBuckets, "Plot Eval Func # Bkts (dist) : ", GUIObj_Type.IntVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_FinalGradeNumMmnts, new Object[] {new double[]{2, 4, .1},			                         1.0*finalGradeNumMmnts, "Final Grade # Momments (2-4) : ", GUIObj_Type.IntVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_FinalGradeMean, new Object[] {new double[]{0.0, 1.0,.01},			                         1.0*finalGradeMmtns[0], "Final Grade Mean : ", GUIObj_Type.FloatVal, new boolean[]{true}}); 
		tmpUIObjArray.put(gIDX_FinalGradeSTD, new Object[] {new double[]{0.0, 1.0,.01},				                         1.0*finalGradeMmtns[1], "Final Grade Std Dev : ", GUIObj_Type.FloatVal, new boolean[]{true}}); 
		tmpUIObjArray.put(gIDX_FinalGradeSkew, new Object[] {new double[]{-5.0,5.0,.01},			                         1.0*finalGradeMmtns[2], "Final Grade Skew : ", GUIObj_Type.FloatVal, new boolean[]{true}}); 
		tmpUIObjArray.put(gIDX_FinalGradeExKurt, new Object[] {new double[]{0.0, 5.0,.01},			                         1.0*finalGradeMmtns[3],  "Final Grade Ex Kurt : ", GUIObj_Type.FloatVal, new boolean[]{true}});
		//min max modify values for each modifiable UI comp	

	}//setupGUIObjsAras
	
	/**
	 * Called if int-handling guiObjs[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_IntValsCustom(int UIidx, int ival, int oldVal) {
		switch(UIidx){	
			case gIDX_NumStudents 			:{
				numStudents = ival;
				//(boolean rebuildStudents, boolean rebuildClasses,  boolean rebuildGrades, boolean loadFromRosters)
				setGradeExp(true, false, false, false);
				break;}			
			case gIDX_NumClasses 			:{
				numClasses = ival;
				setGradeExp(false, true, false, false);
				break;}
			case gIDX_ExpDistType : {							
				expTypeIDX = ival;
				setGradeExp(false, false, false, false);
				break;}	
			case gIDX_FuncTypeEval : 	{
				funcEvalType = ival;		
				break;	}
			case gIDX_FuncEvalNumVals : 	{						
				funcEvalNumVals = ival;					
				break;	}
			case gIDX_FuncEvalNumBkts : 	{						
				funcEvalNumBuckets = ival;					
				break;	}			
			case gIDX_FinalGradeNumMmnts  : 	{
				finalGradeNumMmnts = ival;			
				break;}	
			default : {
				msgObj.dispWarningMessage(className, "setUI_IntValsCustom", "No int-defined gui object mapped to idx :"+UIidx);
				break;}
		}
	}//setUI_IntValsCustom
	
	/**
	 * Called if float-handling guiObjs[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal float value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_FloatValsCustom(int UIidx, float val, float oldVal) {
		switch(UIidx){	
			case gIDX_FuncEvalLower : 	{						
				funcEvalLow = val;					
				break;	}
			case gIDX_FuncEvalHigher : 	{
				funcEvalHigh = val;					
				break;	}
			case gIDX_FinalGradeMean	  : 	{
				finalGradeMmtns[0] = val;	setFinalGradeVals();		
				break;}    
			case gIDX_FinalGradeSTD	      : 	{
				finalGradeMmtns[1] = val;	setFinalGradeVals();					
				break;}
			case gIDX_FinalGradeSkew	  : 	{	
				finalGradeMmtns[2] = val;	
				double skewSQ = finalGradeMmtns[2] * finalGradeMmtns[2], bound = -1.2264489 + 1.6410373*skewSQ; 
				guiObjs[gIDX_FinalGradeExKurt].setNewMin((bound > 0 ? bound : 0));
				guiObjs[gIDX_FinalGradeExKurt].setNewMax((guiObjs[gIDX_FinalGradeExKurt].getMinVal()+1)*10);
				break;}   
			case gIDX_FinalGradeExKurt	  : 	{		//Exkurtosis is  skewness vals for fleishman polynomial
				//verify legitimate kurt for fleishman dist
				double skewSQ = finalGradeMmtns[2] * finalGradeMmtns[2], bound = -1.2264489 + 1.6410373*skewSQ; 
				finalGradeMmtns[3] = (val < bound ? bound : val) ;
				guiObjs[UIidx].setVal(finalGradeMmtns[3]);			
				break;}	
			default : {
				msgObj.dispWarningMessage(className, "setUI_FloatValsCustom", "No float-defined gui object mapped to idx :"+UIidx);
				break;}
		}	
	}//setUI_FloatValsCustom
	
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
		_descVals[1]= (finalGradeNumMmnts == 2 ? baseProbExpMgr.ziggRandGen : baseProbExpMgr.fleishRandGen_UniVar);		
		_descVals[2]= (finalGradeNumMmnts == 2 ? baseProbExpMgr.gaussRandVarIDX : baseProbExpMgr.fleishRandVarIDX);
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
		pa.pushMatState();
		pa.translate(this.rectDim[0],0,0);
		//all drawing stuff goes here
		gradeAvgExperiment.drawExp();
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
		gradeAvgExperiment.setVisibleScreenDims(curVisScrDims);		
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
					resetButtonState();
					break;}
				case 1 : {	
					resetButtonState();
					break;}
				case 2 : {	
					resetButtonState();
					break;}
				default : {
					resetButtonState();
					break;}
			}	
			break;}//row 1 of menu side bar buttons
		case 1 : {
			msgObj.dispInfoMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 2 | Btn : " + btn);
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
					resetButtonState();
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
			case 0: {				gradeAvgExperiment.testSeqZigGen();break;			}
			case 1: {				gradeAvgExperiment.dbgTestStuff();break;			}
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
	protected void setCameraIndiv(float[] camVals){		
		//, float rx, float ry, float dz are now member variables of every window
		pa.setCameraWinVals(camVals);//(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
		// puts origin of all drawn objects at screen center and moves forward/away by dz
		pa.translate(camVals[0],camVals[1],(float)dz); 
	    setCamOrient();	
	}//setCameraIndiv

	@Override
	protected void stopMe() {msgObj.dispInfoMessage(className,"stopMe","Stop");}	
	
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
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		boolean res = chkMouseMoveDragState2D(mouseX, mouseY, -1);
		return res;
	}
	//alt key pressed handles trajectory
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {	
		boolean res = chkMouseClick2D(mouseX, mouseY, mseBtn);
		
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

	@Override
	public void processTrajIndiv(DrawnSimpleTraj drawnTraj) {
		// TODO Auto-generated method stub
		
	}

}
