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
	protected final float distBtwnClassBars;
	//distance between the raw and transformed bar for this class
	protected final float distBtwnRawTransBars;	
	//3 class bars - original distribution (raw), uniform, and scaled
	protected gradeBar[] gradeBars;// rawGradeBar, transGradeBar;
	
	//distribution plot rectangle
	protected float[] distPlotDimRect;
	//rand gen used to model underlying grade distribution for this class
	protected myRandGen rawGradeDistModel;
	
	//types of transforms that have been calculated for this class
	protected int[] transPerformed;	
	
	//list of possible classifications of grades
	protected static final String[] transTypes = new String[] {"raw","uniform","uni_scaled"};
	//idxs corresponding to trans types
	protected static final int
		GB_rawGradeTypeIDX 			= 0,
		GB_uniTransGradeTypeIDX 	= 1,
		GB_scaledUniGradeTypeIDX	= 2;
	
	//this class's bar color
	private final int[] clsLineClr;
	
	//state flags - bits in array holding relevant info about this random variable function
	private int[] stFlags;						
	public static final int
			debugIDX 						= 0,
			rawGradeDistMdlSetIDX			= 1,			//whether or not the raw grade distribution model for the grades in this class has been set - transformations can only occur if this is so
			classRawToTransEnabledIDX		= 2,			//if class's raw grades can/should be mapped to transformed grades
			classRawIsTransformedIDX		= 3,			//if raw grades have been transformed
			classTransUsedInFinalGradeIDX	= 4,			//if class's transformed grades are used in final grade
			drawLineBetweenGradesIDX		= 5,			//draw a line connecting students between grade types
			rebuildDistWhenMoveIDX			= 6;			//rebuild the underlying distribution when a grade value is moved by the user
	public static final int numFlags 		= 7;
	
	private myFinalGradeRoster _finalGrades;			//ref to final grade roster
	
	public myClassRoster(GraphProbExpMain _pa, ClassGradeExperiment _gradeExp, String _name, float[][] _barLocs) {
		pa =_pa;gradeExp=_gradeExp;
		ObjID = IDCnt++;  name=_name;
		initFlags();
		//visualization stuff
		distPlotDimRect = new float[] {_barLocs[2][0], _barLocs[2][1],gradeExp.getVisibleSreenWidth(),ClassGradeExperiment.distBtwnAdjPlots};
		distBtwnClassBars = ClassGradeExperiment.distBtwnAdjBars;
		distBtwnRawTransBars =  _barLocs[1][1] - _barLocs[0][1];
		clsLineClr = pa.getRndClr2(255);//should be brighter colors
		gradeBars = new gradeBar[transTypes.length];
		int barLocIDX = 0;
		for(int i=0;i<gradeBars.length;++i) {
			if(i>0) {barLocIDX=1;}
			gradeBars[i]=new gradeBar(this, new float[] {_barLocs[barLocIDX][0], _barLocs[barLocIDX][1], distBtwnClassBars}, transTypes[i],clsLineClr, "Visualization of "+transTypes[i]+" grades for class :"+name);			
		}
		gradeBars[GB_scaledUniGradeTypeIDX].setIsVisible(false);
		students = new HashMap<Integer,myStudent>();			
		transPerformed = new int[transTypes.length];
	}//ctor
	
	public void setFinalGradeRoster(myFinalGradeRoster _fgr) {_finalGrades=_fgr;}
	
	public void setDispWidth(float _dispWidth) {
		for(int i=0;i<gradeBars.length;++i) {			gradeBars[i].setDispWidth(_dispWidth);		}
		//also for plot res object
		rawGradeDistModel.dataVisSetDispWidth(_dispWidth);
	}//setBarWidth
	//when new transform added, need to clear out existing transformed grades
	public void setBaseDistModel(myRandGen _randGen) {		
		rawGradeDistModel = _randGen;
		rawGradeDistModel.buildDistVisObj(distPlotDimRect);
		updateName();
		setFlag(rawGradeDistMdlSetIDX, true);
	}//setRandGenAndType	
	
	protected void updateName() {
		for(int i=0;i<gradeBars.length;++i) {
			String newVisName = "Vis of "+transTypes[i]+" grades for class :"+name+"|Dist Mdl :"+ rawGradeDistModel.getDispTransName();
			gradeBars[i].updateName(newVisName);
		}
		rawGradeDistModel.updateVisName("Vis of Dist/Hist for sample grade distribution for class :"+name+"|Dist Mdl :"+ rawGradeDistModel.getDispTransName());
		
	}//updateName
	
	//remove all students from class
	public void clearClass() {	students.clear();}
	
	//clear all transformed student grades for this class, retaining raw grades
	public void clearAllTransformedStudentGrades() {
		for (myStudent s : students.values()) {			s.clearTransformedGrades(this);		}
	}
	
	//set reference to global student list
	public void setStudents(HashMap<Integer,myStudent> _students) {	students = _students;}	
	//set the raw grade for a student
	public void setAllStudentRawGrades(HashMap<Integer, Double> classGrades) {
		transPerformed[GB_rawGradeTypeIDX] = 1;		
		for (myStudent s : students.values()) {
			Integer SID = s.ObjID;
			Double grade = classGrades.get(SID);
			if(null==grade) {//no grade for student - this is an error
				gradeExp.dispMessage("myClassRoster","setAllStudentRawGrades","In class : " +name + "| No grade found for student ID :"+SID +" | Name : " +s.name+" | Defaulting grade to 0",true);
				grade=0.0;
			}
			s.setTransformedGrade(transTypes[GB_rawGradeTypeIDX], this, grade);
		}
	}//setAllStudentRawGrades
			
	public void setStudentGrade(int ID, double grade) {
		students.get(ID).setTransformedGrade(transTypes[GB_rawGradeTypeIDX], this, grade);
	}
	
	//transform all students in this class using passed rand gen's function to uniform from base distribution
	public void transformStudentGradesToUniform() {
		transPerformed[GB_uniTransGradeTypeIDX] = 1;
		for (myStudent s : students.values()) { transformStudentFromRawToUni(s);}
		setFlag(classRawIsTransformedIDX, true);
		updateFinalGrades();
	}//transformStudentGrades
	
	//transform all students in this class using passed rand gen's function to uniform from base distribution
	public void transformStudentGradesFromUniform() {
		transPerformed[GB_uniTransGradeTypeIDX] = 1;
		for (myStudent s : students.values()) { transformStudentFromUniToRaw(s);}
		setFlag(classRawIsTransformedIDX, true);
		updateFinalGrades();
	}//transformStudentGrades
	
	//TODO need to retransform student when student is moved - need to set randGen and _type
	public void transformStudentFromRawToUni(myStudent s) {
		double _rawGrade = s.getTransformedGrade(transTypes[GB_rawGradeTypeIDX], this);
		//double _newGrade = randGen.inverseCDF(_rawGrade);
		double _newGrade = rawGradeDistModel.CDF(_rawGrade);
		s.setTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this, _newGrade);
		updateFinalGrades();
	}//transformStudent
	
	//find appropriate value for raw student grade given transformed uniform grade
	public void transformStudentFromUniToRaw(myStudent s) {
		double _transGrade = s.getTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this);
		//double _newGrade = randGen.CDF(_transGrade);
		double _newGrade = rawGradeDistModel.inverseCDF(_transGrade);
		s.setTransformedGrade(transTypes[GB_rawGradeTypeIDX],this, _newGrade);
		updateFinalGrades();
	}//transformStudentToRaw
	
	protected void updateFinalGrades() {_finalGrades.calcTotalGrades();}
		
	//when bar is enabled/disabled, this is called
	public void setGradeBarEnabled(boolean val, String type) {
		if(type.equals(transTypes[GB_rawGradeTypeIDX])) {			setFlag(classRawToTransEnabledIDX, val);		} 
		else {														setFlag(classTransUsedInFinalGradeIDX, val);	}		
	}//setGradeBarEnabled	
	
	protected myProbSummary getCurGradeProbSummary(String typ) {
		double[] tmpTransGrades = new double[students.size()];
		int idx =0;
		for (myStudent s : students.values()) {	tmpTransGrades[idx++]=s.getTransformedGrade(typ, this);}
		myProbSummary tmpSummary = new myProbSummary(tmpTransGrades);
		return tmpSummary;
	}//getCurGradeProbSummary
		
	//check if mouse click is in one of grade bars for this class
	public boolean mseClickCheck(int msx, int msy, int btn) {
		boolean res = false;
		for(int i=0;i<gradeBars.length;++i) {
			res = gradeBars[i].checkMouseClick(msx, msy, btn);
			if(res) {return true;}
		}
		return res;
	}//mseClickCheck
	
	public boolean mseDragCheck(int msx, int msy, int btn) {
		boolean res = false;
		for(int barIDX=0;barIDX<gradeBars.length;++barIDX) {
			res = gradeBars[barIDX].checkMouseMoveDrag(msx, msy, btn);
			if (res) {//mouse move caused value to change
				if (btn <  0) {//mouse over behavior - no button has been clicked
					
				} else {//drag
					if((getFlag(classRawIsTransformedIDX)) && (gradeBars[barIDX]._modStudent != null)) {
						//if allow grade to modify distribution
						updateDistributionAndGrades(barIDX);
					}
					return true;
				}
			}
		}
		return res;
	}//mseClickCheck
	
	//moving around a raw grade should update the underlying distribution
	public void updateDistributionAndGrades(int barIDX) {		
		if (getFlag(rebuildDistWhenMoveIDX)) {
			//if we are modifying distribution
			if(barIDX == 0) {				
				myProbSummary newSummary = getCurGradeProbSummary(transTypes[GB_rawGradeTypeIDX]);
				rawGradeDistModel.setFuncSummary(newSummary);
				updateName();
				transformStudentGradesToUniform();//transform all grades here				
			} 		//using raw grade, transform student grade appropriately
			else {		
				//from transformed uniform to raw distribution
				transformStudentFromUniToRaw(gradeBars[barIDX]._modStudent);
				//rebuild summary obj
				myProbSummary newSummary = getCurGradeProbSummary(transTypes[GB_rawGradeTypeIDX]);
				rawGradeDistModel.setFuncSummary(newSummary);
				updateName();
				transformStudentGradesToUniform();			
			}		//using transformed grade, re-calc raw grade appropriately
		} else {
			//else if we do notmodify distribution -  move individual grade without modifying underlying distribution
			if(barIDX == 0) {	
				transformStudentFromRawToUni(gradeBars[barIDX]._modStudent);	
			} 		//using raw grade, transform student grade appropriately
			else {						
				transformStudentFromUniToRaw(gradeBars[barIDX]._modStudent);	
			}		//using transformed grade, re-calc raw grade appropriately
		}
	}//updateDistribution
	
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
	
	public void clearPlotEval() {
		rawGradeDistModel.clearPlotEval();
	}
	
	public void evalAndPlotFuncRes(int numVals, double low, double high, int funcType ) {
		if(rawGradeDistModel == null) {			this.gradeExp.dispMessage("myClassRoster", "evalAndPlotFuncRes", "rawGradeDistModel has not been set/is null.  Aborting", true);		}
		rawGradeDistModel.calcFuncValsForDisp(numVals, low, high, funcType);		
	}
	public void evalAndPlotHistRes(int numVals, int numBuckets) {
		if(rawGradeDistModel == null) {			this.gradeExp.dispMessage("myClassRoster", "evalAndPlotHistRes", "rawGradeDistModel has not been set/is null.  Aborting", true);		}		
		rawGradeDistModel.calcDistValsForDisp(numVals, numBuckets);
	}
	
		
	public void mseRelease() {
		for(int i=0;i<gradeBars.length;++i) {gradeBars[i].mouseRelease();}
	}//mseRelease
	
	//draw plot results from functional histogram/evaluation of rawGradeDistModel
	public void drawPlotRes() {	rawGradeDistModel.drawDist(pa);}
	
	public void drawRawToUniformLine() {
		drawRawToTransformedLine(transTypes[GB_rawGradeTypeIDX],
				transTypes[GB_uniTransGradeTypeIDX], this, this, 
				gradeBars[GB_rawGradeTypeIDX], distBtwnRawTransBars,
				(getFlag(classRawToTransEnabledIDX) || getFlag(classTransUsedInFinalGradeIDX)) && (getFlag(classRawIsTransformedIDX)));
	}
	
	public void drawTransToFinalLine(myFinalGradeRoster finalGrades) {
		drawRawToTransformedLine(transTypes[GB_uniTransGradeTypeIDX],
				transTypes[GB_uniTransGradeTypeIDX], this, finalGrades, 
				gradeBars[GB_uniTransGradeTypeIDX], (finalGrades.gradeBars[GB_uniTransGradeTypeIDX].getAbsYLoc() - gradeBars[GB_uniTransGradeTypeIDX].getAbsYLoc()),
				(getFlag(classTransUsedInFinalGradeIDX)) && (getFlag(classRawIsTransformedIDX)) );
	}
	
	
	//draw lines connecting student grades to help visualize mapping if grade is enabled and transformation has been performed
	protected void drawRawToTransformedLine(String fromTransType, String toTransType, myClassRoster fromCls, myClassRoster toCls, gradeBar stGradeBar, float dist, boolean check) {
		//TODO check if passed trasnformation has occured
		//if((getFlag(classRawToTransEnabledIDX)) && (getFlag(classRawIsTransformedIDX))) {
		if(check) {
			pa.pushMatrix();pa.pushStyle();
			//first transform to this class's raw grade line
			stGradeBar.transToBarStart(pa);
			for (myStudent s : students.values()) {//uses student color
				double rawXLoc = s.getGradeXLoc(fromTransType, fromCls, gradeBars[GB_rawGradeTypeIDX].barWidth);
				double transXLoc = s.getGradeXLoc(toTransType, toCls, gradeBars[GB_rawGradeTypeIDX].barWidth);
				pa.setStroke(s.clr);
				pa.line(rawXLoc, 0, 0, transXLoc,dist,0);				
			}
			pa.popStyle();pa.popMatrix();		
		}		
	}//drawRawToTransformedLine	
	
	
	//draw all student grades on line 
	public void drawStudentGradesRaw() {_drawStudentBar(GB_rawGradeTypeIDX);}//drawStudentGradesRaw	
	public void drawStudentGradesUni() {_drawStudentBar(GB_uniTransGradeTypeIDX);}
	
	private void _drawStudentBar(int idx) {gradeBars[idx].drawVis(pa);}
	
	//incase we wish to store class rosters in sorted mechanism
	@Override
	public int compareTo(myClassRoster othr) {
		int res = this.name.toLowerCase().compareTo(othr.name.toLowerCase());
		return (res == 0 ? Integer.compare(ObjID, othr.ObjID) : res);
	}//compareTo
	
	public void setRebuildDistWhenMove(boolean val) {setFlag(rebuildDistWhenMoveIDX, val);}
	
	//state flag management
	private void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX 						: {break;}	
			case rawGradeDistMdlSetIDX			: {break;}
			case classRawToTransEnabledIDX 		: {break;}	
			case classRawIsTransformedIDX		: {break;}
			case classTransUsedInFinalGradeIDX 	: {break;}				
			case drawLineBetweenGradesIDX		: {break;}		
			case rebuildDistWhenMoveIDX			: {break;}
		}
	}//setFlag		
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}	
	
	@Override
	public String toString() {
		String res = "Class Name : " + name + " # of students : " + students.size();
		
		return res;
	}//toString

}//myClassRoster





