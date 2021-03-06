package graphProbExp_PKG;

import java.io.File;

import base_Math_Objects.MyMathUtils;
import processing.core.*;

import base_UI_Objects.*;
import base_UI_Objects.windowUI.base.myDispWindow;
import base_UI_Objects.windowUI.sidebar.mySideBarMenu;
import classGradeExperimentsPKG.Grade2DWindow;

/**
 * Testbed to experiment with synthesizing probablity distributions and displaying the results
 * John Turner 
 */
public class GraphProbExpMain extends GUI_AppManager {
	//project-specific variables
	public String prjNmLong = "Testbed for Graphical Probability Experiments", prjNmShrt = "GraphProbExp";
	
	//platform independent path separator
	public String dirSep = File.separator;
	//don't use sphere background for this program
	private boolean useSphereBKGnd = false;	
	private final int
		showUIMenu 			= 0,
		show1stWinIDX		= 1,			//whether to show 1st window
		show2ndWinIDX		= 2,			//whether to show 2nd window
		show2DRayTracerIDX 	= 3,
		showGradeWinIDX		= 4;			//whether or not to show grade experiment window

	public final int numVisFlags = 5;		//must only be used for visible windows
	//idx's in dispWinFrames for each window - 0 is always left side menu window
	private static final int disp1stWinIDX = 1,
							disp2ndWinIDX = 2,
							disp2DRayTracerIDX = 3,
							dispGradeWinIDX = 4;

	//private boolean cyclModCmp;										//comparison every draw of cycleModDraw			
	private final int[] bground = new int[]{244,244,244,255};		//bground color
	private PShape bgrndSphere;										//giant sphere encapsulating entire scene


///////////////
//CODE STARTS
///////////////	
	//////////////////////////////////////////////// code
	
	//do not modify this
	public static void main(String[] passedArgs) {		
		GraphProbExpMain me = new GraphProbExpMain();
		my_procApplet._invokedMain(me, passedArgs);		    
	}//main

	/**
	 * whether or not we want to restrict window size on widescreen monitors
	 * 
	 * @return 0 - use monitor size regardless
	 * 			1 - use smaller dim to be determine window 
	 * 			2+ - TBD
	 */
	@Override
	protected int setAppWindowDimRestrictions() {	return 1;}	
	
	@Override
	protected String getPrjNmLong() {return prjNmLong;}
	@Override
	protected String getPrjNmShrt() {return prjNmShrt;}
	/**
	 * return the default background color set in the calling application
	 * @return
	 */
	@Override
	protected void setup_indiv() {	if(useSphereBKGnd) {			setBkgndSphere();	} else {		setBkgrnd();	}}// setup
	
	private void setBkgndSphere() {
		pa.setSphereDetail(100);
		//TODO move to window to set up specific background for each different "scene" type
		PImage bgrndTex = ((my_procApplet)pa).loadImage("bkgrndTex.jpg");
		bgrndSphere = ((my_procApplet)pa).createShape(PConstants.SPHERE, 10000);
		bgrndSphere.setTexture(bgrndTex);
		bgrndSphere.rotate(MyMathUtils.halfPi_f,-1,0,0);
		bgrndSphere.setStroke(false);	
		//TODO move to myDispWindow
		((my_procApplet)pa).background(bground[0],bground[1],bground[2],bground[3]);		
		((my_procApplet)pa).shape(bgrndSphere);	
		pa.setSphereDetail(10);
	}
	
	@Override
	protected void setBkgrnd(){
		((my_procApplet)pa).background(bground[0],bground[1],bground[2],bground[3]);		
	}//setBkgrnd
	
	/**
	 * determine which main flags to show at upper left of menu 
	 */
	@Override
	protected void initMainFlags_Indiv() {
		setMainFlagToShow_debugMode(false);
		setMainFlagToShow_saveAnim(true); 
		setMainFlagToShow_runSim(false);
		setMainFlagToShow_singleStep(false);
		setMainFlagToShow_showRtSideMenu(true);
	}

