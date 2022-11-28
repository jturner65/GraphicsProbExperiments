package base_RayTracer.scene.geometry.sceneObjects.planar;

import base_RayTracer.ray.rayCast;
import base_RayTracer.scene.objType;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.sceneObjects.planar.base.Base_PlanarObject;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

//infinite plane object
public class myPlane extends Base_PlanarObject{

	public myPlane(Base_Scene _scn){
		super(_scn);
		vCount = 4;
		type = objType.Plane;
		initObjVals();
	}
	
	public void setPlaneVals(double _a, double _b, double _c, double _d){
		N.set( _a, _b, _c);
		double mag = N._mag();
		N._normalize();
		peqA = N.x;
		peqB = N.y;
		peqC = N.z;
		peqD = _d/mag;		
		myVector rotVec = new myVector(peqB, peqC, peqA);//vector to cross N with - so 
		if((peqA == peqB) && (peqA == peqC)){	rotVec._add(1,0,0);		}//if all equal then shifting above them gives no benefit
		rotVec._normalize();
		//basis vex in plane :
		int idx = 7;//x,y,z
		double sum = peqA + peqB + peqC;					//potential point coords
		if(sum == 0){sum = peqA + peqB;idx = 6; 			//x,y,!z
			if(sum == 0){sum = peqA + peqC;idx = 5;			//x,!y,z
				if(sum == 0){sum = peqB + peqC;idx = 3;}}}	//!x,y,z		
		myVector planePt = new myVector(((idx & 4) == 4 ? -peqD/sum : 0),((idx & 2) == 2 ? -peqD/sum : 0),((idx & 1) == 1 ? -peqD/sum : 0) );
		myVector inPlaneU = N._cross(rotVec),//any vector perp to N will be in plane
		inPlaneV = N._cross(inPlaneU);		//in other direction 
		//find 4 verts on this plane
		vertX[0]=planePt.x;vertY[0]=planePt.y;vertZ[0]=planePt.z;
		myVector nextPt = new myVector(planePt);
		nextPt._add(inPlaneU);
		vertX[1]=nextPt.x;vertY[1]=nextPt.y;vertZ[1]=nextPt.z;
		nextPt._add(inPlaneV);
		vertX[2]=nextPt.x;vertY[2]=nextPt.y;vertZ[2]=nextPt.z;
		nextPt.set(planePt);
		nextPt._add(inPlaneV);
		vertX[3]=nextPt.x;vertY[3]=nextPt.y;vertZ[3]=nextPt.z;
		
		//set origin TODO no origin, find a point on the plane
		origin.set(vertX[0],vertY[0],vertZ[0]);
		//setCurrColors();		
	}
	//plane is infinite, so min/max are meaningless.  min/max used for bboxes, and plane isect check is faster than bbox isect check anyway, so shouldn't ever put in bbox
	@Override
	public myVector getMaxVec(){return new myVector(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE);}	
	@Override
	public myVector getMinVec(){return  new myVector(-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE);}
	@Override//infinite plane isect is always inside
	public boolean checkInside(myPoint rayPoint, rayCast ray){	return true;	}//checkInside method
	@Override
	public double[] findTxtrCoords(myPoint isctPt, PImage myTexture, double time){		
	    myVector v2 = new myVector(P[0],isctPt);
	    double dot20 = v2._dot(P2P[0]), dot21 = v2._dot(P2P0),
	    c_u = ((dotVals[2] * dot20) - (dotVals[vCount] * dot21)) * baryIDenomTxtr,
	    c_v = ((dotVals[0] * dot21) - (dotVals[vCount] * dot20)) * baryIDenomTxtr,
	    c_w = 1 - c_u - c_v;
	    double u = vertU[0] * c_w + vertU[1] * c_u + vertU[2]*c_v, v = vertV[0] * c_w + vertV[1] * c_u + vertV[2]*c_v;
	    return new double[]{u*(myTexture.width-1),(1-v)*(myTexture.height-1)};
	}
	
}//myPlane