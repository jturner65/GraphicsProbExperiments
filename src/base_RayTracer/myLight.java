package base_RayTracer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

import processing.core.PConstants;
import processing.core.PImage;
import base_Utils_Objects.*;


public abstract class myLight extends mySceneObject{
	public myColor lightColor;            //does not use super's color vals
	public int lightID;
	public myKD_Tree photonTree;
	public myVector orientation;
	
	//TODO light intensity should fall off by inverse sq dist
	
	public myLight(myScene _scn, int _lightID, double _r, double _g, double _b, double _x, double _y, double _z, double _dx, double _dy, double _dz) {
		super(_scn, _x,_y,_z);
	    minVals = this.getMinVec();
	    maxVals = this.getMaxVec();
	    postProcBBox();
	    System.out.println("Making light " + ID);
		rFlags[isLightIDX] = true;
		setVals(_lightID, _r,_g,_b,_x,_y,_z);
	    orientation = new myVector(_dx,_dy,_dz); 
	    orientation._normalize();
	}	
	@Override
	//assumes that transRay dir is toward light.
	public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){  
		myVector hitNorm = new myVector(transRay.direction);
		hitNorm._mult(-1);//norm is just neg ray direction
		hitNorm._normalize();
		double t = transRay.origin._dist(getOrigin(transRay.getTime()));
		rayHit hit = transRay.objHit(this, _ray.direction, _ctAra, transRay.pointOnRay(t), new int[]{}, t);
		//rayHit hit = new rayHit(transRay, _ray.direction, this, _ctAra, hitNorm,transRay.pointOnRay(t),t, new int[]{});		
		return hit;
	}		//point light always intersects

	//TODO textured light could give different color light to scene based on location? BATSIGNAL!
	public double[] findTxtrCoords(myVector isctPt, PImage myTexture, double time){
		double v = findTextureV(isctPt,myTexture,time);	
		return new double[]{findTextureU(isctPt,v,myTexture,time),v};
	}
	protected double findTextureU(myVector isctPt, double v, PImage myTexture, double time){ return 0.0; }   
	protected double findTextureV(myVector isctPt, PImage myTexture, double time){	return 0.0;  } 
	//public boolean intersectCheck(myRay ray){  return true;}
	public void setLightColor(double _r, double _g, double _b){	this.lightColor = new myColor(_r,_g,_b);} 
	//sets color and position of a light
	public void setVals(int lightID, double r, double g, double b, double x, double y, double z){
		this.setLightColor(r,g,b);
		super.origin.set(x,y,z);
		this.lightID = lightID;
	}
	//get a random direction for a photon to travel - from jensen photon mapping
	public myVector getRandDir(){
		double x,y,z, sqmag, mag;
		
		//replace this with better random randomizer
		do{
			x = ThreadLocalRandom.current().nextDouble(-1.0,1.0);
			y = ThreadLocalRandom.current().nextDouble(-1.0,1.0);
			z = ThreadLocalRandom.current().nextDouble(-1.0,1.0);
			sqmag = (x*x) + (y*y) + (z*z);
		}
		while ((sqmag > 1.0) || (sqmag < epsVal));
		mag=Math.sqrt(sqmag);
		myVector res = new myVector(x/mag,y/mag,z/mag);
		//res._normalize();
		return res;
	}
	
	//probability/weighting of angle between inner and outer radii - linear
	protected double getAngleProb(double angle, double innerThetRad, double outerThetRad, double radDiff){return (angle < innerThetRad ) ? 1 : (angle > outerThetRad ) ? 0 : (outerThetRad - angle)/radDiff;}	
	//send direction vector, finds multiplier for penumbra effect
	protected double calcT_Mult(myVector dir, double time, double innerThetRad, double outerThetRad, double radDiff){
		double angle = Math.acos(-1*dir._dot(getOrientation(time)));			//intersection pt to light dir is neg light to intersection pt dir - want acos of this to get angle
		return getAngleProb(angle,innerThetRad,outerThetRad, radDiff);// (angle < innerThetRad ) ? 1 : (angle > outerThetRad ) ? 0 : (outerThetRad - angle)/radDiff;		
	}
	
	//get starting point for photon ray - will vary based on light type
	public abstract myRay genRndPhtnRay();
	@Override
	public myVector getOrigin(double _t){	return origin;	}	
	public myVector getOrientation(double _t){	return orientation;	}

	public String toString(){  return super.toString() + " \ncolor : " + this.lightColor + " light ID : " + this.lightID;  }
}//class myLight

