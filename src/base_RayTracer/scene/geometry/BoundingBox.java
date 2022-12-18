package base_RayTracer.scene.geometry;

import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.utils.myRTColor;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.matrixObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * use this just to enclose other objects - make a myRndrdBox to render a box
 * @author 7strb
 *
 */
public class BoundingBox extends Base_Geometry {
	/**
	 * what this box bounds - can be other bboxes, lists, accel structs, or some mySceneObject
	 */
	private Base_Geometry obj;						
	
	public int maxExtentIdx;						//idx  (0,1,2) of maximum extent in this bounding box
	public myPoint sArea;
	public BoundingBox(Base_Scene _scn, myPoint _minVals, myPoint _maxVals){
		super (_scn, 0, 0, 0, GeomObjType.BBox);
		calcMinMaxCtrVals(_minVals, _maxVals);
		_bbox = null;							//a bbox should not have a bounding box
	}

	/**
	 * expand passed bbox to hold passed point - point is in box coords
	 * @param newPt
	 */
	private void expandMePt(myPoint newPt) {
		minVals.x = (minVals.x < newPt.x) ?minVals.x : newPt.x; 
		minVals.y = (minVals.y < newPt.y) ?minVals.y : newPt.y; 
		minVals.z = (minVals.z < newPt.z) ?minVals.z : newPt.z; 
		maxVals.x = (maxVals.x > newPt.x) ?maxVals.x : newPt.x; 
		maxVals.y = (maxVals.y > newPt.y) ?maxVals.y : newPt.y; 
		maxVals.z = (maxVals.z > newPt.z) ?maxVals.z : newPt.z; 
		calcMinMaxCtrVals(minVals,maxVals);
	}

	/**
	 * expand bbox to encompass passed box
	 * @param srcBox
	 * @param fwdTrans
	 */
	public void expandMeByTransBox(BoundingBox srcBox, myMatrix fwdTrans) {
		expandMePt(fwdTrans.transformPoint(srcBox.minVals));
		expandMePt(fwdTrans.transformPoint(srcBox.maxVals));
	}
	public void expandMeByBox(BoundingBox srcBox) {
		expandMePt(srcBox.minVals);
		expandMePt(srcBox.maxVals);
	}
	/**
	 * expand bbox by delta in all dir
	 * @param delta
	 */
	public void expandMeBoxDelta(double delta) {
		myPoint delVec = new myPoint(minVals);
		delVec._sub(delta, delta, delta);
		expandMePt(delVec);
		delVec.set(maxVals);
		delVec._add(delta, delta, delta);
		expandMePt(delVec);		
	}
	/**
	 * point needs to be in box space(transformed via box's ctm)
	 * @param tarBox
	 * @param pt
	 * @return
	 */
	public boolean pointIsInBox(BoundingBox tarBox, myVector pt){return (((tarBox.minVals.x < pt.x) && ( pt.x < tarBox.maxVals.x)) && 
													((tarBox.minVals.y < pt.y) && ( pt.y < tarBox.maxVals.y)) && 
													((tarBox.minVals.z < pt.z) && ( pt.z < tarBox.maxVals.z)));}

	
	public void calcMinMaxCtrVals(myPoint _minVals, myPoint _maxVals){
		minVals.set(MyMathUtils.min(_minVals.x, minVals.x),MyMathUtils.min(_minVals.y, minVals.y),MyMathUtils.min(_minVals.z, minVals.z));
		maxVals.set(MyMathUtils.max(_maxVals.x, maxVals.x),MyMathUtils.max(_maxVals.y, maxVals.y),MyMathUtils.max(_maxVals.z, maxVals.z));
		origin = new myPoint(minVals);
		origin._add(maxVals);
		origin._mult(.5);
		buildTransOrigin();
	    //trans_origin = origin.getAsAra();
	    myPoint difs = myPoint._sub(maxVals, minVals);	    
	    //myVector difs = new myVector(minVals,maxVals);
		double[] difVals = difs.asArray();
		double maxVal = MyMathUtils.max(difVals);
		maxExtentIdx = (maxVal == difVals[0] ? 0 : maxVal == difVals[1] ? 1 : 2);
		//myVector dif = new myVector(minVals,maxVals);
		sArea = new myPoint(difs.y*difs.z, difs.x*difs.z, difs.x*difs.y);						
	}
	
	/**
	 * Set the object this bounding box bounds
	 * @param _obj
	 */
	public void addObj(Base_Geometry _obj) {		
		obj = _obj;	
		CTMara = obj.CTMara;
	}	
	@Override
	public myPoint getMaxVec() {	return maxVals;	}
	@Override
	public myPoint getMinVec() {	return minVals;	}
	
