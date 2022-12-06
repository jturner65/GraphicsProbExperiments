package base_RayTracer.scene.base;

import java.util.*;
import java.util.concurrent.*;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.geometry.ObjInstance;
import base_RayTracer.scene.geometry.accelStruct.*;
import base_RayTracer.scene.geometry.accelStruct.base.Base_AccelStruct;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.scene.geometry.sceneObjects.base.Base_SceneObject;
import base_RayTracer.scene.geometry.sceneObjects.implicit.*;
import base_RayTracer.scene.geometry.sceneObjects.lights.*;
import base_RayTracer.scene.geometry.sceneObjects.lights.base.Base_Light;
import base_RayTracer.scene.geometry.sceneObjects.planar.myPlane;
import base_RayTracer.scene.photonMapping.Photon_KDTree;
import base_RayTracer.scene.photonMapping.myPhoton;
import base_RayTracer.scene.shaders.myObjShader;
import base_RayTracer.scene.shaders.mySimpleReflObjShdr;
import base_RayTracer.scene.textures.base.Base_TextureHandler;
import base_RayTracer.scene.textures.imageTextures.myImageTexture;
import base_RayTracer.scene.textures.miscTextures.myNoneTexture;
import base_RayTracer.scene.textures.noiseTextures.*;
import base_RayTracer.scene.textures.noiseTextures.cellularTextures.myCellularTexture;
import base_RayTracer.ui.base.Base_RayTracerWin;
import base_RayTracer.utils.myRTColor;
import base_UI_Objects.*;
import base_Utils_Objects.io.messaging.MsgCodes;
import processing.core.PConstants;
import processing.core.PImage;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.matrixObjs.doubles.myMatStack;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * class to hold all objects within a desired scene
 * @author 7strb
 *
 */
public abstract class Base_Scene {
	protected static IRenderInterface pa;
	//Owning window
	protected Base_RayTracerWin win;
	
	//multi-threaded stuff
	protected ExecutorService th_exec;
	
	//max # of prims per leaf of accel structure 
	public final int maxPrimsPerLeaf = 5;

	////
	//constants and control variables for a particular scene - make changeable via .cli reader
	//maximum height/depth of matrix stack
	private final int matStackMaxHeight = 20;

	//row and col values of scene - scene dimensions in pxls
	protected int sceneCols = 300;
	//public static final int sceneRows = 240;
	protected int sceneRows = 300;
	//number of rays in play - including initial ray (recursive depth)
	public int numRays = 8;

	//origin of eye rays
	protected myVector eyeOrigin;
	
	//halfway point in rows and cols
	protected double rayYOffset, rayXOffset;
	
	/////////////////////////////
	//refining index list
	private int[] RefineIDX;
	private int curRefineStep;
	
	public ArrayDeque<String> srcFileNames;
	
	protected String saveName, fileName, folderName;										//name of cli file used to describe this scene, save file, name of containing folder
	
	//the current texture to be used for subsequent objects for either their "top" or "bottom" as determined by their normal, the texture for the background, result image of rendering
	public PImage currTextureTop, currTextureBottom, currBkgTexture; 
	private PImage rndrdImg;
	
	/**
	 * an array list of all the objects in the scene : objList does not include lights, objAndLightList includes all lights
	 */
	public ArrayList<Base_Geometry> allObjsToFind;
	//the following to facilitate faster light and object lookup and instancing by separating them
	public ArrayList<Base_Geometry> objList;
	public ArrayList<Base_Geometry> lightList; 	
	
	//objects to be put in accel structure - temp storage as list is being built
	public ArrayList<Base_Geometry> tmpObjList;

	//named objects - to be instanced later
	public TreeMap<String, Base_Geometry> namedObjs;
	public TreeMap<String, Integer> numInstances;
	
	//background texture-holding sphere.
	public mySphere mySkyDome;	
	
	//current number of lights, number of objects built, used for ID field in mySceneObject constructor and addNewLights, num non-light objects
	public int numLights, objCount, numNonLights, numPxls, numNamedObjs;
	//debug - count rays refracted, reflected
	public long refrRays = 0, reflRays = 0, globRayCount = 0;
	
	private int[] scFlags;			//boolean flags describing characteristics of this scene
	
	private static final int	
		//scene/global level flags
		debugIDX			= 0,		//enable debug functionality
		renderedIDX 		= 1,		//this scene has been rendered since any changes were made
		saveImageIDX		= 2,		//whether or not to save an image
		saveImgInDirIDX		= 3,		//save image inside specific image directory, rather than root
		simpleRefrIDX		= 4,		//whether scene should use simplified refraction (intended to minimize refactorization requirement in mySceneObject)
		
		flipNormsIDX		= 5,		//whether or not we should flip the normal directions in this scene
		hasDpthOfFldIDX		= 6,		//using depth of field
		showObjInfoIDX 		= 7,		//print out object info after rendering image
		addToTmpListIDX		= 8,		//add object to the temp list, so that the objects will be added to some accel structure.
		timeRndrIDX			= 9,		//time the length of rendering and display the results.
		glblTxtrdBkgIDX		= 10,		//whether the background is to be textured
		glblRefineIDX 		= 11,		//whether scene should be rendered using iterative refinement technique
		useFGColorIDX		= 12,		//whether or not to use a foregroundcolor in this scene

		//currently-loading object level flags
		glblTxtrdTopIDX		= 13,		//whether the currently loading object should be txtred on the top
		glblTxtrdBtmIDX		= 14,		//whether the currently loading object should be txtred on the bottom
	
		usePhotonMapIDX		= 15,		//whether to use a photon map for caustics, indirect illumination
		isCausticPhtnIDX	= 16,		//whether to use caustic photons or indirect illumination photons (hacky)
		isPhtnMapRndrdIDX	= 17,		//whether or not photons have been cast yet
		doFinalGatherIDX	= 18;		//whether to do final gather
	
	public static final int numFlags = 19;
	
	////////////////
	//indirect illumination/caustics stuff
	public Photon_KDTree photonTree;		//TODO make two of these, one for caustics, one for indirect illum
	public int numGatherRays;
	public int numPhotons, kNhood;
	public float photonMaxNearDist;
	//recursive depth for photons
	public int numPhotonRays = 4;	
	//to correct for light power oddness
	public double causticsLightPwrMult  = 40.0,
			diffuseLightPwrMult = 8.0;
	
	//end indirect illumination/caustics stuff
	////////
	
	//////////////////
	// proc & noise txtrs - colors and constants 	
	public int txtrType;				//set to be 0 : none; 1 : image; 2 : noise; 3 : wood; 4 : marble; 5 : stone/brick
	private double noiseScale;			//set if using noise-based proc txtr
	
	public myRTColor[] noiseColors = new myRTColor[]{new myRTColor(.7,.7,.7),
												new myRTColor(.2,.2,.2)};
	//how much weight each color should have - TODO
	public Double[] clrWts = new Double[]{.5,.5};
	//turbulence values
	public int numOctaves = 8;
	public double //turbScale = 128.0,						// equivalent to max scale/size value of turbulence
					turbMult = 1.0;							//multiplier of turbulence effect
	public double colorScale = 10.0, colorMult = .2;
	public myVector pdMult = new myVector(10,10,10);		//vector to hold multipliers for periodic textures
	public boolean rndColors,								//randomize colors a little bit; 
					useCustClrs,							//colors specified in cli file
					useFwdTrans;							//use fwd-transformed hit location (for meshes, don't use for prims)		
	public int numOverlays = 1;								//number of parallel txtrs used as overlays (semi transparent)
	//worley noise
	public double avgNumPerCell = 1, mortarThresh = .04;						//avg # of particles per cell
	public int numPtsDist = 2;								//# of closest points we take distance of to get color/effect
	public int distFunc = 1;								//distance function used for worley noise : 0-> manhattan, 1->euclid
	//function for determining region of interest in worley noise :
	// 0 : nearest x sum, 1 : alternating linear sum, 2: ...
	public int roiFunc = 1;									

	
	//end proc & noise txtrs
	/////////////////////////////	
	//global values set by surface parameter
	public myRTColor currDiffuseColor, currAmbientColor, currSpecularColor,globCurPermClr, currKReflClr, backgroundColor;//, foregroundColor;
	public double currPhongExp, currKRefl,globRfrIdx,currKTrans, currDepth, lens_radius, lens_focal_distance;   //value representing the visible depth of a colloid (i.e. subsurface experiment) 
	
	public myPlane focalPlane;							//if Depth of Field scene, this is the plane where the lens is in focus
	
	//replaced by num rays per pixel
	public int numRaysPerPixel;
	
	//constant color for mask of fisheye
	public myRTColor blkColor = new myRTColor(0,0,0);
	
	public double maxDim, yStart, xStart, fishMult;			//compensate for # rows or # cols not being max - make sure projection is centered in non-square images
	
	//length of time to render
	public float renderTime;	
	
	public boolean initFlag = false;
	
	//Matrix stack used to process transformations from file descriptions
	private myMatStack matrixStack;
	//current depth in matrix stack - starts at 0;
	private int currMatrixDepthIDX;	
		
	public Base_Scene(IRenderInterface _p, Base_RayTracerWin _win, String _sceneName, int _numCols, int _numRows) {
		pa = _p;
		win = _win;
		initFlags();
		setFlags(saveImageIDX, true);    												//default to saving image
		setFlags(saveImgInDirIDX, true);    											//save to timestamped directories, to keep track of changing images
		setFlags(showObjInfoIDX, true);    												//default to showing info
		
		gtInitialize();       															 //sets up matrix stack

		folderName = "pics." + win.getAppFileSubdirName();
		
		allObjsToFind = new ArrayList<Base_Geometry>();
		lightList = new ArrayList<Base_Geometry>();
		objList = new ArrayList<Base_Geometry>();
		
		srcFileNames = new ArrayDeque<String>();
		eyeOrigin = new myVector(0,0,0);
		namedObjs = new TreeMap<String,Base_Geometry>();
		numInstances = new TreeMap<String,Integer>();

		tmpObjList = new ArrayList<Base_Geometry>();										//list to hold objects being built to be put into acceleration structure	
		initVars(_sceneName,_numCols, _numRows);
	}
	
	public Base_Scene(Base_Scene _old){//copy ctor, for when scene type is being set - only use when old scene is being discarded (shallow copy)
		//pa = _old.pa;
		win = _old.win;
		folderName = _old.folderName;
		
		initFlags();
		for(int i=0;i<scFlags.length;++i) {	scFlags[i] = _old.scFlags[i];}
		
		gtInitialize();       															 //sets up matrix stack
		
		allObjsToFind = new ArrayList<Base_Geometry>(_old.allObjsToFind);
		lightList = new ArrayList<Base_Geometry>(_old.lightList);
		objList = new ArrayList<Base_Geometry>(_old.objList);
		
		srcFileNames = _old.srcFileNames;
		eyeOrigin = new myVector(0,0,0);
		eyeOrigin.set(_old.eyeOrigin);
		namedObjs = new TreeMap<String,Base_Geometry>(_old.namedObjs);
		numInstances = new TreeMap<String,Integer>(_old.numInstances);

		tmpObjList = new ArrayList<Base_Geometry>(_old.tmpObjList);

		copyVars(_old);	
		matrixStack = _old.matrixStack;
	}//myScene
	
