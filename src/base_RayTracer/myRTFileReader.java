package base_RayTracer;

import java.util.*;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_RayTracer.scene.myFOVScene;
import base_RayTracer.scene.myFishEyeScene;
import base_RayTracer.scene.myOrthoScene;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.geometry.sceneObjects.mySceneObject;
import base_RayTracer.scene.geometry.sceneObjects.implicit.mySphere;
import base_RayTracer.scene.geometry.sceneObjects.planar.myPlanarObject;
import base_RayTracer.scene.geometry.sceneObjects.planar.myQuad;
import base_RayTracer.scene.geometry.sceneObjects.planar.myTriangle;
import base_RayTracer.scene.textures.myImageTexture;
import base_UI_Objects.my_procApplet;
import processing.core.PApplet;

public class myRTFileReader {
	public IRenderInterface p;
	public final String textureDir;
	private int timer;
	private int curNumRows, curNumCols;
	public myRTFileReader(IRenderInterface _p, String _txtrDirs) {
		p = _p;
		textureDir = _txtrDirs;		
	}
	
	//passed scene is for when called recursively - is null on first time in, passes scene to be used otherwise
	public myScene readRTFile(TreeMap<String, myScene> loadedScenes, String fileName, myScene _scene, int _numCols, int _numRows) {		  
		//build individual scene for each file		
		timer = ((my_procApplet)p).millis();			//times rendering
		curNumRows = _numRows;
		curNumCols = _numCols;
		
		myScene scene = null;
		boolean isMainFileScene = true;			//whether this is primary scene's file or secondary/recursive read call; whether scene has been named and finalized yet
		if(_scene != null){//recursively being passed into readRTFile - will have different file name
			scene =  _scene;				
			isMainFileScene = false;			//set this so that scene info is not put into
		} else {
			scene = loadedScenes.get(fileName);
			if(scene != null){return scene;}//if scene exists, don't read in again
			scene = new myFOVScene(p,fileName,_numCols, _numRows);
			scene.setSceneParams(new double[]{60}); 	//set default scene to be FOV with width 60 degrees
		}
		String[] str = null;
		try{
			str = ((my_procApplet)p).loadStrings("../data/"+fileName);
			System.out.println("File name : " + fileName + " Size : " + str.length);
		} catch (Exception e) {	System.out.println("myRTFileReader::readRTFile : File Read Error : File name : " + fileName + " not found."); return null;		}	 
	
		scene = parseStringArray(loadedScenes, str, scene, isMainFileScene, fileName);
		return scene;
	}//interpreter method
	
	
	//build the objects in a scene
	public myScene parseStringArray(TreeMap<String, myScene> loadedScenes, String[] fileStrings, myScene scene, boolean isMainFileScene, String fileName) {
		boolean finalized = false;
		String vertType = "triangle";    		//assume default object type is triangle
		int myVertCount = 0;		
		//temp objects intended to hold 
		mySceneObject myPoly = null;
		int curNumRaysPerPxl = scene.numRaysPerPixel;
		//reinitializes the image so that any previous values from other images are not saved
		//if (str == null) {System.out.println("Error! Failed to read the file.");}
		for (int i=0; i<fileStrings.length; ++i) { 
			//System.out.println("Line " + i + " : " + str[i]);
			String[] token = PApplet.splitTokens(fileStrings[i], " "); // Get a line and parse tokens.
			if ((token.length == 0) || (token[0].startsWith("#"))) continue; // Skip blank line or comments.
			switch (token[0]){
			//determine the kind of scene - needs to be the first component in base scene file
				case "fov" 	: {	
					if(!isMainFileScene){System.out.println("Error - unsupported setting scene type ('FOV') in recursive child scene file"); break;}
					myScene tmp = new myFOVScene(scene);
					scene = tmp;
					//scene = new myFOVScene(p);
					scene.setNumRaysPerPxl((curNumRaysPerPxl != 0) ? curNumRaysPerPxl : 1);
					scene.setSceneParams(new double[]{Double.parseDouble(token[1])}); 
					break;}	
				   
			    case "lens" : { 			//for depth of field -only in FOV scenes - specifies the lens size (radius) and what distance in front of the eye is in focus - greater radius should blur more of the image
			    	double radius = Double.parseDouble(token[1]);
			    	double focal_distance = Double.parseDouble(token[2]);
			    	scene.setDpthOfFld(radius, focal_distance);			    	
			    	break;}
			    
				case "fishEye" :
				case "fisheye" : { 
					if(!isMainFileScene){System.out.println("Error - unsupported setting scene type ('fishEye') in recursive child scene file"); break;}					
					myScene tmp = new myFishEyeScene(scene);
					scene = tmp;
					//scene = new myFishEyeScene(p);
					scene.setNumRaysPerPxl((curNumRaysPerPxl != 0) ? curNumRaysPerPxl : 1);
					scene.setSceneParams(new double[]{Double.parseDouble(token[1])}); 
					break;}	
				case "ortho" :
				case "orthographic" : {// width height
					if(!isMainFileScene){System.out.println("Error - unsupported setting scene type ('Ortho') in recursive child scene file"); break;}
					myScene tmp = new myOrthoScene(scene,curNumCols, curNumRows);
					scene = tmp;
					//scene = new myOrthoScene(p);
					scene.setNumRaysPerPxl((curNumRaysPerPxl != 0) ? curNumRaysPerPxl : 1);
					scene.setSceneParams(new double[]{Double.parseDouble(token[1]),Double.parseDouble(token[2])}); 
					break;}				
				//file save and read subordinate file
				//NEEDS TO RENDER SCENE and then save it so that rendering can be timed - doesn't work with refine currently
			    case "write" : {		
			    	scene.saveName = token[1];			    	
			    	finalizeScene(loadedScenes,fileName, scene);		
			    	finalized = true;
			    	//render scene -here- - needs to be modified to not stop until scene is finished rendering (for incremental/refine scene renders)
			    	if(!fileName.equals("")){scene.draw();}
			    	else{System.out.println("Can't render unknown/incomplete scene");}
			    	break;}		
			    case "read" : {//read another scene file - nested to load multiple files
			    	readRTFile(loadedScenes,token[1],scene,curNumCols, curNumRows);
			    	break;}		
		    //timer stuff
			    case "reset_timer" : {//Reset a global timer that will be used to determine how long it takes to render a scene. 
			    	timer = ((my_procApplet)p).millis();
			    	break;
			    }
			    case "print_timer" : {//Print the time elapsed since reset_timer was called (in seconds). Use the code snippet 
			    	int new_timer = ((my_procApplet)p).millis();
			    	int diff = new_timer - timer;
			    	float seconds = diff / 1000.0f;
			    	scene.renderTime = seconds;
			    	System.out.println ("timer = " + seconds);
			    	break;
			    }			    
			//global modifications to alg
			    case "refine" : {    	scene.setRefine(token[1]); break;}//user iterative refinement when rendering scene - start at (int)log2 of dim, then decrease by 2 every iteration			    

			    case "rays_per_pixel" : {	//how many rays should be shot per pixel.
			    	int rays = Integer.parseInt(token[1]);
			    	System.out.println("Num Rays Per Pixel : " + rays);
			    	curNumRaysPerPxl = rays;				//doing this because it is being set before FOV, which rebuilds scene
			    	scene.setNumRaysPerPxl(rays);			    	
			    	break;}			
			    
				case "antialias" : {
					int aaDepthRow = Integer.parseInt(token[1]);
					int aaDepthCol = Integer.parseInt(token[2]);
					int prod = aaDepthRow * aaDepthCol;
					System.out.println("aa depth r/c " + aaDepthRow + "|" + aaDepthCol +" -> convert to numRays per pixel " + prod);
			    	curNumRaysPerPxl = prod;				//doing this because it is being set before FOV, which rebuilds scene
			    	scene.setNumRaysPerPxl(prod);			    	
					break;} 			    
			//background texture/color/foreground color
				case "background" : {//visible background in front of camera - negative infinite z\
					if (token[1].equals("texture")) {
						//load texture to be used for background
						String textureName = token[2];
						scene.currBkgTexture = ((my_procApplet)p).loadImage(textureDir+textureName);
						scene.scFlags[myScene.glblTxtrdBkgIDX] = true;
						System.out.println("Background texture loaded");
						//build "skydome" - textured sphere encircling scene
						double rad = Double.parseDouble(token[3]);
						double xC = Double.parseDouble(token[4]);
						double yC = Double.parseDouble(token[5]);
						double zC = Double.parseDouble(token[6]);
						System.out.println("Skydome : rad:" + rad +" ctr : ["+ xC +","+ yC +","+ zC+"]");
						scene.mySkyDome = new mySphere(scene,rad,xC,yC,zC,true);
						((myImageTexture)scene.mySkyDome.shdr.txtr).setMyTextureBottom(scene.currBkgTexture);	
					} else {//set bkg color
						scene.setBackgroundColor(Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]));
						scene.txtrType = 0;
					}
					break;}
//				case "foreground" : {//visible foreground behind camera - positive infinite z - rgb - modify this to also allow for textures as in background, but mapped to billboard behind viewer?
//					scene.setForegroundColor(Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]));
//					break;}				
			//lights
				case "point_light" : {
					scene.addMyPointLight(token);	      
					break;}
				case "spotlight" : {//spotlight x y z dx dy dz angle_inner angle_outer r g b
					System.out.println("File : " + fileName+ " has a spotLight!");
					scene.addMySpotLight(token);   
					break;}				
				case "disk_light" : {//disk_light x y z radius dx dy dz r g b
					scene.addMyDiskLight(token);
					break;}	

				case "caustic_photons" : //caustic_photons num_cast num_near max_near_dist
				case "diffuse_photons" : {//diffuse_photons num_cast num_near max_near_dist 
					scene.setPhotonHandling(token);
					break;}
				//
//				final_gather num_rays
//				This command indicates how the diffuse photons for global illumination will be used to create 
//				the final image. If num_rays is set to zero, then your renderer should directly use the diffuse 
//				photons stored on each surface. This is quite similar to how you render using caustic photons. 
//				This should be fairly fast, but unfortunately this will create noisy images. If num_rays is non-zero, 
//				then you will estimate the indirect illumination using a "final gather" step. To calculate the indirect 
//				illumination at a surface point, you will shoot num_rays rays in random directions on the hemisphere 
//				surrounding this point. You will then query the kD-tree at each surface that such a ray hit, and use 
//				this to determine how much light should reach the point in question from this surface. This will produce 
//				much better images, but will also be significantly slower. Speeding this up would require irradiance caching, 
//				but we will not implement this due to lack of time. 
				case "final_gather" : {//final_gather num_rays
					int numRays = Integer.parseInt(token[1]);
					//
					break;}
			
			//color commands
				case "diffuse" : {//new assignment requirements
					myColor cDiff = readColor(token,1);//new myColor(Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]));
					myColor cAmb = readColor(token,4);//new myColor(Double.parseDouble(token[4]),Double.parseDouble(token[5]),Double.parseDouble(token[6]));
					myColor cSpec = new myColor(0,0,0);
					scene.scFlags[myScene.glblTxtrdTopIDX]  = false;
					scene.scFlags[myScene.glblTxtrdBtmIDX] = false;
					scene.setSurface(cDiff,cAmb,cSpec,0,0);					
					break;}
				//use shiny for new refr/refl; use surface for older cli files - handles mix of colors and refr/refl better
				case "shiny" :{//Cdr Cdg Cdb, Car Cag Cab, Csr Csg Csb, Exp Krefl Ktrans Index 
					setSurfaceShiny(scene, token, true);
					break;}
				case "surface" : {
					setSurfaceShiny(scene, token, false);
					break;}
				//reflective Cdr Cdg Cdb Car Cag Cab k_refl 
//				This command describes a new kind of surface material. Just as with the 
				//diffuse command, this command defines the diffuse and ambient color components of a surface. 
				//In addition, however, it also defines a reflectance coefficient k_refl. This value should be in the 
				//range of zero to one. When it is non-zero, k_refl indicates how much of the light that strikes the 
				//surface is to be reflected. Eye rays that hit such a reflective surface should spawn a new reflective ray, 
				//and this reflective ray should contribute to the surface's color. Moreover, if a caustic photon hits such 
				//a reflective surface, this should cause the caustic photon to "bounce", and to travel in a new direction. 
				//Caustic photons only stop bouncing when they hit a diffuse surface. 				
				case "reflective" : {//reflective Cdr Cdg Cdb Car Cag Cab k_refl 
					myColor cDiff = readColor(token,1);//new myColor(Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]));
					myColor cAmb = readColor(token,4);//new myColor(Double.parseDouble(token[4]),Double.parseDouble(token[5]),Double.parseDouble(token[6]));
					myColor cSpec = new myColor(0,0,0);
					scene.scFlags[myScene.glblTxtrdTopIDX]  = false;
					scene.scFlags[myScene.glblTxtrdBtmIDX] = false;
					double kRefl = Double.parseDouble(token[7]);
					scene.setSurface(cDiff,cAmb,cSpec,0,kRefl);		
					break;}
				
			    case "perm" : {//load new permiability value - used for refraction
			    	scene.setRfrIdx(Double.parseDouble(token[1]));
			    	try {scene.setRfrIdx(Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]),Double.parseDouble(token[4]));} catch (Exception e){}
			    	break;}//if perm
			    case "phong" : {scene.setPhong(Double.parseDouble(token[1]));	break;}//phong
			    case "krefl" : {scene.setKRefl(Double.parseDouble(token[1]));  	break;}//krefl
				case "depth" : {scene.setDepth(Double.parseDouble(token[1]));	break;}//depth for subsurface scattering - TODO
			    case "ktrans" : {scene.setKTrans(Double.parseDouble(token[1]));	break;}//ktrans - //load new permiability value - used for refraction		
			    
			    //accel structs
			    case "begin_list" :{	    	scene.startTmpObjList();		    break;}
			    case "end_list" 	:{	    	scene.endTmpObjList(0);		    	break;}
			    case "end_accel" 	:{	    	scene.endTmpObjList(1);		    	break;}			//TODO modify to accept multiple accel struct types		 
			    		    
			    //layouts
			    case "sierpinski" :{
			    	//generate sierpinski tet arrangement of named object
			    	String objName = token[1], useShdr="No";
			    	float scale = .5f;
			    	int depth = 5;		//default to 5 layers deep	    	
			    	try {depth = Integer.parseInt(token[2]);scale = Float.parseFloat(token[3]);	useShdr = token[4];}catch (Exception e) {}	 
			    	scene.buildSierpinski(objName, scale, depth, useShdr != "No" );
			    	break;}
			    
			    //instance creation
			    case "named_object" : {
			    	//named_object <name>		    	
			    	String objName = token[1];
			    	scene.setObjectAsNamedObject(objName);			    	
			    	break;}
			    	//instance <name>
			    case "instance" : {
			    	String objName = token[1];
			    	String useCurShdr = "";				//whether to use the currently set shader for this instance, instead of the default shader for the named object
			    	try {	    		useCurShdr = token[2];			    	} catch (Exception e){	        			    	}			    	
			    	scene.addInstance(objName, useCurShdr != "");		    	
			    	break;}	
			    
			    case "image_texture" :
			    case "texture" : {//load texture to be used for subsequent objects.  will be overridden by a surface command			    	
			    	String side = token[1];
			    	String textureName = token[1];
			    	if (side.toLowerCase().equals("top") || !side.toLowerCase().equals("bottom")){
			    		//if not specified then assume texture goes on top
			    		if (side.toLowerCase().equals("top")){  		  	textureName = token[2];   } 
			    		else {								         		textureName = token[1];   }
			    		scene.currTextureTop = ((my_procApplet)p).loadImage("..\\data\\"+textureDir+"\\"+textureName);
			    		scene.scFlags[myScene.glblTxtrdTopIDX] = true;
			    		System.out.println("top surface texture loaded");
			    	} else if (side.toLowerCase().equals("bottom")){
			    		scene.currTextureBottom = ((my_procApplet)p).loadImage("..\\data\\"+textureDir+"\\"+textureName);
			    		scene.scFlags[myScene.glblTxtrdBtmIDX] = true;
			    		System.out.println("bottom surface texture loaded");      }
			    	scene.txtrType = 1;		//texture type is set to image/none
			    	break;}			    
			    
			    //procedural textures
			    //noise as txtr -> token[1] is scale value	
			    case "noise" : {    	scene.setNoise(Double.parseDouble(token[1]), token);    	break; }			    
			    //set colors used by procedural texture : <noise color tag> <color r g b> <-specify multiple times for each color
			    //need to put color commands/list after proc txtr type command in cli file, before object
			    case "noise_color" :{			    	scene.setTxtrColor(token);				    	break;   }			    
				//the following may just have <typ> or may have up to color scale, or may have all values - use defaults for all values not specified
				//<typ> <noise scale> <numOctaves> <turbMult> <pdMult x y z> <multByPI 1/0 1/0 1/0> <useFwdTransform 0/1> 
			    //		<rndomize colors colorScale - if present then true> <color mult> <num overlays - if present, otherwise 1>
			    case "marble" :
			    case "stone" : 
			    case "wood" : 
			    case "wood2" : {   	scene.setTexture(token);   	break;    }			 
			    
			    //polygons		    
			    case "begin" : {//begin shape - defaults to triangle
			    	try {	vertType = token[1];   	} catch (Exception e) {	}      //triangle is default;  quad will be specified
			     	myVertCount = 0;
			      	if (vertType.equals("quad")){	myPoly = new myQuad(scene);} 
			      	else {				     		myPoly = new myTriangle(scene);}
			    	break;}
			    //texture_coord u v
			    case "texture_coord" : {
				//specifies the texture coordinate for the next vertex that is to be created for a triangle. - each "texture_coord" command will come before the corresponding "vertex" command in the .cli files
			    	((myPlanarObject) myPoly).setTxtrCoord(Double.parseDouble(token[1]),Double.parseDouble(token[2]), myVertCount); 
			    	break;}
			    case "vertex" : {
			    	((myPlanarObject) myPoly).setVert(Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]), myVertCount);
			    	myVertCount++;
			    	break;}
			    case "end" : {//end shape - reset vars to add new triangle, finalize triangle, add to arraylist of sceneobjects		
			    	
		    		((myPlanarObject) myPoly).finalizePoly();
		    		myPoly.shdr = scene.getCurShader();				//set shader for object <- important! this is where any proc textures used are specified
		    		scene.addObjectToScene(myPoly);

		    		vertType = "triangle";    //reset default object type as triangle
			    	myVertCount = 0;      
			    	break;}
			    
			    //prims - build in scene code
			    case "box" : 	    
			    case "plane" : 
			    case "cyl" :		   
			    case "cylinder" : 
			    case "hollow_cylinder" :
			    case "sphere" : 	    
			    case "moving_sphere":   
			    case "sphereIn" : 
			    case "ellipsoid" : {	scene.readPrimData(token);   	break;}
