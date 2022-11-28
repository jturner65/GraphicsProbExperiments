package base_RayTracer.scene.geometry.accelStruct.base;

import java.util.List;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

 
//abstract base class for myGeomBase objects that are directly involved in holding other objects for acceleration purposes
public abstract class Base_AccelStruct extends Base_Geometry{
	private int typeOfAccel;
	protected int treedepth;

	public Base_AccelStruct(Base_Scene _scn, double _x, double _y, double _z) {
		super (_scn, _x,  _y,  _z);
		typeOfAccel = -1;
		treedepth = 0;
	}

	@Override//myAccelStruct has no txtrs
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time) {return new double[]{0,0};}
	
	public abstract rayHit traverseStruct(rayCast _ray,rayCast _trans, myMatrix[] _ctAra);

	@Override
	public rayHit intersectCheck(rayCast _ray,rayCast transRay, myMatrix[] _ctAra) {	
		//check first bbox and then traverse struct
		rayHit bboxHit = _bbox.intersectCheck(_ray,transRay, _ctAra);
		if(!bboxHit.isHit){return bboxHit;}
		//if hit bbox, traverse structure
		return traverseStruct(_ray,transRay, _ctAra);
	}
	//returns idx (0-2) of coord of max variance in array of arraylists for bvh
	protected int getIDXofMaxBVHSpan(List<Base_Geometry>[] _tmpCtrObjList){
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
	protected final double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){ return 0.0; }
	@Override
	protected final double findTextureV(myPoint isctPt, PImage myTexture, double time){	return 0.0;  } 
	@Override
	public myVector getNormalAtPoint(myPoint point, int[] args) {	return _bbox.getNormalAtPoint(point, args);}
	@Override
	public myRTColor getColorAtPos(rayHit transRay) {//debug mechanism, will display colors of bounding boxes of particular depths in BVH tree
		switch(typeOfAccel){
		case 0 : {return new myRTColor(0,0,0);}
		case 1 : {return new myRTColor(1,0,1);}//flat list
		case 2 : {return new myRTColor(0,1,1);}//bvh parrent
		case 3 : {return new myRTColor(0,0,1);}//a left child
		case 4 : {return new myRTColor(1,0,0);}//a right child
		case 5 : {return new myRTColor(1,1,0);}//leaf list
		}		
		return new myRTColor(0,1,0);
	}
	@Override
	public myPoint getMaxVec() {	return _bbox.maxVals;	}
	@Override
	public myPoint getMinVec() {	return  _bbox.minVals;}

	@Override
	public myPoint getOrigin(double t) {
		origin = new myPoint(_bbox.minVals);
		origin._add(_bbox.maxVals);
		origin._mult(.5);
		return origin;
	}

	/**
	 * @return the typeOfAccel
	 */
	public int getTypeOfAccel() {
		return typeOfAccel;
	}

	/**
	 * @param typeOfAccel the typeOfAccel to set
	 */
	public void setTypeOfAccel(int typeOfAccel) {
		this.typeOfAccel = typeOfAccel;
	}
}//Base_AccelStruct