	//scene-wide variables set during loading of scene info from .cli file
	private void initVars(String _sceneName, int _numCols, int _numRows){
		setImageSize(_numCols, _numRows);	
		currTextureTop = null;
		currTextureBottom = null;
		currBkgTexture = null;
		photonTree = null;
		numGatherRays = 0;
		numPhotons = 0;
		kNhood = 0;
		photonMaxNearDist = 0;
		//save file name and source file name should be the same unless overridden by command in scene description file
		fileName = _sceneName;
		saveName = _sceneName;
		txtrType = 0;
		noiseScale = 1;
		numNonLights = 0;
		numLights = 0;
		objCount = 0;
		numNamedObjs = 0;
		
		backgroundColor = new myRTColor(0,0,0);
		currDiffuseColor = new myRTColor(0,0,0);
		currAmbientColor = new myRTColor(0,0,0);
		currSpecularColor = new myRTColor(0,0,0);
		currPhongExp = 0;
		currKRefl = 0;
		currKReflClr = new myRTColor(0,0,0);
		currDepth = 0;		 
		currKTrans = 0;
		globRfrIdx = 0.0;
		globCurPermClr = new myRTColor(0,0,0);

		curRefineStep = 0;
		reflRays = 0;
		refrRays = 0;
		globRayCount = 0;
		numRaysPerPixel = 1;
		focalPlane = new myPlane(this);
	}//initVars method
	
	//scene-wide variables set during loading of scene info from .cli file
	private void copyVars(Base_Scene _old){
		setImageSize(_old.sceneCols,_old.sceneRows);
		currTextureTop = _old.currTextureTop;
		currTextureBottom = _old.currTextureBottom;
		currBkgTexture = _old.currBkgTexture;
		saveName = _old.saveName;

		fileName = _old.fileName;
		txtrType = _old.txtrType;
		noiseScale = _old.noiseScale;
		
		numGatherRays = _old.numGatherRays;
		photonTree = _old.photonTree;	
		numPhotons = _old.numPhotons;
		kNhood = _old.kNhood;
		photonMaxNearDist = _old.photonMaxNearDist;
		
		numNonLights = _old.numNonLights;
		numLights = _old.numLights;
		objCount = _old.objCount;
		numNamedObjs = _old.numNamedObjs;
		
		backgroundColor = _old.backgroundColor;
//		foregroundColor = _old.foregroundColor;
		currDiffuseColor = _old.currDiffuseColor;
		currAmbientColor = _old.currAmbientColor;
		currSpecularColor = _old.currSpecularColor;
		currPhongExp = _old.currPhongExp;
		currKRefl = _old.currKRefl;
		currKReflClr = _old.currKReflClr;
		currDepth = _old.currDepth;		 
		currKTrans = _old.currKTrans;
		globRfrIdx = _old.globRfrIdx;
		globCurPermClr = _old.globCurPermClr;

		curRefineStep = _old.curRefineStep;
		reflRays = _old.reflRays;
		refrRays = _old.refrRays;
		globRayCount = _old.globRayCount;
		
		numRaysPerPixel = _old.numRaysPerPixel;

		lens_radius = _old. lens_radius;
		lens_focal_distance = _old.lens_focal_distance;		
		focalPlane = _old.focalPlane;		
	}//initVars from old scene method	

	public void startTmpObjList(){
		tmpObjList = new ArrayList<Base_Geometry>();
		setAddToTmpList(true);
	}
	
	/**
	 * end building the accel struct - if is list just build arraylist object, otherwise build acceleration struct
	 * @param lstType
	 */
	public void endTmpObjList(int lstType){
		setAddToTmpList(false);
		Base_AccelStruct accelObjList = null;
		if(lstType == 0){//flat geomlist
			accelObjList = new GeoList_AccelStruct(this);
			//int objAdded = 1;
			for(Base_Geometry obj : tmpObjList){		((GeoList_AccelStruct)accelObjList).addObj(obj);	}
		} else if(lstType == 1){//bvh tree
			win.getMsgObj().dispInfoMessage("Base_Scene", "endTmpObjList", "Begin adding to BVH structure - # objs : " + tmpObjList.size());
			accelObjList = new BVH_AccelStruct(this);
			List<Base_Geometry>[] _tmpCtr_ObjList = ((BVH_AccelStruct)accelObjList).buildSortedObjAras(tmpObjList,-1);
			((BVH_AccelStruct)accelObjList).addObjList(_tmpCtr_ObjList, 0, _tmpCtr_ObjList[0].size()-1);
			win.getMsgObj().dispInfoMessage("Base_Scene", "endTmpObjList", "Done Adding to BVH structure");
		} else if(lstType == 2){//KDtree/octree TODO
			win.getMsgObj().dispInfoMessage("Base_Scene", "endTmpObjList", "Begin adding to octree structure TODO");
			win.getMsgObj().dispInfoMessage("Base_Scene", "endTmpObjList", "Done Adding to octree structure TODO");
		}
		addObjectToScene(accelObjList);
	}//endTmpObjList
	
	/**
	 * Build a sierpinski tet arrangement using instances of object name.
	 * @param name
	 * @param scVal
	 * @param depth how deep to build the tetrahedrons
	 * @param useShdr whether or not to use changing shader based on depth
	 */
	public void buildSierpinski(String name, float scVal, int depth, boolean useShdr){
		startTmpObjList();
		win.buildSierpSubTri(this, 8,scVal, name,0,depth,useShdr);
		endTmpObjList(1);			//bvh of sierp objs
		win.getMsgObj().dispInfoMessage("Base_Scene", "buildSierpinski", "Total Objects : "+((Math.pow(4, depth)-1)/3.0f));
	}	
	/**
	 * remove most recent object from list of objects and instead add to instance object struct.
	 * @param name
	 */
	public void setObjectAsNamedObject(String name){
		Base_Geometry _obj = allObjsToFind.remove(allObjsToFind.size()-1);
		objCount--;
		if(_obj instanceof Base_Light){												lightList.remove(lightList.size()-1);	numLights--;	} 
		else { 																		objList.remove(objList.size()-1); 		numNonLights--;	}	
		namedObjs.put(name, _obj);
		numInstances.put(name, 0);			//keep count of specific instances
		numNamedObjs++;
	}//setObjectAsNamedObject
	
	public void addInstance(String name, boolean addShdr){
		Base_Geometry baseObj = namedObjs.get(name);
		ObjInstance _inst = new ObjInstance(this, baseObj);
		if(addShdr){		_inst.useInstShader();	}
		addObjectToScene(_inst, baseObj);			
		numInstances.put(name, numInstances.get(name)+1);
	}//
	
