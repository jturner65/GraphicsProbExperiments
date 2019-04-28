package base_RayTracer;
/////
///		Final ray tracer from cs7490 - supports distribution RT, acceleration structure(BVH), perlin and worley-noise-based textures, photon mapping
/////

import processing.core.*;

import java.awt.event.KeyEvent;
import java.util.*;


public class DistRayTracer extends PApplet {

	public final int sceneCols = 300;
	public final int sceneRows = 300;
	
	
//	public final int hashPrime1 = 1572869;
//	public final int hashPrime2 = 6291469;
	
	//map holding all loaded scene descriptions - scene should describe all scene and rendering-specific variables and quantities
	public TreeMap<String, myScene> loadedScenes;

	
//	public final float sqrt3 = PApplet.sqrt(3.0f),
//			sqrt66 = PApplet.sqrt(6.0f)/6.0f, 
//			sqrt612 = .5f*sqrt66;

	//name to save the file with, folder to put picture file in, a global variable for holding current active file name.
	public String gCurrentFile;
	public String currentDir;	//current directory to search for cli file
	//file reader/interpreter
	public myRTFileReader rdr; 	
	
	public boolean[] flags;
	//interface flags	
	public final int altKeyPressed  	= 0;			//alt pressed
	public final int cntlKeyPressed  	= 1;			//cntrl pressed
	public final int numFlags = 2;
//	
//	public final double epsVal = .0000001;
		
	
	public static void main(String[] passedArgs) {		
		String[] appletArgs = new String[] { "base_RayTracer.DistRayTracer" };
		    if (passedArgs != null) {
		    	PApplet.main(PApplet.concat(appletArgs, passedArgs));
		    } else {
		    	PApplet.main(appletArgs);
		    }
	}//main

	public void settings(){	size(sceneCols,sceneRows, P3D);	}	
	public void setup() {
		colorMode(RGB, 1.0f);
		background(0, 0, 0);
		initProg();
		//initialize_table();
		gCurrentFile = "";
		currentDir = "";		//TODO set different directories
	}
	
	public void initProg(){	rdr = new myRTFileReader(this);	loadedScenes = new TreeMap<String, myScene>();	initBoolFlags();}//initProg method
	public void draw() {if(!gCurrentFile.equals("")){loadedScenes.get(gCurrentFile).draw();}}
	public void initBoolFlags(){
		flags = new boolean[numFlags];
		for (int i = 0; i < numFlags; ++i) { flags[i] = false;}	
	}		
	
	//print out info at the end of rendering
	public void DispEnd(){
		System.out.println("");
		System.out.println("Image rendered : " +gCurrentFile + " Current directory for cli's : " + getDirName());
		System.out.println("");
	}
	
