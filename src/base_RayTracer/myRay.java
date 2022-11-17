package base_RayTracer;

import java.util.concurrent.ThreadLocalRandom;

import base_RayTracer.scene.myScene;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myRay{

  //the transmission constant of the material this ray is inside - will have 4 elements
	public myScene scn;
	public double[] currRfrIdx;
	public double[] currKTrans;
	
	public myPoint origin;
	public myVector originAsVec;
	public myVector direction;
	
	public double[] originAra;
	public double[] dirAra;
	/**
	 * for motion blur - ray has a time value
	 */
	private double time = -1;
		
	/**
	 * what generation is this ray?  primary rays are 0, reflected rays increase generation by 1 for every reflection
	 */
	public int gen;

	public myRay(myScene _scn, myPoint _origin, myVector _direction, int _gen){
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
	    this.origin = new myPoint(_origin);
	    this.originAsVec = new myVector(this.origin);
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
	    this.originAsVec.set(originVals[0],originVals[1],originVals[2]);
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
	public myPoint pointOnRay(double t){
		myPoint result = new myPoint(direction);
		result._mult(t);
		result._add(origin);
	    return result;  
	}
	/**
	 * these are placed here for potential multi-threading - will pivot threads on rays 
	 * this will apply the inverse of the current transformation matrix to the ray passed as a parameter and return the transformed ray 
	 * pass correct matrix to use for transformation
	 * @param ray
	 * @param trans
	 * @return
	 */
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

	/**
	 * get transformed/inverse transformed point - homogeneous coords
	 * @param pt
	 * @param trans
	 * @return
	 */
	public myPoint getTransformedPt(myPoint pt, myMatrix trans){
		double[] newPtAra = trans.multVert(pt.asHAraPt());	
		myPoint newPt = new myPoint(newPtAra[0],newPtAra[1],newPtAra[2]);
		return newPt;
	}
	
	/**
	 * get transformed/inverse transformed vector - homogeneous coords
	 * @param vec
	 * @param trans
	 * @return
	 */
	public myVector getTransformedVec(myVector vec, myMatrix trans){
		double[] newVecAra = trans.multVert(vec.asHAraVec());		
		myVector newVec = new myVector(newVecAra[0],newVecAra[1],newVecAra[2]);
		return newVec;
	}	
	/**
	 * build object for hit - contains all relevant info from intersection, including CTM matrix array 
	 * args ara : idx 0 is cylinder stuff, idx 1 is bound box plane idx (0-5) args is used only in normal calc
	 * @param _obj
	 * @param _rawRayDir
	 * @param _ctMtrx
	 * @param pt
	 * @param args
	 * @param _t
	 * @return
	 */
	public rayHit objHit(Base_Geometry _obj, myVector _rawRayDir, myMatrix[] _ctMtrx, myPoint pt, int[] args, double _t){
		myPoint fwdTransPt = getTransformedPt(pt, _ctMtrx[Base_Geometry.glblIDX]);		//hit location in world space		
		myVector _newNorm = getTransformedVec(_obj.getNormalAtPoint(pt,args), _ctMtrx[Base_Geometry.invTransIDX]);
		_newNorm._normalize();
 		rayHit _hit = new rayHit(this, _rawRayDir, _obj,  _ctMtrx, _newNorm, pt,fwdTransPt,  _t,args);
 		return _hit;
	}//
	public String toString(){   return "Origin : " + origin + " dir : " + direction + " Gen  : " + gen; }
}//class myRay


