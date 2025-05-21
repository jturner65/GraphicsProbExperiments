package experiments_PKG.classGradeExp.experiment;

import java.util.*;

import base_Render_Interface.IRenderInterface;
import base_ProbTools.*;
import base_ProbTools.randGenFunc.gens.myFleishUniVarRandGen;
import base_ProbTools.randGenFunc.gens.base.Base_RandGen;
import base_StatsTools.summary.myProbSummary_Dbls;
import base_Utils_Objects.dataAdapter.Base_DataAdapter;
import experiments_PKG.classGradeExp.roster.myClassRoster;
import experiments_PKG.classGradeExp.roster.myFinalGradeRoster;
import experiments_PKG.classGradeExp.ui.Grade2DUIDataUpdater;
import experiments_PKG.classGradeExp.ui.Grade2DWindow;
/**
 * this class is a specialized type of probability experiment manager, specifically for the class grades project
 * @author john
 * @param 
 */
public class ClassGradeExperiment extends baseProbExpMgr{
	public static IRenderInterface ri;
	//Owning window
	protected Grade2DWindow win;
	//structure holding all classes
	private HashSet<myClassRoster> classRosters;
	private int numClasses;
	//final grade class is class representing the final grade (not a real class)
	private myFinalGradeRoster finalGradeClass;
	
	//structure holding all students
	private HashMap<Integer,myStudent> students;
	private int numStudents;
	
	//initial grade distribution moments-  for gaussian to draw grades from (i.e. "unknown" underlying grade population)
	private final double[] initGradeMoments = new double[] {0.5,.25,0,0};
	
	//structure holding grades for a particular class read in from a file, or otherwise synthesized from some unknown distribution.  is keyed by class name and then by student ID
	private HashMap<String,HashMap<Integer, Double>> perClassStudentGrades;
	//summary objects for each class, based on loaded grades
	private HashMap<String, myProbSummary_Dbls> perClassSummaryObjMap;
		
	//types of different transformed grades
	public static final int
		rawGradeIDX					= 0,
		normTransGradeIDX			= 1,
		scaleTransformGradeIDX		= 2;
	public static final int numGradeTypes = 3;	
	
	
	//experiment-specific state flag idxs - bits in array holding relevant process info
	public static final int
			debugIDX 					= 0,
			useZScoreFinalGradeIDX		= 1,
			rebuildDistOnGradeModIDX 	= 2,
			showPlotsIDX				= 3,			//if true show each class distribution plot, if false show class bar
			stdntsHaveCurGradesIDX 	 	= 4,			//whether or not the current grade set has been distributed to the current students
			classesHaveCurStdntsIDX		= 5,			//whether current students have been sent to current class distribution
			gradesNeedRecalcIDX			= 6;			//raw grades for each student need to be recalculated due to class/student structure size change or user request
	public static final int numFlags = 7;	
	
	//display-related values
	public static final float 
		distBtwnAdjBars = 75.0f, 
		distBtwnRawTransBars = 550.0f,
		distBtwnAdjPlots = 300.0f;
	//where first class bar starts relative to top left corner of display window
	private static float[] classBarStart = new float[] {10,50};
	private static float[] classPlotStart = new float[] {10,50};
	
	public ClassGradeExperiment(Grade2DWindow _win, float[] _curWinVisScrDims) {
		super(_win.getMsgObj(), _curWinVisScrDims);
		win = _win; ri = Grade2DWindow.ri;		
		
	}//ctor
	
	//called at end of ctor and whenever experiment needs to be re-instanced
	@Override
	public void initExp() {
		classRosters = new HashSet<myClassRoster>();
		numClasses = 0;
		students = new HashMap<Integer,myStudent>();
		numStudents = 0;
		//build initial final grade "class" that is used to aggregate and inverse-map to some target distribution
		finalGradeClass = buildFinalGradeRoster(getPlotHeight());
	}//	initExp

	@Override
	protected Base_DataAdapter initUIDataUpdater(Base_DataAdapter dataUpdate) {
		return new Grade2DUIDataUpdater((Grade2DUIDataUpdater)dataUpdate);
	}

	@Override
	protected void updateUIDataValues_Priv() {
		//TODO use this to update all UI values from Grade2DWindow
	}
	
	
	//called by base class call to buildSolvers, during base class ctor
	@Override
	protected void buildSolvers_Indiv() {	//any solvers that are custom should be built here		
	}//buildSolvers_indiv	
	
	//update all rand gen objects for this function, including updating rand var funcs
	@Override
	protected void updateAllRandGens_Priv() {
		finalGradeClass.setRVFOptionFlags(randAlgOptSettings);
		for (myClassRoster _cls : classRosters) {_cls.setRVFOptionFlags(randAlgOptSettings); _cls.updateAllDistsAndGrades();}		
		finalGradeClass.updateAllDistsAndGrades();
	}//

	
	/////////////////////////////////////
	// 	file-based grading experiment
	
