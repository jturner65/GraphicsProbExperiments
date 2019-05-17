package base_RayTracer.scene.geometry.accelStruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import base_RayTracer.scene.myScene;
import base_RayTracer.scene.geometry.sceneObjects.lights.myPhoton;

//used explicitly for photon mapping
public class myKD_Tree {
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

//One node of a kD-tree
class myKD_Node {
	myPhoton photon;    // one photon is stored at each split node
	int split_axis;   // which axis separates children: 0, 1 or 2 (-1 signals we are at a leaf node)
	myKD_Node left,right;  // child nodes
}

