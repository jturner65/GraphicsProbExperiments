package base_RayTracer.scene.geometry.sceneObjects.lights;

import java.util.concurrent.ThreadLocalRandom;

import base_RayTracer.ray.rayCast;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.lights.base.Base_Light;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * Create a disk-shaped light source. The center of the disk is (x, y, z), the radius is rad, and a normal vector to the disk is (dx, dy, dz). 
 * As with point lights, there is also a color associated with the light (r, g, b). Since this light source has a non-zero area, 
 * it sould cast a shadow that is soft on the edges. For each shadow ray that is cast at this light source, you should select a random position on this disk. 
 */
public class myDiskLight extends Base_Light{	
	public final double radius;
	public myVector surfTangent;	//unit vector tangent to surface of light - randomly rotate around normal and extend from 0->radius to get random position	
	public myPoint curShadowTarget;		//current target for this light - changes every time shadow ray is sent
	
	public myDiskLight(Base_Scene _scn, int _lightID, 
			double _r, double _g, double _b, 
			double _x, double _y, double _z, 
			double _dx, double _dy, double _dz, 
			double _radius) {
		super(_scn, _lightID, _r, _g, _b, _x, _y, _z, _dx, _dy, _dz, GeomObjType.DiskLight);
		radius = _radius;	
		surfTangent = getOrthoVec(orientation);
		setMinAndMaxVals(radius);	}
	/**
	 * generates a ray to park a photon in the photon map
	 */
	@Override
	public rayCast genRndPhtnRay() {
		myVector dir = new myVector();
		double prob, angle;
		do{//penumbra isn't as likely
			angle = ThreadLocalRandom.current().nextDouble(0,Math.PI);
			prob = getAngleProb(angle, 0, Math.PI, Math.PI);			
		} while (prob > ThreadLocalRandom.current().nextDouble(0,1));
		dir.set(orientation.rotMeAroundAxis(surfTangent,angle));
		dir._normalize();
		//rotate in phi dir for random direction
		//dir = myVector._rotAroundAxis(dir, orientation,ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI));		
		dir = dir.rotMeAroundAxis(orientation,ThreadLocalRandom.current().nextDouble(0,MyMathUtils.TWO_PI));		
		myPoint loc = getRandomDiskPos();
		return new rayCast(scene, CTMara[glblIDX].transformPoint(loc), dir, 0);
	}//genRndPhtnRay
		
	//find random position within this light disk to act as target
	//TODO : t is time in ray - use this to determine if this light is moving
	protected myPoint getRandomDiskPos(){
		myVector tmp = surfTangent.rotMeAroundAxis(orientation,ThreadLocalRandom.current().nextDouble(0,MyMathUtils.TWO_PI));				//rotate surfTangent by random angle
		tmp._normalize();
		double mult = ThreadLocalRandom.current().nextDouble(0,radius);			//find displacement radius from origin
		tmp._mult(mult);
		tmp._add(origin);																														//find displacement point on origin
		return tmp;
	}			

	@Override
	public myPoint getOrigin(double t) {	
		//do this 2x if light is moving, and interpolate between two values
		curShadowTarget = getRandomDiskPos(); 
		return curShadowTarget;
	}
	
	//TODO textured light could give different color light to scene based on location? BATSIGNAL!
	@Override
	protected double findTextureU_Indiv(myPoint isctPt, double v, double time){ return 0.0; }
	//TODO textured light could give different color light to scene based on location? BATSIGNAL!
	@Override
	protected double findTextureV_Indiv(myPoint isctPt, double time){	return 0.0;  } 
	
	@Override
	public String toString(){  return super.toString() + "\nDiskLight : Direction : " + orientation + " radius : " + radius +"\n";}	
}//class myDiskLight