package base_RayTracer.scene.geometry.accelStruct.base;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum defining the types of Acceleration Structure objects. Primarily BVH and Flat list now.
 * @author 7strb
 *
 */
public enum AccelStructType {	
	Unknown(0),
	FlatList(1),
	BVHTree(2),
	BVHLeftChild(3),
	BVHRightChild(4),
	BVHLeafList(5);
	private int value;
	
	private String[] _typeExplanation = new String[] {
		"Unknown Acceleration Structure",
		"Flat List of Objects",
		"Bounding Volume Hierarchy Tree",
		"Bounding Volume Hierarchy Left Child",
		"Bounding Volume Hierarchy Right Child",
		"Bounding Volume Hierarchy Leaf List"			
	};
	private static String[] _typeName = new String[] {
		"Unknown","Flat Object List","BVH Tree","BVH Left Child","BVH Right Child","BVH Leaf List"		
	};
	
	public static String[] getListOfTypes() {return _typeName;}
	private static Map<Integer, AccelStructType> map = new HashMap<Integer, AccelStructType>(); 
	static { for (AccelStructType enumV : AccelStructType.values()) { map.put(enumV.value, enumV);}}
	private AccelStructType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static AccelStructType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
	public String getName() {return _typeName[value];}
	@Override
    public String toString() { return ""+value + ":"+_typeExplanation[value]; }
	
}//enum AccelStructType