/**
 * this class will implement a final grade roster that will aggregate all the (uniform/transformed) 
 * class grades for a student and map them to a particular distribution
 *		the raw grades come from the avg of the transformed/uniform class grades for each student, the 
 *   	final transformed grade comes from this value being transformed by desired distribution
 *   
 *   the direction of the calculations for the final grade roster is backwards from the direction of the class rosters
 * @author john
 */

class myFinalGradeRoster extends myClassRoster {
	//whether or not to use zscore calc for final grades
	private boolean useZScore;
	
	public myFinalGradeRoster(GraphProbExpMain _pa, ClassGradeExperiment _gradeExp, String _name, float[][] _barLocs) {
		super(_pa, _gradeExp, _name, _barLocs);
		useZScore = false;
	}//ctor
	
	
	//take result of per class totals, determine the inverse mapping based on desired output distribution
	public void calcTotalGrades() {
		transPerformed[GB_rawGradeTypeIDX] = 1;		
		transPerformed[GB_uniTransGradeTypeIDX] = 1;
		for (myStudent s : students.values()) {		s.calcTotalGrade(this);	}
		//get summary of current aggregate uniform grades
		myProbSummary tmpSummary = getCurGradeProbSummary(transTypes[GB_uniTransGradeTypeIDX]);
		if (useZScore){					
			for (myStudent s : students.values()) {
				//now need to transform all uniform student grades for this class roster back to "raw", which in this case will be the final grade
				//transformStudentFromUniToRaw(s);
				double _transGrade = s.getTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this);
				double z = (_transGrade - tmpSummary.smpl_mean())/tmpSummary.smpl_std();
				double _newGrade = (85 + (10*z))/100.0;
				s.setTransformedGrade(transTypes[GB_rawGradeTypeIDX],this, _newGrade);
			}			
		} else {
			if(null==rawGradeDistModel) {return;}
			rawGradeDistModel.setFuncSummary(tmpSummary);
			updateName();
			for (myStudent s : students.values()) {
				//now need to transform all uniform student grades for this class roster back to "raw", which in this case will be the final grade
				//transformStudentFromUniToRaw(s);
				double _transGrade = s.getTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this);
				//double _newGrade = randGen.CDF(_transGrade);
				double _newGrade = rawGradeDistModel.inverseCDF(_transGrade);
				s.setTransformedGrade(transTypes[GB_rawGradeTypeIDX],this, _newGrade);
			}			
		}
		
		setFlag(classRawIsTransformedIDX, true);
		//now need to transform all uniform student grades for this class roster back to "raw", which in this case will be the final grade - perform this via feeding randGen with uniform value?
	}//calcTotalGrades
	
	//moving around a raw grade should update the underlying distribution
	@Override
	public void updateDistributionAndGrades(int barIDX) {				
		if(barIDX == 0) {					transformStudentFromRawToUni(gradeBars[barIDX]._modStudent);			} 		//using raw grade, transform student grade appropriately - backward mapping to uniform
		else {								transformStudentFromUniToRaw(gradeBars[barIDX]._modStudent);			}		//using transformed grade, re-calc raw grade appropriately			
	}//updateDistribution	
	
	protected double getZScoreFromGrade(myProbSummary tmpSummary, double _transGrade) {	
		double z = (_transGrade - tmpSummary.smpl_mean())/tmpSummary.smpl_std();
		double _newGrade = (85 + (10*z))/100.0;
		return _newGrade;
	}
	
	protected double getGradeFromZScore(myProbSummary tmpSummary, double _zScore) {
		double scaledZ = ((100.0f * _zScore) - 85)/10.0;
		double _newGrade = (scaledZ * tmpSummary.smpl_std()) + tmpSummary.smpl_mean();
		return _newGrade;
	}
	
	@Override
	public void transformStudentFromRawToUni(myStudent s) {
		double _rawGrade = s.getTransformedGrade(transTypes[GB_rawGradeTypeIDX], this),_newGrade;
		if (useZScore){
			myProbSummary tmpSummary = getCurGradeProbSummary(transTypes[GB_uniTransGradeTypeIDX]);
			_newGrade = getGradeFromZScore(tmpSummary, _rawGrade);
		} else {
			_newGrade = rawGradeDistModel.CDF(_rawGrade);
		}
		s.setTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this, _newGrade);
		updateFinalGrades();
	}//transformStudent
	
	//find appropriate value for raw student grade given transformed uniform grade
	@Override
	public void transformStudentFromUniToRaw(myStudent s) {
		//changes here should modify individual class uniform grades using s.disperseFromTotalGrade(this)
		
		double _transGrade = s.getTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this);
		double _newGrade;
		if (useZScore){
			myProbSummary tmpSummary = getCurGradeProbSummary(transTypes[GB_uniTransGradeTypeIDX]);
			_newGrade =getZScoreFromGrade(tmpSummary,_transGrade);			
		} else {
			_newGrade = rawGradeDistModel.inverseCDF(_transGrade);
		}
		s.setTransformedGrade(transTypes[GB_rawGradeTypeIDX],this, _newGrade);
		updateFinalGrades();
	}//transformStudentToRaw

	public void setUseZScore(boolean _val) {
		System.out.println("use z score : " + _val);
		useZScore=_val;
		calcTotalGrades();
	}
	//override this - don't want any mods on final grade bars calling this
	@Override
	protected void updateFinalGrades() {}

	//this is not used to set grades
	@Override
	public void setAllStudentRawGrades(HashMap<Integer, Double> classGrades) {
		this.gradeExp.dispMessage("myFinalGradeRoster", "setAllStudentRawGrades", "Final Grades for students are not set via setAllStudentRawGrades method.  Final Grades must be calculated" , true);
	}
}//class myFinalGradeRoster

