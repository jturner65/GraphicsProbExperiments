package base_RayTracer;

import java.util.*;

//type of myGeomBase object
public enum objType {
	None(0),
	BBox(1),
	RenderedBBox(2),
	Instance(3),
	PointLight(4),
	SpotLight(5),
	DiskLight(6),
	Triangle(7),
	Quad(8),
	Plane(9),
	Sphere(10),
	Cylinder(11),
	Hollow_Cylinder(12),
	Torus(13),
	AccelFlatList(14),
	AccelBVH(15);	
	private int value; 
	private static Map<Integer, objType> map = new HashMap<Integer, objType>(); 
	static { for (objType enumV : objType.values()) { map.put(enumV.value, enumV);}}
	private objType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static objType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
}//enum objType