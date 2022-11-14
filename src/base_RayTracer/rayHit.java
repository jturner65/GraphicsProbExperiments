package base_RayTracer;

import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.scene.shaders.myObjShader;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * this class stores information regarding a ray hit - the owning ray, the t value, the object hit, the hit location, the object normal at that hit, the object's transformation matrices array
 * @author 7strb
 *
 */
public class rayHit implements Comparable<rayHit>{
	public myRay transRay;
	public Base_Geometry obj;
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

	public rayHit(myRay _tray, myVector _rawRayDir, Base_Geometry _obj, myMatrix[] _ctMtrx, myVector _objNorm, myVector _hitLoc, myVector _fwdTransHitLoc, double _t, int[] _iSectArgs){
		transRay = _tray;
		isHit = true;
		obj = _obj;
		shdr = _obj.shdr;
		CTMara = _ctMtrx;
		objNorm = new myVector(_objNorm);
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
		double[] newNormDir = CTMara[Base_Geometry.adjIDX].multVert(norm.asHAraVec());//fix for scaling - N' == R . S^-1 . N -> CTMadj  
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
