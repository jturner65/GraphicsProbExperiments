package base_RayTracer;

import java.util.*;

import processing.core.PImage;
import base_Utils_Objects.*;

public abstract class myPlanarObject extends mySceneObject{
	//objects used for both square and triangle
	protected double[] vertX, vertY, vertZ,
						vertU, vertV;			//texture coordinates corresponding to each vertex of this poly
	protected myVector N, P2P0;		//Normal, vector from pt2 to pt0
	protected myVector[] P, P2P;//points of the planar polygon, vectors between each point
	protected int vCount; //object id, number of verticies in this object - used for square and triangle  
	protected double peqA, peqB, peqC, peqD, 
				baryIDenomTxtr;		//used for texture calc - precalculating dot prod res
	protected double[] dotVals;
//	//list of adjacent faces to this face
//	protected ArrayList<myPlanarObject> adjFaces;  
	public myPlanarObject(myScene _p){   
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
		P = new myVector[vCount];
		P2P = new myVector[vCount];		
		P2P0 = new myVector(0,0,0);//used for textures
		N = new myVector(0,0,1);
		for (int i = 0; i < vCount; i ++){
			P[i] = new myVector(0,0,0);
			P2P[i] = new myVector(0,0,0);
		}  
	}//initObjVals method
	
	protected void setPointsAndNormal(){
		double tempX = 0, tempY = 0, tempZ = 0;
		//set all vectors to correspond to entered points
		for(int i = 0; i < this.vCount; i++){
			tempX += vertX[i];
			tempY += vertY[i];
			tempZ += vertZ[i];
			P[i].set(vertX[i],vertY[i],vertZ[i]);
			int idx = (i != 0 ? i-1 : vCount-1);
			P2P[idx].set(vertX[i]-vertX[idx],vertY[i]-vertY[idx],vertZ[i]-vertZ[idx]); 
			dotVals[idx] = P2P[idx]._dot(P2P[idx]);//precalc for txtrs
		}		
		//P2P0.set(vertX[2]-vertX[0],vertY[2]-vertY[0],vertZ[2]-vertZ[0]);//for textures, precalc
		P2P0.set(P2P[2]);//for textures, precalc
		P2P0._mult(-1.0);
    	//find normals for each vertex
		//dotVals[vCount] = P2P[0]._dot(P2P0);
		dotVals[vCount] = -P2P[0]._dot(P2P[2]);
		baryIDenomTxtr =  1.0 / ((dotVals[0] * dotVals[2]) - (dotVals[vCount] * dotVals[vCount]));
		
    	N = P2P[1]._cross(P2P[0]);
    	N._normalize();
		//set center x,y,z to be centroid of planar object
    	origin.set(tempX/vCount,tempY/vCount,tempZ/vCount);
    	trans_origin = getTransformedPt(origin, CTMara[glblIDX]).asArray();
	}
	
	protected void invertNormal(){
		double[] tmpX = new double[this.vCount],
		tmpY = new double[this.vCount],
		tmpZ = new double[this.vCount],
		tmpU = new double[this.vCount],
		tmpV = new double[this.vCount];
		
		for(int i = 0; i < this.vCount; i++){
			tmpX[this.vCount-1-i] = vertX[i];
			tmpY[this.vCount-1-i] = vertY[i];
			tmpZ[this.vCount-1-i] = vertZ[i];
			tmpU[this.vCount-1-i] = vertU[i];
			tmpV[this.vCount-1-i] = vertV[i];
		}
		vertX = tmpX; vertY = tmpY; vertZ = tmpZ; vertU = tmpU; vertV = tmpV;
		setPointsAndNormal();
		setEQ();
	}
	
	protected void setEQ(){peqA = N.x;peqB = N.y;peqC = N.z;peqD = -((peqA * vertX[0]) + (peqB * vertY[0]) + (peqC * vertZ[0]));}//equals D in equation given in class notes		
	
	//finalize loading of polygon info from cli file
	public void finalizePoly(){		
		setPointsAndNormal();
		setEQ();
		minVals.set(min(vertX),min(vertY),min(vertZ));
		maxVals.set(max(vertX),max(vertY),max(vertZ));
		_bbox.calcMinMaxCtrVals(minVals, maxVals);
		_bbox.addObj(this);
	}//setVects method

