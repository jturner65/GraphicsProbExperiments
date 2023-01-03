package experiments_PKG.classGradeExp.roster;

import java.util.HashMap;

import base_Render_Interface.IRenderInterface;
import base_ProbTools.randGenFunc.gens.base.myRandGen;
import base_StatsTools.summary.myProbSummary_Dbls;
import experiments_PKG.classGradeExp.experiment.ClassGradeExperiment;
import experiments_PKG.classGradeExp.experiment.myStudent;

/**
 * this class will implement a final grade roster that will aggregate all the (uniform/transformed) 
 * class grades for a student and map them to a particular distribution
 *		the raw grades come from the avg of the transformed/uniform class grades for each student, the 
 *   	final transformed grade comes from this value being transformed by desired distribution
 *   
 *   the direction of the calculations for the final grade roster is backwards from the direction of the class rosters
 * @author john
 */
public class myFinalGradeRoster extends myClassRoster {
	//whether or not to use zscore calc for final grades
	private boolean useZScore;
	
	public myFinalGradeRoster(IRenderInterface _pa, ClassGradeExperiment _gradeExp, String _name, float[][] _barLocs) {
		super(_pa, _gradeExp, _name, _barLocs);
		useZScore = false;
	}//ctor
	
	//take result of per class totals, determine the inverse mapping based on desired output distribution
	public void calcTotalGrades() {
		for (myStudent s : students.values()) {		s.calcTotalGrade(this);	}
		//get summary of current aggregate uniform grades
		myProbSummary_Dbls tmpSummary = getCurGradeProbSummary(transTypes[GB_uniTransGradeTypeIDX]);
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
			myRandGen baseDistModel = baseDistModels.get(curDistModel);
			if(null==baseDistModel) {	msgObj.dispInfoMessage("myFinalGradeRoster","calcTotalGrades","baseDistModel == null");	return;}
			//baseDistModel.setFuncSummary(tmpSummary);
			updateName();
			for (myStudent s : students.values()) {
				//now need to transform all uniform student grades for this class roster back to "raw", which in this case will be the final grade
				//transformStudentFromUniToRaw(s);
				double _transGrade = s.getTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this);
				//double _newGrade = randGen.CDF(_transGrade);
				double _newGrade = baseDistModel.inverseCDF(_transGrade);
				s.setTransformedGrade(transTypes[GB_rawGradeTypeIDX],this, _newGrade);
			}			
		}
		
		setFlag(classRawIsTransformedIDX, true);
		//now need to transform all uniform student grades for this class roster back to "raw", which in this case will be the final grade - perform this via feeding randGen with uniform value?
	}//calcTotalGrades
	
	//moving around a raw grade should update the underlying distribution
	@Override
	public void updateDistributionAndGrades(int barIDX) {	
		myRandGen baseDistModel = baseDistModels.get(curDistModel);
		if(gradeBars[barIDX].getModStudent() != null) {
			if(barIDX == 0) {					transformStudentFromRawToUni(gradeBars[barIDX].getModStudent(),baseDistModel);			} 		//using raw grade, transform student grade appropriately - backward mapping to uniform
			else {								transformStudentFromUniToRaw(gradeBars[barIDX].getModStudent(),baseDistModel);			}		//using transformed grade, re-calc raw grade appropriately			
		}
	}//updateDistribution	
	
	protected double getZScoreFromGrade(myProbSummary_Dbls tmpSummary, double _transGrade) {	
		double z = (_transGrade - tmpSummary.smpl_mean())/tmpSummary.smpl_std();
		double _newGrade = (85 + (10*z))/100.0;
		return _newGrade;
	}
	
	protected double getGradeFromZScore(myProbSummary_Dbls tmpSummary, double _zScore) {
		double scaledZ = ((100.0f * _zScore) - 85)/10.0;
		double _newGrade = (scaledZ * tmpSummary.smpl_std()) + tmpSummary.smpl_mean();
		return _newGrade;
	}
	
	@Override
	public void transformStudentFromRawToUni(myStudent s, myRandGen baseDistModel) {
		double _rawGrade = s.getTransformedGrade(transTypes[GB_rawGradeTypeIDX], this),_newGrade;
		if (useZScore){
			myProbSummary_Dbls tmpSummary = getCurGradeProbSummary(transTypes[GB_uniTransGradeTypeIDX]);
			_newGrade = getGradeFromZScore(tmpSummary, _rawGrade);
		} else {
			_newGrade = baseDistModels.get(curDistModel).CDF(_rawGrade);
		}
		s.setTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this, _newGrade);
		updateFinalGrades();
	}//transformStudent
	
	//find appropriate value for raw student grade given transformed uniform grade
	@Override
	public void transformStudentFromUniToRaw(myStudent s,myRandGen baseDistModel) {
		//changes here should modify individual class uniform grades using s.disperseFromTotalGrade(this)
		double _transGrade = s.getTransformedGrade(transTypes[GB_uniTransGradeTypeIDX], this);
		double _newGrade;
		if (useZScore){
			myProbSummary_Dbls tmpSummary = getCurGradeProbSummary(transTypes[GB_uniTransGradeTypeIDX]);
			_newGrade =getZScoreFromGrade(tmpSummary,_transGrade);			
		} else {
			_newGrade = baseDistModel.inverseCDF(_transGrade);
		}
		s.setTransformedGrade(transTypes[GB_rawGradeTypeIDX],this, _newGrade);
		updateFinalGrades();
	}//transformStudentToRaw

	public void setUseZScore(boolean _val) {
		useZScore=_val;
		calcTotalGrades();
	}
	//override this - don't want any mods on final grade bars calling this
	@Override
	protected void updateFinalGrades() {}

	//this is not used to set grades
	@Override
	public void setAllStudentRawGrades(HashMap<Integer, Double> classGrades) {
		msgObj.dispErrorMessage("myFinalGradeRoster", "setAllStudentRawGrades", "Final Grades for students are not set via setAllStudentRawGrades method.  Final Grades must be calculated.");
	}
}//class myFinalGradeRoster