	@Override//bbox has no txtrs
	public double[] findTxtrCoords(myPoint isctPt, int textureH, int textureW, double time) {return new double[]{0,0};}
	@Override
	protected double findTextureU(myPoint isctPt, double v, int textureH, int textureW, double time){ return 0.0; }
	@Override
	protected double findTextureV(myPoint isctPt, int textureH, int textureW, double time){	return 0.0;  } 
	
	//only says if bbox is hit
	@Override //_ctAra is ara of ctm for object held by bbox, and responsible for transformation of transray
	public final rayHit intersectCheck(rayCast _ray,rayCast transRay, myMatrix[] _ctAra) {
		//iterate through first low and then high values
		double[] rayO = transRay.originHAra,//new double[]{transRay.origin.x,transRay.origin.y,transRay.origin.z},
				rayD = transRay.dirHAra,//new double[]{transRay.direction.x,transRay.direction.y,transRay.direction.z},
				minValsAra = minVals.asArray(), maxValsAra = maxVals.asArray(),
				tmpVals1 = new double[]{-1,-1,-1},tmpVals2 = new double[]{-1,-1,-1},
				tMinVals = new double[]{Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE}, 
				tMaxVals = new double[]{-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE};
		double biggestMin = -Double.MAX_VALUE;
		int idx = -1;
		//for this to be inside, the max min value has to be smaller than the min max value 
		for(int i=0;i<3;++i){
			tmpVals1[i] = (minValsAra[i]-rayO[i])/rayD[i];
			tmpVals2[i] = (maxValsAra[i]-rayO[i])/rayD[i];
		}
		for(int i=0;i<3;++i){
			if(tmpVals1[i] < tmpVals2[i]){
				tMinVals[i] = tmpVals1[i];
				tMaxVals[i] = tmpVals2[i];
				if(biggestMin < tMinVals[i]){		idx = i;	biggestMin = tMinVals[i];	}//find biggest min val
			} else {
				tMinVals[i] = tmpVals2[i];
				tMaxVals[i] = tmpVals1[i];
				if(biggestMin < tMinVals[i]){		idx = i + 3;	biggestMin = tMinVals[i];}
			}
		}		
		if((MyMathUtils.min(tMaxVals) > MyMathUtils.max(tMinVals)) && biggestMin > 0){ //hit happens
			//pass args array to rayHit args : use idx[1] : this is idx (0 - 5) of plane intersected (low const x plane, low const y plane, low const z plane, high const x plane, high const y plane, high const z plane
			//return (obj instanceof myRndrdBox) ? 
				//transRay.objHit(transRay,obj,  _ctAra, transRay.pointOnRay(biggestMin),new int[]{0,idx},biggestMin) : 
				//obj.intersectCheck(ray, transRay, _ctAra);
			//TODO - should we use obj ctara or bbox ctara? should they be same?
			return transRay.objHit(obj,
					transRay.getTransformedVec(transRay.direction, _ctAra[glblIDX]), 
					_ctAra, transRay.pointOnRay(biggestMin),new int[]{0,idx},biggestMin);		
		} else {	return new rayHit(false);}			//does not hit		
	}//intersectCheck
	
	//determine if shadow ray is a hit or not - returns if object bounded by box is a hit
	@Override
	public final int calcShadowHit(rayCast _ray,rayCast _trans, myMatrix[] _ctAra, double distToLight){		
		rayHit hitChk = intersectCheck(_ray,_trans,_ctAra);			
		if (hitChk.isHit && (distToLight - hitChk.t) > epsVal){	return 1;}   
		return 0;
	}//
	
	@Override
	
	/**
	 * args : 
	 * use idx[1] : this is idx (0 - 5) of plane intersected 
	 * (low const x plane, low const y plane, low const z plane, high const x plane, high const y plane, high const z plane). 
	 * low planes have neg normal high planes have pos normal
	 */
	public myVector getNormalAtPoint(myPoint point, int[] args) {
		//System.out.print(args[1]);
		switch (args[1]){
		case 0 : {return new myVector(-1,0,0);}
		case 1 : {return new myVector(0,-1,0);}
		case 2 : {return new myVector(0,0,-1);}
		case 3 : {return new myVector(1,0,0);}
		case 4 : {return new myVector(0,1,0);}
		case 5 : {return new myVector(0,0,1);}		
		default : {return new myVector(0,0,-1);}
		}
	}
	
	@Override
	public myRTColor getColorAtPos(rayHit transRay) {	return new myRTColor(1,0,0);}

	public String toString(){
		String res = "\tBBOX : "+ ID + " BBox bounds : Min : " + minVals + " | Max : " + maxVals + " | Ctr " + origin + " Obj type : " + obj.type.toStrBrf()+"\n";
		return res;		
	}

}//myBBox
