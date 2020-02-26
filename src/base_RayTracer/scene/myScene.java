package base_RayTracer.scene;

import java.util.*;
import java.util.concurrent.*;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_RayTracer.myColor;
import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.geometry.myGeomBase;
import base_RayTracer.scene.geometry.accelStruct.*;
import base_RayTracer.scene.geometry.sceneObjects.myInstance;
import base_RayTracer.scene.geometry.sceneObjects.mySceneObject;
import base_RayTracer.scene.geometry.sceneObjects.implicit.myCylinder;
import base_RayTracer.scene.geometry.sceneObjects.implicit.myHollow_Cylinder;
import base_RayTracer.scene.geometry.sceneObjects.implicit.mySphere;
import base_RayTracer.scene.geometry.sceneObjects.lights.*;
import base_RayTracer.scene.geometry.sceneObjects.planar.myPlane;
import base_RayTracer.scene.geometry.sceneObjects.planar.myRndrdBox;
import base_RayTracer.scene.shaders.myObjShader;
import base_RayTracer.scene.shaders.mySimpleReflObjShdr;
import base_RayTracer.scene.textures.*;
import base_UI_Objects.*;
import processing.core.*;
import base_Utils_Objects.io.MessageObject;
import base_Math_Objects.vectorObjs.doubles.myMatStack;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myVector;

//class to hold all objects within a desired scene
public abstract class myScene {
	public static IRenderInterface pa;
	//for screen display
	public static MessageObject msgObj = null;
	
	//multi-threaded stuff
	public ExecutorService th_exec;
	
	public static final double epsVal = .0000001;
	//max # of prims per leaf of accel structure 
	public final int maxPrimsPerLeaf = 5;

	//used for determining refinement array
	public static final int[] pow2 = new int[]{1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768};
	protected static final double log10_2 = Math.log10(2.0);//div to get base 2 log
	public static final float sqrt3 = PApplet.sqrt(3.0f),
	sqrt66 = PApplet.sqrt(6.0f)/6.0f, 
	sqrt612 = .5f*sqrt66;

	////
	//constants and control variables for a particular scene - make changeable via .cli reader
	//maximum height/depth of matrix stack
	public int matStackMaxHeight = 20;

	//row and col values of scene - scene dimensions in pxls
	protected int sceneCols = 300;
	//public static final int sceneRows = 240;
	protected int sceneRows = 300;
	//number of rays in play - including initial ray (recursive depth)
	public int numRays = 8;

	//origin of eye rays
	public myVector eyeOrigin;
	
	//halfway point in rows and cols
	protected double rayYOffset, rayXOffset;
	
	//epsilon value for double calcs; 
	public int objCnt = 0;
	
	/////////////////////////////
	//refining index list
	public int[] RefineIDX;
	public int curRefineStep;
	public ArrayDeque<String> srcFileNames;
	
	public String saveName, folderName;										//name of cli file used to describe this scene, save file, name of containing folder
	
	//the current texture to be used for subsequent objects for either their "top" or "bottom" as determined by their normal, the texture for the background, result image of rendering
	public PImage currTextureTop, currTextureBottom, currBkgTexture, rndrdImg;
	
	//an array list of all the objects in the scene : objList does not include lights, objAndLightList includes all lights
	public ArrayList<myGeomBase> allObjsToFind;
	//the following to facilitate faster light lookup and instancing
	public ArrayList<myGeomBase> objList;
	public ArrayList<myGeomBase> lightList; 	
	
	//objects to be put in accel structure - temp storage as list is being built
	public ArrayList<myGeomBase> tmpObjList;

	//named objects - to be instanced later
	public TreeMap<String, myGeomBase> namedObjs;
	public TreeMap<String, Integer> numInstances;
	
	//background texture-holding sphere.
	public mySphere mySkyDome;	
	
	//current number of lights, number of objects built, used for ID field in mySceneObject constructor and addNewLights, num non-light objects
	public int numLights, objCount, numNonLights, numPxls, numNamedObjs;
	//debug - count rays refracted, reflected
	public long refrRays = 0, reflRays = 0, globRayCount = 0;
	
	public boolean[] scFlags;			//boolean flags describing characteristics of this scene
	
	public static final int	
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

		//currenlty-loading object level flags
		glblTxtrdTopIDX		= 13,		//whether the currently loading object should be txtred on the top
		glblTxtrdBtmIDX		= 14,		//whether the currently loading object should be txtred on the bottom
	
		usePhotonMapIDX		= 15,		//whether to use a photon map for caustics, indirect illumination
		isCausticPhtnIDX	= 16,		//whether to use caustic photons or indirect illumination photons (hacky)
		isPhtnMapRndrdIDX	= 17,		//whether or not photons have been cast yet
		doFinalGatherIDX	= 18;		//whether to do final gather
	
	public static final int numFlags = 19;
	
	////////////////
	//indirect illumination/caustics stuff
	public myKD_Tree photonTree;		//TODO make two of these, one for caustics, one for indirect illum
	public int numGatherRays;
	public int numPhotons, kNhood;
	public float ph_max_near_dist;
	//recursive depth for photons
	public int numPhotonRays = 4;	
	//to correct for light power oddness
	public double causticsLightPwrMult  = 40.0,
			diffuseLightPwrMult = 8.0;
	
	//end indirect illumination/caustics stuff
	////////
	
	//////////////////
	// proc txtrs - colors and constants 	
	public int txtrType;				//set to be 0 : none; 1 : image; 2 : noise; 3 : wood; 4 : marble; 5 : stone/brick
	public double noiseScale;			//set if using noise-based proc txtr
	
	public myColor[] noiseColors = new myColor[]{new myColor(.7,.7,.7),
												new myColor(.2,.2,.2)};
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
	public int roiFunc = 1;									//function for determining region of interest in worley noise - 0 : nearest x sum, 1 : alternating linear sum, 2: ...
	//end proc txtrs
	/////////////////////////////	
		
	public Calendar now;
	//global values set by surface parameter
	public myColor currDiffuseColor, currAmbientColor, currSpecularColor,globCurPermClr, currKReflClr, backgroundColor;//, foregroundColor;
	public double currPhongExp, currKRefl,globRfrIdx,currKTrans, currDepth, lens_radius, lens_focal_distance;   //value representing the visible depth of a colloid (i.e. subsurface experiment) 
	
	public myPlane focalPlane;							//if Depth of Field scene, this is the plane where the lens is in focus
	
	//replaced by num rays per pixel
	public int numRaysPerPixel;
	
	//constant color for mask of fisheye
	public myColor blkColor = new myColor(0,0,0);
	
	public double maxDim, yStart, xStart, fishMult;			//compensate for # rows or # cols not being max - make sure projection is centered in non-square images
	
	//project 3 timer stuff - length of time to render
	public float renderTime;	
	
	public boolean init_flag = false;
	public int grad3[][] = {{1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},{1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},{0,1,1},{0,-1,1},{0,1,-1},{0,-1,-1}};
	public final int p[] = {151,160,137,91,90,15,
				131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
				190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
				88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
				77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
				102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
				135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
				5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
				223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
				129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
				251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
				49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
				138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180};

	// To remove the need for index wrapping, double the permutation table length
	public int perm[] = new int[512];

	
	///////
	//transformation stack stuff
	//
	public myMatStack matrixStack;
	//current depth in matrix stack - starts at 0;
	public int currMatrixDepthIDX;	
	
	public myScene(IRenderInterface _p, String _sceneName, int _numCols, int _numRows) {
		pa = _p;
		
		now = Calendar.getInstance();
		folderName = "pics." +getDateTimeString(); 
		setImageSize(_numCols, _numRows);		
		initialize_table();
		allObjsToFind = new ArrayList<myGeomBase>();
		lightList = new ArrayList<myGeomBase>();
		objList = new ArrayList<myGeomBase>();
		
		srcFileNames = new ArrayDeque<String>();
		eyeOrigin = new myVector(0,0,0);
		gtInitialize();       															 //sets up matrix stack
		initFlags();
		scFlags[saveImageIDX] = true;    												//default to saving image
		scFlags[saveImgInDirIDX] = true;    											//save to timestamped directories, to keep track of changing images
		scFlags[showObjInfoIDX] = true;    												//default to showing info
		namedObjs = new TreeMap<String,myGeomBase>();
		numInstances = new TreeMap<String,Integer>();

		tmpObjList = new ArrayList<myGeomBase>();										//list to hold objects being built to be put into acceleration structure	
		initVars(_sceneName);
	}
	
	public myScene(myScene _old){//copy ctor, for when scene type is being set - only use when old scene is being discarded (shallow copy)
		//pa = _old.pa;
		now = _old.now;
		folderName = _old.folderName;
		setImageSize(_old.sceneCols,_old.sceneRows);

		allObjsToFind = _old.allObjsToFind;
		lightList = _old.lightList;
		objList = _old.objList;
		
		srcFileNames = _old.srcFileNames;
		eyeOrigin = new myVector(0,0,0);
		eyeOrigin.set(_old.eyeOrigin);
		namedObjs = _old.namedObjs;
		numInstances = _old.numInstances;

		tmpObjList = _old.tmpObjList;
		
		gtInitialize();       															 //sets up matrix stack
		scFlags=new boolean[numFlags];for(int i=0;i<numFlags;++i){scFlags[i]=_old.scFlags[i];}
		initVars(_old);	
		matrixStack = _old.matrixStack;
	}//myScene
	
