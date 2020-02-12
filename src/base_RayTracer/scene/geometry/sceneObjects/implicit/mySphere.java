package base_RayTracer.scene.geometry.sceneObjects.implicit;

import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

public class mySphere extends myImpObject{
	  
		public mySphere(myScene _p, double _radX, double _radY, double _radZ, double x, double y, double z){
			super(_p, x,y,z);
			type = objType.Sphere;
			radX = _radX;
			radY = _radY;
			radZ = _radZ;
		    minVals = this.getMinVec();
		    maxVals = this.getMaxVec();	    
			postProcBBox();				//cnstrct and define bbox
		}
	  
		public mySphere(myScene _p, double myRadius, double x, double y, double z, boolean active){	this(_p, myRadius, myRadius, myRadius,x,y,z);}    
		// calculates the "A" value for the quadratic equation determining the potential intersection of this ray with a sphere of given radius and center
		public double getAVal(myRay ray){return((ray.direction.x/radX) * (ray.direction.x/radX)) + ((ray.direction.y/radY) * (ray.direction.y/radY)) + ((ray.direction.z/radZ) * (ray.direction.z/radZ));}
	  	// calculates the "B" value for the quadratic equation determining the potential intersection of this ray with a sphere of given radius and center
		public double getBVal(myRay ray){
			double result = 0.0;
			myVector pC = this.originRadCalc(ray);
			result = 2*(((ray.direction.x/radX) * pC.x) + ((ray.direction.y/radY) * pC.y) + ((ray.direction.z/radZ) * pC.z));
			return result;  
		}  
		// calculates the "C" value for the quadratic equation determining the potential intersection of this ray with a sphere of given radius and center
		public double getCVal(myRay ray){
			double result = 0.0;  
		   //don't need to worry about pC components being negative because of square
			//this value should actually be rayorigin - sphereorigin coords - originRadCalc accounts for radius in each direction
			myVector pC = this.originRadCalc(ray);
			result = (pC.x * pC.x) + (pC.y * pC.y) + (pC.z * pC.z) - 1;    
			return result;
		}  
		// returns surface normal of sphere at given point on sphere
		public myVector getNormalAtPoint(myVector pt, int[] args){
			myVector result = new myVector(pt);
			result._sub(this.origin);
			result._normalize();
			if (rFlags[invertedIDX]){	result._mult(-1.0);   }
	    return result;
		}//method getNormalAtPoint	
		//check if the passed ray intersects with this sphere
		public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){
//			if(!_bbox.intersectCheck(ray, _ctAra).isHit){return new rayHit(false);	}
//			myRay transRay = ray.getTransformedRay(ray, _ctAra[invIDX]);
			//boolean result = false;
			double a = this.getAVal(transRay), ta = 2*a, b = this.getBVal(transRay), c = this.getCVal(transRay), discr = ((b*b) - (2*ta*c));
			//quadratic - check first if imaginary - if so then no intersection
			if (!(discr < 0)){
				//result = true;  
				//find values of t - want those that give largest value of z, to indicate the closest intersection to the eye
				double discr1 = Math.pow(discr,.5), t1 = (-1*b + discr1)/(ta), t2 = (-1*b - discr1)/(ta);
				//set the t value of the intersection to be the minimum of these two values (which would be the edge closest to the eye/origin of the ray)
				double tVal = Math.min(t1,t2);
				if (tVal < epsVal){//if min less than 0 then that means it intersects behind the viewer.  pick other t
					tVal = Math.max(t1,t2);
					if (tVal < epsVal){	return new rayHit(false);}		//if still less than 0 then that means both intersections behind viewer - this isn't an intersection, dont draw
				}//if the min t val is less than 0
				return transRay.objHit(this,  _ray.direction,_ctAra, transRay.pointOnRay(tVal),null,tVal); }
			else{			return new rayHit(false);	}    
		}//intersectCheck method
		//find the u (x) value in a texture to plot to a specific point on the sphere
		@Override
		protected double findTextureU(myVector isctPt, double v, PImage myTexture, double time){		
			myVector t_origin = this.getOrigin(time);
			double u = 0.0, q,a0, a1, a2, shWm1 = myTexture.width-1, z1 = (isctPt.z - t_origin.z);	  
			q = v/(myTexture.height-1);//normalize v to be 0-1
			a0 = (isctPt.x - t_origin.x)/ (this.radX);
			a0 = (a0 > 1) ? 1 : (a0 < -1) ? -1 : a0;
			a1 = ( Math.sin(q* Math.PI));
			a2 = ( Math.abs(a1) < epsVal) ? 1 : a0/a1;
			u = (z1 <= epsVal) ? ((shWm1 * ( Math.acos(a2))/ (MyMathUtils.twoPi)) + shWm1/2.0f) : 
						shWm1 - ((shWm1 * ( Math.acos(a2))/ (MyMathUtils.twoPi)) + shWm1/2.0f);
			u = (u < 0) ? 0 : (u > shWm1) ? shWm1 : u;
			return u;	
		}//method findTexture    
	        
		// find the v (y) value in a texture to plot to a specific point on the sphere  remember top of texture should correspond to 0, bottom to texture.height.
		@Override
		protected double findTextureV(myVector isctPt, PImage myTexture, double time){
			myVector t_origin = this.getOrigin(time);
			double v = 0.0;
			//double a0 = super.rayIntersectPoint[gen].y - this.origin.y;
			double a0 = isctPt.y - t_origin.y;
			double a1 = a0 /(this.radY);
			a1 = (a1 > 1)? 1 : (a1 < -1) ?  -1 : a1;    
			v = (myTexture.height-1) * Math.acos(a1)/Math.PI;
			return v;
		}//method findTextureV    
		
		@Override
		public myVector getOrigin(double _t){	return origin;	}
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
		public String toString(){   return super.toString() + "\n"+ type +"-Specific : Radius : [" + this.radX+ "|" + this.radY+  "|" +this.radZ+"]"; }
	}//class mySphere