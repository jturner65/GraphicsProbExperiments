package base_RayTracer;
/////
///		Final ray tracer from cs7490 - supports distribution RT, acceleration structure(BVH), perlin and worley-noise-based textures, photon mapping
/////

import processing.core.*;

import java.io.File;
import java.util.*;

import base_RayTracer.scene.myScene;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.my_procApplet;

//TODO this needs to instance a window - cannot use this method anymore with just the single processing window

//this file can act as a stub for the ray tracer and can launch it
public class DistRayTracer extends GUI_AppManager {
	//project-specific variables
	public String prjNmLong = "Testbed for base ray tracer", prjNmShrt = "RayTracerBaseExp";

	private final int sceneCols = 300;
	private final int sceneRows = 300;
	
	//map holding all loaded scene descriptions - scene should describe all scene and rendering-specific variables and quantities
	public TreeMap<String, myScene> loadedScenes;


	//name to save the file with, folder to put picture file in, a global variable for holding current active file name.
	public String gCurrentFile;
	private String currentDir;	//current directory to search for cli file
	//file reader/interpreter
	private myRTFileReader rdr; 	
		
	public static void main(String[] passedArgs) {		
		DistRayTracer me = new DistRayTracer();
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
	
	public void setup_Indiv() {
		((my_procApplet)pa).colorMode(PConstants.RGB, 1.0f);
		((my_procApplet)pa).background(0, 0, 0);
		//initialize_table();
		gCurrentFile = "t01.cli";
		currentDir = "";		//TODO set different directories
	}
	
	
	/**
	 * determine which main flags to show at upper left of menu 
	 */
	@Override
	protected void initMainFlags_Indiv() {
		setMainFlagToShow_debugMode(true);
		setMainFlagToShow_saveAnim(true); 
		setMainFlagToShow_runSim(true);
		setMainFlagToShow_singleStep(true);
		setMainFlagToShow_showRtSideMenu(true);
	}

	@Override
	protected void setBkgrnd() {((my_procApplet)pa).background(0, 0, 0);}
	@Override
	/**
	 * Overriding main because handling 2d + 3d windows
	 */
	
	public final void drawMePost_Indiv(float modAmtMillis, boolean is3DDraw){if(!gCurrentFile.equals("")){loadedScenes.get(gCurrentFile).draw();}}
	@Override
	protected String getPrjNmLong() {return prjNmLong;}
	@Override
	protected String getPrjNmShrt() {return prjNmShrt;}

	/**
	 * handle key pressed
	 * @param keyVal 0-9, with or without shift ((keyCode>=48) && (keyCode <=57))
	 * @param keyCode actual code of key having been pressed
	 */
	protected void handleKeyPress(char key, int keyCode) {
		switch(key) {
			case '`' : {if(!gCurrentFile.equals("")){ loadedScenes.get(gCurrentFile).flipNormal();}   break;}//flip poly norms
			case '1':  {gCurrentFile = currentDir + "t01.cli";      loadFile();       break;}		//photon map cli's
			case '2':  {gCurrentFile = currentDir + "t02.cli";      loadFile();       break;}
			case '3':  {gCurrentFile = currentDir + "t03.cli";      loadFile();       break;}
			case '4':  {gCurrentFile = currentDir + "t04.cli";      loadFile();       break;}
			case '5':  {gCurrentFile = currentDir + "t05.cli";      loadFile();       break;}
			case '6':  {gCurrentFile = currentDir + "t06.cli";      loadFile();       break;}
			case '7':  {gCurrentFile = currentDir + "t07.cli";      loadFile();       break;}
			case '8':  {gCurrentFile = currentDir + "t08.cli";      loadFile();       break;}
			case '9':  {gCurrentFile = currentDir + "t09.cli";      loadFile();       break;}
			case '0':  {gCurrentFile = currentDir + "t10.cli";      loadFile();       break;}
			case '-':  {gCurrentFile = currentDir + "t11.cli";      loadFile();       break;}		//cornell box			
		    case '!':  {gCurrentFile = currentDir + "p4_st01.cli";  loadFile();       break;}		//worley textures on spheres
		    case '@':  {gCurrentFile = currentDir + "p4_st02.cli";  loadFile();       break;}
		    case '#':  {gCurrentFile = currentDir + "p4_st03.cli";  loadFile();       break;}
		    case '$':  {gCurrentFile = currentDir + "p4_st04.cli";  loadFile();       break;}
		    case '%':  {gCurrentFile = currentDir + "p4_st05.cli";  loadFile();       break;}
		    case '^':  {gCurrentFile = currentDir + "p4_st06.cli";  loadFile();       break;}
		    case '&':  {gCurrentFile = currentDir + "p4_st07.cli";  loadFile();       break;}	    
		    case '*':  {gCurrentFile = currentDir + "p4_st08.cli";  loadFile();       break;}		    
		    case 'l':  {gCurrentFile = currentDir + "p4_st09.cli";  loadFile();       break;}	    
		    case ';':  {gCurrentFile = currentDir + "p4_t09.cli";   loadFile();       break;}		    
			case 'O':  {gCurrentFile = currentDir + "p4_t05.cli";   loadFile();       break;}		//wood bunny texture
			case 'P':  {gCurrentFile = currentDir + "p4_t06.cli";   loadFile();       break;}		//marble bunny texture
			case '(':  {gCurrentFile = currentDir + "p4_t07.cli";   loadFile();       break;}		//worley circles bunny texture
			case ')':  {gCurrentFile = currentDir + "p4_t08.cli";   loadFile();       break;}		//crackle bunny texture		
			case '_':  {gCurrentFile = currentDir + "p3_t10.cli"; 	loadFile();		break;}
			case '=':  {gCurrentFile = currentDir + "plnts3ColsBunnies.cli"; loadFile(); break;}
		    case '+':  {gCurrentFile = currentDir + "p3_t02_sierp.cli"; loadFile();	break;}		//TODO INVESTIGATE THIS		    
		    case 'A':  {gCurrentFile = currentDir + "p2_t01.cli"; 		loadFile();	break;}	//these are images from project 2
		    case 'S':  {gCurrentFile = currentDir + "p2_t02.cli"; 		loadFile();	break;}
		    case 'D':  {gCurrentFile = currentDir + "p2_t03.cli"; 		loadFile();	break;}
		    case 'F':  {gCurrentFile = currentDir + "p2_t04.cli"; 		loadFile();	break;}
		    case 'G':  {gCurrentFile = currentDir + "p2_t05.cli"; 		loadFile();	break;}
		    case 'H':  {gCurrentFile = currentDir + "p2_t06.cli"; 		loadFile();	break;}
		    case 'J':  {gCurrentFile = currentDir + "p2_t07.cli"; 		loadFile();	break;}
		    case 'K':  {gCurrentFile = currentDir + "p2_t08.cli"; 		loadFile();	break;}
		    case 'L':  {gCurrentFile = currentDir + "p2_t09.cli"; 		loadFile();	break;}
		    case ':':  {gCurrentFile = currentDir + "old_t07c.cli"; 	loadFile();	break;}
			case 'a':  {gCurrentFile = currentDir + "earthAA1.cli";   	loadFile();	break;}
			case 's':  {gCurrentFile = currentDir + "earthAA2.cli";   	loadFile();	break;}
			case 'd':  {gCurrentFile = currentDir + "earthAA3.cli";   	loadFile();	break;}
			case 'f':  {gCurrentFile = currentDir + "c2clear.cli";   	loadFile();	break;}
			case 'g':  {gCurrentFile = currentDir + "c3shinyBall.cli";  loadFile(); 	break;}
			case 'h':  {gCurrentFile = currentDir + "c4InSphere.cli";   loadFile();	break;}
			case 'j':  {gCurrentFile = currentDir + "c6.cli";   		loadFile();	break;}
			case 'k':  {gCurrentFile = currentDir + "c6Fish.cli";   	loadFile();	break;}				
		    case 'Q':  {gCurrentFile = currentDir + "c2torus.cli"; 		loadFile();	break;}			    
		    case 'W':  {gCurrentFile = currentDir + "old_t02.cli"; 		loadFile();	break;}//this is the most recent block of images for project 1b
		    case 'E':  {gCurrentFile = currentDir + "old_t03.cli"; 		loadFile();	break;}
		    case 'R':  {gCurrentFile = currentDir + "old_t04.cli"; 		loadFile();	break;}
		    case 'T':  {gCurrentFile = currentDir + "old_t05.cli"; 		loadFile();	break;}
		    case 'Y':  {gCurrentFile = currentDir + "old_t06.cli"; 		loadFile();	break;}
		    case 'U':  {gCurrentFile = currentDir + "old_t07.cli"; 		loadFile();	break;}
		    case 'I':  {gCurrentFile = currentDir + "old_t08.cli"; 		loadFile();	break;}
			case '{':  {gCurrentFile = currentDir + "old_t09.cli"; 		loadFile();	break;}
			case '}':  {gCurrentFile = currentDir + "old_t10.cli"; 		loadFile();	break;}			
			case 'q':  {gCurrentFile = currentDir + "planets.cli"; 		loadFile();	break; }
			case 'w':  {gCurrentFile = currentDir + "planets2.cli"; 	loadFile();	break;}
			case 'e':  {gCurrentFile = currentDir + "planets3.cli"; 	loadFile();	break;}
			case 'r':  {gCurrentFile = currentDir + "planets3columns.cli";loadFile();   break;}
			case 't' : {gCurrentFile = currentDir + "trTrans.cli";   	loadFile();	break;}
			case 'y':  {gCurrentFile = currentDir + "planets3Ortho.cli";loadFile(); 	break;}			
			case 'u':  {gCurrentFile = currentDir + "c1.cli";  			loadFile();	break;}
			case 'i':  {gCurrentFile = currentDir + "c2.cli";  			loadFile();	break;}
			case 'o':  {gCurrentFile = currentDir + "c3.cli";  			loadFile();	break;}
			case 'p':  {gCurrentFile = currentDir + "c4.cli";  			loadFile();	break;}
			case '[':  {gCurrentFile = currentDir + "c5.cli";  			loadFile();	break;}
			case ']':  {gCurrentFile = currentDir + "c0.cli";  			loadFile();	break;}		    
			case 'Z':  {gCurrentFile = currentDir + "p3_t01.cli"; 		loadFile();	break;}
			case 'X':  {gCurrentFile = currentDir + "p3_t02.cli"; 		loadFile();	break;}
			case 'C':  {gCurrentFile = currentDir + "p3_t03.cli"; 		loadFile();	break;}
			case 'V':  {gCurrentFile = currentDir + "p3_t04.cli"; 		loadFile();	break;}
			case 'B':  {gCurrentFile = currentDir + "p3_t05.cli"; 		loadFile();	break;}
			case 'N':  {gCurrentFile = currentDir + "p3_t06.cli"; 		loadFile();	break;}
			case 'M':  {gCurrentFile = currentDir + "p3_t07.cli"; 		loadFile();	break;}
			case '<':  {gCurrentFile = currentDir + "p4_t06_2.cli"; 	loadFile();	break;}
			case '>':  {gCurrentFile = currentDir + "p4_t09.cli"; 		loadFile();	break;}
			case '?':  {gCurrentFile = currentDir + "p3_t11_sierp.cli"; loadFile();		break;}		//my bunny scene		
			case 'z':  {gCurrentFile = currentDir + "cylinder1.cli";  	loadFile();	break;}
			case 'x':  {gCurrentFile = currentDir + "tr0.cli";   		loadFile();	break;}
			case 'c':  {gCurrentFile = currentDir + "c0Square.cli";  	loadFile();	break;}
			case 'v':  {gCurrentFile = currentDir + "c1octo.cli";  		loadFile();	break;}
			case 'b':  {gCurrentFile = currentDir + "old_t0rotate.cli"; loadFile(); 	break;}
		    case 'n':  {gCurrentFile = currentDir + "old_t03a.cli"; 	loadFile();	break;}	//this block contains the first set of images given with assignment 1b 
		    case 'm':  {gCurrentFile = currentDir + "old_t04a.cli"; 	loadFile();	break;}
		    case ',':  {gCurrentFile = currentDir + "old_t05a.cli"; 	loadFile();	break;}
		    case '.':  {gCurrentFile = currentDir + "old_t06a.cli"; 	loadFile();	break;}
		    case '/':  {gCurrentFile = currentDir + "old_t07a.cli"; 	loadFile();	break;}			
			default : {return;}
		}//switch		
	}//handleKeyPress
	
	private void loadFile() {
		if(!gCurrentFile.equals("")){
			myScene tmp = rdr.readRTFile(loadedScenes, gCurrentFile, null, sceneCols, sceneRows);//pass null as scene so that we don't add to an existing scene
			if(null==tmp) {gCurrentFile = "";}
		}		
	}
	
	public String getDirName(){	if(currentDir.equals("")){	return "data/";}	return "data/"+currentDir;}
	public void setCurDir(int input){
		switch (input){
		case 0 :{currentDir = "";break;}
		case 1 :{currentDir = "old/";break;}
		case 2 :{currentDir = "project1_1/";break;}
		case 3 :{currentDir = "project1_2/";break;}
		case 4 :{currentDir = "project2_1/";break;}
		case 5 :{currentDir = "project2_2/";break;}
		case 6 :{currentDir = "project3/";break;}
		case 7 :{currentDir = "project4/";break;}
		case 8 :{currentDir = "project5/";break;}
		default :{currentDir = "";break;}
		}
	}//setCurDir

	@Override
	protected void initVisOnce_Indiv() {}
	@Override
	protected void initOnce_Indiv() {}
	@Override
	protected void initVisProg_Indiv() {}
	@Override
	protected void initProgram_Indiv() {
		rdr = new myRTFileReader((my_procApplet)pa,".."+File.separator+"data"+File.separator+"txtrs"+File.separator);	
		loadedScenes = new TreeMap<String, myScene>();	
		myScene tmp = rdr.readRTFile(loadedScenes, gCurrentFile, null, sceneCols, sceneRows);//pass null as scene so that we don't add to an existing scene
		//returns null means not found
		if(null==tmp) {gCurrentFile = "";}
	}

	//////////////////////////////////////////
	/// graphics and base functionality utilities and variables
	//////////////////////////////////////////
	
	/**
	 * return the number of visible window flags for this application
	 * @return
	 */
	@Override
	public int getNumVisFlags() {return 1;}
	@Override
	//address all flag-setting here, so that if any special cases need to be addressed they can be
	protected void setVisFlag_Indiv(int idx, boolean val ){}
	@Override
	public double clickValModMult() {return 0;}
	@Override
	public boolean isClickModUIVal() {return false;}
	@Override
	public float[] getUIRectVals(int idx) {return null;}
	@Override
	public void handleShowWin(int btn, int val, boolean callFlags) {}
	
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
	public String[] getMouseOverSelBtnNames() {
		// TODO Auto-generated method stub
		return new String[0];
	}

	@Override
	protected void setSmoothing() {		pa.setSmoothing(0);		}


}//DistRayTracer class