	private void initFlags(){scFlags=new boolean[numFlags];for(int i=0;i<numFlags;++i){scFlags[i]=false;}}
	
	//scene-wide variables set during loading of scene info from .cli file
	private void initVars(String _saveName){
		currTextureTop = null;
		currTextureBottom = null;
		currBkgTexture = null;
		photonTree = null;
		numGatherRays = 0;
		numPhotons =0;
		kNhood = 0;
		ph_max_near_dist = 0;
		
		saveName = _saveName;
		txtrType = 0;
		noiseScale = 1;
		numNonLights = 0;
		numLights = 0;
		objCount = 0;
		numNamedObjs = 0;
		
		backgroundColor = new myColor(0,0,0);
//		foregroundColor = new myColor(1,1,1);
		currDiffuseColor = new myColor(0,0,0);
		currAmbientColor = new myColor(0,0,0);
		currSpecularColor = new myColor(0,0,0);
		currPhongExp = 0;
		currKRefl = 0;
		currKReflClr = new myColor(0,0,0);
		currDepth = 0;		 
		currKTrans = 0;
		globRfrIdx = 0.0;
		globCurPermClr = new myColor(0,0,0);

		curRefineStep = 0;
		reflRays = 0;
		refrRays = 0;
		globRayCount = 0;
		focalPlane = new myPlane(this);
	}//initVars method
	
	//scene-wide variables set during loading of scene info from .cli file
	private void initVars(myScene _old){
		currTextureTop = _old.currTextureTop;
		currTextureBottom = _old.currTextureBottom;
		currBkgTexture = _old.currBkgTexture;
		saveName = _old.saveName;
		txtrType = _old.txtrType;
		noiseScale = _old.noiseScale;
		
		numGatherRays = _old.numGatherRays;
		photonTree = _old.photonTree;	
		numPhotons = _old.numPhotons;
		kNhood = _old.kNhood;
		ph_max_near_dist = _old.ph_max_near_dist;
		
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

		lens_radius = _old. lens_radius;
		lens_focal_distance = _old.lens_focal_distance;		
		focalPlane = _old.focalPlane;		
	}//initVars from old scene method	
	protected abstract void initVarsPriv();
	
	
	public abstract void setSceneParams(double[] args);	

	public void startTmpObjList(){
		tmpObjList = new ArrayList<myGeomBase>();
		scFlags[addToTmpListIDX] = true;
	}
	//end building the accel struct - if is list just build arraylist object, otherwise build acceleration struct
	public void endTmpObjList(int lstType){
		scFlags[addToTmpListIDX] = false;
		myAccelStruct accelObjList = null;
		if(lstType == 0){//flat geomlist
			accelObjList = new myGeomList(this);
			//int objAdded = 1;
			for(myGeomBase obj : tmpObjList){		((myGeomList)accelObjList).addObj(obj);	}
		} else if(lstType == 1){//bvh tree
			System.out.println("begin adding to BVH structure - # objs : " + tmpObjList.size());
			accelObjList = new myBVH(this);
			List<myGeomBase>[] _tmpCtr_ObjList = ((myBVH)accelObjList).buildSortedObjAras(tmpObjList,-1);
			((myBVH)accelObjList).addObjList(_tmpCtr_ObjList, 0, _tmpCtr_ObjList[0].size()-1);
			System.out.println("");
			System.out.println("Done Adding to BVH structure");
		} else if(lstType == 2){//KDtree/octree TODO
			System.out.println("begin adding to octree structure TODO");
			System.out.println("Done Adding to octree structure TODO");
		}
		addObjectToScene(accelObjList);
	}//endTmpObjList
	
	//build miniTet - call recursively to build successively smaller tets
	//set shader for object based on current level - pastel bunnies
	private void setSierpShdr(int level, int maxLevel){
		float bVal = 1.0f - Math.min(1,(1.5f*level/maxLevel)), 
				rVal = 1.0f - bVal, 
				tmp = Math.min((1.2f*(level-(maxLevel/2)))/(1.0f*maxLevel),1), 
				gVal = (tmp*tmp);
		myColor cDiff = new myColor(Math.min(1,rVal+.5f), Math.min(1,gVal+.5f), Math.min(1,bVal+.5f));
		scFlags[glblTxtrdTopIDX]  = false;
		scFlags[glblTxtrdBtmIDX] = false;
		setSurface(cDiff,new myColor(0,0,0),new myColor(0,0,0),0,0);
	}
	//recursively build sierpenski tetrahedron - dim is relative dimension, decreases at each recursive call
	private void buildSierpSubTri(float dim, float scVal, String instName, int level, int maxLevel, boolean addShader){
		if(level>=maxLevel){return;}
		float newDim = scVal*dim;
		
		gtPushMatrix();
		gtTranslate(0,.1f*dim,0);
		gtRotate(70, 0, 1, 0);	
		if(addShader){	setSierpShdr(level, maxLevel);	}
		addInstance(instName,addShader);
		gtPopMatrix();
		//up bunny
		float newTrans = sqrt66*dim;
		gtPushMatrix();		
		gtTranslate(0,newTrans,0);
		gtScale(scVal,scVal,scVal);
		buildSierpSubTri(newDim, scVal, instName,level+1,maxLevel,addShader);
		gtPopMatrix();
		//front bunny
		gtPushMatrix();	
		sierpShiftObj(newTrans);
		gtScale(scVal,scVal,scVal);
		buildSierpSubTri(newDim, scVal,instName,level+1,maxLevel,addShader);
		gtPopMatrix();
		//left bunny
		gtPushMatrix();		
		gtRotate(120,0,1,0);
		sierpShiftObj(newTrans);
		gtRotate(-120,0,1,0);
		gtScale(scVal,scVal,scVal);
		buildSierpSubTri(newDim, scVal,instName,level+1,maxLevel,addShader);
		gtPopMatrix();		
		//right bunny
		gtPushMatrix();		
		gtRotate(-120,0,1,0);
		sierpShiftObj(newTrans);
		gtRotate(120,0,1,0);
		gtScale(scVal,scVal,scVal);
		buildSierpSubTri(newDim,scVal,instName,level+1,maxLevel,addShader);
		gtPopMatrix();		
	}
	
	private void sierpShiftObj(float newTrans){
		gtRotate(120,1,0,0);
		gtTranslate(0,newTrans,0);
		gtRotate(-120,1,0,0);		
	}
	//build a sierpinski tet arrangement using instances of object name
	//depth is how deep to build the tetrahedron, useShdr is whether or not to use changing shader based on depth
	public void buildSierpinski(String name, float scVal, int depth, boolean useShdr){
		startTmpObjList();
		buildSierpSubTri(8,scVal, name,0,depth,useShdr);
		endTmpObjList(1);			//bvh of sierp objs
		System.out.println("total buns : "+((PApplet.pow(4, depth)-1)/3.0f));
	}	
	//remove most recent object from list of objects and instead add to instance object struct.
	public void setObjectAsNamedObject(String name){
		myGeomBase _obj = allObjsToFind.remove(allObjsToFind.size()-1);
		objCount--;
		if(_obj instanceof myLight){												lightList.remove(lightList.size()-1);	numLights--;	} 
		else { 																		objList.remove(objList.size()-1); 		numNonLights--;	}	
		namedObjs.put(name, _obj);
		numInstances.put(name, 0);			//keep count of specific instances
		numNamedObjs++;
	}//setObjectAsNamedObject
	
	public void addInstance(String name, boolean addShdr){
		myGeomBase baseObj = namedObjs.get(name);
		myInstance _inst = new myInstance(this, baseObj);
		if(addShdr){		_inst.useInstShader();	}
		addObjectToScene(_inst, baseObj);			
		numInstances.put(name, numInstances.get(name)+1);
	}//
	
