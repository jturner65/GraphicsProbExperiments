package base_RayTracer;

import java.util.*;
import java.util.Map.Entry;

import processing.core.PImage;
import base_Utils_Objects.*;
//abstract base class from which scene objects, instances, bounding boxes and acceleration structures all inherit
//make this very skinny since we may have thousands of them
public abstract class myGeomBase {
	public myScene scene;
	public final int ID;
	public objType type;    //what kind of object this is
	//first 2 vectors of object, center of object,the orientation vector of this object, the min and max values in xyz that this object spans, for bounding box
	public myVector origin;
	double[] trans_origin;		//used only to speed up initial calcs for bvh structure
	public myObjShader shdr;

	public myMatrix[] CTMara;
	public final int 
			glblIDX = 0,
			invIDX = 1,
			transIDX = 2,
			adjIDX = 3;

	public myBBox _bbox;						//everything has a bounding box
	
	public static final double epsVal = myScene.epsVal;//.0000001;
	
	protected myVector minVals, maxVals;
	
	public myGeomBase(myScene _scn, double _x, double _y, double _z) {
		scene = _scn;
	    ID = scene.objCnt++;
	    type = objType.None;
	    //build this object's transformation matrix - since this is the base/owning object, pass the identity for "prev obj matrix"
		minVals = new myVector(100000,100000,100000);
		maxVals = new myVector(-100000,-100000,-100000);
	    CTMara = buildCTMara(scene);	
	    origin = new myVector(_x,_y,_z);
		trans_origin =  getTransformedPt(origin, CTMara[glblIDX]).asArray();
	}
	
	//inv mat idx : 1; transpose mat idx : 2; adjoint mat idx : 3
	private myMatrix[] buildMatExt(myMatrix[] CTMara){CTMara[1] = CTMara[0].inverse();CTMara[2] = CTMara[0].transpose();CTMara[3] = CTMara[1].transpose();return CTMara;}
	//passing Mat so that can instance transformed prims like distorted spheres
	public myMatrix[] buildCTMara(myScene scene, myMatrix _mat){myMatrix[] CTMara = new myMatrix[4];CTMara[0] = _mat.multMat(scene.matrixStack.peek());return buildMatExt(CTMara);}	
	//rebuild mat ara such that passed matrix _mat1 to be fwd transformed by _baseMat.  
	public myMatrix[] reBuildCTMara(myMatrix _mat1, myMatrix _prntMat){myMatrix[] CTMara = new myMatrix[4];CTMara[0] = _prntMat.multMat(_mat1);return buildMatExt(CTMara);}	
	public myMatrix[] buildCTMara(myScene scene){myMatrix[] CTMara = new myMatrix[4];CTMara[0] = scene.matrixStack.peek(); return buildMatExt(CTMara);}	
	public myMatrix[] buildIdentCTMara(){myMatrix[] CTMara = new myMatrix[4];CTMara[0] = new myMatrix(); return buildMatExt(CTMara);	}

	
	public void postProcBBox(){
		_bbox = new myBBox(scene, minVals, maxVals);
		_bbox.addObj(this);	
	}
	//max and min of array of doubles
	protected double max(double[] valAra) {double maxVal = -Double.MAX_VALUE;for (double val : valAra){	if(val > maxVal){maxVal = val;}	}return maxVal;}
	protected double min(double[] valAra) {double minVal = Double.MAX_VALUE;for (double val : valAra){	if(val < minVal){minVal = val;}	}return minVal;}

	//get transformed/inverse transformed point - homogeneous coords
	protected myVector getTransformedPt(myVector pt, myMatrix trans){
		double[] newPtAra = trans.multVert(new double[]{pt.x, pt.y, pt.z, 1});	
		myVector newPt = new myVector(newPtAra[0],newPtAra[1],newPtAra[2]);
		return newPt;
	}	
	//get transformed/inverse transformed vector - homogeneous coords
	protected myVector getTransformedVec(myVector vec, myMatrix trans){
		double[] newVecAra = trans.multVert(new double[]{vec.x, vec.y, vec.z, 0});		
		myVector newVec = new myVector(newVecAra[0],newVecAra[1],newVecAra[2]);
		return newVec;
	}	
	protected float fastAbs(float x) {return x>0?x:-x;}
	protected double fastAbs(double x) {return x>0?x:-x;}
	// This method is a *lot* faster than using (int)Math.floor(x)
	protected int fastfloor(float x) { return x>0 ? (int)x : (int)x-1;}
	protected int fastfloor(double x) { return x>0 ? (int)x : (int)x-1;}

