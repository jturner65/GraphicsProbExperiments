package base_RayTracer.scene.geometry.accelStruct;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.geometry.myGeomBase;
import base_Math_Objects.vectorObjs.doubles.myMatrix;

//bvh structure 
public class myBVH extends myAccelStruct{
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
		@SuppressWarnings("unchecked")
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