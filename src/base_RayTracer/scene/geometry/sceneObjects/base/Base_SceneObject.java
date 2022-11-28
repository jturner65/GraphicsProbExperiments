package base_RayTracer.scene.geometry.sceneObjects.base;
import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.scene.shaders.myObjShader;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myVector;

public abstract class Base_SceneObject extends Base_Geometry{	
	
	public boolean[] rFlags;					//various state-related flags for this object
	public static final int 
			invertedIDX			= 0,				//normals point in or out
			isLightIDX			= 1//,
			//isTemplateObjIDX	= 2
			;				//this object is used to instance multiple objects (doesn't exist in scene itself)
	public static final int numFlags = 2;	

	public Base_SceneObject(Base_Scene _scn, double _x, double _y, double _z){
		super(_scn, _x, _y, _z);	    
	    initFlags();
	    shdr = new myObjShader(scene);//sets currently defined colors for this object also    
	}//constructor 6 var
		
	public void initFlags(){rFlags = new boolean[numFlags];for(int i=0; i<numFlags;++i){rFlags[i]=false;}}
	
	public void setFlags(int idx, boolean val){
		rFlags[idx]=val;
		switch(idx){
		case invertedIDX		:{break;}		
		case isLightIDX			:{break;}
	//	case isTemplateObjIDX	:{break;}			//this object is used to instance multiple objects (doesn't exist in scene itself)
		}
	}
	//determine if shadow ray is a hit or not
	@Override
	public int calcShadowHit(rayCast _ray, rayCast transRay, myMatrix[] _ctAra, double distToLight){		
		rayHit hitChk = intersectCheck(_ray,transRay,_ctAra);			
		if (hitChk.isHit && (distToLight - hitChk.t) > epsVal){	return 1;}  
		//if (distToLight - hitChk.t > scene.epsVal){	return 1;}  
		return 0;
	}
	
	//interpolate 2 vectors, t==0 == a, t==1 == b
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
		if(fastAbs(tmpVec._dot(vec) - 1) < epsVal ){		tmpVec.set(0,0,1); }	//colinear - want non-colinear vector for xprod
		myVector tmpRes = vec._cross(tmpVec);												//surface tangent vector - rotate this around normal by random amt, and extend from origin by random dist 0->radius			
		tmpRes._normalize();
		return tmpRes;
	}	

	public String toString(){
		String result = super.toString() +"\n"+shdr.toString();
//		if(rFlags[isTemplateObjIDX]){
//			result += "Template object not instanced\n";
//		}
		return result;
	}  
	
}//class mySceneObject