	//to implement motion blur - t is interpolant between two set values
	public abstract myVector getOrigin(double t);
	//for both below, CALLER MUST FWD TRANSFORM! - to support instances of objects
	//return vector with maximum x/y/z coords of this object
	public abstract myVector getMaxVec();	
	//return vector with minimum x/y/z coords of this object
	public abstract myVector getMinVec();
	//intersection check for shadows 
	public abstract int calcShadowHit(myRay _ray,myRay _trans, myMatrix[] _ctAra, double distToLight);
	//find rayhit value for hitting this geometry object
	public abstract rayHit intersectCheck(myRay _ray,myRay transRay, myMatrix[] _ctAra);
	//find the appropriate normal for the hit on this object
	public abstract myVector getNormalAtPoint(myVector point, int[] args);
	//everything should be able to handle a get color query
	public abstract myColor getColorAtPos(rayHit transRay);
	//get texture coordinates
	public abstract double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time);
	
	public String toString(){
		String result = "Object Type : " + type + " ID:"+ID+" origin : " + origin;
		result+="\nCTM :                         CTMInv :\n";
		String tmpString,tmp2str;
	    for (int row = 0; row < 4; ++row){
	    	result += "[";
	    	for (int col = 0; col < 4; ++col){   tmp2str = (CTMara[glblIDX].m[row][col] < 0 ? "" : " ") + String.format("%.2f", CTMara[glblIDX].m[row][col]); if (col != 3) {tmp2str += ", ";} result += tmp2str;}    	tmpString = "]";result += tmpString + "      [";
	    	for (int col = 0; col < 4; ++col){   tmp2str = (CTMara[invIDX].m[row][col] < 0 ? "" : " ")+String.format("%.2f", CTMara[invIDX].m[row][col]); if (col != 3) {tmp2str += ", ";} result += tmp2str;}    	tmpString = "]";  if (row != 3) { tmpString += "\n"; }
	    	result += tmpString;
	    }
		result+="\nCTMTrans :                         CTMAdj :\n";
		
	    for (int row = 0; row < 4; ++row){
	    	result += "[";
	    	for (int col = 0; col < 4; ++col){   tmp2str = (CTMara[transIDX].m[row][col] < 0 ? "" : " ") + String.format("%.2f", CTMara[transIDX].m[row][col]); if (col != 3) {tmp2str += ", ";} result += tmp2str;}    	tmpString = "]";result += tmpString + "      [";
	    	for (int col = 0; col < 4; ++col){   tmp2str = (CTMara[adjIDX].m[row][col] < 0 ? "" : " ")+String.format("%.2f", CTMara[adjIDX].m[row][col]); if (col != 3) {tmp2str += ", ";} result += tmp2str;}    	tmpString = "]";  if (row != 3) { tmpString += "\n"; }
	    	result += tmpString;
	    }
	    if(_bbox != null){		result += "\nBounding box : " + _bbox.toString();}
	    return result;
	}
	
}//myGeomBase

//use this just to enclose other objects - make a mySceneObj box to render a box
class myBBox extends myGeomBase {
	private myGeomBase obj;							//what this box bounds - can be other bboxes, lists, accel structs, or some mySceneObject
	
	public int maxExtentIdx;						//idx  (0,1,2) of maximum extent in this bounding box
	public myVector sArea;
	public myBBox(myScene _scn, myVector _minVals, myVector _maxVals){
		super (_scn, 0, 0, 0);
		type = objType.BBox;
		calcMinMaxCtrVals(_minVals, _maxVals);
		_bbox = null;							//a bbox should not have a bounding box
	}

	//expand passed bbox to hold passed point - point is in box coords
	private void expandMePt(myVector newPt) {
		minVals.x = (minVals.x < newPt.x) ?minVals.x : newPt.x; 
		minVals.y = (minVals.y < newPt.y) ?minVals.y : newPt.y; 
		minVals.z = (minVals.z < newPt.z) ?minVals.z : newPt.z; 
		maxVals.x = (maxVals.x > newPt.x) ?maxVals.x : newPt.x; 
		maxVals.y = (maxVals.y > newPt.y) ?maxVals.y : newPt.y; 
		maxVals.z = (maxVals.z > newPt.z) ?maxVals.z : newPt.z; 
		calcMinMaxCtrVals(minVals,maxVals);
	}

