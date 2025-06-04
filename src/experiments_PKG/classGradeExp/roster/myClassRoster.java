package experiments_PKG.classGradeExp.roster;

import java.util.HashMap;

import base_Math_Objects.MyMathUtils;
import base_ProbTools.randGenFunc.gens.base.Base_RandGen;
import base_ProbTools.samples.mySampleSet;
import base_Render_Interface.IRenderInterface;
import base_StatsTools.summary.myProbSummary_Dbls;
import base_StatsTools.visualization.myDistFuncHistVisMgr;
import base_StatsTools.visualization.base.baseVisMgr;
import experiments_PKG.classGradeExp.myGradeDistVisBar;
import experiments_PKG.classGradeExp.experiment.ClassGradeExperiment;
import experiments_PKG.classGradeExp.experiment.myStudent;


/**
 * this class will hold a roster - a collection of students for a specific class, and will manage working with grades
 * it will consist of a collection of "grade bars" that will be used to display the grades of the class on a line,
 * @author john
 */
public class myClassRoster extends mySampleSet{
	/**
	 * 
	 */
	public static IRenderInterface ri;
	//structure holding references to the students in this class - students need to be created external to class and added
	protected HashMap<Integer,myStudent> students;

	///////////////////////////
	//rendering 
	//distance between adjacent classes
	protected final float distBtwnClassBars;
	//distance between the raw and transformed bar for this class
	protected final float distBtwnRawTransBars;	
	//3 class bars - original distribution (raw), uniform, and scaled
	protected myGradeDistVisBar[] gradeBars;// rawGradeBar, transGradeBar;
	
	//distribution plot rectangle
	protected float[] distPlotDimRect;
	
	//list of possible classifications of grades
	public static final String[] transTypes = new String[] {"raw","uniform","uni_scaled"};
	
	//idxs corresponding to trans types
	public static final int
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
	
	public myClassRoster(IRenderInterface _ri, ClassGradeExperiment _gradeExp, String _name, float[][] _barLocs) {
		super(_name);
		ri = _ri;
		initFlags();
		//visualization stuff
		distPlotDimRect = new float[] {_barLocs[2][0], _barLocs[2][1],_gradeExp.getVisibleSreenWidth(),ClassGradeExperiment.distBtwnAdjPlots};
		distBtwnClassBars = ClassGradeExperiment.distBtwnAdjBars;
		distBtwnRawTransBars =  _barLocs[1][1] - _barLocs[0][1];
		clsLineClr = MyMathUtils.randomIntBrightClrAra();//should be brighter colors
		gradeBars = new myGradeDistVisBar[transTypes.length];
		int barLocIDX = 0;
		for(int i=0;i<gradeBars.length;++i) {
			if(i>0) {barLocIDX=1;}
			gradeBars[i]=new myGradeDistVisBar(this, ri, new float[] {_barLocs[barLocIDX][0], _barLocs[barLocIDX][1], distPlotDimRect[2], distBtwnClassBars}, transTypes[i],clsLineClr, "Visualization of "+transTypes[i]+" grades for class :"+name);			
		}
		gradeBars[GB_scaledUniGradeTypeIDX].setIsVisible(false);
		students = new HashMap<Integer,myStudent>();		
	}//ctor
	
	public void setFinalGradeRoster(myFinalGradeRoster _fgr) {_finalGrades=_fgr;}
	
	public void setDispWidth(float _dispWidth) {
		for(int i=0;i<gradeBars.length;++i) {			gradeBars[i].setDispWidth(_dispWidth);		}
		//also for plot res object
		distModelVis.get(curDistModel).setDispWidth(_dispWidth);
	}//setBarWidth
	
	/**
	 * Build visualization mgr corresponding to current randGen model
	 * @return
	 */
	@Override
	protected final myDistFuncHistVisMgr buildVisMgr(String _name) {
		return new myDistFuncHistVisMgr(ri, distPlotDimRect, _name);
	}
	
	//when new transform added, need to clear out existing transformed grades
	@Override
	protected void setBaseDistModel_Indiv() {	
		updateName();
		setFlag(rawGradeDistMdlSetIDX, true);
	}//setRandGenAndType	
	
