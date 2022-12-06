package base_RayTracer.scene.geometry.sceneObjects.planar;

import base_RayTracer.ray.rayCast;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.planar.base.Base_PlanarObject;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

/**
 * infinite plane object
 * @author 7strb
 *
 */
public class myPlane extends Base_PlanarObject{	
	public myPlane(Base_Scene _scn){
		super(_scn, 4, GeomObjType.Plane);
	}
	/**
	 * Given a planar equation's coefficients, derive 4 vertices for the plane using coplanar orthogonal vectors
	 * @param _a
	 * @param _b
	 * @param _c
	 * @param _d
	 * @param scale how far apart the vertices should be
	 */
	public void setPlaneVals(double _a, double _b, double _c, double _d, double scale){
		myVector tmpNorm = new myVector( _a, _b, _c);
		double mag = tmpNorm.magn;
		tmpNorm._normalize();	
		double pEqA = tmpNorm.x;
		double pEqB = tmpNorm.y;
		double pEqC = tmpNorm.z;
		double pEqD = _d/mag;		
		myVector rotVec = new myVector(pEqB, pEqC, pEqA);//vector to cross N with 
		if((pEqA == pEqB) && (pEqA == pEqC)){	rotVec._add(1,0,0);		}//if all equal then shifting above them gives no benefit
		rotVec._normalize();
		//basis vex in plane :
		myVector inPlaneU = tmpNorm._cross(rotVec),//any vector perp to N will be in plane
		inPlaneV = tmpNorm._cross(inPlaneU);		//in other direction 
		//Scale basis vectors to spread planar points apart
		if (scale != 1.0) {
			inPlaneU._mult(scale);
			inPlaneV._mult(scale);
		}
		int idx = 7;//x,y,z
		double sum = pEqA + pEqB + pEqC;					//potential point coords
		if(sum == 0){sum = pEqA + pEqB;idx = 6; 			//x,y,!z
			if(sum == 0){sum = pEqA + pEqC;idx = 5;			//x,!y,z
				if(sum == 0){sum = pEqB + pEqC;idx = 3;}}}	//!x,y,z		
		myPoint planePt = new myPoint(((idx & 4) == 4 ? -pEqD/sum : 0),((idx & 2) == 2 ? -pEqD/sum : 0),((idx & 1) == 1 ? -pEqD/sum : 0) );
		
		//Derive the 4 verts for this plane
		//find 4 verts on this plane
		setVert(planePt.x, planePt.y, planePt.z, 0);
		myPoint nextPt = new myPoint(planePt);
		nextPt._add(inPlaneU);
		setVert(nextPt.x, nextPt.y, nextPt.z, 1);
		nextPt._add(inPlaneV);
		setVert(nextPt.x, nextPt.y, nextPt.z, 2);
		nextPt.set(planePt);
		nextPt._add(inPlaneV);
		setVert(nextPt.x, nextPt.y, nextPt.z, 3);		
		//build arrays of points, vectors and this object's origin
		setPointsAndNormal(true);
	}//setPlaneVals	
	
	//plane is infinite, so min/max are meaningless.  min/max used for bboxes, and plane isect check is faster than bbox isect check anyway, so shouldn't ever put in bbox
	@Override
	public final myVector getMaxVec(){return new myVector(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE);}	
	@Override
	public final myVector getMinVec(){return  new myVector(-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE);}
	@Override
	/**
	 * infinite plane isect is always inside, so override default
	 */
	public final boolean checkInside(myPoint rayPoint, rayCast ray){	return true;	}//checkInside method
	//infinite plane txtr coords - tiled at bounds described by planar points. Should look pretty interesting if points do not form convex quad
	@Override
	public double[] findTxtrCoords(myPoint isCtPt, PImage myTexture, double time){		
		myVector v2 = normToUse.getVecPtToPassedPt(isCtPt, 0), p2p0 = normToUse.getP2P(0), p2pLast = normToUse.getPLastP0();
	    double 
	    // will tile infinite plane
	    uRaw = (v2._dot(p2p0) / p2p0.sqMagn), u = uRaw - MyMathUtils.floor(uRaw), 
	    vRaw = (v2._dot(p2pLast) / p2pLast.sqMagn), v = vRaw - MyMathUtils.floor(vRaw);
	    return new double[]{(1-u)*(myTexture.width-1),(1-v)*(myTexture.height-1)};
	}
	
}//myPlane