	//expand bbox to encompass passed box
	public void expandMeByTransBox(myBBox srcBox, myMatrix fwdTrans) {
		expandMePt(getTransformedPt(srcBox.minVals,fwdTrans));
		expandMePt(getTransformedPt(srcBox.maxVals,fwdTrans));
	}
	public void expandMeByBox(myBBox srcBox) {
		expandMePt(srcBox.minVals);
		expandMePt(srcBox.maxVals);
	}
	//expand bbox by delta in all dir
	public void expandMeBoxDel(double delta) {
		myVector delVec = new myVector(minVals);
		delVec._sub(delta, delta, delta);
		expandMePt(delVec);
		delVec = new myVector(maxVals);
		delVec._add(delta, delta, delta);
		expandMePt(delVec);		
	}
	//point needs to be in box space(transformed via box's ctm)
	public boolean pointIsInBox(myBBox tarBox, myVector pt){return (((tarBox.minVals.x < pt.x) && ( pt.x < tarBox.maxVals.x)) && 
													((tarBox.minVals.y < pt.y) && ( pt.y < tarBox.maxVals.y)) && 
													((tarBox.minVals.z < pt.z) && ( pt.z < tarBox.maxVals.z)));}


	
	public void calcMinMaxCtrVals(myVector _minVals, myVector _maxVals){
		minVals.set(Math.min(_minVals.x, minVals.x),Math.min(_minVals.y, minVals.y),Math.min(_minVals.z, minVals.z));
		maxVals.set(Math.max(_maxVals.x, maxVals.x),Math.max(_maxVals.y, maxVals.y),Math.max(_maxVals.z, maxVals.z));
		origin = new myVector(minVals);
		origin._add(maxVals);
		origin._mult(.5);
	    trans_origin = getTransformedPt(origin, CTMara[glblIDX]).asArray();
	    //trans_origin = origin.getAsAra();
		myVector difs = new myVector(minVals,maxVals);
		double[] difVals = difs.asArray();
		double maxVal =  max(difVals);
		maxExtentIdx = (maxVal == difVals[0] ? 0 : maxVal == difVals[1] ? 1 : 2);
		myVector dif = new myVector(minVals,maxVals);
		sArea =  new myVector (dif.y*dif.z, dif.x*dif.z, dif.x*dif.y);						
	}

	public void addObj(myGeomBase _obj) {		
		obj = _obj;	
		CTMara = obj.CTMara;
	}	
	@Override
	public myVector getMaxVec() {	return maxVals;	}
	@Override
	public myVector getMinVec() {	return minVals;	}
	
	@Override//bbox has no txtrs
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time) {return new double[]{0,0};}
	//only says if bbox is hit
	@Override //_ctAra is ara of ctm for object held by bbox, and responsible for transformation of transray
	public rayHit intersectCheck(myRay _ray,myRay transRay, myMatrix[] _ctAra) {
		//iterate through first low and then high values
		double[] rayO = transRay.originAra,//new double[]{transRay.origin.x,transRay.origin.y,transRay.origin.z},
				rayD = transRay.dirAra,//new double[]{transRay.direction.x,transRay.direction.y,transRay.direction.z},
				minValsAra = minVals.asArray(), maxValsAra = maxVals.asArray(),
				tmpVals1 = new double[]{-Double.MAX_VALUE,-1,-1},tmpVals2 = new double[]{-1,-1,-1},
				tMinVals = new double[]{Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE}, tMaxVals = new double[]{-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE};
		double biggestMin = -Double.MAX_VALUE;
		int idx = -1;
		//for this to be inside, the max min value has to be smaller than the min max value 
		for(int i=0;i<3;++i){
			tmpVals1[i] = (minValsAra[i]-rayO[i])/rayD[i];
			tmpVals2[i] = (maxValsAra[i]-rayO[i])/rayD[i];
		}
		for(int i=0;i<3;++i){
			if(tmpVals1[i] < tmpVals2[i]){
				tMinVals[i] = tmpVals1[i];
				tMaxVals[i] = tmpVals2[i];
				if(biggestMin < tmpVals1[i]){		idx = i;	biggestMin = tmpVals1[i];	}
			} else {
				tMinVals[i] = tmpVals2[i];
				tMaxVals[i] = tmpVals1[i];
				if(biggestMin < tmpVals2[i]){		idx = i + 3;	biggestMin = tmpVals2[i];}
			}
		}		
		if((min(tMaxVals) > max(tMinVals)) && biggestMin > 0){ //hit happens
			//pass args array to rayHit args : use idx[1] : this is idx (0 - 5) of plane intersected (low const x plane, low const y plane, low const z plane, high const x plane, high const y plane, high const z plane
			//return (obj instanceof myRndrdBox) ? transRay.objHit(transRay,obj,  _ctAra, transRay.pointOnRay(biggestMin),new int[]{0,idx},biggestMin) : obj.intersectCheck(ray, transRay, _ctAra);
			return transRay.objHit(obj,transRay.getTransformedVec(transRay.direction, _ctAra[glblIDX]),  _ctAra, transRay.pointOnRay(biggestMin),new int[]{0,idx},biggestMin);		//TODO - should we use obj ctara or bbox ctara? should they be same?
		} else {	return new rayHit(false);}			//does not hit		
	}
	
	//determine if shadow ray is a hit or not - returns if object bounded by box is a hit
	@Override
	public int calcShadowHit(myRay _ray,myRay _trans, myMatrix[] _ctAra, double distToLight){		
		rayHit hitChk = intersectCheck(_ray,_trans,_ctAra);			
		if (hitChk.isHit && (distToLight - hitChk.t) > epsVal){	return 1;}   
		return 0;
	}//
	
	@Override
	//args : use idx[1] : this is idx (0 - 5) of plane intersected (low const x plane, low const y plane, low const z plane, high const x plane, high const y plane, high const z plane
	//low planes have neg normal high planes have pos normal
	public myVector getNormalAtPoint(myVector point, int[] args) {
		//System.out.print(args[1]);
		switch (args[1]){
		case 0 : {return new myVector(-1,0,0);}
		case 1 : {return new myVector(0,-1,0);}
		case 2 : {return new myVector(0,0,-1);}
		case 3 : {return new myVector(1,0,0);}
		case 4 : {return new myVector(0,1,0);}
		case 5 : {return new myVector(0,0,1);}		
		default : {return new myVector(0,0,-1);}
		}
	}
	
	@Override
	public myColor getColorAtPos(rayHit transRay) {	return new myColor(1,0,0);}
	@Override
	public myVector getOrigin(double t) {return origin;}	
	public String toString(){
		String res = "\t\tBBOX : "+ ID + " BBox bounds : Min : " + minVals + " | Max : " + maxVals + " | Ctr " + origin + " Obj type : " + obj.type+"\n";
		return res;		
	}

}//myBBox