	//primary entry point for experiment using students and class/grade rosters from file names 	//build roster of classes, students and grades from passed file names
	//_finalGradeVals : idx 0 : up to first 4 moments of final grade target mapping; idx 1 : min/max of target mapping
	//_finalGradeDescs : idx 0 : # of moments final grade mapping dist should use, idx 1 : type of myRandGen ; idx 2 : type of myRandFunc to use
	public void buildFileBasedGradeExp(String _studentFileName, String[] _classRosterFileNames, int expType, double[][] _finalGradeVals, int[] _finalGradeDescs) {	//1 roster per class
		msgObj.dispInfoMessage("ClassGradeExperiment","buildFileBasedGradeExp","Start loading students from " + _studentFileName+" and all grades for " + _classRosterFileNames.length+" classes.");
		//reset class and student structures
		//build students
		String [] studentNames = loadSpecifiedStudents(_studentFileName);
		_buildNewStudents(studentNames);
		//build classes and per-class/per-student grades
		loadAllClassData(_classRosterFileNames);
		//above builds all class summary objs and per class grades for each student
		String [] classNames = perClassStudentGrades.keySet().toArray(new String[0]);
		_buildNewClasses(classNames);
		//after classes are built gradesNeedRecalcIDX will be true, but grades and classes are already linked
		setStntClassRebuildFlags();			//students have not received current grade distribution;classes have not had students assigned; grades need to be rebuilt <- last one we counteract
		setFlag(gradesNeedRecalcIDX, false);	
		//set desired final mapping - TODO build this from UI entries
		//setDesiredFinalGradeSummaryObj(new double[] {0.75,.2,0,0}, new double[] {0.0,1.0}, 2);
		setDesiredFinalGradeSummaryObj(_finalGradeVals, _finalGradeDescs);
		//assign current student roster to current class rosters, and final class roster, and assign all raw grades to students
		assignGradesToStudentsAndStudentsToClasses();
		//TODO must perform initial final grade mapping here
		
		//assign desired transformation/description distribution to each class
		assignGradeTransformation(expType);
		
		msgObj.dispInfoMessage("ClassGradeExperiment","buildFileBasedGradeExp","Finished loading students from " + _studentFileName+" and all grades for " + _classRosterFileNames.length+" classes.");		
	}//buildFileBasedGradeExp
	
	//load individual class's grades into per-class map
	private void loadAllClassData(String[] _classRosterFileNames) {
		msgObj.dispInfoMessage("ClassGradeExperiment","loadSpecifiedStudentGrades","Start loading grades from all  " + _classRosterFileNames.length + " classes.");
		//all student grades for each class
		perClassStudentGrades = new HashMap<String,HashMap<Integer, Double>>();
		//object describing class distribution
		perClassSummaryObjMap = new HashMap<String, myProbSummary_Dbls>();
		for(String clsFileName : _classRosterFileNames) {
			msgObj.dispInfoMessage("ClassGradeExperiment","loadSpecifiedStudentGrades","Start loading grades for class from " + clsFileName);
			//perClassStudentGrades and perClassSummaryObjMap gets populated here
			//results from reading file clsFileName from FileIO object
			String[] classFileRes = new String[1];
			//get from classFileRes
			String className = classFileRes[0];
			
			HashMap<Integer, Double> classGrades = perClassStudentGrades.get(className);
			if (null == classGrades) {classGrades = new HashMap<Integer, Double>(); perClassStudentGrades.put(className, classGrades);}
			//to hold for distribution - all students must be built by here
			double[] vals = new double[students.size()];
			//int idx =0;			
			//TODO get values from file and give to each student
			//overwrites grade in structure
	//		for (myStudent s : students.values()) {
	//			//replace this with source of class data - reading file, building from "unknown" distribution, etc.
	//			double grade = tmpRandGen.getSample();
	//			while ((grade < 0) || (grade > 1)) {					grade = tmpRandGen.getSample();			} 			
	//			vals[idx++]=grade;
	//			classGrades.put(s.ObjID, grade);
	//		}//for each student assign a random grade and add them to class roster			
			//build summary object from vals
			myProbSummary_Dbls summaryObj = new myProbSummary_Dbls(vals);
			msgObj.dispInfoMessage("ClassGradeExperiment","loadAllClassData","Built Summary object with following stats : " + summaryObj.getMinNumMmntsDesc());
			perClassSummaryObjMap.put(className, summaryObj);
			msgObj.dispInfoMessage("ClassGradeExperiment","loadAllClassData","Finished loading grades for class from " + clsFileName);
		}
		msgObj.dispInfoMessage("ClassGradeExperiment","loadAllClassData","Finished loading grades from all  " + _classRosterFileNames.length + " classes.");
	}//loadSpecifiedStudentGrades
	
	private String[] loadSpecifiedStudents(String _studentFileName) {
		String [] studentNames = new String[0];
		//get from file IO TODO
		
		return studentNames;
		
	}//loadSpecifiedStudents
	
