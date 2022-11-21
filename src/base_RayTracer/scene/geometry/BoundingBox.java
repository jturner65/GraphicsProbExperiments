package base_RayTracer.scene.geometry;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;
 
/**
 * use this just to enclose other objects - make a mySceneObj box to render a box
 * @author 7strb
 *
 */
public class BoundingBox extends Base_Geometry {
	private Base_Geometry obj;							//what this box bounds - can be other bboxes, lists, accel structs, or some mySceneObject
	
	public int maxExtentIdx;						//idx  (0,1,2) of maximum extent in this bounding box
	public myVector sArea;
	public BoundingBox(myScene _scn, myPoint _minVals, myPoint _maxVals){
		super (_scn, 0, 0, 0);
		type = objType.BBox;
		calcMinMaxCtrVals(_minVals, _maxVals);
		_bbox = null;							//a bbox should not have a bounding box
	}

	//expand passed bbox to hold passed point - point is in box coords
	private void expandMePt(myPoint newPt) {
		minVals.x = (minVals.x < newPt.x) ?minVals.x : newPt.x; 
		minVals.y = (minVals.y < newPt.y) ?minVals.y : newPt.y; 
		minVals.z = (minVals.z < newPt.z) ?minVals.z : newPt.z; 
		maxVals.x = (maxVals.x > newPt.x) ?maxVals.x : newPt.x; 
		maxVals.y = (maxVals.y > newPt.y) ?maxVals.y : newPt.y; 
		maxVals.z = (maxVals.z > newPt.z) ?maxVals.z : newPt.z; 
		calcMinMaxCtrVals(minVals,maxVals);
	}

	//expand bbox to encompass passed box
	public void expandMeByTransBox(BoundingBox srcBox, myMatrix fwdTrans) {
		expandMePt(getTransformedPt(srcBox.minVals,fwdTrans));
		expandMePt(getTransformedPt(srcBox.maxVals,fwdTrans));
	}
	public void expandMeByBox(BoundingBox srcBox) {
		expandMePt(srcBox.minVals);
		expandMePt(srcBox.maxVals);
	}
	//expand bbox by delta in all dir
	public void expandMeBoxDel(double delta) {
		myVector delVec = new myVector(minVals);
		delVec._sub(delta, delta, delta);
		expandMePt(delVec);
		delVec = new myVector(maxVals);
		delVec._add(delta, delta, delta);
		expandMePt(delVec);		
	}
	//point needs to be in box space(transformed via box's ctm)
	public boolean pointIsInBox(BoundingBox tarBox, myVector pt){return (((tarBox.minVals.x < pt.x) && ( pt.x < tarBox.maxVals.x)) && 
													((tarBox.minVals.y < pt.y) && ( pt.y < tarBox.maxVals.y)) && 
													((tarBox.minVals.z < pt.z) && ( pt.z < tarBox.maxVals.z)));}


	
	public void calcMinMaxCtrVals(myPoint _minVals, myPoint _maxVals){
		minVals.set(Math.min(_minVals.x, minVals.x),Math.min(_minVals.y, minVals.y),Math.min(_minVals.z, minVals.z));
		maxVals.set(Math.max(_maxVals.x, maxVals.x),Math.max(_maxVals.y, maxVals.y),Math.max(_maxVals.z, maxVals.z));
		origin = new myPoint(minVals);
		origin._add(maxVals);
		origin._mult(.5);
	    trans_origin = getTransformedPt(origin, CTMara[glblIDX]).asArray();
	    //trans_origin = origin.getAsAra();
	    myPoint difs = myPoint._sub(maxVals, minVals);	    
	    //myVector difs = new myVector(minVals,maxVals);
		double[] difVals = difs.asArray();
		double maxVal =  max(difVals);
		maxExtentIdx = (maxVal == difVals[0] ? 0 : maxVal == difVals[1] ? 1 : 2);
		//myVector dif = new myVector(minVals,maxVals);
		sArea =  new myVector (difs.y*difs.z, difs.x*difs.z, difs.x*difs.y);						
	}

	public void addObj(Base_Geometry _obj) {		
		obj = _obj;	
		CTMara = obj.CTMara;
	}	
	@Override
	public myPoint getMaxVec() {	return maxVals;	}
	@Override
	public myPoint getMinVec() {	return minVals;	}
	
	@Override//bbox has no txtrs
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time) {return new double[]{0,0};}
	@Override
	protected double findTextureU(myPoint isctPt, double v, PImage myTexture, double time){ return 0.0; }
	@Override
	protected double findTextureV(myPoint isctPt, PImage myTexture, double time){	return 0.0;  } 
	
	//only says if bbox is hit
	@Override //_ctAra is ara of ctm for object held by bbox, and responsible for transformation of transray
	public rayHit intersectCheck(rayCast _ray,rayCast transRay, myMatrix[] _ctAra) {
		//iterate through first low and then high values
		double[] rayO = transRay.originAra,//new double[]{transRay.origin.x,transRay.origin.y,transRay.origin.z},
				rayD = transRay.dirAra,//new double[]{transRay.direction.x,transRay.direction.y,transRay.direction.z},
				minValsAra = minVals.asArray(), maxValsAra = maxVals.asArray(),
				tmpVals1 = new double[]{-Double.MAX_VALUE,-1,-1},tmpVals2 = new double[]{-1,-1,-1},
				tMinVals = new double[]{Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE}, tMaxVals = new double[]{-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE};
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
				if(biggestMin < tmpVals1[i]){		idx = i;	biggestMin = tmpVals1[i];	}
			} else {
				tMinVals[i] = tmpVals2[i];
				tMaxVals[i] = tmpVals1[i];
				if(biggestMin < tmpVals2[i]){		idx = i + 3;	biggestMin = tmpVals2[i];}
			}
		}		
		if((min(tMaxVals) > max(tMinVals)) && biggestMin > 0){ //hit happens
			//pass args array to rayHit args : use idx[1] : this is idx (0 - 5) of plane intersected (low const x plane, low const y plane, low const z plane, high const x plane, high const y plane, high const z plane
			//return (obj instanceof myRndrdBox) ? transRay.objHit(transRay,obj,  _ctAra, transRay.pointOnRay(biggestMin),new int[]{0,idx},biggestMin) : obj.intersectCheck(ray, transRay, _ctAra);
			return transRay.objHit(obj,transRay.getTransformedVec(transRay.direction, _ctAra[glblIDX]),  _ctAra, transRay.pointOnRay(biggestMin),new int[]{0,idx},biggestMin);		//TODO - should we use obj ctara or bbox ctara? should they be same?
		} else {	return new rayHit(false);}			//does not hit		
	}
	
	//determine if shadow ray is a hit or not - returns if object bounded by box is a hit
	@Override
	public int calcShadowHit(rayCast _ray,rayCast _trans, myMatrix[] _ctAra, double distToLight){		
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
	@Override
	public myPoint getOrigin(double t) {return origin;}	
	public String toString(){
		String res = "\t\tBBOX : "+ ID + " BBox bounds : Min : " + minVals + " | Max : " + maxVals + " | Ctr " + origin + " Obj type : " + obj.type+"\n";
		return res;		
	}

}//myBBox
