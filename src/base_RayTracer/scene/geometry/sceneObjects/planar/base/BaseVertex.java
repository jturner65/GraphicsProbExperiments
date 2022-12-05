package base_RayTracer.scene.geometry.sceneObjects.planar.base;

import java.util.ArrayList;

import base_Math_Objects.vectorObjs.doubles.myVector;
import base_RayTracer.scene.base.Base_Scene;

//class to hold a vertex - will be shared by planar objects, will own it's own normal
class BaseVertex implements Comparable<BaseVertex> {
	public Base_Scene scn;
	public myVector V, N;
	private ArrayList<Base_PlanarObject> owners;
	
	public BaseVertex(Base_Scene _scn, myVector _v){
		scn = _scn;
		V = new myVector(_v);
		owners = new ArrayList<Base_PlanarObject>();		
	}
	
	public void addOwner(Base_PlanarObject obj){owners.add(obj);}
	//calc vertex normal of this vertex by finding the verts of each adjacent planar object and adding them
	public myVector calcNorm(){
		myVector res = new myVector(0,0,0);
		for(Base_PlanarObject obj : owners){res._add(obj.getNorm());}
		res._normalize();		
		return res;		
	}

	@Override
	public int compareTo(BaseVertex arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
		
	
	
}//myVertex