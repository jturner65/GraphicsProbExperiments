package base_RayTracer.ray;

import java.util.concurrent.ThreadLocalRandom;

import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.utils.myRTColor;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class rayCast{

	private Base_Scene scn;
	
	//private double[] currRfrIdx;
	//the transmission constant of the material this ray is inside - will have 4 elements
	public double[] currKTrans;
	
	public myPoint origin;
	public myVector originAsVec;
	public myVector direction;
	
	public double[] originHAra;
	public double[] dirHAra;
	/**
	 * for motion blur - ray has a time value
	 */
	private double time = -1;
		
	/**
	 * what generation is this ray?  primary rays are 0, reflected rays increase generation by 1 for every reflection
	 */
	public int gen;

	public rayCast(Base_Scene _scn, myPoint _origin, myVector _direction, int _gen){
		//use this constructor when making rays r0 + t(dR) (direction is direction vector
		//all vectors originate at origin
		scn = _scn;
	    scn.globRayCount++;
	    currKTrans = new double[5];
	    //currRfrIdx = new double[5];				//TODO
	    for (int i = 0; i < 5; ++i){
	      //initializing to air values for permiability and index of refraction
	      currKTrans[i] = 1;  
	    }
	    gen = _gen;
	    origin = new myPoint(_origin);
	    originAsVec = new myVector(origin);
	    originHAra = origin.asHAraPt();
	    direction = new myVector(_direction);
	    direction._normalize();
	    dirHAra = direction.asHAraVec();
	}//myray constructor (5)
	
	/**
	 * build new copy of passed ray, transformed by passed transformation.
	 * @param _otr
	 * @param trans
	 */
	public rayCast(rayCast _otr, myMatrix trans) {
		scn = _otr.scn;
		scn.globRayCount++;
	    currKTrans = new double[_otr.currKTrans.length];
	    System.arraycopy(_otr.currKTrans, 0, currKTrans, 0, currKTrans.length);
	    gen = _otr.gen;
	    double[] _originVals = trans.multVert(_otr.originHAra);
	    double[] _dirVals = trans.multVert(_otr.dirHAra);
	    
	    origin = new myPoint(_originVals[0],_originVals[1],_originVals[2]);
	    originAsVec = new myVector(origin);
	    originHAra = origin.asHAraPt();
	    direction = new myVector(_dirVals[0],_dirVals[1],_dirVals[2]);
	    //do not normalize direction since this is post transformation
	    dirHAra = direction.asHAraVec();
		
	}//copy existing ray with alternate/transformed origin and direction
	
	/**
	 * For shadow and motion blur calcs
	 * @return
	 */
	public double getTime() {
		if(time==-1) { time = ThreadLocalRandom.current().nextDouble(0,1.0);}
		return time;
	}
	
	  /**
	  *  set this ray's current 1/n transmission values - n never less than 1 currently
	  *  might be a good way to mock up a light or metamaterials
	  */
	public void setCurrKTrans(double _ktrans, double _currPerm, myRTColor _perm){
		currKTrans[0] = _ktrans;
		currKTrans[1] = _currPerm;
		currKTrans[2] = _perm.x;
		currKTrans[3] = _perm.y;
		currKTrans[4] = _perm.z;
	}
  
	public double[] getCurrKTrans(){   return currKTrans; }
	public myPoint pointOnRay(double t){
		myPoint result = new myPoint(direction);
		result._mult(t);
		result._add(origin);
	    return result;  
	}
	/**
	 * This will apply the passed transformation matrix to this ray and return a new instance of the transformed ray 
	 * pass correct matrix to use for transformation
	 * @param trans
	 * @return
	 */
	public rayCast getTransformedRay(myMatrix trans){	return new rayCast(this, trans);}//getTransformedRay

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