//abstract base class for myGeomBase objects that are directly involved in holding other objects for acceleration purposes
abstract class myAccelStruct extends myGeomBase{
	int typeOfAccel;
	public int treedepth;

	public myAccelStruct(myScene _scn, double _x, double _y, double _z) {
		super (_scn, _x,  _y,  _z);
		typeOfAccel = -1;
		treedepth = 0;
	}

	@Override//myAccelStruct has no txtrs
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time) {return new double[]{0,0};}
	
	protected abstract rayHit traverseStruct(myRay _ray,myRay _trans, myMatrix[] _ctAra);

	@Override
	public rayHit intersectCheck(myRay _ray,myRay transRay, myMatrix[] _ctAra) {	
		//check first bbox and then traverse struct
		rayHit bboxHit = _bbox.intersectCheck(_ray,transRay, _ctAra);
		if(!bboxHit.isHit){return bboxHit;}
		//if hit bbox, traverse structure
		return traverseStruct(_ray,transRay, _ctAra);
	}
	//returns idx (0-2) of coord of max variance in array of arraylists for bvh
	protected int getIDXofMaxBVHSpan(List<myGeomBase>[] _tmpCtrObjList){
		double maxSpan = -1, diff;
		int idxOfMaxSpan = -1, araSize = _tmpCtrObjList[0].size();
		for(int i = 0; i<3; ++i){
			diff = _tmpCtrObjList[i].get(araSize-1).trans_origin[i] - _tmpCtrObjList[i].get(0).trans_origin[i] ; 
			//System.out.println("Diff : arasize : " + araSize + " diff : " + diff);
			if(maxSpan < diff){		maxSpan = diff;	idxOfMaxSpan = i;}				
		}
		return idxOfMaxSpan;
	}//getIDXofMaxSpan
	
	
	
	
	@Override
	public myVector getNormalAtPoint(myVector point, int[] args) {	return _bbox.getNormalAtPoint(point, args);}
	@Override
	public myColor getColorAtPos(rayHit transRay) {//debug mechanism, will display colors of bounding boxes of particular depths in BVH tree
		switch(this.typeOfAccel){
		case 0 : {return new myColor(0,0,0);}
		case 1 : {return new myColor(1,0,1);}//flat list
		case 2 : {return new myColor(0,1,1);}//bvh parrent
		case 3 : {return new myColor(0,0,1);}//a left child
		case 4 : {return new myColor(1,0,0);}//a right child
		case 5 : {return new myColor(1,1,0);}//leaf list
		}		
		return new myColor(0,1,0);
	}
	@Override
	public myVector getMaxVec() {	return _bbox.maxVals;	}
	@Override
	public myVector getMinVec() {	return  _bbox.minVals;}

	@Override
	public myVector getOrigin(double t) {
		origin = new myVector(_bbox.minVals);
		origin._add(_bbox.maxVals);
		origin._mult(.5);
		return origin;
	}
}//myAccelStruct