class myPointLight extends myLight{
 
	public myPointLight(myScene _scn, int _lightID, double _r, double _g, double _b, double _x, double _y, double _z){
		super(_scn,_lightID, _r, _g, _b, _x,_y,_z,0,0,0);
		type = objType.PointLight;
	}//myPointLight constructor(7)
 
	@Override//normal is meaningless for pointlight
	public myVector getNormalAtPoint(myVector point, int[] args) {return new myVector(0,1,0);}

	@Override
	public myRay genRndPhtnRay() {
		myVector tmp = getRandDir();
		return new myRay(scene, getTransformedPt(origin, CTMara[glblIDX]), tmp, 0);
	}
	
	@Override
	public myVector getMaxVec(){
		myVector res = new myVector(origin);
		res._add(epsVal,epsVal,epsVal);
		return res;
	}	
	@Override
	public myVector getMinVec(){
		myVector res = new myVector(origin);
		res._add(-epsVal,-epsVal,-epsVal);
		return res;
	}
	public String toString(){  return super.toString() + " Point Light";}
}//class myPointLight

/**spotlight x y z dx dy dz angle_inner angle_outer r g b
Create a spotlight. In addition to the position of the spotlight, 
the command specifies the direction in which the light is pointing and an inner and outer angle. 
If a point is inside the cone of the inner angle, it is fully lit. 
If it is between the inner and outer angle, it is partially lit. 
If it is outside of the outer angle, it is not lit by this light source. 

Note that the angle to a given point can be calculated based on the dot product between the (normalized) spotlight direction and a (normalized) vector from the light to the point in question. 
*/

class mySpotLight extends myLight{
	
	public double innerThet, outerThet, innerThetRad, outerThetRad, radDiff;

	public myVector oPhAxis;	//unit vector ortho to orientation, to use for randomly calculating direction for photon casting
	
	public mySpotLight(myScene _scn, int _lightID, 
			double _r, double _g, double _b, 
			double _x, double _y, double _z, 
			double _dx, double _dy, double _dz, 
			double _inThet, double _outThet) {
		super(_scn, _lightID, _r, _g, _b, _x, _y, _z, _dx, _dy, _dz);
		type = objType.SpotLight;
		setSpotlightVals(_inThet,_outThet);
	}
	
	public void setSpotlightVals(double inAngle, double outAngle){
		innerThet = inAngle;
		innerThetRad = innerThet * PConstants.DEG_TO_RAD;
		outerThet = outAngle;
		outerThetRad = outerThet * PConstants.DEG_TO_RAD;		
		radDiff = outerThetRad - innerThetRad;				//for interpolation 
		oPhAxis = getOrthoVec(orientation);			//for rotation of dir vector for generating photons
	}//setSpotlightVals	
	@Override
	public rayHit intersectCheck(myRay _ray, myRay transRay, myMatrix[] _ctAra){  
		rayHit hit = super.intersectCheck(_ray, transRay, _ctAra);
		hit.ltMult = calcT_Mult(transRay.direction,transRay.getTime(), innerThetRad, outerThetRad, radDiff);
		return hit;
	}
	@Override
	public myRay genRndPhtnRay() {  //diminish power of photon by t value at fringe
		//find random unit vector at some angle from orientation < outerThetRad, scale pwr of photon by t for angle >innerThetRad, <outerThetRad 
		myVector tmp = new myVector();
		double prob, angle;
		
		//as per CLT this should approach gaussian
		double checkProb = ThreadLocalRandom.current().nextDouble(0,1);
		do{//penumbra isn't as likely
			angle = ThreadLocalRandom.current().nextDouble(0,outerThetRad);
			prob = getAngleProb(angle, innerThetRad, outerThetRad, radDiff);			
		//} while (prob > ThreadLocalRandom.current().nextDouble(0,1));
		} while (prob > checkProb);
		
		
		tmp.set(orientation.rotMeAroundAxis(oPhAxis,angle));	
		tmp._normalize();
		//rotate in phi dir for random direction
		tmp = tmp.rotMeAroundAxis(orientation,ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI));
		