	@Override
	//check if passed ray intersects with this planar object - ray already transformed	
	public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){
//		if(!_bbox.intersectCheck(ray, _ctAra).isHit){return new rayHit(false);	}
//		myRay transRay = ray.getTransformedRay(ray, _ctAra[invIDX]);
		//get the result of plugging in this ray's direction term with the plane in question - if 0 then this ray is parallel with the plane
		double planeRes = N._dot(transRay.direction);			
		if ( fastAbs(planeRes) > 0){//intersection with poly plane present - need to check if inside polygon	
			if(planeRes > 0){invertNormal(); return intersectCheck(_ray,transRay, _ctAra);}			//blorch @ norm recalc.  could speed this up if i found all eye intersections first and then all shadow intersections
			double t = -(N._dot(transRay.origin) + peqD)/planeRes;	
			if ((t > epsVal) && (checkInside(transRay.pointOnRay(t), transRay))) {return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(t),null,t);}
		} 
		return new rayHit(false);
	}//intersectCheck planar object	

	//sets the vertex values for a particular vertex, given by idx
	public void setVert(double _x, double _y, double _z, int idx){vertX[idx] = _x;  		vertY[idx] = _y;  		vertZ[idx] = _z;}
	public void setTxtrCoord(double _u, double _v, int idx){vertU[idx]=_u;			vertV[idx]=_v;}
	// determine if a ray that intersects the plane containing this polygon does so within the bounds of this polygon.
	public abstract boolean checkInside(myVector rayPoint, myRay ray);
	
	/**
	*  determine this object's normal - with individual objects, the normal will just
	*  be the normalized cross product of two coplanar vectors.  with meshes, need to
	*  calculate the normals at every vertex based on the polys that share that vertex 
	*  and then find barycentric average based on hit loc
	*/  
	@Override
	public myVector getNormalAtPoint(myVector point, int[] args) {
		//polygon is flat, normals will all be the same
		myVector res = N;
		res._normalize();
		if (rFlags[invertedIDX]){invertNormal(); res = new myVector(N);res._normalize();}
		return res;
	}
	
	@Override //TODO modify these if we want to support moving polys
	public myVector getOrigin(double _t){	return origin;	}
	
	@Override
	public myVector getMaxVec(){return new myVector(max(vertX),max(vertY),max(vertZ));}//set in finalize poly, this is tmp
	@Override
	public myVector getMinVec(){return new myVector(min(vertX),min(vertY),min(vertZ));}//set in finalize poly, this is tmp	
	//finds u,v in array, based on loaded U,V coordinates for each vertex, and interpolation of U,V of intersection point

	@Override
	public String toString(){
		String res = super.toString()+ type +"-Specific : Normal :  " + this.N + " Planar eq : " + peqA+"x + "+peqB+"y  + " + peqC+"z + "+peqD + " = 0\n";  
		res += "Vertices :\n";
		for(int i =0; i<P.length;++i){	res+="i : " + P[i]+"\n";}
		return res;
	}
}//class myPlanarObject

class myTriangle extends myPlanarObject{
  
	public myTriangle(myScene _scn){
		super(_scn);
		vCount = 3;
		type = objType.Triangle;
		initObjVals();    
	}	//myTriangle constructor (4)

	public boolean checkInside(myVector rayPoint, myRay ray){
		//find ray from each vertex to the planar intersection point
		myVector intRay = new myVector(0,0,0);
		for(int i =0; i<vCount; ++i){
			int pIdx = (i==0 ? vCount-1 : i-1);
			intRay.set(rayPoint.x - vertX[i],rayPoint.y - vertY[i], rayPoint.z - vertZ[i]);
			myVector tmp = intRay._cross(P2P[pIdx]);
			if(tmp._dot(N) < -epsVal){return false;}		//means opposite to normal direction
		}
		return true;
	}//checkInside method

	@Override
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){
	    myVector v2 = new myVector(P[0],isctPt);
	    double dot20 = v2._dot(P2P[0]), dot21 = v2._dot(P2P0),
	    c_u = ((dotVals[2] * dot20) - (dotVals[vCount] * dot21)) * baryIDenomTxtr,
	    c_v = ((dotVals[0] * dot21) - (dotVals[vCount] * dot20)) * baryIDenomTxtr,
	    c_w = 1 - c_u - c_v;
	    double u = vertU[0] * c_w + vertU[1] * c_u + vertU[2]*c_v, v = vertV[0] * c_w + vertV[1] * c_u + vertV[2]*c_v;
	    return new double[]{u*(myTexture.width-1),(1-v)*(myTexture.height-1)};
	}
	
}//class myTriangle

class myQuad extends myPlanarObject{
  
	public myQuad(myScene _scn){
		super(_scn);
		this.vCount = 4;
		type = objType.Quad;
		initObjVals();
	}//myQuad constructor (4)
	
	public boolean checkInside(myVector rayPoint, myRay ray){
		//find ray from each vertex to the planar intersection point
		myVector intRay = new myVector(0,0,0);
		for(int i =0; i<vCount; ++i){
			int pIdx = (i==0 ? vCount-1 : i-1);
			intRay.set(rayPoint.x - vertX[i],rayPoint.y - vertY[i], rayPoint.z - vertZ[i]);
			myVector tmp = intRay._cross(P2P[pIdx]);
			//tmp._normalize();
			if(tmp._dot(N)< -epsVal){return false;}			
		}
		return true;
	}//checkInside method
	
