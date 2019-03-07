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
	public HashSet<myClassRoster> classRosters;
	public int numClasses;
	
	//structure holding all students
	public HashMap<Integer,myStudent> students;
	public int numStudents;
	
	//structure holding grades for a particular class read in from a file, or otherwise synthesized from some unknown distribution.  is keyed by class name and then by student ID
	public HashMap<String,HashMap<Integer, Double>> perClassStudentGrades;
	//summary objects for each class, based on loaded grades
	public HashMap<String, myProbSummary> perClassSummaryObjMap;
	
	//types of different transformed grades
	public static final int
		rawGradeIDX					= 0,
		normTransGradeIDX			= 1,
		scaleTransformGradeIDX		= 2,
		rebuildDistOnGradeModIDX 	= 3;
	public static final int numGradeTypes = 4;	
	
	//final grade class is class representing the final grade (not a real class)
	private myFinalGradeRoster finalGradeClass;
	
	
	//experiment-specific state flag idxs - bits in array holding relevant process info
	public static final int
			debugIDX 				= 0,
			useZScoreFinalGradeIDX	= 1,
			showPlotsIDX			= 2;			//if true show each class distribution plot, if false show class bar
	public static final int numFlags = 3;	
	
	//display-related values
	public static final float 
		distBtwnAdjBars = 80.0f, 
		distBtwnRawTransBars = 500.0f,
		distBtwnAdjPlots = 300.0f;
	//where first class bar starts relative to top left corner of display window
	public static float[] classBarStart = new float[] {10,100};
	public static float[] classPlotStart = new float[] {10,50};
	
	public ClassGradeExperiment(myDispWindow _win) {
		super(_win);
		initExp();
	}//ctor
	
	private float getPlotHeight() {
		return .95f * win.curVisScrDims[1]/(numClasses + 1);
	}
	
	//called at end of ctor and whenever experiment needs to be re-instanced
	@Override
	public void initExp() {
		classRosters = new HashSet<myClassRoster>();
		numClasses = 0;
		students = new HashMap<Integer,myStudent>();
		numStudents = 0;
		
		finalGradeClass = buildFinalGradeRoster(getPlotHeight());
		

		//gradeInvMapGen = buildAndInitRandGen(ziggRandGen, new myProbSummary(new double[] {0.0,0.1,0,0},2));
		//build fleishman with data set ultimately
		//gradeSourceDistGen = buildAndInitRandGen(fleishRandGen_UniVar, new myProbSummary(new double[] {0,1,1,4},4));	
		//curTransformType = gradeInvMapGen.getTransformName();
		//finalGPA = new gradeBar(this, new float[] {_barLocs[barLocIDX][0], _barLocs[barLocIDX][1], distBtwnClassBars}, transTypes[i],clsLineClr, "Visualization of "+transTypes[i]+" grades for class :"+name);	
	}//	initExp
	
	//numMmnts are # of moments that should be used of mmnts array (<= len mmnts Ara)
	private void setDesiredFinalGradeSummaryObj(double[] _mmnts, double[] _minMax, int _numMmnts) {
		double[] mmnts = new double[_numMmnts];		
		for(int i=0;i< _numMmnts; ++i) {			mmnts[i]=_mmnts[i];		}
		myProbSummary tmpFinal = new myProbSummary(mmnts,_numMmnts);
		tmpFinal.setMinMax(_minMax);
		perClassSummaryObjMap.put(finalGradeClass.name, tmpFinal);		
	}//setDesiredFinalGradeSummaryObj
	
	//rebuild final grade distribution mapping based on passed type, momments and minMax
	public void buildNewDesiredFinalGradeRandGen(double[] _mmnts, double[] _minMax, int _numMmnts, int _type) {
		setDesiredFinalGradeSummaryObj(_mmnts,  _minMax, _numMmnts);
		myRandGen tmpFinalRandGen = buildAndInitRandGen(_type, perClassSummaryObjMap.get(finalGradeClass.name));
		finalGradeClass.setBaseDistModel(tmpFinalRandGen);
	}//buildNewDesiredFinalGradeRandGen
		
	
	//perform affine linear transformation of grades so that they span 0->1
	public void linearTransformExperiment(int _numStudents, int _numClasses) {
		dispMessage("ClassGradeExperiment","linearTransformExperiment","Start building " + _numStudents +" students and " + _numClasses+" classes for LinearTransform EXP",true);
		//rebuild all student and class objs, and final roster object
		buildStudentsAndClasses(buildRandStudentNameList(_numStudents), _numClasses);
		//generate random/load specified student grades, assign to students
		buildAndSetStudentGrades(true, null);
		//build randGen mapper
		setRandGensAndTransformGrades(linearTransformMap);
		refreshBuildStateFlags();
		dispMessage("ClassGradeExperiment","linearTransformExperiment","Finished building " + students.size() +" students and " + classRosters.size()+" classes for LinearTransform EXP",true);		
	}//linearTransformExperiment
	
	//perform uniform transform of grades so that the actual value is lost but the grades are just ranked #/n where # is sorted order of grade and n is total # of grades
	public void uniformTransformExperiment(int _numStudents, int _numClasses) {
		dispMessage("ClassGradeExperiment","uniformTransformExperiment","Start building " + _numStudents +" students and " + _numClasses+" classes for Uniform Mapping EXP",true);
		//rebuild all student and class objs, and final roster object
		buildStudentsAndClasses(buildRandStudentNameList(_numStudents), _numClasses);
		//generate random/load specified student grades, assign to students
		buildAndSetStudentGrades(true, null);
		//build randGen mapper
		setRandGensAndTransformGrades(uniformTransformMap);
		refreshBuildStateFlags();
		dispMessage("ClassGradeExperiment","uniformTransformExperiment","Finished building " + students.size() +" students and " + classRosters.size()+" classes for Uniform Mapping EXP",true);		
	}//uniformTransformExperiment
	
	//fleishman model of distribution
	public void fleishModelExperiment(int _numStudents, int _numClasses) {
		dispMessage("ClassGradeExperiment","fleishModelExperiment","Start building " + _numStudents +" students and " + _numClasses+" classes for Fleishman model of grades EXP",true);
		//rebuild all student and class objs, and final roster object
		buildStudentsAndClasses(buildRandStudentNameList(_numStudents), _numClasses);
		//generate random/load specified student grades, assign to students
		buildAndSetStudentGrades(true, null);
		//build randGen mapper
		setRandGensAndTransformGrades(fleishRandGen_UniVar);
		refreshBuildStateFlags();
		dispMessage("ClassGradeExperiment","fleishModelExperiment","Finished building " + students.size() +" students and " + classRosters.size()+" classes for Fleishman model of grades EXP",true);		
	}//uniformTransformExperiment

	
	//build list of students with randomly assigned names - to be replaced by file loading all student names
	public String[] buildRandStudentNameList(int _numStudents) {
		String[] res = new String[_numStudents];
		for (int i=0;i<numStudents;++i) {			res[i]="Student : " +i;	}
		return res;
	}//buildStudentList_Random

	//this will build _numStudents random students, _numClasses classes, a final class roster and setup random grades and grade mappings
	public void buildStudentsClassesRandGrades(int _numStudents, int _numClasses) {
		//rebuild all student and class objs, and final roster object
		buildStudentsAndClasses( buildRandStudentNameList(_numStudents), _numClasses);			
		//generate random/load specified student grades, assign to students
		buildAndSetStudentGrades(true, null);
		//build and assign all randGen transforms to each class and final roster, and transform grades
		setRandGensAndTransformGrades(ziggRandGen);		
	}//buildStudentsClassesRandGrades clearAllTransformedStudentGrades()
	
	//call this to build student grades - if is random file name is ignored, otherwise will load grades for classes specified in file name
	public void buildAndSetStudentGrades(boolean isRandom, String[] fileNames) {
		dispMessage("ClassGradeExperiment","buildAndSetStudentGrades","Start loading/generating and assigning student grades, and building and assigning randGens of sample dists for each class",true);
		//build random grades for each class, or load random grades for each class, including summary objects
		if (isRandom) {			buildRandomStudentGrades();		} 
		else {				
			perClassStudentGrades = new HashMap<String,HashMap<Integer, Double>>();
			perClassSummaryObjMap = new HashMap<String, myProbSummary>();
			for(String fileName : fileNames) {			loadSpecifiedStudentGrades(fileName);			}
		}
		
		//perClassStudentGrades and perClassSummaryObjMap should be populated by here	
		//set grades for each student, in each class
		for (myClassRoster _cls : classRosters) {				_cls.setAllStudentRawGrades(perClassStudentGrades.get(_cls.name));	}

		dispMessage("ClassGradeExperiment","buildAndSetStudentGrades","Finished loading/generating and assigning student grades, and building and assigning randGens of sample dists for each class",true);
	}//buildAndSetStudentGrades
	
	//use passed randGenType to build modeling distributions of loaded/random grades - model will be of this type; possible models are specified in 
	//BaseProbExpMgr at tag "types of random number generators implemented/supported so far"
	public void setRandGensAndTransformGrades(int randGenType) {		
		//set up desired final grade prob
		//!!!! NEED TO REBUILD  perClassSummaryObjMap.get(finalGradeClass.name) if moving to different kind of transform
		myRandGen tmpFinalRandGen = buildAndInitRandGen(randGenType, perClassSummaryObjMap.get(finalGradeClass.name));
		finalGradeClass.setBaseDistModel(tmpFinalRandGen);
		//final grades need to be specified first
		for (myClassRoster _cls : classRosters) {	
			myRandGen gen = buildAndInitRandGen(randGenType, perClassSummaryObjMap.get(_cls.name));
			_cls.setBaseDistModel(gen);
			_cls.transformStudentGradesToUniform();
		}
	}//setAllRosterTransforms
	
	public void clearAllPlotEval() {
		for (myClassRoster _cls : classRosters) {			_cls.clearPlotEval();	}
		finalGradeClass.clearPlotEval();	
		setShowPlots(false);
	}//clearAllPlotEval
	
	//derive and show plots of different distributions behind each class calc
	public void evalPlotClassDists(boolean isHist, int funcType, int numVals, int numBuckets, double low, double high) {
		if(isHist) {
			for (myClassRoster _cls : classRosters) {			_cls.evalAndPlotHistRes(numVals, numBuckets);	}
			finalGradeClass.evalAndPlotHistRes(numVals, numBuckets);	
			
		} else {
			for (myClassRoster _cls : classRosters) {			_cls.evalAndPlotFuncRes(numVals, low, high, funcType);	}
			finalGradeClass.evalAndPlotFuncRes(numVals, low, high, funcType);
		}
		setShowPlots(true);
	}//evalPlotClassDists
	
	
	//whether or not to show plots for all specified distributions
	public void setShowPlots(boolean val) {setFlag(this.showPlotsIDX,val);	}	
	
	//derive random student grades for each class, build prob summary objs of sample dists per class, and define and set final grade dist prob summary
	private void buildRandomStudentGrades() {
		dispMessage("ClassGradeExperiment","buildRandomStudentGrades","Start sampling "  +students.size() + " random student grades for " + classRosters.size() +" classes",true);
		perClassStudentGrades = new HashMap<String,HashMap<Integer, Double>>();
		perClassSummaryObjMap = new HashMap<String, myProbSummary>();
		
		//tmp dist for use with generating raw grades - since raw grades are rejected outside the range 0-1 the final dist will be somewhat different
		myProbSummary tmp = new myProbSummary(new double[] {0.5,.15,0,0},2);
		tmp.setMinMax(0.0, 1.0);
		myRandGen tmpRandGen = buildAndInitRandGen(ziggRandGen, tmp);
		for (myClassRoster _cls : classRosters) {
			HashMap<Integer, Double> classGrades = perClassStudentGrades.get(_cls.name);
			if (null == classGrades) {classGrades = new HashMap<Integer, Double>(); perClassStudentGrades.put(_cls.name, classGrades);}
			//to hold for distribution
			double[] vals = new double[students.size()];
			int idx =0;			
			//get values from file
			//overwrites grade in structure
			for (myStudent s : students.values()) {
				//replace this with source of class data - reading file, building from "unknown" distribution, etc.
				double grade = tmpRandGen.getSample();
				while ((grade < 0) || (grade > 1)) {
					grade = tmpRandGen.getSample();
				} 
				
				vals[idx++]=grade;
				classGrades.put(s.ObjID, grade);
			}//for each student assign a random grade and add them to class roster			
			//build summary object from vals
			myProbSummary summaryObj = new myProbSummary(vals);
			dispMessage("ClassGradeExperiment","loadStudentGrades","Built Summary object with following stats : " + summaryObj.getMinNumMmnts(),true);
			perClassSummaryObjMap.put(_cls.name, summaryObj);
		}
		//set desired final mapping
		setDesiredFinalGradeSummaryObj(new double[] {0.75,.2,0,0}, new double[] {0.0,1.0}, 2);
		dispMessage("ClassGradeExperiment","buildRandomStudentGrades","Finished sampling "  +students.size() + " random student grades for " + classRosters.size() +" classes",true);
	}//buildRandomStudentGrades
	
	private void loadSpecifiedStudentGrades(String fileName) {
		dispMessage("ClassGradeExperiment","loadSpecifiedStudentGrades","Start loading grades from " + fileName,true);
		
		//TODO - load grades specified by fileName (perhaps per class?
		
		dispMessage("ClassGradeExperiment","loadSpecifiedStudentGrades","Finished loading grades from " + fileName,true);
		
	}//loadSpecifiedStudentGrades

	private myFinalGradeRoster buildFinalGradeRoster(float heightOfFinalPlot) {
		//build final grade roster
		float[] finalUniBarLocSt = new float[] {classBarStart[0], win.curVisScrDims[1]- distBtwnAdjBars},
				finalTransBarLocSt = new float[] {classBarStart[0], win.curVisScrDims[1] - 3*distBtwnAdjBars}, 
				finalPlotLocSt = new float[] {classPlotStart[0], win.curVisScrDims[1] - heightOfFinalPlot , heightOfFinalPlot}
				;		

		return new myFinalGradeRoster(win.pa, this, "Final Grades For All Students", new float[][] { finalUniBarLocSt, finalTransBarLocSt,finalPlotLocSt});
	}//buildFinalGradeRoster
	
	//rebuild all students, classes and final grades class roster
	private void buildStudentsAndClasses(String[] _studentNames, int _numClasses) {
		dispMessage("ClassGradeExperiment","buildStudentsAndClasses","Start building " + _studentNames.length +" students and " + _numClasses+" classes",true);		
		numStudents = _studentNames.length;
		students.clear();
		myStudent stdnt;
		for (int i=0;i<numStudents;++i) {
			stdnt = new myStudent(win.pa, _studentNames[i]);
			students.put(stdnt.ObjID, stdnt);			
		}	
		numClasses = _numClasses;
		float heightOfPlots =  getPlotHeight();
		finalGradeClass = buildFinalGradeRoster(heightOfPlots);
		//set students in final grade roster
		finalGradeClass.setStudents(students);	
		finalGradeClass.setUseZScore(getFlag(useZScoreFinalGradeIDX));
		myClassRoster cls;
		//start location x,y for raw and transformed bars
		float[] rawBarLocSt = new float[] {classBarStart[0],classBarStart[1]},
				transBarLocSt = new float[] {classBarStart[0],classBarStart[1] + distBtwnRawTransBars},
				plotRectLocSt = new float[] {classPlotStart[0],classPlotStart[1], heightOfPlots};
		classRosters.clear();
		for (int i=0;i<numClasses;++i) {
			cls = new myClassRoster(win.pa, this, "Class : " + (i+1), new float[][] { rawBarLocSt, transBarLocSt,plotRectLocSt});
			cls.setFinalGradeRoster(finalGradeClass);
			cls.setStudents(students);			
			classRosters.add(cls);
			//move to next class's bar
			rawBarLocSt[1] +=distBtwnAdjBars;
			transBarLocSt[1] += distBtwnAdjBars;
			plotRectLocSt[1] += heightOfPlots;
		}//for numClasses
		dispMessage("ClassGradeExperiment","buildStudentsAndClasses","Finished building " + students.size() +" students and " + classRosters.size()+" classes",true);		
	}//buildStudentsAndClasses
	
	
	
	//test efficacy of fleishman polynomial transform
	public void testFleishTransform() {
		myRandGen gradeSourceDistGen = buildAndInitRandGen(fleishRandGen_UniVar, new myProbSummary(new double[] {0,1,1,4},4));
		//test fleishman polynomial-based transformation
		_testFlTransform(gradeSourceDistGen, 10000);
		double min = -1, max = 1;
		double area = ((myFleishUniVarRandGen) gradeSourceDistGen).testInteg(min,max);
		dispMessage("ClassGradeExperiment","testFleishTransform","area under fleish poly from "+min+"->"+max+" : " + area,true);
	}//
	
	private void _testFlTransform(myRandGen flRandGen, int numVals) {
		//test fleishman polynomial-based transformation
		dispMessage("ClassGradeExperiment","_testFlTransform","Specified summary for fleishman polynomial : " + flRandGen.summary.getMomentsVals(),true);
		double[] testData = new double[numVals];
		for(int i=0;i<testData.length;++i) {
			testData[i] = flRandGen.getSample();
		}
		myProbSummary testSummary = new myProbSummary(testData);
		dispMessage("ClassGradeExperiment","_testFlTransform","Analysis res of testSummary for fleishman polynomial : " + testSummary.getMomentsVals(),true);
		
	}
	
	//vary fleishman transformation values to see when it works and when it doesn't
	public void testFleishRangeOfVals() {
		for (double skew=0.0; skew<1.0;skew+=.1) {
	        double bound = -1.2264489 + 1.6410373*skew*skew;
			for (double exKurt=0.0; exKurt<1.0;exKurt+=.1) {
		        if (exKurt < bound) { 
		        	dispMessage("ClassGradeExperiment", "testFleishRangeOfVals", "!!!! Coefficient error : ex kurt : " + exKurt+ " is not feasible with skew :" + skew + " | Bounds is :"+bound,true);
		        } else {		        	
		        	dispMessage("ClassGradeExperiment", "testFleishRangeOfVals", "ex kurt : " + exKurt+ " is feasible with skew :" + skew + " | Bounds is :"+bound,true);
		        }
//				myRandGen flRandGen = buildAndInitRandGen(fleishRandGen_Uni, GL_QuadSlvrIDX, 256,new myProbSummary(new double[] {0,1,skew,exKurt},4, true));
//				_testFlTransform(flRandGen, 10000);
			}
		}
	}//testFleishRangeOfVals	
	
	//this will calculate the mapping from uniform for each class to a total mapping that follows some distribution
	public void calcMappingClassToTotal() {
		
		
	}
	
