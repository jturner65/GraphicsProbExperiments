package base_RayTracer.scene.geometry.sceneObjects.planar;

import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.geometry.sceneObjects.mySceneObject;
import base_Utils_Objects.vectorObjs.myMatrix;
import base_Utils_Objects.vectorObjs.myVector;
import processing.core.PImage;

//a scene object representing a box - basically a bounding box + a shader
public class myRndrdBox extends mySceneObject{

	public myRndrdBox(myScene _scn, double _x, double _y, double _z, myVector _minVals, myVector _maxVals) {
		super(_scn, _x, _y, _z);
	    minVals.set(_minVals);
	    maxVals.set(_maxVals);
	    type = objType.RenderedBBox;
		postProcBBox();				//cnstrct and define bbox
	}
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){
		double v = findTextureV(isctPt,myTexture,time);	
		return new double[]{findTextureU(isctPt,v,myTexture,time),v};
	}

	protected double findTextureU(myVector isctPt, double v, PImage myTexture, double time) {		//TODO
		// TODO Auto-generated method stub
		return 0;
	}

	protected double findTextureV(myVector isctPt, PImage myTexture, double time) {					//TODO
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public myVector getOrigin(double t) {return origin;}
	@Override
	public myVector getMaxVec() {		return _bbox.getMaxVec();}
	@Override
	public myVector getMinVec() {		return _bbox.getMinVec();}
	@Override
	public rayHit intersectCheck(myRay _ray,myRay transRay, myMatrix[] _ctAra) {return _bbox.intersectCheck(_ray, transRay, _ctAra);	}
	@Override
	public myVector getNormalAtPoint(myVector point, int[] args) {return _bbox.getNormalAtPoint(point, args);}
}