	@Override
	//build windows here
	protected void initVisOnce_Indiv() {
		showInfo = true;
		int numWins = numVisFlags;//includes 1 for menu window (never < 1)
		//titles and descs, need to be set before sidebar menu is defined
		String[] _winTitles = new String[]{"","3D Exp Win","2D Exp Win","2D Ray Tracer","Grading Exp Win"},
				_winDescr = new String[] {"", "3D environment to conduct and visualize experiments","2D environment to conduct and visualize experiments","2D ray tracing environment for probability experiments","2D Class Grade Experiment Visualization"};
		initWins(numWins,_winTitles, _winDescr);

		//call for menu window
		buildInitMenuWin(showUIMenu);
		//instanced window dimensions when open and closed - only showing 1 open at a time
		float[] _dimOpen  =  new float[]{menuWidth, 0, pa.getWidth()-menuWidth,  pa.getHeight()}, _dimClosed  =  new float[]{menuWidth, 0, hideWinWidth,  pa.getHeight()};	
		System.out.println("Width : " + pa.getWidth() + " | Height : " +  pa.getHeight());
		int wIdx = dispMenuIDX,fIdx=showUIMenu;
		dispWinFrames[wIdx] = this.buildSideBarMenu(wIdx, fIdx, new String[]{"Special Functions 1","Special Functions 2"}, new int[] {3,5}, 5, true, true);
		// new mySideBarMenu(this, winTitles[wIdx], fIdx, winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);			
		
		//define windows
		//idx 0 is menu, and is ignored	
		//setInitDispWinVals : use this to define the values of a display window
		//int _winIDX, 
		//float[] _dimOpen, float[] _dimClosed  : dimensions opened or closed
		//String _ttl, String _desc 			: window title and description
		//boolean[] _dispFlags 					: 
		//   flags controlling display of window :  idxs : 0 : canDrawInWin; 1 : canShow3dbox; 2 : canMoveView; 3 : dispWinIs3d
		//int[] _fill, int[] _strk, 			: window fill and stroke colors
		//int _trajFill, int _trajStrk)			: trajectory fill and stroke colors, if these objects can be drawn in window (used as alt color otherwise)

		//3D window
		wIdx = disp1stWinIDX; fIdx = show1stWinIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,true,true,true}, new int[]{255,255,255,255},new int[]{0,0,0,255},new int[]{180,180,180,255},new int[]{100,100,100,255}); 
		dispWinFrames[wIdx] = new Main3DWindow(pa, this, winTitles[wIdx], fIdx, winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);
		//2d window
		wIdx = disp2ndWinIDX; fIdx = show2ndWinIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,false,false}, new int[]{50,40,20,255}, new int[]{255,255,255,255},new int[]{180,180,180,255},new int[]{100,100,100,255});
		dispWinFrames[wIdx] = new Alt2DWindow(pa, this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);
		//ray tracer window
		wIdx = disp2DRayTracerIDX; fIdx = show2DRayTracerIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,false,false}, new int[]{20,30,10,255}, new int[]{255,255,255,255},new int[]{180,180,180,255},new int[]{100,100,100,255}); 
		dispWinFrames[wIdx] = new RayTracer2DWin(pa, this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);
		//grades experiment window
		wIdx = dispGradeWinIDX; fIdx = showGradeWinIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,false,false}, new int[]{50,20,50,255}, new int[]{255,255,255,255},new int[]{180,180,180,255},new int[]{100,100,100,255}); 
		dispWinFrames[wIdx] = new Grade2DWindow(pa, this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);

		//specify windows that cannot be shown simultaneously here
		initXORWins(new int[]{show1stWinIDX,show2ndWinIDX, show2DRayTracerIDX, showGradeWinIDX},new int[]{disp1stWinIDX, disp2ndWinIDX, disp2DRayTracerIDX, dispGradeWinIDX});

		
	}//initVisOnce_Indiv
		
	@Override
	//called from base class, once at start of program after vis init is called
	protected void initOnce_Indiv(){
		setVisFlag(showUIMenu, true);					//show input UI menu	
		//setVisFlag(showGradeWinIDX, true);
		setVisFlag(show2DRayTracerIDX, true);
	}//	initOnce
	
	@Override
	//called multiple times, whenever re-initing
	protected void initProgram_Indiv(){}//initProgram
	
	@Override
	protected void initVisProg_Indiv() {}	
	@Override
	public String[] getMouseOverSelBtnNames() {
		// TODO Auto-generated method stub
		return new String[0];
	}


	//////////////////////////////////////////////////////
	/// user interaction
	//////////////////////////////////////////////////////	
	//key is key pressed
	//keycode is actual physical key pressed == key if shift/alt/cntl not pressed.,so shift-1 gives key 33 ('!') but keycode 49 ('1')


	/**
	 * handle non-numeric keys being pressed
	 * @param keyVal character of key having been pressed
	 * @param keyCode actual code of key having been pressed
	 */
	@Override
	protected void handleKeyPress(char keyVal, int keyCode) {
		switch (keyVal){
			case ' ' : {toggleSimIsRunning(); break;}							//run sim
			case 'f' : {dispWinFrames[curFocusWin].setInitCamView();break;}					//reset camera
			case 'a' :
			case 'A' : {toggleSaveAnim();break;}						//start/stop saving every frame for making into animation
			case 's' :
			case 'S' : {break;}//saveSS(prjNmShrt);break;}//save picture of current image			
			default : {	}
		}//switch	
			
	}//handleNonNumberKeyPress

	
	@Override
	//gives multiplier based on whether shift, alt or cntl (or any combo) is pressed
	public double clickValModMult(){return ((altIsPressed() ? .1 : 1.0) * (shiftIsPressed() ? 10.0 : 1.0));}	
	//keys/criteria are present that means UI objects are modified by set values based on clicks (as opposed to dragging for variable values)
	//to facilitate UI interaction non-mouse computers, set these to be single keys
	@Override
	public boolean isClickModUIVal() {
		//TODO change this to manage other key settings for situations where multiple simultaneous key presses are not optimal or conventient
		return altIsPressed() || shiftIsPressed();		
	}
	
	@Override
	public void handleShowWin(int btn, int val, boolean callFlags){//display specific windows - multi-select/ always on if sel
		if(!callFlags){//called from setflags - only sets button state in UI to avoid infinite loop
			setMenuBtnState(mySideBarMenu.btnShowWinIdx,btn, val);
		} else {//called from clicking on buttons in UI
			//val is btn state before transition 
			boolean bVal = (val == 1?  false : true);
			//each entry in this array should correspond to a clickable window
			setVisFlag(winFlagsXOR[btn], bVal);
		}
	}//handleShowWin
	
	
	//get the ui rect values of the "master" ui region (another window) -> this is so ui objects of one window can be made, clicked, and shown displaced from those of the parent windwo
	public float[] getUIRectVals(int idx){
			//this.pr("In getUIRectVals for idx : " + idx);
		switch(idx){
			case dispMenuIDX 		: { return new float[0];}			//idx 0 is parent menu sidebar
			case disp1stWinIDX 		: { return dispWinFrames[dispMenuIDX].uiClkCoords;}
			case disp2ndWinIDX 		: {	return dispWinFrames[dispMenuIDX].uiClkCoords;}
			case disp2DRayTracerIDX	: {	return dispWinFrames[dispMenuIDX].uiClkCoords;}
			case dispGradeWinIDX	: {	return dispWinFrames[dispMenuIDX].uiClkCoords;}
			
			default :  return dispWinFrames[dispMenuIDX].uiClkCoords;
			}
	}//getUIRectVals
	
	//////////////////////////////////////////
	/// graphics and base functionality utilities and variables
	//////////////////////////////////////////
	
	/**
	 * return the number of visible window flags for this application
	 * @return
	 */
	@Override
	public int getNumVisFlags() {return numVisFlags;}
	@Override
	//address all flag-setting here, so that if any special cases need to be addressed they can be
	protected void setVisFlag_Indiv(int idx, boolean val ){
		switch (idx){
			case showUIMenu 	    : { dispWinFrames[dispMenuIDX].setFlags(myDispWindow.showIDX,val);    break;}											//whether or not to show the main ui window (sidebar)			
			case show1stWinIDX			: {setWinFlagsXOR(disp1stWinIDX, val); break;}
			case show2ndWinIDX			: {setWinFlagsXOR(disp2ndWinIDX, val); break;}
			case show2DRayTracerIDX		: {setWinFlagsXOR(disp2DRayTracerIDX, val); break;}		
			case showGradeWinIDX		: {setWinFlagsXOR(dispGradeWinIDX, val); break;}			
			default : {break;}
		}
	}//setFlags  
	
	/**
	 * any instancing-class-specific colors - colorVal set to be higher than IRenderInterface.gui_OffWhite
	 * @param colorVal
	 * @param alpha
	 * @return
	 */
	@Override
	public int[] getClr_Custom(int colorVal, int alpha) {		return new int[] {255,255,255,alpha};	}
	@Override
	protected void setSmoothing() {
		pa.setSmoothing(0);
		
	}

}//class GraphProbExpMain
