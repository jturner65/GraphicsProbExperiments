package base_RayTracer;

import processing.core.PImage;
import base_Utils_Objects.*;

//scene object described by implicit equations
public abstract class myImpObject extends mySceneObject {
	public double radX, radY, radZ;  

	public myImpObject(myScene _p, double _x, double _y, double _z) {
		super(_p, _x, _y, _z);
	}

	/**
	*  calculate the correct value for the differences between the origin of a ray and whatever the origin for this 
	*  object is. this displacement is used for determining values in intersection equation for sphere.  includes values
	*  for seperate axes radii
	*/	
	public myVector originRadCalc(myRay ray){//need to get ray time value
	    myVector result = new myVector(), _rayOrigin = ray.origin, thisOrigin = getOrigin(ray.getTime());
	    result.set((_rayOrigin.x - thisOrigin.x)/radX, (_rayOrigin.y - thisOrigin.y)/radY, (_rayOrigin.z - thisOrigin.z)/radZ);
	    return result;  
	}//method originRadCalc
	@Override
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){
		double v = findTextureV(isctPt,myTexture,time);	
		return new double[]{findTextureU(isctPt,v,myTexture,time),v};
	}
	protected abstract double findTextureU(myVector isctPt, double v, PImage myTexture, double time);
	protected abstract double findTextureV(myVector isctPt, PImage myTexture, double time);	


}//class myImpObject

class mySphere extends myImpObject{
  
	public mySphere(myScene _p, double _radX, double _radY, double _radZ, double x, double y, double z){
		super(_p, x,y,z);
		type = objType.Sphere;
		radX = _radX;
		radY = _radY;
		radZ = _radZ;
	    minVals = this.getMinVec();
	    maxVals = this.getMaxVec();	    
		postProcBBox();				//cnstrct and define bbox
	}
  
