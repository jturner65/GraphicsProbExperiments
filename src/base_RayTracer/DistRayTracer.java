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
	//private boolean useSphereBKGnd = false;	
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
		setMainFlagToShow_debugMode(false);
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
		
//			case '`' : {if(!gCurrentFile.equals("")){ loadedScenes.get(gCurrentFile).flipNormal();}   break;}//flip poly norms
//			case '1':  {gCurrentFile = currentDir + "t01.cli";      loadFile();       break;}		//photon map cli's
//			case '2':  {gCurrentFile = currentDir + "t02.cli";      loadFile();       break;}
//			case '3':  {gCurrentFile = currentDir + "t03.cli";      loadFile();       break;}
//			case '4':  {gCurrentFile = currentDir + "t04.cli";      loadFile();       break;}
//			case '5':  {gCurrentFile = currentDir + "t05.cli";      loadFile();       break;}
//			case '6':  {gCurrentFile = currentDir + "t06.cli";      loadFile();       break;}
//			case '7':  {gCurrentFile = currentDir + "t07.cli";      loadFile();       break;}
//			case '8':  {gCurrentFile = currentDir + "t08.cli";      loadFile();       break;}
//			case '9':  {gCurrentFile = currentDir + "t09.cli";      loadFile();       break;}
//			case '0':  {gCurrentFile = currentDir + "t10.cli";      loadFile();       break;}
//			case '-':  {gCurrentFile = currentDir + "t11.cli";      loadFile();       break;}		//cornell box			
//		    case '!':  {gCurrentFile = currentDir + "p4_st01.cli";  loadFile();       break;}		//worley textures on spheres
//		    case '@':  {gCurrentFile = currentDir + "p4_st02.cli";  loadFile();       break;}
//		    case '#':  {gCurrentFile = currentDir + "p4_st03.cli";  loadFile();       break;}
//		    case '$':  {gCurrentFile = currentDir + "p4_st04.cli";  loadFile();       break;}
//		    case '%':  {gCurrentFile = currentDir + "p4_st05.cli";  loadFile();       break;}
//		    case '^':  {gCurrentFile = currentDir + "p4_st06.cli";  loadFile();       break;}
//		    case '&':  {gCurrentFile = currentDir + "p4_st07.cli";  loadFile();       break;}	    
//		    case '*':  {gCurrentFile = currentDir + "p4_st08.cli";  loadFile();       break;}		    
//		    case 'l':  {gCurrentFile = currentDir + "p4_st09.cli";  loadFile();       break;}	    
//		    case ';':  {gCurrentFile = currentDir + "p4_t09.cli";   loadFile();       break;}		    
//			case 'O':  {gCurrentFile = currentDir + "p4_t05.cli";   loadFile();       break;}		//wood bunny texture
//			case 'P':  {gCurrentFile = currentDir + "p4_t06.cli";   loadFile();       break;}		//marble bunny texture
//			case '(':  {gCurrentFile = currentDir + "p4_t07.cli";   loadFile();       break;}		//worley circles bunny texture
//			case ')':  {gCurrentFile = currentDir + "p4_t08.cli";   loadFile();       break;}		//crackle bunny texture		
//			case '_':  {gCurrentFile = currentDir + "p3_t10.cli"; 	loadFile();		break;}
//			case '=':  {gCurrentFile = currentDir + "plnts3ColsBunnies.cli"; loadFile(); break;}
//		    case '+':  {gCurrentFile = currentDir + "p3_t02_sierp.cli"; loadFile();	break;}		//TODO INVESTIGATE THIS		    
//		    case 'A':  {gCurrentFile = currentDir + "p2_t01.cli"; 		loadFile();	break;}	//these are images from project 2
//		    case 'S':  {gCurrentFile = currentDir + "p2_t02.cli"; 		loadFile();	break;}
//		    case 'D':  {gCurrentFile = currentDir + "p2_t03.cli"; 		loadFile();	break;}
//		    case 'F':  {gCurrentFile = currentDir + "p2_t04.cli"; 		loadFile();	break;}
//		    case 'G':  {gCurrentFile = currentDir + "p2_t05.cli"; 		loadFile();	break;}
//		    case 'H':  {gCurrentFile = currentDir + "p2_t06.cli"; 		loadFile();	break;}
//		    case 'J':  {gCurrentFile = currentDir + "p2_t07.cli"; 		loadFile();	break;}
//		    case 'K':  {gCurrentFile = currentDir + "p2_t08.cli"; 		loadFile();	break;}
//		    case 'L':  {gCurrentFile = currentDir + "p2_t09.cli"; 		loadFile();	break;}
//		    case ':':  {gCurrentFile = currentDir + "old_t07c.cli"; 	loadFile();	break;}
//			case 'a':  {gCurrentFile = currentDir + "earthAA1.cli";   	loadFile();	break;}
//			case 's':  {gCurrentFile = currentDir + "earthAA2.cli";   	loadFile();	break;}
//			case 'd':  {gCurrentFile = currentDir + "earthAA3.cli";   	loadFile();	break;}
//			case 'f':  {gCurrentFile = currentDir + "c2clear.cli";   	loadFile();	break;}
//			case 'g':  {gCurrentFile = currentDir + "c3shinyBall.cli";  loadFile(); 	break;}
//			case 'h':  {gCurrentFile = currentDir + "c4InSphere.cli";   loadFile();	break;}
//			case 'j':  {gCurrentFile = currentDir + "c6.cli";   		loadFile();	break;}
//			case 'k':  {gCurrentFile = currentDir + "c6Fish.cli";   	loadFile();	break;}				
//		    case 'Q':  {gCurrentFile = currentDir + "c2torus.cli"; 		loadFile();	break;}			    
//		    case 'W':  {gCurrentFile = currentDir + "old_t02.cli"; 		loadFile();	break;}//this is the most recent block of images for project 1b
//		    case 'E':  {gCurrentFile = currentDir + "old_t03.cli"; 		loadFile();	break;}
//		    case 'R':  {gCurrentFile = currentDir + "old_t04.cli"; 		loadFile();	break;}
//		    case 'T':  {gCurrentFile = currentDir + "old_t05.cli"; 		loadFile();	break;}
//		    case 'Y':  {gCurrentFile = currentDir + "old_t06.cli"; 		loadFile();	break;}
//		    case 'U':  {gCurrentFile = currentDir + "old_t07.cli"; 		loadFile();	break;}
//		    case 'I':  {gCurrentFile = currentDir + "old_t08.cli"; 		loadFile();	break;}
//			case '{':  {gCurrentFile = currentDir + "old_t09.cli"; 		loadFile();	break;}
//			case '}':  {gCurrentFile = currentDir + "old_t10.cli"; 		loadFile();	break;}			
//			case 'q':  {gCurrentFile = currentDir + "planets.cli"; 		loadFile();	break; }
//			case 'w':  {gCurrentFile = currentDir + "planets2.cli"; 	loadFile();	break;}
//			case 'e':  {gCurrentFile = currentDir + "planets3.cli"; 	loadFile();	break;}
//			case 'r':  {gCurrentFile = currentDir + "planets3columns.cli";loadFile();   break;}
//			case 't' : {gCurrentFile = currentDir + "trTrans.cli";   	loadFile();	break;}
//			case 'y':  {gCurrentFile = currentDir + "planets3Ortho.cli";loadFile(); 	break;}			
//			case 'u':  {gCurrentFile = currentDir + "c1.cli";  			loadFile();	break;}
//			case 'i':  {gCurrentFile = currentDir + "c2.cli";  			loadFile();	break;}
//			case 'o':  {gCurrentFile = currentDir + "c3.cli";  			loadFile();	break;}
//			case 'p':  {gCurrentFile = currentDir + "c4.cli";  			loadFile();	break;}
//			case '[':  {gCurrentFile = currentDir + "c5.cli";  			loadFile();	break;}
//			case ']':  {gCurrentFile = currentDir + "c0.cli";  			loadFile();	break;}		    
//			case 'Z':  {gCurrentFile = currentDir + "p3_t01.cli"; 		loadFile();	break;}
//			case 'X':  {gCurrentFile = currentDir + "p3_t02.cli"; 		loadFile();	break;}
//			case 'C':  {gCurrentFile = currentDir + "p3_t03.cli"; 		loadFile();	break;}
//			case 'V':  {gCurrentFile = currentDir + "p3_t04.cli"; 		loadFile();	break;}
//			case 'B':  {gCurrentFile = currentDir + "p3_t05.cli"; 		loadFile();	break;}
//			case 'N':  {gCurrentFile = currentDir + "p3_t06.cli"; 		loadFile();	break;}
//			case 'M':  {gCurrentFile = currentDir + "p3_t07.cli"; 		loadFile();	break;}
//			case '<':  {gCurrentFile = currentDir + "p4_t06_2.cli"; 	loadFile();	break;}
//			case '>':  {gCurrentFile = currentDir + "p4_t09.cli"; 		loadFile();	break;}
//			case '?':  {gCurrentFile = currentDir + "p3_t11_sierp.cli"; loadFile();		break;}		//my bunny scene		
//			case 'z':  {gCurrentFile = currentDir + "cylinder1.cli";  	loadFile();	break;}
//			case 'x':  {gCurrentFile = currentDir + "tr0.cli";   		loadFile();	break;}
//			case 'c':  {gCurrentFile = currentDir + "c0Square.cli";  	loadFile();	break;}
//			case 'v':  {gCurrentFile = currentDir + "c1octo.cli";  		loadFile();	break;}
//			case 'b':  {gCurrentFile = currentDir + "old_t0rotate.cli"; loadFile(); 	break;}
//		    case 'n':  {gCurrentFile = currentDir + "old_t03a.cli"; 	loadFile();	break;}	//this block contains the first set of images given with assignment 1b 
//		    case 'm':  {gCurrentFile = currentDir + "old_t04a.cli"; 	loadFile();	break;}
//		    case ',':  {gCurrentFile = currentDir + "old_t05a.cli"; 	loadFile();	break;}
//		    case '.':  {gCurrentFile = currentDir + "old_t06a.cli"; 	loadFile();	break;}
//		    case '/':  {gCurrentFile = currentDir + "old_t07a.cli"; 	loadFile();	break;}			
			default : {return;}
		}//switch		
	}//handleKeyPress
	
//	private void loadFile() {
//		if(!gCurrentFile.equals("")){
//			Base_Scene tmp = rdr.readRTFile(loadedScenes, gCurrentFile, null, sceneCols, sceneRows);//pass null as scene so that we don't add to an existing scene
//			if(null==tmp) {gCurrentFile = "";}
//		}		
//	}
	
//	public String getDirName(){	if(currentDir.equals("")){	return "data/";}	return "data/"+currentDir;}
//	public void setCurDir(int input){
//		switch (input){
//		case 0 :{currentDir = "";break;}
//		case 1 :{currentDir = "old/";break;}
//		case 2 :{currentDir = "project1_1/";break;}
//		case 3 :{currentDir = "project1_2/";break;}
//		case 4 :{currentDir = "project2_1/";break;}
//		case 5 :{currentDir = "project2_2/";break;}
//		case 6 :{currentDir = "project3/";break;}
//		case 7 :{currentDir = "project4/";break;}
//		case 8 :{currentDir = "project5/";break;}
//		default :{currentDir = "";break;}
//		}
//	}//setCurDir


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