		return new myRay(scene, getTransformedPt(origin, CTMara[glblIDX]), tmp, 0);
	}	
	@Override //no need for surface normal of light (??)
	public myVector getNormalAtPoint(myVector point, int[] args) {	return new myVector(0,1,0);	}
	@Override
	public myVector getMaxVec(){
		myVector res = new myVector(origin);
		res._add(epsVal,epsVal,epsVal);
		return res;
	}
	@Override
	public myVector getMinVec(){
		myVector res = new myVector(origin);
		res._add(-epsVal,-epsVal,-epsVal);
		return res;
	}	
	@Override
	public String toString(){  
		String res = super.toString();
		res+= "\nSpotlight : Direction : " + orientation + " inner angle rad : " + innerThetRad + " outer angle rad : " + outerThetRad;
		return res;
	}
}//class mySpotLight
/**
 * Create a disk-shaped light source. The center of the disk is (x, y, z), the radius is rad, and a normal vector to the disk is (dx, dy, dz). 
 * As with point lights, there is also a color associated with the light (r, g, b). Since this light source has a non-zero area, 
 * it sould cast a shadow that is soft on the edges. For each shadow ray that is cast at this light source, you should select a random position on this disk. 
 */
class myDiskLight extends myLight{	
	public double radius;
	public int lightDistType;		//TODO support more than just normal light distribution (i.e. gaussian)
	public myVector surfTangent,	//unit vector tangent to surface of light - randomly rotate around normal and extend from 0->radius to get random position	
					curShadowTarget;		//current target for this light - changes every time shadow ray is sent
	
	public myDiskLight(myScene _scn, int _lightID, 
			double _r, double _g, double _b, 
			double _x, double _y, double _z, 
			double _dx, double _dy, double _dz, 
			double _radius) {
		super(_scn, _lightID, _r, _g, _b, _x, _y, _z, _dx, _dy, _dz);
		type = objType.DiskLight;
		setDisklightVals(_radius);
	}
	//generates a ray to park a photon in the photon map
	@Override
	public myRay genRndPhtnRay() {
		myVector dir = new myVector();
		double prob, angle;
		do{//penumbra isn't as likely
			angle = ThreadLocalRandom.current().nextDouble(0,Math.PI);
			prob = getAngleProb(angle, 0, Math.PI, Math.PI);			
		} while (prob > ThreadLocalRandom.current().nextDouble(0,1));
		dir.set(orientation.rotMeAroundAxis(surfTangent,angle));
		dir._normalize();
		//rotate in phi dir for random direction
		//dir = myVector._rotAroundAxis(dir, orientation,ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI));		
		dir = dir.rotMeAroundAxis(orientation,ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI));		
		myVector loc = getRandomDiskPos();
		return new myRay(scene, getTransformedPt(loc, CTMara[glblIDX]), dir, 0);
	}//genRndPhtnRay
	
	public void setDisklightVals(double _radius){
		radius = _radius;	
		surfTangent = getOrthoVec(orientation);
	}//setSpotlightVals	
	
	//find random position within this light disk to act as target
	//TODO : t is time in ray - use this to determine if this light is moving
	protected myVector getRandomDiskPos(){
		myVector tmp = surfTangent.rotMeAroundAxis(orientation,ThreadLocalRandom.current().nextDouble(0,PConstants.TWO_PI));				//rotate surfTangent by random angle
		tmp._normalize();
		double mult = ThreadLocalRandom.current().nextDouble(0,radius);			//find displacement radius from origin
		tmp._mult(mult);
		tmp._add(origin);																														//find displacement point on origin
		return tmp;
	}			
	@Override //normal is used for illumination of an object, are we going to illuminate/render a light?
	public myVector getNormalAtPoint(myVector point, int[] args) {	return new myVector(0,1,0);	}
	@Override
	public myVector getOrigin(double t) {	
		//do this 2x if light is moving, and interpolate between two values
		curShadowTarget = getRandomDiskPos(); 
		return curShadowTarget;
	}

	//improve these - need disk dimensions, but using sphere is ok for now
	@Override
	public myVector getMaxVec(){myVector res = new myVector(origin);res._add(radius,radius,radius);return res;}	
	@Override
	public myVector getMinVec(){myVector res = new myVector(origin);res._add(-radius,-radius,-radius);return res;}
	@Override
	public String toString(){  return super.toString() + "\nDiskLight : Direction : " + orientation + " radius : " + radius;}	
}//class myDiskLight

