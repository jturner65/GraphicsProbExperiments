package base_RayTracer.scene.photonMapping;

public class KDNode <T extends Base_KDObject<T>> {
	protected T nodeObject;
	protected int splitAxis;   		// which axis separates children: 0, 1 or 2 (-1 signals we are at a leaf node)
	public KDNode<T> left,right;  	// child nodes

	public KDNode() {
		splitAxis = -1;
		left = null;
		right = null;
	}
	
	public T getNodeObject() {	return nodeObject;}
	public void setNodeObject(T _nodeObject) {	nodeObject = _nodeObject;}
	
	public int getSplitAxis() {return splitAxis;}
	public void setSplitAxis(int _splitAxis) {	splitAxis = _splitAxis;}

}//class Base_KDNode
