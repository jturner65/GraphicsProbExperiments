package base_RayTracer.scene.geometry.sceneObjects.planar;

import base_RayTracer.ray.rayCast;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.sceneObjects.planar.base.Base_PlanarObject;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

public class myQuad extends Base_PlanarObject{
  
	public myQuad(Base_Scene _scn){
		super(_scn);
		vCount = 4;
		type = objType.Quad;
		initObjVals();
	}//myQuad constructor (4)
	
	public boolean checkInside(myPoint rayPoint, rayCast ray){
		//find ray from each vertex to the planar intersection point
		myVector intRay = new myVector(0,0,0);
		for(int i =0; i<vCount; ++i){
			int pIdx = (i==0 ? vCount-1 : i-1);
			intRay.set(rayPoint.x - vertX[i],rayPoint.y - vertY[i], rayPoint.z - vertZ[i]);
			myVector tmp = intRay._cross(P2P[pIdx]);
			if(tmp._dot(N)< -epsVal){return false;}			
		}
		return true;
	}//checkInside method
	
	@Override
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time){
	    myVector v2 = new myVector(P[0],isctPt);
	    double 
	    v = v2._dot(P2P[0]) / dotVals[0], 
	    u = v2._dot(PLastP0) / dotVals[vCount-1];
		return new double[]{u*(myTexture.width-1),(1-v)*(myTexture.height-1)};
	}
	
}//class myQuad

