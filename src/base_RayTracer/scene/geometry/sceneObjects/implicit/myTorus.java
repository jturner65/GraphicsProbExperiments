package base_RayTracer.scene.geometry.sceneObjects.implicit;

import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;


//TODO quartic torus
public class myTorus extends myImpObject{
	//bodyRad is radius of entire donut, ringRad is radius of circle making up "arm" of torus
	@SuppressWarnings("unused")
	private double bodyRad, ringRad;

	public myTorus(myScene _p, double _bodyRad, double _ringRad, double _xC, double _yC, double _zC){
		super(_p,_xC,_yC,_zC);
		//calculate torus as a collection of numPolys facets indexed by relative position of center of poly
		//with the intention of making ray collision detection faster 
		type = objType.Torus;
		
		bodyRad = _bodyRad;
		ringRad = _ringRad;
	    minVals = this.getMinVec();
	    maxVals = this.getMaxVec();	    
		postProcBBox();				//cnstrct and define bbox
	}
	
	public myTorus(myScene _p, double primeRad, double secondRad){this(_p,primeRad,secondRad,0,0,0); }

	@Override
	protected double findTextureU(myVector isctPt, double v, PImage myTexture, double time){
		double u = 0.0;
		return u;
	}    
	      
	@Override
	protected double findTextureV(myVector isctPt, PImage myTexture, double time){
		double v = 0.0;
		return v;
	}
	
	@Override
	public myVector getOrigin(double _t){	return origin;	}
	@Override
	public myVector getMaxVec(){
		myVector res = new myVector(origin);
		res._add(bodyRad,bodyRad,bodyRad);
		return res;
	}	
	@Override
	public myVector getMinVec(){
		myVector res = new myVector(origin);
		res._add(-bodyRad,-bodyRad,-bodyRad);
		return res;
	}
	@Override
	public myVector getNormalAtPoint(myVector point, int[] args) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){
//		if(!_bbox.intersectCheck(ray, _ctAra).isHit){return new rayHit(false);	}
//		myRay transRay = ray.getTransformedRay(ray, _ctAra[invIDX]);

		// TODO Auto-generated method stub
		return new rayHit(false);
	}


}//myTorus class