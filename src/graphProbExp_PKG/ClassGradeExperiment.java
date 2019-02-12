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
	//random generator providing model of source distribution to be used to map from distribution to uniform
	protected myRandGen gradeSourceDistGen;	
	
	//random # generator to be used to generate -inverse- mapping from uniform to target distribution for final grade
	protected myRandGen gradeInvMapGen;
	//current transformation used for students
	protected String curTransformType;
	
	//types of different transformed grades
	public static final int
		rawGradeIDX					= 0,
		normTransGradeIDX			= 1;
	public static final int numGradeTypes = 2;	
	
	//experiment-specific state flag idxs - bits in array holding relevant process info
	public static final int
			debugIDX 			= 0;		
	public static final int numFlags = 1;	
	
	//display-related values
	public static final float 
		distBtwnAdjBars = 60.0f, 
		distBtwnRawTransBars = 600.0f;
	//where first class bar starts
	//public static myPointf classBarStart = new myPointf(10,100,0);
	public static float[] classBarStart = new float[] {10,100};
		
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
		gradeInvMapGen = buildAndInitRandGen(ziggRandGen, GL_QuadSlvrIDX, 256,new myProbSummary(new double[] {0.0,0.1,0,0},2));
		//build fleishman with data set ultimately
		gradeSourceDistGen = buildAndInitRandGen(fleishRandGen_Uni, GL_QuadSlvrIDX, 256,new myProbSummary(new double[] {0,1,1,4},4));	
		curTransformType = gradeInvMapGen.getTransformName();
	}//	initExp
	
	//called by base class call to buildSolvers, during base class ctor
	@Override
	protected void buildSolvers_indiv() {	//any solvers that are custom should be built here		
	}//buildSolvers_indiv
	
	//build an experiment with random students 
	//need to build distribution of raw grades for each class before this is called
	public void buildStudentsAndClasses(int _numStudents, int _numClasses) {
		dispMessage("ClassGradeExperiment","buildStudentsAndClasses","Start building " + _numStudents +" students and " + _numClasses+" classes");
		numStudents = _numStudents;
		students.clear();
		myStudent stdnt;
		for (int i=0;i<numStudents;++i) {
			stdnt = new myStudent(win.pa, "Student : " +i);
			students.put(stdnt.ObjID, stdnt);			
		}
		
		numClasses = _numClasses;
		myClassRoster cls;
		//start location x,y for raw and transformed bars
		float[] rawBarLocSt = new float[] {classBarStart[0],classBarStart[1]},
				transBarLocSt = new float[] {classBarStart[0],classBarStart[1] + distBtwnRawTransBars};
		classes.clear();
		for (int i=0;i<numClasses;++i) {
			cls = new myClassRoster(win.pa, this, "Class : " + i, new float[][] { rawBarLocSt, transBarLocSt});
			classes.add(cls);
			//move to next class's bar
			rawBarLocSt[1] +=distBtwnAdjBars;
			transBarLocSt[1] += distBtwnAdjBars;
			for (myStudent s : students.values()) {
				cls.addStudent(s);
				double grade = ThreadLocalRandom.current().nextDouble();
				cls.setStudentRawGrade(s.ObjID,grade);
			}//for each student assign a random grade and add them to class roster			
		}//for numClasses
		
		calcTotalStudentGrade();
		dispMessage("ClassGradeExperiment","buildStudentsAndClasses","Finished building " + students.size() +" students and " + classes.size()+" classes");		
	}//buildStudentsAndClasses
	
	//calculate the mapping of the raw grades, that follow some distribution, to uniform.
	public void calcMappingDistToUniform(double mappingStd) {		
		//TODO need to determine distribution that follows the grades
		
		curTransformType = gradeInvMapGen.getTransformName();
		for (myClassRoster cls : classes) {
			cls.setRandGenAndType(gradeSourceDistGen,curTransformType);
			cls.transformStudentGrades();
		}
	}//calcInverseMapping
	
	public void testFleishTransform() {
		//test fleishman polynomial-based transformation
		_testFlTransform(gradeSourceDistGen, 10000);
		double area = ((myFleishUniRandGen) gradeSourceDistGen).testInteg();
		dispMessage("ClassGradeExperiment","testFleishTransform","area under fleish poly from 0->1 : " + area);
	}//
	
	private void _testFlTransform(myRandGen flRandGen, int numVals) {
		//test fleishman polynomial-based transformation
		dispMessage("ClassGradeExperiment","_testFlTransform","Specified summary for fleishman polynomial : " + flRandGen.summary.getMomentsVals());
		double[] testData = new double[numVals];
		for(int i=0;i<testData.length;++i) {
			testData[i] = flRandGen.getSample();
		}
		myProbSummary testSummary = new myProbSummary(testData);
		dispMessage("ClassGradeExperiment","_testFlTransform","Analysis res of testSummary for fleishman polynomial : " + testSummary.getMomentsVals());
		
	}
	
	//vary fleishman transformation values to see when it works and when it doesn't
	public void testFleishRangeOfVals() {
		for (double skew=0.0; skew<1.0;skew+=.1) {
	        double bound = -1.2264489 + 1.6410373*skew*skew;
			for (double exKurt=0.0; exKurt<1.0;exKurt+=.1) {
		        if (exKurt < bound) { 
		        	dispMessage("ClassGradeExperiment", "testFleishRangeOfVals", "!!!! Coefficient error : ex kurt : " + exKurt+ " is not feasible with skew :" + skew + " | Bounds is :"+bound);
		        } else {		        	
		        	dispMessage("ClassGradeExperiment", "testFleishRangeOfVals", "ex kurt : " + exKurt+ " is feasible with skew :" + skew + " | Bounds is :"+bound);
		        }
//				myRandGen flRandGen = buildAndInitRandGen(fleishRandGen_Uni, GL_QuadSlvrIDX, 256,new myProbSummary(new double[] {0,1,skew,exKurt},4, true));
//				_testFlTransform(flRandGen, 10000);
			}
		}
	}//testFleishRangeOfVals	
	
	//this will calculate the mapping from uniform for each class to a total mapping that follows some distribution
	public void calcMappingClassToTotal() {
		
		
	}
	
	//calculate the inverse mapping for the raw grades in every class that is active
	public void calcInverseMapping(float mappingStd) {	
		gradeInvMapGen = buildAndInitRandGen(ziggRandGen, GL_QuadSlvrIDX, 256, new myProbSummary(new double[] {0.5,mappingStd,0,0},2));	
		curTransformType = gradeInvMapGen.getTransformName();
		for (myClassRoster cls : classes) {
			cls.setRandGenAndType(gradeInvMapGen,curTransformType);
			cls.transformStudentGrades();
		}
	}//calcInverseMapping
	
	//this will calculate the values for the inverse cdf of the given gaussian, when fed 0->1
	public void calcInverseCDFSpan(double std) {
		myRandGen tmpRandGen = buildAndInitRandGen(ziggRandGen, GL_QuadSlvrIDX, 256, new myProbSummary(new double[] {0.5,std,0,0},2));	
		int numVals = 100;
		double[] xVals = new double[numVals], resVals = new double[numVals];
		for (int i=0;i<numVals; ++i) {
			xVals[i] = (i+1)/(1.0*(numVals)+1);
			resVals[i] = tmpRandGen.inverseCDF(xVals[i]);
			dispMessage("ClassGradeExperiment","calcInverseCDFSpan","Xval ["+i+"] = " + String.format("%3.10f", xVals[i])+" -> Y : "  + String.format("%3.10f", resVals[i]));			
		}
	}//calcInverseCDFSpan
	
	//this is called whenever screen width is changed - used to modify visualizations if necessary
	@Override
	protected void setVisWidth_Priv() {
		if(classes == null) {return;}
		for (myClassRoster cls : classes) {			cls.setDispWidth(visScreenWidth);		}		
	}//setVisWidth_Priv

	//calculate total grade for all students
	public void calcTotalStudentGrade() {
		for (myStudent s : students.values()) {		s.calcTotalGrade();	}
	}//calcTotalStudentGrade
	

	//check mouse over/click in 2d experiment - btn == -1 is mouse over
	@Override	
	public boolean checkMouseClickInExp2D(int msx, int msy, int btn) {		
		for (myClassRoster cls : classes) {			
			//for each class roster check if in bounds of class
			if ((cls.mseClickCheck(msx,msy, btn)) && (btn > -1)){		return true;		}
		}
		return false;
	}
	
	//check mouse over/click in 2d experiment - btn == -1 is mouse over
	@Override	
	public boolean checkMouseDragMoveInExp2D(int msx, int msy, int btn) {		
		for (myClassRoster cls : classes) {			
			//for each class roster check if in bounds of class
			if ((cls.mseDragCheck(msx,msy, btn)) && (btn > -1)){		return true;		}
		}
		return false;
	}
	
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
				cls.drawStudentGradesTransformed(curTransformType);
				cls.drawRawToTransformedLine();
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

