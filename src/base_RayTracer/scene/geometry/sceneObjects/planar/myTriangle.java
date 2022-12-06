package base_RayTracer.scene.geometry.sceneObjects.planar;

import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.GeomObjType;
import base_RayTracer.scene.geometry.sceneObjects.planar.base.Base_PlanarObject;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

public class myTriangle extends Base_PlanarObject{
  
	public myTriangle(Base_Scene _scn){
		super(_scn, 3, GeomObjType.Triangle);
	}	//myTriangle constructor
	
	@Override
	public double[] findTxtrCoords(myPoint isCtPt, PImage myTexture, double time){
	    myVector v2 = normToUse.getVecPtToPassedPt(isCtPt, 0);
	    double dot20 = v2._dot(normToUse.getP2P(0)), dot21 = v2._dot(normToUse.getPLastP0()),
	    c_u = ((normToUse.getP2P(2).sqMagn * dot20) - (normToUse.getDotValFinal() * dot21)) * normToUse.getBaryIDenomTxtr(),
	    c_v = ((normToUse.getP2P(0).sqMagn * dot21) - (normToUse.getDotValFinal() * dot20)) * normToUse.getBaryIDenomTxtr(),
	    c_w = 1 - c_u - c_v;
	    double u = normToUse.getVertU(0) * c_w + normToUse.getVertU(1) * c_u + normToUse.getVertU(2)*c_v, 
	    		v = normToUse.getVertV(0) * c_w + normToUse.getVertV(1) * c_u + normToUse.getVertV(2)*c_v;
	    return new double[]{u*(myTexture.width-1),(1-v)*(myTexture.height-1)};
	}
	
}//class myTriangle

