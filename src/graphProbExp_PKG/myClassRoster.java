package graphProbExp_PKG;

import java.util.*;

/**
 * this class will hold a roster - a collection of students for a specific class, and will manage working with grades
 * it will consist of a collection of "grade bars" that will be used to display the grades of the class on a line,
 * @author john
 */
public class myClassRoster implements Comparable<myClassRoster>{
	public static GraphProbExpMain pa;
	//experiment owning this class
	public static ClassGradeExperiment gradeExp;
	public final int ObjID;
	private static int IDCnt = 0;
	//class name
	public final String name;
	//structure holding references to the students in this class - students need to be created external to class and added
	protected HashMap<Integer,myStudent> students;

	///////////////////////////
	//rendering 
	//distance between adjacent classes
	private final float distBtwnClassBars;
	//distance between the raw and transformed bar for this class
	private final float distBtwnRawTransBars;	
	//3 class bars - original distribution (raw), uniform, and final distribution (transformed)
	private gradeBar rawGradeBar, transGradeBar;
	//type of transformation for transformed grade bar
	private String transformType;
	//types of transforms that have been calculated for this class
	private HashSet<String> transPerformed;
	//rand gen used to perform mapping
	private myRandGen randGen;
	
	//this class's bar color
	private final int[] clsLineClr;
	
	//state flags - bits in array holding relevant info about this random variable function
	private int[] stFlags;						
	public static final int
			debugIDX 						= 0,
			classRawToTransEnabledIDX		= 1,			//if class's raw grades can/should be mapped to transformed grades
			classRawIsTransformedIDX		= 2,			//if raw grades have been transformed
			classTransUsedInFinalGradeIDX	= 3,			//if class's transformed grades are used in final grade
			drawLineBetweenGradesIDX		= 4;			//draw a line connecting students between grade types
	public static final int numFlags 		= 5;
	
	public myClassRoster(GraphProbExpMain _pa, ClassGradeExperiment _gradeExp, String _name, float[][] _barLocs) {
		pa =_pa;gradeExp=_gradeExp;
		ObjID = IDCnt++;  name=_name;
		initFlags();
		//visualization stuff
		distBtwnClassBars = ClassGradeExperiment.distBtwnAdjBars;
		distBtwnRawTransBars =  ClassGradeExperiment.distBtwnRawTransBars;
		clsLineClr = pa.getRndClr2(255);//should be brighter colors
		transformType = "unk";		
		rawGradeBar = new gradeBar(this, new float[] {_barLocs[0][0], _barLocs[0][1], distBtwnClassBars}, "raw",clsLineClr);
		transGradeBar = new gradeBar(this, new float[] {_barLocs[1][0], _barLocs[1][1], distBtwnClassBars}, transformType,clsLineClr);		
		students = new HashMap<Integer,myStudent>();			
		transPerformed = new HashSet<String>();
	}//ctor
	
	public void setDispWidth(float _dispWidth) {
		rawGradeBar.setDispWidth(_dispWidth);
		transGradeBar.setDispWidth(_dispWidth);
	}//setBarWidth
	//when new transform added, need to clear out existing transformed grades
	public void setRandGenAndType(myRandGen _randGen, String _type) {		
		randGen = _randGen;
		for (myStudent s : students.values()) {//keep raw grades, clear out all transformations
			s.clearTransformedGrades(this);
		}
		setTransformType(_type);
	}//setRandGenAndType	
	
	//set transform to false, set true when actually transformed
	private void setTransformType(String _typ) {
		transformType = _typ;
		transGradeBar.setType(_typ);
		setFlag(classRawIsTransformedIDX, transPerformed.contains(transformType));
	}//setTransformType
	
	public String getTransformType() { return transformType;}
	
	
	public void clearClass() {	students.clear();}
	public void addStudent(myStudent _st) {	students.put(_st.ObjID,_st);}	
	//set the raw grade for a student
	public void setStudentRawGrade(int ID, double grade) {students.get(ID).setRawGrade(this, grade);}
	
	//transform all students in this class using passed rand gen's function
	public void transformStudentGrades() {
		transPerformed.add(transformType);
		for (myStudent s : students.values()) { transformStudentFromRaw(s);}
		setFlag(classRawIsTransformedIDX, true);
	}//transformStudentGrades
	
	//TODO need to retransform student when student is moved - need to set randGen and _type
	public void transformStudentFromRaw(myStudent s) {
		double _rawGrade = s.getRawGrade(this);
		double _newGrade = randGen.inverseCDF(_rawGrade);
		s.setTransformedGrade(transformType, this, _newGrade);
	}//transformStudent
	
	public void transformStudentToRaw(myStudent s) {
		double _transGrade = s.getTransformedGrade(transformType, this);
		double _newGrade = randGen.CDF(_transGrade);
		s.setRawGrade(this, _newGrade);
	}//transformStudentToRaw
		
	//when bar is enabled/disabled, this is called
	public void setGradeBarEnabled(boolean val, String type) {
		if(type.equals("raw")) {			setFlag(classRawToTransEnabledIDX, val);		} 
		else {								setFlag(classTransUsedInFinalGradeIDX, val);	}		
	}//setGradeBarEnabled
		
