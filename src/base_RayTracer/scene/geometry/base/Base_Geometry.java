package base_RayTracer.scene.geometry.base;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.BoundingBox;
import base_RayTracer.scene.shaders.myObjShader;
import processing.core.PImage;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * abstract base class from which scene objects, instances, bounding boxes and acceleration structures all inherit.
 * Make this very skinny since we may have thousands of them
 * @author 7strb
 *
 */
public abstract class Base_Geometry {
	public Base_Scene scene;
	public final int ID;
	public objType type;    //what kind of object this is
	//first 2 vectors of object, center of object,the orientation vector of this object, the min and max values in xyz that this object spans, for bounding box
	public myPoint origin;
	public double[] transOrigin;		//used only to speed up initial calcs for bvh structure
	public myObjShader shdr;

	public myMatrix[] CTMara;
	public static final int 
			glblIDX = 0,
			invIDX = 1,
			transIDX = 2,
			invTransIDX = 3;

	/**
	 * everything has a bounding box
	 */
	protected BoundingBox _bbox;						
	
	public static final double epsVal = MyMathUtils.EPS;//.0000001;
	
	protected myPoint minVals, maxVals;
	
	public Base_Geometry(Base_Scene _scn, double _x, double _y, double _z) {
		scene = _scn;
	    ID = scene.objCnt++;
	    type = objType.None;
	    //build this object's transformation matrix - since this is the base/owning object, pass the identity for "prev obj matrix"
		minVals = new myPoint(100000,100000,100000);
		maxVals = new myPoint(-100000,-100000,-100000);
	    CTMara = buildCTMara(scene.gtPeekMatrix());	
	    origin = new myPoint(_x,_y,_z);
	    buildTransOrigin();
	}
	
	/**
	 * Build transformed origin array (instead of point, to speed up calculations)
	 */
	protected void buildTransOrigin() {
		transOrigin =  CTMara[glblIDX].transformPointIntoAra(origin);
	}
	
	/**
	 * Build array of matrices of precalced matrix operations
	 * inv mat idx : 1; transpose mat idx : 2; transpose of inverse : 3
	 * @param CTMara
	 * @return
	 */
	private myMatrix[] buildMatExt(myMatrix[] CTMara){CTMara[1] = CTMara[0].inverse();CTMara[2] = CTMara[0].transpose();CTMara[3] = CTMara[1].transpose();return CTMara;}
	/**
	 * Build this object's current transformation matrix precalced array of transformation matrices, using the passed matrix as the root transformation
	 * @param _mat1
	 * @return
	 */
	protected myMatrix[] buildCTMara(myMatrix _mat1){myMatrix[] CTMara = new myMatrix[4];CTMara[0] = _mat1; return buildMatExt(CTMara);}	
	/**
	 * build mat ara such that passed matrix _mat1 to be fwd transformed by _baseMat.  
	 * @param _mat1
	 * @param _baseMat
	 * @return
	 */
	protected myMatrix[] buildCTMara(myMatrix _mat1, myMatrix _baseMat){return buildCTMara(_baseMat.multMat(_mat1));}	
	
	
	protected void postProcBBox(){
		_bbox = new BoundingBox(scene, minVals, maxVals);
		_bbox.addObj(this);	
	}

	/**
	 * To implement motion blur - t is interpolant between two set values
	 * @param t
	 * @return
	 */
	public myPoint getOrigin(double t) {return origin;}
	/**
	 * CALLER MUST FWD TRANSFORM! - to support instances of objects; 
	 * @return point with maximum x/y/z coords of this object
	 */
	public abstract myPoint getMaxVec();	
	
	/**
	 * CALLER MUST FWD TRANSFORM! - to support instances of objects; 
	 * @return point with minimum x/y/z coords of this object
	 */
	public abstract myPoint getMinVec();
	/**
	 * intersection check for shadows 
	 * @param _ray
	 * @param _trans
	 * @param _ctAra
	 * @param distToLight
	 * @return
	 */
	public abstract int calcShadowHit(rayCast _ray,rayCast _trans, myMatrix[] _ctAra, double distToLight);
	/**
	 * find rayhit value for hitting this geometry object
	 * @param _ray
	 * @param transRay
	 * @param _ctAra
	 * @return
	 */
	public abstract rayHit intersectCheck(rayCast _ray,rayCast transRay, myMatrix[] _ctAra);
	/**
	 * find the appropriate normal for the hit on this object
	 * @param point
	 * @param args
	 * @return
	 */
	public abstract myVector getNormalAtPoint(myPoint point, int[] args);
	/**
	 * everything should be able to handle a get color query
	 * @param transRay
	 * @return
	 */
	public abstract myRTColor getColorAtPos(rayHit transRay);
	/**
	 * get texture coordinates
	 * @param isctPt
	 * @param myTexture
	 * @param time
	 * @return
	 */
	public abstract double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time);
	/**
	 * 
	 * @param isctPt
	 * @param v
	 * @param myTexture
	 * @param time
	 * @return
	 */
	protected abstract double findTextureU(myPoint isctPt, double v, PImage myTexture, double time);  
	/**
	 * 
	 * @param isctPt
	 * @param myTexture
	 * @param time
	 * @return
	 */
	protected abstract double findTextureV(myPoint isctPt, PImage myTexture, double time); 
	
	/**
	 * Accessor
	 * @return
	 */
	public BoundingBox getBBox() {return _bbox;}
	
	
	public String toString(){
		String result = "Object Type : " + type + " ID:"+ID+" origin : " + origin;
		result+="\nCTM :                         CTMInv :\n";
		String tmpString,tmp2str;
	    for (int row = 0; row < 4; ++row){
	    	result += "[";
	    	for (int col = 0; col < 4; ++col){   tmp2str = (CTMara[glblIDX].m[row][col] < 0 ? "" : " ") + String.format("%.2f", CTMara[glblIDX].m[row][col]); if (col != 3) {tmp2str += ", ";} result += tmp2str;}    	tmpString = "]";result += tmpString + "      [";
	    	for (int col = 0; col < 4; ++col){   tmp2str = (CTMara[invIDX].m[row][col] < 0 ? "" : " ")+String.format("%.2f", CTMara[invIDX].m[row][col]); if (col != 3) {tmp2str += ", ";} result += tmp2str;}    	tmpString = "]";  if (row != 3) { tmpString += "\n"; }
	    	result += tmpString;
	    }
		result+="\nCTMTrans :                         CTMAdj :\n";
		
	    for (int row = 0; row < 4; ++row){
	    	result += "[";
	    	for (int col = 0; col < 4; ++col){   tmp2str = (CTMara[transIDX].m[row][col] < 0 ? "" : " ") + String.format("%.2f", CTMara[transIDX].m[row][col]); if (col != 3) {tmp2str += ", ";} result += tmp2str;}    	tmpString = "]";result += tmpString + "      [";
	    	for (int col = 0; col < 4; ++col){   tmp2str = (CTMara[invTransIDX].m[row][col] < 0 ? "" : " ")+String.format("%.2f", CTMara[invTransIDX].m[row][col]); if (col != 3) {tmp2str += ", ";} result += tmp2str;}    	tmpString = "]";  if (row != 3) { tmpString += "\n"; }
	    	result += tmpString;
	    }
	    if(_bbox != null){		result += "\nBounding box : " + _bbox.toString();}
	    return result;
	}
	
}//myGeomBase