////special class for uniform mapping just to manage requirement to have all data values set - won't use any transform
//class myUniformCountFinalGradeRoster extends myFinalGradeRoster {
//
//	public myUniformCountFinalGradeRoster(GraphProbExpMain _pa, ClassGradeExperiment _gradeExp, String _name,float[][] _barLocs) {
//		super(_pa, _gradeExp, _name, _barLocs);
//	}
//	//this is not used to set grades
//	@Override
//	public void setAllStudentRawGrades(HashMap<Integer, Double> classGrades) {
//		this.gradeExp.dispMessage("myUniformCountFinalGradeRoster", "setAllStudentRawGrades", "Final Grades for students are not set via setAllStudentRawGrades method.  Final Grades must be calculated" , true);
//	}
//}//class myUniformCountFinalGradeRoster


/**
 * this class will hold an instance of a student, who belongs to numerous class rosters, and has grades in each. 
 * This class will record their grade values and map them to a line
 * @author john
 *
 */
class myStudent implements Comparable<myStudent>{
	public final int ObjID;
	private static int IDCnt = 0;
	//used these for rendering student on line
	private static final float rad = 12.0f;
	//black for stroke
	private static final int[] blkStrk = new int[] {0,0,0,255};
	//grey for disabled
	private static final int[] greyOff = new int[] {100,100,100,255};
	//student name
	public final String name;
	//color to render student
	public final int[] clr;
	//location to put point for text display
	private final myPointf textLoc;
	