	public boolean mseClickCheck(int msx, int msy, int btn) {
		boolean res = rawGradeBar.checkMouseClick(msx, msy, btn);
		if(!res) {		res = transGradeBar.checkMouseClick(msx, msy, btn);	}
		return res;
	}//mseClickCheck
	
	public boolean mseDragCheck(int msx, int msy, int btn) {
		boolean res = rawGradeBar.checkMouseMoveDrag(msx, msy, btn);
		if(!res) {		
			res = transGradeBar.checkMouseMoveDrag(msx, msy, btn);	
			//trans grade moved if res
			if((res) && (getFlag(classRawIsTransformedIDX)) && (transGradeBar._modStudent != null)) {transformStudentToRaw(transGradeBar._modStudent);}
		}
		else {//grade moved, retransform, if current transform exists
			if((getFlag(classRawIsTransformedIDX)) && (rawGradeBar._modStudent != null)) {		transformStudentFromRaw(rawGradeBar._modStudent);}
		}
		return res;
	}//mseClickCheck
	
	//using passed scl val [0->1], find closest student to this location
	public myStudent findClosestStudent(float scl, String gradeType) {
		if(students.size() == 0) {return null;}
		myStudent closest = null;
		double closestSqDist = 10000, grade, dist;
		for (myStudent s : students.values()) {
			grade = s.getTransformedGrade(gradeType, this);
			dist = (grade - scl)*(grade-scl);
			if((dist < closestSqDist)) {
				closestSqDist = dist;
				closest = s;
			}
		}	
		return closest;
	}//findClosestStudent
	
	public void mseRelease() {
		rawGradeBar.mouseRelease();
		transGradeBar.mouseRelease();
	}//mseRelease
	
	//draw lines connecting student grades to help visualize mapping if grade is enabled and transformation has been performed
	public void drawRawToTransformedLine() {
		if((getFlag(classRawToTransEnabledIDX)) && (getFlag(classRawIsTransformedIDX))) {
			pa.pushMatrix();pa.pushStyle();
			//first transform to this class's raw grade line
			rawGradeBar.transToBarStart(pa);
			for (myStudent s : students.values()) {
				double rawXLoc = s.getGradeXLoc("raw", this, rawGradeBar.barWidth);
				double transXLoc = s.getGradeXLoc(transformType, this, rawGradeBar.barWidth);
				pa.setStroke(s.clr);
				pa.line(rawXLoc, 0, 0, transXLoc,distBtwnRawTransBars,0);
				
			}
			pa.popStyle();pa.popMatrix();		
		}		
	}//drawRawToTransformedLine
	
	
	//draw all student grades on line 
	public void drawStudentGradesRaw() {rawGradeBar.drawVis(pa);}//drawStudentGradesRaw	
	public void drawStudentGradesTransformed(String _type) {
		transGradeBar.setType(_type);
		transGradeBar.drawVis(pa);		
	}//drawStudentGradesRaw
	
	//incase we wish to store class rosters in sorted mechanism
	@Override
	public int compareTo(myClassRoster othr) {
		int res = this.name.toLowerCase().compareTo(othr.name.toLowerCase());
		return (res == 0 ? Integer.compare(ObjID, othr.ObjID) : res);
	}//compareTo
	
