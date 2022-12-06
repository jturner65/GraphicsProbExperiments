package base_RayTracer;
/////
///		Final ray tracer from cs7490 - supports distribution RT, acceleration structure(BVH), perlin and worley-noise-based textures, photon mapping (caustic and diffuse) in KD tree
/////

import base_RayTracer.ui.RayTracer2DWin;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.sidebar.SidebarMenu;

//TODO this needs to instance a window - cannot use this method anymore with just the single processing window

//this file can act as a stub for the ray tracer and can launch it
public class DistRayTracer extends GUI_AppManager {
	//project-specific variables
	public String prjNmLong = "Testbed for base ray tracer", prjNmShrt = "RayTracerBaseExp";
	
	//don't use sphere background for this program
	private final int
		showUIMenu 			= 0,
		show2DRayTracerIDX	= 1;			//whether to show 1st window

	public final int numVisFlags = 2;		//must only be used for visible windows
	//idx's in dispWinFrames for each window - 0 is always left side menu window
	private static final int disp2DRayTracerIDX = 1;

	private final int[] bground = new int[]{244,244,244,255};		//bground color

		
	public static void main(String[] passedArgs) {		
		DistRayTracer me = new DistRayTracer();
		DistRayTracer.invokeProcessingMain(me, passedArgs);		    
	}//main

	@Override
	protected void setRuntimeArgsVals(String[] _passedArgs) {
	}
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
	
	@Override
	public void setup_Indiv() {
		setBkgrnd();
	}

	@Override
	protected void setBkgrnd() {pa.setRenderBackground(bground[0],bground[1],bground[2],bground[3]);}	
	
