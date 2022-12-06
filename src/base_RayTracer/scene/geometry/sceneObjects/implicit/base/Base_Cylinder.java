package base_RayTracer.scene.geometry.sceneObjects.implicit.base;

import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import processing.core.PImage;

public abstract class Base_Cylinder extends Base_ImplicitObject {
	protected double myHeight; 
	protected double yTop, yBottom;

	public Base_Cylinder(
			Base_Scene _scn, double _myRadius, double _myHeight, 
			double _x, double _y, double _z, double _xO,double _yO, double _zO, 
			GeomObjType _type) {
		super(_scn, _x, _y, _z, _type);
		radX= _myRadius;
		radZ = _myRadius;
		myHeight = _myHeight;
		yTop = origin.y + myHeight;//top of can
		yBottom = origin.y; //bottom of can
	    minVals = getMinVec();
	    maxVals = getMaxVec();	 		
	}//Ctor

	
	/**
	 * check if passed ray intersects with this cylinder - first using x/z for circular intersection, then planar intersection with end caps, then check which is closest and positive  
	 */
	@Override
	public final rayHit intersectCheck(rayCast _ray, rayCast _transRay, myMatrix[] _ctAra){	
		myPoint pC = originRadCalc(_transRay);
		double a = getAVal(_transRay),b = getBVal(_transRay, pC), c = getCVal(_transRay, pC), discr = ((b*b) - (4*a*c));
		
		//quadratic - check first if imaginary - if so then no intersection
		if (!(discr < 0)){//real roots exist - means ray hits x-z walls somewhere
			//find values of t - want those that give largest value of z, to indicate the closest intersection to the eye
			double discr1 = Math.pow(discr,.5),t1 = (-b + discr1)/(2*a),t2 = (-b - discr1)/(2*a);
			//set the t value of the intersection to be the minimum of these two values (which would be the edge closest to the eye/origin of the ray)
			// (the other value, if one exists, is on the other side of the sphere)
			double cyltVal = Math.min(t1,t2), cyltOtr = Math.max(t1, t2);
			if (cyltVal < epsVal){//if min less than 0 then that means it intersects behind the viewer.  pick other t
				double tmp = cyltOtr;
				cyltOtr = cyltVal;
				cyltVal = tmp;				
				if (cyltVal < epsVal){//if still less than 0 then that means both intersections behind ray origin - this isn't an intersection, dont draw
					return new rayHit(false);	
				}//if both t's are less than 0 then don't paint anything
			}//if the min t val is less than 0
			//Tube or Capped cylinder check
			return intersectCheck_Indiv(_ray, _transRay, _ctAra, cyltVal, cyltOtr);
		}
		return new rayHit(false);	
	}
	
	/**
	 * Check specific cylinder instance for ray hit.
	 * @param _ray ray cast
	 * @param _transRay transformed ray cast
	 * @param _ctAra transformation matrix for the ray
	 * @param cyltVal closest t value to origin of ray
	 * @param cyltOtr furthest t value from origin of ray
	 * @return
	 */
	protected abstract rayHit intersectCheck_Indiv(rayCast _ray, rayCast _transRay, myMatrix[] _ctAra, double cyltVal, double cyltOtr);
	
	/**
	 * find the u (x) value in a texture to plot to a specific point on the object	TODO
	 */
	@Override
	protected final double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){double u = 0.0;return u;}     	
  	/**
  	 * find the v (y) value in a texture to plot to a specific point on the object  	TODO
  	 */
	@Override
	protected final double findTextureV(myPoint isctPt, PImage myTexture, double time){		double v = 0.0; 		return v; 	}   
  	/**
  	 * calculates the "A" value for the quadratic equation determining the potential intersection of the passed ray with a cylinder of given radius and center
  	 * @param _ray
  	 * @return
  	 */
  	protected final double getAVal(rayCast _ray){return ((_ray.direction.x/radX) * (_ray.direction.x/radX)) + ((_ray.direction.z/radZ) * (_ray.direction.z/radZ));  	}  
  	/**
  	 * calculates the "B" value for the quadratic equation determining the potential intersection of the passed ray with a cylinder of given radius and center
  	 * @param _ray
  	 * @param pC
  	 * @return
  	 */
  	protected final double getBVal(rayCast _ray, myPoint pC){return 2*(((_ray.direction.x/radX) * pC.x) + ((_ray.direction.z/radZ) * pC.z)); }  
  	/**
  	 * calculates the "C" value for the quadratic equation determining the potential intersection of the passed ray with a cylinder of given radius and center
  	 * @param _ray
  	 * @param pC
  	 * @return
  	 */
  	protected final double getCVal(rayCast _ray, myPoint pC){return (pC.x * pC.x) + (pC.z * pC.z) - 1; }  	
	
	public final double getMyHeight(){  return myHeight;}  
	
	@Override
	public final myVector getMaxVec(){
		myVector res = new myVector(origin);
		double tmpVal = radX+radZ;
		res._add(tmpVal,myHeight,tmpVal);
		return res;
	}	
	@Override
	public final myVector getMinVec(){
		myVector res = new myVector(origin);
		double tmpVal = radX+radZ;
		res._add(-tmpVal,0,-tmpVal);
		return res;
	}	
	@Override
 	public String toString(){  return super.toString() + "\n"+ type +"-Specific : Height : " + myHeight + " radius : [" + radX+ "|" +radZ+"] y End : " + yBottom; 	}

}//class Base_Cylinder