	public mySphere(myScene _p, double myRadius, double x, double y, double z, boolean active){	this(_p, myRadius, myRadius, myRadius,x,y,z);}    
	// calculates the "A" value for the quadratic equation determining the potential intersection of this ray with a sphere of given radius and center
	public double getAVal(myRay ray){return((ray.direction.x/radX) * (ray.direction.x/radX)) + ((ray.direction.y/radY) * (ray.direction.y/radY)) + ((ray.direction.z/radZ) * (ray.direction.z/radZ));}
  	// calculates the "B" value for the quadratic equation determining the potential intersection of this ray with a sphere of given radius and center
	public double getBVal(myRay ray){
		double result = 0.0;
		myVector pC = this.originRadCalc(ray);
		result = 2*(((ray.direction.x/radX) * pC.x) + ((ray.direction.y/radY) * pC.y) + ((ray.direction.z/radZ) * pC.z));
		return result;  
	}  
	// calculates the "C" value for the quadratic equation determining the potential intersection of this ray with a sphere of given radius and center
	public double getCVal(myRay ray){
		double result = 0.0;  
	   //don't need to worry about pC components being negative because of square
		//this value should actually be rayorigin - sphereorigin coords - originRadCalc accounts for radius in each direction
		myVector pC = this.originRadCalc(ray);
		result = (pC.x * pC.x) + (pC.y * pC.y) + (pC.z * pC.z) - 1;    
		return result;
	}  
	// returns surface normal of sphere at given point on sphere
	public myVector getNormalAtPoint(myVector pt, int[] args){
		myVector result = new myVector(pt);
		result._sub(this.origin);
		result._normalize();
		if (rFlags[invertedIDX]){	result._mult(-1.0);   }
    return result;
	}//method getNormalAtPoint	
	//check if the passed ray intersects with this sphere
	public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){
//		if(!_bbox.intersectCheck(ray, _ctAra).isHit){return new rayHit(false);	}
//		myRay transRay = ray.getTransformedRay(ray, _ctAra[invIDX]);
		//boolean result = false;
		double a = this.getAVal(transRay), ta = 2*a, b = this.getBVal(transRay), c = this.getCVal(transRay), discr = ((b*b) - (2*ta*c));
		//quadratic - check first if imaginary - if so then no intersection
		if (!(discr < 0)){
			//result = true;  
			//find values of t - want those that give largest value of z, to indicate the closest intersection to the eye
			double discr1 = Math.pow(discr,.5), t1 = (-1*b + discr1)/(ta), t2 = (-1*b - discr1)/(ta);
			//set the t value of the intersection to be the minimum of these two values (which would be the edge closest to the eye/origin of the ray)
			double tVal = Math.min(t1,t2);
			if (tVal < epsVal){//if min less than 0 then that means it intersects behind the viewer.  pick other t
				tVal = Math.max(t1,t2);
				if (tVal < epsVal){	return new rayHit(false);}		//if still less than 0 then that means both intersections behind viewer - this isn't an intersection, dont draw
			}//if the min t val is less than 0
			return transRay.objHit(this,  _ray.direction,_ctAra, transRay.pointOnRay(tVal),null,tVal); }
		else{			return new rayHit(false);	}    
	}//intersectCheck method
	//find the u (x) value in a texture to plot to a specific point on the sphere
	@Override
	protected double findTextureU(myVector isctPt, double v, PImage myTexture, double time){		
		myVector t_origin = this.getOrigin(time);
		double u = 0.0, q,a0, a1, a2, shWm1 = myTexture.width-1, z1 = (isctPt.z - t_origin.z);	  
		q = v/(myTexture.height-1);//normalize v to be 0-1
		a0 = (isctPt.x - t_origin.x)/ (this.radX);
		a0 = (a0 > 1) ? 1 : (a0 < -1) ? -1 : a0;
		a1 = ( Math.sin(q* Math.PI));
		a2 = ( Math.abs(a1) < epsVal) ? 1 : a0/a1;
		u = (z1 <= epsVal) ? ((shWm1 * ( Math.acos(a2))/ (scene.pa.TWO_PI)) + shWm1/2.0f) : 
					shWm1 - ((shWm1 * ( Math.acos(a2))/ (scene.pa.TWO_PI)) + shWm1/2.0f);
		u = (u < 0) ? 0 : (u > shWm1) ? shWm1 : u;
		return u;	
	}//method findTexture    
        
	// find the v (y) value in a texture to plot to a specific point on the sphere  remember top of texture should correspond to 0, bottom to texture.height.
	@Override
	protected double findTextureV(myVector isctPt, PImage myTexture, double time){
		myVector t_origin = this.getOrigin(time);
		double v = 0.0;
		//double a0 = super.rayIntersectPoint[gen].y - this.origin.y;
		double a0 = isctPt.y - t_origin.y;
		double a1 = a0 /(this.radY);
		a1 = (a1 > 1)? 1 : (a1 < -1) ?  -1 : a1;    
		v = (myTexture.height-1) * Math.acos(a1)/Math.PI;
		return v;
	}//method findTextureV    
	
	@Override
	public myVector getOrigin(double _t){	return origin;	}
	@Override
	public myVector getMaxVec(){
		myVector res = new myVector(origin);
		double tmpVal = radX + radY+radZ;			//L1 norm for enclosing box
		res._add(tmpVal,tmpVal,tmpVal);
		return res;
	}	
	@Override
	public myVector getMinVec(){
		myVector res = new myVector(origin);
		double tmpVal = radX + radY+radZ;			//L1 norm for enclosing box
		res._add(-tmpVal,-tmpVal,-tmpVal);
		return res;
	}
	public String toString(){   return super.toString() + "\n"+ type +"-Specific : Radius : [" + this.radX+ "|" + this.radY+  "|" +this.radZ+"]"; }
}//class mySphere

