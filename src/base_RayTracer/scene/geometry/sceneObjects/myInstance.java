package base_RayTracer.scene.geometry.sceneObjects;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.geometry.accelStruct.base.Base_AccelStruct;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

/**
 * an instance of an instanced object - used to minimize memory footprint - only have a reference of the object, and then the relevant transformation matrices
 * @author 7strb
 *
 */
public class myInstance extends Base_Geometry{
	public Base_Geometry obj;						//object this is instance of
	public boolean useShader, isAccel;					//whether to use instance shader or base object shader
	public myInstance(myScene scene, Base_Geometry _obj){
		super(scene, 0,0,0);
		obj = _obj;										//owning object
		isAccel = (obj instanceof Base_AccelStruct);
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
	@Override
	public myPoint getMaxVec(){return getTransformedPt(obj.getMaxVec(), obj.CTMara[glblIDX]);}
	//return vector with minimum x/y/z coords of this object
	@Override
	public myPoint getMinVec(){return getTransformedPt(obj.getMinVec(), obj.CTMara[glblIDX]);}
	@Override
	public int calcShadowHit(rayCast _ray,rayCast _trans, myMatrix[] _ctAra, double distToLight) {
		return obj.calcShadowHit(_trans, _trans, _ctAra, distToLight);
	}
	@Override
	public myPoint getOrigin(double t) {	return getTransformedPt(obj.getOrigin(t), obj.CTMara[glblIDX]);}
	@Override
	public rayHit intersectCheck(rayCast _ray,rayCast transRay, myMatrix[] _ctAra) {
		rayHit _hit = obj.intersectCheck(transRay, transRay, _ctAra);		//copy trans ray over so that ctm-transformed accel structs will still register hits appropriately TODO make this better
		if(useShader){_hit.shdr = shdr;}
		return _hit;	
	}
	@Override
	public myVector getNormalAtPoint(myPoint point, int[] args) {		return obj.getNormalAtPoint(point, args);		}	
	//set to override base object's shader
	public void useInstShader(){	useShader = true; shdr = scene.getCurShader();}//new myObjShader(scene);	}	
	//this is probably not the best way to do this - each instance needs to have its own UV coords.  TODOgetTransformedPt(isctPt, CTMara[invIDX]);
	//public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){ return obj.findTxtrCoords(isctPt, myTexture, time);}
	@Override
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time){ 
		return obj.findTxtrCoords(getTransformedPt(isctPt, CTMara[invIDX]), myTexture, time);}

	@Override
	protected double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){ return 0.0; }
	@Override
	protected double findTextureV(myPoint isctPt, PImage myTexture, double time){	return 0.0;  } 

	
	@Override
	public myRTColor getColorAtPos(rayHit hit) {		return (useShader) ? shdr.getColorAtPos(hit) : hit.obj.shdr.getColorAtPos(hit);}//return (hit.obj instanceof mySceneObject) ? ((mySceneObject)hit.obj).shdr.getColorAtPos(hit) : new myColor(-1,-1,-1,1);	}
	public String toString(){
		String result = super.toString() + " which is an instance of : "+obj.type;
	    return result;
	}

}//myInstance

