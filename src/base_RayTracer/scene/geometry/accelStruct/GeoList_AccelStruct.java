package base_RayTracer.scene.geometry.accelStruct;

import java.util.ArrayList;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.accelStruct.base.AccelStructType;
import base_RayTracer.scene.geometry.accelStruct.base.Base_AccelStruct;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
/**
 * Flat list structure 
 * @author 7strb
 *
 */
public class GeoList_AccelStruct extends Base_AccelStruct{
	public ArrayList<Base_Geometry> objList;
	public GeoList_AccelStruct(Base_Scene _scn){
		super(_scn,0,0,0, GeomObjType.AccelFlatList);
		objList = new ArrayList<Base_Geometry>();
		setTypeOfAccel(AccelStructType.FlatList);
		postProcAccelStruct();
	}
	
	/**
	 * Add passed object to this acceleration list
	 * @param _obj
	 */
	public void addObj(Base_Geometry _obj) {
		objList.add(_obj);	
		myMatrix tmp = CTMara[invIDX].multMat(_obj.CTMara[glblIDX]);
		//gtMatrix tmp = (_obj.CTMara[glblIDX]);
		_bbox.expandMeByTransBox(_obj.getBBox(), tmp);
	}
	@Override  
	//check if object's contents block the light - check if any are accel structs or instance of accel struct
	public final int calcShadowHit(rayCast _ray,rayCast _trans, myMatrix[] _ctAra, double distToLight) {
		if(_bbox.calcShadowHit( _ray, _trans, _ctAra, distToLight) == 0 ){return 0;}				//no hit of bounding box, then no hit 	
		rayCast _objTransRay;
		for (Base_Geometry obj : objList){
			_objTransRay = _ray.getTransformedRay(_ray, obj.CTMara[invIDX]);
			//double dist = distToLight/_objTransRay.scale;
			if(obj.calcShadowHit( _ray, _objTransRay, _ctAra, distToLight)==1){return 1;}
		}//for each object in scene
		return 0;
	}//
	
	/**
	 * build traversal based on _ray - go through all objects in structure
	 * @param _ray ray cast
	 * @param _transRay transformed ray cast
	 * @param _ctAra transformation matrix for the ray
	 */
	@Override
	public rayHit traverseStruct(rayCast _ray, rayCast _transRay, myMatrix[] _ctAra){	
		double _clsT = Double.MAX_VALUE;
		rayHit _clsHit = null;
		rayCast _objTransRay, _closestTransRay = null;
		Base_Geometry _clsObj = null;
		for (Base_Geometry obj : objList){
			_objTransRay = _ray.getTransformedRay(_ray, obj.CTMara[invIDX]);
			//checking if from instance so we can propagate the instance transform mat
			rayHit _hit =  obj.intersectCheck(_ray, _objTransRay, obj.CTMara);	//scene.p.reBuildCTMara(obj.CTMara[glblIDX], _ctAra[glblIDX])
			if (_hit.t < _clsT ){
				_clsObj = obj;
				_clsHit = _hit;
				_clsT = _hit.t;
				_closestTransRay = _objTransRay;
			}
		}//for obj in scenelist
		if(_clsHit == null){return new rayHit(false);}
		_clsHit.reCalcCTMHitNorm(buildCTMara(_clsObj.CTMara[glblIDX], CTMara[glblIDX]));
		if(!(_clsObj instanceof Base_AccelStruct)){	return _clsHit;		}		//hit object
		rayHit _hit2 = ((Base_AccelStruct)_clsObj).traverseStruct(_ray,_closestTransRay, _clsHit.CTMara);	
		return _hit2;
	}//	traverseStruct
	
	@Override
	public String toString(){String res = super.toString() + "Flat List of Size : "+objList.size() + "\n"; return res;	}
}//GeometryList