package base_RayTracer.scene.geometry.sceneObjects.lights.base;

import java.util.concurrent.ThreadLocalRandom;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.base.Base_SceneObject;
import base_RayTracer.scene.photonMapping.Photon_KDTree;
import processing.core.PImage;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;


public abstract class Base_Light extends Base_SceneObject{
	public myRTColor lightColor;            //does not use super's color vals
	public int lightID;
	public Photon_KDTree photonTree;
	public myVector orientation;
	
	//TODO light intensity should fall off by inverse sq dist
	
	public Base_Light(Base_Scene _scn, int _lightID, double _r, double _g, double _b, double _x, double _y, double _z, double _dx, double _dy, double _dz, GeomObjType _type) {
		super(_scn, _x,_y,_z, _type);
	    minVals = this.getMinVec();
	    maxVals = this.getMaxVec();
	    postProcBBox();
	    System.out.println("Making light " + ID);
		setIsLight(true);
		setVals(_lightID, _r,_g,_b,_x,_y,_z);
	    orientation = new myVector(_dx,_dy,_dz); 
	    orientation._normalize();
	}	
	@Override
	//assumes that transRay dir is toward light.
	public rayHit intersectCheck(rayCast _ray, rayCast transRay, myMatrix[] _ctAra){  
		myVector hitNorm = new myVector(transRay.direction);
		hitNorm._mult(-1);//norm is just neg ray direction
		hitNorm._normalize();
		double t = transRay.origin._dist(getOrigin(transRay.getTime()));
		rayHit hit = transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(t), new int[]{}, t);
		//rayHit hit = new rayHit(transRay, _ray.direction, this, _ctAra, hitNorm,transRay.pointOnRay(t),t, new int[]{});		
		return hit;
	}		//point light always intersects

	//TODO textured light could give different color light to scene based on location? BATSIGNAL!
	@Override
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time){
		double v = findTextureV(isctPt,myTexture,time);	
		return new double[]{findTextureU(isctPt,v,myTexture,time),v};
	}
	@Override
	protected double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){ return 0.0; }
	@Override
	protected double findTextureV(myPoint isctPt, PImage myTexture, double time){	return 0.0;  } 
	//public boolean intersectCheck(myRay ray){  return true;}
	public void setLightColor(double _r, double _g, double _b){	this.lightColor = new myRTColor(_r,_g,_b);} 
	//sets color and position of a light
	public void setVals(int lightID, double r, double g, double b, double x, double y, double z){
		this.setLightColor(r,g,b);
		super.origin.set(x,y,z);
		this.lightID = lightID;
	}
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
	
	//probability/weighting of angle between inner and outer radii - linear
	protected double getAngleProb(double angle, double innerThetRad, double outerThetRad, double radDiff){return (angle < innerThetRad ) ? 1 : (angle > outerThetRad ) ? 0 : (outerThetRad - angle)/radDiff;}	
	//send direction vector, finds multiplier for penumbra effect
	protected double calcT_Mult(myVector dir, double time, double innerThetRad, double outerThetRad, double radDiff){
		double angle = Math.acos(-1*dir._dot(getOrientation(time)));			//intersection pt to light dir is neg light to intersection pt dir - want acos of this to get angle
		return getAngleProb(angle,innerThetRad,outerThetRad, radDiff);// (angle < innerThetRad ) ? 1 : (angle > outerThetRad ) ? 0 : (outerThetRad - angle)/radDiff;		
	}
	
	//get starting point for photon ray - will vary based on light type
	public abstract rayCast genRndPhtnRay();
	
	public myVector getOrientation(double _t){	return orientation;	}

	public String toString(){  return super.toString() + " \ncolor : " + this.lightColor + " light ID : " + this.lightID;  }
}//class myLight