	//this will save the current students, classes and raw grade values to disk
	public void saveCurrExpState() {
		
	}//saveCurrExpState
	
	//this will update global grade values to be current grade values
	public void updateGlblGrades() {
		for (myClassRoster _cls : classRosters) {
			HashMap<Integer, Double> classGrades = perClassStudentGrades.get(_cls.name);
			if (null == classGrades) {classGrades = new HashMap<Integer, Double>(); perClassStudentGrades.put(_cls.name, classGrades);}
			//current student raw grades
			double[] vals = new double[students.size()];
			int idx =0;			
			//get values from file
			//overwrites grade in structure
			for (myStudent s : students.values()) {
				//replace this with source of class data - reading file, building from "unknown" distribution, etc.
				double grade = s.getRawGrade(_cls);
				vals[idx++]=grade;
				classGrades.put(s.ObjID, grade);
			}//for each student assign a random grade and add them to class roster			
			//build summary object from vals
			myProbSummary_Dbls summaryObj = new myProbSummary_Dbls(vals);
			msgObj.dispInfoMessage("ClassGradeExperiment","updateGlblGrades","Built Summary object for class : " + _cls.name+ " with following stats  : " + summaryObj.getMinNumMmntsDesc());
			perClassSummaryObjMap.put(_cls.name, summaryObj);
		}
	}//updateGlblGrades
	
	/////////////////////////////////////
	// 	end file-based grading experiment
	
	/////////////////////////////////////
	//  randomly generated grading experiment	
	
	//primary entry point for experiment using random grade distributions
	//_finalGradeVals : idx 0 : up to first 4 moments of final grade target mapping; idx 1 : min/max of target mapping
	//_finalGradeDescs : idx 0 : # of moments final grade mapping dist should use, idx 1 : type of myRandGen ; idx 2 : type of myRandFunc to use
	public void buildRandGradeExp(boolean[] flags, int[] counts, int expType, double[][] _finalGradeVals, int[] _finalGradeDescs) {
		String dispInfoMessage = (flags[0] ? "rebuild "+ counts[0] + " random students" : "map "+ numStudents + " current students to ") + (flags[1] ? ""+ counts[1] + "rebuilt classes" : numClasses+ " current classes") + " using "+ (flags[2] ? "new random generated grades" : "existing grades") ;
		msgObj.dispInfoMessage("ClassGradeExperiment","buildRandGradeExp","Start " + dispInfoMessage);		
		//idx 0 == students rebuilt
		if(flags[0]) {											buildNewRandomStudents(counts[0]);}//new students
		//idx 1 == classes rebuilt
		if(flags[1]) {											buildNewRandomClasses(counts[1]);}//new students
		//idx 2 == grades rebuilt
		if((flags[2]) || (getFlag(gradesNeedRecalcIDX))) {		
			buildRandomStudentGrades();	
			//set desired final mapping - TODO build this from UI entries
			setDesiredFinalGradeSummaryObj(_finalGradeVals, _finalGradeDescs);
		}//grades have been recalced		
		
		//assign current student roster to current class rosters, and final class roster, and assign all raw grades to students
		assignGradesToStudentsAndStudentsToClasses();		
		//assign desired transformation/description distribution to each class
		assignGradeTransformation(expType);		
		
		msgObj.dispInfoMessage("ClassGradeExperiment","buildRandGradeExp","Finished " + dispInfoMessage);
	}//buildRandomGradeExperiment
	
	//build list of students with randomly assigned names - to be replaced by file loading all student names
	private String[] _buildRandStudentNameList(int _numStudents) {
		String[] res = new String[_numStudents];
		for (int i=0;i<res.length;++i) {			res[i]="Student : " +i;	}
		return res;
	}//buildStudentList_Random
		
	//build set of random students
	private void buildNewRandomStudents(int _numStudents) {
		msgObj.dispInfoMessage("ClassGradeExperiment","buildNewRandomStudents","Start Building "  +_numStudents + " random students.");
		String[] studentNames = _buildRandStudentNameList(_numStudents);
		_buildNewStudents(studentNames);
		msgObj.dispInfoMessage("ClassGradeExperiment","buildNewRandomStudents","Finished Building "  +_numStudents + " random students.");
	}//buildNewRandomStudents	

	//random classes
	private void buildNewRandomClasses(int _numClasses) {
		msgObj.dispInfoMessage("ClassGradeExperiment","buildNewRandomClasses","Start Building "  +_numClasses + " random classes.");
		String[] classNames = new String[_numClasses];
		for (int i=0;i<classNames.length;++i) {classNames[i]="Class : " + (i+1);}
		_buildNewClasses(classNames);		
		msgObj.dispInfoMessage("ClassGradeExperiment","buildNewRandomClasses","Finished Building "  +_numClasses + " random classes.");
	}//buildNewRandomClasses
		