//sphere that is moving between origin 0 and origin 1
class myMovingSphere extends mySphere{
	public myVector origin0, origin1;				//values of origins for this moving sphere (0 at t==0, 1 at t==1);
	public myMovingSphere(myScene _p, double myRadius, double x0, double y0, double z0, double x1, double y1, double z1, boolean active){	
		super(_p, myRadius,x0,y0,z0,active); 		//force main origin to be 0,0,0
		origin0 = new myVector(origin);
		origin1 = new myVector(x1,y1,z1);
	}
	
	@Override
	public myVector getOrigin(double _t){	return interpVec(origin0, _t, origin1);	}
	
}//myMovingSphere

class myHollow_Cylinder extends myImpObject{
	protected double myHeight, yTop, yBottom;

	public myHollow_Cylinder(myScene _p, double _myRadius, double _myHeight, double x, double y, double z, double xO,double yO, double zO) {
		super(_p, x,y,z);
		type = objType.Hollow_Cylinder;
		radX= _myRadius;
		radZ = _myRadius;
		myHeight = _myHeight;
		yTop = origin.y + myHeight;//top of can
		yBottom = origin.y; //bottom of can
	    minVals = this.getMinVec();
	    maxVals = this.getMaxVec();	    
		postProcBBox();				//cnstrct and define bbox
	}
	 
	//check if passed ray intersects with this cylinder - first using x/z for circular intersection, then planar intersection with end caps, then check which is closest and positive  
	public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){		
		double a = getAVal(transRay), b = getBVal(transRay), c = getCVal(transRay), discr = ((b*b) - (4*a*c));
		//quadratic - check first if imaginary - if so then no intersection
		if (!(discr < 0)){//real roots exist - means ray hits x-z walls somewhere
			double discr1 = Math.pow(discr,.5),t1 = (-b + discr1)/(2*a),t2 = (-b - discr1)/(2*a);
			double cyltVal = Math.min(t1,t2), cyltOtr = Math.max(t1, t2);
			if (cyltVal < -epsVal){//if min less than 0 then that means it intersects behind the viewer.  pick other t
				double tmp = cyltOtr; 	cyltOtr = cyltVal;	cyltVal = tmp;				
				if (cyltVal < -epsVal){		return new rayHit(false);		}//if both t's are less than 0 then don't paint anything
			}//if the min t val is less than 0
			//light very near to bounds of cylinder, need check to avoid shadow shennanigans
			double yInt1 = transRay.origin.y + (cyltVal * transRay.direction.y);
			//in args ara 0 means hitting outside of cylinder, 1 means hitting inside
			if((cyltVal > epsVal) && (yInt1 > yBottom ) && (yInt1 < yTop)){return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(cyltVal),new int[]{0},cyltVal);}
			double yInt2 = transRay.origin.y + (cyltOtr * transRay.direction.y);
			if((cyltOtr > epsVal) && (yInt2 > yBottom ) && (yInt2 < yTop)){return transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(cyltOtr),new int[]{1},cyltOtr);}
		} 
		return new rayHit(false);	    
	}//intersectCheck method

  	//returns surface normal of cylinder at given point on cylinder	
  	@Override
  	public myVector getNormalAtPoint(myVector pt,  int[] args){//in args ara 0 means hitting outside of cylinder, 1 means hitting inside	
  		myVector result= (args[0] == 1) ? new myVector((origin.x - pt.x), 0, (origin.z - pt.z)) : new myVector((pt.x - origin.x), 0, (pt.z - origin.z));
  		result._normalize();
  		if (rFlags[invertedIDX]){result._mult(-1);}
  		//System.out.println("normal :" + result.toStrBrf() + " @ pt :"+ pt.toStrBrf());
  		return result;
  	}//method getNormalAtPoint  		

	//find the u (x) value in a texture to plot to a specific point on the object	TODO
	@Override
	protected double findTextureU(myVector isctPt, double v, PImage myTexture, double time){double u = 0.0;return u;}     	
  	//find the v (y) value in a texture to plot to a specific point on the object  	TODO
	@Override
	protected double findTextureV(myVector isctPt, PImage myTexture, double time){		double v = 0.0; 		return v; 	}   
	
  	public double getMyHeight(){  return myHeight;}  
  	
  	// calculates the "A" value for the quadratic equation determining the potential intersection of the passed ray with a cylinder of given radius and center
  	public double getAVal(myRay _ray){	return ((_ray.direction.x/radX) * (_ray.direction.x/radX)) + ((_ray.direction.z/radZ) * (_ray.direction.z/radZ));  	}  
  	//calculates the "B" value for the quadratic equation determining the potential intersection of the passed ray with a cylinder of given radius and center
  	public double getBVal(myRay _ray){myVector pC = originRadCalc(_ray);return 2*(((_ray.direction.x/radX) * pC.x) + ((_ray.direction.z/radZ) * pC.z)); }  
  	//calculates the "C" value for the quadratic equation determining the potential intersection of the passed ray with a cylinder of given radius and center
  	public double getCVal(myRay _ray){	myVector pC = originRadCalc(_ray);	return (pC.x * pC.x) + (pC.z * pC.z) - 1; }  
  	
	@Override
	public myVector getOrigin(double _t){	return origin;	}
	@Override
	public myVector getMaxVec(){
		myVector res = new myVector(origin);
		double tmpVal = radX+radZ;
		res._add(tmpVal,myHeight,tmpVal);
		return res;
	}	
	@Override
	public myVector getMinVec(){
		myVector res = new myVector(origin);
		double tmpVal = radX+radZ;
		res._add(-tmpVal,0,-tmpVal);
		return res;
	}	
  	public String toString(){  return super.toString() + "\n"+ type +"-Specific : Height : " + myHeight + " radius : [" + radX+ "|" +radZ+"] y End : " + yBottom; 	}	
}//myHollow_Cylinder

