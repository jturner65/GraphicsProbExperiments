package base_RayTracer.scene.geometry.base;

import java.util.*;

/**
 * Enum used to specify type of myGeomBase object
 * @author 7strb
 *
 */
public enum GeomObjType {
	None(0),BBox(1),RenderedBBox(2),Instance(3),
	PointLight(4),SpotLight(5),	DiskLight(6),
	Triangle(7),Quad(8),Plane(9),
	Sphere(10),	Cylinder(11),Hollow_Cylinder(12),Torus(13),
	AccelFlatList(14),AccelBVH(15);	
	private int value; 

	private String[] _typeExplanation = new String[] {
			"Non-object or unknown",
			"Bounding Box",
			"Renderable Bounding Box",
			"Instance of a Particular Object",
			"Point Light",
			"Spot Light",
			"Circular Area Light",
			"Triangle Planar Object",
			"Quadrilateral Planar Object",
			"Infinte Implicit Plane",
			"Implicit Sphere",
			"Implicit Capped Cylinder",
			"Implicit Cylindrical Tube",
			"Implicit Torus",
			"Flat List of Objects",
			"Bounding Volume Hierarchy Structure Holding Objects"};
	
	private static String[] _typeName = new String[] {
			"None","Bounding Box","Rendered BBox", "Object Instance",
			"Point Light","Spot Light","Disk Light",
			"Triangle","Quad","Plane","Sphere","Capped Cylinder","Cylindrical Tube","Torus",
			"Flat List","BVH"};
	public static String[] getListOfTypes() {return _typeName;}
	private static Map<Integer, GeomObjType> map = new HashMap<Integer, GeomObjType>(); 
	static { for (GeomObjType enumV : GeomObjType.values()) { map.put(enumV.value, enumV);}}
	private GeomObjType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static GeomObjType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
	public String getName() {return _typeName[value];}
	@Override
    public String toString() { return ""+_typeExplanation[value] + "("+value+")"; }	
    public String toStrBrf() { return ""+_typeExplanation[value]; }	

}//enum GeomObjType