	//state flag management
	private void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX 						: {break;}	
			case classRawToTransEnabledIDX 		: {break;}	
			case classRawIsTransformedIDX		: {break;}
			case classTransUsedInFinalGradeIDX 	: {break;}				
			case drawLineBetweenGradesIDX		: {break;}		
		}
	}//setFlag		
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}	

	
	@Override
	public String toString() {
		String res = "Class Name : " + name + " # of students : " + students.size();
		
		return res;
	}//toString

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
	//listing of raw grade following some distribution, and uniform grade, from result of mapping
	private HashMap<String,HashMap<myClassRoster, Double>> grades;
	//total grade for each type - average
	private HashMap<String, Double> ttlGradePerType;
	//used these for rendering student on line
	private static float rad = 12.0f;
	//color to render student
	public final int[] clr;
	//black for stroke
	private static final int[] blkStrk = new int[] {0,0,0,255};
	//grey for disabled
	private static final int[] greyOff = new int[] {100,100,100,255};
	//location to put point for text display
	private myPointf textLoc;
	
	public myStudent(GraphProbExpMain pa, String _name) {
		ObjID = IDCnt++;  name=_name;
		grades = new HashMap<String,HashMap<myClassRoster, Double>>();
		ttlGradePerType = new HashMap<String, Double>();
		clr = pa.getRndClr2(255);//should be brighter colors
		textLoc = new myPointf(-10, (ObjID % 2 == 0 ? 15 : -10) , 0);
	}//ctor
	
	//clip to be within 0->1 - should always be within this range
	private double clipGrade(String _type, double _gr) {
		//_type present for dbg messages only - add any debugging display code here if desired
		if(_gr >= 1) {			return 1;} 
		else if (_gr <= 0 ) {	return 0;}
		return _gr;
	}//clipGrade
	
	//clear out current transformed grades, preserving raw grades
	public void clearTransformedGrades(myClassRoster _cls) {
		Double ttlGradeRaw = ttlGradePerType.get("raw");
		ttlGradePerType.clear();
		if(ttlGradeRaw != null) {ttlGradePerType.put("raw", ttlGradeRaw);}
		for (String typ : grades.keySet()) {
			if (typ.equals("raw")) {continue;}		//don't clear raw grades
			HashMap<myClassRoster, Double> typeToClearForClasses = grades.get(typ);
			typeToClearForClasses.put(_cls,null);
		}
	}//
		
	//calculate the total grade for this student for each type of grade
	public void calcTotalGrade() {
		ttlGradePerType = new HashMap<String, Double>();
		int numClasses = grades.size();
		for(String _typ : grades.keySet()) {
			HashMap<myClassRoster, Double> classValsForType = grades.get(_typ);			//all student's grades for particular class
			double ttlGrade = 0.0;
			for (myClassRoster _class : classValsForType.keySet()) {				
				Double gr = classValsForType.get(_class);
				ttlGrade += gr;	
			}
			ttlGrade/=numClasses;
			ttlGradePerType.put(_typ, ttlGrade);
		}
	}//calcTotalGrade
		
	public void setRawGrade(myClassRoster _class, double _gr) {setTransformedGrade("raw",_class,_gr); }
	public double getRawGrade(myClassRoster _class) {return getTransformedGrade("raw",_class);}//getRawGrade
	
	//set transformed values for student, keyed by type of transformation and class
	public void setTransformedGrade(String _type, myClassRoster _class, double _gr) {	
		HashMap<myClassRoster, Double> classValsForType = grades.get(_type);	
		if (classValsForType==null) {// no values for this class set yet - build new map of types for this class
			classValsForType = new HashMap<myClassRoster, Double>(); 
			grades.put(_type,classValsForType);
		}		
		classValsForType.put(_class,clipGrade(_type,_gr));
	}//setTransformedGrade
	
	public double getTransformedGrade(String _type,myClassRoster _class) {
		HashMap<myClassRoster, Double> classValsForType = grades.get(_type);
		if (classValsForType==null) {						// no values for this type for any classes set yet  - return 0			
			return 0.0;	}
		Double val = classValsForType.get(_class);			//no grade for this particular class for specified type
		if (val==null) {									//student has no transformed grade for this class, default to 0	- db message here if desired				
			return 0.0;	}
		return val;
	}//getTransformedGrade

	public float getGradeXLoc(String _type,  myClassRoster _class, float ttlWidth) {
		HashMap<myClassRoster, Double> classValsForType = grades.get(_type);
		if (classValsForType==null) {						// no values for this type for any classes set yet 		
			return 0.0f;
		}
		Double val = classValsForType.get(_class);			//grade for class for specified type
		if (val==null) {									//student has no transformed grade for this class	
			return 0.0f; }
		return ttlWidth*val.floatValue();
	}//getGradeXLoc
	
	//////////////////
	// draw this student on line of width ttlWidth - expected to be at start of line when this is called
	private void _drawMe(GraphProbExpMain pa, float gr, float ttlWidth) {
		pa.pushMatrix();pa.pushStyle();
		pa.translate(ttlWidth*gr, 0.0f, 0.0f);
		pa.showCrclNoBox_ClrAra(myPointf.ZEROPT, rad, clr,blkStrk, pa.gui_White, textLoc,  ""+ String.format("%.3f",gr));
		pa.popStyle();pa.popMatrix();
	}//_drawMe

	private void _drawMeDisabled(GraphProbExpMain pa, float gr, float ttlWidth) {
		pa.pushMatrix();pa.pushStyle();
		pa.translate(ttlWidth*gr, 0.0f, 0.0f);
		pa.showCrclNoBox_ClrAra(myPointf.ZEROPT, rad, greyOff,blkStrk, pa.gui_White, textLoc,  ""+ String.format("%.3f",gr));
		pa.popStyle();pa.popMatrix();
	}//_drawMe

	public void drawMeTransformed(GraphProbExpMain pa, String _type, myClassRoster _class, float ttlWidth) {	
		HashMap<myClassRoster, Double> classValsForType = grades.get(_type);
		if (classValsForType==null) {						// no values for this type for any classes set yet 		
			return;	}
		Double val = classValsForType.get(_class);			//grade for class for specified type
		if (val==null) {									//student has no transformed grade for this class	
			return;	}
		_drawMe(pa, val.floatValue(), ttlWidth);
	}//drawMeTransformed
	
	public void drawMeTransformedOff(GraphProbExpMain pa, String _type, myClassRoster _class, float ttlWidth) {	
		HashMap<myClassRoster, Double> classValsForType = grades.get(_type);
		if (classValsForType==null) {						// no values for this type for any classes set yet 		
			return;	}
		Double val = classValsForType.get(_class);			//grade for class for specified type
		if (val==null) {									//student has no transformed grade for this class	
			return;	}
		_drawMeDisabled(pa, val.floatValue(), ttlWidth);
	}//drawMeTransformed
	
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