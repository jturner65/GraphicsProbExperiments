package graphProbExp_PKG;

import java.util.*;

/**
 * this class will hold a roster - a collection of students for a specific class, and will manage working with grades
 * @author john
 *
 */

public class myClassRoster implements Comparable<myClassRoster>{
	public final int ObjID;
	private static int IDCnt = 0;
	//class name
	public final String name;
	//structure holding references to the students in this class - students need to be created external to class and added
	private TreeMap<String, myStudent> students;

	//location and dimenions of raw line on display representing this class
	

	
	
	public myClassRoster(String _name) {
		ObjID = IDCnt++;  name=_name;
		students = new TreeMap<String, myStudent>();
	}//ctor
	
	public void clearClass() {
		students.clear();
	}

	public void addStudent(myStudent _st) {
		
	}
	
	public void drawStudentGradesRaw(GraphProbExpMain pa) {
		pa.pushMatrix();pa.pushStyle();


		
		
		pa.popStyle();pa.popMatrix();					
	}//drawStudentGradesRaw
	
	public void drawStudentGradesTransformed(GraphProbExpMain pa,  String _type) {
		pa.pushMatrix();pa.pushStyle();


		
		
		pa.popStyle();pa.popMatrix();					
	}//drawStudentGradesRaw
	
	//incase we wish to store class rosters in sorted mechanism
	@Override
	public int compareTo(myClassRoster othr) {
		int res = this.name.toLowerCase().compareTo(othr.name.toLowerCase());
		return (res == 0 ? Integer.compare(ObjID, othr.ObjID) : res);
	}//compareTo

	@Override
	public String toString() {
		String res = "Class Name : " + name;
		
		return res;
	}

}//classRoster


/**
 * this class will hold an instance of a student, who belongs to numerous class rosters, and has grades in each. 
 * This class will record their grade values and map them to a line
 * @author john
 *
 */
class myStudent implements Comparable<myStudent>{
	public final int ObjID;
	private static int IDCnt = 0;
	
	public final String name;
	//raw grade value for student for each class - to be transformed - should be between 0 and 1
	private HashMap<myClassRoster, Double> rawGrade;
	//listing of various transformations of student's grade - should be between 0 and 1
	private HashMap<myClassRoster, HashMap<String, Double>> transformedGrade;
	
	//used these for rendering student on line
	private static float rad = 4.0f;
	private static int det = 4;
	//color to render student
	private final int[] clr;
	
	public myStudent(GraphProbExpMain pa, String _name) {
		ObjID = IDCnt++;  name=_name;
		transformedGrade = new HashMap<myClassRoster, HashMap<String, Double>>();
		rawGrade = new HashMap<myClassRoster, Double>();
		clr = pa.getRndClr2(255);//should be brighter colors
	}//ctor
	
	//clip to be within 0->1 - should always be within this range
	private double clipGrade(String _type, double _gr) {
		//_type present for dbg messages only - add any debugging display code here if desired
		if(_gr > 1) {			return 1;} 
		else if (_gr < 0 ) {	return 0;}
		return _gr;
	}//clipGrade
		
	public void setRawGrade(myClassRoster _class, double _gr) {rawGrade.put(_class,clipGrade("raw",_gr)); }
	public double getRawGrade(myClassRoster _class) {
		Double gr = rawGrade.get(_class);
		if(gr == null) {			//student has no raw grade for this class, default to 0	; dbg display code in here if desired
			return 0.0;
		}
		return gr;
	}//getRawGrade
	
	//set transformed values for student, keyed by type of transformation and class
	public void setTransformedGrade(String _type, myClassRoster _class, double _gr) {	
		HashMap<String, Double> typeValsForClass =  transformedGrade.get(_class);			//all student's grades for particular type
		if (typeValsForClass==null) {// no values for this class set yet - build new map of types for this class
			typeValsForClass = new HashMap<String, Double>(); 
			transformedGrade.put(_class,typeValsForClass);
		}		
		typeValsForClass.put(_type,clipGrade(_type,_gr));
	}//setTransformedGrade
	
	public double getTransformedGrade(String _type,myClassRoster _class) {
		HashMap<String, Double> typeValsForClass =  transformedGrade.get(_class);			//all student's grades for particular type
		if (typeValsForClass==null) {						// no values for this class set yet  - return 0
			
			return 0.0;
		}
		Double val = typeValsForClass.get(_type);			//grade for class for specified type
		if (val==null) {			//student has no transformed grade for this class, default to 0	- db message here if desired	
			
			return 0.0;
		}
		return val;
	}//getTransformedGrade


	//////////////////
	// draw this student on line of width ttlWidth - expected to be at start of line when this is called
	private void _drawMe(GraphProbExpMain pa, float gr, float ttlWidth) {
		pa.pushMatrix();pa.pushStyle();
		pa.translate(ttlWidth*gr, 0.0f, 0.0f);
		//showNoBox_ClrAra(myPointf P, float rad, int det, int[] fclr, int[] strkclr, int tclr, String txt)
		pa.showNoBox_ClrAra(myPointf.ZEROPT, rad, det, clr,clr, pa.gui_White,  ""+ String.format("%.4f",gr));
		pa.popStyle();pa.popMatrix();		
		
	}//_drawMe
	
	public void drawMeRaw(GraphProbExpMain pa, myClassRoster _class, float ttlWidth) {	
		Double gr = rawGrade.get(_class);
		if(gr == null) {//student has no grade for this class, debug message	
			return;
		}
		_drawMe(pa, gr.floatValue(), ttlWidth);		
	}//drawMeRaw
	
	public void drawMeTransformed(GraphProbExpMain pa, String _type, myClassRoster _class, float ttlWidth) {	
		HashMap<String, Double> typeValsForClass =  transformedGrade.get(_class);			//all student's grades for particular type
		if (typeValsForClass==null) {// no values for this class set yet - dbg message here
			
			return;
		}
		Double val = typeValsForClass.get(_type);			//grade for class for specified type
		if (val==null) {// no values set for this type - dbg message here
			
			return;
		}
		_drawMe(pa, val.floatValue(), ttlWidth);
	}
	//compare based on name first, then sort by ObjID
	@Override
	public int compareTo(myStudent othr) {
		int res = this.name.toLowerCase().compareTo(othr.name.toLowerCase());
		return (res == 0 ? Integer.compare(ObjID, othr.ObjID) : res);
	}//compareTo
	
	
	@Override
	public String toString() {
		String res = "Name : " + name;
		
		return res;
	}
	
}//myStudent