	@Override
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){
	    myVector v2 = new myVector(P[0],isctPt);
	    double dot20 = v2._dot(P2P[0]), dot21 = v2._dot(P2P0),
	    c_u = ((dotVals[2] * dot20) - (dotVals[vCount] * dot21)) * baryIDenomTxtr,
	    c_v = ((dotVals[0] * dot21) - (dotVals[vCount] * dot20)) * baryIDenomTxtr,
	    c_w = 1 - c_u - c_v;
	    double u = vertU[0] * c_w + vertU[1] * c_u + vertU[2]*c_v, v = vertV[0] * c_w + vertV[1] * c_u + vertV[2]*c_v;
	    return new double[]{u*(myTexture.width-1),(1-v)*(myTexture.height-1)};
	}
	
}//class myQuad

//infinite plane object
class myPlane extends myPlanarObject{

	public myPlane(myScene _scn){
		super(_scn);
		vCount = 4;
		type = objType.Plane;
		initObjVals();
	}
	
	public void setPlaneVals(double _a, double _b, double _c, double _d){
		N.set( _a, _b, _c);
		double mag = N._mag();
		N._normalize();
		peqA = N.x;
		peqB = N.y;
		peqC = N.z;
		peqD = _d/mag;		
		myVector rotVec = new myVector(peqB, peqC, peqA);//vector to cross N with - so 
		if((peqA == peqB) && (peqA == peqC)){	rotVec._add(1,0,0);		}//if all equal then shifting above them gives no benefit
		rotVec._normalize();
		//basis vex in plane :
		int idx = 7;//x,y,z
		double sum = peqA + peqB + peqC;					//potential point coords
		if(sum == 0){sum = peqA + peqB;idx = 6; 			//x,y,!z
			if(sum == 0){sum = peqA + peqC;idx = 5;			//x,!y,z
				if(sum == 0){sum = peqB + peqC;idx = 3;}}}	//!x,y,z		
		myVector planePt = new myVector(((idx & 4) == 4 ? -peqD/sum : 0),((idx & 2) == 2 ? -peqD/sum : 0),((idx & 1) == 1 ? -peqD/sum : 0) );
		myVector inPlaneU = N._cross(rotVec),//any vector perp to N will be in plane
		inPlaneV = N._cross(inPlaneU);		//in other direction 
		//find 4 verts on this plane
		vertX[0]=planePt.x;vertY[0]=planePt.y;vertZ[0]=planePt.z;
		myVector nextPt = new myVector(planePt);
		nextPt._add(inPlaneU);
		vertX[1]=nextPt.x;vertY[1]=nextPt.y;vertZ[1]=nextPt.z;
		nextPt._add(inPlaneV);
		vertX[2]=nextPt.x;vertY[2]=nextPt.y;vertZ[2]=nextPt.z;
		nextPt.set(planePt);
		nextPt._add(inPlaneV);
		vertX[3]=nextPt.x;vertY[3]=nextPt.y;vertZ[3]=nextPt.z;
		
		//set origin TODO no origin, find a point on the plane
		origin.set(vertX[0],vertY[0],vertZ[0]);
		//setCurrColors();		
	}
	//plane is infinite, so min/max are meaningless.  min/max used for bboxes, and plane isect check is faster than bbox isect check anyway, so shouldn't ever put in bbox
	@Override
	public myVector getMaxVec(){return new myVector(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE);}	
	@Override
	public myVector getMinVec(){return  new myVector(-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE);}
	@Override//infinite plane isect is always inside
	public boolean checkInside(myVector rayPoint, myRay ray){	return true;	}//checkInside method
	@Override
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){		
	    myVector v2 = new myVector(P[0],isctPt);
	    double dot20 = v2._dot(P2P[0]), dot21 = v2._dot(P2P0),
	    c_u = ((dotVals[2] * dot20) - (dotVals[vCount] * dot21)) * baryIDenomTxtr,
	    c_v = ((dotVals[0] * dot21) - (dotVals[vCount] * dot20)) * baryIDenomTxtr,
	    c_w = 1 - c_u - c_v;
	    double u = vertU[0] * c_w + vertU[1] * c_u + vertU[2]*c_v, v = vertV[0] * c_w + vertV[1] * c_u + vertV[2]*c_v;
	    return new double[]{u*(myTexture.width-1),(1-v)*(myTexture.height-1)};
	}
	
}//myPlane

//class to hold a vertex - will be shared by planar objects, will own it's own normal
class myVertex implements Comparable<myVertex> {
	public myScene scn;
	public myVector V, N;
	private ArrayList<myPlanarObject> owners;
	
	public myVertex(myScene _scn, myVector _v){
		scn = _scn;
		V = new myVector(_v);
		owners = new ArrayList<myPlanarObject>();		
	}
	
	public void addOwner(myPlanarObject obj){owners.add(obj);}
	//calc vertex normal of this vertex by finding the verts of each adjacent planar object and adding them
	public myVector calcNorm(){
		myVector res = new myVector(0,0,0);
		for(myPlanarObject obj : owners){res._add(obj.N);}
		res._normalize();		
		return res;		
	}

	@Override
	public int compareTo(myVertex arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
		
	
	
}//myVertex

