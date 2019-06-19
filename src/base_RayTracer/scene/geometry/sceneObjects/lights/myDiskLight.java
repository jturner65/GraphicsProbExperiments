package base_RayTracer.scene.geometry.sceneObjects.lights;

import java.util.concurrent.ThreadLocalRandom;

import base_RayTracer.myRay;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_Utils_Objects.vectorObjs.myVector;
import processing.core.PConstants;

/**
 * Create a disk-shaped light source. The center of the disk is (x, y, z), the radius is rad, and a normal vector to the disk is (dx, dy, dz). 
 * As with point lights, there is also a color associated with the light (r, g, b). Since this light source has a non-zero area, 
 * it sould cast a shadow that is soft on the edges. For each shadow ray that is cast at this light source, you should select a random position on this disk. 
 */
public class myDiskLight extends myLight{	
	public double radius;
	public int lightDistType;		//TODO support more than just normal light distribution (i.e. gaussian)
	public myVector surfTangent,	//unit vector tangent to surface of light - randomly rotate around normal and extend from 0->radius to get random position	
					curShadowTarget;		//current target for this light - changes every time shadow ray is sent
	
	public myDiskLight(myScene _scn, int _lightID, 
			double _r, double _g, double _b, 
			double _x, double _y, double _z, 
			double _dx, double _dy, double _dz, 
			double _radius) {
		super(_scn, _lightID, _r, _g, _b, _x, _y, _z, _dx, _dy, _dz);
		type = objType.DiskLight;
		setDisklightVals(_radius);
	}
	//generates a ray to park a photon in the photon map
	@Override
	public myRay genRndPhtnRay() {
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
		dir = dir.rotMeAroundAxis(orientation,ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI));		
		myVector loc = getRandomDiskPos();
		return new myRay(scene, getTransformedPt(loc, CTMara[glblIDX]), dir, 0);
	}//genRndPhtnRay
	
	public void setDisklightVals(double _radius){
		radius = _radius;	
		surfTangent = getOrthoVec(orientation);
	}//setSpotlightVals	
	
	//find random position within this light disk to act as target
	//TODO : t is time in ray - use this to determine if this light is moving
	protected myVector getRandomDiskPos(){
		myVector tmp = surfTangent.rotMeAroundAxis(orientation,ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI));				//rotate surfTangent by random angle
		tmp._normalize();
		double mult = ThreadLocalRandom.current().nextDouble(0,radius);			//find displacement radius from origin
		tmp._mult(mult);
		tmp._add(origin);																														//find displacement point on origin
		return tmp;
	}			
	@Override //normal is used for illumination of an object, are we going to illuminate/render a light?
	public myVector getNormalAtPoint(myVector point, int[] args) {	return new myVector(0,1,0);	}
	@Override
	public myVector getOrigin(double t) {	
		//do this 2x if light is moving, and interpolate between two values
		curShadowTarget = getRandomDiskPos(); 
		return curShadowTarget;
	}

	//improve these - need disk dimensions, but using sphere is ok for now
	@Override
	public myVector getMaxVec(){myVector res = new myVector(origin);res._add(radius,radius,radius);return res;}	
	@Override
	public myVector getMinVec(){myVector res = new myVector(origin);res._add(-radius,-radius,-radius);return res;}
	@Override
	public String toString(){  return super.toString() + "\nDiskLight : Direction : " + orientation + " radius : " + radius;}	
}//class myDiskLight