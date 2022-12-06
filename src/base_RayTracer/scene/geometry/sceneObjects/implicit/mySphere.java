package base_RayTracer.scene.geometry.sceneObjects.implicit;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.Geom_ObjType;
import base_RayTracer.scene.geometry.sceneObjects.implicit.base.Base_ImplicitObject;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

public class mySphere extends Base_ImplicitObject{
	  
		public mySphere(Base_Scene _p, double _radX, double _radY, double _radZ, double x, double y, double z){
			super(_p, x,y,z);
			type = Geom_ObjType.Sphere;
			radX = _radX;
			radY = _radY;
			radZ = _radZ;
		    minVals = getMinVec();
		    maxVals = getMaxVec();	    
			postProcBBox();				//cnstrct and define bbox
		}
	  
		public mySphere(Base_Scene _p, double myRadius, double x, double y, double z, boolean active){	this(_p, myRadius, myRadius, myRadius,x,y,z);}    
		/**
		 * calculates the "A" value for the quadratic equation determining the potential intersection of this ray with a sphere of given radius and center
		 * @param ray
		 * @return
		 */
		public double getAVal(rayCast _ray){return((_ray.direction.x/radX) * (_ray.direction.x/radX)) + ((_ray.direction.y/radY) * (_ray.direction.y/radY)) + ((_ray.direction.z/radZ) * (_ray.direction.z/radZ));}
	  	/**
	  	 * calculates the "B" value for the quadratic equation determining the potential intersection of this ray with a sphere of given radius and center
	  	 * @param ray
	  	 * @return
	  	 */
		public double getBVal(rayCast _ray, myPoint pC){
			double result = 0.0;
			result = 2*(((_ray.direction.x/radX) * pC.x) + ((_ray.direction.y/radY) * pC.y) + ((_ray.direction.z/radZ) * pC.z));
			return result;  
		}  
		/**
		 * calculates the "C" value for the quadratic equation determining the potential intersection of this ray with a sphere of given radius and center
		 * @param ray
		 * @return
		 */
		public double getCVal(rayCast _ray, myPoint pC){
			double result = 0.0;  
		    //this value should actually be rayorigin - sphereorigin coords - originRadCalc accounts for radius in each direction
			result = (pC.x * pC.x) + (pC.y * pC.y) + (pC.z * pC.z) - 1;    
			return result;
		}  
		
		/**
		 * returns surface normal of sphere at given point on sphere
		 */
		@Override
		public myVector getNormalAtPoint(myPoint pt, int[] args){
			myVector result = new myVector(pt);
			result._sub(origin);
			result._normalize();
			if (isInverted()){	result._mult(-1.0);   }
	    return result;
		}//method getNormalAtPoint	
		
		/**
		 * check if the passed ray intersects with this sphere
		 */
		@Override
		public rayHit intersectCheck(rayCast _ray, rayCast transRay, myMatrix[] _ctAra){
//			if(!_bbox.intersectCheck(ray, _ctAra).isHit){return new rayHit(false);	}
//			myRay transRay = ray.getTransformedRay(ray, _ctAra[invIDX]);
			//boolean result = false;
			myPoint pC = originRadCalc(transRay);
			double a = getAVal(transRay), ta = 2*a, 
					b = getBVal(transRay, pC), 
					c = getCVal(transRay, pC), 
					discr = ((b*b) - (2*ta*c));
			//quadratic - check first if imaginary - if so then no intersection
			if (discr >= 0){
				//result = true;  
				//find values of t - want those that give largest value of z, to indicate the closest intersection to the eye
				double discr1 = Math.pow(discr,.5), t1 = (-1*b + discr1)/(ta), t2 = (-1*b - discr1)/(ta);
				//set the t value of the intersection to be the minimum of these two values (which would be the edge closest to the eye/origin of the ray)
				double tVal = Math.min(t1,t2);
				if (tVal < epsVal){//if min less than 0 then that means it intersects behind the viewer.  pick other t
					tVal = MyMathUtils.max(t1,t2);
					if (tVal < epsVal){	return new rayHit(false);}		//if still less than 0 then that means both intersections behind viewer - this isn't an intersection, dont draw
				}//if the min t val is less than 0
				return transRay.objHit(this,  _ray.direction,_ctAra, transRay.pointOnRay(tVal),null,tVal); }
			else{			return new rayHit(false);	}    
		}//intersectCheck method
		//find the u (x) value in a texture to plot to a specific point on the sphere
		@Override
		protected double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){		
			myPoint t_origin = getOrigin(time);
			double u = 0.0, q,a0, a1, a2, shWm1 = myTexture.width-1, z1 = (isctPt.z - t_origin.z);	  
			q = v/(myTexture.height-1);//normalize v to be 0-1
			a0 = (isctPt.x - t_origin.x)/ (radX);
			a0 = (a0 > 1) ? 1 : (a0 < -1) ? -1 : a0;
			a1 = ( Math.sin(q* MyMathUtils.PI));
			a2 = ( Math.abs(a1) < epsVal) ? 1 : a0/a1;
			u = (z1 <= epsVal) ? ((shWm1 * ( Math.acos(a2))/ (MyMathUtils.TWO_PI)) + shWm1/2.0f) : 
						shWm1 - ((shWm1 * ( Math.acos(a2))/ (MyMathUtils.TWO_PI)) + shWm1/2.0f);
			u = (u < 0) ? 0 : (u > shWm1) ? shWm1 : u;
			return u;	
		}//method findTexture    
	        
		// find the v (y) value in a texture to plot to a specific point on the sphere  remember top of texture should correspond to 0, bottom to texture.height.
		@Override
		protected double findTextureV(myPoint isctPt, PImage myTexture, double time){
			myPoint t_origin = getOrigin(time);
			double v = 0.0;
			//double a0 = super.rayIntersectPoint[gen].y - origin.y;
			double a0 = isctPt.y - t_origin.y;
			double a1 = a0 /(radY);
			a1 = (a1 > 1)? 1 : (a1 < -1) ?  -1 : a1;    
			v = (myTexture.height-1) * Math.acos(a1)/MyMathUtils.PI;
			return v;
		}//method findTextureV    

		@Override
		public myVector getMaxVec(){
			myVector res = new myVector(origin);
			double tmpVal = radX + radY+radZ;			//L1 norm for enclosing box
			res._add(tmpVal,tmpVal,tmpVal);
			return res;
		}	
		@Override
		public myVector getMinVec(){
			myVector res = new myVector(origin);
			double tmpVal = radX + radY+radZ;			//L1 norm for enclosing box
			res._add(-tmpVal,-tmpVal,-tmpVal);
			return res;
		}
		public String toString(){   return super.toString() + "\n"+ type +"-Specific : Radius : [" + radX+ "|" + radY+  "|" +radZ+"]"; }
	}//class mySphere