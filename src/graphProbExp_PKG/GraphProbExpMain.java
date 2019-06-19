package graphProbExp_PKG;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;			//used for threading

import processing.core.*;
import processing.event.MouseEvent;
import processing.opengl.*;

import base_UI_Objects.*;
import base_UI_Objects.windowUI.myDispWindow;
import base_Utils_Objects.*;
import classGradeExperimentsPKG.Grade2DWindow;

/**
 * Testbed to experiment with synthesizing probablity distributions and displaying the results
 * John Turner 
 */
public class GraphProbExpMain extends my_procApplet {
	//project-specific variables
	public String prjNmLong = "Testbed for Graphical Probability Experiments", prjNmShrt = "GraphProbExp";
	
	//data in files created by SOM_MAP separated by spaces
	public String SOM_FileToken = " ", csvFileToken = "\\s*,\\s*";
	//platform independent path separator
	public String dirSep = File.separator;
	//don't use sphere background for this program
	private boolean useSphereBKGnd = false;	
	
	private int[] visFlags;
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
		String[] appletArgs = new String[] { "graphProbExp_PKG.GraphProbExpMain" };
		    if (passedArgs != null) {PApplet.main(PApplet.concat(appletArgs, passedArgs)); } else {PApplet.main(appletArgs);		    }
	}//main
	public void settings(){	size((int)(displayWidth*.95f), (int)(displayHeight*.92f),P3D);	noSmooth();}		
	
	@Override
	protected void setup_indiv() {	if(useSphereBKGnd) {			setBkgndSphere();	} else {		setBkgrnd();	}}// setup
	
	private void setBkgndSphere() {
		sphereDetail(100);
		//TODO move to window to set up specific background for each different "scene" type
		PImage bgrndTex = loadImage("bkgrndTex.jpg");
		bgrndSphere = createShape(SPHERE, 10000);
		bgrndSphere.setTexture(bgrndTex);
		bgrndSphere.rotate(HALF_PI,-1,0,0);
		bgrndSphere.setStroke(false);	
		//TODO move to myDispWindow
		background(bground[0],bground[1],bground[2],bground[3]);		
		shape(bgrndSphere);	
	}
	
	public void setBkgrnd(){
		background(bground[0],bground[1],bground[2],bground[3]);		
	}//setBkgrnd
	
	/**
	 * determine which main flags to show at upper left of menu 
	 */
	@Override
	protected void initMainFlags_Priv() {
		setMainFlagToShow_debugMode(false);
		setMainFlagToShow_saveAnim(true); 
		setMainFlagToShow_runSim(false);
		setMainFlagToShow_singleStep(false);
		setMainFlagToShow_showRtSideMenu(true);
	}

	@Override
	//build windows here
	protected void initVisOnce_Priv() {
		showInfo = true;
		drawnTrajEditWidth = 10;
		int numWins = numVisFlags;//includes 1 for menu window (never < 1)
		//titles and descs, need to be set before sidebar menu is defined
		String[] _winTitles = new String[]{"","3D Exp Win","2D Exp Win","2D Ray Tracer","Grading Exp Win"},
				_winDescr = new String[] {"", "3D environment to conduct and visualize experiments","2D environment to conduct and visualize experiments","2D ray tracing environment for probability experiments","2D Class Grade Experiment Visualization"};
		initWins(numWins,_winTitles, _winDescr);

		//call for menu window
		buildInitMenuWin(showUIMenu);
		//instanced window dimensions when open and closed - only showing 1 open at a time
		float[] _dimOpen  =  new float[]{menuWidth, 0, width-menuWidth, height}, _dimClosed  =  new float[]{menuWidth, 0, hideWinWidth, height};	
		System.out.println("Width : " + width + " | Height : " + height);
		int wIdx = dispMenuIDX,fIdx=showUIMenu;
		dispWinFrames[wIdx] = new mySideBarMenu(this, winTitles[wIdx], fIdx, winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx],dispWinFlags[wIdx][dispCanDrawInWinIDX]);			
		
		//define windows
		//idx 0 is menu, and is ignored	
		//setInitDispWinVals : use this to define the values of a display window
		//int _winIDX, 
		//float[] _dimOpen, float[] _dimClosed  : dimensions opened or closed
		//String _ttl, String _desc 			: window title and description
		//boolean[] _dispFlags 					: flags controlling display of window :  idxs : 0 : canDrawInWin; 1 : canShow3dbox; 2 : canMoveView; 3 : dispWinIs3d
		//int[] _fill, int[] _strk, 			: window fill and stroke colors
		//int _trajFill, int _trajStrk)			: trajectory fill and stroke colors, if these objects can be drawn in window (used as alt color otherwise)

		//3D window
		wIdx = disp1stWinIDX; fIdx = show1stWinIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,true,true,true}, new int[]{255,255,255,255},new int[]{0,0,0,255},gui_LightGray,gui_DarkGray); 
		dispWinFrames[wIdx] = new Main3DWindow(this, winTitles[wIdx], fIdx, winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx],dispWinFlags[wIdx][dispCanDrawInWinIDX]);
		//2d window
		wIdx = disp2ndWinIDX; fIdx = show2ndWinIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,false,false}, new int[]{50,40,20,255}, new int[]{255,255,255,255},gui_LightGray,gui_DarkGray);
		dispWinFrames[wIdx] = new Alt2DWindow(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx],dispWinFlags[wIdx][dispCanDrawInWinIDX]);
		//ray tracer window
		wIdx = disp2DRayTracerIDX; fIdx = show2DRayTracerIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,false,false}, new int[]{20,30,10,255}, new int[]{255,255,255,255},gui_LightGray,gui_DarkGray); 
		dispWinFrames[wIdx] = new RayTracer2DWin(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx],dispWinFlags[wIdx][dispCanDrawInWinIDX]);
		//grades experiment window
		wIdx = dispGradeWinIDX; fIdx = showGradeWinIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,false,false}, new int[]{50,20,50,255}, new int[]{255,255,255,255},gui_LightGray,gui_DarkGray); 
		dispWinFrames[wIdx] = new Grade2DWindow(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx],dispWinFlags[wIdx][dispCanDrawInWinIDX]);

		//specify windows that cannot be shown simultaneously here
		initXORWins(new int[]{show1stWinIDX,show2ndWinIDX, show2DRayTracerIDX, showGradeWinIDX},new int[]{disp1stWinIDX, disp2ndWinIDX, disp2DRayTracerIDX, dispGradeWinIDX});

		
	}//initVisOnce_Priv
		
	@Override
	//called from base class, once at start of program after vis init is called
	protected void initOnce_Priv(){
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
	//main draw loop
	public void draw(){	
		//private draw routine
		_drawPriv();
		surface.setTitle(prjNmLong + " : " + (int)(frameRate) + " fps|cyc curFocusWin : " + curFocusWin);
	}//draw

	//handle pressing keys 0-9
	//keyVal is actual value of key (screen character as int)
	//keyPressed is actual key pressed (shift-1 gives keyVal 33 ('!') but keyPressed 49 ('1')) 
	//need to subtract 48 from keyVal or keyPressed to get actual number
	private void handleNumberKeyPress(int keyVal, int keyPressed) {
		//use key if want character 
		if (key == '0') {
			setVisFlag(showUIMenu,true);
		} 
		
	}//handleNumberKeyPress
	
	//////////////////////////////////////////////////////
	/// user interaction
	//////////////////////////////////////////////////////	
	//key is key pressed
	//keycode is actual physical key pressed == key if shift/alt/cntl not pressed.,so shift-1 gives key 33 ('!') but keycode 49 ('1')
	public void keyPressed(){
		if(key==CODED) {
			if(!shiftIsPressed()){setShiftPressed(keyCode  == 16);} //16 == KeyEvent.VK_SHIFT
			if(!cntlIsPressed()){setCntlPressed(keyCode  == 17);}//17 == KeyEvent.VK_CONTROL			
			if(!altIsPressed()){setAltPressed(keyCode  == 18);}//18 == KeyEvent.VK_ALT
		} else {	
			//handle pressing keys 0-9 (with or without shift,alt, cntl)
			if ((keyCode>=48) && (keyCode <=57)) { handleNumberKeyPress(((int)key),keyCode);}
			else {					//handle all other (non-numeric) keys
				switch (key){
					case ' ' : {toggleSimIsRunning(); break;}							//run sim
					case 'f' : {dispWinFrames[curFocusWin].setInitCamView();break;}					//reset camera
					case 'a' :
					case 'A' : {toggleSaveAnim();break;}						//start/stop saving every frame for making into animation
					case 's' :
					case 'S' : {save(getScreenShotSaveName(prjNmShrt));break;}//save picture of current image			
					default : {	}
				}//switch	
			}
		}
	}//keyPressed()
	
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
	@Override
		//init boolean state machine flags for program
	public void initVisFlags(){
		visFlags = new int[1 + numVisFlags/32];for(int i =0; i<numVisFlags;++i){forceVisFlag(i,false);}	
		((mySideBarMenu)dispWinFrames[dispMenuIDX]).initPFlagColors();			//init sidebar window flags
	}		
	@Override
	//address all flag-setting here, so that if any special cases need to be addressed they can be
	public void setVisFlag(int idx, boolean val ){
		int flIDX = idx/32, mask = 1<<(idx%32);
		visFlags[flIDX] = (val ?  visFlags[flIDX] | mask : visFlags[flIDX] & ~mask);
		switch (idx){
			case showUIMenu 	    : { dispWinFrames[dispMenuIDX].setFlags(myDispWindow.showIDX,val);    break;}											//whether or not to show the main ui window (sidebar)			
			case show1stWinIDX			: {setWinFlagsXOR(disp1stWinIDX, val); break;}
			case show2ndWinIDX			: {setWinFlagsXOR(disp2ndWinIDX, val); break;}
			case show2DRayTracerIDX		: {setWinFlagsXOR(disp2DRayTracerIDX, val); break;}		
			case showGradeWinIDX		: {setWinFlagsXOR(dispGradeWinIDX, val); break;}			
			default : {break;}
		}
	}//setFlags  
	@Override
	//get vis flag
	public boolean getVisFlag(int idx){int bitLoc = 1<<(idx%32);return (visFlags[idx/32] & bitLoc) == bitLoc;}	
	@Override
	public void forceVisFlag(int idx, boolean val) {
		int flIDX = idx/32, mask = 1<<(idx%32);
		visFlags[flIDX] = (val ?  visFlags[flIDX] | mask : visFlags[flIDX] & ~mask);
		//doesn't perform any other ops - to prevent 
	}
	

}//class GraphProbExpMain
