package base_RayTracer.ui.base;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.TreeMap;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_RayTracer.myRTFileReader;
import base_RayTracer.scene.base.Base_Scene;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Type;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * Base class holding display window/UI functionality for Distribution Ray Tracer
 * @author 7strb
 *
 */
public abstract class Base_RayTracerWin extends Base_DispWindow {
	
	/////////////
	// ui objects 
	////////////
	//////////////////////////////////
	//initial values of UI variables
	//ints
	protected final int initSceneCols = 300;
	protected final int initSceneRows = 300;

	//local versions of UI values
	protected int sceneCols;
	protected int sceneRows;
	protected String currSceneName = "", currDispSceneName = "";
	
	//////////////////////////////////////
	//private child-class flags - window specific
	public static final int 
		shootRays			 		= 0,					//shoot rays
		flipNorms					= 1;
	public static final int numPrivFlags = 2;
	
	//idxs - need one per object
	public final static int
		gIDX_SceneCols		= 0,
		gIDX_SceneRows		= 1,
		gIDX_CurrSceneCLI	= 2;
	public final int numGUIObjs = 3;	
	
	public String[] gIDX_NoiseTxtrCLIFileList = new String[] {
			"noiseTxtr_st01.cli","noiseTxtr_st02.cli","noiseTxtr_st03.cli","noiseTxtr_st04.cli","noiseTxtr_st05.cli",
			"noiseTxtr_st06.cli","noiseTxtr_st07.cli","noiseTxtr_st08.cli","noiseTxtr_st09.cli",
			"noiseTxtr_t01.cli","noiseTxtr_t02.cli","noiseTxtr_t03.cli","noiseTxtr_t04.cli","noiseTxtr_t05.cli","noiseTxtr_t05Alt.cli",
			"noiseTxtr_t06_2.cli","noiseTxtr_t06.cli","noiseTxtr_t06Alt.cli","noiseTxtr_t07.cli","noiseTxtr_t08.cli","noiseTxtr_t09.cli"
	};

	public String[] gIDX_CurrSceneCLIList = new String[] {
			"trTrans.cli","plnts3BunInstances.cli","test_2triTxtures.cli","test_QuadTxtures.cli","test_Plane.cli",
			"t01.cli","t02.cli","t03.cli","t04.cli","t05.cli","t06.cli","t07.cli","t08.cli","t09.cli","t10.cli","t11.cli",
			"noiseTxtr_st01.cli","noiseTxtr_st02.cli","noiseTxtr_st03.cli","noiseTxtr_st04.cli","noiseTxtr_st05.cli",
			"noiseTxtr_st06.cli","noiseTxtr_st07.cli","noiseTxtr_st08.cli","noiseTxtr_st09.cli",
			"noiseTxtr_t01.cli","noiseTxtr_t02.cli","noiseTxtr_t03.cli","noiseTxtr_t04.cli","noiseTxtr_t05.cli","noiseTxtr_t05Alt.cli",
			"noiseTxtr_t06_2.cli","noiseTxtr_t06.cli","noiseTxtr_t06Alt.cli","noiseTxtr_t07.cli","noiseTxtr_t08.cli","noiseTxtr_t09.cli",
			"plnts3ColsBunnies.cli","p3_t02_sierp.cli",
			"p2_t01.cli", "p2_t02.cli", "p2_t03.cli", "p2_t04.cli", "p2_t05.cli","p2_t06.cli", "p2_t07.cli", "p2_t08.cli", "p2_t09.cli", 			
			"old_t07c.cli","earthAA1.cli","earthAA2.cli","earthAA3.cli","c2clear.cli","c3shinyBall.cli","c4InSphere.cli","c6.cli", "c6Fish.cli","c2torus.cli",
			"old_t02.cli","old_t03.cli","old_t04.cli","old_t05.cli","old_t06.cli","old_t07.cli","old_t08.cli","old_t09.cli","old_t10.cli",
			"planets.cli","planets2.cli","planets3.cli","planets3columns.cli","planets3Ortho.cli",
			"c0.cli", "c1.cli","c2.cli","c3.cli","c4.cli","c5.cli",
			"p3_t01.cli","p3_t02.cli","p3_t03.cli","p3_t04.cli","p3_t05.cli","p3_t06.cli","p3_t07.cli","p3_t11_sierp.cli", 	
			"cylinder1.cli", "tr0.cli","c0Square.cli",  "c1octo.cli",		
			"old_t0rotate.cli","old_t03a.cli", "old_t04a.cli", "old_t05a.cli", "old_t06a.cli", 	"old_t07a.cli",		
	};

