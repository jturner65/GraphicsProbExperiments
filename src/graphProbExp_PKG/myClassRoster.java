package graphProbExp_PKG;

import java.util.*;

/**
 * this class will hold a roster - a collection of students for a specific class, and will manage working with grades
 * @author john
 */
public class myClassRoster implements Comparable<myClassRoster>{
	public static GraphProbExpMain pa;
	public final int ObjID;
	private static int IDCnt = 0;
	//class name
	public final String name;
	//structure holding references to the students in this class - students need to be created external to class and added
	protected HashMap<Integer,myStudent> students;

	///////////////////////////
	//rendering 
	//location and dimenions of raw line on display representing this class
	private final float distBetweenBars;

	//2 class bars - raw and transformed
	private gradeBar rawGradeBar, transGradeBar;
	//type of transformation for transformed grade bar
	private String transformType;
	
	
	//this class's bar color
	private final int[] clsLineClr;
	
	//state flags - bits in array holding relevant info about this random variable function
	private int[] stFlags;						
	public static final int
			debugIDX 					= 0,
			classRawToTransEnabledIDX	= 1,			//if class's raw grades are mapped to transformed grades
			classUsedInFinalGradeIDX	= 2;
	public static final int numFlags 	= 3;
	
	// gradeBar(GraphProbExpMain _pa, myClassRoster _owningClass, float _d, myPointf _bs, String _typ)
	
	public myClassRoster(GraphProbExpMain _pa, String _name, float _dToNextBar, myPointf[] _barLocs) {
		pa =_pa;
		ObjID = IDCnt++;  name=_name;
		initFlags();
		
		distBetweenBars=_dToNextBar;
		rawGradeBar = new gradeBar(pa, this, distBetweenBars,_barLocs[0], "raw");
		transformType = "unk";
		transGradeBar = new gradeBar(pa, this, distBetweenBars,_barLocs[1], transformType);		
		students = new HashMap<Integer,myStudent>();				
		clsLineClr = pa.getRndClr2(255);//should be brighter colors
	}//ctor
	
	
	public void setBarWidth(float _barWidth) {
		rawGradeBar.setBarWidth(_barWidth);
		transGradeBar.setBarWidth(_barWidth);
	}//setBarWidth
	
	public void setTransformType(String _typ) {
		transformType = _typ;
		transGradeBar.setType(_typ);
	}
	
	public String getTransformType() { return transformType;}
	
	
	public void clearClass() {	students.clear();}
	public void addStudent(myStudent _st) {	students.put(_st.ObjID,_st);}	
	//set the raw grade for a student
	public void setStudentRawGrade(int ID, double grade) {students.get(ID).setRawGrade(this, grade);}
	
	//set the transformed grade for a student
	public void setStudentTransformedGrade(String _type, int ID, double grade) {		
		students.get(ID).setTransformedGrade(_type,this, grade);
	}
	
	
	public boolean mseClickCheck(int msx, int msy, int btn) {
		boolean res = rawGradeBar.checkMouseClick(msx, msy, btn);
		if(!res) {		res = transGradeBar.checkMouseClick(msx, msy, btn);	}
		return res;
	}//mseClickCheck
	
	public boolean mseDragCheck(int msx, int msy, int btn) {
		boolean res = rawGradeBar.checkMouseMoveDrag(msx, msy, btn);
		if(!res) {		res = transGradeBar.checkMouseMoveDrag(msx, msy, btn);	}
		return res;
	}//mseClickCheck
	
