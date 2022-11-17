package base_RayTracer.scene.geometry.base;

import base_RayTracer.myColor;
import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.geometry.BoundingBox;
import base_RayTracer.scene.shaders.myObjShader;
import processing.core.PImage;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
//abstract base class from which scene objects, instances, bounding boxes and acceleration structures all inherit
//make this very skinny since we may have thousands of them
public abstract class Base_Geometry {
	public myScene scene;
	public final int ID;
	public objType type;    //what kind of object this is
	//first 2 vectors of object, center of object,the orientation vector of this object, the min and max values in xyz that this object spans, for bounding box
	public myPoint origin;
	public double[] trans_origin;		//used only to speed up initial calcs for bvh structure
	public myObjShader shdr;

	public myMatrix[] CTMara;
	public static final int 
			glblIDX = 0,
			invIDX = 1,
			transIDX = 2,
			invTransIDX = 3;

	public BoundingBox _bbox;						//everything has a bounding box
	
	public static final double epsVal = MyMathUtils.EPS;//.0000001;
	
	public myPoint minVals, maxVals;
	
	public Base_Geometry(myScene _scn, double _x, double _y, double _z) {
		scene = _scn;
	    ID = scene.objCnt++;
	    type = objType.None;
	    //build this object's transformation matrix - since this is the base/owning object, pass the identity for "prev obj matrix"
		minVals = new myPoint(100000,100000,100000);
		maxVals = new myPoint(-100000,-100000,-100000);
	    CTMara = buildCTMara(scene);	
	    origin = new myPoint(_x,_y,_z);
		trans_origin =  getTransformedPt(origin, CTMara[glblIDX]).asArray();
	}
	
	//inv mat idx : 1; transpose mat idx : 2; transpose of inverse : 3
	private myMatrix[] buildMatExt(myMatrix[] CTMara){CTMara[1] = CTMara[0].inverse();CTMara[2] = CTMara[0].transpose();CTMara[3] = CTMara[1].transpose();return CTMara;}
	//passing Mat so that can instance transformed prims like distorted spheres
	public myMatrix[] buildCTMara(myScene scene, myMatrix _mat){myMatrix[] CTMara = new myMatrix[4];CTMara[0] = _mat.multMat(scene.matrixStack.peek());return buildMatExt(CTMara);}	
	//rebuild mat ara such that passed matrix _mat1 to be fwd transformed by _baseMat.  
	public myMatrix[] reBuildCTMara(myMatrix _mat1, myMatrix _prntMat){myMatrix[] CTMara = new myMatrix[4];CTMara[0] = _prntMat.multMat(_mat1);return buildMatExt(CTMara);}	
	public myMatrix[] buildCTMara(myScene scene){myMatrix[] CTMara = new myMatrix[4];CTMara[0] = scene.matrixStack.peek(); return buildMatExt(CTMara);}	
	public myMatrix[] buildIdentCTMara(){myMatrix[] CTMara = new myMatrix[4];CTMara[0] = new myMatrix(); return buildMatExt(CTMara);	}

	
	public void postProcBBox(){
		_bbox = new BoundingBox(scene, minVals, maxVals);
		_bbox.addObj(this);	
	}
	//max and min of array of doubles
	protected double max(double[] valAra) {double maxVal = -Double.MAX_VALUE;for (double val : valAra){	if(val > maxVal){maxVal = val;}	}return maxVal;}
	protected double min(double[] valAra) {double minVal = Double.MAX_VALUE;for (double val : valAra){	if(val < minVal){minVal = val;}	}return minVal;}

	//get transformed/inverse transformed point - homogeneous coords
	protected myPoint getTransformedPt(myPoint pt, myMatrix trans){
		double[] newPtAra = trans.multVert(new double[]{pt.x, pt.y, pt.z, 1});	
		myPoint newPt = new myVector(newPtAra[0],newPtAra[1],newPtAra[2]);
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
	public abstract myPoint getOrigin(double t);
	//for both below, CALLER MUST FWD TRANSFORM! - to support instances of objects
	//return point with maximum x/y/z coords of this object
	public abstract myPoint getMaxVec();	
	//return point with minimum x/y/z coords of this object
	public abstract myPoint getMinVec();
	//intersection check for shadows 
	public abstract int calcShadowHit(myRay _ray,myRay _trans, myMatrix[] _ctAra, double distToLight);
	//find rayhit value for hitting this geometry object
	public abstract rayHit intersectCheck(myRay _ray,myRay transRay, myMatrix[] _ctAra);
	//find the appropriate normal for the hit on this object
	public abstract myVector getNormalAtPoint(myPoint point, int[] args);
	//everything should be able to handle a get color query
	public abstract myColor getColorAtPos(rayHit transRay);
	//get texture coordinates
	public abstract double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time);
	protected abstract double findTextureU(myPoint isctPt, double v, PImage myTexture, double time);  
	protected abstract double findTextureV(myPoint isctPt, PImage myTexture, double time); 
	
	
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
	    	for (int col = 0; col < 4; ++col){   tmp2str = (CTMara[invTransIDX].m[row][col] < 0 ? "" : " ")+String.format("%.2f", CTMara[invTransIDX].m[row][col]); if (col != 3) {tmp2str += ", ";} result += tmp2str;}    	tmpString = "]";  if (row != 3) { tmpString += "\n"; }
	    	result += tmpString;
	    }
	    if(_bbox != null){		result += "\nBounding box : " + _bbox.toString();}
	    return result;
	}
	
}//myGeomBase



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

