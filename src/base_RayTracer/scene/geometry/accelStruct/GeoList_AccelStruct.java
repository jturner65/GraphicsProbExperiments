package base_RayTracer.scene.geometry.accelStruct;

import java.util.ArrayList;

import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.geometry.accelStruct.base.Base_AccelStruct;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_Math_Objects.vectorObjs.doubles.myMatrix;

public class GeoList_AccelStruct extends Base_AccelStruct{
	public ArrayList<Base_Geometry> objList;
	public GeoList_AccelStruct(myScene _scn){
		super(_scn,0,0,0);
		objList = new ArrayList<Base_Geometry>();
		type = objType.AccelFlatList;
		setTypeOfAccel(0);
		postProcBBox();				//cnstrct and define bbox
	}

	public void addObj(Base_Geometry _obj) {
		objList.add(_obj);	
		myMatrix tmp = CTMara[invIDX].multMat(_obj.CTMara[glblIDX]);
		//gtMatrix tmp = (_obj.CTMara[glblIDX]);
		_bbox.expandMeByTransBox(_obj._bbox, tmp);
	}
	@Override  //check if object's contents block the light - check if any are accel structs or instance of accel struct
	public int calcShadowHit(myRay _ray,myRay _trans, myMatrix[] _ctAra, double distToLight) {
		if(_bbox.calcShadowHit( _ray, _trans, _ctAra, distToLight) == 0 ){return 0;}				//no hit of bounding box, then no hit 	
		myRay _objTransRay;
		for (Base_Geometry obj : objList){
			_objTransRay = _ray.getTransformedRay(_ray, obj.CTMara[invIDX]);
			//double dist = distToLight/_objTransRay.scale;
			if(obj.calcShadowHit( _ray, _objTransRay, _ctAra, distToLight)==1){return 1;}
		}//for each object in scene
		return 0;
	}//
	
	//build traversal based on _ray - go through all objects in structure
	@Override
	public rayHit traverseStruct(myRay _ray,myRay _trans, myMatrix[] _ctAra){	
		double _clsT = Double.MAX_VALUE;
		rayHit _clsHit = null;
		myRay _objTransRay, _closestTransRay = null;
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
		_clsHit.reCalcCTMHitNorm(reBuildCTMara(_clsObj.CTMara[glblIDX], CTMara[glblIDX]));
		if(!(_clsObj instanceof Base_AccelStruct)){	return _clsHit;		}		//hit object
		rayHit _hit2 = ((Base_AccelStruct)_clsObj).traverseStruct(_ray,_closestTransRay, _clsHit.CTMara);	
		return _hit2;
	}//	traverseStruct
	
	@Override
	public String toString(){String res = super.toString() + "Flat List of Size : "+objList.size() + "\n"; return res;	}
}//GeometryList