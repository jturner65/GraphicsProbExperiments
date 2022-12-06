package base_RayTracer.scene.geometry.sceneObjects.base;
import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.shaders.myObjShader;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myVector;

public abstract class Base_SceneObject extends Base_Geometry{	
	
	public int[] rFlags;					//various state-related flags for this object
	public static final int 
			invertedIDX			= 0,				//normals point in or out
			isLightIDX			= 1//,
			//isTemplateObjIDX	= 2
			;				//this object is used to instance multiple objects (doesn't exist in scene itself)
	public static final int numFlags = 2;	

	public Base_SceneObject(Base_Scene _scn, double _x, double _y, double _z, GeomObjType _type){
		super(_scn, _x, _y, _z, _type);	    
	    initFlags();
	    shdr = new myObjShader(scene);//sets currently defined colors for this object also    
	}//constructor 6 var
	
	
	/**
	 * base class flags init
	 */
	public final void initFlags(){rFlags = new int[1 + numFlags/32];for(int i =0; i<numFlags;++i){setFlags(i,false);}}			
	/**
	 * get baseclass flag
	 * @param idx
	 * @return
	 */
	public final boolean getFlags(int idx){int bitLoc = 1<<(idx%32);return (rFlags[idx/32] & bitLoc) == bitLoc;}	
	
	/**
	 * check list of flags
	 * @param idxs
	 * @return
	 */
	public final boolean getAllFlags(int [] idxs){int bitLoc; for(int idx =0;idx<idxs.length;++idx){bitLoc = 1<<(idx%32);if ((rFlags[idx/32] & bitLoc) != bitLoc){return false;}} return true;}
	public final boolean getAnyFlags(int [] idxs){int bitLoc; for(int idx =0;idx<idxs.length;++idx){bitLoc = 1<<(idx%32);if ((rFlags[idx/32] & bitLoc) == bitLoc){return true;}} return false;}
		
	public final boolean isInverted() {return getFlags(invertedIDX);}
	public final boolean isLight() {return getFlags(isLightIDX);}
	
	/**
	 * set baseclass flags  //setFlags(showIDX, 
	 * @param idx
	 * @param val
	 */
	public final void setFlags(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		rFlags[flIDX] = (val ?  rFlags[flIDX] | mask : rFlags[flIDX] & ~mask);
		switch(idx){
		case invertedIDX		:{break;}		
		case isLightIDX			:{break;}
		}				
	}//setFlags

	//determine if shadow ray is a hit or not
	@Override
	public int calcShadowHit(rayCast _ray, rayCast transRay, myMatrix[] _ctAra, double distToLight){		
		rayHit hitChk = intersectCheck(_ray,transRay,_ctAra);			
		if (hitChk.isHit && (distToLight - hitChk.t) > epsVal){	return 1;}  
		//if (distToLight - hitChk.t > scene.epsVal){	return 1;}  
		return 0;
	}
	
	/**
	 * interpolate 2 vectors, t==0 == a, t==1 == b
	 * @param a
	 * @param t
	 * @param b
	 * @return
	 */
	public myVector interpVec(myVector a, double t, myVector b){
		myVector bMa = new myVector(a,b);
		return new myVector(a.x + t*bMa.x, a.y + t*bMa.y, a.z + t*bMa.z );
	}

	
	public String showUV(){
		String result = "ObjectType : " + type + " ID : " + ID ;//+ "\nMIN u&v :" + minU + " | " + minV + " | MAX u&v " + maxU + " | " + maxV;
		result += "\ncenter : " + origin ;
		result += shdr.showUV();
		return result;
	}//showUV
	@Override
	public myRTColor getColorAtPos(rayHit hit) {	return shdr.getColorAtPos(hit);}
		
	//get orthogonal vector to passed vector
	public myVector getOrthoVec(myVector vec){
		myVector tmpVec = new myVector(1,1,0);
		tmpVec._normalize();
		if(Math.abs(tmpVec._dot(vec) - 1) < epsVal ){		tmpVec.set(0,0,1); }	//colinear - want non-colinear vector for xprod
		myVector tmpRes = vec._cross(tmpVec);												//surface tangent vector - rotate this around normal by random amt, and extend from origin by random dist 0->radius			
		tmpRes._normalize();
		return tmpRes;
	}	

	public String toString(){
		String result = super.toString() +"\n"+shdr.toString();
		return result;
	}  
	
}//class mySceneObject

