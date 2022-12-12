package base_RayTracer.scene.geometry.sceneObjects.lights;

import base_RayTracer.ray.rayCast;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.lights.base.Base_Light;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myPointLight extends Base_Light{
 
	public myPointLight(Base_Scene _scn, int _lightID, double _r, double _g, double _b, double _x, double _y, double _z){
		super(_scn,_lightID, _r, _g, _b, _x,_y,_z,0,0,0, GeomObjType.PointLight);
		setMinAndMaxVals(epsVal);
	}//myPointLight constructor(7)
 
	@Override
	public rayCast genRndPhtnRay() {
		myVector tmp = getRandDir();
		return new rayCast(scene, CTMara[glblIDX].transformPoint(origin), tmp, 0);
	}
	
	//TODO textured light could give different color light to scene based on location? BATSIGNAL!
	@Override
	protected double findTextureU_Indiv(myPoint isctPt, double v, double time){ return 0.0; }
	//TODO textured light could give different color light to scene based on location? BATSIGNAL!
	@Override
	protected double findTextureV_Indiv(myPoint isctPt, double time){	return 0.0;  }  
	
	public String toString(){  return super.toString() + " Point Light\n";}
}//class myPointLight