class myCylinder extends myHollow_Cylinder{
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

//TODO quartic torus
class myTorus extends myImpObject{
	//bodyRad is radius of entire donut, ringRad is radius of circle making up "arm" of torus
	private double bodyRad, ringRad;

	public myTorus(myScene _p, double _bodyRad, double _ringRad, double _xC, double _yC, double _zC){
		super(_p,_xC,_yC,_zC);
		//calculate torus as a collection of numPolys facets indexed by relative position of center of poly
		//with the intention of making ray collision detection faster 
		type = objType.Torus;
		
		bodyRad = _bodyRad;
		ringRad = _ringRad;
	    minVals = this.getMinVec();
	    maxVals = this.getMaxVec();	    
		postProcBBox();				//cnstrct and define bbox
	}
	
	public myTorus(myScene _p, double primeRad, double secondRad){this(_p,primeRad,secondRad,0,0,0); }

	@Override
	protected double findTextureU(myVector isctPt, double v, PImage myTexture, double time){
		double u = 0.0;
		return u;
	}    
	      
	@Override
	protected double findTextureV(myVector isctPt, PImage myTexture, double time){
		double v = 0.0;
		return v;
	}
	
	@Override
	public myVector getOrigin(double _t){	return origin;	}
	@Override
	public myVector getMaxVec(){
		myVector res = new myVector(origin);
		res._add(bodyRad,bodyRad,bodyRad);
		return res;
	}	
	@Override
	public myVector getMinVec(){
		myVector res = new myVector(origin);
		res._add(-bodyRad,-bodyRad,-bodyRad);
		return res;
	}
	@Override
	public myVector getNormalAtPoint(myVector point, int[] args) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){
//		if(!_bbox.intersectCheck(ray, _ctAra).isHit){return new rayHit(false);	}
//		myRay transRay = ray.getTransformedRay(ray, _ctAra[invIDX]);

		// TODO Auto-generated method stub
		return new rayHit(false);
	}

  
}//myMesh class