	//listing of raw grade following some distribution, and uniform grade, from result of mapping
	private HashMap<String,HashMap<myClassRoster, Double>> grades;
	
	public myStudent(GraphProbExpMain pa, String _name) {
		ObjID = IDCnt++;  name=_name;
		grades = new HashMap<String,HashMap<myClassRoster, Double>>();
		clr = pa.getRndClr2(255);//should be brighter colors
		int tag = ObjID % 4;
		textLoc = new myPointf(-10, (tag < 2 ? 15 * (tag+1) : -10 * ( tag-1)) , 0);
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
		for (String typ : grades.keySet()) {
			if (typ.equals("raw")) {continue;}		//don't clear raw grades
			HashMap<myClassRoster, Double> typeToClearForClasses = grades.get(typ);
			typeToClearForClasses.put(_cls,null);
		}
	}//
	
	//convert transformed uniform grade to span 0->1, using passed min and max values of observed uniform grades in a particular class
	public void setScaledUniformGrade(myClassRoster _cls, String _transType, String _scaledUnitype,  double min, double max) {
		HashMap<myClassRoster, Double> allClassesUniType = grades.get(_transType);
		HashMap<myClassRoster, Double> allClassesScaledType = grades.get(_scaledUnitype);
		double uniVal = allClassesUniType.get(_cls), diff = max - min, sclVal;
		if(max - min == 0) {	sclVal = uniVal;} 
		else {					sclVal = (uniVal - min)/diff; }
		sclVal = (sclVal < 0 ? 0 : sclVal > 1 ? 1 : sclVal);
		allClassesScaledType.put(_cls, sclVal);
	}//setUniformGrade
		
