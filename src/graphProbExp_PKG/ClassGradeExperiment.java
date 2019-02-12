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
	
	//structure holding grades for a particular class read in from a file, or otherwise synthesized from some unknown distribution.  is keyed by class name and then by student ID
	public HashMap<String,HashMap<Integer, Double>> perClassStudentGrades;
	
	//random generators describing the distributions of the grades in each class, keyed by class name
	protected HashMap<String, myRandGen> gradeDistsPerClass;
	//random # generator to be used to generate -inverse- mapping from uniform to target distribution for final grade
	//protected myRandGen gradeInvMapGen;
	//current transformation used for students
	//protected String curTransformType;
	
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
		distBtwnRawTransBars = 500.0f;
	//where first class bar starts relative to top left corner of display window
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
		//gradeInvMapGen = buildAndInitRandGen(ziggRandGen, new myProbSummary(new double[] {0.0,0.1,0,0},2));
		//build fleishman with data set ultimately
		gradeSourceDistGen = buildAndInitRandGen(fleishRandGen_Uni, new myProbSummary(new double[] {0,1,1,4},4));	
		//curTransformType = gradeInvMapGen.getTransformName();
	}//	initExp
	
	//load student grades into per class grade structure - all classes and students should be already made by here
	public void loadStudentGrades() {
		perClassStudentGrades = new HashMap<String,HashMap<Integer, Double>>();
		gradeDistsPerClass = new HashMap<String, myRandGen>();
		myProbSummary tmp = new myProbSummary(new double[] {0.5,1,0,0},2);
		tmp.setMinMax(0.0, 1.0);
		myRandGen tmpRandGen = buildAndInitRandGen(ziggRandGen, tmp);
		
		for (myClassRoster _cls : classes) {
			HashMap<Integer, Double> classGrades = perClassStudentGrades.get(_cls.name);
			if (null == classGrades) {classGrades = new HashMap<Integer, Double>(); perClassStudentGrades.put(_cls.name, classGrades);}
			//to hold for distribution
			double[] vals = new double[students.size()];
			int idx =0;
			
			//get values from file
			
			for (myStudent s : students.values()) {
				//replace this with source of class data - reading file, building from "unknown" distribution, etc.
				double grade = tmpRandGen.getSample();
				
				
				
				
				
				
				
				
				
				vals[idx++]=grade;
				classGrades.put(s.ObjID, grade);
			}//for each student assign a random grade and add them to class roster			
			//build summary object from vals
			myProbSummary summaryObj = new myProbSummary(vals);
			dispMessage("ClassGradeExperiment","loadStudentGrades","Built Summary object with following stats : " + summaryObj.getMinNumMmnts());
			//Ultimately need to model non-normal distributions
			//gradeDistsPerClass.put(_cls.name, buildAndInitRandGen(fleishRandGen_Uni, summaryObj));
			gradeDistsPerClass.put(_cls.name, buildAndInitRandGen(ziggRandGen, summaryObj));
		
		}
	}//loadStudentGrades

	
		
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
			}//for each student assign a random grade and add them to class roster			
		}//for numClasses
		
		//TODO remove this once it is working
		loadStudentGrades();
		setStudentGradeValues();
		dispMessage("ClassGradeExperiment","buildStudentsAndClasses","Finished building " + students.size() +" students and " + classes.size()+" classes");		
	}//buildStudentsAndClasses
	
	//call this to populate all student grades from class grade structure
	public void setStudentGradeValues() {
		dispMessage("ClassGradeExperiment","setStudentGradeValues","Start setting " + students.size() + " student grades for each of " + classes.size()+" classes");
		//set grades
		for (myClassRoster _cls : classes) {	
			HashMap<Integer, Double> classGrades = perClassStudentGrades.get(_cls.name);
			for (myStudent s : students.values()) {
				Integer SID = s.ObjID;
				Double grade = classGrades.get(SID);
				if(null==grade) {//no grade for student - this is an error
					dispMessage("ClassGradeExperiment","setStudentGradeValues","No grade found for student ID :"+SID +" | Name : " +s.name+" | Defaulting grade to 0");
					grade=0.0;
				}
				_cls.setStudentRawGrade(s.ObjID,grade);
			}//for each student assign a random grade and add them to class roster			
		}
		calcTotalStudentGrade();
		dispMessage("ClassGradeExperiment","setStudentGradeValues","Finished setting " + students.size() + " student grades for each of " + classes.size()+" classes");
	}//setStudentGradeValues
	
//	//calculate the mapping of the raw grades, that follow some distribution, to uniform.
//	public void calcMappingDistToUniform(double mappingStd) {		
//		//TODO need to determine distribution that follows the grades
//		
//		curTransformType = gradeInvMapGen.getTransformName();
//		for (myClassRoster cls : classes) {
//			cls.setRandGenAndType(gradeSourceDistGen,curTransformType);
//			cls.transformStudentGrades();
//		}
//	}//calcInverseMapping
	
	public void testFleishTransform() {
		//test fleishman polynomial-based transformation
		_testFlTransform(gradeSourceDistGen, 10000);
		double min = -1, max = 1;
		double area = ((myFleishUniRandGen) gradeSourceDistGen).testInteg(min,max);
		dispMessage("ClassGradeExperiment","testFleishTransform","area under fleish poly from "+min+"->"+max+" : " + area);
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
//		gradeInvMapGen = buildAndInitRandGen(ziggRandGen, new myProbSummary(new double[] {0.5,mappingStd,0,0},2));	
//		curTransformType = gradeInvMapGen.getTransformName();
		
		
		for (myClassRoster cls : classes) {
			myRandGen randGen = gradeDistsPerClass.get(cls.name);
			cls.setRandGenAndType(randGen);
			cls.transformStudentGrades();
		}
	}//calcInverseMapping
	
	//this will calculate the values for the inverse cdf of the given gaussian, when fed 0->1
	public void calcInverseCDFSpan(double std) {
		myRandGen tmpRandGen = buildAndInitRandGen(ziggRandGen, new myProbSummary(new double[] {0.5,std,0,0},2));	
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

	//called by base class call to buildSolvers, during base class ctor
	@Override
	protected void buildSolvers_indiv() {	//any solvers that are custom should be built here		
	}//buildSolvers_indiv
	

	/////////////////////////////	
	//draw routines
	
	//draw raw and transformed class results
	public void drawClassRes() {
		pa.pushMatrix();pa.pushStyle();
			for (myClassRoster cls : classes) {
				cls.drawStudentGradesRaw();
				//cls.drawStudentGradesTransformed(curTransformType);
				cls.drawStudentGradesTransformed();
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