	// adds a new pointlight to the array of lights :   @params rgb - color, xyz - location
	public void addMyPointLight(String[] token){
		System.out.println("Point Light : current # of lights : " + numLights);
		myPointLight tmp = new myPointLight(this, numLights, 
				Double.parseDouble(token[4]),Double.parseDouble(token[5]),Double.parseDouble(token[6]),
				Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]));
		addObjectToScene(tmp);
	}//addMyPointLight method
	 
		// adds a new spotlight to the array of lights :   @params rgb - color, xyz - location, dx,dy,dz direction, innerThet, outerThet - angle bounds
	public void addMySpotLight(String[] token){
		double _inThet = Double.parseDouble(token[7]);
		double _outThet = Double.parseDouble(token[8]);
		System.out.println("Spotlight : current # of lights : " + numLights + " inner angle : " + _inThet + " outer angle : " + _outThet);
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
		System.out.println("Disk Light : current # of lights : " + numLights + " radius : " + radius);
		myDiskLight tmp = new myDiskLight(this, numLights,
				Double.parseDouble(token[8]),Double.parseDouble(token[9]),Double.parseDouble(token[10]),
				Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]),
				Double.parseDouble(token[5]),Double.parseDouble(token[6]),Double.parseDouble(token[7]),
				radius);  
		addObjectToScene(tmp);
	}//addMyDiskLight method	
	//max and min of array of doubles
	protected double max(double[] valAra) {double maxVal = -Double.MAX_VALUE;for (double val : valAra){	if(val > maxVal){maxVal = val;}	}return maxVal;}
	protected double min(double[] valAra) {double minVal = Double.MAX_VALUE;for (double val : valAra){	if(val < minVal){minVal = val;}	}return minVal;}

	//read in prim data from txt file and create object
	public void readPrimData(String[] token){
		mySceneObject tmp = null;
		switch(token[0]){
		    case "box" : {//box xmin ymin zmin xmax ymax zmax :
		    	double minX = min(new double[]{Double.parseDouble(token[1]),Double.parseDouble(token[4])}),
		    		maxX = max(new double[]{Double.parseDouble(token[1]),Double.parseDouble(token[4])}),
		    		ctrX = (minX + maxX)*.5,
					minY = min(new double[]{Double.parseDouble(token[2]),Double.parseDouble(token[5])}),
		    		maxY = max(new double[]{Double.parseDouble(token[2]),Double.parseDouble(token[5])}),
		    		ctrY = (minY + maxY)*.5,
					minZ = min(new double[]{Double.parseDouble(token[3]),Double.parseDouble(token[6])}),
		    		maxZ = max(new double[]{Double.parseDouble(token[3]),Double.parseDouble(token[6])}),
		    		ctrZ = (minZ + maxZ)*.5;
		    	//putting box as a rendered bbox to minimize size of pure bboxes - rendered bbox is a bbox + shdr ref + some shdr-related functions and vars.
		    	tmp = new myRndrdBox(this,ctrX, ctrY, ctrZ, new myVector(minX, minY, minZ),	new myVector(maxX, maxY, maxZ));
		    	break;}			    
		    case "plane" : {			//infinite plane shape
		    	tmp = new myPlane(this);
		    	((myPlane)tmp).setPlaneVals(Double.parseDouble(token[1]), Double.parseDouble(token[2]),Double.parseDouble(token[3]),Double.parseDouble(token[4]));
		    	break;}
		    case "cyl" : {//old cylinder code
		    	double rad = Double.parseDouble(token[1]), hght = Double.parseDouble(token[2]);
		    	double xC = Double.parseDouble(token[3]), yC = Double.parseDouble(token[4]),zC = Double.parseDouble(token[5]);
		    	double xO = 0, yO = 1, zO = 0;
		    	try {
		    		xO = Double.parseDouble(token[6]);yO = Double.parseDouble(token[7]);zO = Double.parseDouble(token[8]);
		    	} catch (Exception e){	        			    	}
		    	tmp = new myCylinder(this, rad,hght,xC,yC,zC,xO,yO,zO);
		    	break;}			   
		    case "cylinder" : { //new reqs : cylinder radius x z ymin ymax
		    	double rad = Double.parseDouble(token[1]), xC = Double.parseDouble(token[2]), zC = Double.parseDouble(token[3]);
		    	double yMin  = Double.parseDouble(token[4]), yMax = Double.parseDouble(token[5]);
		    	double hght = yMax - yMin;
		    	
		    	double xO = 0,yO = 1,zO = 0;
		    	tmp = new myCylinder(this, rad,hght,xC,yMin,zC,xO,yO,zO);
		    	break;}			    

		    case "hollow_cylinder" : {//hollow_cylinder radius x z ymin ymax
		    	double rad = Double.parseDouble(token[1]), xC = Double.parseDouble(token[2]), zC = Double.parseDouble(token[3]);
		    	double yMin  = Double.parseDouble(token[4]), yMax = Double.parseDouble(token[5]);
		    	double hght = yMax - yMin;
		    	
		    	double xO = 0,yO = 1,zO = 0;
		    	tmp = new myHollow_Cylinder(this, rad,hght,xC,yMin,zC,xO,yO,zO);		    	
		    	break;}
		    case "sphere" : {
		    	//create sphere
		    	tmp = new mySphere(this, Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]),Double.parseDouble(token[4]),true);
		    	break;}			    
		    case "moving_sphere": {//moving_sphere radius x1 y1 z1 x2 y2 z2
		    	tmp = new myMovingSphere(this, 
					Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]),Double.parseDouble(token[4]), 
					Double.parseDouble(token[5]),Double.parseDouble(token[6]),Double.parseDouble(token[7]), true);
		    	break;}			    
		    case "sphereIn" : {
		    	//create sphere with internal reflections - normals point in
		    	tmp = new mySphere(this, Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]),Double.parseDouble(token[4]),true);
		    	tmp.rFlags[mySceneObject.invertedIDX] = true;
		    	break;}	
		    case "ellipsoid" : {//create elliptical sphere with 3 radii elements in each of 3 card directions			    	
		    	tmp = new mySphere(this, 
		    			Double.parseDouble(token[1]),Double.parseDouble(token[2]),Double.parseDouble(token[3]),
		    			Double.parseDouble(token[4]),Double.parseDouble(token[5]),Double.parseDouble(token[6]));
		    	break;}
		    default :{
		    	System.out.println("Object type not handled : "+ token[0]);
		    	return;
		    }
		}//switch
		//set shader and texture
		tmp.shdr = getCurShader();
		//add object to scene
    	addObjectToScene(tmp);		
	}//
	
	//return a shader built with the current settings
	public myObjShader getCurShader(){
		myObjShader tmp = (scFlags[simpleRefrIDX]) ? new mySimpleReflObjShdr(this) : new myObjShader(this);
		tmp.txtr = getCurTexture(tmp);				
		return tmp;
	}//getCurShader
	
	//return appropriate texture handler
	public myTextureHandler getCurTexture(myObjShader tmp){
		switch (txtrType){
			case 0 : {	return new myNonTexture(this,tmp);}						//diffuse/shiny only
			case 1 : { 	return new myImageTexture(this,tmp);}					//has an image texture
			case 2 : {	return new myNoiseTexture(this,tmp,noiseScale);}		//perlin noise
			case 3 : {	return new myBaseWoodTexture(this,tmp,noiseScale);}
			case 4 : { 	return new myMarbleTexture(this,tmp,noiseScale);}
			case 5 : { 	return new myCellularTexture(this,tmp,noiseScale);}	
			case 6 : { 	return new myWoodTexture(this,tmp,noiseScale);}
			default : { return new myNonTexture(this,tmp);}
		}
	}//myTextureHandler
	//return appropriate texture handler
	public String getTxtrName(){
		switch (txtrType){
			case 0 : {	return "No Texture";}						//diffuse/shiny only
			case 1 : { 	return "Image Texture";}					//has an image texture
			case 2 : {	return "Pure Noise Texture";}		//perlin noise
			case 3 : {	return "Base Wood Texture";}
			case 4 : { 	return "Marble Texture";}
			case 5 : { 	return "Cellular Texture";}	
			case 6 : { 	return "Alt Wood Texture";}
			default : { return "No Texture";}
		}
	}//myTextureHandler
	
	//entry point
	public void addObjectToScene(myGeomBase _obj){addObjectToScene(_obj,_obj);}
	public void addObjectToScene(myGeomBase _obj, myGeomBase _cmpObj){
		if(scFlags[addToTmpListIDX]){tmpObjList.add(_obj); return;}
		if(_cmpObj instanceof myLight){			lightList.add(_obj);	numLights++;} 
		else {									objList.add(_obj);		numNonLights++;}
		allObjsToFind.add(_obj);
		objCount++;
	}//addObjectToScene

	/////////
	//setting values from reader
	////
	
	public void setNoise(double scale, String[] vals){
		resetDfltTxtrVals();//reset values for next call
		txtrType = 2;
		System.out.println("Setting Noise to scale : " + scale);	
		noiseScale = scale;		//only specified value is scale currently
	}//setNoise
	
	//call after a proc texture is built, to reset values to defaults
	public void resetDfltTxtrVals(){
		setProcTxtrVals(new int[]{0, 4, 1, 2, 1, 1}, 
				new boolean[]{false,false,false}, 
				new double[]{1.0, 1.0, 5.0, .1, 1.0, 0.05}, 
				new myVector[]{new myVector(1.0,1.0,1.0)}, 
				new myColor[]{ getClr("clr_nearblack"),getClr("clr_white")},
				new Double[]{.5,.5});
	}//resetDfltTxtrVals

	//int[] ints =  txtrType,  numOctaves,  numOverlays,  numPtsDist,distFunc, roiFunc
	//boolean[] bools = rndColors , useCustClrs, useFwdTrans
	//double[] dbls = noiseScale, turbMult , colorScale , colorMult, avgNumPerCell, mortarThresh
	//myVector[] vecs = pdMult
	//myColor[] clrs = noiseColors
	//Double[] wts = clrWts
	private void setProcTxtrVals(int[] ints, boolean[] bools, double[] dbls, myVector[] vecs, myColor[] clrs, Double[] wts){
		txtrType = ints[0];	numOctaves = ints[1];numOverlays = ints[2];	numPtsDist = ints[3];	distFunc = ints[4];	roiFunc = ints[5];	
		rndColors = bools[0];useCustClrs = bools[1];useFwdTrans = bools[2];		
		noiseScale = dbls[0];turbMult = dbls[1];colorScale = dbls[2];colorMult = dbls[3];avgNumPerCell = dbls[4];	mortarThresh = dbls[5];		
		pdMult = vecs[0];		
		noiseColors = clrs;
//		clrWts = wts;		
	}//setProcTxtrVals
	
	//build a color value from a string array read in from a cli file.  stIdx is position in array where first color resides
	private myColor readColor(String[] token, int stIdx){return new myColor(Double.parseDouble(token[stIdx]),Double.parseDouble(token[stIdx+1]),Double.parseDouble(token[stIdx+2]));}

	//set colors used by proc texture
	public void setTxtrColor(String[] clrs){
		//get current noise color array
		if(!useCustClrs){
			noiseColors = new myColor[0];
			clrWts = new Double[0];
			useCustClrs = true;
		}
		ArrayList<myColor> tmpAra = new ArrayList<myColor>(Arrays.asList(noiseColors));
		ArrayList<Double> tmpWtAra = new ArrayList<Double>(Arrays.asList(clrWts));
		//<noise color spec tag> (<'named'> <clr name>) or  (<color r g b>)  <wt> <-specify once for each color
		try{	
			myColor tmp = null;
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
			System.out.println("Finished loading color : " + tmp + " for txtr " + getTxtrName());
		}
		catch (Exception e) {String res = "Invalid color specification : " ;	for(int i =0; i<clrs.length;++i){res+=" {"+clrs[i]+"} ";}res+=" so color not added to array";System.out.println(res);}	 		
		noiseColors = tmpAra.toArray(new myColor[0]);
	}//setTxtrColors
	
	//read in constants configured for perlin noise
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
				pyMult._mult(PConstants.TWO_PI-1.0);				//change 0|1 to 0|2pi-1 vector
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
				System.out.println("Proc Perlin-based txtr not specifying randomize colors or overlays so defaults are used for txtr type : " + getTxtrName());	
				rndColors = false;	colorScale = 25.0;colorMult = .1;numOverlays = 1;}	 
			useDefaults = false;
			System.out.println("Finished loading custom values for texture type : " + getTxtrName());
		}
		catch (Exception e) {System.out.println("No Proc Texture values specified for texture type : " + getTxtrName() + " so using defaults.");	useDefaults = true;	}	 
		return useDefaults;	
	}//readProcTxtrPerlinVals
	
	//read in constants configured for worley noise
	private boolean readProcTxtrWorleyVals(String[] vals){
		boolean useDefaults;
		//different format than perlin
		//may just have <typ> or may have up to color scale, or may have all values - use defaults for all values not specified
		//<typ> <noise scale> <distfunction 0=man/1=euc> <roiFunc 0=altLinSum/1=?><num pts for dist func - should be even> <avg # pts per cell> <mortar threshold 0.0-1.0> <useFwdTransform 0/1> <?rndomize colors colorScale - if present then true> <color mult> <num overlays - if present, otherwise 1>
		try{
			int parseIDX = 1;
			noiseScale = Double.parseDouble(vals[parseIDX++]);
			distFunc = Integer.parseInt(vals[parseIDX++]);
			roiFunc = Integer.parseInt(vals[parseIDX++]);	//function for determining region of interest in worley noise - 0 : nearest lin sum 1 : alternating linear sum, 2+: ....
			numPtsDist = Integer.parseInt(vals[parseIDX++]);
			avgNumPerCell = Double.parseDouble(vals[parseIDX++]);
			mortarThresh = Double.parseDouble(vals[parseIDX++]);
			useFwdTrans = (Double.parseDouble(vals[parseIDX++]) == 1.0);		//whether or not to use fwd transform on points
			try{
				colorScale = Double.parseDouble(vals[parseIDX++]);
				colorMult = Double.parseDouble(vals[parseIDX++]);
				rndColors = true;
				try{	numOverlays = Integer.parseInt(vals[parseIDX++]);	}	catch (Exception e) {numOverlays = 1;	}		
			}
			catch (Exception e) {	
				System.out.println("Proc txtr not specifying randomize colors or overlays so defaults are used for txtr type : " + getTxtrName());	
				rndColors = false;	colorScale = 25.0;colorMult = .1;numOverlays = 1;}	 
			useDefaults = false;
			System.out.println("Finished loading custom values for texture type : " + getTxtrName());
		}
		catch (Exception e) {System.out.println("No Proc Texture values specified for texture type : " + getTxtrName() + " so using defaults.");	useDefaults = true;	}	 
		return useDefaults;	
	}//readProcTxtrWorleyVals
	
	//read in procedural texture values for perlin noise and populate globals used to build txtr
	public boolean readProcTxtrVals(String[] vals, boolean isPerlin){
		if(isPerlin){return readProcTxtrPerlinVals(vals);}
		else { return readProcTxtrWorleyVals(vals);}
	}//readProcTxtrVals	
	
	//proc texture components
	public void setTexture(String[] vals){
		//TODO get all these values from CLI
		resetDfltTxtrVals();//reset values for next call
		String _typ = vals[0];
		switch(_typ){
			case "wood":{
				txtrType = 3;
				boolean useDefaults = readProcTxtrVals(vals, true);
				//may be overwritten by later color commands in cli
				if(!useCustClrs){noiseColors = new myColor[]{ getClr("clr_dkwood1"),	getClr("clr_ltwood1")};clrWts = new Double[] {1.0,1.0};}
				if(useDefaults){					
					setProcTxtrVals(new int[]{txtrType, 4, 1, 2, 1,1}, 				//int[] ints =  txtrType,  numOctaves,  numOverlays,  numPtsDist,distFunc (not used for perlin), roiFunc (not used for perlin)
							new boolean[]{true,useCustClrs,false}, 					//boolean[] bools = rndColors , useCustClrs, useFwdTrans -> useFwdTrans needs to be specified in command in cli
							new double[]{2.0, .4, 25.0, .2, 1.0, 0.05}, 			//double[] dbls = noiseScale, turbMult , colorScale , colorMult, avgNumPerCell,mortarThresh
							new myVector[]{new myVector(PConstants.TWO_PI* 2.7,3.6,4.3)}, 	//myVector[] vecs = pdMult
							noiseColors, 											//myColor[] clrs = noiseColors
							clrWts);												//Double[] wts = clrWts					
				} break;}		
			case "wood2"  : {//yellow-ish by default
				txtrType = 6;
				boolean useDefaults = readProcTxtrVals(vals, true);
				if(!useCustClrs){noiseColors = new myColor[]{ getClr("clr_dkwood2"),	getClr("clr_ltwood2")};}// 		clrWts = new Double[] {1.0,1.0};}
				//turbulence values
				if(useDefaults){
					setProcTxtrVals(new int[]{txtrType, 8, 1, 2, 1, 1}, 			//int[] ints =  txtrType,  numOctaves,  numOverlays,  numPtsDist,distFunc (not used), roiFunc (not used)
							new boolean[]{true,useCustClrs,false}, 					//boolean[] bools = rndColors , useCustClrs, useFwdTrans -> useFwdTrans needs to be specified in command in cli
							new double[]{1.0, .4, 25.0, .3, 1.0, 0.05}, 			//double[] dbls = noiseScale, turbMult , colorScale , colorMult, avgNumPerCell
							new myVector[]{new myVector(PConstants.TWO_PI*3.5,7.9,6.2)}, 	//myVector[] vecs = pdMult
							noiseColors, 											//myColor[] clrs = noiseColors
							clrWts);												//Double[] wts = clrWts					
				} break;}		
			case "marble":{
				txtrType = 4;
				boolean useDefaults = readProcTxtrVals(vals, true);
				if(!useCustClrs){noiseColors = new myColor[]{ getClr("clr_nearblack"),	getClr("clr_offwhite")}; clrWts = new Double[] {1.0,1.0};}
				//turbulence values
				if(useDefaults){
				setProcTxtrVals(new int[]{txtrType, 16, 1, 2, 1, 1}, 				//int[] ints =  txtrType,  numOctaves,  numOverlays,  numPtsDist,distFunc (not used), roiFunc (not used)
							new boolean[]{true,useCustClrs,false}, 					//boolean[] bools = rndColors , useCustClrs, useFwdTrans -> useFwdTrans needs to be specified in command in cli
							new double[]{1.0, 15.0, 24.0, .1, 1.0, 0.05}, 			//double[] dbls = noiseScale, turbMult , colorScale , colorMult, avgNumPerCell
							new myVector[]{new myVector(PConstants.TWO_PI * 0.1,PConstants.TWO_PI * 31.4,PConstants.TWO_PI *4.1)}, 	//myVector[] vecs = pdMult
							noiseColors, 											//myColor[] clrs = noiseColors
							clrWts);												//Double[] wts = clrWts					
				} break;}			
			//this uses 
			case "stone":{
				txtrType = 5;
				boolean useDefaults = readProcTxtrVals(vals, false);
				if(!useCustClrs){
					noiseColors = new myColor[]{getClr("clr_mortar1"), getClr("clr_mortar2"),
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
			default : {	System.out.println("Unknown Texture type : " + _typ); txtrType = 0; return;}
		}
		System.out.println("Set Texture type : " + _typ); 
	}//setTexture
	
	
	//returns one of a set of predefined colors or a random color as an array of 0-1 doubles based on tag passed
	public myColor getClr(String colorVal){
		switch (colorVal.toLowerCase()){
			case "clr_rnd"				: { return new myColor(ThreadLocalRandom.current().nextDouble(0,1),ThreadLocalRandom.current().nextDouble(0,1),ThreadLocalRandom.current().nextDouble(0,1));}
	    	case "clr_gray"   		    : { return new myColor(0.47,0.47,0.47);}
	    	case "clr_white"  		    : { return new myColor(1.0,1.0,1.0);}
	    	case "clr_yellow" 		    : { return new myColor(1.0,1.0,0);}
	    	case "clr_cyan"			    : { return new myColor(0,1.0,1.0);} 
	    	case "clr_magenta"		    : { return new myColor(1.0,0,1.0);}  
	    	case "clr_red"    		    : { return new myColor(1.0,0,0);} 
	    	case "clr_blue"			    : { return new myColor(0,0,1.0);}
	    	case "clr_purple"		    : { return new myColor(0.6,0.2,1.0);}
	    	case "clr_green"		    : { return new myColor(0,1.0,0);}  
	    	//lower idxs are darker
	    	case "clr_ltwood1"			: { return new myColor(0.94, 0.47, 0.12);}  
	    	case "clr_ltwood2"			: { return new myColor(0.94, 0.8, 0.4);}  
	    	
	    	case "clr_dkwood1"			: { return new myColor(0.2, 0.08, 0.08);}  
	    	case "clr_dkwood2"			: { return new myColor(0.3, 0.20, 0.16);}  
	    	
	    	case "clr_mortar1"			: { return new myColor(0.2, 0.2, 0.2);}
	    	case "clr_mortar2"			: { return new myColor(0.7, 0.7, 0.7);}

	    	case "clr_brick1_1"			: { return new myColor(0.6, 0.18, 0.22);}
	    	case "clr_brick1_2"			: { return new myColor(0.8, 0.26, 0.33);}
	    	
	    	case "clr_brick2_1"			: { return new myColor(0.6, 0.32, 0.16);}
	    	case "clr_brick2_2"			: { return new myColor(0.8, 0.45, 0.25);}
	    	
	    	case "clr_brick3_1"			: { return new myColor(0.3, 0.01, 0.07);}
	    	case "clr_brick3_2"			: { return new myColor(0.6, 0.02, 0.13);}
	    	
	    	case "clr_brick4_1"			: { return new myColor(0.4, 0.1, 0.17);}
	    	case "clr_brick4_2"			: { return new myColor(0.6, 0.3, 0.13);}
	    	
	    	case "clr_darkgray"   	    : { return new myColor(0.31,0.31,0.31);}
	    	case "clr_darkred"    	    : { return new myColor(0.47,0,0);}
	    	case "clr_darkblue"  	 	: { return new myColor(0,0,0.47);}
	    	case "clr_darkpurple"		: { return new myColor(0.4,0.2,0.6);}
	    	case "clr_darkgreen"  	    : { return new myColor(0,0.47,0);}
	    	case "clr_darkyellow" 	    : { return new myColor(0.47,0.47,0);}
	    	case "clr_darkmagenta"	    : { return new myColor(0.47,0,0.47);}
	    	case "clr_darkcyan"   	    : { return new myColor(0,0.47,0.47);}	  
	    	
	    	case "clr_lightgray"   	    : { return new myColor(0.78,0.78,0.78);}
	    	case "clr_lightred"    	    : { return new myColor(1.0,.43,.43);}
	    	case "clr_lightblue"   	    : { return new myColor(0.43,0.43,1.0);}
	    	case "clr_lightgreen"  	    : { return new myColor(0.43,1.0,0.43);}
	    	case "clr_lightyellow"	    : { return new myColor(1.0,1.0,.43);}
	    	case "clr_lightmagenta"	    : { return new myColor(1.0,.43,1.0);}
	    	case "clr_lightcyan"   	    : { return new myColor(0.43,1.0,1.0);}
	    	
	    	case "clr_black"		    : { return new myColor(0,0,0);}
	    	case "clr_nearblack"		: { return new myColor(0.05,0.05,0.05);}
	    	case "clr_faintgray" 		: { return new myColor(0.43,0.43,0.43);}
	    	case "clr_faintred" 	 	: { return new myColor(0.43,0,0);}
	    	case "clr_faintblue" 	 	: { return new myColor(0,0,0.43);}
	    	case "clr_faintgreen" 	    : { return new myColor(0,0.43,0);}
	    	case "clr_faintyellow" 	    : { return new myColor(0.43,0.43,0);}
	    	case "clr_faintcyan"  	    : { return new myColor(0,0.43,0.43);}
	    	case "clr_faintmagenta"  	: { return new myColor(0.43,0,0.43);}    	
	    	case "clr_offwhite"			: { return new myColor(0.95,0.98,0.92);}
	    	default         		    : { System.out.println("Color not found : " + colorVal + " so using white.");	return new myColor(1.0,1.0,1.0);}    
		}//switch
	}//getClr
	
	public void setImageSize(int numCols, int numRows){//set size and all size-related variables, including image dims
		sceneCols = numCols;
		sceneRows = numRows;
		numPxls = sceneRows * sceneCols;
		rayYOffset = sceneRows/2.0;
		rayXOffset = sceneCols/2.0;
		
		maxDim = Math.max(sceneRows,sceneCols);
		yStart = ((maxDim - sceneRows)/2.0) - rayYOffset;	
		xStart = ((maxDim - sceneCols)/2.0) - rayXOffset;			//compensate for # rows or # cols not being max - make sure projection is centered in non-square images
		fishMult = 2.0/maxDim; 
			
		rndrdImg = ((my_procApplet) pa).createImage(sceneCols,sceneRows,PConstants.RGB);		
	}
	
	//refining
	public void setRefine(String refState){
    	curRefineStep = 0;
		scFlags[myScene.glblRefineIDX] = refState.toLowerCase().equals("on");
		//build refinement #pxls array dynamically by finding average dim of image and then math.
		int refIDX = (int)(Math.log10(.5*(this.sceneCols + this.sceneRows)/16.0)/ log10_2);
		RefineIDX = new int[(refIDX+1)];
		for(int i =refIDX; i >=0; --i){	RefineIDX[refIDX-i]=pow2[i];}
	}//setRefine
		
	public void setDpthOfFld(double lRad, double lFD){//depth of field effect
		lens_radius = lRad;
		lens_focal_distance = lFD;
		scFlags[hasDpthOfFldIDX] = true;
		focalPlane.setPlaneVals(0, 0, 1, lens_focal_distance);      //needs to be modified so that d = viewZ + lens_focal_distance once FOV has been specified.
	}
	
	public void setNumRaysPerPxl(int _num){
		System.out.println("Num Rays Per Pixel : " + _num);
		this.numRaysPerPixel = _num;
	}
	
	public void setSurface(myColor Cdiff, myColor Camb, myColor Cspec, double phongExp, double newKRefl){
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

	public void setSurface(myColor Cdiff, myColor Camb, myColor Cspec, double phongExp, double KRefl, double KTrans){
		setSurface(Cdiff,Camb, Cspec,phongExp, KRefl);
		currKTrans = KTrans;
		setRfrIdx(0, 0, 0, 0);	
	}//setSurface method with refractance

	public void setSurface(myColor Cdiff, myColor Camb, myColor Cspec, double phongExp, double KRefl, double KTrans, double rfrIdx){
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
//	public void setForegroundColor(double r, double g, double b){  foregroundColor.set(r,g,b);  scFlags[useFGColorIDX] = true;}//initBackground 3 color method
	////////
	///end setting values
	////////
	
	///////
	//RT functionality
	///////	
	//get random location within "lens" for depth of field calculation - consider loc to be center, pick random point in constant z plane within some radius of loc point
	public myVector getDpthOfFldEyeLoc(myVector loc){
		//myVector tmp = p.rotVecAroundAxis(new myVector(0,1,0),new myVector(0,0,-1),ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI));				//rotate surfTangent by random angle
		myVector tmp1 = new myVector(0,1,0);
		myVector tmp = tmp1.rotMeAroundAxis(new myVector(0,0,-1),ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI));				//rotate surfTangent by random angle
		tmp._normalize();
		double mult = ThreadLocalRandom.current().nextDouble(0,lens_radius);			//find displacement radius from origin
		tmp._mult(mult);
		tmp._add(loc);																														//find displacement point on origin
		return tmp;		
	}
	//determines if a light source is blocked by another object for shadow detection
	//currently only returns 1 or 0 if light is blocked
	
	public int calcShadow(myRay _ray, double distToLight){
		//for each object in scene, check if intersecting any objects before hitting light
		for (myGeomBase obj : objList){
			if(obj.calcShadowHit(_ray, _ray.getTransformedRay(_ray, obj.CTMara[myGeomBase.invIDX]), obj.CTMara, distToLight) == 1){	return 1;}
		}//for each object in scene
		return 0;
	}//findLight method
	
	//eventually multithread/shdr per object?
	public rayHit findClosestRayHit(myRay _ray){
		//objList does not hold lights - no need to check pointlights - TODO need to check lights for non-point lights- ?	
		TreeMap<rayHit, myGeomBase>objsAtRayHits = new TreeMap<rayHit,myGeomBase>();
		objsAtRayHits.put(new rayHit(false), null);
		//myRay transRay;
		for (myGeomBase obj : objList){	
			rayHit _hit = null;
			try{
				_hit = obj.intersectCheck(_ray,_ray.getTransformedRay(_ray, obj.CTMara[myGeomBase.invIDX]),obj.CTMara);		
			} catch (Exception e){
				System.out.println("find closest ray hit exception :"+e);
			}
			if(_hit.isHit){			objsAtRayHits.put(_hit, _hit.obj);		}
		}//for obj in scenelist
		return objsAtRayHits.firstKey();
	}//findClosestRayHit
	
	
	//determine color of a reflected ray - careful with recursive depth  
	public myColor reflectRay(myRay _ray){
		rayHit hitChk = findClosestRayHit(_ray);
		//if ((hitChk.isHit)) {												return(hitChk.obj.getColorAtPos(hitChk));}//to debug BVH use this - displays colors of leaf boxes (red/blue)
		if ((hitChk.isHit)) {												return(hitChk.shdr.getColorAtPos(hitChk));}
		else if (scFlags[glblTxtrdBkgIDX]) {								return getBackgroundTextureColor(_ray);	} 	//using skydome
//		else if ((_ray.direction.z > epsVal) && (scFlags[useFGColorIDX])){	return foregroundColor;	} 					//for getting color reflected from behind viewer
		else {																return backgroundColor;	}
	}//reflectRay	
	
	
	///////////////
	//setup photon handling kdtree
	public void setPhotonHandling(String[] token){
		scFlags[usePhotonMapIDX] = true;
		scFlags[isPhtnMapRndrdIDX] = false;
		String type = token[0];
		scFlags[isCausticPhtnIDX] = type.contains("caustic");
		//type is "caustic_photons" or "diffuse_photons"
		//caustic_photons 80000000  80 0.05
		numPhotons = Integer.parseInt(token[1]);
		kNhood = Integer.parseInt(token[2]);
		ph_max_near_dist = Float.parseFloat(token[3]);
		//build photon tree
		photonTree = new myKD_Tree(this, numPhotons, kNhood, ph_max_near_dist);
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
  				
  	//TODO set  up seperate maps for caustic and diffuse photons
	//send out photons for all lights if we are using photon mapping - put these photons in kd tree
	//need different photon configurations/processes for caustic photons and indirect illumination (diffuse)photons
	protected void sendCausticPhotons(){
		int numDiv = 100,lastCastCnt = 0, starCount = 0, pctCastCnt = numDiv/10;
		int numCastPerDisp = photonTree.num_Cast/numDiv;
		myLight tmpLight; 
		double pwrMult = causticsLightPwrMult/photonTree.num_Cast;
		myRay reflRefrRay;
		double[] tmpPwr,photon_pwr, d;
		rayHit hitChk;
		myPhoton phn;
		//TODO scale # of photons sent into scene by light intensity
		for(myGeomBase light : lightList){//either a light or an instance of a light
			tmpLight = (light instanceof myLight) ? tmpLight = (myLight)light : ((myLight)((myInstance)light).obj);
			System.out.print("Casting " + photonTree.num_Cast + " Caustic photons for light ID " + tmpLight.ID + ": Progress:");			
			for(int i =0; i<photonTree.num_Cast; ++i){
				photon_pwr = new double[]{tmpLight.lightColor.RGB.x * pwrMult,tmpLight.lightColor.RGB.y * pwrMult,tmpLight.lightColor.RGB.z * pwrMult };
				hitChk = findClosestRayHit(tmpLight.genRndPhtnRay());//first hit
				if((!hitChk.isHit) || (!hitChk.shdr.shdrFlags[myObjShader.hasCaustic])){continue;}			//either hit background or 1st hit is diffuse object - caustic has to hit spec first
				//System.out.println("hit obj : " + hitChk.obj.ID);
				//we have first hit here at caustic-generating surface.  need to propagate through to first diffuse surface
				hitChk.phtnPwr = photon_pwr;
				do{
		 			reflRefrRay = hitChk.shdr.findCausticRayHit(hitChk,hitChk.phtnPwr);
		 			if(reflRefrRay != null){	
		 				tmpPwr = new double[]{hitChk.phtnPwr[0],hitChk.phtnPwr[1],hitChk.phtnPwr[2]};
		 				hitChk = findClosestRayHit(reflRefrRay);
		 				hitChk.phtnPwr = tmpPwr;
		 			} else {					hitChk.isHit = false;}
				} while((hitChk.isHit) && (hitChk.shdr.shdrFlags[myObjShader.hasCaustic]) && (reflRefrRay.gen <= numPhotonRays));				//keep going while we have a hit and we are hitting a caustic
				if((!hitChk.isHit) || (reflRefrRay.gen > numPhotonRays)){continue;}																//bounced off into space
				
				//d = hitChk.fwdTransRayDir._normalized().getAsAra();				
				//phn = new myPhoton(photonTree, hitChk.phtnPwr, hitChk.fwdTransHitLoc, Math.acos(d[2]), PConstants.PI + Math.atan2(d[1], d[0])); 	
				//phn = new myPhoton(photonTree, photon_pwr, hitChk.fwdTransHitLoc.x,  hitChk.fwdTransHitLoc.y,  hitChk.fwdTransHitLoc.z); 	
				phn = new myPhoton(photonTree, hitChk.phtnPwr, hitChk.fwdTransHitLoc.x,  hitChk.fwdTransHitLoc.y,  hitChk.fwdTransHitLoc.z); 	
				photonTree.add_photon(phn);
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
		photonTree.build_tree();
	}//sendCausticPhotons
	
	protected void sendDiffusePhotons(){
		//for every light
		//for each photon of n, 
		//for every object
		//check if hit, save where lands
		int numDiv = 100,lastCastCnt = 0, starCount = 0, pctCastCnt = numDiv/10;
		int numCastPerDisp = photonTree.num_Cast/numDiv;
		myLight tmpLight; 
		double pwrMult = diffuseLightPwrMult/photonTree.num_Cast;
		myRay reflRefrRay;
		double[] tmpPwr,photon_pwr, d;
		rayHit hitChk;
		myPhoton phn;
		//TODO scale # of photons sent into scene by light intensity
		for(myGeomBase light : lightList){//either a light or an instance of a light
			tmpLight = (light instanceof myLight) ? tmpLight = (myLight)light : ((myLight)((myInstance)light).obj);
			System.out.print("Casting " + photonTree.num_Cast + " Diffuse (indirect) photons for light ID " + tmpLight.ID + ": Progress:");			
			for(int i =0; i<photonTree.num_Cast; ++i){
				photon_pwr = new double[]{tmpLight.lightColor.RGB.x * pwrMult,tmpLight.lightColor.RGB.y * pwrMult,tmpLight.lightColor.RGB.z * pwrMult };
				hitChk = findClosestRayHit(tmpLight.genRndPhtnRay());//first hit
				if(!hitChk.isHit){continue;}							//hit background - ignore
				//now we hit an object, spec or diffuse - if specular, bounce without storing, if diffuse store and bounce with prob based on avg color				
				//System.out.println("hit obj : " + hitChk.obj.ID);
				//we have first hit here at caustic-generating surface.  need to propagate through to first diffuse surface
				hitChk.phtnPwr = photon_pwr;
				boolean done = false, firstDiff = true;
				do{
					if(hitChk.shdr.KRefl == 0){//diffuse, store and maybe bounce
						double prob = 0;
						if(!firstDiff){//don't store first
							phn = new myPhoton(photonTree, hitChk.phtnPwr, hitChk.fwdTransHitLoc.x,  hitChk.fwdTransHitLoc.y,  hitChk.fwdTransHitLoc.z); 	
							photonTree.add_photon(phn);
							prob = ThreadLocalRandom.current().nextDouble(0,1.0);//russian roulette to see if casting
						}
						firstDiff = false;
						if(prob < hitChk.shdr.avgDiffClr){	//reflect in new random dir, scale phtn power by diffClr/avgClr
							//get new bounce dir
							myVector hitLoc = hitChk.fwdTransHitLoc;
					  		//first calc random x,y,z
							
							
					  		double x=0,y=0,z=0, sqmag;
							do{
								x = ThreadLocalRandom.current().nextDouble(-1.0,1.0);
								y = ThreadLocalRandom.current().nextDouble(-1.0,1.0);			
								sqmag = (x*x) + (y*y);
							}
							while ((sqmag >= 1.0) || (sqmag < epsVal));
							z = Math.sqrt(1 - ((x*x) + (y*y)));							//cosine weighting preserved by projecting up to sphere
							
							
					  		//then build ortho basis from normal - n' , q' , r' 
					  		myVector n = new myVector(hitChk.objNorm),_p = new myVector(),_q = new myVector(); 
					  				//tmpV = (((n.x > n.y) && (n.x > n.z)) || ((-n.x > -n.y) && (-n.x > -n.z))  ? new myVector(0,0,1)  : new myVector(1,0,0));//find vector not close to n or -n to use to find tangent
							double nxSq = n.x * n.x, nySq = n.y * n.y, nzSq = n.z * n.z;
							myVector tmpV = (((nxSq > nySq) && (nxSq > nzSq))  ? new myVector(0,0,1)  : new myVector(1,0,0));//find vector not close to n or -n to use to find tangent
					  		//set _p to be tangent, _q to be binorm
					  		_p = n._cross(tmpV);	_q = _p._cross(n);
					  		//if(_p.sqMagn < p.epsVal){System.out.println("bad _p : " + _p + " | n : " + n + " | tmpV : " + tmpV);}
					  		//lastly multiply ortho basis vectors by x,y,z : x * p, y * q', z*n', and then sum these products - z is projection/hemisphere dir, so should coincide with normal
					  		
					  		n._mult(z);	_p._mult(x);_q._mult(y);
					  		
					  		myVector bounceDir = new myVector(n.x + _p.x + _q.x,n.y + _p.y + _q.y,n.z + _p.z + _q.z);
					  		bounceDir._normalize();
					 		//save power before finding ray hit, to reset it after ray hit
					  		tmpPwr = new double[]{hitChk.phtnPwr[0]*hitChk.shdr.phtnDiffScl.x,hitChk.phtnPwr[1]*hitChk.shdr.phtnDiffScl.y,hitChk.phtnPwr[2]*hitChk.shdr.phtnDiffScl.z};			//hitChk changes below, we want to propagate tmpPwr
					 		//new photon ray - photon power : 
			 				reflRefrRay = new myRay(this, hitLoc, bounceDir, hitChk.transRay.gen+1);
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
				//phn = new myPhoton(photonTree, photon_pwr, hitChk.fwdTransHitLoc.x,  hitChk.fwdTransHitLoc.y,  hitChk.fwdTransHitLoc.z); 	
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
		photonTree.build_tree();
	}//sendDiffusePhotons
	
	//initialize drawing routine - build photon map if it exists
	protected void initRender(){
		rndrdImg.loadPixels();
		if ((scFlags[usePhotonMapIDX]) && (!scFlags[isPhtnMapRndrdIDX])){	if (scFlags[isCausticPhtnIDX]) {sendCausticPhotons(); } else {sendDiffusePhotons();scFlags[isPhtnMapRndrdIDX] = true;}}		
	}//initRender	
	
	/////////////
	////skydome stuff - move to sphere code, set flag for internal normals TODO
	// find corresponding u and v values for background texture	
	public double findSkyDomeT(myRay transRay){
		//similar to intersection of known direction vectors to lights
		//this code finds t of where passed ray hits mySkyDome edge
		double t = -Double.MAX_VALUE;  //this t is the value of the ray equation where it hits the dome - init to bogus value	  
		//find t for intersection : 
		double a = mySkyDome.getAVal(transRay), b = mySkyDome.getBVal(transRay), c = mySkyDome.getCVal(transRay);	    
		double discr = ((b * b) - (4 * a * c));
		//quadratic - check first if imaginary - if so then no intersection
		if (discr > 0){     
			double discr1 = Math.pow(discr,.5), t1 = (-1*b + discr1)/ (2*a), t2 = (-1*b - discr1)/ (2*a), tVal = Math.min(t1,t2);
			if (tVal < epsVal){tVal = Math.max(t1,t2);}//if the min t val is less than 0
			t = tVal;
		}//if positive t
		else {System.out.println ("error - non-colliding ray doesn't hit sky dome.  b^2 - 4ac , eval : "+ b + "^2 - 4 " + a + "*" + c + " : " + discr);}//should never get here - any ray that is sent to this function hasn't intersected with any other
		return t;
	}//findSkjDomeT func
	
	// find corresponding u and v values for background texture
	public double findBkgTextureV(myVector isctPt, double t){
		double v = 0.0;
		double a0 = isctPt.y - mySkyDome.origin.y;
		double a1 = a0 /(mySkyDome.radY);
		a1 = (a1 > 1)? 1 : (a1 < -1) ? -1 : a1;   
		v = (((myImageTexture)mySkyDome.shdr.txtr).myTextureBottom.height-1) * Math.acos(a1)/ Math.PI;
	  return v;
	}

	public double findBkgTextureU(myVector isctPt, double v, double t){
		double u = 0.0, q,a0, a1, a2, shWm1 = ((myImageTexture)mySkyDome.shdr.txtr).myTextureBottom.width-1, z1 = (isctPt.z - mySkyDome.origin.z);	  
		q = v/(((myImageTexture)mySkyDome.shdr.txtr).myTextureBottom.height-1);//normalize v to be 0-1
		a0 = (isctPt.x - mySkyDome.origin.x)/ (mySkyDome.radX);
		a0 = (a0 > 1) ? 1 : (a0 < -1) ? -1 : a0;
		a1 = ( Math.sin(q* Math.PI));
		a2 = ( fastAbs(a1) < epsVal) ? 1 : a0/a1;
		u = (z1 <= epsVal) ? ((shWm1 * ( Math.acos(a2))/ (PConstants.TWO_PI)) + shWm1/2.0f) : 
					shWm1 - ((shWm1 * ( Math.acos(a2))/ (PConstants.TWO_PI)) + shWm1/2.0f);
		u = (u < 0) ? 0 : (u > shWm1) ? shWm1 : u;
		return u;
	} 

	public myColor getBackgroundTextureColor(myRay ray){
		double t = findSkyDomeT(ray);
		myVector isctPt = ray.pointOnRay(t);
		double v = findBkgTextureV(isctPt, t), u = findBkgTextureU(isctPt, v, t);
		return new myColor(((myImageTexture)mySkyDome.shdr.txtr).myTextureBottom.pixels[(int)v * ((myImageTexture)mySkyDome.shdr.txtr).myTextureBottom.width + (int)u]);
	}//getBackgroundTexturecolor
	
	//////////////////
	//flip the normal directions for this scene
	public void flipNormal(){
		scFlags[flipNormsIDX] = !scFlags[flipNormsIDX];
		scFlags[renderedIDX] = false;				//so will be re-rendered
		scFlags[saveImageIDX] =  true;				//save image with flipped norm
		curRefineStep = 0;
		reflRays = 0;
		refrRays = 0;
		globRayCount = 0;
		for (myGeomBase obj : objList){//set for all scene objects or instances of sceneobjects
			if(obj instanceof mySceneObject){((mySceneObject)obj).setFlags(mySceneObject.invertedIDX, scFlags[flipNormsIDX]);}//either a scene object or an instance of a scene object
			else {if(obj instanceof myInstance && ((myInstance)obj).obj instanceof mySceneObject){((mySceneObject)((myInstance)obj).obj).setFlags(mySceneObject.invertedIDX, scFlags[flipNormsIDX]);}}
		}
	}//flipNormal	
	//return abs vals of vector as vector
	public myVector absVec(myVector _v){return new myVector(fastAbs(_v.x),fastAbs(_v.y),fastAbs(_v.z));}

	public double noise_3d(myVector pt){return noise_3d((float)pt.x, (float)pt.y, (float)pt.z);}
	//from code given by greg for project 4, 3d perlin noise
	public float noise_3d(float x, float y, float z) {		
		// make sure we've initilized table
		if (init_flag == false) {	  initialize_table();	  init_flag = true;	}		
		// Find unit grid cell containing point
		int X = fastfloor(x),Y = fastfloor(y), Z = fastfloor(z);		
		// Get relative xyz coordinates of point within that cell
		x = x - X;	y = y - Y;	z = z - Z;		
		// Wrap the integer cells at 255 (smaller integer period can be introduced here)
		X = X & 255;	Y = Y & 255;	Z = Z & 255;		
		// Calculate a set of eight hashed gradient indices
		int gi000 = perm[X+perm[Y+perm[Z]]] % 12;
		int gi001 = perm[X+perm[Y+perm[Z+1]]] % 12;
		int gi010 = perm[X+perm[Y+1+perm[Z]]] % 12;
		int gi011 = perm[X+perm[Y+1+perm[Z+1]]] % 12;
		int gi100 = perm[X+1+perm[Y+perm[Z]]] % 12;
		int gi101 = perm[X+1+perm[Y+perm[Z+1]]] % 12;
		int gi110 = perm[X+1+perm[Y+1+perm[Z]]] % 12;
		int gi111 = perm[X+1+perm[Y+1+perm[Z+1]]] % 12;
		
		// The gradients of each corner are now:
		// gXXX = grad3[giXXX];
		
		// Calculate noise contributions from each of the eight corners
		float n000= dot(grad3[gi000], x, y, z);
		float n100= dot(grad3[gi100], x-1, y, z);
		float n010= dot(grad3[gi010], x, y-1, z);
		float n110= dot(grad3[gi110], x-1, y-1, z);
		float n001= dot(grad3[gi001], x, y, z-1);
		float n101= dot(grad3[gi101], x-1, y, z-1);
		float n011= dot(grad3[gi011], x, y-1, z-1);
		float n111= dot(grad3[gi111], x-1, y-1, z-1);
		
		// Compute the fade curve value for each of x, y, z
		float u = fade(x), v = fade(y), w = fade(z);
		
//		// Interpolate along x the contributions from each of the corners
//		float nx00 = mix(n000, n100, u);
//		float nx01 = mix(n001, n101, u);
//		float nx10 = mix(n010, n110, u);
//		float nx11 = mix(n011, n111, u);
//		
//		// Interpolate the four results along y
//		float nxy0 = mix(nx00, nx10, v);
//		float nxy1 = mix(nx01, nx11, v);
//		
//		// Interpolate the two last results along z
//	
//		return mix(nxy0, nxy1, w);
		return mix(mix(mix(n000, n100, u), mix(n010, n110, u), v), mix(mix(n001, n101, u), mix(n011, n111, u), v), w);
	
	}//noise_3d

	public void initialize_table() { for(int i=0; i<512; ++i) perm[i]=p[i & 255];}
	public float fastAbs(float x) {return x>0?x:-x;}
	public double fastAbs(double x) {return x>0?x:-x;}
	// This method is a *lot* faster than using (int)Math.floor(x)
	public int fastfloor(float x) { return x>0 ? (int)x : (int)x-1;}
	public int fastfloor(double x) { return x>0 ? (int)x : (int)x-1;}
	private float dot(int g[], float x, float y, float z) { return g[0]*x + g[1]*y + g[2]*z;}
	private float mix(float a, float b, float t) { return (1-t)*a + t*b;}
	private float fade(float t) { return t*t*t*(t*(t*6-15)+10);}
	//end given code, 3d perlin noise
	
	//utility functions
	//find signed area of enclosed poly, with given points and normal N
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
	//write span of pixels with same value, for iterative refinement
	public int writePxlSpan(int clrInt, int row, int col, int _numPxls, int[] pxls){
		int pxlIDX = (row*sceneCols) + col;								//idx of pxl in pxls ara
		int rowPxlCnt, rowStart = row, rowEnd = Math.min(rowStart + _numPxls, sceneRows ),//dont try to go beyond pxl array dims
			colStart = col, colEnd = Math.min(colStart + _numPxls, sceneCols);
		for(int pxlR = rowStart; pxlR < rowEnd; ++pxlR){rowPxlCnt = (pxlR*sceneCols);	for(int pxlC = colStart; pxlC < colEnd; ++pxlC){pxls[(rowPxlCnt + pxlC)] = clrInt;}}		
		return pxlIDX;
	}//writePxlSpan
	
	//instance scene-specific 
	//public abstract myColor calcAAColor(double pRayX, double pRayY, double xIncr, double yIncr);
	public abstract myColor shootMultiRays(double pRayX, double pRayY);
	public abstract void draw(); 
	
	/**
	 * called at end of drawing - should save image
	 */
	protected final void finalizeDraw() {
		((my_procApplet) pa).imageMode(PConstants.CORNER);
		((my_procApplet) pa).image(rndrdImg,0,0);	
	}

	//file
	private void saveFile(){
		now = Calendar.getInstance();
		String tmpSaveName;
		String[] tmp = saveName.split("\\.(?=[^\\.]+$)");				//remove extension from given savename
		//if (scFlags[saveImgInDirIDX]){	tmpSaveName = folderName.toString() + "\\"  + tmp[0]+(scFlags[myScene.flipNormsIDX] ? "_normFlipped" : "")+"_"+getDateTimeString(false,true,"-") + ".png";} //rebuild name to include directory and image name including render time
		if (scFlags[saveImgInDirIDX]){	tmpSaveName = folderName.toString() + "\\"  + tmp[0]+(scFlags[myScene.flipNormsIDX] ? "_normFlipped" : "")+ ".png";} //rebuild name to include directory and image name including render time
		else {							tmpSaveName = tmp[0]+(scFlags[myScene.flipNormsIDX] ? "_normFlipped" : "")+".png";		}
		System.out.println("File saved as  : "+ tmpSaveName);
		rndrdImg.save(tmpSaveName);
		scFlags[saveImageIDX] =  false;//don't keep saving every frame
	}//save image
	  
	//common finalizing for all rendering methods
	protected void finishImage(){
		if (scFlags[saveImageIDX]){		saveFile();	}//if savefile is true, save the file
		if (scFlags[showObjInfoIDX]){
			for (myGeomBase obj : allObjsToFind){
	     		System.out.println(obj.toString());
	     		System.out.println();
	     		if(obj instanceof mySceneObject){
		     		if (((mySceneObject)obj).shdr.txtr.txtFlags[myTextureHandler.txtrdTopIDX]){
		     			System.out.println("" + ((mySceneObject)obj).showUV());
		     		}//if textured
	     		}
		     	else if(obj instanceof myInstance){
		     		myInstance inst = (myInstance)obj;
		     		if ((inst.obj instanceof mySceneObject) && (((mySceneObject)inst.obj).shdr.txtr.txtFlags[myTextureHandler.txtrdTopIDX])){			//TODO need to modify this when using instanced polys having textures - each instance will need a notion of where it should sample from
		     			System.out.println("" + ((mySceneObject)inst.obj).showUV());
		     		}	     	
		     	}
	     	}
		}//for objects and instances, to print out info
		if (scFlags[glblTxtrdBkgIDX]){
			System.out.println("\nBackground : \n");
			System.out.println("" + mySkyDome.showUV());  
		}
		System.out.println("total # of rays : " + globRayCount + " | refl/refr rays " + reflRays +"/" + refrRays);
		System.out.println("");
		System.out.println("Image rendered from file name : " + saveName);
		System.out.println("");
	}
	
	
	/**
	*  build translate, scale and rotation matricies to use for ray tracer
	*  need to implement inversion for each matrix - will apply inverses of these matricies to generated ray so that object is rendered in appropriate manner :
	*
	*  so if object A is subjected to a translate/rotate/scale sequence to render A' then to implement this we need to 
	*  subject the rays attempting to intersect with it by the inverse of these operations to find which rays will actually intersect with it.
	*/
	 public void gtDebugStack(String caller){ System.out.println("Caller : "+caller + "\nCurrent stack status : \n"+matrixStack.toString()); }//gtdebugStack method

	 public void gtInitialize() {
		 currMatrixDepthIDX = 0;
		 matrixStack = new myMatStack(this.matStackMaxHeight);
		 matrixStack.initStackLocation(0);
	 }//gtInitialize method

	public void gtPushMatrix() {
		if (currMatrixDepthIDX < matStackMaxHeight){
	    	matrixStack.push();
	    	currMatrixDepthIDX++;
		} else {	System.out.println("Error, matrix depth maximum " + matStackMaxHeight + " exceeded");	}	  
	}//gtPushMatrix method

	public void gtPopMatrix() { 
		if (matrixStack.top == 0){System.out.println("Error : Cannot pop the last matrix in the matrix stack");} 
		else {		//temp was last matrix at top of stack - referencing only for debugging purposes
			myMatrix temp = matrixStack.pop();
			currMatrixDepthIDX--;
		}
	}//gtPopMatrix method

	public void gtTranslate(double tx, double ty, double tz) { 
		//build and push onto stack the translation matrix
		myMatrix TransMat = new myMatrix();
		//set the 4th column vals to be the translation coordinates
		TransMat.setValByIdx(0,3,tx);
		TransMat.setValByIdx(1,3,ty);
		TransMat.setValByIdx(2,3,tz);
		updateCTM(TransMat);
	}//gtTranslate method

	public void gtScale(double sx, double sy, double sz) {
		//build and push onto stack the scale matrix
		myMatrix ScaleMat = new myMatrix();
		//set the diagonal vals to be the scale coordinates
		ScaleMat.setValByIdx(0,0,sx);
		ScaleMat.setValByIdx(1,1,sy);
		ScaleMat.setValByIdx(2,2,sz);
		updateCTM(ScaleMat);
	}//gtScale method

	/**
	*  sets a rotation matrix to be in "angle" degrees CCW around the axis given by ax,ay,az
	*  and multiples this matrix against the CTM
	*/
	public void gtRotate(double angle, double ax, double ay, double az) { 
		// build and add to top of stack the rotation matrix
		double angleRad = (double)(angle * Math.PI)/180.0;
		myMatrix RotMat = new myMatrix();
		myMatrix RotMatrix1 = new myMatrix();      //translates given axis to x axis
		myMatrix RotMatrix2 = new myMatrix();      //rotation around x axis by given angle
		myMatrix RotMatrix1Trans = new myMatrix();
	  
		myVector axisVect, axisVectNorm, bVect, bVectNorm, cVect, cVectNorm, normVect;
		//first build rotation matrix to rotate ax,ay,az to lie in line with x axis		
		axisVect = new myVector(ax,ay,az);
		axisVectNorm = axisVect._normalized();
	  
		if (ax == 0) { 	normVect = new myVector(1,0,0);} 
		else {			normVect = new myVector(0,1,0);}
		bVect = axisVectNorm._cross(normVect);
		bVectNorm = bVect._normalized();
	  
		cVect = axisVectNorm._cross(bVectNorm);
		cVectNorm = cVect._normalized();
	  
		RotMatrix1.setValByRow(0,axisVectNorm);
		RotMatrix1.setValByRow(1,bVectNorm);
		RotMatrix1.setValByRow(2,cVectNorm);
		
		RotMatrix1Trans = RotMatrix1.transpose();
		//second build rotation matrix to rotate around x axis by angle
		//need to set 1,1 ; 1,2 ; 2,1 ; and 2,2 to cos thet, neg sine thet, sine thet, cos thet, respectively
	 
		RotMatrix2.setValByIdx(1,1,(Math.cos(angleRad)));
		RotMatrix2.setValByIdx(1,2,(-Math.sin(angleRad)));
		RotMatrix2.setValByIdx(2,1,(Math.sin(angleRad)));
		RotMatrix2.setValByIdx(2,2,(Math.cos(angleRad)));
		//lastly, calculate full rotation matrix

		myMatrix tmp = RotMatrix2.multMat(RotMatrix1);
		RotMat = RotMatrix1Trans.multMat(tmp);
		updateCTM(RotMat);
	}//gtrotate
	
	public void updateCTM(myMatrix _mat){		
		myMatrix CTM = matrixStack.peek();
		matrixStack.replaceTop(CTM.multMat(_mat));
	}
	/////
	//end matrix stuff
	/////	

	//build a date with each component separated by token
	public String getDateTimeString(){return getDateTimeString(true, false,".");}
	public String getDateTimeString(boolean useYear, boolean toSecond, String token){
		String result = "";
		int val;
		if(useYear){val = now.get(Calendar.YEAR);		result += ""+val+token;}
		val = now.get(Calendar.MONTH)+1;				result += (val < 10 ? "0"+val : ""+val)+ token;
		val = now.get(Calendar.DAY_OF_MONTH);			result += (val < 10 ? "0"+val : ""+val)+ token;
		val = now.get(Calendar.HOUR);					result += (val < 10 ? "0"+val : ""+val)+ token;
		val = now.get(Calendar.MINUTE);					result += (val < 10 ? "0"+val : ""+val);
		if(toSecond){val = now.get(Calendar.SECOND);	result += token + (val < 10 ? "0"+val : ""+val);}
		return result;
	}
	
	//describe scene
	public String toString(){
		String res = "";
		return res;
	}
}//myScene






