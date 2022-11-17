package base_RayTracer.scene.photonMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public abstract class KDTree <T extends Base_KDObject<T>> {
	/**
	 * root node of kd-tree
	 */
	protected KDNode<T> root; 
	
	/**
	 * initial list of objects to be placed into tree structure (empty after building tree) 
	 */
	protected ArrayList<T> nodeObjectList; 

	/**
	 * axis/index in Base_KDObject to partition along for building the kD-tree
	 */
	protected int sortAxis;  
	
	/**
	 * This is set so that each time neighbor hoods are searched the size can be reset.
	 */
	private final double maxNeighborSqDist;
	
	/**
	 * Max Sq Distance to be considered within a neighborhood; MaxDouble for maxNumNeighbors closest
	 */
	protected double tempMaxNeighborSqDist;
	
	/**
	 * Maximum # of neighbors to be considered in the neighborhood; MaxInt for all within distance
	 */
	protected int maxNumNeighbors;
	
	public KDTree(int _maxNumNeighbors, double _maxNeighborSqDist) {
		root = null;
		nodeObjectList = new ArrayList<T>();
		maxNumNeighbors = _maxNumNeighbors;
		tempMaxNeighborSqDist = _maxNeighborSqDist;
		maxNeighborSqDist = _maxNeighborSqDist;
		System.out.println("num near set : " + maxNumNeighbors + " max_dist sq : " + maxNeighborSqDist);
	}
	

	/**
	 * add a KD Object to the KD-tree
	 * @param p Object to add
	 */
	public void addKDObject(T p){nodeObjectList.add(p);}
	
	/**
	 * Build the kd-tree.  Should only be called after all of the objects have been added to the initial list.
	 */
	public void buildKDTree() {		
		System.out.println("Building a tree with :"+nodeObjectList.size() + " photons");
		root = buildKDTree(nodeObjectList);		}
		
	/**
	 * helper function to build tree -- should not be called by user
	 * @param objList
	 * @return
	 */	
	private KDNode<T> buildKDTree(List<T> objList) {
		KDNode<T> node = new KDNode<T>();
		  
		T p = objList.get(0);
		// see if we should make a leaf node
		if (objList.size() == 1) {
			node.setNodeObject(p);
			return node;
		}		
		
		sortAxis = -1;			
		// if we get here, we need to decide which axis to split
		// the split axis is the one that is longest
		// (ignoring the last value in T's posVals array, which is sqDist)
		int numPosVals = p.getNumPosVals();
		//set first value in list as having both min and max
		double[] mins = new double[]{1e20,1e20,1e20};
		double[] maxs = new double[]{-1e20,-1e20,-1e20};	
		
		// now find min and max values for each axis for rest of objects in list
		for (int i = 0; i < objList.size(); i++) {
			p = objList.get(i);
			for (int j = 0; j < numPosVals; j++) {
				if (p.posVals[j] < mins[j]) {mins[j] = p.posVals[j];}
				if (p.posVals[j] > maxs[j]) {maxs[j] = p.posVals[j];}
			}
		}


		double dx = maxs[0] - mins[0];
		double dy = maxs[1] - mins[1];
		double dz = maxs[2] - mins[2];
		
	
		if (dx >= dy && dx >= dz){	  	sortAxis = 0;}
		else if (dy >= dx && dy >= dz){ sortAxis = 1;}
		else if (dz >= dx && dz >= dy){ sortAxis = 2;}
		else {  System.out.println ("KDTree::buildKDTree : Fatal Error : Cannot deterine sort axis");  System.exit(1);}

		// sort the elements in list according to the selected axis
		Collections.sort(objList);
		
		// determine the median element and make that this node's photon
		int splitPoint = objList.size() / 2;
		T obj = objList.get(splitPoint);
		node.setNodeObject(obj);
		node.splitAxis = sortAxis;
			
		if(splitPoint == 0){node.left = null;} else {node.left = buildKDTree(objList.subList(0, splitPoint));}		
		if(splitPoint == objList.size()-1){node.right = null;} else {node.right = buildKDTree (objList.subList(splitPoint+1,  objList.size()));}
	
		// return the newly created node
		return (node);
	}// buildKDTree
	
	/**
	 * Find the nearby objects to the given location.
	 * @param x given location for finding nearby photons
	 * @param y given location for finding nearby photons
	 * @param z given location for finding nearby photons
	 * @return
	 */
	public T[] findNeighborhood (double x, double y, double z) {
		//reset from last run of find findNeighborhood
		tempMaxNeighborSqDist = maxNeighborSqDist;	
		// create an empty list of nearest photons
		PriorityQueue<T> queue = new PriorityQueue<T>(maxNumNeighbors, Collections.reverseOrder());  // max queue
		sortAxis = 3;  // sort on sq distance (stored as the last idx of the )
		// find several of the nearest photons
		double[] pos = new double[]{x,y,z};
		findNearbyNodes (pos, root, queue);		
		// move the objects from the queue into the list of nearby objects to return
		return getQueueAsArray(queue);
	}//find_near
	
	/**
	 * Because of Java's type erasure we cannot convert the queue to an array of T, so have instancing class do the conversion
	 * @param queue
	 * @return
	 */
	protected abstract T[] getQueueAsArray(PriorityQueue<T> queue);
		
	/**
	 * help find nearby photons (should not be called by user)
	 * @param pos
	 * @param node
	 * @param queue
	 * @param numNear
	 * @param maxDistSq
	 */
	protected void findNearbyNodes (double[] pos, KDNode<T> node, PriorityQueue<T> queue) {
		T nodeObj = node.getNodeObject();
		// examine photon stored at this current node
		double dx = pos[0] - nodeObj.posVals[0];
		double dy = pos[1] - nodeObj.posVals[1];
		double dz = pos[2] - nodeObj.posVals[2];
		double len2 = dx*dx + dy*dy + dz*dz;		//sq dist from query position
	
		if (len2 < tempMaxNeighborSqDist) {
			// store distance squared in 4th double of a photon (for comparing distances)
			nodeObj.setSqDist(len2);
			// add photon to the priority queue
			queue.add(nodeObj);
			// keep the queue within desired range
			if (queue.size() > maxNumNeighbors){	queue.poll();  }// delete the most distant element(the first)
			// shrink maxNeighborSqDist if our queue is full and the farthest photon is closer
			if (queue.size() == maxNumNeighbors) {
				//first object is most distant
				double furthestDist = queue.peek().getSqDist();				
				if (furthestDist < tempMaxNeighborSqDist) {
					tempMaxNeighborSqDist = furthestDist;
				}
			}
		}//if len2<maxNeighborSqDist
		
		int axis = node.splitAxis;
		if (axis != -1) {  // make sure we're not at a leaf
			// calculate distance to split plane
			double delta = pos[axis] - nodeObj.posVals[axis], delta2 = delta * delta;
			if (delta < 0) {
				if (node.left != null){							findNearbyNodes (pos, node.left, queue);}
				if (node.right != null && delta2 < tempMaxNeighborSqDist){    findNearbyNodes (pos, node.right, queue);}
			} else {
				if (node.right != null){						    findNearbyNodes (pos, node.right, queue);}
				if (node.left != null && delta2 < tempMaxNeighborSqDist){       findNearbyNodes (pos, node.left, queue);}
			}
		}		
	}//findNearbyPhotons
	
	//////////////////////////////
	// Getters and setters
	public int getSortAxis() {					return sortAxis;	}
	public void setSortAxis(int _sortAxis) {	sortAxis = _sortAxis;}
	
	public int getMaxNumNeighbors() {						return maxNumNeighbors;	}
	public void setMaxNumNeighbors(int _maxNumNeighbors) {	maxNumNeighbors = _maxNumNeighbors;}
	
	
}//class KDTree
