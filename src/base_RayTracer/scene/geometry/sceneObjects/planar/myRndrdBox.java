package base_RayTracer.scene.geometry.sceneObjects.planar;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.sceneObjects.base.Base_SceneObject;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

//a scene object representing a box - basically a bounding box + a shader
public class myRndrdBox extends Base_SceneObject{

	public myRndrdBox(Base_Scene _scn, double _x, double _y, double _z, myVector _minVals, myVector _maxVals) {
		super(_scn, _x, _y, _z);
	    minVals.set(_minVals);
	    maxVals.set(_maxVals);
	    type = objType.RenderedBBox;
		postProcBBox();				//cnstrct and define bbox
	}
	@Override
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time){
		double v = findTextureV(isctPt,myTexture,time);	
		return new double[]{findTextureU(isctPt,v,myTexture,time),v};
	}
	@Override
	protected double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){ return 0.0; }
	@Override
	protected double findTextureV(myPoint isctPt, PImage myTexture, double time){	return 0.0;  } 

	@Override
	public myPoint getOrigin(double t) {return origin;}
	@Override
	public myPoint getMaxVec() {		return _bbox.getMaxVec();}
	@Override
	public myPoint getMinVec() {		return _bbox.getMinVec();}
	@Override
	public rayHit intersectCheck(rayCast _ray,rayCast transRay, myMatrix[] _ctAra) {return _bbox.intersectCheck(_ray, transRay, _ctAra);	}
	@Override
	public myVector getNormalAtPoint(myPoint point, int[] args) {return _bbox.getNormalAtPoint(point, args);}
}
