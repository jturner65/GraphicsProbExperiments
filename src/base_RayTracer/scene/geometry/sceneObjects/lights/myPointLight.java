package base_RayTracer.scene.geometry.sceneObjects.lights;

import base_RayTracer.myRay;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.geometry.sceneObjects.lights.base.Base_Light;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myPointLight extends Base_Light{
 
	public myPointLight(myScene _scn, int _lightID, double _r, double _g, double _b, double _x, double _y, double _z){
		super(_scn,_lightID, _r, _g, _b, _x,_y,_z,0,0,0);
		type = objType.PointLight;
	}//myPointLight constructor(7)
 
	@Override//normal is meaningless for pointlight
	public myVector getNormalAtPoint(myVector point, int[] args) {return new myVector(0,1,0);}

	@Override
	public myRay genRndPhtnRay() {
		myVector tmp = getRandDir();
		return new myRay(scene, getTransformedPt(origin, CTMara[glblIDX]), tmp, 0);
	}
	
	@Override
	public myVector getMaxVec(){
		myVector res = new myVector(origin);
		res._add(epsVal,epsVal,epsVal);
		return res;
	}	
	@Override
	public myVector getMinVec(){
		myVector res = new myVector(origin);
		res._add(-epsVal,-epsVal,-epsVal);
		return res;
	}
	public String toString(){  return super.toString() + " Point Light";}
}//class myPointLight
