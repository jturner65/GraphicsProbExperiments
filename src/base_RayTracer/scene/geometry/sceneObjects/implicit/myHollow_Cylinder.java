package base_RayTracer.scene.geometry.sceneObjects.implicit;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.implicit.base.Base_Cylinder;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * Class defining a hollow tube via implicit equation
 * @author 7strb
 *
 */
public class myHollow_Cylinder extends Base_Cylinder{

	public myHollow_Cylinder(Base_Scene _p, double _myRadius, double _myHeight, double x, double y, double z, double xO,double yO, double zO) {
		super(_p, _myRadius, _myHeight, x, y, z, xO, yO, zO, GeomObjType.Hollow_Cylinder);
		postProcBBox();				//cnstrct and define bbox
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
	@Override
	protected final rayHit intersectCheck_Indiv(rayCast _ray, rayCast transRay, myMatrix[] _ctAra, double cyltVal, double cyltOtr){	
		//light very near to bounds of cylinder, need check to avoid shadow shennanigans
		double yInt1 = transRay.origin.y + (cyltVal * transRay.direction.y);
		//in args ara 0 means hitting outside of cylinder, 1 means hitting inside
		if((cyltVal > epsVal) && (yInt1 > yBottom ) && (yInt1 < yTop)){return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(cyltVal),new int[]{0},cyltVal);}
		double yInt2 = transRay.origin.y + (cyltOtr * transRay.direction.y);
		if((cyltOtr > epsVal) && (yInt2 > yBottom ) && (yInt2 < yTop)){return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(cyltOtr),new int[]{1},cyltOtr);}

		return new rayHit(false);	    
	}//intersectCheck method

  	/**
  	 * returns surface normal of cylinder at given point on cylinder	
  	 */
  	@Override
  	public myVector getNormalAtPoint(myPoint pt,  int[] args){//in args ara 0 means hitting outside of cylinder, 1 means hitting inside	
  		myVector result= (args[0] == 1) ? new myVector((origin.x - pt.x), 0, (origin.z - pt.z)) : new myVector((pt.x - origin.x), 0, (pt.z - origin.z));
  		result._normalize();
  		if (isInverted()){result._mult(-1);}
  		//System.out.println("normal :" + result.toStrBrf() + " @ pt :"+ pt.toStrBrf());
  		return result;
  	}//method getNormalAtPoint  		

  	public String toString(){  return super.toString() + "\n"+ type +"-Specific : Height : " + myHeight + " radius : [" + radX+ "|" +radZ+"] y End : " + yBottom +"\n"; 	}

}//myHollow_Cylinder
