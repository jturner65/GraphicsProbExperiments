package base_RayTracer.scene.geometry.sceneObjects.planar.base;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_RayTracer.ray.rayCast;

/**
 * Contains data used for planar object normals, vertices and txture coords. 
 * Moved to a separate class to more efficiently organize values. We will have 
 * standard normal and inverted normal based structures for each planar object.
 * @author 7strb
 *
 */
public class PlanarNormAndEq {
	//owning object
	protected Base_PlanarObject obj;
	//number of verticies in owning object
	protected final int vCount; 

	
	//verts x,y,z; texture coordinates u,v corresponding to each vertex of this poly
	private double[] vertX, vertY, vertZ,
						vertU, vertV;			
	//Normal, vector from last point to pt0
	private myVector N, PLastP0;	
	
	// verts of this object as points
	private myPoint[] P;
	// vectors between adjacent verts of planar objects
	private myVector[] P2P;
	
	//descriptive planar equation coefficients for this object based on points
	private double peqA, peqB, peqC, peqD;
	//used for texture calc - precalculating dot prod res
	private double baryIDenomTxtr;
	//projected of last edge vector on first edge vector (for triangles)
	private double dotValFinal;
	
	public PlanarNormAndEq(Base_PlanarObject _obj, int _vCount) {
		obj = _obj;
		vCount = _vCount;
		initObjVals();
	}
	/**
	 *  method to initialize values for planar objects
	 *  @param count of verticies
	 */
	public void initObjVals(){		
		vertX = new double[vCount];
		vertY = new double[vCount];
		vertZ = new double[vCount];
		vertU = new double[vCount];
		vertV = new double[vCount];
		P = new myPoint[vCount];
		P2P = new myVector[vCount];		
		PLastP0 = new myVector(0,0,0);//used for textures
		N = new myVector(0,0,1);
		for (int i = 0; i < vCount; i ++){
			P[i] = new myVector(0,0,0);
			P2P[i] = new myVector(0,0,0);
		}  
	}//initObjVals method

	/**
	 * Take passed normal object and set this object to its inverse
	 * @param norm
	 */
	public void buildThisAsInvertedNormal(PlanarNormAndEq norm){
		double[] tmpX = new double[vCount],
		tmpY = new double[vCount],
		tmpZ = new double[vCount],
		tmpU = new double[vCount],
		tmpV = new double[vCount];
		int idx = 0;
		for(int i = vCount-1; i >= 0; --i){
			tmpX[i] = norm.vertX[idx];
			tmpY[i] = norm.vertY[idx];
			tmpZ[i] = norm.vertZ[idx];
			tmpU[i] = norm.vertU[idx];
			tmpV[i] = norm.vertV[idx];
			++idx;
		}
		vertX = tmpX; vertY = tmpY; vertZ = tmpZ; vertU = tmpU; vertV = tmpV;
		setPointsAndNormal(false);
	}//buildInvertedNormal
	
	
	protected void setPointsAndNormal(boolean updateOrigin){
		//set all vectors to correspond to entered points
		int lastPtIDX = vCount-1;
		for(int i = 0; i < vCount; ++i){
			P[i].set(vertX[i],vertY[i],vertZ[i]);
			int idx = (i != 0 ? i-1 : lastPtIDX);
			P2P[idx].set(vertX[i]-vertX[idx],vertY[i]-vertY[idx],vertZ[i]-vertZ[idx]); 
		}		
		//P2P0.set(vertX[2]-vertX[0],vertY[2]-vertY[0],vertZ[2]-vertZ[0]);//for textures, precalc
		PLastP0.set(P2P[lastPtIDX]);//for textures, precalc
		PLastP0._mult(-1.0);
    	//find normals for each vertex
		dotValFinal = P2P[0]._dot(PLastP0);
		baryIDenomTxtr =  1.0 / ((P2P[0].sqMagn * P2P[lastPtIDX].sqMagn) - (dotValFinal * dotValFinal));
		//only update origin if necessary
		if(updateOrigin) {
			double tempX = 0, tempY = 0, tempZ = 0;
			for(int i = 0; i < vCount; ++i){
				tempX += vertX[i];
				tempY += vertY[i];
				tempZ += vertZ[i];
			}
			//set center x,y,z to be centroid of planar object
	    	obj.updateOriginFromNormCalcs(tempX/vCount,tempY/vCount,tempZ/vCount);
		}
		
    	N = P2P[1]._cross(P2P[0]);
    	N._normalize();
    	//set equation values
    	setEQVals();
	}//setPointsAndNormal
	
