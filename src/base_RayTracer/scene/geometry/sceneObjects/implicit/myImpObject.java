package base_RayTracer.scene.geometry.sceneObjects.implicit;

import processing.core.PImage;
import base_RayTracer.myRay;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.geometry.sceneObjects.mySceneObject;
import base_Utils_Objects.*;
import base_Utils_Objects.vectorObjs.myVector;

//scene object described by implicit equations
public abstract class myImpObject extends mySceneObject {
	public double radX, radY, radZ;  

	public myImpObject(myScene _p, double _x, double _y, double _z) {
		super(_p, _x, _y, _z);
	}

	/**
	*  calculate the correct value for the differences between the origin of a ray and whatever the origin for this 
	*  object is. this displacement is used for determining values in intersection equation for sphere.  includes values
	*  for seperate axes radii
	*/	
	public myVector originRadCalc(myRay ray){//need to get ray time value
	    myVector result = new myVector(), _rayOrigin = ray.origin, thisOrigin = getOrigin(ray.getTime());
	    result.set((_rayOrigin.x - thisOrigin.x)/radX, (_rayOrigin.y - thisOrigin.y)/radY, (_rayOrigin.z - thisOrigin.z)/radZ);
	    return result;  
	}//method originRadCalc
	@Override
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){
		double v = findTextureV(isctPt,myTexture,time);	
		return new double[]{findTextureU(isctPt,v,myTexture,time),v};
	}
	protected abstract double findTextureU(myVector isctPt, double v, PImage myTexture, double time);
	protected abstract double findTextureV(myVector isctPt, PImage myTexture, double time);	


}//class myImpObject