	//derive random student grades for each class and build prob summary objs of grade sample dists per class
	private void buildRandomStudentGrades() {
		msgObj.dispInfoMessage("ClassGradeExperiment","buildRandomStudentGrades","Start sampling "  +students.size() + " random student grades for " + classRosters.size() +" classes");
		perClassStudentGrades = new HashMap<String,HashMap<Integer, Double>>();
		perClassSummaryObjMap = new HashMap<String, myProbSummary_Dbls>();
		
		//tmp dist for use with generating raw grades - since raw grades are rejected outside the range 0-1 the final dist will be somewhat different
		myProbSummary_Dbls tmp = new myProbSummary_Dbls(initGradeMoments,2);
		tmp.setMinMax(0.0, 1.0);
		int _randVarType = gaussRandVarIDX;
		Base_RandGen tmpRandGen = buildAndInitRandGen(ziggRandGen, _randVarType, tmp);		
		
		//for every class for every student, derive a random grade
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
				while ((grade < 0) || (grade > 1)) {					grade = tmpRandGen.getSample();			} 			
				vals[idx++]=grade;
				classGrades.put(s.ObjID, grade);
			}//for each student assign a random grade and add them to class roster			
			//build summary object from vals
			myProbSummary_Dbls summaryObj = new myProbSummary_Dbls(vals);
			msgObj.dispInfoMessage("ClassGradeExperiment","buildRandomStudentGrades","Built Summary object for class : " + _cls.name+ " with following stats : " + summaryObj.getMinNumMmntsDesc());
			perClassSummaryObjMap.put(_cls.name, summaryObj);
		}
		setStntClassRebuildFlags();			//students have not received current grade distribution;classes have not had students assigned; grades need to be rebuilt <- last one we counteract
		//grades have been recalced
		setFlag(gradesNeedRecalcIDX, false);
		msgObj.dispInfoMessage("ClassGradeExperiment","buildRandomStudentGrades","Finished sampling "  +students.size() + " random student grades for " + classRosters.size() +" classes");
	}//buildRandomStudentGrades
	
	/////////////////////////////////////
	//  end randomly generated grading experiment	
	
	//rebuild final grade distribution mapping based on passed type, momments and minMax - must be a model type that supports not having samples only momments
	//_descVals : idx 0 == # mmnts used, idx 1 == _randGenType, idx 2 == _randVarType
	private void setDesiredFinalGradeSummaryObj(double[][] _mmntsAndMinMax, int[] _descVals) {
		String mmtntsStr = "[";
		for(int i=0;i<_descVals[0]-1;++i) {			mmtntsStr += ""+_mmntsAndMinMax[0][i]+", ";		}
		mmtntsStr += ""+_mmntsAndMinMax[0][_descVals[0]-1]+"]";
		msgObj.dispInfoMessage("ClassGradeExperiment","setDesiredFinalGradeSummaryObj","Start setting final grade summary object with "+_descVals[0]+" moments : " + mmtntsStr+".");
		
		//int _numMmnts = _descVals[0];int _randGenType = _descVals[1];int _randVarType = _descVals[2];
		double[] mmnts = new double[_descVals[0]], minMax =  new double[_mmntsAndMinMax[1].length] ;
		System.arraycopy(_mmntsAndMinMax[0], 0, mmnts, 0, _descVals[0]);
		System.arraycopy(_mmntsAndMinMax[1], 0, minMax, 0, _mmntsAndMinMax[1].length);		
		//kurtosis from UI is excess kurtosis
		myProbSummary_Dbls tmpFinal = new myProbSummary_Dbls(mmnts,_descVals[0], true);
		tmpFinal.setMinMax(minMax);
		perClassSummaryObjMap.put(finalGradeClass.name, tmpFinal);			
		Base_RandGen tmpFinalRandGen = buildAndInitRandGen(_descVals[1], _descVals[2], perClassSummaryObjMap.get(finalGradeClass.name));
		finalGradeClass.setBaseDistModel(tmpFinalRandGen);		
		msgObj.dispInfoMessage("ClassGradeExperiment","setDesiredFinalGradeSummaryObj","Finished setting final grade summary object with "+_descVals[0]+" moments : " + mmtntsStr+".");
		
	}//buildNewDesiredFinalGradeRandGen
	
	//should only be called after grades have been set and mappings to uniform per grade have been performed
	//call only from UI when values change
	//_mmntsAndMinMax : idx 0 : up to first 4 moments of final grade target mapping; idx 1 : min/max of target mapping
	//_descVals : idx 0 : # of moments final grade mapping dist should use, idx 1 : type of myRandGen ; idx 2 : type of myRandFunc to use
	public void rebuildDesFinalGradeMapping(double[][] _mmntsAndMinMax, int[] _descVals) {
		setDesiredFinalGradeSummaryObj(_mmntsAndMinMax,_descVals);
		//now must reperform mapping from uniform for each class
		msgObj.dispInfoMessage("ClassGradeExperiment","rebuildDesFinalGradeMapping","Start calculating final grade mapping of student grades.");
		finalGradeClass.calcTotalGrades();
		msgObj.dispInfoMessage("ClassGradeExperiment","rebuildDesFinalGradeMapping","Finished calculating final grade mapping of student grades.");
	}//rebuildDesFinalGradeMapping	
	
	/**
	 * build experimental transformation and assign grades to students - must be called after students, classes and grades have been loaded/generated, and all students assigned to classes and all raw grades assigned to students
	 * @param expTypeIDX : index of experimental list we are using : "Linear", "Uniform Spaced","Gaussian","Fleishman Poly","Cosine Mmnts derived","Cosine CDF derived"
	 */
	private void assignGradeTransformation(int _expTypeIDX) {		
		if((_expTypeIDX < 0) || (_expTypeIDX >= expType.length)){			msgObj.dispErrorMessage("ClassGradeExperiment","buildClassGradeExperiment","Attempting to build experiment for unknown transformational model type IDX : " + _expTypeIDX + ". Aborting.");return;}
		msgObj.dispInfoMessage("ClassGradeExperiment","buildClassGradeExperiment","Start assigning students to classes, grades to students, and "+ expType[_expTypeIDX]+" model of grades distribution to each grade.");
		//Final Grades mappings have to be specified first, before this function has been called
		switch(_expTypeIDX) {
			case 0 : { 		//gaussian model - inverse CDF to map to uniform space
				setRandGensAndTransformGrades(ziggRandGen, gaussRandVarIDX);		
				break;}
			case 1 : {		//linear mapping to uniform space
				setRandGensAndTransformGrades(linearTransformMap,-1);
				break;}
			case 2 : { 		//uniformly spaced into buckets
				setRandGensAndTransformGrades(uniformTransformMap,-1);
				break;}
			case 3 : { 		//Fleishman polynomial model - inverse CDF to map to uniform space
				setRandGensAndTransformGrades(fleishRandGen_UniVar,fleishRandVarIDX);  
				break;}
			case 4 : { 		//cosine model with eq built from momments - inverse CDF to map to uniform space
				setRandGensAndTransformGrades(boundedRandGen, raisedCosRandVarIDX);	
				break;}
			case 5 : { 		//cosine model with eq built CDF of given data
				setRandGensAndTransformGrades(boundedRandGen, cosCDFRandVarIDX);	//TODO this needs to be changed from cosRandVarIDX to cosCDFRandVarIDX
				break;}
			default : {}		//unknown type
		}//switch
		refreshBuildStateFlags();
		msgObj.dispInfoMessage("ClassGradeExperiment","buildClassGradeExperiment","Finished assigning students to classes, grades to students, and "+ expType[_expTypeIDX]+" model of grades distribution to each grade.");
	}//buildClassGradeExperiment
	
	
	//use passed randGenType to build modeling distributions of loaded/random grades - model will be of this type; possible models are specified in 
	//BaseProbExpMgr at tag "types of random number generators implemented/supported so far"
	//_randVarType : type of random variable to use for _randGenType generator - currently only zig randGenType uses this value
	public void setRandGensAndTransformGrades(int _randGenType, int _randVarType) {		
		//final grades need to be specified first - before this function is called
		//determine specific options for random variable function, based on current UI configuration
		for (myClassRoster _cls : classRosters) {	
			Base_RandGen gen = buildAndInitRandGen(_randGenType, _randVarType, perClassSummaryObjMap.get(_cls.name));
			_cls.setBaseDistModel(gen);
			_cls.transformStudentGradesToUniform();			//calculates total grades as well
		}
	}//setAllRosterTransforms
	
	//assign current students to current classes
	private void assignGradesToStudentsAndStudentsToClasses() {
		if(!getFlag(classesHaveCurStdntsIDX)){	//current students have not been assigned to classes
			finalGradeClass.setStudents(students);
			for (myClassRoster _cls : classRosters) {				_cls.setStudents(students);				}
			setFlag(classesHaveCurStdntsIDX, true);		//classes have current students
		}
		if(getFlag(gradesNeedRecalcIDX)) {//need to rebuild grades before attempting to assign them
			msgObj.dispWarningMessage("ClassGradeExperiment","assignGradesToStudentsAndStudentsToClasses","Need to rebuild grades before attempting to assign them to students.");
			return;
		}
		if (!getFlag(stdntsHaveCurGradesIDX)) {//students have not received current grade distribution;
			//set grades for each student, in each class
			for (myClassRoster _cls : classRosters) {				_cls.setAllStudentRawGrades(perClassStudentGrades.get(_cls.name));	}	
			setFlag(stdntsHaveCurGradesIDX, true);		//students have now received current grade distribution;
		}
	}//assignStudentsToClasses
	
	private myFinalGradeRoster buildFinalGradeRoster(float heightOfFinalPlot) {
		//build final grade roster
		float[] finalUniBarLocSt = new float[] {classBarStart[0], curWinVisScrDims[1]- distBtwnAdjBars},
				finalTransBarLocSt = new float[] {classBarStart[0], curWinVisScrDims[1] - 3*distBtwnAdjBars}, 
				finalPlotLocSt = new float[] {classPlotStart[0], curWinVisScrDims[1] - heightOfFinalPlot , heightOfFinalPlot}
				;		

		return new myFinalGradeRoster(Grade2DWindow.ri, this, "Final Grades For All Students", new float[][] { finalUniBarLocSt, finalTransBarLocSt,finalPlotLocSt});
	}//buildFinalGradeRoster

	//pass names of students, either from a file or randomly generated
	private void _buildNewStudents(String[] studentNames) {
		numStudents = studentNames.length;
		msgObj.dispInfoMessage("ClassGradeExperiment","_buildNewStudents","Start building " + numStudents +" students");	
		students.clear();
		myStudent stdnt;
		for (int i=0;i<numStudents;++i) {
			int[] _clr = Grade2DWindow.ri.getRndClrBright(255);
			stdnt = new myStudent(_clr, studentNames[i]);
			students.put(stdnt.ObjID, stdnt);			
		}	
		setStntClassRebuildFlags();			//students have not received current grade distribution;classes have not had students assigned; grades need to be rebuilt
		msgObj.dispInfoMessage("ClassGradeExperiment","_buildNewStudents","Finished building " + numStudents +" students");				
	}//_buildNewStudents	
	
	//build new class structures, if called for - requires new grade derivation and assignment, as well as assigning students to classes
	private void _buildNewClasses(String[] _className) {
		numClasses = _className.length;
		msgObj.dispInfoMessage("ClassGradeExperiment","_buildNewClasses","Start building " + numClasses + " new classes and final grade \"class\".");	
		//set up final grade "class"
		float heightOfPlots =  getPlotHeight();
		finalGradeClass = buildFinalGradeRoster(heightOfPlots);	
		finalGradeClass.setUseZScore(getFlag(useZScoreFinalGradeIDX));
		myClassRoster cls;
		//start location x,y for raw and transformed bars
		float[] rawBarLocSt = new float[] {classBarStart[0],classBarStart[1]},
				transBarLocSt = new float[] {classBarStart[0],classBarStart[1] + distBtwnRawTransBars},
				plotRectLocSt = new float[] {classPlotStart[0],classPlotStart[1], heightOfPlots};
		classRosters.clear();
		for (int i=0;i<numClasses;++i) {
			cls = new myClassRoster(Grade2DWindow.ri, this, _className[i], new float[][] { rawBarLocSt, transBarLocSt,plotRectLocSt});	//build class
			cls.setFinalGradeRoster(finalGradeClass);																				//give class final grade "class"
			classRosters.add(cls);																									//add class to roster
			//move to next class's bar
			rawBarLocSt[1] += distBtwnAdjBars;
			transBarLocSt[1] += distBtwnAdjBars;
			plotRectLocSt[1] += heightOfPlots;
		}//for numClasses
		
		setStntClassRebuildFlags();			//students have not received current grade distribution;classes have not had students assigned; grades need to be rebuilt
		msgObj.dispInfoMessage("ClassGradeExperiment","_buildNewClasses","Finished building " + numClasses + " new classes and final grade \"class\".");	
	}//buildNewClasses

	
	////////////////////////////////////
	// tests
	
	//test efficacy of fleishman polynomial transform
	public void testFleishTransform() {

		Base_RandGen gradeSourceDistGen = buildAndInitRandGen(fleishRandGen_UniVar, fleishRandVarIDX, new myProbSummary_Dbls(new double[] {0,1,1,4},4));
		//test fleishman polynomial-based transformation
		_testFlTransform(gradeSourceDistGen, 10000);
		double min = -1, max = 1;
		double area = gradeSourceDistGen.testInteg(min,max);
		msgObj.dispInfoMessage("ClassGradeExperiment","testFleishTransform","area under fleish poly from "+min+"->"+max+" : " + area);
	}//
	
	private void _testFlTransform(Base_RandGen flRandGen, int numVals) {
		//test fleishman polynomial-based transformation
		msgObj.dispInfoMessage("ClassGradeExperiment","_testFlTransform","Specified summary for fleishman polynomial : " + flRandGen.getSummary().getMomentsVals());
		double[] testData = new double[numVals];
		for(int i=0;i<testData.length;++i) {
			testData[i] = flRandGen.getSample();
		}
		myProbSummary_Dbls testSummary = new myProbSummary_Dbls(testData);
		msgObj.dispInfoMessage("ClassGradeExperiment","_testFlTransform","Analysis res of testSummary for fleishman polynomial : " + testSummary.getMomentsVals());
		
	}
	
	//vary fleishman transformation values to see when it works and when it doesn't
	public void testFleishRangeOfVals() {
		for (double skew=0.0; skew<1.0;skew+=.1) {
	        double bound = -1.2264489 + 1.6410373*skew*skew;
			for (double exKurt=0.0; exKurt<1.0;exKurt+=.1) {
		        if (exKurt < bound) { 
		        	msgObj.dispWarningMessage("ClassGradeExperiment", "testFleishRangeOfVals", "!!!! Coefficient error : ex kurt : " + exKurt+ " is not feasible with skew :" + skew + " | Bounds is :"+bound);
		        } else {		        	
		        	msgObj.dispInfoMessage("ClassGradeExperiment", "testFleishRangeOfVals", "ex kurt : " + exKurt+ " is feasible with skew :" + skew + " | Bounds is :"+bound);
		        }
//				myRandGen flRandGen = buildAndInitRandGen(fleishRandGen_Uni, GL_QuadSlvrIDX, 256,new myProbSummary(new double[] {0,1,skew,exKurt},4, true));
//				_testFlTransform(flRandGen, 10000);
			}
		}
	}//testFleishRangeOfVals	
	
	public void testCosFunction() {//generate cdf values given uniform input
    	msgObj.dispInfoMessage("ClassGradeExperiment", "testCosFunction", "Starting test cosine function : generate CDF values given uniform values.");
    	Base_RandGen cosFuncTestGen = buildAndInitRandGen(boundedRandGen, raisedCosRandVarIDX, new myProbSummary_Dbls(new double[] {0,1,0,0},2));
    	TreeMap<Double,Double> genVals = new TreeMap<Double,Double>();
    	msgObj.dispInfoMessage("ClassGradeExperiment", "testCosFunction", "p \t\t| val");
    		
    	for(Double i = 0.0; i<=1.0f;i+=.00001) {
    		genVals.put(i, cosFuncTestGen.inverseCDF(i));
    		msgObj.dispInfoMessage("ClassGradeExperiment", "testCosFunction", String.format("%.8f",i)+"," + String.format("%.8f",genVals.get(i)));
    	}
    	
		
    	msgObj.dispInfoMessage("ClassGradeExperiment", "testCosFunction", "Finished test cosine function: generate CDF values given uniform values.");
	}//testCosFunction
	
	
	//test inverse fleishman calculation
	public void testInvFleishCalc(double xDesired) {
		Base_RandGen gradeSourceDistGen = buildAndInitRandGen(fleishRandGen_UniVar, fleishRandVarIDX, new myProbSummary_Dbls(new double[] {0,1,1,4},4));
		double val = ((myFleishUniVarRandGen)gradeSourceDistGen).calcInvFuncVal(xDesired);
		msgObj.dispInfoMessage("ClassGradeExperiment", "testInvFleishCalc", "Finished test Fleishman Inverse Function Val Calc : "+val);
	}//testInvFleishCalc
	
	//////////////////////////
	// plots
	
	//return estimate of individual plot value height based on # of classes
	private float getPlotHeight() {return .95f * curWinVisScrDims[1]/(numClasses + 1);}

	
	public void clearAllPlotEval() {
		for (myClassRoster _cls : classRosters) {			_cls.clearPlotEval();	}
		finalGradeClass.clearPlotEval();	
		setShowPlots(false);
	}//clearAllPlotEval

	//this will build and set a reduced cosine rand gen for the passed class and then restore current model type
	private void _buildCosRandGenForTest(myClassRoster _cls) {
		String curClsMdl = _cls.getCurDistModel();
		Base_RandGen cosgen = buildAndInitRandGen(boundedRandGen, raisedCosRandVarIDX, perClassSummaryObjMap.get(_cls.name));
		_cls.setBaseDistModel(cosgen);
		_cls.setCurDistModel(curClsMdl);
	}
	
	//this will evaluate data for specified plots and create an overlay
	public void evalCosAndNormWithHist(int numVals, int numBuckets, double low, double high) {
		for (myClassRoster _cls : classRosters) {		
			//build cosine rand gen and assign to class but restore appropriate current rand gen
			msgObj.dispInfoMessage("ClassGradeExperiment", "evalCosAndNormWithHist", "Class : " + _cls.name);
			_buildCosRandGenForTest(_cls);	
			_cls.evalCosAndNormWithHist(numVals, numBuckets, low, high);	
		}
		_buildCosRandGenForTest(finalGradeClass);
		finalGradeClass.evalCosAndNormWithHist(numVals, numBuckets, low, high);	
		setShowPlots(true);
		
	}//
	public void dbgTestStuff() {
		msgObj.dispInfoMessage("ClassGradeExperiment", "dbgTestStuff", "Start test");
		//Base_RandGen gen = buildAndInitRandGen(linearTransformMap, -1, new myProbSummary(new double[] {0,1,1,4},2));		
		Base_RandGen gen = buildAndInitRandGen(uniformTransformMap, -1, new myProbSummary_Dbls(new double[] {0,1,1,4},2));
		double[] vals = gen.getMultiFastSamples(100000);
		
		myProbSummary_Dbls tmpSummary = new myProbSummary_Dbls(vals); 
		
		
		msgObj.dispInfoMessage("ClassGradeExperiment", "dbgTestStuff", "Finish test : summary : "+ tmpSummary.toString());
	}

	//derive and show plots of different distributions behind each class calc
	public void evalPlotClassFuncs(int funcType, int numVals, double low, double high) {		
		for (myClassRoster _cls : classRosters) {		_cls.evalAndPlotFuncRes(numVals, low, high, funcType);	}	
		finalGradeClass.evalAndPlotFuncRes(numVals, low, high, funcType);	
		setShowPlots(true);
	}//evalPlotClassDists
	
	//derive and show plots of different distributions behind each class calc
	public void evalPlotClassHists(int numVals, int numBuckets, double low, double high) {
		for (myClassRoster _cls : classRosters) {			_cls.evalAndPlotHistRes(numVals, numBuckets);	}
		finalGradeClass.evalAndPlotHistRes(numVals, numBuckets);				
		setShowPlots(true);
	}//evalPlotClassDists
	
	
	//whether or not to show plots for all specified distributions
	public void setShowPlots(boolean val) {setFlag(showPlotsIDX,val);	}

	/////////////////////////////	
	//mouse and draw routines
	
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
		//if here then not in any class roster
		return ((finalGradeClass.mseDragCheck(msx,msy, btn)) && (btn > -1));
	}

	//check mouse over/click in 2d experiment; if btn == -1 then mouse over
	@Override	
	public void setMouseReleaseInExp2D() {	for (myClassRoster cls : classRosters) {	cls.mseRelease();}finalGradeClass.mseRelease();	}
	//draw experimental results
	@Override
	public void drawExp() {
		if(getFlag(showPlotsIDX)) {			drawPlotRes();		} 
		else {								drawExpRes();		}		
	}//drawExp
	
	
	protected void drawPlotRes() {
		ri.pushMatState();
			for (myClassRoster cls : classRosters) {
				cls.drawPlotRes();
			}
			finalGradeClass.drawPlotRes();
	
		ri.popMatState();
		
	}//drawPlotRes
	//draw raw and transformed class results
	protected void drawExpRes() {
		ri.pushMatState();
			for (myClassRoster cls : classRosters) {
				cls.drawStudentGradesRaw();
				cls.drawStudentGradesUni();
				cls.drawRawToUniformLine();
				cls.drawTransToFinalLine(finalGradeClass);
			}
			finalGradeClass.drawStudentGradesRaw();
			finalGradeClass.drawStudentGradesUni();
			finalGradeClass.drawRawToUniformLine();
		
		ri.popMatState();
	}//drawClassRes
		
	/////////////////////////////	
	//init and manage state flags
	public void setUseZScore(boolean val) {				setFlag(useZScoreFinalGradeIDX,val);	}	
	public void setRebuildDistOnGradeMod(boolean val) {	setFlag(rebuildDistOnGradeModIDX, val);	}
	//flags that all are set to the same value whenever students or classes are rebuilt
	private static int[] rebuildStdntFlags = new int[] {stdntsHaveCurGradesIDX,classesHaveCurStdntsIDX};
	//set flags to false based on current student and/or class structure being rebuilt
	private void setStntClassRebuildFlags() {	for (int idx : rebuildStdntFlags) {			setFlag(idx, false);		};setFlag(gradesNeedRecalcIDX, true);}
	
	//flags that must be reset whenever class or student structures change, so that their values can be propagated out to structures
	private static int[] rebuildAllFlags = new int[] {useZScoreFinalGradeIDX,rebuildDistOnGradeModIDX};
	//re-send current values to flags so any actions can be retaken
	private void refreshBuildStateFlags() {
		for (int idx : rebuildAllFlags) {			setFlag(idx, getFlag(idx));		}
	}
	
	@Override
	protected void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	@Override
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX : {break;}		
			case useZScoreFinalGradeIDX : {		//whether to use the ZScore calc or the specified target final grade distirbution for final grades
				if(null != finalGradeClass) {	finalGradeClass.setUseZScore(val);}				
				break;}
			case rebuildDistOnGradeModIDX : {
				if(null == classRosters) {break;}
				for (myClassRoster cls : classRosters) {cls.setRebuildDistWhenMove(val);}
				finalGradeClass.setRebuildDistWhenMove(val);
				break;}
			case showPlotsIDX : {
				if (val) {
					
				} else {
					
				}				
				break;}
			case stdntsHaveCurGradesIDX : {			//whether current grade set has been distributed to all students
				
				break;}
			case classesHaveCurStdntsIDX : {		//whether current class set has been populated by all students
				
				break;}
			case gradesNeedRecalcIDX	: {			//raw grades for each student need to be recalculated due to class/student structure size change or user request
				
				break;}
		}
	}//setFlag		

}//class ClassGradeExperiment