	/**
	 * Return whether the passed intersection point is inside the bounds of this poly
	 * @param rayPoint
	 * @param ray
	 * @return
	 */
	public boolean checkInsideVerts(myPoint rayPoint, rayCast ray){
		//find ray from each vertex to the planar intersection point
		myVector intRay = new myVector(0,0,0);
		for(int i =0; i<vCount; ++i){
			int pIdx = (i==0 ? vCount-1 : i-1);
			intRay.set(rayPoint.x - vertX[i],rayPoint.y - vertY[i], rayPoint.z - vertZ[i]);
			myVector tmp = intRay._cross(P2P[pIdx]);
			if(dotWithNorm(tmp) < -MyMathUtils.EPS){return false;}		//means opposite to normal direction
		}
		return true;
	}//checkInside method
	
	/**
	 * Dot vector or point against this object's norm
	 * @param v
	 * @return
	 */
	public double dotWithNorm(myPoint v) {return ((N.x * v.x) + (N.y * v.y) + (N.z * v.z));}
	
	/**
	 * Find t value for intersection of passed point
	 * @param pt known intersection point
	 * @param planeRes scaling value from previous projection of ray vector against normal
	 * @return
	 */
	public double calcTVal(myPoint pt, double planeRes) {
		return -(((N.x * pt.x) + (N.y * pt.y) + (N.z * pt.z)) + peqD)/planeRes;
	}
	
	public myVector getVecPtToPassedPt(myPoint isCtPt, int idx) {
		return new myVector(P[idx], isCtPt);
	}
	
	public myPoint getPoint(int idx) {return P[idx];}
	
	public myVector getP2P(int idx) {return P2P[idx];}
	
	public myVector getPLastP0() {return PLastP0;}
	
	public double getBaryIDenomTxtr() {return baryIDenomTxtr;}
	
	public double getDotValFinal() {return dotValFinal;}
	
	public double getVertU(int idx) {return vertU[idx];}
	public double getVertV(int idx) {return vertV[idx];}
	
	public double[] getMinAndMaxVertX() {return MyMathUtils.minAndMax(vertX);}
	public double[] getMinAndMaxVertY() {return MyMathUtils.minAndMax(vertY);}
	public double[] getMinAndMaxVertZ() {return MyMathUtils.minAndMax(vertZ);}
	
	/**
	 * @return the base normal.
	 */
	public myVector getNorm() {
		return new myVector(N);
	}
	
	/**
	 * Planar equation governing
	 */
	protected void setEQVals(){peqA = N.x;peqB = N.y;peqC = N.z;peqD = -((peqA * vertX[0]) + (peqB * vertY[0]) + (peqC * vertZ[0]));}
	
	//sets the vertex values for a particular vertex, given by idx
	public final void setVert(double _x, double _y, double _z, int idx){vertX[idx] = _x;  		vertY[idx] = _y;  		vertZ[idx] = _z;}
	public final void setTxtrCoord(double _u, double _v, int idx){vertU[idx]=_u;			vertV[idx]=_v;}
	
	
	@Override
	public String toString(){
		String res = "-Specific : Normal :  " + N + " Planar eq : " + peqA+"x + "+peqB+"y  + " + peqC+"z + "+peqD + " = 0\n";  
		res += "Vertices :\n";
		for(int i =0; i<P.length;++i){	res+="i : " + P[i]+"\n";}
		return res;
	}
	
}//class PlanarNormAndEq
