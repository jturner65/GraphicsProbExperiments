package base_RayTracer.scene.geometry.sceneObjects.implicit;

import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_Utils_Objects.myMatrix;
import base_Utils_Objects.myVector;
import processing.core.PImage;

public class myHollow_Cylinder extends myImpObject{
	protected double myHeight, yTop, yBottom;

	public myHollow_Cylinder(myScene _p, double _myRadius, double _myHeight, double x, double y, double z, double xO,double yO, double zO) {
		super(_p, x,y,z);
		type = objType.Hollow_Cylinder;
		radX= _myRadius;
		radZ = _myRadius;
		myHeight = _myHeight;
		yTop = origin.y + myHeight;//top of can
		yBottom = origin.y; //bottom of can
	    minVals = this.getMinVec();
	    maxVals = this.getMaxVec();	    
		postProcBBox();				//cnstrct and define bbox
	}
	 
	//check if passed ray intersects with this cylinder - first using x/z for circular intersection, then planar intersection with end caps, then check which is closest and positive  
	public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){		
		double a = getAVal(transRay), b = getBVal(transRay), c = getCVal(transRay), discr = ((b*b) - (4*a*c));
		//quadratic - check first if imaginary - if so then no intersection
		if (!(discr < 0)){//real roots exist - means ray hits x-z walls somewhere
			double discr1 = Math.pow(discr,.5),t1 = (-b + discr1)/(2*a),t2 = (-b - discr1)/(2*a);
			double cyltVal = Math.min(t1,t2), cyltOtr = Math.max(t1, t2);
			if (cyltVal < -epsVal){//if min less than 0 then that means it intersects behind the viewer.  pick other t
				double tmp = cyltOtr; 	cyltOtr = cyltVal;	cyltVal = tmp;				
				if (cyltVal < -epsVal){		return new rayHit(false);		}//if both t's are less than 0 then don't paint anything
			}//if the min t val is less than 0
			//light very near to bounds of cylinder, need check to avoid shadow shennanigans
			double yInt1 = transRay.origin.y + (cyltVal * transRay.direction.y);
			//in args ara 0 means hitting outside of cylinder, 1 means hitting inside
			if((cyltVal > epsVal) && (yInt1 > yBottom ) && (yInt1 < yTop)){return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(cyltVal),new int[]{0},cyltVal);}
			double yInt2 = transRay.origin.y + (cyltOtr * transRay.direction.y);
			if((cyltOtr > epsVal) && (yInt2 > yBottom ) && (yInt2 < yTop)){return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(cyltOtr),new int[]{1},cyltOtr);}
		} 
		return new rayHit(false);	    
	}//intersectCheck method

  	//returns surface normal of cylinder at given point on cylinder	
  	@Override
  	public myVector getNormalAtPoint(myVector pt,  int[] args){//in args ara 0 means hitting outside of cylinder, 1 means hitting inside	
  		myVector result= (args[0] == 1) ? new myVector((origin.x - pt.x), 0, (origin.z - pt.z)) : new myVector((pt.x - origin.x), 0, (pt.z - origin.z));
  		result._normalize();
  		if (rFlags[invertedIDX]){result._mult(-1);}
  		//System.out.println("normal :" + result.toStrBrf() + " @ pt :"+ pt.toStrBrf());
  		return result;
  	}//method getNormalAtPoint  		

	//find the u (x) value in a texture to plot to a specific point on the object	TODO
	@Override
	protected double findTextureU(myVector isctPt, double v, PImage myTexture, double time){double u = 0.0;return u;}     	
  	//find the v (y) value in a texture to plot to a specific point on the object  	TODO
	@Override
	protected double findTextureV(myVector isctPt, PImage myTexture, double time){		double v = 0.0; 		return v; 	}   
	
  	public double getMyHeight(){  return myHeight;}  
  	
  	// calculates the "A" value for the quadratic equation determining the potential intersection of the passed ray with a cylinder of given radius and center
  	public double getAVal(myRay _ray){	return ((_ray.direction.x/radX) * (_ray.direction.x/radX)) + ((_ray.direction.z/radZ) * (_ray.direction.z/radZ));  	}  
  	//calculates the "B" value for the quadratic equation determining the potential intersection of the passed ray with a cylinder of given radius and center
  	public double getBVal(myRay _ray){myVector pC = originRadCalc(_ray);return 2*(((_ray.direction.x/radX) * pC.x) + ((_ray.direction.z/radZ) * pC.z)); }  
  	//calculates the "C" value for the quadratic equation determining the potential intersection of the passed ray with a cylinder of given radius and center
  	public double getCVal(myRay _ray){	myVector pC = originRadCalc(_ray);	return (pC.x * pC.x) + (pC.z * pC.z) - 1; }  
  	
	@Override
	public myVector getOrigin(double _t){	return origin;	}
	@Override
	public myVector getMaxVec(){
		myVector res = new myVector(origin);
		double tmpVal = radX+radZ;
		res._add(tmpVal,myHeight,tmpVal);
		return res;
	}	
	@Override
	public myVector getMinVec(){
		myVector res = new myVector(origin);
		double tmpVal = radX+radZ;
		res._add(-tmpVal,0,-tmpVal);
		return res;
	}	
  	public String toString(){  return super.toString() + "\n"+ type +"-Specific : Height : " + myHeight + " radius : [" + radX+ "|" +radZ+"] y End : " + yBottom; 	}

}//myHollow_Cylinder