	//key press IO -ugh.  change to UI
	public void keyPressed() {
		switch(key) {
			case '`' : {if(!gCurrentFile.equals("")){ loadedScenes.get(gCurrentFile).flipNormal();}   break;}//flip poly norms
			case '1':  {gCurrentFile = currentDir + "t01.cli"; break;}		//photon map cli's
			case '2':  {gCurrentFile = currentDir + "t02.cli"; break;}
			case '3':  {gCurrentFile = currentDir + "t03.cli"; break;}
			case '4':  {gCurrentFile = currentDir + "t04.cli"; break;}
			case '5':  {gCurrentFile = currentDir + "t05.cli"; break;}
			case '6':  {gCurrentFile = currentDir + "t06.cli"; break;}
			case '7':  {gCurrentFile = currentDir + "t07.cli"; break;}
			case '8':  {gCurrentFile = currentDir + "t08.cli"; break;}
			case '9':  {gCurrentFile = currentDir + "t09.cli"; break;}
			case '0':  {gCurrentFile = currentDir + "t10.cli"; break;}
			case '-':  {gCurrentFile = currentDir + "t11.cli"; break;}		//cornell box
			
		    case '!':  {gCurrentFile = currentDir + "p4_st01.cli"; break;}		//worley textures on spheres
		    case '@':  {gCurrentFile = currentDir + "p4_st02.cli"; break;}
		    case '#':  {gCurrentFile = currentDir + "p4_st03.cli"; break;}
		    case '$':  {gCurrentFile = currentDir + "p4_st04.cli"; break;}
		    case '%':  {gCurrentFile = currentDir + "p4_st05.cli"; break;}
		    case '^':  {gCurrentFile = currentDir + "p4_st06.cli"; break;}
		    case '&':  {gCurrentFile = currentDir + "p4_st07.cli"; break;}	    
		    case '*':  {gCurrentFile = currentDir + "p4_st08.cli"; break;}		    
		    case 'l':  {gCurrentFile = currentDir + "p4_st09.cli"; break;}		    
		    
		    case ';':  {gCurrentFile = currentDir + "p4_t09.cli"; break;}		    
		    
			case 'O':  {gCurrentFile = currentDir + "p4_t05.cli"; break;}		//wood bunny texture
			case 'P':  {gCurrentFile = currentDir + "p4_t06.cli"; break;}		//marble bunny texture
			case '(':  {gCurrentFile = currentDir + "p4_t07.cli"; break;}		//worley circles bunny texture
			case ')':  {gCurrentFile = currentDir + "p4_t08.cli"; break;}		//crackle bunny texture		

			case '_':  {gCurrentFile = currentDir + "p3_t10.cli"; break;}
			case '=':  {gCurrentFile = currentDir + "plnts3ColsBunnies.cli";  break;}
		    case '+':  {gCurrentFile = currentDir + "p3_t02_sierp.cli"; break;}		//TODO INVESTIGATE THIS
		    
		    case 'A':  {gCurrentFile = currentDir + "p2_t01.cli"; break;}	//these are images from project 2
		    case 'S':  {gCurrentFile = currentDir + "p2_t02.cli"; break;}
		    case 'D':  {gCurrentFile = currentDir + "p2_t03.cli"; break;}
		    case 'F':  {gCurrentFile = currentDir + "p2_t04.cli"; break;}
		    case 'G':  {gCurrentFile = currentDir + "p2_t05.cli"; break;}
		    case 'H':  {gCurrentFile = currentDir + "p2_t06.cli"; break;}
		    case 'J':  {gCurrentFile = currentDir + "p2_t07.cli"; break;}
		    case 'K':  {gCurrentFile = currentDir + "p2_t08.cli"; break;}
		    case 'L':  {gCurrentFile = currentDir + "p2_t09.cli"; break;}
		    case ':':  {gCurrentFile = currentDir + "old_t07c.cli"; break;}
		    
			case 'a':  {gCurrentFile = currentDir + "earthAA1.cli";   break;}
			case 's':  {gCurrentFile = currentDir + "earthAA2.cli";   break;}
			case 'd':  {gCurrentFile = currentDir + "earthAA3.cli";   break;}
			case 'f':  {gCurrentFile = currentDir + "c2clear.cli";   break;}
			case 'g':  {gCurrentFile = currentDir + "c3shinyBall.cli";   break;}
			case 'h':  {gCurrentFile = currentDir + "c4InSphere.cli";   break;}
			case 'j':  {gCurrentFile = currentDir + "c6.cli";   break;}
			case 'k':  {gCurrentFile = currentDir + "c6Fish.cli";   break;}	
			
		    case 'Q':  {gCurrentFile = currentDir + "c2torus.cli"; break;}			    
		    case 'W':  {gCurrentFile = currentDir + "old_t02.cli"; break;}//this is the most recent block of images for project 1b
		    case 'E':  {gCurrentFile = currentDir + "old_t03.cli"; break;}
		    case 'R':  {gCurrentFile = currentDir + "old_t04.cli"; break;}
		    case 'T':  {gCurrentFile = currentDir + "old_t05.cli"; break;}
		    case 'Y':  {gCurrentFile = currentDir + "old_t06.cli"; break;}
		    case 'U':  {gCurrentFile = currentDir + "old_t07.cli"; break;}
		    case 'I':  {gCurrentFile = currentDir + "old_t08.cli"; break;}
			case '{':  {gCurrentFile = currentDir + "old_t09.cli"; break;}
			case '}':  {gCurrentFile = currentDir + "old_t10.cli"; break;}
			
			case 'q':  {gCurrentFile = currentDir + "planets.cli"; break; }
			case 'w':  {gCurrentFile = currentDir + "planets2.cli"; break;}
			case 'e':  {gCurrentFile = currentDir + "planets3.cli"; break;}
			case 'r':  {gCurrentFile = currentDir + "planets3columns.cli";   break;}
			case 't' : {gCurrentFile = currentDir + "trTrans.cli";   break;}
			case 'y':  {gCurrentFile = currentDir + "planets3Ortho.cli"; break;}			
			case 'u':  {gCurrentFile = currentDir + "c1.cli";  break;}
			case 'i':  {gCurrentFile = currentDir + "c2.cli";  break;}
			case 'o':  {gCurrentFile = currentDir + "c3.cli";  break;}
			case 'p':  {gCurrentFile = currentDir + "c4.cli";  break;}
			case '[':  {gCurrentFile = currentDir + "c5.cli";  break;}
			case ']':  {gCurrentFile = currentDir + "c0.cli";  break;}
		    
			case 'Z':  {gCurrentFile = currentDir + "p3_t01.cli"; break;}
			case 'X':  {gCurrentFile = currentDir + "p3_t02.cli"; break;}
			case 'C':  {gCurrentFile = currentDir + "p3_t03.cli"; break;}
			case 'V':  {gCurrentFile = currentDir + "p3_t04.cli"; break;}
			case 'B':  {gCurrentFile = currentDir + "p3_t05.cli"; break;}
			case 'N':  {gCurrentFile = currentDir + "p3_t06.cli"; break;}
			case 'M':  {gCurrentFile = currentDir + "p3_t07.cli"; break;}
			case '<':  {gCurrentFile = currentDir + "p4_t06_2.cli"; break;}
			case '>':  {gCurrentFile = currentDir + "p4_t09.cli"; break;}
			case '?':  {gCurrentFile = currentDir + "p3_t11_sierp.cli"; break;}		//my bunny scene		
			case 'z':  {gCurrentFile = currentDir + "cylinder1.cli";  break;}
			case 'x':  {gCurrentFile = currentDir + "tr0.cli";   break;}
			case 'c':  {gCurrentFile = currentDir + "c0Square.cli";  break;}
			case 'v':  {gCurrentFile = currentDir + "c1octo.cli";  break;}
			case 'b':  {gCurrentFile = currentDir + "old_t0rotate.cli";  break;}
		    case 'n':  {gCurrentFile = currentDir + "old_t03a.cli"; break;}	//this block contains the first set of images given with assignment 1b 
		    case 'm':  {gCurrentFile = currentDir + "old_t04a.cli"; break;}
		    case ',':  {gCurrentFile = currentDir + "old_t05a.cli"; break;}
		    case '.':  {gCurrentFile = currentDir + "old_t06a.cli"; break;}
		    case '/':  {gCurrentFile = currentDir + "old_t07a.cli"; break;}			
			default : {return;}
		}//switch
		if(!gCurrentFile.equals("")){
			rdr.readRTFile(gCurrentFile, null);//pass null as scene so that we don't add to an existing scene
		}
		if((!flags[altKeyPressed])&&(key==CODED)){setFlags(altKeyPressed,(keyCode  == KeyEvent.VK_ALT));}
		if((!flags[cntlKeyPressed])&&(key==CODED)){setFlags(cntlKeyPressed,(keyCode  == KeyEvent.VK_CONTROL));}
	}	
	
	public void keyReleased(){
		if((flags[altKeyPressed])&&(key==CODED)){ if(keyCode == KeyEvent.VK_ALT){endAltKey();}}
		if((flags[cntlKeyPressed])&&(key==CODED)){ if(keyCode == KeyEvent.VK_CONTROL){endCntlKey();}}
	}		
	public void endAltKey(){clearFlags(new int []{altKeyPressed});	}
	public void endCntlKey(){clearFlags(new int []{cntlKeyPressed});}
	
	public void setFlags(int idx, boolean val ){
		flags[idx] = val;
		switch (idx){
			case altKeyPressed 		: { break;}//anything special for altKeyPressed 	
			case cntlKeyPressed 		: { break;}//anything special for altKeyPressed 	
		}
	}
	public void clearFlags(int[] idxs){		for(int idx : idxs){flags[idx]=false;}	}			
	
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
}//DistRayTracer class