	/**
	 * Ray Tracer cli file reader/interpreter
	 */
	protected myRTFileReader rdr; 		
	
	protected final String cliFilesDir;
	/**
	 * holds references to all loaded scenes
	 */
	public TreeMap<String, Base_Scene> loadedScenes;

	
	/**
	 * @param _p
	 * @param _AppMgr
	 * @param _winIdx
	 * @param _flagIdx
	 */
	public Base_RayTracerWin(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx, int _flagIdx) {
		super(_p, _AppMgr, _winIdx, _flagIdx);
		
		Path path = Paths.get("");
		cliFilesDir = path.toAbsolutePath().toString()+File.separator+"data";
		msgObj.dispInfoMessage(className, "Constructor", "Currently Set absolute working directory for CLI Files is :"+cliFilesDir);
		rdr = new myRTFileReader(pa,this, cliFilesDir+File.separator+"txtrs"+File.separator);
		loadedScenes = new TreeMap<String, Base_Scene>();	
	}

	@Override
	protected final void initMe() {
		//this window is runnable
		setFlags(isRunnable, true);
		//this window uses a customizable camera
		setFlags(useCustCam, true);
		//this window uses right side info window
		setFlags(drawRightSideMenu, true);
		//set offset to use for custom menu objects
		custMenuOffset = uiClkCoords[3];		
		//instance-specific init
		initMe_Indiv();
		//call first ray trace
		startRayTrace();
	}
	/**
	 * Instance-class specific init
	 */
	protected abstract void initMe_Indiv();
	