class myGeomList extends myAccelStruct{
	public ArrayList<myGeomBase> objList;
	public myGeomList(myScene _scn){
		super(_scn,0,0,0);
		objList = new ArrayList<myGeomBase>();
		type = objType.AccelFlatList;
		typeOfAccel =  0;
		postProcBBox();				//cnstrct and define bbox
	}

	public void addObj(myGeomBase _obj) {
		objList.add(_obj);	
		myMatrix tmp = CTMara[invIDX].multMat(_obj.CTMara[glblIDX]);
		//gtMatrix tmp = (_obj.CTMara[glblIDX]);
		_bbox.expandMeByTransBox(_obj._bbox, tmp);
	}
	@Override  //check if object's contents block the light - check if any are accel structs or instance of accel struct
	public int calcShadowHit(myRay _ray,myRay _trans, myMatrix[] _ctAra, double distToLight) {
		if(_bbox.calcShadowHit( _ray, _trans, _ctAra, distToLight) == 0 ){return 0;}				//no hit of bounding box, then no hit 	
		myRay _objTransRay;
		for (myGeomBase obj : objList){
			_objTransRay = _ray.getTransformedRay(_ray, obj.CTMara[invIDX]);
			//double dist = distToLight/_objTransRay.scale;
			if(obj.calcShadowHit( _ray, _objTransRay, _ctAra, distToLight)==1){return 1;}
		}//for each object in scene
		return 0;
	}//
	
	//build traversal based on _ray - go through all objects in structure
	@Override
	public rayHit traverseStruct(myRay _ray,myRay _trans, myMatrix[] _ctAra){	
		double _clsT = Double.MAX_VALUE;
		rayHit _clsHit = null;
		myRay _objTransRay, _closestTransRay = null;
		myGeomBase _clsObj = null;
		for (myGeomBase obj : objList){
			_objTransRay = _ray.getTransformedRay(_ray, obj.CTMara[invIDX]);
			//checking if from instance so we can propagate the instance transform mat
			rayHit _hit =  obj.intersectCheck(_ray, _objTransRay, obj.CTMara);	//scene.p.reBuildCTMara(obj.CTMara[glblIDX], _ctAra[glblIDX])
			if (_hit.t < _clsT ){
				_clsObj = obj;
				_clsHit = _hit;
				_clsT = _hit.t;
				_closestTransRay = _objTransRay;
			}
		}//for obj in scenelist
		if(_clsHit == null){return new rayHit(false);}
		_clsHit.reCalcCTMHitNorm(reBuildCTMara(_clsObj.CTMara[glblIDX], CTMara[glblIDX]));
		if(!(_clsObj instanceof myAccelStruct)){	return _clsHit;		}		//hit object
		rayHit _hit2 = ((myAccelStruct)_clsObj).traverseStruct(_ray,_closestTransRay, _clsHit.CTMara);	
		return _hit2;
	}//	traverseStruct
	
	@Override
	public String toString(){String res = super.toString() + "Flat List of Size : "+objList.size() + "\n"; return res;	}
}//myGeomList

//bvh structure 
class myBVH extends myAccelStruct{
	public boolean isLeaf;					//whether this is a leaf or a branch of the structure
	
	public myGeomList leafVals;				//if this is a leaf, this holds scene.p.maxPrimsPerLeaf prims
	public int maxSpanSplitIDX;
	public double splitVal;					//value to split on	
	
	public myBVH leftChild, rightChild;
	//public int treedepth;
	public myBVH(myScene _scn){
		super(_scn, 0, 0, 0);
		type = objType.AccelBVH;
		typeOfAccel = 1;
		treedepth = 0;
		isLeaf = true;
		maxSpanSplitIDX = -1;						//can use this to determine if this is a leaf or not
		leafVals = new myGeomList(scene);
		postProcBBox();				//cnstrct and define bbox
	}	
	
	public myBVH buildChild(){
		myBVH res =  new myBVH(scene);
		res.treedepth = this.treedepth + 1;//buildIdentCTMara()
		res.CTMara = CTMara;
		//res.CTMara = scene.p.buildIdentCTMara();
		return res;
	}//buildChild
	
