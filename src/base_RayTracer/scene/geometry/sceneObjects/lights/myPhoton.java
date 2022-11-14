package base_RayTracer.scene.geometry.sceneObjects.lights;

import base_RayTracer.scene.geometry.accelStruct.myKD_Tree;

/**
 * Photon class
 * @author 7strb
 *
 */
public class myPhoton implements Comparable<myPhoton>{
//	public myVector dir;				//direction of photon on surface TODO
	public double[] pwr;				//power of light of this photon in r,g,b	
	public double[] pos;  
	private myKD_Tree phKD_Tree;//ref to owning tree
	
	public myPhoton (myKD_Tree _tree, double[] _pwr, double x, double y, double z) {phKD_Tree = _tree;pos = new double[]{x,y,z,0}; pwr = _pwr; } // x,y,z position, plus fourth value that is used for nearest neighbor queries
	//public myPhoton (myKD_Tree _tree, double[] _pwr, myVector pt, myVector _dir){this(_tree, _pwr, pt.x,pt.y,pt.z); dir = new myVector(_dir);}
	// Compare two photons, used in two different circumstances:
	// 1) for sorting along a given axes during kd-tree construction (sort_axis is 0, 1 or 2)
	// 2) for comparing distances when locating nearby photons (sort_axis is 3)
	public int compareTo(myPhoton other_photon) {	return ((this.pos[phKD_Tree.sort_axis] < other_photon.pos[phKD_Tree.sort_axis]) ? -1 : ((this.pos[phKD_Tree.sort_axis] > other_photon.pos[phKD_Tree.sort_axis]) ? 1 : 0));}

}//Photon class