	protected void updateName() {
		Base_RandGen baseDistModel = baseDistModels.get(curDistModel);
		baseVisMgr distMdlViz = distModelVis.get(curDistModel);
		for(int i=0;i<gradeBars.length;++i) {
			String newVisName = "Vis of "+transTypes[i]+" grades for class :"+name+"|Dist Mdl :"+ baseDistModel.getDispTransName();
			gradeBars[i].updateName(newVisName);
		}
		distMdlViz.updateName("Vis of Dist/Hist for sample grade distribution for class :"+name+"|Dist Mdl :"+ baseDistModel.getDispTransName());
		
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
		for (myStudent s : students.values()) {
			Integer SID = s.ObjID;
			Double grade = classGrades.get(SID);
			if(null==grade) {//no grade for student - this is an error
				msgObj.dispInfoMessage("myClassRoster","setAllStudentRawGrades","In class : " +name + "| No grade found for student ID :"+SID +" | Name : " +s.name+" | Defaulting grade to 0");
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
		Base_RandGen baseDistModel = baseDistModels.get(curDistModel);
		for (myStudent s : students.values()) { transformStudentFromRawToUni(s,baseDistModel);}
		setFlag(classRawIsTransformedIDX, true);
		updateFinalGrades();
	}//transformStudentGrades
	
	//transform all students in this class using passed rand gen's function to uniform from base distribution
	public void transformStudentGradesFromUniform() {
		Base_RandGen baseDistModel = baseDistModels.get(curDistModel);
		for (myStudent s : students.values()) { transformStudentFromUniToRaw(s,baseDistModel);}
		setFlag(classRawIsTransformedIDX, true);
		updateFinalGrades();
	}//transformStudentGrades
	
	//TODO need to retransform student when student is moved - need to set randGen and _type
	public void transformStudentFromRawToUni(myStudent s, Base_RandGen baseDistModel) {
		double _rawGrade = s.getTransformedGrade(transTypes[GB_rawGradeTypeIDX], this);
		//double _newGrade = randGen.inverseCDF(_rawGrade);		
		double _newGrade = baseDistModel.CDF(_rawGrade);
		s.setTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this, _newGrade);
		updateFinalGrades();
	}//transformStudent
	
	//find appropriate value for raw student grade given transformed uniform grade
	public void transformStudentFromUniToRaw(myStudent s, Base_RandGen baseDistModel) {
		double _transGrade = s.getTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this);
		//double _newGrade = randGen.CDF(_transGrade);
		double _newGrade = baseDistModel.inverseCDF(_transGrade);
		s.setTransformedGrade(transTypes[GB_rawGradeTypeIDX],this, _newGrade);
		updateFinalGrades();
	}//transformStudentToRaw
	
	protected void updateFinalGrades() {_finalGrades.calcTotalGrades();}
		
	//when bar is enabled/disabled, this is called
	public void setGradeBarEnabled(boolean val, String type) {
		if(type.equals(transTypes[GB_rawGradeTypeIDX])) {			setFlag(classRawToTransEnabledIDX, val);		} 
		else {														setFlag(classTransUsedInFinalGradeIDX, val);	}		
	}//setGradeBarEnabled	
	
	protected myProbSummary_Dbls getCurGradeProbSummary(String typ) {
		double[] tmpTransGrades = new double[students.size()];
		int idx =0;
		for (myStudent s : students.values()) {	tmpTransGrades[idx++]=s.getTransformedGrade(typ, this);}
		myProbSummary_Dbls tmpSummary = new myProbSummary_Dbls(tmpTransGrades);
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
		boolean res;
		//check every bar if moved or dragged
		if (btn <  0) {//mouse over behavior - no mouse button has been clicked
//			for(int barIDX=0;barIDX<gradeBars.length;++barIDX) {
//				res = gradeBars[barIDX].checkMouseMoveDrag(msx, msy, btn);
//				if (res) {//manage mouse over
//					return true;
//				}				
//			}//for every bar			
		} else {//drag			
			for(int barIDX=0;barIDX<gradeBars.length;++barIDX) {
				res = gradeBars[barIDX].checkMouseMoveDrag(msx, msy, btn);
				if (res) {//mouse drag in this object caused value to change
					if((getFlag(classRawIsTransformedIDX)) && (gradeBars[barIDX].getModStudent() != null)) {
						//if allow grade to modify distribution
						updateDistributionAndGrades(barIDX);
					}
					return true;
				}
			}//for every bar
		}		
		return false;
	}//mseClickCheck
	
	public void updateAllDistsAndGrades() {
		for(int barIDX=0;barIDX<gradeBars.length;++barIDX) {	updateDistributionAndGrades(barIDX);}
	}
	
