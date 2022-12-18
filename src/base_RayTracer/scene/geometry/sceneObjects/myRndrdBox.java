package base_RayTracer.scene.geometry.sceneObjects;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.base.Base_SceneObject;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * a scene object representing a box - basically a bounding box + a shader
 * @author 7strb
 *
 */
public class myRndrdBox extends Base_SceneObject{

	public myRndrdBox(Base_Scene _scn, double _x, double _y, double _z, myVector _minVals, myVector _maxVals) {
		super(_scn, _x, _y, _z,GeomObjType.RenderedBBox);
	    minVals.set(_minVals);
	    maxVals.set(_maxVals);
		postProcBBox();				//cnstrct and define bbox
	}
	@Override
	public double[] findTxtrCoords(myPoint isctPt, int textureH, int textureW, double time){
		double v = findTextureV(isctPt,textureH, textureW,time);	
		return new double[]{findTextureU(isctPt,v,textureH, textureW,time),v};
	}
	@Override
	protected double findTextureU(myPoint isctPt, double v, int textureH, int textureW, double time){ return 0.0; }
	@Override
	protected double findTextureV(myPoint isctPt, int textureH, int textureW, double time){	return 0.0;  } 

	@Override
	public myPoint getMaxVec() {		return _bbox.getMaxVec();}
	@Override
	public myPoint getMinVec() {		return _bbox.getMinVec();}
	@Override
	public rayHit intersectCheck(rayCast _ray,rayCast transRay, myMatrix[] _ctAra) {return _bbox.intersectCheck(_ray, transRay, _ctAra);	}
	@Override
	public myVector getNormalAtPoint(myPoint point, int[] args) {return _bbox.getNormalAtPoint(point, args);}
}//class myRndrdBox
