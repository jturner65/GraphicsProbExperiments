package base_RayTracer.scene.geometry.sceneObjects.planar;

import base_RayTracer.scene.objType;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.sceneObjects.planar.base.Base_PlanarObject;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

public class myQuad extends Base_PlanarObject{
  
	public myQuad(Base_Scene _scn){
		super(_scn, 4, objType.Quad);
	}//myQuad constructor
	
	@Override
	public double[] findTxtrCoords(myPoint isCtPt, PImage myTexture, double time){
	    myVector v2 = normToUse.getVecPtToPassedPt(isCtPt, 0), p2p0 = normToUse.getP2P(0), p2pLast = normToUse.getPLastP0();
	    double     
	    rawU = v2._dot(p2pLast) / p2pLast.sqMagn,
	    rawV = v2._dot(p2p0) / p2p0.sqMagn;
	    //map to given UV coords
		double u = (rawU*normToUse.getVertU(3)) + (1.0-rawU)*normToUse.getVertU(0),
			v =  (rawV*normToUse.getVertV(1)) + (1.0-rawV)*normToUse.getVertV(0);
	    
	    return new double[]{u*(myTexture.width-1),(1-v) *(myTexture.height-1)};
	}
	
}//class myQuad

