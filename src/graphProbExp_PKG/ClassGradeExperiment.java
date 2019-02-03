package graphProbExp_PKG;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * this class is a specialized type fo probability experiment manager, specifically for the class grades project
 * @author john
 *
 */
public class ClassGradeExperiment extends BaseProbExpMgr{
	//structure holding all classes
	public HashSet<myClassRoster> classes;
	public int numClasses;
	
	//structure holding all students
	public HashMap<Integer,myStudent> students;
	public int numStudents;
	
	
	//types of different transformed grades
	public static final int
		rawGradeIDX			= 0,
		normTransGradeIDX	= 1;
	public static final int numGradeTypes = 2;
	
	
	//experiment-specific state flag idxs - bits in array holding relevant process info
	public static final int
			debugIDX 			= 0;		
	public static final int numFlags = 1;	
	
	//display-related values
	
	public float barWidth, distBetweenBars = 75;
	public myPointf classBarStart = new myPointf(10,100,0);
	
	
	public ClassGradeExperiment(myDispWindow _win) {
		super(_win);	
		initExp();
	}//ctor
	
	//called at end of ctor and whenever experiment needs to be re-instanced
	@Override
	public void initExp() {
		classes = new HashSet<myClassRoster>();
		numClasses = 0;
		students = new HashMap<Integer,myStudent>();
		numStudents = 0;
	}//	initExp
	
	//called by base class call to buildSolvers, during base class ctor
	@Override
	protected void buildSolvers_indiv() {
		//any solvers that are custom should be built here		
	}//buildSolvers_indiv
	
	//build an experiment with random students
	public void buildStudentsAndClasses(int _numStudents, int _numClasses, float winWidth) {
		dispMessage("ClassGradeExperiment","buildStudentsAndClasses","Start building " + _numStudents +" students and " + _numClasses+" classes");
		numStudents = _numStudents;
		myStudent stdnt;
		for (int i=0;i<numStudents;++i) {
			stdnt = new myStudent(win.pa, "Student : " +i);
			students.put(stdnt.ObjID, stdnt);			
		}
		numClasses = _numClasses;
		myClassRoster cls;
		barWidth = .9f*winWidth;
		myPointf rawBarLocSt = new myPointf(classBarStart),
				transBarLocSt = new myPointf(classBarStart.x, classBarStart.y + 600, classBarStart.z);
		
		for (int i=0;i<numClasses;++i) {
			cls = new myClassRoster(win.pa, "Class : " + i, distBetweenBars, new myPointf[] { rawBarLocSt, transBarLocSt});
			cls.setBarWidth(barWidth);
			classes.add(cls);
			rawBarLocSt.y +=distBetweenBars;
			transBarLocSt.y += distBetweenBars;
			for (myStudent s : students.values()) {
				cls.addStudent(s);
				double grade = ThreadLocalRandom.current().nextDouble();
				cls.setStudentRawGrade(s.ObjID,grade);
			}//for each student assign a random grade and add them to class roster			
		}//for numClasses
		
		calcTotalStudentGrade();
		dispMessage("ClassGradeExperiment","buildStudentsAndClasses","Finished building " + students.size() +" students and " + classes.size()+" classes");		
	}//buildStudentsAndClasses
	
	//set this to shrink the class bars to make room for the right side bar menu
	public void setAllClassBarWidths(float _barWidth) {
		barWidth = _barWidth;
		for (myClassRoster cls : classes) {
			cls.setBarWidth(barWidth);
		}
	}//setAllClassBarWidths
	
//	
//	public boolean mseClickCheck(int msx, int msy, int btn) {
//		boolean res = rawGradeBar.checkMouseClick(msx, msy, btn);
//		if(!res) {		res = transGradeBar.checkMouseClick(msx, msy, btn);	}
//		return res;
//	}//mseClickCheck
//	
//	public boolean mseDragCheck(int msx, int msy, int btn) {
//		boolean res = rawGradeBar.checkMouseMoveDrag(msx, msy, btn);
//		if(!res) {		res = transGradeBar.checkMouseMoveDrag(msx, msy, btn);	}
//		return res;
//	}//mseClickCheck

	
	//calculate total grade for all students
	public void calcTotalStudentGrade() {
		for (myStudent s : students.values()) {		s.calcTotalGrade();	}
	}//calcTotalStudentGrade
	

	//check mouse over/click in 2d experiment - btn == -1 is mouse over
	@Override	
	public boolean checkMouseClickInExp2D(int msx, int msy, int btn) {
		
		for (myClassRoster cls : classes) {			
			//for each class roster check if in bounds of class
			if ((cls.mseClickCheck(msx,msy, btn)) && (btn > -1)){
				
				return true;
			}
//			if ((cls.mseNearTransGradeLine(msx,msy, btn)) && (btn > -1)){
//				
//				return true;
//			}
		}
		return false;
	};
	
	//check mouse over/click in 2d experiment - btn == -1 is mouse over
	@Override	
	public boolean checkMouseDragMoveInExp2D(int msx, int msy, int btn) {
		
		for (myClassRoster cls : classes) {			
			//for each class roster check if in bounds of class
			if ((cls.mseDragCheck(msx,msy, btn)) && (btn > -1)){
				
				return true;
			}
//			if ((cls.mseNearTransGradeLine(msx,msy, btn)) && (btn > -1)){
//				
//				return true;
//			}
		}
		return false;
	};
	
	//check mouse over/click in 2d experiment; if btn == -1 then mouse over
	@Override	
	public void setMouseReleaseInExp2D() {	for (myClassRoster cls : classes) {	cls.mseRelease();}	}


	/////////////////////////////	
	//draw routines
	
	//draw raw and transformed class results
	public void drawClassRes() {
		pa.pushMatrix();pa.pushStyle();
			for (myClassRoster cls : classes) {
				cls.drawStudentGradesRaw();
				//cls.drawStudentGradesTransformed(_type);
			}
		
		pa.popStyle();pa.popMatrix();
	}//drawClassRes
	
	
	
	/////////////////////////////	
	//init and manage state flags
	@Override
	protected void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	@Override
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	@Override
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX : {break;}	
			
		}
	}//setFlag		
	@Override
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}

}//class ClassGradeExperiment

