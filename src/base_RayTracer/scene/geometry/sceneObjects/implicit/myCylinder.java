package base_RayTracer.scene.geometry.sceneObjects.implicit;

import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.objType;
import base_Math_Objects.vectorObjs.doubles.myMatrix;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myCylinder extends myHollow_Cylinder{
	private double[][] capEqs;
  
	public myCylinder(myScene _p, double _myRadius, double _myHeight, double x, double y, double z, double xO,double yO, double zO){
		super(_p, _myRadius, _myHeight, x, y, z, xO, yO, zO);
		type = objType.Cylinder;
		
		//cap eqs are planar equations for endcaps of cylinder - treat xO,yO,zO (orientation vector) as direction of top cap 
		double [] topCapEq = new double[4], btmCapEq = new double[4];		//ax + by + cz + d = 0 : a,b,c are normal, d is -distance from origin
		topCapEq[0] = xO;    topCapEq[1] = yO; 	topCapEq[2] = zO;
		btmCapEq[0] = xO;    btmCapEq[1] = -yO;	btmCapEq[2] = zO;	
		topCapEq[3] = -yTop;
		btmCapEq[3] = yBottom;			//mult by -1 so that normal is pointing down
		capEqs = new double[][]{topCapEq, btmCapEq};
	    minVals = this.getMinVec();
	    maxVals = this.getMaxVec();	    
		postProcBBox();				//cnstrct and define bbox
	}
 
	//check if passed ray intersects with this cylinder - first using x/z for circular intersection, then planar intersection with end caps, then check which is closest and positive  
	public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){
		
		double a = getAVal(transRay),b = getBVal(transRay), c = getCVal(transRay);
		double discr = ((b*b) - (4*a*c));
		//quadratic - check first if imaginary - if so then no intersection
		if (!(discr < 0)){//real roots exist - means ray hits x-z walls somewhere
			//find values of t - want those that give largest value of z, to indicate the closest intersection to the eye
			double discr1 = Math.pow(discr,.5),t1 = (-b + discr1)/(2*a),t2 = (-b - discr1)/(2*a);
			//set the t value of the intersection to be the minimum of these two values (which would be the edge closest to the eye/origin of the ray)
			// (the other value, if one exists, is on the other side of the sphere)
			double cyltVal = Math.min(t1,t2), cyltOtr = Math.max(t1, t2);
			if (cyltVal < epsVal){//if min less than 0 then that means it intersects behind the viewer.  pick other t
				cyltOtr = cyltVal;
				cyltVal = Math.max(t1, t2);				
				if (cyltVal < epsVal){//if still less than 0 then that means both intersections behind ray origin - this isn't an intersection, dont draw
					return new rayHit(false);	
				}//if both t's are less than 0 then don't paint anything
			}//if the min t val is less than 0
			
			//check if caps instead of walls - intersect t for plane should be between tVal and tOtr	
			boolean planeRes = true;
			double[] num = new double[]{0,0}, denom = new double[]{1,1}, pl_tVal = new double[]{0,0};
			for(int i = 0; i< capEqs.length;++i){
				denom[i]=capEqs[i][0]*transRay.direction.x + capEqs[i][1]*transRay.direction.y + capEqs[i][2]*transRay.direction.z;
				if(Math.abs(denom[i]) > epsVal) {
					num[i]=capEqs[i][0]*transRay.origin.x + capEqs[i][1]*transRay.origin.y + capEqs[i][2]*transRay.origin.z + capEqs[i][3];
					pl_tVal[i] = -num[i]/denom[i];
				} else {					pl_tVal[i] = 10000;}
			}
			double pltVal = Math.min(pl_tVal[0],pl_tVal[1]);
			int idxVis = (pltVal == pl_tVal[0] ? 0 : 1);
			if (pltVal < 0){//if min less than 0 then that means it intersects behind the viewer.  pick other t
				pltVal = pl_tVal[idxVis];
				if (pltVal < epsVal){		planeRes = false;	}//if both t's are less than 0 then didn't hit caps within circular bounds
			}//if the min t val is less than 0		
			double tVal = 0, maxCylT = Math.max(cyltVal, cyltOtr), minCylT = Math.min(cyltVal, cyltOtr);
			if(planeRes && (((minCylT <= 0)					//inside cylinder, 
				&& (pltVal >= -epsVal) && (pltVal <= maxCylT)) || ((pltVal > minCylT) && (pltVal <= maxCylT)))) {		tVal = pltVal;} 
			else {					tVal = cyltVal;					idxVis = 2;	}
			double yInt1 = transRay.origin.y + (tVal * transRay.direction.y);			
			if((yInt1+epsVal >= yBottom ) && (yInt1-epsVal <= yTop)){return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(tVal),new int[]{idxVis},tVal);} 
		}	
		return new rayHit(false);	 
	}//intersectCheck method
  	//returns surface normal of cylinder at given point on cylinder
  	@Override
  	public myVector getNormalAtPoint(myVector pt,  int[] args){
  		myVector result;
  		if(args[0]>= capEqs.length){ 		result = new myVector((pt.x - origin.x), 0, (pt.z - origin.z));	} 
  		else {								result = new myVector(capEqs[args[0]][0],capEqs[args[0]][1],capEqs[args[0]][2]);}
  		result._normalize();
  		if (rFlags[invertedIDX]){result._mult(-1);}
  		return result;
  	}//method getNormalAtPoint  	


  	public String toString(){  
  		String[] eqvals = new String[]{"x + ","y + ","z + ",""};
  		String res = super.toString();
  		res += "\n cap eqs : ";
  		for(int i =0;i<capEqs.length;++i){
  			res +="\n";
  			for(int j=0; j<capEqs[i].length;++j){
  				res+="" + capEqs[i][j]+ eqvals[j];
  			}
  		}
  		return res;
  	}
}//class myCylinder
