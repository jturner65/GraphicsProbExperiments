package base_RayTracer.scene.photonMapping;

import base_Utils_Objects.kdTree.KDTree;

/**
 * Specialization of Base_KDTree to provide support for photon map, holds target number of cast photons
 * @author 7strb
 *
 */
public class Photon_KDTree extends KDTree<myPhoton>{
	/**
	 * Target number of photons to cast into scene
	 */
	public final int numCast;
	public Photon_KDTree(int _numCast, int _maxNumNeighbors, double _maxNeighborSqDist) {
		super(_maxNumNeighbors, _maxNeighborSqDist);
		numCast = _numCast;
	}
	
}//Photon_KDTree