	//calculate the total grade for this student for each type of grade
	public void calcTotalGrade(myClassRoster _finalRoster) {
		for (String typ : grades.keySet()) {		//for every type of grade, calculate total grade and put in new "class"
			if(typ.equals(myClassRoster.transTypes[myClassRoster.GB_rawGradeTypeIDX])) {continue;}
			HashMap<myClassRoster, Double> classValsForType = grades.get(typ);
			double tot = 0, ttlGrade = 0;
			int numClasses = 0;
			for (myClassRoster _class : classValsForType.keySet()) {
				if(_class.name.equals(_finalRoster.name)){continue;}
				Double gr = classValsForType.get(_class);
				tot += gr;	
				++numClasses;				
			}
			if(numClasses > 0) {	
				ttlGrade = tot/numClasses;		
			}
			classValsForType.put(_finalRoster, ttlGrade);			
		}
	}//calcTotalGrade
	//recalculate transformed grades in every class based on newly transformed final grade
	public void disperseFromTotalGrade(myClassRoster _finalRoster) {
		for (String typ : grades.keySet()) {		//for every type of grade, calculate total grade and put in new "class"
			if(typ.equals(myClassRoster.transTypes[myClassRoster.GB_rawGradeTypeIDX])) {continue;}
			HashMap<myClassRoster, Double> classValsForType = grades.get(typ);
			//new value from moving student grade
			double newAvg = classValsForType.get(_finalRoster);
			double tot = 0, oldAverage = 0;
			int numClasses = 0;
			for (myClassRoster _class : classValsForType.keySet()) {
				if(_class.name.equals(_finalRoster.name)){continue;}
				Double gr = classValsForType.get(_class);
				tot += gr;	
				++numClasses;				
			}
			if(numClasses > 0) {	oldAverage = tot/numClasses;	}
			double ratio = newAvg/oldAverage;
			for (myClassRoster _class : classValsForType.keySet()) {
				if(_class.name.equals(_finalRoster.name)){continue;}
				Double gr = classValsForType.get(_class);
				gr *= ratio;
				classValsForType.put(_class, gr);
			}
		}
	}
	
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
	public void drawMeTransformed(GraphProbExpMain pa, String _type, myClassRoster _class, float ttlWidth) {	
		HashMap<myClassRoster, Double> classValsForType = grades.get(_type);
		if (classValsForType==null) {						// no values for this type for any classes set yet 		
			return;	}
		Double val = classValsForType.get(_class);			//grade for class for specified type
		if (val==null) {									//student has no transformed grade for this class	
			return;	}
		float gr = val.floatValue();
		pa.pushMatrix();pa.pushStyle();
		pa.translate(ttlWidth*gr, 0.0f, 0.0f);
		pa.showCrclNoBox_ClrAra(myPointf.ZEROPT, rad, clr,blkStrk, pa.gui_White, textLoc,  ""+ String.format("%.3f",gr));
		pa.popStyle();pa.popMatrix();
	}//drawMeTransformed
	
	public void drawMeTransformedOff(GraphProbExpMain pa, String _type, myClassRoster _class, float ttlWidth) {	
		HashMap<myClassRoster, Double> classValsForType = grades.get(_type);
		if (classValsForType==null) {						// no values for this type for any classes set yet 		
			return;	}
		Double val = classValsForType.get(_class);			//grade for class for specified type
		if (val==null) {									//student has no transformed grade for this class	
			return;	}
		float gr = val.floatValue();
		pa.pushMatrix();pa.pushStyle();
		pa.translate(ttlWidth*gr, 0.0f, 0.0f);
		pa.showCrclNoBox_ClrAra(myPointf.ZEROPT, rad, greyOff,blkStrk, pa.gui_White, textLoc,  ""+ String.format("%.3f",gr));
		pa.popStyle();pa.popMatrix();
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