	//moving around a raw grade should update the underlying distribution
	public void updateDistributionAndGrades(int barIDX) {		
		Base_RandGen baseDistModel = baseDistModels.get(curDistModel);
		if (getFlag(rebuildDistWhenMoveIDX)) {
			//if we are modifying distribution
			if(barIDX == 0) {				
				myProbSummary_Dbls newSummary = getCurGradeProbSummary(transTypes[GB_rawGradeTypeIDX]);
				baseDistModel.setFuncSummary(newSummary);
				updateName();
				transformStudentGradesToUniform();//transform all grades here				
			} 		//using raw grade, transform student grade appropriately
			else {		
				//from transformed uniform to raw distribution
				if(gradeBars[barIDX].getModStudent() != null) {transformStudentFromUniToRaw(gradeBars[barIDX].getModStudent(), baseDistModel);}
				//rebuild summary obj
				myProbSummary_Dbls newSummary = getCurGradeProbSummary(transTypes[GB_rawGradeTypeIDX]);
				baseDistModel.setFuncSummary(newSummary);
				updateName();
				transformStudentGradesToUniform();			
			}		//using transformed grade, re-calc raw grade appropriately
		} else {
			if(gradeBars[barIDX].getModStudent() != null) {
				//else if we do notmodify distribution -  move individual grade without modifying underlying distribution
				if(barIDX == 0) {					transformStudentFromRawToUni(gradeBars[barIDX].getModStudent(), baseDistModel);	} 		//using raw grade, transform student grade appropriately
				else {								transformStudentFromUniToRaw(gradeBars[barIDX].getModStudent(), baseDistModel);	}		//using transformed grade, re-calc raw grade appropriately
			}
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
		
	public void mseRelease() {
		for(int i=0;i<gradeBars.length;++i) {gradeBars[i].mouseRelease();}
	}//mseRelease
	
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
	protected void drawRawToTransformedLine(String fromTransType, String toTransType, myClassRoster fromCls, myClassRoster toCls, myGradeDistVisBar stGradeBar, float dist, boolean check) {
		//TODO check if passed trasnformation has occured
		//if((getFlag(classRawToTransEnabledIDX)) && (getFlag(classRawIsTransformedIDX))) {
		if(check) {
			ri.pushMatState();
			//first transform to this class's raw grade line
			stGradeBar.transToBarStart();
			for (myStudent s : students.values()) {//uses student color
				double rawXLoc = s.getGradeXLoc(fromTransType, fromCls, gradeBars[GB_rawGradeTypeIDX].barWidth);
				double transXLoc = s.getGradeXLoc(toTransType, toCls, gradeBars[GB_rawGradeTypeIDX].barWidth);
				ri.setStroke(s.clr, s.clr[3]);
				ri.drawLine(rawXLoc, 0, 0, transXLoc,dist,0);				
			}
			ri.popMatState();		
		}		
	}//drawRawToTransformedLine	
	
	//draw all student grades on line 
	public void drawStudentGradesRaw() {_drawStudentBar(GB_rawGradeTypeIDX);}//drawStudentGradesRaw	
	public void drawStudentGradesUni() {_drawStudentBar(GB_uniTransGradeTypeIDX);}
	
	private void _drawStudentBar(int idx) {gradeBars[idx].drawVis();}
	
	/**
	 * Draw all students on bar
	 * @param gradeType
	 * @param barWidth
	 * @param drawGrey
	 */
	public void drawAllStudents(String gradeType, float barWidth, boolean enabled) {
		if (enabled) {
			for (myStudent s : students.values()) {		s.drawMeTransformed(ri, gradeType, this, s.clr, barWidth);	}
		} else {
			for (myStudent s : students.values()) {		s.drawMeTransformed(ri, gradeType, this, myGradeDistVisBar.greyOff, barWidth);	}	
		}
	}
	
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

////special class for uniform mapping just to manage requirement to have all data values set - won't use any transform
//class myUniformCountFinalGradeRoster extends myFinalGradeRoster {
//
//	public myUniformCountFinalGradeRoster(IRenderInterface _pa, ClassGradeExperiment _gradeExp, String _name,float[][] _barLocs) {
//		super(_pa, _gradeExp, _name, _barLocs);
//	}
//	//this is not used to set grades
//	@Override
//	public void setAllStudentRawGrades(HashMap<Integer, Double> classGrades) {
//		this.gradeExp.msgObj.dispMessage("myUniformCountFinalGradeRoster", "setAllStudentRawGrades", "Final Grades for students are not set via setAllStudentRawGrades method.  Final Grades must be calculated" , true);
//	}
//}//class myUniformCountFinalGradeRoster

