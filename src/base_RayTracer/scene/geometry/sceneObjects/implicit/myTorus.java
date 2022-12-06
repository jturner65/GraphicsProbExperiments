package base_RayTracer.scene.geometry.sceneObjects.implicit;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.Geom_ObjType;
import base_RayTracer.scene.geometry.sceneObjects.implicit.base.Base_ImplicitObject;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;


//TODO quartic torus
public class myTorus extends Base_ImplicitObject{
	//bodyRad is radius of entire donut, ringRad is radius of circle making up "arm" of torus
	@SuppressWarnings("unused")
	private double bodyRad, ringRad;

	public myTorus(Base_Scene _p, double _bodyRad, double _ringRad, double _xC, double _yC, double _zC){
		super(_p,_xC,_yC,_zC);
		//calculate torus as a collection of numPolys facets indexed by relative position of center of poly
		//with the intention of making ray collision detection faster 
		type = Geom_ObjType.Torus;
		
		bodyRad = _bodyRad;
		ringRad = _ringRad;
	    minVals = getMinVec();
	    maxVals = getMaxVec();	    
		postProcBBox();				//cnstrct and define bbox
	}
	
	public myTorus(Base_Scene _p, double primeRad, double secondRad){this(_p,primeRad,secondRad,0,0,0); }

	@Override
	protected double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){
		double u = 0.0;
		return u;
	}    
	      
	@Override
	protected double findTextureV(myPoint isctPt, PImage myTexture, double time){
		double v = 0.0;
		return v;
	}
	
	@Override
	public myPoint getMaxVec(){
		myPoint res = new myPoint(origin);
		res._add(bodyRad,bodyRad,bodyRad);
		return res;
	}	
	@Override
	public myPoint getMinVec(){
		myPoint res = new myPoint(origin);
		res._add(-bodyRad,-bodyRad,-bodyRad);
		return res;
	}
	@Override
	public myVector getNormalAtPoint(myPoint point, int[] args) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public rayHit intersectCheck(rayCast _ray, rayCast transRay, myMatrix[] _ctAra){
//		if(!_bbox.intersectCheck(ray, _ctAra).isHit){return new rayHit(false);	}
//		myRay transRay = ray.getTransformedRay(ray, _ctAra[invIDX]);

		// TODO Auto-generated method stub
		return new rayHit(false);
	}


}//myTorus class