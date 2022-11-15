package base_RayTracer.scene.photonMapping;

/**
 * This class defines an object held in the node of a KD tree, as well as the comparison function
 * @author 7strb
 *
 */
public abstract class Base_KDObject <T extends Base_KDObject<T>> implements Comparable<Base_KDObject<T>> {
	/**
	 * Values to partition for KD Tree
	 */
	protected double[] posVals;
	/**
	 * Tree that owns the hierarchy this object will be placed within+
	 */
	protected KDTree<T> owningTree;
	/**
	 * Number of posVals used to determine partitioning when building tree
	 */
	protected final int numPosVals;
	
	public Base_KDObject(KDTree<T> _owningTree, double[] _locVals) {
		owningTree = _owningTree;
		numPosVals = _locVals.length;
		//add extra spot for sq distance component comparison, to derive neighborhoods
		posVals = new double[_locVals.length + 1];
		System.arraycopy(_locVals, 0, posVals, 0, _locVals.length);
	}

	
	/**
	 * Get value for dist entry (last value)
	 */
	public double getSqDist() {return posVals[numPosVals];}
	public void setSqDist(double _sqDist) {posVals[numPosVals] = _sqDist;}
	/**
	 * Get number of values used to partition tree
	 * @return
	 */
	public int getNumPosVals() {return numPosVals;}
	

	/**
	 * Compare two objects, used in two different circumstances with KD-Tree 
	 *    1) for sorting along a given axis during kd-tree construction (sortAxis is [0..posVals.length-2]) 
	 *    2) for comparing distances when locating nearby objects (sortAxis is posVals.length -1)
	 */
	@Override
	public final int compareTo(Base_KDObject<T> o) {		
		return ((posVals[owningTree.sortAxis] < o.posVals[owningTree.sortAxis]) ? 
				-1 : 
					((posVals[owningTree.sortAxis] > o.posVals[owningTree.sortAxis]) ? 
							1 : 0));}

}//class Base_KDObject
