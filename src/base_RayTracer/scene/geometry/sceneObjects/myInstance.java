package base_RayTracer.scene.geometry.sceneObjects;

import base_RayTracer.myColor;
import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.geometry.myGeomBase;
import base_RayTracer.scene.geometry.accelStruct.*;
import base_Utils_Objects.vectorObjs.myMatrix;
import base_Utils_Objects.vectorObjs.myVector;
import processing.core.PImage;

//an instance of an instanced object - used to minimize memory footprint - only have a reference of the object, and then the relevant transformation matrices
public class myInstance extends myGeomBase{
	public myGeomBase obj;						//object this is instance of
	public boolean useShader, isAccel;					//whether to use instance shader or base object shader
	public myInstance(myScene scene, myGeomBase _obj){
		super(scene, 0,0,0);
		obj = _obj;										//owning object
		isAccel = (obj instanceof myAccelStruct);
		CTMara = buildCTMara(scene, obj.CTMara[glblIDX]);//build this object's transformation matrix - since this is instancing the owning object, pass the owning object's matrix
	    //CTMara = scene.p.buildCTMara(scene);//build this object's transformation matrix		    
	    type = objType.Instance;//"Instance of "+obj.objType;
	    this.minVals = getMinVec();
	    this.maxVals = getMaxVec();
		postProcBBox();				//cnstrct and define bbox
		shdr = null;
		useShader = false;
	}
	
	//return vector with maximum x/y/z coords of this object
	public myVector getMaxVec(){return getTransformedPt(obj.getMaxVec(), obj.CTMara[glblIDX]);}
	//return vector with minimum x/y/z coords of this object
	public myVector getMinVec(){return getTransformedPt(obj.getMinVec(), obj.CTMara[glblIDX]);}
	@Override
	public int calcShadowHit(myRay _ray,myRay _trans, myMatrix[] _ctAra, double distToLight) {
		return obj.calcShadowHit(_trans, _trans, _ctAra, distToLight);
	}
	@Override
	public myVector getOrigin(double t) {	return getTransformedPt(obj.getOrigin(t), obj.CTMara[glblIDX]);}
	@Override
	public rayHit intersectCheck(myRay _ray,myRay transRay, myMatrix[] _ctAra) {
		rayHit _hit = obj.intersectCheck(transRay, transRay, _ctAra);		//copy trans ray over so that ctm-transformed accel structs will still register hits appropriately TODO make this better
		if(useShader){_hit.shdr = shdr;}
		return _hit;	
	}
	@Override
	public myVector getNormalAtPoint(myVector point, int[] args) {		return obj.getNormalAtPoint(point, args);		}	
	//set to override base object's shader
	public void useInstShader(){	useShader = true; shdr = scene.getCurShader();}//new myObjShader(scene);	}	
	//this is probably not the best way to do this - each instance needs to have its own UV coords.  TODOgetTransformedPt(isctPt, CTMara[invIDX]);
	@Override
	//public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){ return obj.findTxtrCoords(isctPt, myTexture, time);}
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){ 
		return obj.findTxtrCoords(getTransformedVec(isctPt, CTMara[invIDX]), myTexture, time);}

	@Override
	public myColor getColorAtPos(rayHit hit) {		return (useShader) ? shdr.getColorAtPos(hit) : hit.obj.shdr.getColorAtPos(hit);}//return (hit.obj instanceof mySceneObject) ? ((mySceneObject)hit.obj).shdr.getColorAtPos(hit) : new myColor(-1,-1,-1,1);	}
	public String toString(){
		String result = super.toString() + " which is an instance of : "+obj.type;
	    return result;
	}

}//myInstance

