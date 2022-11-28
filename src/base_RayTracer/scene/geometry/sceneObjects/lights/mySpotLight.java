package base_RayTracer.scene.geometry.sceneObjects.lights;

import java.util.concurrent.ThreadLocalRandom;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.sceneObjects.lights.base.Base_Light;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.matrixObjs.doubles.myMatrix;

/**
 * spotlight x y z dx dy dz angle_inner angle_outer r g b
 * Create a spotlight. In addition to the position of the spotlight, 
 * the command specifies the direction in which the light is pointing and an inner and outer angle. 
 * If a point is inside the cone of the inner angle, it is fully lit. 
 * If it is between the inner and outer angle, it is partially lit. If it is outside of the outer angle, it is not lit by this light source. 
 * Note that the angle to a given point can be calculated based on the dot product between the (normalized) spotlight direction and a (normalized) vector from the light to the point in question. 
*/

public class mySpotLight extends Base_Light{
	
	public double innerThet, outerThet, innerThetRad, outerThetRad, radDiff;

	public myVector oPhAxis;	//unit vector ortho to orientation, to use for randomly calculating direction for photon casting
	
	public mySpotLight(Base_Scene _scn, int _lightID, 
			double _r, double _g, double _b, 
			double _x, double _y, double _z, 
			double _dx, double _dy, double _dz, 
			double _inThet, double _outThet) {
		super(_scn, _lightID, _r, _g, _b, _x, _y, _z, _dx, _dy, _dz);
		type = objType.SpotLight;
		setSpotlightVals(_inThet,_outThet);
	}
	
	public void setSpotlightVals(double inAngle, double outAngle){
		innerThet = inAngle;
		innerThetRad = innerThet * MyMathUtils.DEG_TO_RAD;
		outerThet = outAngle;
		outerThetRad = outerThet * MyMathUtils.DEG_TO_RAD;		
		radDiff = outerThetRad - innerThetRad;				//for interpolation 
		oPhAxis = getOrthoVec(orientation);			//for rotation of dir vector for generating photons
	}//setSpotlightVals	
	@Override
	public rayHit intersectCheck(rayCast _ray, rayCast transRay, myMatrix[] _ctAra){  
		rayHit hit = super.intersectCheck(_ray, transRay, _ctAra);
		hit.ltMult = calcT_Mult(transRay.direction,transRay.getTime(), innerThetRad, outerThetRad, radDiff);
		return hit;
	}
	@Override
	public rayCast genRndPhtnRay() {  //diminish power of photon by t value at fringe
		//find random unit vector at some angle from orientation < outerThetRad, scale pwr of photon by t for angle >innerThetRad, <outerThetRad 
		myVector tmp = new myVector();
		double prob, angle;
		
		//as per CLT this should approach gaussian
		double checkProb = ThreadLocalRandom.current().nextDouble(0,1);
		do{//penumbra isn't as likely
			angle = ThreadLocalRandom.current().nextDouble(0,outerThetRad);
			prob = getAngleProb(angle, innerThetRad, outerThetRad, radDiff);			
		//} while (prob > ThreadLocalRandom.current().nextDouble(0,1));
		} while (prob > checkProb);
		
		
		tmp.set(orientation.rotMeAroundAxis(oPhAxis,angle));	
		tmp._normalize();
		//rotate in phi dir for random direction
		tmp = tmp.rotMeAroundAxis(orientation,ThreadLocalRandom.current().nextDouble(0,MyMathUtils.TWO_PI));
		
		return new rayCast(scene, getTransformedPt(origin, CTMara[glblIDX]), tmp, 0);
	}	
	@Override //no need for surface normal of light (??)
	public myVector getNormalAtPoint(myPoint point, int[] args) {	return new myVector(0,1,0);	}
	@Override
	public myPoint getMaxVec(){
		myPoint res = new myPoint(origin);
		res._add(epsVal,epsVal,epsVal);
		return res;
	}
	@Override
	public myPoint getMinVec(){
		myPoint res = new myPoint(origin);
		res._add(-epsVal,-epsVal,-epsVal);
		return res;
	}	
	@Override
	public String toString(){  
		String res = super.toString();
		res+= "\nSpotlight : Direction : " + orientation + " inner angle rad : " + innerThetRad + " outer angle rad : " + outerThetRad;
		return res;
	}
}//class mySpotLight