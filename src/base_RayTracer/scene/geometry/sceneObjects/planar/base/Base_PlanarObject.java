package base_RayTracer.scene.geometry.sceneObjects.planar.base;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.base.Base_SceneObject;
import processing.core.PImage;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

public abstract class Base_PlanarObject extends Base_SceneObject{
	//Normal structure and inverted normal structure, precalculated
	private PlanarNormAndEq[] normalStructs;
	//index of normal struct to use, either outward or inverted precalculated normal
	private int normToUseIDX;	
	//normal struct to use
	protected PlanarNormAndEq normToUse;
	//number of verts in this object - used for square and triangle  
	protected final int vCount; 
		
	public Base_PlanarObject(Base_Scene _scn, int _vCount, GeomObjType _type){   
		super(_scn,0,0,0, _type); 	
		vCount = _vCount;
		postProcBBox();				//cnstrct and define bbox - rebuilt when finalizePoly()
		normalStructs = new PlanarNormAndEq[2];
		for(int i=0;i<normalStructs.length;++i) {
			normalStructs[i] = new PlanarNormAndEq(this, vCount);
		}
		setNormToUse(0);
	}//constructor (4) 
	
	protected void setPointsAndNormal(boolean updateOrigin){
		//set all points and normals in normal struct, for both direction of normals
		normalStructs[0].setPointsAndNormal(true);
		normalStructs[1].buildThisAsInvertedNormal(normalStructs[0]);
		setNormToUse(0);
	}//setPointsAndNormal

	/**
	 * Update the origin for this planar object based on normal calcs/recalcs
	 * @param x
	 * @param y
	 * @param z
	 */
	public void updateOriginFromNormCalcs(double x, double y, double z) {
		origin.set(x, y, z);
    	buildTransOrigin();
	}	
	
	protected void invertNormal(){
		//swap which normal struct we are using
		setNormToUse(normToUseIDX+1);
	}
	
	/**
	 * finalize loading of polygon info from cli file
	 */
	public final void finalizePoly(){	
		setPointsAndNormal(true);	
		double[] minAndMaxX = normalStructs[0].getMinAndMaxVertX();
		double[] minAndMaxY = normalStructs[0].getMinAndMaxVertY();
		double[] minAndMaxZ = normalStructs[0].getMinAndMaxVertZ();		
		minVals.set(minAndMaxX[0], minAndMaxY[0], minAndMaxZ[0]);
		maxVals.set(minAndMaxX[1], minAndMaxY[1], minAndMaxZ[1]);
		_bbox.calcMinMaxCtrVals(minVals, maxVals);
		_bbox.addObj(this);
	}//setVects method
	
	/**
	 * check if passed ray intersects with this planar object - ray already transformed	
	 */
	@Override
	public final rayHit intersectCheck(rayCast _ray, rayCast transRay, myMatrix[] _ctAra){
		//no bbox check for planar object - actual plane check is faster than bbox
		//if(!_bbox.intersectCheck(ray, _ctAra).isHit){return new rayHit(false);	}
		//get the result of plugging in this ray's direction term with the plane in question - if 0 then this ray is parallel with the plane
		double planeRes = normToUse.dotWithNorm(transRay.direction);
		if(planeRes == 0) {
			return new rayHit(false);		
		}
		//intersection with poly plane present - need to check if inside polygon
		//if greater than 0 then invert the normal.
		if(planeRes > 0){invertNormal(); planeRes *= -1;}	
		//Dotting with origin and dividing by planeRes is actually calculating the origin's value 
		//within the plane's equation - this is telling what side of the plane the origin is on.
		//If behind the origin, we're not going to see this plane so return a false hit
		double t = normToUse.calcTVal(transRay.origin,planeRes);	
		if ((t > epsVal) && (checkInside(transRay.pointOnRay(t), transRay))) {return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(t),null,t);}
		return new rayHit(false);	
	}//intersectCheck planar object	

	//sets the vertex values for a particular vertex, given by idx
	public final void setVert(double _x, double _y, double _z, int idx){
		for(int i=0;i<normalStructs.length;++i) {
			normalStructs[i].setVert(_x, _y, _z, idx);
		}		
	}
	public final void setTxtrCoord(double _u, double _v, int idx){
		for(int i=0;i<normalStructs.length;++i) {
			normalStructs[i].setTxtrCoord(_u, _v, idx);
		}
	}
	/**
	 * determine if a ray that intersects the plane containing this polygon does so within the bounds of this polygon.
	 * @param rayPoint
	 * @param ray
	 * @return
	 */
	protected boolean checkInside(myPoint rayPoint, rayCast ray){
		return normToUse.checkInsideVerts(rayPoint, ray);
	}//checkInside method
		
	/**
	*  determine this object's normal - with individual objects, the normal will just
	*  be the normalized cross product of two coplanar vectors.  with meshes, need to
	*  calculate the normals at every vertex based on the polys that share that vertex 
	*  and then find barycentric average based on hit loc
	*/  
	@Override
	public myVector getNormalAtPoint(myPoint point, int[] args) {
		//polygon is flat, normals will all be the same
		if (isInverted()){invertNormal();} 
		myVector res = normToUse.getNorm();
		res._normalize();
		return res;
	}//getNormalAtPoint
	
	/**
	 * @return the face normal for this planar object, weighted by its area. THIS SHOULD ONLY BE USED BY BaseVertex class
	 */
	public myVector getWeightedNorm() {
		return normToUse.getNorm();
	}

	private void setNormToUse(int idx) {
		normToUseIDX = (idx % normalStructs.length);
		normToUse = normalStructs[normToUseIDX];		
	}
	
	@Override
	public myPoint getMaxVec(){return maxVals;}//set in finalize poly, this is tmp
	@Override
	public myPoint getMinVec(){return minVals;}//set in finalize poly, this is tmp	
	//finds u,v in array, based on loaded U,V coordinates for each vertex, and interpolation of U,V of intersection point
	
	//Not currently used for planar objects
	@Override
	protected double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){ return 0.0; }
	@Override
	protected double findTextureV(myPoint isctPt, PImage myTexture, double time){	return 0.0;  } 
	@Override
	public String toString(){
		return super.toString()+ type + normalStructs[0].toString();
	}
}//class myPlanarObject