	//build sorted arrays of the incoming aras of objects, sorted on x,y,z coord - _sortedAra is already in order, idxToSkip is the idx/coord _sortedAra is sorted on
	public List<myGeomBase>[] buildSortedObjAras( List<myGeomBase> _sortedAra, int idxToSkip){
		List<myGeomBase>[] resAra = (List<myGeomBase>[]) new List[3];
		TreeMap<Double, List<myGeomBase>> tmpMap;
		if(idxToSkip != -1) {resAra[idxToSkip] = new ArrayList<myGeomBase>(_sortedAra);}	
		for(int i =0; i<3; ++i){
			if(i==idxToSkip){	continue;	} 
			else { 	
				resAra[i] = new ArrayList<myGeomBase>();
				tmpMap = new TreeMap<Double,List<myGeomBase>>();				
				for(myGeomBase _obj : _sortedAra){
					List<myGeomBase> tmpList = tmpMap.get(_obj.trans_origin[i]);
					if(null == tmpList){tmpList = new ArrayList<myGeomBase>();}
					tmpList.add(_obj);
					tmpMap.put( _obj.trans_origin[i], tmpList);
				}
				for (Entry<Double, List<myGeomBase>> e: tmpMap.entrySet()) {		resAra[i].addAll(e.getValue());		}
			}
		}
		return resAra;
	}//buildSortedObjAras
	
	//add list of objects to the bvh tree
	public void addObjList(List<myGeomBase>[] _addObjsList, int stIDX, int endIDX){//incl->excl
		int objListSize = endIDX - stIDX;
		if(objListSize <= scene.maxPrimsPerLeaf){//fewer objs than limit of prims per leaf - just use as list object
			isLeaf = true;
			leafVals = new myGeomList(scene);//clears this
			leafVals.CTMara = CTMara;
			//leafVals.CTMara = scene.p.buildIdentCTMara();
			for(myGeomBase _obj : _addObjsList[0]){	leafVals.addObj(_obj);		}
			leafVals.typeOfAccel = 5;
			_bbox.expandMeByBox(leafVals._bbox);
		} else {
			isLeaf = false;
			//go through ctr-sorted objects to find appropriate first partition
			int _araSplitIDX = (int)(.5 * objListSize);
			//below determines the split in the list of objects
			maxSpanSplitIDX = getIDXofMaxBVHSpan(_addObjsList);
			//by here we have index (0==x, 1==y, etc) of widest dimension and the idx in the array to split the array of widest dimension
			leftChild = buildChild();
			rightChild = buildChild();
			leftChild.addObjList(buildSortedObjAras(_addObjsList[maxSpanSplitIDX].subList(0, _araSplitIDX),maxSpanSplitIDX), stIDX, stIDX+_araSplitIDX);
			leftChild.typeOfAccel = 3;
			rightChild.addObjList(buildSortedObjAras( _addObjsList[maxSpanSplitIDX].subList( _araSplitIDX,objListSize),maxSpanSplitIDX), stIDX+_araSplitIDX, endIDX);
			rightChild.typeOfAccel = 4;
			_bbox.expandMeByBox(leftChild._bbox);
			_bbox.expandMeByBox(rightChild._bbox);
		}
	}//addObjList

//	@Override
//	public void addObj(myGeomBase _obj) {
//		//shouldn't ever hit this - always adding by list of objects
//		System.out.println("Why is addobj in myBVH being called? Shouldn't be");
//		leafVals.addObj(_obj);		
//		_bbox = leafVals._bbox;
//	}//addObj
//	
	@Override
	public int calcShadowHit(myRay _ray, myRay _trans, myMatrix[] _ctAra, double distToLight) {
		if(isLeaf){return leafVals.calcShadowHit(_ray,_trans, _ctAra, distToLight);}
		int leftRes = leftChild._bbox.calcShadowHit(_ray,_trans, _ctAra, distToLight);
		if((leftRes == 1) && leftChild.calcShadowHit(_ray,_trans, _ctAra, distToLight) == 1){return 1;}
		int	rightRes = rightChild._bbox.calcShadowHit(_ray,_trans, _ctAra, distToLight);
		if((rightRes == 1) && rightChild.calcShadowHit(_ray,_trans, _ctAra, distToLight)==1){return 1;}
		return 0;
	}//calcShadowHit	
	
	@Override
	public rayHit traverseStruct(myRay _ray,myRay _trans, myMatrix[] _ctAra){
		if(isLeaf){return leafVals.traverseStruct(_ray,_trans, _ctAra);}		
		rayHit _hit = leftChild._bbox.intersectCheck(_ray,_trans,_ctAra);
//		int dpthToEnd = 18;
//		if((_hit.isHit) && (this.treedepth < dpthToEnd)){
		if(_hit.isHit){
			_hit = leftChild.traverseStruct(_ray,_trans, _ctAra);
		}
		rayHit _hit2 = rightChild._bbox.intersectCheck(_ray,_trans,_ctAra);
//		if((_hit2.isHit) && (this.treedepth < dpthToEnd) && (!_hit.isHit || (_hit2.t < _hit.t))){
		if((_hit2.isHit) && (!_hit.isHit || (_hit2.t < _hit.t))){
			_hit2 = rightChild.traverseStruct(_ray,_trans, _ctAra);
		}		
		return  _hit.t <= _hit2.t ? _hit : _hit2;
	}//traverseStruct

}//myBVH