//Photon class
class myPhoton implements Comparable<myPhoton>{
//	public myVector dir;				//direction of photon on surface TODO
	public double[] pwr;				//power of light of this photon in r,g,b	
	public double[] pos;  
	private myKD_Tree phKD_Tree;//ref to owning tree
	
	public myPhoton (myKD_Tree _tree, double[] _pwr, double x, double y, double z) {phKD_Tree = _tree;pos = new double[]{x,y,z,0}; pwr = _pwr; } // x,y,z position, plus fourth value that is used for nearest neighbor queries
	//public myPhoton (myKD_Tree _tree, double[] _pwr, myVector pt, myVector _dir){this(_tree, _pwr, pt.x,pt.y,pt.z); dir = new myVector(_dir);}
	// Compare two nodes, used in two different circumstances:
	// 1) for sorting along a given axes during kd-tree construction (sort_axis is 0, 1 or 2)
	// 2) for comparing distances when locating nearby photons (sort_axis is 3)
	public int compareTo(myPhoton other_photon) {	return ((this.pos[phKD_Tree.sort_axis] < other_photon.pos[phKD_Tree.sort_axis]) ? -1 : ((this.pos[phKD_Tree.sort_axis] > other_photon.pos[phKD_Tree.sort_axis]) ? 1 : 0));}

}//Photon class

//One node of a kD-tree
class myKD_Node {
	myPhoton photon;    // one photon is stored at each split node
	int split_axis;   // which axis separates children: 0, 1 or 2 (-1 signals we are at a leaf node)
	myKD_Node left,right;  // child nodes
}

class myKD_Tree {
	public myScene scene;
	public myKD_Node root;  // root node of kd-tree
	public ArrayList<myPhoton> photon_list;  // initial list of photons (empty after building tree)
	public double max_dist2;                // squared maximum distance, for nearest neighbor search (gets propagated and not passed through recursion, so can't be global), 
	public final double _baseMaxDist2;
	public int sort_axis;  // for building the kD-tree
	
	public final int num_Cast, num_Near;		//total # of photons cast from each light, size of neighborhood
	
	// initialize a kd-tree
	public myKD_Tree(myScene _scene, int _numCast, int  _numNear, double _max_Dist) {
		scene = _scene;
		photon_list = new ArrayList<myPhoton>();
		num_Cast = _numCast;
		num_Near = _numNear;
		_baseMaxDist2 = _max_Dist * _max_Dist;
		System.out.println("num near set : " + num_Near + " max_dist sq : " + _baseMaxDist2);
	}

	// add a photon to the kd-tree
	public void add_photon(myPhoton p){photon_list.add (p);}

	// Build the kd-tree.  Should only be called after all of the
	// photons have been added to the initial list of photons.
	public void build_tree() {
		System.out.println("Building a tree with :"+photon_list.size() + " photons");
		root = build_tree (photon_list);	
		System.out.println("Tree Built");
	}

