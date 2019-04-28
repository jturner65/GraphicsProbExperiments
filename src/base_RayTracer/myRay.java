package base_RayTracer;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import base_Utils_Objects.*;

public class myRay{

  //the transmission constant of the material this ray is inside - will have 4 elements
	public myScene scn;
	public double[] currRfrIdx;
	public double[] currKTrans;
	
	public myVector origin;  
	public myVector direction;
	
	public double[] originAra;
	public double[] dirAra;
	//public double scale;
	//for motion blur - ray has a time value
	private double time = -1;
		
	//what generation is this ray?  primary rays are 0, reflected rays increase generation by 1 for every reflection
	public int gen;

	public myRay(myScene _scn, myVector _origin, myVector _direction, int _gen){
		//use this constructor when making rays r0 + t(dR) (direction is direction vector
		//all vectors originate at origin
		this.scn = _scn;
	    this.scn.globRayCount++;
	    currKTrans = new double[5];
	    currRfrIdx = new double[5];				//TODO
	    for (int i = 0; i < 5; i++){
	      //initializing to air values for permiability and index of refraction
	      this.currKTrans[i] = 1;  
	    }
	    this.gen = _gen;
	    this.origin = new myVector(_origin);
	    this.originAra = this.origin.asArray();
	    this.direction = new myVector(_direction);
	    this.direction._normalize();
	    this.dirAra = this.direction.asArray();
	    //sorted list of all hits for this ray
	    //scale = 1.0;
	    //for blur
	    //this.time = ThreadLocalRandom.current().nextDouble(0,1.0);
	}//myray constructor (5)
	
	public double getTime() {
		if(time==-1) { time = ThreadLocalRandom.current().nextDouble(0,1.0);}
		return time;
	}
	
	//used by ray transformation of object's inv CTM
	private void setRayVals(double[] originVals, double[] dirVals){
	    this.origin.set(originVals[0],originVals[1],originVals[2]);
	    this.originAra = this.origin.asArray();
	    this.direction.set(dirVals[0],dirVals[1],dirVals[2]);
	    this.dirAra = this.direction.asArray();
	    //this.scale = this.direction._mag();
	    //don't want to normalize direction when this is a transformed ray, or t's won't correspond properly 
	    //^- normalizng here was the cause of the weird shadow edge on the concentric cube image
	    //if(normDir){
	    //this.direction._normalize();//}
	}
		
	  /**
	  *  set this ray's current 1/n transmission values - n never less than 1 currently
	  *  might be a good way to mock up a light or metamaterials
	  */
	public void setCurrKTrans(double _ktrans, double _currPerm, myColor _perm){
		this.currKTrans[0] = _ktrans;
		this.currKTrans[1] = _currPerm;
		this.currKTrans[2] = _perm.RGB.x;
		this.currKTrans[3] = _perm.RGB.y;
		this.currKTrans[4] = _perm.RGB.z;
	}
	
	public void setCurrKTrans(double[] vals){for(int i=0;i<currKTrans.length;++i){currKTrans[i]=vals[i];}}
  
	public double[] getCurrKTrans(){   return currKTrans; }
	public myVector pointOnRay(double t){
		myVector result = new myVector(direction);
		result._mult(t);
		result._add(origin);
	    return result;  
	}
	//these are placed here for potential multi-threading - will pivot threads on rays
	//this will apply the inverse of the current transformation matrix to the ray passed as a parameter and return the transformed ray
	//pass correct matrix to use for transformation
	public myRay getTransformedRay(myRay ray, myMatrix trans){
		double[] rayOrigin,rayDirection;
		ray.direction._normalize();
		rayOrigin = trans.multVert(ray.origin.asHAraPt());
		rayDirection = trans.multVert(ray.direction.asHAraVec());
		//make new ray based on these new quantitiies
		myRay newRay = new myRay(scn, ray.origin, ray.direction, ray.gen);
		newRay.setRayVals(rayOrigin, rayDirection);
		newRay.setCurrKTrans(ray.currKTrans);
		//don't want to normalize, or t's won't correspond properly <--YES
		return newRay;
	}//getTransformedRay

	//get transformed/inverse transformed point - homogeneous coords
	public myVector getTransformedPt(myVector pt, myMatrix trans){
		double[] newPtAra = trans.multVert(pt.asHAraPt());	
		myVector newPt = new myVector(newPtAra[0],newPtAra[1],newPtAra[2]);
		return newPt;
	}
	