//class myKDTree extends myAccelStruct{		//TODO octree implementation
//	public boolean isLeaf;					//whether this is a leaf or a branch of the structure
//	
//	public myGeomList leafVals;				//if this is a leaf, this holds scene.p.maxPrimsPerLeaf prims
//	public int maxSpanSplitIDX;
//	public double splitVal;					//value to split on	
//	
//	public myKDTree leftChild, rightChild;
//	//public int treedepth;
//	public myKDTree(myScene _scn){
//		super(_scn, 0, 0, 0);
//		type = objType.AccelBVH;
//		typeOfAccel = 1;
//		treedepth = 0;
//		isLeaf = true;
//		maxSpanSplitIDX = -1;						//can use this to determine if this is a leaf or not
//		leafVals = new myGeomList(scene);
//		postProcBBox();				//cnstrct and define bbox
//	}	
//	
//	public myKDTree buildChild(){
//		myKDTree res =  new myBVH(scene);
//		res.treedepth = this.treedepth + 1;
//		res.CTMara = CTMara;
//		return res;
//	}//buildChild
//	
//	//build sorted arrays of the incoming aras of objects, sorted on x,y,z coord - _sortedAra is already in order, idxToSkip is the idx/coord _sortedAra is sorted on
//	public List<myGeomBase>[] buildSortedObjAras( List<myGeomBase> _sortedAra, int idxToSkip){
//		List<myGeomBase>[] resAra = (List<myGeomBase>[]) new List[3];
//		TreeMap<Double, List<myGeomBase>> tmpMap;
//		double[] ctrAra;
//		if(idxToSkip != -1) {resAra[idxToSkip] = new ArrayList<myGeomBase>(_sortedAra);}	
//		for(int i =0; i<3; ++i){
//			if(i==idxToSkip){	continue;	} 
//			else { 	
//				resAra[i] = new ArrayList<myGeomBase>();
//				tmpMap = new TreeMap<Double,List<myGeomBase>>();				
//				for(myGeomBase _obj : _sortedAra){
//					List<myGeomBase> tmpList = tmpMap.get(_obj.trans_origin[i]);
//					if(null == tmpList){tmpList = new ArrayList<myGeomBase>();}
//					tmpList.add(_obj);
//					tmpMap.put( _obj.trans_origin[i], tmpList);
//				}
//				for (Entry<Double, List<myGeomBase>> e: tmpMap.entrySet()) {		resAra[i].addAll(e.getValue());		}
//			}
//		}
//		return resAra;
//	}//buildSortedObjAras
//	
//	//public String[] idxNm = new String[]{"x","y","z"};
//	
//	public void addObjList(List<myGeomBase>[] _addObjsList, int stIDX, int endIDX){//incl->excl
//		int objListSize = endIDX - stIDX;
//		if(objListSize <= scene.p.maxPrimsPerLeaf){//fewer objs than limit of prims per leaf - just use as list object
//			isLeaf = true;
//			leafVals = new myGeomList(scene);//clears this
//			leafVals.CTMara = CTMara;
//			for(myGeomBase _obj : _addObjsList[0]){		
//				leafVals.addObj(_obj);		
//			}
//			leafVals.typeOfAccel = 5;
//			gtMatrix tmpL = CTMara[invIDX].multMat(leafVals.CTMara[glblIDX]);
//			scene.p.expandBoxByBox(_bbox,leafVals._bbox,tmpL);
//		} else {
//			isLeaf = false;
//			//go through ctr-sorted objects to find appropriate first partition
//			int _araSplitIDX = (int)(.5 * objListSize);
//			//below determines the split in the list of objects
//			maxSpanSplitIDX = getIDXofMaxSpan(_addObjsList);
//			//by here we have index (0==x, 1==y, etc) of widest dimension and the idx in the array to split the array of widest dimension
//			//depth of children in tree
//			leftChild = buildChild();
//			rightChild = buildChild();
//	//		splitVal = (objListSize == 1)  ? _addObjsList[maxSpanSplitIDX].get(_araSplitIDX).trans_origin[maxSpanSplitIDX] :
//	//				.5 * (_addObjsList[maxSpanSplitIDX].get(_araSplitIDX).trans_origin[maxSpanSplitIDX] + _addObjsList[maxSpanSplitIDX].get(_araSplitIDX+1).trans_origin[maxSpanSplitIDX]); 
//	//		System.out.println("Dpth : " + treedepth+ " splitIDX : " + idxNm[maxSpanSplitIDX] + " : splitval : " + String.format("%.008f", splitVal) + " ara split idx : " + _araSplitIDX);
//			String dpthDots ="";
//			for(int i =0;i<treedepth;++i){dpthDots +=".-.";}
//			int cutIDX = stIDX+_araSplitIDX;
//			if(scene.scFlags[myScene.debugIDX]){System.out.println(dpthDots+".Lst"+leftChild.treedepth+":"+stIDX + ":"+cutIDX+".");}
//			leftChild.addObjList(buildSortedObjAras(_addObjsList[maxSpanSplitIDX].subList(0, _araSplitIDX),maxSpanSplitIDX), stIDX, stIDX+_araSplitIDX);
//			if(scene.scFlags[myScene.debugIDX]){System.out.println(dpthDots+".Lend"+leftChild.treedepth+":"+stIDX + ":"+cutIDX+".bbox : "+leftChild._bbox.minVals + "|"+leftChild._bbox.maxVals);}
//			leftChild.typeOfAccel = 3;
//			if(scene.scFlags[myScene.debugIDX]){System.out.println(dpthDots+".Rst"+rightChild.treedepth+":"+cutIDX+"-"+endIDX+".");}
//			rightChild.addObjList(buildSortedObjAras( _addObjsList[maxSpanSplitIDX].subList( _araSplitIDX,objListSize),maxSpanSplitIDX), stIDX+_araSplitIDX, endIDX);
//			if(scene.scFlags[myScene.debugIDX]){System.out.println(dpthDots+".Rend"+rightChild.treedepth+":"+cutIDX+"-"+endIDX+".bbox : "+rightChild._bbox.minVals + "|"+rightChild._bbox.maxVals);}
//			rightChild.typeOfAccel = 4;
//			//System.out.println("right child depth: " +rightChild.treedepth + "  " + rightChild);
//			gtMatrix tmpL = CTMara[invIDX].multMat(leftChild.CTMara[glblIDX]),
//					tmpR = CTMara[invIDX].multMat(rightChild.CTMara[glblIDX]);
//			scene.p.expandBoxByBox(_bbox,leftChild._bbox, tmpL);
//			scene.p.expandBoxByBox(_bbox,rightChild._bbox, tmpR);
//	//		expandBoxByBox(leftChild._bbox);
//	//		expandBoxByBox(rightChild._bbox);
//		}
//	}//addObjList
//	
//	@Override
//	public void addObj(myGeomBase _obj) {
//		//shouldn't ever hit this - always adding by list of objects
//		System.out.println("Why is addobj in myBVH being called?");
//		leafVals.addObj(_obj);		
//		_bbox = leafVals._bbox;
//	}//addObj
//	
//	@Override
//	public int calcShadowHit( myRay _trans, gtMatrix[] _ctAra, double distToLight) {
//		if(isLeaf){return leafVals.calcShadowHit(_trans, _ctAra, distToLight);}
//		int leftRes = leftChild._bbox.calcShadowHit(_trans, _ctAra, distToLight);
//		if((leftRes == 1) && leftChild.calcShadowHit(_trans, _ctAra, distToLight) == 1){return 1;}
//		int	rightRes = rightChild._bbox.calcShadowHit(_trans, _ctAra, distToLight);
//		if((rightRes == 1) && rightChild.calcShadowHit(_trans, _ctAra, distToLight)==1){return 1;}
//		return 0;
//	}//calcShadowHit	
//	
//	@Override
//	public rayHit traverseStruct(myRay _trans, gtMatrix[] _ctAra){
//		if(isLeaf){return leafVals.traverseStruct(_trans, _ctAra);}		
//		rayHit _hit = leftChild._bbox.intersectCheck(_trans,_ctAra),
//				_hit2 = rightChild._bbox.intersectCheck(_trans,_ctAra);
//	//	int dpthToEnd = 18;
//	//	if((_hit.isHit) && (this.treedepth < dpthToEnd)){
//		if(_hit.isHit){
//			_hit = leftChild.traverseStruct(_trans, _ctAra);
//		}
//	//	if((_hit2.isHit) && (this.treedepth < dpthToEnd) && (!_hit.isHit || (_hit2.t < _hit.t))){
//		if((_hit2.isHit) && (!_hit.isHit || (_hit2.t < _hit.t))){
//			_hit2 = rightChild.traverseStruct(_trans, _ctAra);
//		}		
//		return  _hit.t <= _hit2.t ? _hit : _hit2;
//	}//traverseStruct
//}//myKDTree