//	//calculate the inverse mapping for the raw grades in every class that is active
//	public void calcInverseMapping() {	
//		for (myClassRoster cls : classes) {
//			myRandGen randGen = gradeDistsPerClass.get(cls.name);
//			cls.setBaseDistModel(randGen);
//			cls.transformStudentGradesToUniform();
//		}
//	}//calcInverseMapping
	
	//this will calculate the values for the inverse cdf of the given gaussian, when fed 0->1
	public void calcInverseCDFSpan(double std) {
		myRandGen tmpRandGen = buildAndInitRandGen(ziggRandGen, new myProbSummary(new double[] {0.5,std,0,0},2));	
		int numVals = 100;
		double[] xVals = new double[numVals], resVals = new double[numVals];
		for (int i=0;i<numVals; ++i) {
			xVals[i] = (i+1)/(1.0*(numVals)+1);
			resVals[i] = tmpRandGen.inverseCDF(xVals[i]);
			dispMessage("ClassGradeExperiment","calcInverseCDFSpan","Xval ["+i+"] = " + String.format("%3.10f", xVals[i])+" -> Y : "  + String.format("%3.10f", resVals[i]),true);			
		}
	}//calcInverseCDFSpan
	
	//test inverse fleishman calculation
	public void testInvFleishCalc(double xDesired) {
		myRandGen gradeSourceDistGen = buildAndInitRandGen(fleishRandGen_UniVar, new myProbSummary(new double[] {0,1,1,4},4));
		((myFleishUniVarRandGen)gradeSourceDistGen).calcInvFuncVal(xDesired);

	}
	
	//this is called whenever screen width is changed - used to modify visualizations if necessary
	@Override
	protected void setVisWidth_Priv() {
		if(classRosters == null) {return;}
		for (myClassRoster cls : classRosters) {			cls.setDispWidth(visScreenWidth);		}
		finalGradeClass.setDispWidth(visScreenWidth);
	}//setVisWidth_Priv
	
	//check mouse over/click in 2d experiment - btn == -1 is mouse over
	@Override	
	public boolean checkMouseClickInExp2D(int msx, int msy, int btn) {		
		for (myClassRoster cls : classRosters) {	if ((cls.mseClickCheck(msx,msy, btn)) && (btn > -1)){		return true;		}}
		return ((finalGradeClass.mseClickCheck(msx,msy, btn)) && (btn > -1));
	}
	
	//check mouse over/click in 2d experiment - btn == -1 is mouse over
	@Override	
	public boolean checkMouseDragMoveInExp2D(int msx, int msy, int btn) {		
		for (myClassRoster cls : classRosters) {if ((cls.mseDragCheck(msx,msy, btn)) && (btn > -1)){		return true;		}}
		
		return ((finalGradeClass.mseDragCheck(msx,msy, btn)) && (btn > -1));
	}

	//check mouse over/click in 2d experiment; if btn == -1 then mouse over
	@Override	
	public void setMouseReleaseInExp2D() {	for (myClassRoster cls : classRosters) {	cls.mseRelease();}finalGradeClass.mseRelease();	}

	//called by base class call to buildSolvers, during base class ctor
	@Override
	protected void buildSolvers_indiv() {	//any solvers that are custom should be built here		
	}//buildSolvers_indiv	

	/////////////////////////////	
	//draw routines
	
	public void drawExp() {
		if(getFlag(showPlotsIDX)) {			drawPlotRes();		} 
		else {								drawExpRes();		}		
	}//drawExp
	private void drawPlotRes() {
		pa.pushMatrix();pa.pushStyle();
			for (myClassRoster cls : classRosters) {
				cls.drawPlotRes();
			}
			finalGradeClass.drawPlotRes();
	
		pa.popStyle();pa.popMatrix();
		
	}//drawPlotRes
	//draw raw and transformed class results
	private void drawExpRes() {
		pa.pushMatrix();pa.pushStyle();
			for (myClassRoster cls : classRosters) {
				cls.drawStudentGradesRaw();
				cls.drawStudentGradesUni();
				cls.drawRawToUniformLine();
				cls.drawTransToFinalLine(finalGradeClass);
			}
			finalGradeClass.drawStudentGradesRaw();
			finalGradeClass.drawStudentGradesUni();
			finalGradeClass.drawRawToUniformLine();
		
		pa.popStyle();pa.popMatrix();
	}//drawClassRes
		
	/////////////////////////////	
	//init and manage state flags
	public void setUseZScore(boolean val) {				setFlag(useZScoreFinalGradeIDX,val);	}	
	public void setRebuildDistOnGradeMod(boolean val) {	setFlag(rebuildDistOnGradeModIDX, val);	}
	private static int[] rebuildFlags = new int[] {useZScoreFinalGradeIDX,rebuildDistOnGradeModIDX};
	private void refreshBuildStateFlags() {
		for (int idx : rebuildFlags) {			setFlag(idx, getFlag(idx));		}
	}
	
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
			case useZScoreFinalGradeIDX : {//whether to use the ZScore calc or the specified target final grade distirbution for final grades
				if(null != finalGradeClass) {	finalGradeClass.setUseZScore(val);}				
				break;}
			case rebuildDistOnGradeModIDX : {
				for (myClassRoster cls : classRosters) {cls.setRebuildDistWhenMove(val);}
				finalGradeClass.setRebuildDistWhenMove(val);
				break;}
			case showPlotsIDX : {
				if (val) {
					
				} else {
					
				}
				
			break;}
		}
	}//setFlag		
	@Override
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}

}//class ClassGradeExperiment