	//get transformed/inverse transformed vector - homogeneous coords
	public myVector getTransformedVec(myVector vec, myMatrix trans){
		double[] newVecAra = trans.multVert(vec.asHAraVec());		
		myVector newVec = new myVector(newVecAra[0],newVecAra[1],newVecAra[2]);
		return newVec;
	}	
	//build object for hit - contains all relevant info from intersection, including CTM matrix array
	//args ara : idx 0 is cylinder stuff, idx 1 is bound box plane idx (0-5) args is used only in normal calc
	public rayHit objHit(myGeomBase _obj, myVector _rawRayDir, myMatrix[] _ctMtrx, myVector pt, int[] args, double _t){
		myVector fwdTransPt = getTransformedPt(pt, _ctMtrx[_obj.glblIDX]);		//hit location in world space		
		myVector _newNorm = getTransformedVec(_obj.getNormalAtPoint(pt,args), _ctMtrx[_obj.adjIDX]);
		_newNorm._normalize();
 		rayHit _hit = new rayHit(this, _rawRayDir, _obj,  _ctMtrx, _newNorm, pt,fwdTransPt,  _t,args);
 		return _hit;
	}//
	public String toString(){   return "Origin : " + origin + " dir : " + direction + " Gen  : " + gen; }
}//class myRay

//this class stores information regarding a ray hit - the owning ray, the t value, the object hit, the hit location, the object normal at that hit, the object's transformation matrices array
class rayHit implements Comparable<rayHit>{
	public myRay transRay;
	public myGeomBase obj;
	public myVector objNorm, hitLoc, fwdTransHitLoc, fwdTransRayDir;
	public myObjShader shdr;				//what shader to use for the hit object
	public double t, ltMult;				//certain objects have multiple hit values - spotlight has light multiplier to make penumbra
	public int[] iSectArgs;
	public boolean isHit;
	public double[] phtnPwr;
	//ara of object hit by ray that this object represents 
	public myMatrix[] CTMara;
	public final int 
			glblIDX = 0,
			invIDX = 1,
			transIDX = 2,
			adjIDX = 3;

	public rayHit(myRay _tray, myVector _rawRayDir, myGeomBase _obj, myMatrix[] _ctMtrx, myVector _objNorm, myVector _hitLoc, myVector _fwdTransHitLoc, double _t, int[] _iSectArgs){
		transRay = _tray;
		isHit = true;
		obj = _obj;
		shdr = _obj.shdr;
		CTMara = _ctMtrx;
		objNorm = new myVector(0,0,0);
		objNorm.set(_objNorm);
		t = _t;
		hitLoc = _hitLoc;
		fwdTransHitLoc = transRay.getTransformedPt(hitLoc, CTMara[glblIDX]);		//hit location in world space
		fwdTransRayDir = new myVector(_rawRayDir);
		iSectArgs = _iSectArgs;
		ltMult = 1;						//initialize to be full on for light.  only spotlight modifies this value
	}
	//used to represent a miss - div is ID of object that misses - use it to make t different for every object while still much bigger than any valid t values
	public rayHit(boolean _isHit){
		isHit = _isHit;
		t = Double.MAX_VALUE;
	}
	//enable recalculation of hit normal based on modified/updated CTMara
	public void reCalcCTMHitNorm(myMatrix[] _ctMtrx){
		CTMara = _ctMtrx;
		fwdTransHitLoc = transRay.getTransformedPt(hitLoc, CTMara[glblIDX]);
		myVector norm = obj.getNormalAtPoint(hitLoc,iSectArgs);
		double[] newNormDir = CTMara[obj.adjIDX].multVert(norm.asHAraVec());//fix for scaling - N' == R . S^-1 . N -> CTMadj  
		objNorm = new myVector(newNormDir);
		objNorm._normalize();
	}
	
	//compared by t value - lower t means hit gets higher precedence in a map
	@Override
	public int compareTo(rayHit _rh) {	return Double.compare(t, _rh.t);}
	@Override
	public String toString(){
		String res = "Hit : "+transRay+" hits object : " + obj.ID + " at location : " + hitLoc + " with ray t = :"+String.format("%.2f",t) + " and normal @ loc : " + objNorm + "\n";
		return res;
	}
	
}//class rayHit