	// helper function to build tree -- should not be called by user
	public myKD_Node build_tree(List<myPhoton> plist) {
		myKD_Node node = new myKD_Node();
		   
		// see if we should make a leaf node
		if (plist.size() == 1) {
			node.photon = plist.get(0);
			node.split_axis = -1;  // signal a leaf node by setting axis to -1
			node.left = node.right = null;
			return (node);
		}		
		// if we get here, we need to decide which axis to split
		double[] mins = new double[]{1e20,1e20,1e20};
		double[] maxs = new double[]{-1e20,-1e20,-1e20};
		
		// now find min and max values for each axis
		for (int i = 0; i < plist.size(); i++) {
			myPhoton p = plist.get(i);
			for (int j = 0; j < 3; j++) {
				if (p.pos[j] < mins[j]) {mins[j] = p.pos[j];}
				if (p.pos[j] > maxs[j]) {maxs[j] = p.pos[j];}
			}
		}
		
		double dx = maxs[0] - mins[0];
		double dy = maxs[1] - mins[1];
		double dz = maxs[2] - mins[2];
		
		// the split axis is the one that is longest
		
		sort_axis = -1;		
		if (dx >= dy && dx >= dz){	  	sort_axis = 0;}
		else if (dy >= dx && dy >= dz){ sort_axis = 1;}
		else if (dz >= dx && dz >= dy){ sort_axis = 2;}
		else {  System.out.println ("cannot deterine sort axis");  System.exit(1);}

		// sort the elements according to the selected axis
		Collections.sort(plist);
		
		// determine the median element and make that this node's photon
		int split_point = plist.size() / 2;
		myPhoton split_photon = plist.get(split_point);
		node.photon = split_photon;
		node.split_axis = sort_axis;
			
		if(split_point == 0){node.left = null;} else {node.left = build_tree (plist.subList(0, split_point));}		
		if(split_point == plist.size()-1){node.right = null;} else {node.right = build_tree (plist.subList(split_point+1,  plist.size()));}
	
		// return the newly created node
		return (node);
	}// build_tree

	// Find the nearby photons to a given location.
	//
	// x,y,z    - given location for finding nearby photons
	// num      - maxium number of photons to find
	// max_dist - maximum distance to search
	// returns a list of nearby photons
	public ArrayList <myPhoton> find_near (double x, double y, double z) {
		max_dist2 = _baseMaxDist2;		//resetting this from constant built in constructor
		// create an empty list of nearest photons
		PriorityQueue<myPhoton> queue = new PriorityQueue<myPhoton>(20,Collections.reverseOrder());  // max queue
		sort_axis = 3;  // sort on distance (stored as the 4th double of a photon)
		// find several of the nearest photons
		double[] pos = new double[]{x,y,z};
		find_near_helper (pos, root, queue);
		
		// move the photons from the queue into the list of nearby photons to return
		ArrayList<myPhoton> near_list = new ArrayList<myPhoton>();
		do {
			near_list.add (queue.poll());
		} while (queue.size() > 0);
		
		return (near_list);
	}//find_near
		
	// help find nearby photons (should not be called by user)
	private void find_near_helper (double[] pos, myKD_Node node, PriorityQueue<myPhoton> queue) {
		myPhoton photon = node.photon;
		
		// maybe recurse
		int axis = node.split_axis;
		if (axis != -1) {  // see if we're at an internal node
			// calculate distance to split plane
			double delta = pos[axis] - photon.pos[axis], delta2 = delta * delta;
			if (delta < 0) {
				if (node.left != null){							find_near_helper (pos, node.left, queue);}
				if (node.right != null && delta2 < max_dist2){    find_near_helper (pos, node.right, queue);}
			} else {
				if (node.right != null){						    find_near_helper (pos, node.right, queue);}
				if (node.left != null && delta2 < max_dist2){       find_near_helper (pos, node.left, queue);}
			}
		}		
		// examine photon stored at this current node
		double dx = pos[0] - photon.pos[0];
		double dy = pos[1] - photon.pos[1];
		double dz = pos[2] - photon.pos[2];
		double len2 = dx*dx + dy*dy + dz*dz;		//sq dist from query position
	
		if (len2 < max_dist2) {
			// store distance squared in 4th double of a photon (for comparing distances)
			photon.pos[3] = len2;
			// add photon to the priority queue
			queue.add (photon);
			// keep the queue short
			if (queue.size() > num_Near){	queue.poll();  }// delete the most distant photon
			// shrink max_dist2 if our queue is full and we've got a photon with a smaller distance
			if (queue.size() == num_Near) {
				myPhoton near_photon = queue.peek();
				if (near_photon.pos[3] < max_dist2) {
					max_dist2 = near_photon.pos[3];
				}
			}
		}//if len2<maxdist2
	}//find_near_helper
}//myKD_Tree