	/**
	 * determine which main flags to show at upper left of menu 
	 */
	@Override
	protected void initMainFlags_Indiv() {
		setMainFlagToShow_debugMode(true);
		setMainFlagToShow_saveAnim(true); 
		setMainFlagToShow_runSim(false);
		setMainFlagToShow_singleStep(false);
		setMainFlagToShow_showRtSideMenu(true);
	}

	
	@Override
	protected void initAllDispWindows() {
		showInfo = true;
		int numWins = numVisFlags;//includes 1 for menu window (never < 1)
		//titles and descs, need to be set before sidebar menu is defined
		String[] _winTitles = new String[]{"","2D Ray Tracer"},
				_winDescr = new String[] {"", "2D ray tracing environment for probability experiments"};
		initWins(numWins,_winTitles, _winDescr);

		//call for menu window
		buildInitMenuWin(showUIMenu);
		//instanced window dimensions when open and closed - only showing 1 open at a time
		float[] _dimOpen  =  new float[]{menuWidth, 0, pa.getWidth()-menuWidth,  pa.getHeight()}, _dimClosed  =  new float[]{menuWidth, 0, hideWinWidth,  pa.getHeight()};	
		System.out.println("Width : " + pa.getWidth() + " | Height : " +  pa.getHeight());
		//application-wide menu button bar titles and button names
		String[] menuBtnTitles = new String[]{"Special Functions 1","Special Functions 2"};
		String[][] menuBtnNames = new String[][] { // each must have literals for every button defined in side bar menu, or ignored
			{"Func 1", "Func 2","Func 3"},	//row 1
			{"Func 1", "Func 2", "Func 3", "Func 4"}};	//row 1
		String[] dbgBtnNames = new String[] {"Debug 0","Debug 1","Debug 2","Debug 3","Debug 4"};
		int wIdx = dispMenuIDX,fIdx=showUIMenu;
		dispWinFrames[wIdx] = buildSideBarMenu(wIdx, fIdx,menuBtnTitles, menuBtnNames, dbgBtnNames, false, true);		

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

		//ray tracer window
		wIdx = disp2DRayTracerIDX; fIdx = show2DRayTracerIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,false,false}, new int[]{20,30,10,255}, new int[]{255,255,255,255},new int[]{180,180,180,255},new int[]{100,100,100,255}); 
		dispWinFrames[wIdx] = new RayTracer2DWin(pa, this, wIdx, fIdx);

		//specify windows that cannot be shown simultaneously here
		initXORWins(
				new int[]{show2DRayTracerIDX},
				new int[]{disp2DRayTracerIDX});			
	}
	@Override
	protected void initOnce_Indiv() {
		setVisFlag(showUIMenu, true);					//show input UI menu	
		setVisFlag(show2DRayTracerIDX, true);
	}
	@Override
	protected void initVisProg_Indiv() {}
	@Override
	protected void initProgram_Indiv() {}	
	
	@Override
	/**
	 * Overriding main because handling 2d + 3d windows
	 */	
	public final void drawMePost_Indiv(float modAmtMillis, boolean is3DDraw){}

	/**
	 * handle key pressed
	 * @param keyVal 0-9, with or without shift ((keyCode>=48) && (keyCode <=57))
	 * @param keyCode actual code of key having been pressed
	 */
	protected void handleKeyPress(char key, int keyCode) {
		switch(key) {
		case ' ' : {toggleSimIsRunning(); break;}							//run sim
		case 'f' : {dispWinFrames[curFocusWin].setInitCamView();break;}					//reset camera
		case 'a' :
		case 'A' : {toggleSaveAnim();break;}						//start/stop saving every frame for making into animation
		case 's' :
		case 'S' : {break;}//saveSS(prjNmShrt);break;}//save picture of current image
		default : {return;}
		}//switch		
	}//handleKeyPress

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
			case showUIMenu 	    : { dispWinFrames[dispMenuIDX].setFlags(Base_DispWindow.showIDX,val);    break;}											//whether or not to show the main ui window (sidebar)			
			case show2DRayTracerIDX		: {setWinFlagsXOR(disp2DRayTracerIDX, val); break;}		
			default : {break;}
		}			
	}
	
	@Override
	//gives multiplier based on whether shift, alt or cntl (or any combo) is pressed
	public double clickValModMult(){return ((altIsPressed() ? .1 : 1.0) * (shiftIsPressed() ? 10.0 : 1.0));}	
	@Override
	public boolean isClickModUIVal() {
		//TODO change this to manage other key settings for situations where multiple simultaneous key presses are not optimal or conventient
		return altIsPressed() || shiftIsPressed();		
	}
	//get the ui rect values of the "master" ui region (another window) -> this is so ui objects of one window can be made, clicked, and shown displaced from those of the parent windwo
	@Override
	public float[] getUIRectVals(int idx){
			//this.pr("In getUIRectVals for idx : " + idx);
		switch(idx){
			case dispMenuIDX 		: { return new float[0];}			//idx 0 is parent menu sidebar
			case disp2DRayTracerIDX	: {	return dispWinFrames[dispMenuIDX].uiClkCoords;}			
			default :  return dispWinFrames[dispMenuIDX].uiClkCoords;
			}
	}//getUIRectVals
	@Override
	public void handleShowWin(int btn, int val, boolean callFlags){//display specific windows - multi-select/ always on if sel
		if(!callFlags){//called from setflags - only sets button state in UI to avoid infinite loop
			setMenuBtnState(SidebarMenu.btnShowWinIdx,btn, val);
		} else {//called from clicking on buttons in UI
			//val is btn state before transition 
			boolean bVal = (val == 1?  false : true);
			//each entry in this array should correspond to a clickable window
			setVisFlag(winFlagsXOR[btn], bVal);
		}
	}//handleShowWin
	
	/**
	 * any instancing-class-specific colors - colorVal set to be higher than IRenderInterface.gui_OffWhite
	 * @param colorVal
	 * @param alpha
	 * @return
	 */
	@Override
	public int[] getClr_Custom(int colorVal, int alpha) {
		// TODO Auto-generated method stub
		return new int[] {255,255,255,alpha};
	}

	@Override
	public String[] getMouseOverSelBtnLabels() {
		return new String[0];
	}

	@Override
	protected void setSmoothing() {		pa.setSmoothing(0);		}


}//DistRayTracer class