	// adds a new pointlight to the array of lights :   @params rgb - color, xyz - location
	public void addMyPointLight(String[] token){
		win.getMsgObj().dispInfoMessage("Base_Scene", "addMyPointLight", "Point Light : current # of lights : " + numLights);
		myPointLight tmp = new myPointLight(this, numLights, 
				Double.parseDouble(token[4]),Double.parseDouble(token[5]),Double.parseDouble(token[6]),
				Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]));
		addObjectToScene(tmp);
	}//addMyPointLight method
	 
		// adds a new spotlight to the array of lights :   @params rgb - color, xyz - location, dx,dy,dz direction, innerThet, outerThet - angle bounds
	public void addMySpotLight(String[] token){
		double _inThet = Double.parseDouble(token[7]);
		double _outThet = Double.parseDouble(token[8]);
		win.getMsgObj().dispInfoMessage("Base_Scene", "addMySpotLight", "Spotlight : current # of lights : " + numLights + " inner angle : " + _inThet + " outer angle : " + _outThet);
		mySpotLight tmp = new mySpotLight(this, numLights,
				Double.parseDouble(token[9]),Double.parseDouble(token[10]),Double.parseDouble(token[11]),
				Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]),
				Double.parseDouble(token[4]),Double.parseDouble(token[5]),Double.parseDouble(token[6]),
				_inThet,_outThet);  
		addObjectToScene(tmp);
	}//addMySpotLight method
	
	// adds a new disklight to the array of lights : disk_light x y z radius dx dy dz r g b
	public void addMyDiskLight(String[] token){
		double radius = Double.parseDouble(token[4]);
		win.getMsgObj().dispInfoMessage("Base_Scene", "addMyDiskLight", "Disk Light : current # of lights : " + numLights + " radius : " + radius);
		myDiskLight tmp = new myDiskLight(this, numLights,
				Double.parseDouble(token[8]),Double.parseDouble(token[9]),Double.parseDouble(token[10]),
				Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]),
				Double.parseDouble(token[5]),Double.parseDouble(token[6]),Double.parseDouble(token[7]),
				radius);  
		addObjectToScene(tmp);
	}//addMyDiskLight method	
	
	/**
	 * return a shader built with the current settings
	 */
	public myObjShader getCurShader(){
		myObjShader tmp = (doSimpleRefr()) ? new mySimpleReflObjShdr(this) : new myObjShader(this);
		tmp.txtr = getCurTexture(tmp);				
		return tmp;
	}//getCurShader
	
	/**
	 * return appropriate texture handler
	 * @param tmp
	 * @return
	 */
	public Base_TextureHandler getCurTexture(myObjShader tmp){
		switch (txtrType){
			case 0 : {	return new myNoneTexture(this,tmp);}						//diffuse/shiny only
			case 1 : { 	return new myImageTexture(this,tmp);}						//has an image texture
			case 2 : {	return new myNoiseTexture(this,tmp,noiseScale);}			//perlin noise
			case 3 : {	return new myBaseWoodTexture(this,tmp,noiseScale);}
			case 4 : { 	return new myMarbleTexture(this,tmp,noiseScale);}
			case 5 : { 	return new myCellularTexture(this,tmp,noiseScale);}	
			case 6 : { 	return new myWoodTexture(this,tmp,noiseScale);}
			default : { return new myNoneTexture(this,tmp);}
		}
	}//getCurTexture
	/**
	 * return appropriate texture name
	 * @return
	 */
	public String getTxtrName(){
		switch (txtrType){
			case 0 : {	return "No Texture";}						//diffuse/shiny only
			case 1 : { 	return "Image Texture";}					//has an image texture
			case 2 : {	return "Pure Noise Texture";}				//perlin noise
			case 3 : {	return "Base Wood Texture";}
			case 4 : { 	return "Marble Texture";}
			case 5 : { 	return "Cellular Texture";}	
			case 6 : { 	return "Alt Wood Texture";}
			default : { return "No Texture";}
		}
	}//getTxtrName
	
	//entry point
	public void addObjectToScene(Base_Geometry _obj){addObjectToScene(_obj,_obj);}
	public void addObjectToScene(Base_Geometry _obj, Base_Geometry _cmpObj){
		if(doAddToTmpList()){tmpObjList.add(_obj); return;}
		if(_cmpObj instanceof Base_Light){			lightList.add(_obj);	++numLights;} 
		else {									objList.add(_obj);		++numNonLights;}
		allObjsToFind.add(_obj);
		objCount++;
	}//addObjectToScene

	/////////
	//setting values from reader
	////
	
	public void setNoise(double scale, String[] vals){
		resetDfltTxtrVals();//reset values for next call
		txtrType = 2;
		win.getMsgObj().dispInfoMessage("Base_Scene", "setNoise","Setting Noise to scale : " + scale);	
		noiseScale = scale;		//only specified value is scale currently
	}//setNoise
	
	//call after a proc texture is built, to reset values to defaults
	public void resetDfltTxtrVals(){
		setProcTxtrVals(new int[]{0, 4, 1, 2, 1, 1}, 
				new boolean[]{false,false,false}, 
				new double[]{1.0, 1.0, 5.0, .1, 1.0, 0.05}, 
				new myVector[]{new myVector(1.0,1.0,1.0)}, 
				new myRTColor[]{ getClr("clr_nearblack"),getClr("clr_white")},
				new Double[]{.5,.5});
	}//resetDfltTxtrVals

	/**
	 * Set procedural texture values from values in loaded scene description
	 * @param ints 	: txtrType, numOctaves, numOverlays, numPtsDist,distFunc, roiFunc
	 * @param bools : rndColors, useCustClrs, useFwdTrans
	 * @param dbls  : noiseScale, turbMult, colorScale, colorMult, avgNumPerCell, mortarThresh
	 * @param vecs  : pdMult
	 * @param clrs  : noiseColors
	 * @param wts   : clrWts
	 */
	private void setProcTxtrVals(int[] ints, boolean[] bools, double[] dbls, myVector[] vecs, myRTColor[] clrs, Double[] wts){
		txtrType = ints[0];	numOctaves = ints[1];numOverlays = ints[2];	numPtsDist = ints[3];	distFunc = ints[4];	roiFunc = ints[5];	
		rndColors = bools[0]; useCustClrs = bools[1];useFwdTrans = bools[2];		
		noiseScale = dbls[0]; turbMult = dbls[1];colorScale = dbls[2];colorMult = dbls[3];avgNumPerCell = dbls[4];	mortarThresh = dbls[5];		
		pdMult = vecs[0];		
		noiseColors = clrs;
//		clrWts = wts;		
	}//setProcTxtrVals
	
	/**
	 * build a color value from a string array read in from a cli file.  stIdx is position in array where first color resides
	 * @param token
	 * @param stIdx
	 * @return
	 */
	private myRTColor readColor(String[] token, int stIdx){return new myRTColor(Double.parseDouble(token[stIdx]),Double.parseDouble(token[stIdx+1]),Double.parseDouble(token[stIdx+2]));}

	/**
	 * set colors used by proc texture
	 * @param clrs
	 */
	public void setTxtrColor(String[] clrs){
		//get current noise color array
		if(!useCustClrs){
			noiseColors = new myRTColor[0];
			clrWts = new Double[0];
			useCustClrs = true;
		}
		ArrayList<myRTColor> tmpAra = new ArrayList<myRTColor>(Arrays.asList(noiseColors));
		ArrayList<Double> tmpWtAra = new ArrayList<Double>(Arrays.asList(clrWts));
		//<noise color spec tag> (<'named'> <clr name>) or  (<color r g b>)  <wt> <-specify once for each color
		try{	
			myRTColor tmp = null;
			int wtIdx;
			//name has format "clr_<colorname>"
			if(clrs[1].equals("named")){	tmp = getClr(clrs[2]);	wtIdx = 3;} 
			else {							tmp = readColor(clrs, 1);	wtIdx = 4;}
			tmpAra.add(tmp);
			try{
				double wt = Double.parseDouble(clrs[wtIdx]);
				tmpWtAra.add(wt);	//normalize at end, when all colors have been added and txtr being built		
			}
			catch(Exception e) {//no weight specified, just add avg of existing weights
				double tmpWt = 0;
				if(tmpWtAra.size() == 0){tmpWt = 1.0;} 
				else {
					int cnt = 0;
					for(Double wt : tmpWtAra){	tmpWt += wt;cnt++;}
					tmpWt /= (1.0*cnt);
				}
				tmpWtAra.add(tmpWt);
			}//catch		
			win.getMsgObj().dispInfoMessage("Base_Scene", "setTxtrColor","Finished loading color : " + tmp + " for txtr " + getTxtrName());
		}
		catch (Exception e) {
			String res = "Invalid color specification : \n" ;	
			for(int i =0; i<clrs.length;++i){res+=" {"+clrs[i]+"} \n";}
			res+=" so color not added to array\n";
			win.getMsgObj().dispMultiLineMessage("Base_Scene", "addMyPointLight",res, MsgCodes.error1);
		}	 		
		noiseColors = tmpAra.toArray(new myRTColor[0]);
	}//setTxtrColors

	/**
	 * read in constants configured for perlin noise
	 * @param vals
	 * @return
	 */
	private boolean readProcTxtrPerlinVals(String[] vals){
		boolean useDefaults;
		//may just have <typ> or may have up to color scale, or may have all values - use defaults for all values not specified
		//<typ> <noise scale> <numOctaves> <turbMult> <pdMult x y z> <multByPI 1/0 1/0 1/0> <useFwdTransform 0/1> <?rndomize colors colorScale - if present then true> <color mult> <num overlays - if present, otherwise 1>
		try{
			noiseScale = Double.parseDouble(vals[1]);
			numOctaves = Integer.parseInt(vals[2]);
			turbMult = Double.parseDouble(vals[3]);
			pdMult = new myVector(Double.parseDouble(vals[4]),Double.parseDouble(vals[5]),Double.parseDouble(vals[6]));
			myVector pyMult = new myVector(Double.parseDouble(vals[7]),Double.parseDouble(vals[8]),Double.parseDouble(vals[9]));
			if(pyMult.sqMagn > 0){									//if vector values specified
				pyMult._mult(MyMathUtils.TWO_PI-1.0);				//change 0|1 to 0|2pi-1 vector
				pyMult._add(1.0,1.0,1.0);							//change 0|2pi-1 vector to 1|2pi vector
				pdMult = myVector._elemMult(pdMult, pyMult);
			}
			useFwdTrans = (Double.parseDouble(vals[10]) == 1.0);		//whether or not to use fwd transform on points
			try{
				colorScale = Double.parseDouble(vals[11]);
				colorMult = Double.parseDouble(vals[12]);
				rndColors = true;
				try{	numOverlays = Integer.parseInt(vals[13]);	}	catch (Exception e) {numOverlays = 1;	}		
			}
			catch (Exception e) {	
				win.getMsgObj().dispWarningMessage("Base_Scene", "readProcTxtrPerlinVals","Proc Perlin-based txtr not specifying randomize colors or overlays so defaults are used for txtr type : " + getTxtrName());	
				rndColors = false;	colorScale = 25.0;colorMult = .1;numOverlays = 1;}	 
			useDefaults = false;
			win.getMsgObj().dispInfoMessage("Base_Scene", "readProcTxtrPerlinVals", "Finished loading custom values for texture type : " + getTxtrName());
		}
		catch (Exception e) {
			win.getMsgObj().dispWarningMessage("Base_Scene", "readProcTxtrPerlinVals", "No Proc Texture values specified for texture type : " + getTxtrName() + " so using defaults.");	useDefaults = true;	}	 
		return useDefaults;	
	}//readProcTxtrPerlinVals
	
	/**
	 * read in constants configured for worley noise
	 * @param vals
	 * @return
	 */
	private boolean readProcTxtrWorleyVals(String[] vals){
		boolean useDefaults;
		//different format than perlin
		//may just have <typ> or may have up to color scale, or may have all values - use defaults for all values not specified
		//<typ> <noise scale> <distfunction 0=man/1=euc> <roiFunc 0=altLinSum/1=?><num pts for dist func - should be even> <avg # pts per cell> <mortar threshold 0.0-1.0> <useFwdTransform 0/1> <?rndomize colors colorScale - if present then true> <color mult> <num overlays - if present, otherwise 1>
		try{
			noiseScale = Double.parseDouble(vals[1]);
			distFunc = Integer.parseInt(vals[2]);
			roiFunc = Integer.parseInt(vals[3]);	//function for determining region of interest in worley noise - 0 : nearest lin sum 1 : alternating linear sum, 2+: ....
			numPtsDist = Integer.parseInt(vals[4]);
			avgNumPerCell = Double.parseDouble(vals[5]);
			mortarThresh = Double.parseDouble(vals[6]);
			useFwdTrans = (Double.parseDouble(vals[7]) == 1.0);		//whether or not to use fwd transform on points
			try{
				colorScale = Double.parseDouble(vals[8]);
				colorMult = Double.parseDouble(vals[9]);
				rndColors = true;
				try{	numOverlays = Integer.parseInt(vals[10]);	}	catch (Exception e) {numOverlays = 1;	}		
			}
			catch (Exception e) {	
				win.getMsgObj().dispErrorMessage("Base_Scene", "readProcTxtrWorleyVals", "Proc txtr not specifying randomize colors or overlays so defaults are used for txtr type : " + getTxtrName());	
				rndColors = false;	colorScale = 25.0;colorMult = .1;numOverlays = 1;}	 
			useDefaults = false;
			win.getMsgObj().dispInfoMessage("Base_Scene", "readProcTxtrWorleyVals", "Finished loading custom values for texture type : " + getTxtrName());
		}
		catch (Exception e) {win.getMsgObj().dispErrorMessage("Base_Scene", "readProcTxtrWorleyVals", "No Proc Texture values specified for texture type : " + getTxtrName() + " so using defaults.");	useDefaults = true;	}	 
		return useDefaults;	
	}//readProcTxtrWorleyVals
	
	/**
	 * read in procedural texture values for perlin noise and populate globals used to build txtr
	 * @param vals
	 * @param isPerlin
	 * @return
	 */
	public boolean readProcTxtrVals(String[] vals, boolean isPerlin){
		if(isPerlin){return readProcTxtrPerlinVals(vals);}
		else { return readProcTxtrWorleyVals(vals);}
	}//readProcTxtrVals	
	
	/**
	 * proc texture components
	 * @param vals
	 */
	public void setTexture(String[] vals){
		//TODO get all these values from CLI
		resetDfltTxtrVals();//reset values for next call
		String _typ = vals[0];
		switch(_typ){
			case "wood":{
				txtrType = 3;
				boolean useDefaults = readProcTxtrVals(vals, true);
				//may be overwritten by later color commands in cli
				if(!useCustClrs){noiseColors = new myRTColor[]{ getClr("clr_dkwood1"),	getClr("clr_ltwood1")};clrWts = new Double[] {1.0,1.0};}
				if(useDefaults){					
					setProcTxtrVals(new int[]{txtrType, 4, 1, 2, 1,1}, 				//int[] ints =  txtrType,  numOctaves,  numOverlays,  numPtsDist,distFunc (not used for perlin), roiFunc (not used for perlin)
							new boolean[]{true,useCustClrs,false}, 					//boolean[] bools = rndColors , useCustClrs, useFwdTrans -> useFwdTrans needs to be specified in command in cli
							new double[]{2.0, .4, 25.0, .2, 1.0, 0.05}, 			//double[] dbls = noiseScale, turbMult , colorScale , colorMult, avgNumPerCell,mortarThresh
							new myVector[]{new myVector(MyMathUtils.TWO_PI* 2.7,3.6,4.3)}, 	//myVector[] vecs = pdMult
							noiseColors, 											//myColor[] clrs = noiseColors
							clrWts);												//Double[] wts = clrWts					
				} break;}		
			case "wood2"  : {//yellow-ish by default
				txtrType = 6;
				boolean useDefaults = readProcTxtrVals(vals, true);
				if(!useCustClrs){noiseColors = new myRTColor[]{ getClr("clr_dkwood2"),	getClr("clr_ltwood2")};}// 		clrWts = new Double[] {1.0,1.0};}
				//turbulence values
				if(useDefaults){
					setProcTxtrVals(new int[]{txtrType, 8, 1, 2, 1, 1}, 			//int[] ints =  txtrType,  numOctaves,  numOverlays,  numPtsDist,distFunc (not used), roiFunc (not used)
							new boolean[]{true,useCustClrs,false}, 					//boolean[] bools = rndColors , useCustClrs, useFwdTrans -> useFwdTrans needs to be specified in command in cli
							new double[]{1.0, .4, 25.0, .3, 1.0, 0.05}, 			//double[] dbls = noiseScale, turbMult , colorScale , colorMult, avgNumPerCell
							new myVector[]{new myVector(MyMathUtils.TWO_PI*3.5,7.9,6.2)}, 	//myVector[] vecs = pdMult
							noiseColors, 											//myColor[] clrs = noiseColors
							clrWts);												//Double[] wts = clrWts					
				} break;}		
			case "marble":{
				txtrType = 4;
				boolean useDefaults = readProcTxtrVals(vals, true);
				if(!useCustClrs){noiseColors = new myRTColor[]{ getClr("clr_nearblack"),	getClr("clr_offwhite")}; clrWts = new Double[] {1.0,1.0};}
				//turbulence values
				if(useDefaults){
				setProcTxtrVals(new int[]{txtrType, 16, 1, 2, 1, 1}, 				//int[] ints =  txtrType,  numOctaves,  numOverlays,  numPtsDist,distFunc (not used), roiFunc (not used)
							new boolean[]{true,useCustClrs,false}, 					//boolean[] bools = rndColors , useCustClrs, useFwdTrans -> useFwdTrans needs to be specified in command in cli
							new double[]{1.0, 15.0, 24.0, .1, 1.0, 0.05}, 			//double[] dbls = noiseScale, turbMult , colorScale , colorMult, avgNumPerCell
							new myVector[]{new myVector(MyMathUtils.TWO_PI * 0.1,MyMathUtils.TWO_PI * 31.4,MyMathUtils.TWO_PI *4.1)}, 	//myVector[] vecs = pdMult
							noiseColors, 											//myColor[] clrs = noiseColors
							clrWts);												//Double[] wts = clrWts					
				} break;}	
			case "marble2":{
				txtrType = 4;
				boolean useDefaults = readProcTxtrVals(vals, true);
				if(!useCustClrs){noiseColors = new myRTColor[]{ getClr("clr_nearblack"),	getClr("clr_offwhite")}; clrWts = new Double[] {1.0,1.0};}
				//turbulence values
				if(useDefaults){
				setProcTxtrVals(new int[]{txtrType, 16, 1, 2, 1, 1}, 				//int[] ints =  txtrType,  numOctaves,  numOverlays,  numPtsDist,distFunc (not used), roiFunc (not used)
							new boolean[]{true,useCustClrs,false}, 					//boolean[] bools = rndColors , useCustClrs, useFwdTrans -> useFwdTrans needs to be specified in command in cli
							new double[]{1.0, 15.0, 24.0, .1, 1.0, 0.05}, 			//double[] dbls = noiseScale, turbMult , colorScale , colorMult, avgNumPerCell
							new myVector[]{new myVector(MyMathUtils.TWO_PI * 0.1,MyMathUtils.TWO_PI * 31.4,MyMathUtils.TWO_PI *4.1)}, 	//myVector[] vecs = pdMult
							noiseColors, 											//myColor[] clrs = noiseColors
							clrWts);												//Double[] wts = clrWts					
				} break;}
			//this uses 
			case "stone":{
				txtrType = 5;
				boolean useDefaults = readProcTxtrVals(vals, false);
				if(!useCustClrs){
					noiseColors = new myRTColor[]{getClr("clr_mortar1"), getClr("clr_mortar2"),
							getClr("clr_brick1_1"),getClr("clr_brick1_2"),getClr("clr_brick2_1"),getClr("clr_brick2_2"),				//"brick2" color 1,2
							getClr("clr_brick3_1"),getClr("clr_brick3_2"),getClr("clr_brick4_1"),getClr("clr_brick4_2")};
					clrWts = new Double[] {1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0};//normalized in txtr
				}
				if(useDefaults){
					setProcTxtrVals(new int[]{txtrType, 8, 1, 2, 1, 1}, 		//int[] ints =  txtrType,  numOctaves,  numOverlays,  numPtsDist, distfunc
							new boolean[]{true,useCustClrs,false}, 				//boolean[] bools = rndColors , useCustClrs, useFwdTrans -> useFwdTrans needs to be specified in command in cli
							new double[]{4.0, 1.0, 12.0, .2, 1.0, 0.05},		//double[] dbls = noiseScale, turbMult , colorScale , colorMult, avgNumPerCell, mortar thresh
							new myVector[]{new myVector(10.0,10.0,10.0)}, 		//myVector[] vecs = pdMult - not used here yet
							noiseColors, 											//myColor[] clrs = noiseColors
							clrWts);											//Double[] wts = clrWts					
				} break;}
			default : {	win.getMsgObj().dispErrorMessage("Base_Scene", "setTexture", "Unknown Texture type : " + _typ); txtrType = 0; return;}
		}
		win.getMsgObj().dispInfoMessage("Base_Scene", "setTexture", "Set Texture type : " + _typ); 
	}//setTexture
	
	
	/**
	 * returns one of a set of predefined colors or a random color as an array of 0-1 doubles based on tag passed
	 * @param colorVal
	 * @return
	 */
	public myRTColor getClr(String colorVal){
		switch (colorVal.toLowerCase()){
			case "clr_rnd"				: { return new myRTColor(ThreadLocalRandom.current().nextDouble(0,1),ThreadLocalRandom.current().nextDouble(0,1),ThreadLocalRandom.current().nextDouble(0,1));}
	    	case "clr_gray"   		    : { return new myRTColor(0.47,0.47,0.47);}
	    	case "clr_white"  		    : { return new myRTColor(1.0,1.0,1.0);}
	    	case "clr_yellow" 		    : { return new myRTColor(1.0,1.0,0);}
	    	case "clr_cyan"			    : { return new myRTColor(0,1.0,1.0);} 
	    	case "clr_magenta"		    : { return new myRTColor(1.0,0,1.0);}  
	    	case "clr_red"    		    : { return new myRTColor(1.0,0,0);} 
	    	case "clr_blue"			    : { return new myRTColor(0,0,1.0);}
	    	case "clr_purple"		    : { return new myRTColor(0.6,0.2,1.0);}
	    	case "clr_green"		    : { return new myRTColor(0,1.0,0);}  
	    	//lower idxs are darker
	    	case "clr_ltwood1"			: { return new myRTColor(0.94, 0.47, 0.12);}  
	    	case "clr_ltwood2"			: { return new myRTColor(0.94, 0.8, 0.4);}  
	    	
	    	case "clr_dkwood1"			: { return new myRTColor(0.2, 0.08, 0.08);}  
	    	case "clr_dkwood2"			: { return new myRTColor(0.3, 0.20, 0.16);}  
	    	
	    	case "clr_mortar1"			: { return new myRTColor(0.2, 0.2, 0.2);}
	    	case "clr_mortar2"			: { return new myRTColor(0.7, 0.7, 0.7);}

	    	case "clr_brick1_1"			: { return new myRTColor(0.6, 0.18, 0.22);}
	    	case "clr_brick1_2"			: { return new myRTColor(0.8, 0.26, 0.33);}
	    	
	    	case "clr_brick2_1"			: { return new myRTColor(0.6, 0.32, 0.16);}
	    	case "clr_brick2_2"			: { return new myRTColor(0.8, 0.45, 0.25);}
	    	
	    	case "clr_brick3_1"			: { return new myRTColor(0.3, 0.01, 0.07);}
	    	case "clr_brick3_2"			: { return new myRTColor(0.6, 0.02, 0.13);}
	    	
	    	case "clr_brick4_1"			: { return new myRTColor(0.4, 0.1, 0.17);}
	    	case "clr_brick4_2"			: { return new myRTColor(0.6, 0.3, 0.13);}
	    	
	    	case "clr_darkgray"   	    : { return new myRTColor(0.31,0.31,0.31);}
	    	case "clr_darkred"    	    : { return new myRTColor(0.47,0,0);}
	    	case "clr_darkblue"  	 	: { return new myRTColor(0,0,0.47);}
	    	case "clr_darkpurple"		: { return new myRTColor(0.4,0.2,0.6);}
	    	case "clr_darkgreen"  	    : { return new myRTColor(0,0.47,0);}
	    	case "clr_darkyellow" 	    : { return new myRTColor(0.47,0.47,0);}
	    	case "clr_darkmagenta"	    : { return new myRTColor(0.47,0,0.47);}
	    	case "clr_darkcyan"   	    : { return new myRTColor(0,0.47,0.47);}	  
	    	
	    	case "clr_lightgray"   	    : { return new myRTColor(0.78,0.78,0.78);}
	    	case "clr_lightred"    	    : { return new myRTColor(1.0,.43,.43);}
	    	case "clr_lightblue"   	    : { return new myRTColor(0.43,0.43,1.0);}
	    	case "clr_lightgreen"  	    : { return new myRTColor(0.43,1.0,0.43);}
	    	case "clr_lightyellow"	    : { return new myRTColor(1.0,1.0,.43);}
	    	case "clr_lightmagenta"	    : { return new myRTColor(1.0,.43,1.0);}
	    	case "clr_lightcyan"   	    : { return new myRTColor(0.43,1.0,1.0);}
	    	
	    	case "clr_black"		    : { return new myRTColor(0,0,0);}
	    	case "clr_nearblack"		: { return new myRTColor(0.05,0.05,0.05);}
	    	case "clr_faintgray" 		: { return new myRTColor(0.43,0.43,0.43);}
	    	case "clr_faintred" 	 	: { return new myRTColor(0.43,0,0);}
	    	case "clr_faintblue" 	 	: { return new myRTColor(0,0,0.43);}
	    	case "clr_faintgreen" 	    : { return new myRTColor(0,0.43,0);}
	    	case "clr_faintyellow" 	    : { return new myRTColor(0.43,0.43,0);}
	    	case "clr_faintcyan"  	    : { return new myRTColor(0,0.43,0.43);}
	    	case "clr_faintmagenta"  	: { return new myRTColor(0.43,0,0.43);}    	
	    	case "clr_offwhite"			: { return new myRTColor(0.95,0.98,0.92);}
	    	default         		    : { win.getMsgObj().dispErrorMessage("Base_Scene", "getClr", "Color not found : " + colorVal + " so using white.");	return new myRTColor(1.0,1.0,1.0);}    
		}//switch
	}//getClr
	
	/**
	 * Set the destination rendered image size
	 * @param numCols
	 * @param numRows
	 */
	private void setImageSize(int numCols, int numRows){//set size and all size-related variables, including image dims
		sceneCols = numCols;
		sceneRows = numRows;
		numPxls = sceneRows * sceneCols;
		rayYOffset = sceneRows/2.0;
		rayXOffset = sceneCols/2.0;
		
		maxDim = MyMathUtils.max(sceneRows,sceneCols);
		yStart = ((maxDim - sceneRows)/2.0) - rayYOffset;	
		xStart = ((maxDim - sceneCols)/2.0) - rayXOffset;			//compensate for # rows or # cols not being max - make sure projection is centered in non-square images
		fishMult = 2.0/maxDim; 
			
		rndrdImg = ((my_procApplet) pa).createImage(sceneCols,sceneRows,PConstants.RGB);
		//Set scene-specific values for when 
		setImageSize_Indiv();
	}
	protected abstract void setImageSize_Indiv();
	
	//refining
	public void setRefine(String refState){
    	curRefineStep = 0;
		setHasGlblRefine(refState.toLowerCase().equals("on"));
		//build refinement #pxls array dynamically by finding average dim of image and then math.
		int refIDX = (int)(Math.log((this.sceneCols + this.sceneRows)/32.0)/ MyMathUtils.LOG_2);
		RefineIDX = new int[(refIDX+1)];
		for(int i =refIDX; i >=0; --i){	RefineIDX[refIDX-i]=Base_RayTracerWin.pow2[i];}
	}//setRefine
		
	public void setDpthOfFld(double lRad, double lFD){				//depth of field effect
		lens_radius = lRad;
		lens_focal_distance = lFD;
		setHasDpthOfFld(true);
		focalPlane.setPlaneVals(0, 0, 1, lens_focal_distance, 1.0);      //needs to be modified so that d = viewZ + lens_focal_distance once FOV has been specified.
	}
	
	public void setNumRaysPerPxl(int _num){
		win.getMsgObj().dispInfoMessage("Base_Scene", "setNumRaysPerPxl", "Num Rays Per Pixel : " + _num);
		this.numRaysPerPixel = _num;
	}
	
	/**
	 * Set current material surface properties
	 * @param Cdiff
	 * @param Camb
	 * @param Cspec
	 * @param phongExp
	 * @param newKRefl
	 */
	public void setSurface(myRTColor Cdiff, myRTColor Camb, myRTColor Cspec, double phongExp, double newKRefl){
		txtrType = 0;			//set to be no texture
		currDiffuseColor.set(Cdiff);
		currAmbientColor.set(Camb);
		currSpecularColor.set(Cspec);
		currPhongExp = phongExp;
		setKRefl(newKRefl,newKRefl,newKRefl,newKRefl);
		setRfrIdx(0, 0, 0, 0);	
		currKTrans = 0;
		globRfrIdx = 0;
		globCurPermClr.set(0,0,0);
	}//setSurface method
	
	/**
	 * Set current material surface properties
	 * @param Cdiff
	 * @param Camb
	 * @param Cspec
	 * @param phongExp
	 * @param KRefl
	 * @param KTrans
	 */
	public void setSurface(myRTColor Cdiff, myRTColor Camb, myRTColor Cspec, double phongExp, double KRefl, double KTrans){
		setSurface(Cdiff,Camb, Cspec,phongExp, KRefl);
		currKTrans = KTrans;
		setRfrIdx(0, 0, 0, 0);	
	}//setSurface method with refractance
	
	/**
	 * Set current material surface properties
	 * @param Cdiff
	 * @param Camb
	 * @param Cspec
	 * @param phongExp
	 * @param KRefl
	 * @param KTrans
	 * @param rfrIdx
	 */
	public void setSurface(myRTColor Cdiff, myRTColor Camb, myRTColor Cspec, double phongExp, double KRefl, double KTrans, double rfrIdx){
		setSurface(Cdiff,Camb, Cspec,phongExp, KRefl, KTrans);
		//set permiability of object to light
		setRfrIdx(rfrIdx, rfrIdx, rfrIdx,rfrIdx);
	}//setSurface method with refractance
	public void setDepth(double depth){  currDepth = depth;}//subsurface depth - TODO
	
	public void setPhong(double newPhong){ currPhongExp = newPhong;}
	public void setKTrans(double newKTrans){  currKTrans = newKTrans;}
	public void setRfrIdx(double rfrIdx){setRfrIdx(rfrIdx, rfrIdx, rfrIdx, rfrIdx);}
	public void setRfrIdx(double rfrIdx, double pR, double pG, double pB){
		globRfrIdx = rfrIdx;
		//can control color "tint" of transmitted ray through object.  for now all the same
		globCurPermClr.set(pR,pG,pB);
	}
	
	public void setKRefl(double newKRefl){setKRefl(newKRefl,newKRefl,newKRefl,newKRefl);}
	public void setKRefl(double newKRefl, double kr, double kg, double kb){
	  currKRefl  = newKRefl;
	  //can control color "tint" of transmitted ray through object		
	  currKReflClr.set(kr,kg,kb);
	}
	public void setBackgroundColor(double r, double g, double b){  backgroundColor.set(r,g,b);}//initBackground 3 color method
	////////
	///end setting values
	////////
	
	///////
	//RT functionality
	///////	
	/**
	 * get random location within "lens" for depth of field calculation - consider loc to be center, pick random point in constant z plane within some radius of loc point
	 * @param loc
	 * @return
	 */
	public myPoint getDpthOfFldEyeLoc(myPoint loc){
		//myVector tmp = p.rotVecAroundAxis(new myVector(0,1,0),new myVector(0,0,-1),ThreadLocalRandom.current().nextDouble(0,MyMathUtils.TWO_PI));				//rotate surfTangent by random angle
		myVector tmp1 = new myVector(0,1,0);
		myVector tmp = tmp1.rotMeAroundAxis(new myVector(0,0,-1),ThreadLocalRandom.current().nextDouble(0,MyMathUtils.TWO_PI));				//rotate surfTangent by random angle
		tmp._normalize();
		double mult = ThreadLocalRandom.current().nextDouble(0,lens_radius);			//find displacement radius from origin
		tmp._mult(mult);
		tmp._add(loc);																														//find displacement point on origin
		return tmp;		
	}
	//determines if a light source is blocked by another object for shadow detection
	//currently only returns 1 or 0 if light is blocked
	
	public int calcShadow(rayCast _ray, double distToLight){
		//for each object in scene, check if intersecting any objects before hitting light
		for (Base_Geometry obj : objList){
			if(obj.calcShadowHit(_ray, _ray.getTransformedRay(_ray, obj.CTMara[Base_Geometry.invIDX]), obj.CTMara, distToLight) == 1){	return 1;}
		}//for each object in scene
		return 0;
	}//findLight method
	
	//eventually multithread/shdr per object?
	public rayHit findClosestRayHit(rayCast _ray){
		//objList does not hold lights - no need to check pointlights - TODO need to check lights for non-point lights- ?	
		TreeMap<rayHit, Base_Geometry>objsAtRayHits = new TreeMap<rayHit,Base_Geometry>();
		objsAtRayHits.put(new rayHit(false), null);
		//myRay transRay;
		for (Base_Geometry obj : objList){	
//			rayHit _hit = null;
//			try{
			rayHit _hit = obj.intersectCheck(_ray,_ray.getTransformedRay(_ray, obj.CTMara[Base_Geometry.invIDX]),obj.CTMara);		
//			} catch (Exception e){
//				win.getMsgObj().dispErrorMessage("Base_Scene", "findClosestRayHit", "exception :\n"+e);
//			}
			if(_hit.isHit){			objsAtRayHits.put(_hit, _hit.obj);		}
		}//for obj in scenelist
		return objsAtRayHits.firstKey();
	}//findClosestRayHit
	
	
	//determine color of a reflected ray - careful with recursive depth  
	public myRTColor reflectRay(rayCast _ray){
		rayHit hitChk = findClosestRayHit(_ray);
		//if ((hitChk.isHit)) {										return(hitChk.obj.getColorAtPos(hitChk));}//to debug BVH use this - displays colors of leaf boxes (red/blue)
		if (hitChk.isHit) {											return(hitChk.shdr.getColorAtPos(hitChk));}
		else if (hasGlblTxtrdBkg()) {								return getBackgroundTextureColor(_ray);	} 	//using skydome
//		else if ((_ray.direction.z > MyMathUtils.EPS) && (scFlags[useFGColorIDX])){	return foregroundColor;	} 					//for getting color reflected from behind viewer
		else {														return backgroundColor;	}
	}//reflectRay	
	
	
	///////////////
	//setup photon handling kdtree
	public void setPhotonHandling(String[] token){
		setUsePhotonMap(true);
		setIsPhtnMapRndrd(false);
		String type = token[0];
		setIsCausticPhtn(type.contains("caustic"));
		//type is "caustic_photons" or "diffuse_photons"
		//caustic_photons 80000000  80 0.05
		numPhotons = Integer.parseInt(token[1]);
		kNhood = Integer.parseInt(token[2]);
		photonMaxNearDist = Float.parseFloat(token[3]);
		float sqDist = photonMaxNearDist*photonMaxNearDist;
		win.getMsgObj().dispInfoMessage("Base_Scene", "setPhotonHandling", "# photons : "+ numPhotons+ " Hood size :"+kNhood+" Max Near Dist :" +photonMaxNearDist);
		//build photon tree
		photonTree = new Photon_KDTree(numPhotons, kNhood, sqDist);
	}
	//final gather procedure
	public void setFinalGather(String[] token){
		numGatherRays = Integer.parseInt(token[1]);
	}
	/////////////

	//from a hit on an object, return reflection dir
  	public myVector getReflDir(rayHit hit){
  		myVector backToEyeDir = new myVector(hit.fwdTransRayDir);  		
  		backToEyeDir._mult(-1);  		
 		double dotProd = 2 * (backToEyeDir._dot(hit.objNorm));
  		myVector tempVect = new myVector(hit.objNorm.x * dotProd,hit.objNorm.y * dotProd,hit.objNorm.z * dotProd);
  		myVector reflDir = new myVector(backToEyeDir,tempVect);
  		reflDir._normalize();
  		return reflDir;  
  	}//getReflDir
  				
  	private void _buildKDTree(String _type) {
		win.getMsgObj().dispInfoMessage("Base_Scene", "_buildKDTree", "Building KD Tree for "+_type+" Photons");
		photonTree.buildKDTree();
		win.getMsgObj().dispInfoMessage("Base_Scene", "_buildKDTree", "KD Tree Built for "+_type+" Photons"); 		
  	}
  	
  	//TODO set  up seperate maps for caustic and diffuse photons
	//send out photons for all lights if we are using photon mapping - put these photons in kd tree
	//need different photon configurations/processes for caustic photons and indirect illumination (diffuse)photons
	protected void sendCausticPhotons(){
		int numDiv = 100,lastCastCnt = 0, starCount = 0, pctCastCnt = numDiv/10;
		int numCastPerDisp = photonTree.numCast/numDiv;
		Base_Light tmpLight; 
		double pwrMult = causticsLightPwrMult/photonTree.numCast;
		rayCast reflRefrRay;
		double[] tmpPwr,photonPwr;
		rayHit hitChk;
		myPhoton phn;
		//TODO scale # of photons sent into scene by light intensity
		for(Base_Geometry light : lightList){//either a light or an instance of a light
			tmpLight = (light instanceof Base_Light) ? tmpLight = (Base_Light)light : ((Base_Light)((ObjInstance)light).obj);
			System.out.print("Casting " + photonTree.numCast + " Caustic photons for light ID " + tmpLight.ID + ": Progress:");			
			for(int i =0; i<photonTree.numCast; ++i){
				photonPwr = new double[]{tmpLight.lightColor.x * pwrMult,tmpLight.lightColor.y * pwrMult,tmpLight.lightColor.z * pwrMult };
				hitChk = findClosestRayHit(tmpLight.genRndPhtnRay());//first hit
				if((!hitChk.isHit) || (!hitChk.shdr.getHasCaustic())){continue;}			//either hit background or 1st hit is diffuse object - caustic has to hit spec first
				//System.out.println("hit obj : " + hitChk.obj.ID);
				//we have first hit here at caustic-generating surface.  need to propagate through to first diffuse surface
				hitChk.phtnPwr = photonPwr;
				do{
		 			reflRefrRay = hitChk.shdr.findCausticRayHit(hitChk,hitChk.phtnPwr);
		 			if(reflRefrRay != null){	
		 				tmpPwr = new double[]{hitChk.phtnPwr[0],hitChk.phtnPwr[1],hitChk.phtnPwr[2]};
		 				hitChk = findClosestRayHit(reflRefrRay);
		 				hitChk.phtnPwr = tmpPwr;
		 			} else {					hitChk.isHit = false;}
				} while((hitChk.isHit) && (hitChk.shdr.getHasCaustic()) && (reflRefrRay.gen <= numPhotonRays));				//keep going while we have a hit and we are hitting a caustic
				if((!hitChk.isHit) || (reflRefrRay.gen > numPhotonRays)){continue;}																//bounced off into space
				
				//d = hitChk.fwdTransRayDir._normalized().getAsAra();				
				//phn = new myPhoton(photonTree, hitChk.phtnPwr, hitChk.fwdTransHitLoc, Math.acos(d[2]), PConstants.PI + Math.atan2(d[1], d[0])); 	
				//phn = new myPhoton(photonTree, photonPwr, hitChk.fwdTransHitLoc.x,  hitChk.fwdTransHitLoc.y,  hitChk.fwdTransHitLoc.z); 	
				phn = new myPhoton(photonTree, hitChk.phtnPwr, hitChk.fwdTransHitLoc.x,  hitChk.fwdTransHitLoc.y,  hitChk.fwdTransHitLoc.z); 	
				photonTree.addKDObject(phn);
				//this just calcs when to display progress bar, can be deleted
				if(i > lastCastCnt){
					lastCastCnt += numCastPerDisp;					
					System.out.print((starCount % pctCastCnt == 0) ? (10.0*starCount/pctCastCnt) + "%" : "*");
					starCount++;
				}
			}//for each photon of light
			System.out.println("100.0%");
			starCount = 0;lastCastCnt=0;
		}//for each light
		_buildKDTree("caustic");
	}//sendCausticPhotons
	
	protected void sendDiffusePhotons(){
		//for every light
		//for each photon of n, 
		//for every object
		//check if hit, save where lands
		int numDiv = 100,lastCastCnt = 0, starCount = 0, pctCastCnt = numDiv/10;
		int numCastPerDisp = photonTree.numCast/numDiv;
		Base_Light tmpLight; 
		double pwrMult = diffuseLightPwrMult/photonTree.numCast;
		rayCast reflRefrRay;
		double[] tmpPwr,photonPwr;
		rayHit hitChk;
		myPhoton phn;
		//TODO scale # of photons sent into scene by light intensity
		for(Base_Geometry light : lightList){//either a light or an instance of a light
			tmpLight = (light instanceof Base_Light) ? tmpLight = (Base_Light)light : ((Base_Light)((ObjInstance)light).obj);
			System.out.print("Casting " + photonTree.numCast + " Diffuse (indirect) photons for light ID " + tmpLight.ID + ": Progress:");			
			for(int i =0; i<photonTree.numCast; ++i){
				photonPwr = new double[]{tmpLight.lightColor.x * pwrMult,tmpLight.lightColor.y * pwrMult,tmpLight.lightColor.z * pwrMult };
				hitChk = findClosestRayHit(tmpLight.genRndPhtnRay());//first hit
				if(!hitChk.isHit){continue;}							//hit background - ignore
				//now we hit an object, spec or diffuse - if specular, bounce without storing, if diffuse store and bounce with prob based on avg color				
				//System.out.println("hit obj : " + hitChk.obj.ID);
				//we have first hit here at caustic-generating surface.  need to propagate through to first diffuse surface
				hitChk.phtnPwr = photonPwr;
				boolean done = false, firstDiff = true;
				do{
					if(hitChk.shdr.KRefl == 0){//diffuse, store and maybe bounce
						double prob = 0;
						if(!firstDiff){//don't store first
							phn = new myPhoton(photonTree, hitChk.phtnPwr, hitChk.fwdTransHitLoc.x,  hitChk.fwdTransHitLoc.y,  hitChk.fwdTransHitLoc.z); 	
							photonTree.addKDObject(phn);
							prob = ThreadLocalRandom.current().nextDouble(0,1.0);//russian roulette to see if casting
						}
						firstDiff = false;
						if(prob < hitChk.shdr.avgDiffClr){	//reflect in new random dir, scale phtn power by diffClr/avgClr
							//get new bounce dir
							myPoint hitLoc = hitChk.fwdTransHitLoc;
					  		//first calc random x,y,z
					  		double x=0,y=0,z=0, sqmag;
							do{
								x = ThreadLocalRandom.current().nextDouble(-1.0,1.0);
								y = ThreadLocalRandom.current().nextDouble(-1.0,1.0);			
								sqmag = (x*x) + (y*y);
							}
							while ((sqmag >= 1.0) || (sqmag < MyMathUtils.EPS));
							z = Math.sqrt(1 - sqmag);							//cosine weighting preserved by projecting up to sphere
							//then build ortho basis from normal - n' , q' , r' 
					  		myVector n = new myVector(hitChk.objNorm),_p = new myVector(),_q = new myVector(); 
					  				//tmpV = (((n.x > n.y) && (n.x > n.z)) || ((-n.x > -n.y) && (-n.x > -n.z))  ? new myVector(0,0,1)  : new myVector(1,0,0));//find vector not close to n or -n to use to find tangent
							double nxSq = n.x * n.x, nySq = n.y * n.y, nzSq = n.z * n.z;
							myVector tmpV = (((nxSq > nySq) && (nxSq > nzSq))  ? new myVector(0,0,1)  : new myVector(1,0,0));//find vector not close to n or -n to use to find tangent
					  		//set _p to be tangent, _q to be binorm
					  		_p = n._cross(tmpV);	_q = _p._cross(n);
					  		//if(_p.sqMagn < p.MyMathUtils.EPS){System.out.println("bad _p : " + _p + " | n : " + n + " | tmpV : " + tmpV);}
					  		//lastly multiply ortho basis vectors by x,y,z : x * p, y * q', z*n', and then sum these products - z is projection/hemisphere dir, so should coincide with normal
					  		n._mult(z);	_p._mult(x);_q._mult(y);
					  		myVector bounceDir = new myVector(n.x + _p.x + _q.x,n.y + _p.y + _q.y,n.z + _p.z + _q.z);
					  		bounceDir._normalize();
					 		//save power before finding ray hit, to reset it after ray hit
					  		tmpPwr = new double[]{hitChk.phtnPwr[0]*hitChk.shdr.phtnDiffScl.x,hitChk.phtnPwr[1]*hitChk.shdr.phtnDiffScl.y,hitChk.phtnPwr[2]*hitChk.shdr.phtnDiffScl.z};			//hitChk changes below, we want to propagate tmpPwr
					 		//new photon ray - photon power : 
			 				reflRefrRay = new rayCast(this, hitLoc, bounceDir, hitChk.transRay.gen+1);
			 				hitChk = findClosestRayHit(reflRefrRay);
			 				hitChk.phtnPwr = tmpPwr;
						} else {	done = true;}						
					} else {					//specular, just bounce, scale power by avg spec power (?)
			 			reflRefrRay = hitChk.shdr.findCausticRayHit(hitChk,hitChk.phtnPwr);
			 			if(reflRefrRay != null){	
			 				tmpPwr = new double[]{hitChk.phtnPwr[0],hitChk.phtnPwr[1],hitChk.phtnPwr[2]};//save updated photon power
			 				hitChk = findClosestRayHit(reflRefrRay);
			 				hitChk.phtnPwr = tmpPwr;
			 			} else {					hitChk.isHit = false;}
					}
				} while((hitChk.isHit) && (!done) && (hitChk.transRay.gen <= numPhotonRays));				//keep going while we have a hit and we are less than ray recursion depth
				if((!hitChk.isHit) || (hitChk.transRay.gen > numPhotonRays)){continue;}		//bounced off into space
				
				//d = hitChk.fwdTransRayDir._normalized().getAsAra();			//direction of ray hit, for anisotropic materials (TODO)			
				//phn = new myPhoton(photonTree, hitChk.phtnPwr, hitChk.fwdTransHitLoc, Math.acos(d[2]), PConstants.PI + Math.atan2(d[1], d[0])); 	
				//phn = new myPhoton(photonTree, photonPwr, hitChk.fwdTransHitLoc.x,  hitChk.fwdTransHitLoc.y,  hitChk.fwdTransHitLoc.z); 	
				//this just calcs when to display progress bar, can be deleted
				if(i > lastCastCnt){
					lastCastCnt += numCastPerDisp;					
					System.out.print((starCount % pctCastCnt == 0) ? (10.0*starCount/pctCastCnt) + "%" : "*");
					starCount++;
				}
			}
			System.out.println("100.0%");
			starCount = 0;lastCastCnt=0;
		}
		_buildKDTree("diffuse");
	}//sendDiffusePhotons
	
	//initialize drawing routine - build photon map if it exists
	protected void initRender(){
		rndrdImg.loadPixels();
		if ((usePhotonMap()) && (!isPhtnMapRndrd())){	
			if (isCausticPhtn()) {sendCausticPhotons(); } 
			else {sendDiffusePhotons();setIsPhtnMapRndrd(true);}}		
	}//initRender	
	
	public boolean isSameSize(int numCols, int numRows) {
		return (numCols == sceneCols) && (numRows == sceneRows);
	}
	
	/**
	 * Resizing the image from UI input - will redraw right after new size
	 * @param numCols
	 * @param numRows
	 */
	public void setNewSize(int numCols, int numRows) {
		setImageSize(numCols, numRows);
		curRefineStep = 0;
		reflRays = 0;
		refrRays = 0;
		globRayCount = 0;
		if (usePhotonMap()) {
			rebuildPhotonTree();
		}
		setSaveImage(true);				//save image with new size
		setRendered(false);
	}//setNewSize
	
	private void rebuildPhotonTree() {
		//build photon tree
		photonTree = new Photon_KDTree(numPhotons, kNhood, photonMaxNearDist*photonMaxNearDist);			
		
	}
	
	/////////////
	////skydome stuff - move to sphere code, set flag for internal normals TODO
	/**
	 * find corresponding u and v values for background texture	
	 * @param transRay
	 * @return
	 */
	private double findSkyDomeT(rayCast transRay){
		//similar to intersection of known direction vectors to lights
		//this code finds t of where passed ray hits mySkyDome edge
		double t = -Double.MAX_VALUE;  //this t is the value of the ray equation where it hits the dome - init to bogus value	  
		//find t for intersection : 
		myPoint pC = mySkyDome.originRadCalc(transRay);
		double a = mySkyDome.getAVal(transRay), b = mySkyDome.getBVal(transRay,pC), c = mySkyDome.getCVal(transRay,pC);	    
		double discr = ((b * b) - (4 * a * c));
		//quadratic - check first if imaginary - if so then no intersection
		if (discr >= 0){     
			double discr1 = Math.pow(discr,.5), t1 = (-1*b + discr1)/ (2*a), t2 = (-1*b - discr1)/ (2*a), tVal = Math.min(t1,t2);
			if (tVal < MyMathUtils.EPS){tVal = Math.max(t1,t2);}//if the min t val is less than 0
			t = tVal;
		}//if positive t
		else {
			//should never get here - any ray that is sent to this function hasn't intersected with any other
			win.getMsgObj().dispErrorMessage("Base_Scene", "findSkyDomeT", "Error - non-colliding ray doesn't hit sky dome.  b^2 - 4ac , eval : "+ b + "^2 - 4 " + a + "*" + c + " : " + discr);
		}
		return t;
	}//findSkjDomeT func
	
	public myRTColor getBackgroundTextureColor(rayCast ray){
		double t = findSkyDomeT(ray);
		myPoint isctPt = ray.pointOnRay(t);
		double[] bkgTxtrUV = mySkyDome.findTxtrCoords(isctPt, (((myImageTexture)mySkyDome.shdr.txtr).myTextureBottom), t);
		//double v = findBkgTextureV(isctPt, t), u = findBkgTextureU(isctPt, v, t);
		double u = bkgTxtrUV[0], v = bkgTxtrUV[1];
		return new myRTColor(((myImageTexture)mySkyDome.shdr.txtr).myTextureBottom.pixels[(int)v * ((myImageTexture)mySkyDome.shdr.txtr).myTextureBottom.width + (int)u]);
	}//getBackgroundTexturecolor
	
	//////////////////
	//flip the normal directions for this scene
	public void flipNormal(){
		setFlipNorms(!doFlipNorms());
		setRendered(false);				//so will be re-rendered
		setSaveImage(true);				//save image with flipped norm
		curRefineStep = 0;
		reflRays = 0;
		refrRays = 0;
		globRayCount = 0;
		rebuildPhotonTree();
		for (Base_Geometry obj : objList){//set for all scene objects or instances of sceneobjects
			if(obj instanceof Base_SceneObject){((Base_SceneObject)obj).setIsInverted(doFlipNorms());}//either a scene object or an instance of a scene object
			else {if(obj instanceof ObjInstance && ((ObjInstance)obj).obj instanceof Base_SceneObject){((Base_SceneObject)((ObjInstance)obj).obj).setIsInverted(doFlipNorms());}}
		}
	}//flipNormal	
	//return abs vals of vector as vector
	public myVector absVec(myVector _v){return new myVector(Math.abs(_v.x),Math.abs(_v.y),Math.abs(_v.z));}

	public double perlinNoise3D(myPoint pt){return win.perlinNoise3D((float)pt.x, (float)pt.y, (float)pt.z);}
	/**
	 * Derive Perlin noise value at a particular location
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public float perlinNoise3D(float x, float y, float z) {return win.perlinNoise3D(x,y,z);}//noise_3d

	
	//utility functions
	/**
	 * find signed area of enclosed poly, with given points and normal N
	 * @param _pts
	 * @param N
	 * @return
	 */
	public double calcArea(myVector[] _pts, myVector N){
	    double res = 0;	    
	    if (_pts.length < 3) return 0; 
	    myVector absN = absVec(N);			//find abs val of each coord - want to project to plane normal to biggest coord to find area, and then scale by biggest coord	
	    if((absN.x > absN.y) && (absN.x > absN.z)){//x is max coord
	        for (int i=1, j=2, k=0; i<_pts.length; ++i, ++j, ++k){     res += (_pts[i].y * (_pts[(j%_pts.length)].z - _pts[k].z));}
	        res += (_pts[0].y * (_pts[1].z - _pts[_pts.length-1].z));	     
	        return (res / (2.0f * N.x));    		    	
	    } else if ((absN.y > absN.x) && (absN.x > absN.z)){//y is max coord
	    	for (int i=1, j=2, k=0; i<_pts.length; ++i, ++j, ++k){     res += (_pts[i].z * (_pts[(j%_pts.length)].x - _pts[k].x));}
	        res += (_pts[0].z * (_pts[1].x - _pts[_pts.length-1].x));
	        return (res / (2.0f * N.y));
	    } else {//z is max coord
	    	for (int i=1, j=2, k=0; i<_pts.length; ++i, ++j, ++k){     res += (_pts[i].x * (_pts[(j%_pts.length)].y - _pts[k].y));}
	        res += (_pts[0].x * (_pts[1].y - _pts[_pts.length-1].y));
	        //return (res * (1 / (2.0f * N.z)));   	
	        return (res / (2.0f * N.z));   	
	    }
    }//area	
	
	//////////////////////
	//draw utility functions
	//////////////////////
	/**
	 * write span of pixels with same value, for iterative refinement
	 * @param clrInt
	 * @param row
	 * @param col
	 * @param _numPxls
	 * @param pxls
	 * @return
	 */
	public int writePxlSpan(int clrInt, int row, int col, int _numPxls, int[] pxls){
		int pxlIDX = (row*sceneCols) + col;								//idx of pxl in pxls ara
		int rowPxlCnt, rowStart = row, rowEnd = MyMathUtils.min(rowStart + _numPxls, sceneRows ),//dont try to go beyond pxl array dims
			colStart = col, colEnd = MyMathUtils.min(colStart + _numPxls, sceneCols);
		for(int pxlR = rowStart; pxlR < rowEnd; ++pxlR){rowPxlCnt = (pxlR*sceneCols);	for(int pxlC = colStart; pxlC < colEnd; ++pxlC){pxls[(rowPxlCnt + pxlC)] = clrInt;}}		
		return pxlIDX;
	}//writePxlSpan
	
	//instance scene-specific 
	//public abstract myColor calcAAColor(double pRayX, double pRayY, double xIncr, double yIncr);
	public abstract myRTColor shootMultiRays(double pRayX, double pRayY);
	
	public final void draw() {
		//if not rendered yet, render and then draw
		if (!isRendered()){	
			initRender();
			int stepIter = 1;
			boolean skipPxl = false;
			if(useGlblRefine()){
				stepIter = RefineIDX[curRefineStep++];
				skipPxl = curRefineStep != 1;			//skip 0,0 pxl on all sub-images except the first pass
			} 
			if(stepIter == 1){setRendered(true);			}
			
			//Render specific scene
			renderScene(stepIter, skipPxl, rndrdImg.pixels);			
			//Finish up
			//update the display based on the pixels array
			rndrdImg.updatePixels();
			if(isRendered()){	finishImage();	}
		}
		((my_procApplet) pa).imageMode(PConstants.CORNER);
		((my_procApplet) pa).image(rndrdImg,0,0);	
	}
	
	protected abstract void renderScene(int stepIter, boolean skipPxl, int[] pixels);
	
	
	public void setSaveName(String _saveName) {saveName = _saveName;}
	/**
	 * file save
	 */
	private void saveFile(){
		String tmpSaveName;
		String[] tmp = saveName.split("\\.(?=[^\\.]+$)");				//remove extension from given savename
		if (saveImageInDir()){	tmpSaveName = folderName.toString() + "\\"  + tmp[0]+(doFlipNorms() ? "_normFlipped" : "")+ ".png";} //rebuild name to include directory and image name including render time
		else {							tmpSaveName = tmp[0]+(doFlipNorms() ? "_normFlipped" : "")+".png";		}
		win.getMsgObj().dispInfoMessage("Base_Scene", "saveFile", "File saved as  : "+ tmpSaveName);
		rndrdImg.save(tmpSaveName);
		setSaveImage(false);//don't keep saving every frame
	}//save image
	  
	/**
	 * common finalizing for all rendering methods
	 */
	protected void finishImage(){
		if (saveImage()){		saveFile();	}//if savefile is true, save the file
		else {win.getMsgObj().dispInfoMessage("Base_Scene", "finishImage", "Apparently not saving this file : "+saveName);}
		String dispStr = "";
		if (showObjInfo()){
			
			for (Base_Geometry obj : allObjsToFind){
				dispStr += obj.toString() +"\n";
	     		if(obj instanceof Base_SceneObject){
		     		if (((Base_SceneObject)obj).shdr.txtr.getHasTxtrdTop()){
		     			dispStr += ((Base_SceneObject)obj).showUV() +"\n";
		     		}//if textured
	     		} else if(obj instanceof ObjInstance){
		     		ObjInstance inst = (ObjInstance)obj;
		     		if ((inst.obj instanceof Base_SceneObject) && (((Base_SceneObject)inst.obj).shdr.txtr.getHasTxtrdTop())){			//TODO need to modify this when using instanced polys having textures - each instance will need a notion of where it should sample from
		     			dispStr += ((Base_SceneObject)inst.obj).showUV()+"\n";
		     		}	     	
		     	}
	     		dispStr += "_________________________________________________________________________\n";
	     	}
			win.getMsgObj().dispMultiLineInfoMessage("Base_Scene", "finishImage", dispStr);
		}//for objects and instances, to print out info
		if (hasGlblTxtrdBkg()){
			dispStr += "\nBackground : " + mySkyDome.showUV() + "\n";  
		}
		dispStr += "Total # of rays : " + globRayCount + " | refl/refr rays " + reflRays +"/" + refrRays + "\n\nImage rendered from file name : " + saveName;
		win.getMsgObj().dispMultiLineInfoMessage("Base_Scene", "finishImage", dispStr);
	}
	
	
	/**
	 * base class flags init
	 */
	public final void initFlags(){scFlags = new int[1 + numFlags/32];for(int i =0; i<numFlags;++i){setFlags(i,false);}}			
	/**
	 * get baseclass flag
	 * @param idx
	 * @return
	 */
	public final boolean getFlags(int idx){int bitLoc = 1<<(idx%32);return (scFlags[idx/32] & bitLoc) == bitLoc;}	
	
	/**
	 * check list of flags
	 * @param idxs
	 * @return
	 */
	public final boolean getAllFlags(int [] idxs){int bitLoc; for(int idx =0;idx<idxs.length;++idx){bitLoc = 1<<(idx%32);if ((scFlags[idx/32] & bitLoc) != bitLoc){return false;}} return true;}
	public final boolean getAnyFlags(int [] idxs){int bitLoc; for(int idx =0;idx<idxs.length;++idx){bitLoc = 1<<(idx%32);if ((scFlags[idx/32] & bitLoc) == bitLoc){return true;}} return false;}

	public final boolean getDebug() {return getFlags(debugIDX);}
	public final boolean isRendered() {return getFlags(renderedIDX);}
	public final boolean saveImage() {return getFlags(saveImageIDX);}
	public final boolean saveImageInDir() {return getFlags(saveImgInDirIDX);}
	public final boolean doSimpleRefr() {return getFlags(simpleRefrIDX);}
	public final boolean doFlipNorms() {return getFlags(flipNormsIDX);}
	public final boolean hasDpthOfFld() {return getFlags(hasDpthOfFldIDX);}
	public final boolean showObjInfo() {return getFlags(showObjInfoIDX);}
	public final boolean doAddToTmpList() {return getFlags(addToTmpListIDX);}
	public final boolean doTimedRender() {return getFlags(timeRndrIDX);}
	public final boolean hasGlblTxtrdBkg() {return getFlags(glblTxtrdBkgIDX);}
	public final boolean useGlblRefine() {return getFlags(glblRefineIDX);}
	public final boolean useFGColor() {return getFlags(useFGColorIDX);}
	public final boolean hasGlblTxtrdTop() {return getFlags(glblTxtrdTopIDX);}
	public final boolean hasGlblTxtrdBtm() {return getFlags(glblTxtrdBtmIDX);}
	public final boolean usePhotonMap() {return getFlags(usePhotonMapIDX);}
	public final boolean isCausticPhtn() {return getFlags(isCausticPhtnIDX);}
	public final boolean isPhtnMapRndrd() {return getFlags(isPhtnMapRndrdIDX);}
	public final boolean doFinalGather() {return getFlags(doFinalGatherIDX);}
	

	public final void setDebug(boolean _val) {setFlags(debugIDX, _val);}
	public final void setRendered(boolean _val) {setFlags(renderedIDX, _val);}
	public final void setSaveImage(boolean _val) {setFlags(saveImageIDX, _val);}
	public final void setSaveImageInDir(boolean _val) {setFlags(saveImgInDirIDX, _val);}
	public final void setHasSimpleRefr(boolean _val) {setFlags(simpleRefrIDX, _val);}
	public final void setFlipNorms(boolean _val) {setFlags(flipNormsIDX, _val);}
	public final void setHasDpthOfFld(boolean _val) {setFlags(hasDpthOfFldIDX, _val);}
	public final void setShowObjInfo(boolean _val) {setFlags(showObjInfoIDX, _val);}
	public final void setAddToTmpList(boolean _val) {setFlags(addToTmpListIDX, _val);}
	public final void setTimedRender(boolean _val) {setFlags(timeRndrIDX, _val);}
	public final void setHasGlblTxtrdBkg(boolean _val) {setFlags(glblTxtrdBkgIDX, _val);}
	public final void setHasGlblRefine(boolean _val) {setFlags(glblRefineIDX, _val);}
	public final void setUseFGColor(boolean _val) {setFlags(useFGColorIDX, _val);}
	public final void setHasGlblTxtrdTop(boolean _val) {setFlags(glblTxtrdTopIDX, _val);}
	public final void setHasGlblTxtrdBtm(boolean _val) {setFlags(glblTxtrdBtmIDX, _val);}
	public final void setUsePhotonMap(boolean _val) {setFlags(usePhotonMapIDX, _val);}
	public final void setIsCausticPhtn(boolean _val) {setFlags(isCausticPhtnIDX, _val);}
	public final void setIsPhtnMapRndrd(boolean _val) {setFlags(isPhtnMapRndrdIDX, _val);}
	public final void setDoFinalGather(boolean _val) {setFlags(doFinalGatherIDX, _val);}
	
	/**
	 * set baseclass flags  //setFlags(showIDX, 
	 * @param idx
	 * @param val
	 */
	public final void setFlags(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		scFlags[flIDX] = (val ?  scFlags[flIDX] | mask : scFlags[flIDX] & ~mask);
		switch(idx){			
			case debugIDX			:{break;}//enable debug functionality
			case renderedIDX 		:{break;}//this scene has been rendered since any changes were made
			case saveImageIDX		:{break;}//whether or not to save an image
			case saveImgInDirIDX	:{break;}	//save image inside specific image directory, rather than root
			case simpleRefrIDX		:{break;}//whether scene should use simplified refraction (intended to minimize refactorization requirement in mySceneObject)
			case flipNormsIDX		:{break;}//whether or not we should flip the normal directions in this scene
			case hasDpthOfFldIDX	:{break;}	//using depth of field
			case showObjInfoIDX 	:{break;}	//print out object info after rendering image
			case addToTmpListIDX	:{break;}	//add object to the temp list, so that the objects will be added to some accel structure.
			case timeRndrIDX		:{break;}	//time the length of rendering and display the results.
			case glblTxtrdBkgIDX	:{break;}	//whether the background is to be textured
			case glblRefineIDX 		:{break;}//whether scene should be rendered using iterative refinement technique
			case useFGColorIDX		:{break;}//whether or not to use a foregroundcolor in this scene
	        case glblTxtrdTopIDX	:{break;}	//whether the currently loading object should be txtred on the top
			case glblTxtrdBtmIDX	:{break;}	//whether the currently loading object should be txtred on the bottom
		    case usePhotonMapIDX	:{break;}	//whether to use a photon map for caustics, indirect illumination
			case isCausticPhtnIDX	:{break;}//whether to use caustic photons or indirect illumination photons (hacky)
			case isPhtnMapRndrdIDX	:{break;}//whether or not photons have been cast yet
			case doFinalGatherIDX	:{break;}//whether to do final gather
			default :{
				
			}
		}				
	}//setFlags
	
	public void dispInfoMessage(String _class, String _method, String _message) {
		win.getMsgObj().dispInfoMessage(_class, _method, _message);
	}
	
	
	public void dispWarningMessage(String _class, String _method, String _message) {
		win.getMsgObj().dispWarningMessage(_class, _method, _message);
	}
	
	
	public void dispErrorMessage(String _class, String _method, String _message) {
		win.getMsgObj().dispErrorMessage(_class, _method, _message);
	}
	
	/**
	*  build translate, scale and rotation matricies to use for scene descriptions
	*  will apply inverses of these matricies to generated ray so that object is rendered in appropriate manner :
	*
	*  so if object A is subjected to a translate/rotate/scale sequence to render A' then to implement this we need to 
	*  subject the rays attempting to intersect with it by the inverse of these operations to find which rays will actually intersect with it.
	*/
	///////
	//transformation stack stuff - uses "gt" prefix instead of "gl" because cute.
	//
	 public void gtDebugStack(String caller){ win.getMsgObj().dispMultiLineInfoMessage("Base_Scene", "gtDebugStack", "Caller : "+caller + "\nCurrent stack status : \n"+matrixStack.toString()); }//gtdebugStack method

	 private void gtInitialize() {
		 currMatrixDepthIDX = 0;
		 matrixStack = new myMatStack(this.matStackMaxHeight);
		 matrixStack.initStackLocation(0);
	 }//gtInitialize method

	public void gtPushMatrix() {
		if (currMatrixDepthIDX < matStackMaxHeight){
	    	matrixStack.push();
	    	++currMatrixDepthIDX;
		} else {	win.getMsgObj().dispErrorMessage("Base_Scene","gtPushMatrix","Error, matrix depth maximum " + matStackMaxHeight + " exceeded");	}	  
	}//gtPushMatrix method

	public void gtPopMatrix() { 
		if (matrixStack.top == 0){win.getMsgObj().dispErrorMessage("Base_Scene","gtPopMatrix","Error : Cannot pop the last matrix in the matrix stack");} 
		else {		//temp was last matrix at top of stack - referencing only for debugging purposes
			@SuppressWarnings("unused")
			myMatrix temp = matrixStack.pop();
			--currMatrixDepthIDX;
		}
	}//gtPopMatrix method
	
	
	public myMatrix gtPeekMatrix() {return matrixStack.peek();}

	public void gtTranslate(double tx, double ty, double tz) { 
		matrixStack.translate(tx, ty, tz);
	}//gtTranslate method

	public void gtScale(double sx, double sy, double sz) {
		matrixStack.scale(sx, sy, sz);
	}//gtScale method

	/**
	*  sets a rotation matrix to be in "angle" degrees CCW around the axis given by ax,ay,az
	*  and multiples this matrix against the CTM
	*/
	public void gtRotate(double angle, double ax, double ay, double az) {
		matrixStack.rotate(angle, ax, ay, az);
	}//gtrotate
	

	/////
	//end matrix stuff
	/////	
	
	//describe scene
	public String toString(){
		String res = "";
		return res;
	}
}//myScene






