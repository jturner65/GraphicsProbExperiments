package base_RayTracer.scene.geometry.sceneObjects.planar;

import base_RayTracer.myRay;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PImage;

public class myQuad extends myPlanarObject{
	  
		public myQuad(myScene _scn){
			super(_scn);
			this.vCount = 4;
			type = objType.Quad;
			initObjVals();
		}//myQuad constructor (4)
		
		public boolean checkInside(myVector rayPoint, myRay ray){
			//find ray from each vertex to the planar intersection point
			myVector intRay = new myVector(0,0,0);
			for(int i =0; i<vCount; ++i){
				int pIdx = (i==0 ? vCount-1 : i-1);
				intRay.set(rayPoint.x - vertX[i],rayPoint.y - vertY[i], rayPoint.z - vertZ[i]);
				myVector tmp = intRay._cross(P2P[pIdx]);
				//tmp._normalize();
				if(tmp._dot(N)< -epsVal){return false;}			
			}
			return true;
		}//checkInside method
		
		@Override
		public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){
		    myVector v2 = new myVector(P[0],isctPt);
		    double dot20 = v2._dot(P2P[0]), dot21 = v2._dot(P2P0),
		    c_u = ((dotVals[2] * dot20) - (dotVals[vCount] * dot21)) * baryIDenomTxtr,
		    c_v = ((dotVals[0] * dot21) - (dotVals[vCount] * dot20)) * baryIDenomTxtr,
		    c_w = 1 - c_u - c_v;
		    double u = vertU[0] * c_w + vertU[1] * c_u + vertU[2]*c_v, v = vertV[0] * c_w + vertV[1] * c_u + vertV[2]*c_v;
		    return new double[]{u*(myTexture.width-1),(1-v)*(myTexture.height-1)};
		}
		
	}//class myQuad

