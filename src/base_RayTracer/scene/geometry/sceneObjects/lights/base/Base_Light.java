package base_RayTracer.scene.geometry.sceneObjects.lights.base;

import java.util.concurrent.ThreadLocalRandom;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.base.Base_SceneObject;
import base_RayTracer.scene.photonMapping.Photon_KDTree;
import base_RayTracer.utils.myRTColor;
import processing.core.PImage;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;


public abstract class Base_Light extends Base_SceneObject{
	public myRTColor lightColor;            //does not use super's color vals
	public int lightID;
	public int lightDistType;		//TODO support more than just normal light distribution (i.e. gaussian)
	public Photon_KDTree photonTree;
	public myVector orientation;
	
	//TODO light intensity should fall off by inverse sq dist
	
	public Base_Light(Base_Scene _scn, int _lightID, 
			double _r, double _g, double _b, 
			double _x, double _y, double _z, 
			double _dx, double _dy, double _dz, GeomObjType _type) {
		super(_scn, _x,_y,_z, _type);
	    postProcBBox();
		setIsLight(true);
		lightColor = new myRTColor(_r,_g,_b);
		origin.set(_x,_y,_z);
		lightID = _lightID;
	    orientation = new myVector(_dx,_dy,_dz); 
	    orientation._normalize();
	}	
	@Override
	/**
	 * assumes that transRay dir is toward light.
	 */
	public rayHit intersectCheck(rayCast _ray, rayCast transRay, myMatrix[] _ctAra){  
		myVector hitNorm = new myVector(transRay.direction);
		hitNorm._mult(-1);//norm is just neg ray direction
		hitNorm._normalize();
		double t = transRay.origin._dist(getOrigin(transRay.getTime()));
		rayHit hit = transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(t), new int[]{}, t);
		//rayHit hit = new rayHit(transRay, _ray.direction, this, _ctAra, hitNorm,transRay.pointOnRay(t),t, new int[]{});		
		return hit;
	}		//point light always intersects

	@Override
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time){
		double v = findTextureV(isctPt,myTexture,time);	
		return new double[]{findTextureU(isctPt,v,myTexture,time),v};
	}
	
	//TODO textured light could give different color light to scene based on location? BATSIGNAL!
	@Override
	protected final double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){ return findTextureU_Indiv(isctPt, v, time); }
	protected abstract double findTextureU_Indiv(myPoint isctPt, double v, double time);
	//TODO textured light could give different color light to scene based on location? BATSIGNAL!
	@Override
	protected final double findTextureV(myPoint isctPt, PImage myTexture, double time){	return findTextureV_Indiv(isctPt, time);  } 
	protected abstract double findTextureV_Indiv(myPoint isctPt, double time); 
	
	/**
	 * Set min/max vals as ranges around origin
	 * @param _radius
	 */
	protected final void setMinAndMaxVals(double _radius) {
		minVals = new myPoint(origin);minVals._sub(_radius,_radius,_radius);
		maxVals = new myPoint(origin);maxVals._add(_radius,_radius,_radius);
	}
	
	@Override //normal is used for illumination of an object, are we going to illuminate/render a light?
	public final myVector getNormalAtPoint(myPoint point, int[] args) {	return new myVector(0,1,0);	}
	
	//improve these - need disk dimensions, but using sphere is ok for now
	@Override
	public final myPoint getMaxVec(){return maxVals;}	
	@Override
	public final myPoint getMinVec(){return minVals;}
	
	//get a random direction for a photon to travel - from jensen photon mapping
	public myVector getRandDir(){
		double x,y,z, sqmag, mag;
		
		//replace this with better random randomizer
		do{
			x = ThreadLocalRandom.current().nextDouble(-1.0,1.0);
			y = ThreadLocalRandom.current().nextDouble(-1.0,1.0);
			z = ThreadLocalRandom.current().nextDouble(-1.0,1.0);
			sqmag = (x*x) + (y*y) + (z*z);
		}
		while ((sqmag > 1.0) || (sqmag < epsVal));
		mag=Math.sqrt(sqmag);
		myVector res = new myVector(x/mag,y/mag,z/mag);
		//res._normalize();
		return res;
	}
	
	/**
	 * probability/weighting of angle between inner and outer radii - linear
	 * @param angle
	 * @param innerThetRad
	 * @param outerThetRad
	 * @param radDiff
	 * @return
	 */
	protected double getAngleProb(double angle, double innerThetRad, double outerThetRad, double radDiff){return (angle < innerThetRad ) ? 1 : (angle > outerThetRad ) ? 0 : (outerThetRad - angle)/radDiff;}	
	/**
	 * send direction vector, finds multiplier for penumbra effect
	 * @param dir
	 * @param time
	 * @param innerThetRad
	 * @param outerThetRad
	 * @param radDiff
	 * @return
	 */
	protected double calcT_Mult(myVector dir, double time, double innerThetRad, double outerThetRad, double radDiff){
		double angle = Math.acos(-1*dir._dot(getOrientation(time)));			//intersection pt to light dir is neg light to intersection pt dir - want acos of this to get angle
		return getAngleProb(angle,innerThetRad,outerThetRad, radDiff);// (angle < innerThetRad ) ? 1 : (angle > outerThetRad ) ? 0 : (outerThetRad - angle)/radDiff;		
	}
	
	/**
	 * get starting point for photon ray - will vary based on light type
	 * @return
	 */
	public abstract rayCast genRndPhtnRay();
	
	public myVector getOrientation(double _t){	return orientation;	}

	public String toString(){  return super.toString() + " \ncolor : " + this.lightColor + " light ID : " + this.lightID;  }
}//class myLight


