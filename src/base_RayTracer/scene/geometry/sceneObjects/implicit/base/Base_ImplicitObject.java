package base_RayTracer.scene.geometry.sceneObjects.implicit.base;

import base_RayTracer.ray.rayCast;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.base.Base_SceneObject;
import base_Math_Objects.vectorObjs.doubles.myPoint;

//scene object described by implicit equations
public abstract class Base_ImplicitObject extends Base_SceneObject {
	protected double radX, radY, radZ;  

	public Base_ImplicitObject(Base_Scene _scn, double _x, double _y, double _z, GeomObjType _type){
		super(_scn, _x, _y, _z, _type);
	}

	/**
	*  calculate the correct value for the differences between the origin of a ray and whatever the origin for this 
	*  object is. this displacement is used for determining values in intersection equation for sphere.  includes values
	*  for separate axes' radii
	*/	
	public myPoint originRadCalc(rayCast ray){//need to get ray time value
		myPoint result = new myPoint();
	    myPoint _rayOrigin = ray.origin, thisOrigin = getOrigin(ray.getTime());
	    result.set((_rayOrigin.x - thisOrigin.x)/radX, (_rayOrigin.y - thisOrigin.y)/radY, (_rayOrigin.z - thisOrigin.z)/radZ);
	    return result;  
	}//method originRadCalc
	@Override
	public final double[] findTxtrCoords(myPoint isctPt, int textureH, int textureW, double time){
		double v = findTextureV(isctPt,textureH,textureW,time);	
		return new double[]{findTextureU(isctPt,v,textureH,textureW,time),v};
	}
}//class myImpObject


