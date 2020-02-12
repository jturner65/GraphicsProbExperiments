package base_RayTracer.scene.geometry.accelStruct;

import java.util.List;

import base_RayTracer.myColor;
import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.geometry.myGeomBase;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

 
//abstract base class for myGeomBase objects that are directly involved in holding other objects for acceleration purposes
public abstract class myAccelStruct extends myGeomBase{
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