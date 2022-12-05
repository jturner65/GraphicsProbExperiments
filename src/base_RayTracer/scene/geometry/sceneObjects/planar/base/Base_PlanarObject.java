package base_RayTracer.scene.geometry.sceneObjects.planar.base;

import java.util.*;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.sceneObjects.base.Base_SceneObject;
import processing.core.PImage;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

public abstract class Base_PlanarObject extends Base_SceneObject{
	//objects used for both square and triangle
	protected double[] vertX, vertY, vertZ,
						vertU, vertV;			//texture coordinates corresponding to each vertex of this poly
	protected myVector N, PLastP0;		//Normal, vector from last point to pt0
	protected myPoint[] P;
	protected myVector[] P2P;//points of the planar polygon, vectors between each point
	protected int vCount; //object id, number of verticies in this object - used for square and triangle  
	protected double peqA, peqB, peqC, peqD, 
				baryIDenomTxtr;		//used for texture calc - precalculating dot prod res
	protected double[] dotVals;
//	//list of adjacent faces to this face
//	protected ArrayList<myPlanarObject> adjFaces;  
	public Base_PlanarObject(Base_Scene _p){   
		super(_p,0,0,0); 		
		postProcBBox();				//cnstrct and define bbox - rebuilt when finalizePoly()
	}//constructor (4) 
	/**
	 *  method to initialize values for triangle, square and other planar objects
	 *  @param count of verticies
	 */
	public void initObjVals(){		
		vertX = new double[vCount];
		vertY = new double[vCount];
		vertZ = new double[vCount];
		vertU = new double[vCount];
		vertV = new double[vCount];
		dotVals = new double[vCount + 1];
		P = new myPoint[vCount];
		P2P = new myVector[vCount];		
		PLastP0 = new myVector(0,0,0);//used for textures
		N = new myVector(0,0,1);
		for (int i = 0; i < vCount; i ++){
			P[i] = new myVector(0,0,0);
			P2P[i] = new myVector(0,0,0);
		}  
	}//initObjVals method
	
	protected void setPointsAndNormal(){
		double tempX = 0, tempY = 0, tempZ = 0;
		//set all vectors to correspond to entered points
		int lastPtIDX = vCount-1;
		for(int i = 0; i < vCount; ++i){
			tempX += vertX[i];
			tempY += vertY[i];
			tempZ += vertZ[i];
			P[i].set(vertX[i],vertY[i],vertZ[i]);
			int idx = (i != 0 ? i-1 : lastPtIDX);
			P2P[idx].set(vertX[i]-vertX[idx],vertY[i]-vertY[idx],vertZ[i]-vertZ[idx]); 
			dotVals[idx] = P2P[idx].sqMagn;// P2P[idx]._dot(P2P[idx]);//precalc for txtrs
		}		
		//P2P0.set(vertX[2]-vertX[0],vertY[2]-vertY[0],vertZ[2]-vertZ[0]);//for textures, precalc
		PLastP0.set(P2P[lastPtIDX]);//for textures, precalc
		PLastP0._mult(-1.0);
    	//find normals for each vertex
		//dotVals[vCount] = P2P[0]._dot(P2P0);
		dotVals[vCount] = -P2P[0]._dot(P2P[lastPtIDX]);
		baryIDenomTxtr =  1.0 / ((dotVals[0] * dotVals[lastPtIDX]) - (dotVals[vCount] * dotVals[vCount]));
		
    	N = P2P[1]._cross(P2P[0]);
    	N._normalize();
		//set center x,y,z to be centroid of planar object
    	origin.set(tempX/vCount,tempY/vCount,tempZ/vCount);
    	buildTransOrigin();
    	//set equation values
    	setEQ();
	}
	
	protected void invertNormal(){
		double[] tmpX = new double[vCount],
		tmpY = new double[vCount],
		tmpZ = new double[vCount],
		tmpU = new double[vCount],
		tmpV = new double[vCount];
		int idx = 0;
		for(int i = vCount-1; i >= 0; --i){
			tmpX[i] = vertX[idx];
			tmpY[i] = vertY[idx];
			tmpZ[i] = vertZ[idx];
			tmpU[i] = vertU[idx];
			tmpV[i] = vertV[idx];
			++idx;
		}
		vertX = tmpX; vertY = tmpY; vertZ = tmpZ; vertU = tmpU; vertV = tmpV;
		setPointsAndNormal();
	}
	
