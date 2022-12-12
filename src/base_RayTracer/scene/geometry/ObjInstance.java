package base_RayTracer.scene.geometry;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.accelStruct.base.Base_AccelStruct;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.utils.myRTColor;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

/**
 * an instance of an instanced object - used to minimize memory footprint - only have a reference of the object, and then the relevant transformation matrices
 * @author 7strb
 *
 */
public class ObjInstance extends Base_Geometry{
	public Base_Geometry obj;						//object this is instance of
	private int[] instFlags;					//various state-related flags for this object
	private static final int 
		useShaderIDX = 0,
		isAccelStructIDX = 1;
	private static final int numFlags = 2;

	public ObjInstance(Base_Scene scene, Base_Geometry _obj){
		super(scene, 0,0,0, GeomObjType.Instance);
		initFlags();
		obj = _obj;										//owning object
		setAccleStruct(obj instanceof Base_AccelStruct);
		CTMara = buildCTMara(scene.gtPeekMatrix(), obj.CTMara[glblIDX]);//build this object's transformation matrix - since this is instancing the owning object, pass the owning object's matrix
	    //CTMara = scene.p.buildCTMara(scene);//build this object's transformation matrix		    
	    this.minVals = getMinVec();
	    this.maxVals = getMaxVec();
		postProcBBox();				//cnstrct and define bbox
		shdr = null;
		setUseShader(false);
	}
	
	/**
	 * base class flags init
	 */
	private final void initFlags(){instFlags = new int[1 + numFlags/32];for(int i =0; i<numFlags;++i){setFlags(i,false);}}			
	/**
	 * get baseclass flag
	 * @param idx
	 * @return
	 */
	private final boolean getFlags(int idx){int bitLoc = 1<<(idx%32);return (instFlags[idx/32] & bitLoc) == bitLoc;}	
		
	public final boolean doUseShader() {return getFlags(useShaderIDX);}
	public final boolean isAccleStruct() {return getFlags(isAccelStructIDX);}
	
	public final void setUseShader(boolean _val) {setFlags(useShaderIDX, _val);}
	public final void setAccleStruct(boolean _val) {setFlags(isAccelStructIDX, _val);}
	
	
	/**
	 * set baseclass flags  //setFlags(showIDX, 
	 * @param idx
	 * @param val
	 */
	private final void setFlags(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		instFlags[flIDX] = (val ?  instFlags[flIDX] | mask : instFlags[flIDX] & ~mask);
		switch(idx){
			case useShaderIDX			:{break;}		
			case isAccelStructIDX		:{break;}
		}				
	}//setFlags
	
	//return vector with maximum x/y/z coords of this object
	@Override
	public myPoint getMaxVec(){return obj.CTMara[glblIDX].transformPoint(obj.getMaxVec());}
	//return vector with minimum x/y/z coords of this object
	@Override
	public myPoint getMinVec(){return obj.CTMara[glblIDX].transformPoint(obj.getMinVec());}
	@Override
	public final int calcShadowHit(rayCast _ray, rayCast _trans, myMatrix[] _ctAra, double distToLight) {
		return obj.calcShadowHit(_trans, _trans, _ctAra, distToLight);
	}
	@Override
	public myPoint getOrigin(double t) {	return obj.CTMara[glblIDX].transformPoint(obj.getOrigin(t));}
	@Override
	public rayHit intersectCheck(rayCast _ray, rayCast transRay, myMatrix[] _ctAra) {
		//copy trans ray over so that ctm-transformed accel structs will still register hits appropriately TODO make this better
		rayHit _hit = obj.intersectCheck(transRay, transRay, _ctAra);		
		if(doUseShader()){_hit.shdr = shdr;}
		return _hit;	
	}
	@Override
	public myVector getNormalAtPoint(myPoint point, int[] args) {		return obj.getNormalAtPoint(point, args);		}	
	//set to override base object's shader
	public void useInstShader(){	setUseShader(true); shdr = scene.getCurShader();}//new myObjShader(scene);	}	
	//this is probably not the best way to do this - each instance needs to have its own UV coords.  TODOgetTransformedPt(isctPt, CTMara[invIDX]);
	//public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){ return obj.findTxtrCoords(isctPt, myTexture, time);}
	@Override
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time){ 
		return obj.findTxtrCoords(CTMara[invIDX].transformPoint(isctPt), myTexture, time);}

	@Override
	protected double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){ return 0.0; }
	@Override
	protected double findTextureV(myPoint isctPt, PImage myTexture, double time){	return 0.0;  } 

	
	@Override
	public myRTColor getColorAtPos(rayHit hit) {		return (doUseShader()) ? shdr.getColorAtPos(hit) : hit.obj.shdr.getColorAtPos(hit);}//return (hit.obj instanceof mySceneObject) ? ((mySceneObject)hit.obj).shdr.getColorAtPos(hit) : new myColor(-1,-1,-1,1);	}
	public String toString(){
		String result = super.toString() + " which is an instance of : "+obj.type;
	    return result;
	}

}//myInstance

