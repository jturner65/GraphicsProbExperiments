package base_RayTracer.scene.geometry.sceneObjects.planar.base;

import java.util.ArrayList;

import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * class to hold a vertex - will be shared by planar objects, will own it's own normal
 * @author 7strb
 *
 */
class BaseVertex implements Comparable<BaseVertex> {
	public myPoint V;
	public myVector N;
	private ArrayList<Base_PlanarObject> owners;
	
	public BaseVertex( myPoint _v){
		V = new myPoint(_v);
		owners = new ArrayList<Base_PlanarObject>();		
	}
	
	public void addOwner(Base_PlanarObject obj){owners.add(obj);}
	/**
	 * calc vertex normal of this vertex by averaging the face normals of each adjacent face
	 * @return
	 */
	public myVector calcNorm(){
		myVector res = new myVector(0,0,0);
		for(Base_PlanarObject obj : owners){res._add(obj.getWeightedNorm());}
		res._normalize();		
		return res;		
	}

	@Override
	public int compareTo(BaseVertex arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
		
	
	
}//myVertex