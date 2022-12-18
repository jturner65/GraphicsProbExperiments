package base_RayTracer.scene.geometry.accelStruct.base;

import java.util.List;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.utils.myRTColor;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

 
/**
 * abstract base class for myGeomBase objects that are directly involved in holding other objects for acceleration purposes
 * @author 7strb
 *
 */
public abstract class Base_AccelStruct extends Base_Geometry{
	private AccelStructType typeOfAccel;
	protected int treeDepth;

	public Base_AccelStruct(Base_Scene _scn, double _x, double _y, double _z, GeomObjType _type) {
		super (_scn, _x,  _y,  _z, _type);
		typeOfAccel = AccelStructType.Unknown;
		treeDepth = 0;
	}
	protected final void postProcAccelStruct() {
		postProcBBox();				//cnstrct and define bbox
		//build origin based on bounding box
		origin = new myPoint(_bbox.getMinVec());
		origin._add(_bbox.getMaxVec());
		origin._mult(.5);
	}
	
	@Override
	//myAccelStruct has no txtrs
	public double[] findTxtrCoords(myPoint isctPt, int textureH, int textureW, double time) {return new double[]{0,0};}
	
	/**
	 * 
	 * @param _ray
	 * @param _trans
	 * @param _ctAra
	 * @return
	 */
	public abstract rayHit traverseStruct(rayCast _ray,rayCast _trans, myMatrix[] _ctAra);

	@Override
	public final rayHit intersectCheck(rayCast _ray,rayCast transRay, myMatrix[] _ctAra) {	
		//check first bbox and then traverse struct
		rayHit bboxHit = _bbox.intersectCheck(_ray,transRay, _ctAra);
		if(!bboxHit.isHit){return bboxHit;}
		//if hit bbox, traverse structure
		return traverseStruct(_ray,transRay, _ctAra);
	}
	/**
	 * returns idx (0-2) of coord of max variance in array of arraylists for bvh
	 * @param _tmpCtrObjList
	 * @return
	 */
	protected int getIDXofMaxBVHSpan(List<Base_Geometry>[] _tmpCtrObjList){
		double maxSpan = -1, diff;
		int idxOfMaxSpan = -1, araSize = _tmpCtrObjList[0].size();
		for(int i = 0; i<3; ++i){
			diff = _tmpCtrObjList[i].get(araSize-1).transOrigin[i] - _tmpCtrObjList[i].get(0).transOrigin[i] ; 
			//System.out.println("Diff : arasize : " + araSize + " diff : " + diff);
			if(maxSpan < diff){		maxSpan = diff;	idxOfMaxSpan = i;}				
		}
		return idxOfMaxSpan;
	}//getIDXofMaxSpan
	
	@Override
	protected final double findTextureU(myPoint isctPt, double v, int textureH, int textureW, double time){ return 0.0; }
	@Override
	protected final double findTextureV(myPoint isctPt, int textureH, int textureW, double time){	return 0.0;  } 
	@Override
	public myVector getNormalAtPoint(myPoint point, int[] args) {	return _bbox.getNormalAtPoint(point, args);}
	@Override
	public myRTColor getColorAtPos(rayHit transRay) {//debug mechanism, will display colors of bounding boxes of particular depths in BVH tree
		switch(typeOfAccel){
		case Unknown 		: {return new myRTColor(0,0,0);}
		case FlatList 		: {return new myRTColor(1,0,1);}//flat list
		case BVHTree 		: {return new myRTColor(0,1,1);}//bvh parent
		case BVHLeftChild 	: {return new myRTColor(0,0,1);}//a left child
		case BVHRightChild	: {return new myRTColor(1,0,0);}//a right child
		case BVHLeafList	: {return new myRTColor(1,1,0);}//leaf list
		}		
		return new myRTColor(0,1,0);
	}//getColorAtPos
	
	@Override
	public myPoint getMaxVec() {	return _bbox.getMaxVec();	}
	@Override
	public myPoint getMinVec() {	return  _bbox.getMinVec();}

	/**
	 * @return the typeOfAccel
	 */
	public AccelStructType getTypeOfAccel() {	return typeOfAccel;}

	/**
	 * @param typeOfAccel the typeOfAccel to set
	 */
	public void setTypeOfAccel(AccelStructType _typeOfAccel) {typeOfAccel = _typeOfAccel;}
	
	public String toString(){  return super.toString() + "Tree Depth : " +treeDepth +"\n";}
}//Base_AccelStruct