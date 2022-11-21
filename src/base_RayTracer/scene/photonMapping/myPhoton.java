package base_RayTracer.scene.photonMapping;

import base_Utils_Objects.kdTree.KDTree;
import base_Utils_Objects.kdTree.base.Base_KDObject;

/**
 * Photon class, holds photon power.  Placed in KDNode upon construction of KDTree
 * @author 7strb
 *
 */
public class myPhoton extends Base_KDObject<myPhoton>{
//	public myVector dir;				//direction of photon on surface TODO
	public double[] pwr;				//power of light of this photon in r,g,b	
	
	public myPhoton (KDTree<myPhoton> _tree, double[] _pwr, double x, double y, double z) {
		super(_tree, new double[]{x,y,z});
		pwr = _pwr; 
	}
}//Photon class