	@Override
	public final int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray) {
		//give true labels, false labels and specify the indexes of the booleans that should be tied to UI buttons
		tmpBtnNamesArray.add(new Object[]{"Shooting Rays", "Shoot Rays", shootRays});  
		tmpBtnNamesArray.add(new Object[]{"Norms are Flipped", "Flip Normals", flipNorms}); 
		return initAllPrivBtns_Indiv(tmpBtnNamesArray);
	}//initAllPrivBtns	
	
	/**
	 * Instance-class specific UI button init
	 * @param tmpBtnNamesArray
	 * @return # of buttons total
	 */
	protected abstract int initAllPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray);
	
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
	protected final void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){		
		//set up list of files to load
		tmpListObjVals.put(gIDX_CurrSceneCLI, gIDX_CurrSceneCLIList);
		tmpUIObjArray.put(gIDX_SceneCols, new Object[] {new double[]{100,AppMgr.getDisplayWidth(),10}, 1.0*initSceneCols, "Image Width (pxls)", GUIObj_Type.IntVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_SceneRows, new Object[] {new double[]{100,AppMgr.getDisplayHeight(),10}, 1.0*initSceneRows, "Image Height (pxls)", GUIObj_Type.IntVal, new boolean[]{true}});
		tmpUIObjArray.put(gIDX_CurrSceneCLI, new Object[] {new double[]{0,tmpListObjVals.get(gIDX_CurrSceneCLI).length-1,1}, 0.0, "Scene to Display", GUIObj_Type.ListVal, new boolean[]{true}});
		sceneCols = initSceneCols;
		sceneRows = initSceneRows;
		currSceneName = gIDX_CurrSceneCLIList[0];

		setupGUIObjsAras_Indiv(tmpUIObjArray, tmpListObjVals);
	}//setupGUIObjsAras
	
	/**
	 * Instance-class specific UI objects setup
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
	protected abstract void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals);
	
	/**
	 * This function provides an instance of the override class for base_UpdateFromUIData, which would
	 * be used to communicate changes in UI settings directly to the value consumers.  For this abstract class, 
	 * it forces the created inheritor of Base_RayTracerUIUpdater to be created by the instancing window class.
	 */
	@Override
	protected final UIDataUpdater buildUIDataUpdateObject() {
		return buildUIDataUpdateObject_Indiv();
	}
	
	/**
	 * Instance-class specific updater creator
	 * @return
	 */
	protected abstract Base_RayTracerUIUpdater buildUIDataUpdateObject_Indiv();

	@Override
	public final void setPrivFlags(int idx, boolean val) {
		boolean curVal = getPrivFlags(idx);
		if(val == curVal) {return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		privFlags[flIDX] = (val ?  privFlags[flIDX] | mask : privFlags[flIDX] & ~mask);
		switch(idx){
			case shootRays : {//build new image
				if (val) {
					startRayTrace();
					addPrivBtnToClear(idx);
				}
				break;}
			case flipNorms : {
				setFlipNorms();
				break;}
			default : {setPrivFlags_Indiv(idx, val);}
		}
	}//setPrivFlags	
	protected abstract void setPrivFlags_Indiv(int idx, boolean val);
	
	public void setFlipNorms() {
		Base_Scene s = loadedScenes.get(currSceneName);
		if(s!=null) {s.flipNormal();}
	}
	
	public void startRayTrace() {	
		Base_Scene tmp = rdr.readRTFile(loadedScenes, cliFilesDir, currSceneName, null, sceneCols, sceneRows);//pass null as scene so that we don't add to an existing scene
		msgObj.dispMessage("RayTracerExperiment", "startRayTrace", "Done with readRTFile", MsgCodes.info1);
		//returns null means not found
		if(null==tmp) {currSceneName = "";}
		currDispSceneName = currSceneName;
	}
	
	/**
	 * This will return a location to place the rendered image in the display window so that it is centered.
	 * @return
	 */
	public final float[] getLocUpperCrnr() {
		return new float[] {this.rectDim[0]+ .5f*(this.rectDim[2]-sceneCols), this.rectDim[1]+ .5f*(this.rectDim[3]-sceneRows)};		
	}

	@Override
	protected final int[] getFlagIDXsToInitToTrue() {
		int[] flagsToInit = new int[] {};
		return getFlagIDXsToInitToTrue_Indiv(flagsToInit);
	}	
	protected abstract int[] getFlagIDXsToInitToTrue_Indiv(int[] baseFlags);

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
		switch(UIidx) {
			case gIDX_SceneCols		:{
				sceneCols = ival;
				break;}			
			case gIDX_SceneRows		:{
				sceneRows = ival;
				break;}	
			case gIDX_CurrSceneCLI 	:{
				currSceneName = gIDX_CurrSceneCLIList[ival % gIDX_CurrSceneCLIList.length];
				break;}		
			default : {
				//Check Instance-class ints, if any
				boolean found = setUI_IntValsCustom_Indiv(UIidx, ival, oldVal);
				if(!found) {
					msgObj.dispWarningMessage(className, "setUI_IntValsCustom", "No int-defined gui object mapped to idx :"+UIidx);
				}
				break;}
		}	
	}//setUI_IntValsCustom
	
	/**
	 * Instance-class specific. Called if int-handling guiObjs[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 */
	protected abstract boolean setUI_IntValsCustom_Indiv(int UIidx, int ival, int oldVal);
	
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
		switch(UIidx) {		
			default : {
				//Check Instance-class floats, if any
				boolean found = setUI_FloatValsCustom_Indiv(UIidx, val, oldVal);
				if(!found) {
					msgObj.dispWarningMessage(className, "setUI_FloatValsCustom", "No float-defined gui object mapped to idx :"+UIidx);
				}
				break;}
		}
	}//setUI_FloatValsCustom
	
	/**
	 * Instance-class specific. Called if float-handling guiObjs[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal float value of old data in UIUpdater
	 */
	protected abstract boolean setUI_FloatValsCustom_Indiv(int UIidx, float val, float oldVal);

	/**
	 * Get selected scene name
	 * @return
	 */
	protected final String getCurrSceneName() {
		return gIDX_CurrSceneCLIList[((Base_RayTracerUIUpdater)uiUpdateData).getCurrSceneCliFileIDX() % gIDX_CurrSceneCLIList.length];
	}
	
	/**
	 * 
	 */
	public final Base_Scene getCurrScene() { return loadedScenes.get(currDispSceneName);}

	@Override
	protected final void drawMe(float animTimeMod) {
		drawMe_Indiv(animTimeMod);
	}//drawMe
	protected abstract void drawMe_Indiv(float animTimeMod);

	@Override
	protected final void drawOnScreenStuffPriv(float modAmtMillis) {}	

	@Override
	//draw 2d constructs over 3d area on screen - draws behind left menu section
	//modAmtMillis is in milliseconds
	protected final void drawRightSideInfoBarPriv(float modAmtMillis) {
		pa.pushMatState();
		//display current simulation variables - call sim world through sim exec
		drawRightSideInfoBarPriv_Indiv(modAmtMillis);
		pa.popMatState();					
	}//drawOnScreenStuff
	
	protected abstract void drawRightSideInfoBarPriv_Indiv(float modAmtMillis);	
	
	@Override
	protected final void drawCustMenuObjs() {
		pa.pushMatState();
		//all sub menu drawing within push mat call
		pa.translate(5,custMenuOffset+yOff);
		//draw any custom menu stuff here
		drawCustMenuObjs_Indiv();
		
		pa.popMatState();		
	}//drawCustMenuObjs
	protected abstract void drawCustMenuObjs_Indiv();
	
	
	@Override
	protected final void setVisScreenDimsPriv() {
		setVisScreenDimsPriv_Indiv();
	}
	protected abstract void setVisScreenDimsPriv_Indiv();
	
	@Override
	public final void handleSideMenuMseOvrDispSel(int btn, boolean val) {	}
	
	@Override
	public final void handleSideMenuDebugSelEnable(int btn) {
		msgObj.dispMessage(className, "handleSideMenuDebugSelEnable","Click Debug functionality on in " + name + " : btn : " + btn, MsgCodes.info4);
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
		msgObj.dispMessage(className, "handleSideMenuDebugSelEnable", "End Debug functionality on selection.",MsgCodes.info4);
	}
	
	@Override
	public final void handleSideMenuDebugSelDisable(int btn) {
		msgObj.dispMessage(className, "handleSideMenuDebugSelDisable","Click Debug functionality off in " + name + " : btn : " + btn, MsgCodes.info4);
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
		msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "End Debug functionality off selection.",MsgCodes.info4);
	}

	/**
	 * Load the ray tracer file into a string array
	 * @param fileName
	 * @return
	 */
	public String[] loadRTStrings(String fileName) {
		String[] res = fileIO.loadFileIntoStringAra(fileName, "Successfully loaded CLI file", "Failed to load CLI file");
		return res;
	}
	
	@Override
	protected final void setCameraIndiv(float[] camVals){		
		//, float rx, float ry, float dz are now member variables of every window
		pa.setCameraWinVals(camVals);//(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
		// puts origin of all drawn objects at screen center and moves forward/away by dz
		pa.translate(camVals[0],camVals[1],(float)dz); 
	    setCamOrient();	
	}//setCameraIndiv

	/**
	 * type is row of buttons (1st idx in curCustBtn array) 2nd idx is btn
	 * @param funcRow idx for button row
	 * @param btn idx for button within row (column)
	 * @param label label for this button (for display purposes)
	 */
	@Override
	protected final void launchMenuBtnHndlr(int funcRow, int btn, String label){
		msgObj.dispMessage(className, "launchMenuBtnHndlr", "Begin requested action : Click '" + label +"' (Row:"+(funcRow+1)+"|Col:"+btn+") in " + name, MsgCodes.info4);
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
					break;}
			}	
			break;}//row 1 of menu side bar buttons
		case 1 : {
			msgObj.dispInfoMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 2 | Btn : " + btn);
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
	
	//return strings for directory names and for individual file names that describe the data being saved.  used for screenshots, and potentially other file saving
	//first index is directory suffix - should have identifying tags based on major/archtypical component of sim run
	//2nd index is file name, should have parameters encoded
	@Override
	protected final String[] getSaveFileDirNamesPriv() {
		String dirString="", fileString ="";
		//for(int i=0;i<uiAbbrevList.length;++i) {fileString += uiAbbrevList[i]+"_"+ (uiVals[i] > 1 ? ((int)uiVals[i]) : uiVals[i] < .0001 ? String.format("%6.3e", uiVals[i]) : String.format("%3.3f", uiVals[i]))+"_";}
		return new String[]{dirString,fileString};	
	}
	@Override
	public final void hndlFileLoad(File file, String[] vals, int[] stIdx) {
		//TODO Need to redo this - should load file structure in a hierarchy matching the file system
		
//		//if wanting to load/save UI values, uncomment this call and similar in hndlFileSave 
//		//hndlFileLoad_GUI(vals, stIdx);
//		//loading in grade data from grade file - vals holds array of strings, expected to be comma sep values, for a single class, with student names and grades
//		for(String s : vals) {			msgObj.dispInfoMessage(className,"hndlFileLoad",s);}	
//		String fileName = file.getName();
//		TreeMap<String, String> tmpAra = new TreeMap<String, String>();//, valsToIDX = new TreeMap<String, String>();
//		for(String s : gIDX_CurrSceneCLIList) {tmpAra.put(s, "");	}
//		tmpAra.put(fileName, "");
//		int idx = 0;
//		int fileIDX = 0;
//		for(String s : tmpAra.keySet()){
//			if (s.equals(fileName)) {fileIDX=idx; break;}
//			++idx;
//		}		
//		gIDX_CurrSceneCLIList = tmpAra.keySet().toArray(new String[0]);		
//		curSceneCliFileIDX = fileIDX;
//		this.guiObjs[gIDX_CurrSceneCLI].setNewMax(gIDX_CurrSceneCLIList.length-1);
		
	}//hndlFileLoad
	public abstract void hndlFileLoad_Indiv(File file, String[] vals, int[] stIdx);

	@Override
	public final ArrayList<String> hndlFileSave(File file) {
		ArrayList<String> res = new ArrayList<String>();
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileLoad 
		//res = hndlFileSave_GUI();
		//saving student grades to a file for a single class - vals holds array of strings, expected to be comma sep values, for a single class, with student names and grades
		
		return res;
	}
	
	////////////////////////
	// Mouse Handling
	@Override
	protected final myPoint getMsePtAs3DPt(myPoint mseLoc){return new myPoint(mseLoc.x,mseLoc.y,0);}

	@Override
	protected final boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld) {
		boolean res = chkMouseMoveDragState2D(mouseX, mouseY, -1);
		return res;
	}
	
	@Override
	protected final boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		if(!res) {
			res = chkMouseMoveDragState2D(mouseX, mouseY, mseBtn);
		}		
		return res;}	
	
	protected abstract boolean chkMouseMoveDragState2D(int mouseX, int mouseY, int btn);

	@Override
	protected final boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		boolean res =  chkMouseClick2D(mouseX, mouseY, mseBtn);
		
		return res;}//hndlMouseClickIndiv
	protected abstract boolean chkMouseClick2D(int mouseX, int mouseY, int btn);
	
	@Override
	protected final void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}
	
	@Override
	protected final void hndlMouseRelIndiv() {
		setMouseReleaseState2D();
	}
	protected abstract void setMouseReleaseState2D();

	
	///////////////////////
	// Unused keyboard interaction stuff
	@Override
	protected final void endShiftKeyI() {}
	@Override
	protected final void endAltKeyI() {}
	@Override
	protected final void endCntlKeyI() {}
	@Override
	protected final void setCustMenuBtnLabels() {}	
	
	///////////////////////
	// Unused window display stuff 	
	@Override
	protected final void resizeMe(float scale) {}
	@Override
	protected final void showMe() {}
	@Override
	protected final void closeMe() {}
	@Override
	protected final boolean simMe(float modAmtSec) {		return true;	}	
	@Override
	protected final void stopMe() {msgObj.dispInfoMessage(className,"stopMe","Stop");}	
	
	///////////////////////
	// Unused trajectory stuff
	@Override
	protected final void initDrwnTrajIndiv() {}
	@Override
	public final void processTrajIndiv(DrawnSimpleTraj drawnTraj) {}
	@Override
	protected final void addTrajToScrIndiv(int subScrKey, String newTrajKey) {}
	@Override
	protected final void delTrajToScrIndiv(int subScrKey, String newTrajKey) {}

	///////////////////////
	// Unused sub screen stuff
	@Override
	protected final void addSScrToWinIndiv(int newWinKey) {}
	@Override
	protected final void delSScrToWinIndiv(int idx) {}

}
