package base_RayTracer.scene.photonMapping;

import java.util.PriorityQueue;
public class Photon_KDTree extends KDTree<myPhoton>{
	public final int numCast;
	public Photon_KDTree(int _numCast, int _maxNumNeighbors, double _maxNeighborSqDist) {
		super(_maxNumNeighbors, _maxNeighborSqDist);
		numCast = _numCast;
	}
	@Override
	protected myPhoton[] getQueueAsArray(PriorityQueue<myPhoton> queue) {
		myPhoton[] nearList = new myPhoton[queue.size()];
		//if(queue.size() == 0) {return nearList;}
//		for (int i=0;i<nearList.length;++i) {
//			nearList[i]=queue.poll();
//		}
		int i=0;
		while (queue.size() > 0){
			nearList[i++] = queue.poll();
		}
		
		return nearList;
	}
	
}//Photon_KDTree


//
////used explicitly for photon mapping
//public class Photon_KDTree {
//	protected KDNode root;  // root node of kd-tree
//	protected ArrayList<myPhoton> nodeObjectList;  // initial list of photons (empty after building tree)
//	protected double maxDistSq;                // squared maximum distance, for nearest neighbor search (gets propagated and not passed through recursion, so can't be global), 
//	protected final double _baseMaxDistSq;
//	protected int sort_axis;  // for building the kD-tree
//	
//	public final int numCast, numNear;		//total # of photons cast from each light, size of neighborhood
//	
//	// initialize a kd-tree
//	public Photon_KDTree(//myScene _scene, 
//			int _numCast, int  _numNear, double _maxSqDist) {
//		//scene = _scene;
//		nodeObjectList = new ArrayList<myPhoton>();
//		numCast = _numCast;
//		numNear = _numNear;
//		_baseMaxDistSq = _maxSqDist * _maxSqDist;
//		System.out.println("# near set : " + numNear + " maxDist Sq : " + _baseMaxDistSq);
//	}
//
//	// add a photon to the kd-tree
//	public void addKDObject(myPhoton p){nodeObjectList.add (p);}
//
//	/**
//	 * Build the kd-tree.  Should only be called after all of the photons have been added to the initial list of photons.
//	 */
//	public void buildKDTree() {
//		System.out.println("Building a tree with :"+nodeObjectList.size() + " photons.");
//		root = buildKDTree (nodeObjectList);	
//		System.out.println("Tree Built");
//	}
//
//	/**
//	 * helper function to build tree -- should not be called by user
//	 * @param plist
//	 * @return
//	 */
//	private KDNode buildKDTree(List<myPhoton> plist) {
//		Photon_KDNode node = new Photon_KDNode();
//		   
//		// see if we should make a leaf node
//		if (plist.size() == 1) {
//			node.photon = plist.get(0);
//			node.splitAxis = -1;  // signal a leaf node by setting axis to -1
//			node.left = node.right = null;
//			return node;
//		}		
//		// if we get here, we need to decide which axis to split
//		double[] mins = new double[]{1e20,1e20,1e20};
//		double[] maxs = new double[]{-1e20,-1e20,-1e20};
//		
//		// now find min and max values for each axis
//		for (int i = 0; i < plist.size(); i++) {
//			myPhoton p = plist.get(i);
//			for (int j = 0; j < 3; j++) {
//				if (p.pos[j] < mins[j]) {mins[j] = p.pos[j];}
//				if (p.pos[j] > maxs[j]) {maxs[j] = p.pos[j];}
//			}
//		}
//		
//		double dx = maxs[0] - mins[0];
//		double dy = maxs[1] - mins[1];
//		double dz = maxs[2] - mins[2];
//		
//		// the split axis is the one that is longest
//		
//		sort_axis = -1;		
//		if (dx >= dy && dx >= dz){	  	sort_axis = 0;}
//		else if (dy >= dx && dy >= dz){ sort_axis = 1;}
//		else if (dz >= dx && dz >= dy){ sort_axis = 2;}
//		else {  System.out.println ("cannot deterine sort axis");  System.exit(1);}
//
//		// sort the elements according to the selected axis
//		Collections.sort(plist);
//		
//		// determine the median element and make that this node's photon
//		int split_point = plist.size() / 2;
//		node.photon = plist.get(split_point);
//		node.splitAxis = sort_axis;
//			
//		if(split_point == 0){node.left = null;} else {node.left = buildKDTree (plist.subList(0, split_point));}		
//		if(split_point == plist.size()-1){node.right = null;} else {node.right = buildKDTree (plist.subList(split_point+1,  plist.size()));}
//	
//		// return the newly created node
//		return (node);
//	}// buildKDTree
//
//	/**
//	 * Find the nearby photons to a given location. x,y,z    - given location for finding nearby photons num
//	 * @param x given location for finding nearby photons
//	 * @param y given location for finding nearby photons
//	 * @param z given location for finding nearby photons
//	 * @return
//	 */
//	public ArrayList <myPhoton> findNeighborhood (double x, double y, double z) {
//		maxDistSq = _baseMaxDistSq;		//resetting this from constant built in constructor
//		// create an empty list of nearest photons
//		PriorityQueue<myPhoton> queue = new PriorityQueue<myPhoton>(20,Collections.reverseOrder());  // max queue
//		sort_axis = 3;  // sort on sq distance (stored as the 4th double of a photon)
//		// find several of the nearest photons
//		double[] pos = new double[]{x,y,z};
//		findNearbyNodes (pos, root, queue, numNear, maxDistSq);
//		
//		// move the photons from the queue into the list of nearby photons to return
//		ArrayList<myPhoton> near_list = new ArrayList<myPhoton>();
//		do {
//			near_list.add (queue.poll());
//		} while (queue.size() > 0);
//		
//		return near_list;
//	}//find_near
//		
//	/**
//	 * help find nearby photons (should not be called by user)
//	 * @param pos
//	 * @param node
//	 * @param queue
//	 * @param numNear
//	 * @param maxDistSq
//	 */
//	private void findNearbyNodes (double[] pos, KDNode node, PriorityQueue<myPhoton> queue, int numNear, double maxDistSq) {
//		myPhoton photon = node.photon;
//		
//		// maybe recurse
//		int axis = node.splitAxis;
//		if (axis != -1) {  // see if we're at an internal node
//			// calculate distance to split plane
//			double delta = pos[axis] - photon.pos[axis], delta2 = delta * delta;
//			if (delta < 0) {
//				if (node.left != null){							findNearbyNodes (pos, node.left, queue, numNear, maxDistSq);}
//				if (node.right != null && delta2 < maxDistSq){    findNearbyNodes (pos, node.right, queue, numNear, maxDistSq);}
//			} else {
//				if (node.right != null){						    findNearbyNodes (pos, node.right, queue, numNear, maxDistSq);}
//				if (node.left != null && delta2 < maxDistSq){       findNearbyNodes (pos, node.left, queue, numNear, maxDistSq);}
//			}
//		}		
//		// examine photon stored at this current node
//		double dx = pos[0] - photon.pos[0];
//		double dy = pos[1] - photon.pos[1];
//		double dz = pos[2] - photon.pos[2];
//		double len2 = dx*dx + dy*dy + dz*dz;		//sq dist from query position
//	
//		if (len2 < maxDistSq) {
//			// store distance squared in 4th double of a photon (for comparing distances)
//			photon.pos[3] = len2;
//			// add photon to the priority queue
//			queue.add (photon);
//			// keep the queue short
//			if (queue.size() > numNear){	queue.poll();  }// delete the most distant photon
//			// shrink maxSqDist2 if our queue is full and we've got a photon with a smaller distance
//			if (queue.size() == numNear) {
//				myPhoton near_photon = queue.peek();
//				if (near_photon.pos[3] < maxDistSq) {
//					maxDistSq = near_photon.pos[3];
//				}
//			}
//		}//if len2<maxdist2
//	}//findNearbyPhotons
//}//myKD_Tree
//