	protected void setEQ(){peqA = N.x;peqB = N.y;peqC = N.z;peqD = -((peqA * vertX[0]) + (peqB * vertY[0]) + (peqC * vertZ[0]));}//equals D in equation given in class notes		
	
	/**
	 * finalize loading of polygon info from cli file
	 */
	public final void finalizePoly(){		
		setPointsAndNormal();
		double[] minAndMaxX = MyMathUtils.minAndMax(vertX);
		double[] minAndMaxY = MyMathUtils.minAndMax(vertY);
		double[] minAndMaxZ = MyMathUtils.minAndMax(vertZ);		
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
		double planeRes = N._dot(transRay.direction);
		if(planeRes == 0) {
			return new rayHit(false);		
		}
		//intersection with poly plane present - need to check if inside polygon	
		//blorch @ norm recalc.  could speed this up if i found all eye intersections first and then all shadow intersections
		if(planeRes > 0){invertNormal(); return intersectCheck(_ray,transRay, _ctAra);}	
		//Dotting with origin and dividing by planeRes is actually calculating the origin's value 
		//within the plane's equation - this is telling what side of the plane the origin is on.
		//If behind the origin, we're not going to see this plane so return a false hit
		double t = -(N._dot(new myVector(transRay.origin)) + peqD)/planeRes;	
		if ((t > epsVal) && (checkInside(transRay.pointOnRay(t), transRay))) {return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(t),null,t);}
		return new rayHit(false);	
	}//intersectCheck planar object	

	//sets the vertex values for a particular vertex, given by idx
	public final void setVert(double _x, double _y, double _z, int idx){vertX[idx] = _x;  		vertY[idx] = _y;  		vertZ[idx] = _z;}
	public final void setTxtrCoord(double _u, double _v, int idx){vertU[idx]=_u;			vertV[idx]=_v;}
	/**
	 * determine if a ray that intersects the plane containing this polygon does so within the bounds of this polygon.
	 * @param rayPoint
	 * @param ray
	 * @return
	 */
	public abstract boolean checkInside(myPoint rayPoint, rayCast ray);
	
	/**
	*  determine this object's normal - with individual objects, the normal will just
	*  be the normalized cross product of two coplanar vectors.  with meshes, need to
	*  calculate the normals at every vertex based on the polys that share that vertex 
	*  and then find barycentric average based on hit loc
	*/  
	@Override
	public myVector getNormalAtPoint(myPoint point, int[] args) {
		//polygon is flat, normals will all be the same
		myVector res = N;
		res._normalize();
		if (isInverted()){invertNormal(); res = new myVector(N);res._normalize();}
		return res;
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
		String res = super.toString()+ type +"-Specific : Normal :  " + this.N + " Planar eq : " + peqA+"x + "+peqB+"y  + " + peqC+"z + "+peqD + " = 0\n";  
		res += "Vertices :\n";
		for(int i =0; i<P.length;++i){	res+="i : " + P[i]+"\n";}
		return res;
	}
}//class myPlanarObject


//class to hold a vertex - will be shared by planar objects, will own it's own normal
class myVertex implements Comparable<myVertex> {
	public Base_Scene scn;
	public myVector V, N;
	private ArrayList<Base_PlanarObject> owners;
	
	public myVertex(Base_Scene _scn, myVector _v){
		scn = _scn;
		V = new myVector(_v);
		owners = new ArrayList<Base_PlanarObject>();		
	}
	
	public void addOwner(Base_PlanarObject obj){owners.add(obj);}
	//calc vertex normal of this vertex by finding the verts of each adjacent planar object and adding them
	public myVector calcNorm(){
		myVector res = new myVector(0,0,0);
		for(Base_PlanarObject obj : owners){res._add(obj.N);}
		res._normalize();		
		return res;		
	}

	@Override
	public int compareTo(myVertex arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
		
	
	
}//myVertex

