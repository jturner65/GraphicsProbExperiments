package base_RayTracer.scene.photonMapping;

import java.util.PriorityQueue;

import base_Utils_Objects.kdTree.base.Base_KDTree;
public class Photon_KDTree extends Base_KDTree<myPhoton>{
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