//				hollow_cylinder radius x z ymin ymax
//				Create a hollow cylinder that has its axis parallel to the y-axis. The hollow cylinder should not have end caps. 
			    
			    //matrix transformations of objects
			    case "push" : {  	scene.gtPushMatrix(); break;	    }//matrix push
			    case "pop" : {   	scene.gtPopMatrix();break;		    }//matrix pop
			    case "rotate" : {//needs to be in degrees as per assignment
			    	//builds a rotation matrix and adds to current transformation matrix on stack - angle, x,y,z
			    	scene.gtRotate(Double.parseDouble(token[1]), Double.parseDouble(token[2]), Double.parseDouble(token[3]), Double.parseDouble(token[4]));
			    	break;}
			    case "scale" : {
			    	//builds a scale matrix and adds to current transformation matrix on stack - sx,sy,sz
			    	scene.gtScale(Double.parseDouble(token[1]), Double.parseDouble(token[2]), Double.parseDouble(token[3]));
			    	break;}
			    case "translate" : {
			    	//builds a translate matrix and adds to current transformation matrix on stack - tx,ty,tz
			    	scene.gtTranslate(Double.parseDouble(token[1]), Double.parseDouble(token[2]), Double.parseDouble(token[3]));
			    	break;}		
			    default : {
			    	System.out.println("When reading "+fileName+" unknown command encountered : '"+token[0]+"' on line : ["+i+"] : " + fileStrings[i]);
			    	break;}
			}//switch on object type from file	
		}//for each string
		if((isMainFileScene) && !finalized){finalizeScene(loadedScenes,fileName, scene);	}	//puts finished scene -- only put in if _scene is null, meaning this is root file, not 2ndary read file
		return scene;
	}
	
	//build a color value from a string array read in from a cli file.  stIdx is position in array where first color resides
	private myColor readColor(String[] token, int stIdx){return new myColor(Double.parseDouble(token[stIdx]),Double.parseDouble(token[stIdx+1]),Double.parseDouble(token[stIdx+2]));}
	
	public void finalizeScene(TreeMap<String, myScene> loadedScenes, String fileName,myScene scene){
		//finalize scene
		scene.srcFileNames.push(fileName);
		loadedScenes.put(fileName, scene);
	}
	
	//handle surface and shiny commands (very similar in layout but slight differences - shiny will use "simple" version of transmittance, not TIR
	public void setSurfaceShiny(myScene scene, String[] token, boolean useSimple){
		myColor cDiff = readColor(token,1);
		myColor cAmb = readColor(token,4);
		myColor cSpec = readColor(token,7);
		double phongExp = Double.parseDouble(token[10]);
		double kRefl = Double.parseDouble(token[11]);
		double kTrans = 0;
		double rfrIdx = 0;
		scene.scFlags[myScene.glblTxtrdTopIDX] = false;
		scene.scFlags[myScene.glblTxtrdBtmIDX] = false;
		scene.setSurface(cDiff,cAmb,cSpec,phongExp,kRefl);
		try {//if ktrans value in format file, grab it and re-initialize surfaces
			kTrans = Double.parseDouble( token[12]);  
			scene.setSurface(cDiff,cAmb,cSpec,phongExp,kRefl,kTrans);
			rfrIdx = Double.parseDouble(token[13]);	//perm
			scene.setSurface(cDiff,cAmb,cSpec,phongExp,kRefl,kTrans,rfrIdx);
			scene.setRfrIdx(rfrIdx);    	
    		scene.setRfrIdx(rfrIdx,Double.parseDouble(token[14]),Double.parseDouble(token[15]),Double.parseDouble(token[16]));    	
		} catch (Exception e) {}
		if((useSimple) && ((kTrans > 0) || (rfrIdx > 0))){scene.scFlags[myScene.simpleRefrIDX] = true;}			
	}//setSurfaceShiny

}//class myRTFileReader 


