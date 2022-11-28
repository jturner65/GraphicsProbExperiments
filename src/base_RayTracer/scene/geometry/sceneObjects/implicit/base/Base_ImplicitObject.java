package base_RayTracer.scene.geometry.sceneObjects.implicit.base;

import processing.core.PImage;
import base_RayTracer.ray.rayCast;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.sceneObjects.base.Base_SceneObject;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

//scene object described by implicit equations
public abstract class Base_ImplicitObject extends Base_SceneObject {
	public double radX, radY, radZ;  

	public Base_ImplicitObject(Base_Scene _p, double _x, double _y, double _z) {
		super(_p, _x, _y, _z);
	}

	/**
	*  calculate the correct value for the differences between the origin of a ray and whatever the origin for this 
	*  object is. this displacement is used for determining values in intersection equation for sphere.  includes values
	*  for seperate axes radii
	*/	
	public myVector originRadCalc(rayCast ray){//need to get ray time value
	    myVector result = new myVector();
	    myPoint _rayOrigin = ray.origin, thisOrigin = getOrigin(ray.getTime());
	    result.set((_rayOrigin.x - thisOrigin.x)/radX, (_rayOrigin.y - thisOrigin.y)/radY, (_rayOrigin.z - thisOrigin.z)/radZ);
	    return result;  
	}//method originRadCalc
	@Override
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time){
		double v = findTextureV(isctPt,myTexture,time);	
		return new double[]{findTextureU(isctPt,v,myTexture,time),v};
	}
}//class myImpObject