	public void mseRelease() {
		rawGradeBar.mouseRelease();
		transGradeBar.mouseRelease();
	}
	
	
	//draw all student grades on line 
	public void drawStudentGradesRaw() {rawGradeBar.drawGradeBar();}//drawStudentGradesRaw	
	public void drawStudentGradesTransformed(String _type) {
		transGradeBar.setType(_type);
		transGradeBar.drawGradeBar();		
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
			case debugIDX 			: 		{break;}	
			case classRawToTransEnabledIDX 	: 		{break;}	
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
 * this class holds the functionality to manage a class's grade bar display, along with the overall grade bar display
 * @author john
 */
class gradeBar{
	//ref to applet for rendering
	public static GraphProbExpMain pa;
	public final int ObjID;
	private static int IDCnt = 0;
	//class this bar is attached to - if none means overall grade
	private final myClassRoster owningClass;
	//string descriptor of type of grades to display
	private String gradeType;
	//star location of bar
	private final myPointf barStartPoint;
	//distance in y between adjacent bars
	private final float distBetweenBars;
	//black for box stroke; red for box denoting bar is off; green for showing bar is on; grey for line off
	private static final int[] blkStrk = new int[] {0,0,0,255},redBoxOff = new int[] {255,0,0,255}, greenBoxOn = new int[] {0,255,0,255},greyOff = new int[] {100,100,100,255};	
	//click box x,y,w,h dims
	private static final float[] _clkBox = new float[] {0,-8,16,16};
	//this bar's color
	private final int[] barColor;
	//relative x for where bar starts
	private static final float _barSt = 30;
	//specifically set color ctor
	private float barWidth;
	//this bar is enabled/disabled
	private boolean enabled;
	//student being moved by mouse click
	private myStudent _modStudent;
	//previous click scale related to _modStudent, so grade can be modified as mouse is moved
	private float _lastClickScale;
	
	//specific color constructor - used to set up overall grade bar
	public gradeBar(GraphProbExpMain _pa, myClassRoster _owningClass, float _d, myPointf _bs, String _typ, int[] _barColor) {
		pa=_pa; ObjID = IDCnt++; gradeType=_typ;
		barStartPoint = new myPointf(_bs);
		distBetweenBars = _d;
		barColor = _barColor;// getRndClr2 should be brighter colors
		owningClass = _owningClass;
	}//ctor
	//random color ctor - used by classes
	public gradeBar(GraphProbExpMain _pa, myClassRoster _owningClass, float _d, myPointf _bs, String _typ) {this(_pa, _owningClass,_d,_bs,_typ,_pa.getRndClr2(255));}	
	
	//specifically if clicked
	public boolean checkMouseClick(int msx, int msy, int btn) {
		int msXLoc = (int) (msx - barStartPoint.x), mxYLoc = (int) (msy - barStartPoint.y);
		boolean inClassLineRegion = (msXLoc >= 0) && (mxYLoc >= -.5*distBetweenBars) && (msXLoc <= barWidth + _barSt) && (mxYLoc <= .5*distBetweenBars);
		if ((btn < 0 ) || (!inClassLineRegion)) {mouseRelease();return inClassLineRegion;}
		if((msXLoc <= _clkBox[2]) && (mxYLoc >= _clkBox[1]) && (mxYLoc <= (_clkBox[1]+_clkBox[3]))) {	enabled = !enabled;		return true;	} 	//clicked box - toggle state
		else if(msXLoc >= _barSt) {//clicked near student bar - grab a student and attempt to move
			float clickScale = ((msXLoc-_barSt)/barWidth);
			//see if student grade location is being clicked on
			_modStudent = findClosestStudent(clickScale);
			_lastClickScale = clickScale;
			System.out.println("x:"+msXLoc + "| y:"+mxYLoc+ " | clickScale : " + clickScale + "| Closest Student :  " + _modStudent.name);
			return true;
		}		
		return false;
	}//checkMouseClick
	
	//specifically if moved or dragged
	public boolean checkMouseMoveDrag(int msx, int msy, int btn) {
		int msXLoc = (int) (msx - barStartPoint.x), mxYLoc = (int) (msy - barStartPoint.y);
		boolean inClassLineRegion = (msXLoc >= 0) && (mxYLoc >= -.5*distBetweenBars) && (msXLoc <= barWidth + _barSt) && (mxYLoc <= .5*distBetweenBars);
		if ((btn < 0 ) || (!inClassLineRegion)) {mouseRelease();return inClassLineRegion;}
		//x location of click
		float clickScale = ((msXLoc-_barSt)/barWidth);
		//if moving a student, update grade
		if(_modStudent != null) {
			_modStudent.setTransformedGrade(gradeType, owningClass,clickScale);
			_lastClickScale = clickScale;			
			return true;
		}
		return false;
	}//checkMouseMoveDrag
	
	//release student being dragged
	public void mouseRelease() {		
		_modStudent = null;
		_lastClickScale = -1;
	}//
	
	//using passed scl val [0->1], find closest student to this location
	public myStudent findClosestStudent(float scl) {
		if(owningClass.students.size() == 0) {return null;}
		myStudent closest = null;
		double closestSqDist = 10000, grade, dist;
		for (myStudent s : owningClass.students.values()) {
			grade = s.getTransformedGrade(gradeType, owningClass);
			dist = (grade - scl)*(grade-scl);
			if((dist < closestSqDist)) {
				closestSqDist = dist;
				closest = s;
			}
		}	
		return closest;
	}//findClosestStudent
	
	//
	public void drawGradeBar() {
		pa.pushMatrix();pa.pushStyle();
		pa.translate(barStartPoint);
		if(enabled) {
			pa.pushMatrix();pa.pushStyle();
			pa.setFill(greenBoxOn);
			pa.setStroke(blkStrk);
			pa.rect(_clkBox);
			pa.translate(_barSt,0,0);
			pa.setStroke(barColor);
			pa.strokeWeight(2.0f);
			pa.line(0,0,0,barWidth,0,0);
			for (myStudent s : owningClass.students.values()) {		s.drawMeTransformed(pa, gradeType, owningClass, barWidth);	}
			pa.popStyle();pa.popMatrix();					
		} else {							
			pa.pushMatrix();pa.pushStyle();
			pa.setFill(redBoxOff);
			pa.setStroke(blkStrk);
			pa.rect(_clkBox);
			pa.translate(_barSt,0,0);
			pa.setStroke(greyOff);
			pa.strokeWeight(2.0f);
			pa.line(0,0,0,barWidth,0,0);
			for (myStudent s : owningClass.students.values()) {		s.drawMeTransformedOff(pa, gradeType, owningClass, barWidth);	}		
			pa.popStyle();pa.popMatrix();
		}
		pa.popStyle();pa.popMatrix();			
	}//_drawGrades
	
	public void setBarWidth(float _barWidth) {		barWidth = _barWidth;	}
	public boolean isBarEnabled() {return enabled;}
	public void setBarEnabled(boolean _en) {enabled=_en;}
	public void setType(String _typ) {gradeType=_typ;}
	public String getType() {return gradeType;}
		
}//class gradeBar


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
	//listing of raw grade and various transformational mappings of student's grade - should always be between 0 and 1
	private HashMap<String,HashMap<myClassRoster, Double>> grades;
	//total grade for each type - average
	private HashMap<String, Double> ttlGradePerType;
	//used these for rendering student on line
	private static float rad = 12.0f;
	//color to render student
	private final int[] clr;
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
		if(_gr > 1) {			return 1;} 
		else if (_gr < 0 ) {	return 0;}
		return _gr;
	}//clipGrade
	
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
			return 0.0;
		}
		Double val = classValsForType.get(_class);			//no grade for this particular class for specified type
		if (val==null) {									//student has no transformed grade for this class, default to 0	- db message here if desired				
			return 0.0;
		}
		return val;
	}//getTransformedGrade

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
			return;
		}
		Double val = classValsForType.get(_class);			//grade for class for specified type
		if (val==null) {									//student has no transformed grade for this class	
			return;
		}
		_drawMe(pa, val.floatValue(), ttlWidth);
	}//drawMeTransformed
	
	public void drawMeTransformedOff(GraphProbExpMain pa, String _type, myClassRoster _class, float ttlWidth) {	
		HashMap<myClassRoster, Double> classValsForType = grades.get(_type);
		if (classValsForType==null) {						// no values for this type for any classes set yet 		
			return;
		}
		Double val = classValsForType.get(_class);			//grade for class for specified type
		if (val==null) {									//student has no transformed grade for this class	
			return;
		}
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