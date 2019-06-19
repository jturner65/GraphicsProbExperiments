package base_RayTracer.scene.textures;

import base_RayTracer.scene.myScene;
import base_RayTracer.scene.geometry.sceneObjects.implicit.mySphere;
import base_Utils_Objects.vectorObjs.myVector;

//sphere that is moving between origin 0 and origin 1
public class myMovingSphere extends mySphere{
	public myVector origin0, origin1;				//values of origins for this moving sphere (0 at t==0, 1 at t==1);
	public myMovingSphere(myScene _p, double myRadius, double x0, double y0, double z0, double x1, double y1, double z1, boolean active){	
		super(_p, myRadius,x0,y0,z0,active); 		//force main origin to be 0,0,0
		origin0 = new myVector(origin);
		origin1 = new myVector(x1,y1,z1);
	}
	
	@Override
	public myVector getOrigin(double _t){	return interpVec(origin0, _t, origin1);	}
	
}//myMovingSphere
