package classGradeExperimentsPKG;

import java.util.HashMap;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_ProbTools.samples.mySampleObs;
import base_ProbTools.samples.mySampleSet;
import base_Math_Objects.vectorObjs.floats.myPointf;

/**
 * this class will hold an instance of a student, a collection of sample observations from multiple class rosters. 
 * This class will record their grade values and map them to a line
 * @author john
 *
 */
public class myStudent implements Comparable<mySampleObs> {
	//short display name
	public final String shrtName;
	public final int ObjID;
	private static int IDCnt = 0;
	//sample name
	public final String name;
	//color to render sample
	public final int[] clr;
	//radius to draw sample
	protected static final float rad = 12.0f;
	
	//listing of raw grade following some distribution, and uniform grade, from result of mapping, keyed by mapping type
	private HashMap<String,HashMap<mySampleSet, mySampleObs>> grades;
	
	public myStudent(int[] _clr, String _name) {
		ObjID = IDCnt++;  name=_name;
		clr = _clr;//should be brighter colors
		shrtName = ""+(char)((ObjID % 26) + 65) + (ObjID / 26);
		grades = new HashMap<String,HashMap<mySampleSet, mySampleObs>>();
	}//ctor

	
	//clear out current transformed grades, preserving raw grades
	public void clearTransformedGrades(mySampleSet _cls) {
		for (String typ : grades.keySet()) {
			if (typ.equals("raw")) {continue;}		//don't clear raw grades
			HashMap<mySampleSet, mySampleObs> typeToClearForClasses = grades.get(typ);
			typeToClearForClasses.put(_cls,null);
		}
	}//clearTransformedGrades
	
	//convert transformed uniform grade to span 0->1, using passed min and max values of observed uniform grades in a particular class
	public void setScaledUniformGrade(mySampleSet _cls, String _transType, String _scaledUnitype,  double min, double max) {
		HashMap<mySampleSet, mySampleObs> allClassesUniType = grades.get(_transType);
		HashMap<mySampleSet, mySampleObs> allClassesScaledType = grades.get(_scaledUnitype);
		mySampleObs uniObs = allClassesUniType.get(_cls);
		mySampleObs sclObs = allClassesScaledType.get(_cls);
		if(null==sclObs) {			sclObs = new mySampleObs (name+"|"+_cls.name,_cls, 0.0);}	
		double uniVal = uniObs.getValue(), diff = max - min, sclVal;
		if(max - min == 0) {	sclVal = uniVal;} 
		else {					sclVal = (uniVal - min)/diff; }
		sclVal = (sclVal < 0 ? 0 : sclVal > 1 ? 1 : sclVal);
		sclObs.setValue(sclVal);
		allClassesScaledType.put(_cls, sclObs);
	}//setUniformGrade
		
	//calculate the total grade for this student for each type of grade
	public void calcTotalGrade(mySampleSet _finalRoster) {
		for (String typ : grades.keySet()) {		//for every type of grade, calculate total grade and put in new "class"
			if(typ.equals(myClassRoster.transTypes[myClassRoster.GB_rawGradeTypeIDX])) {continue;}
			HashMap<mySampleSet, mySampleObs> classValsForType = grades.get(typ);
			mySampleObs finalObs = classValsForType.get(_finalRoster);
			if(finalObs == null) {		finalObs = new mySampleObs (name+"|"+_finalRoster.name,_finalRoster, 0.0);}
			double tot = 0, ttlGrade = 0;
			int numClasses = 0;
			for (mySampleSet _class : classValsForType.keySet()) {
				if(_class.name.equals(_finalRoster.name)){continue;}
				Double gr = classValsForType.get(_class).getValue();
				tot += gr;	
				++numClasses;				
			}
			if(numClasses > 0) {				ttlGrade = tot/numClasses;			}
			finalObs.setValue(ttlGrade);
			classValsForType.put(_finalRoster, finalObs);			
		}
	}//calcTotalGrade
	//recalculate transformed grades in every class based on newly transformed final grade
	public void disperseFromTotalGrade(mySampleSet _finalRoster) {
		for (String typ : grades.keySet()) {		//for every type of grade, calculate total grade and put in new "class"
			if(typ.equals(myClassRoster.transTypes[myClassRoster.GB_rawGradeTypeIDX])) {continue;}
			HashMap<mySampleSet, mySampleObs> classValsForType = grades.get(typ);
			//new value from moving student grade
			double newAvg = classValsForType.get(_finalRoster).getValue();
			double tot = 0, oldAverage = 0;
			int numClasses = 0;
			for (mySampleSet _class : classValsForType.keySet()) {
				if(_class.name.equals(_finalRoster.name)){continue;}
				Double gr = classValsForType.get(_class).getValue();
				tot += gr;	
				++numClasses;				
			}
			if(numClasses > 0) {	oldAverage = tot/numClasses;	}
			double ratio = newAvg/oldAverage;
			for (mySampleSet _class : classValsForType.keySet()) {
				if(_class.name.equals(_finalRoster.name)){continue;}
				mySampleObs clsSample = classValsForType.get(_class);
				clsSample.setValue(clsSample.getValue() * ratio);
				classValsForType.put(_class, clsSample);
			}
		}
	}//disperseFromTotalGrade
	
	//set transformed values for student, keyed by type of transformation and class
	public void setTransformedGrade(String _type, mySampleSet _class, double _gr) {	
		HashMap<mySampleSet, mySampleObs> classValsForType = grades.get(_type);	
		if (classValsForType==null) {// no values for this class set yet - build new map of types for this class
			classValsForType = new HashMap<mySampleSet, mySampleObs>(); 
			grades.put(_type,classValsForType);
		}		
		mySampleObs clsSample = classValsForType.get(_class);
		if(clsSample == null) {		clsSample = new mySampleObs (name+"|"+_class.name,_class, 0.0);}
		clsSample.setClippedValue(_type,_gr, 0.0, 1.0);
		classValsForType.put(_class,clsSample);
	}//setTransformedGrade
	
	public double getRawGrade(mySampleSet _class) {	return getTransformedGrade(myClassRoster.transTypes[myClassRoster.GB_rawGradeTypeIDX], _class);}
	
	public double getTransformedGrade(String _type,mySampleSet _class) {
		HashMap<mySampleSet, mySampleObs> classValsForType = grades.get(_type);
		if (classValsForType==null) {			return 0.0;	}// no values for this type for any classes set yet  - return 0		
		mySampleObs sample = classValsForType.get(_class);			//no grade for this particular class for specified type
		if (sample==null) {						return 0.0;	}//student has no transformed grade for this class, default to 0	- db message here if desired	
		return sample.getValue();
	}//getTransformedGrade

	public float getGradeXLoc(String _type,  mySampleSet _class, float ttlWidth) {
		HashMap<mySampleSet, mySampleObs> classValsForType = grades.get(_type);
		if (classValsForType==null) {				return 0.0f;		}// no values for this type for any classes set yet 	
		mySampleObs sample = classValsForType.get(_class);			//grade for class for specified type
		if (sample==null) {							return 0.0f;	 	}//student has no transformed grade for this class	
		return ttlWidth*sample.getFValue();
	}//getGradeXLoc
	
	//////////////////
	// draw this student on line of width ttlWidth - expected to be at start of line when this is called
	public void drawMeTransformed(IRenderInterface pa, String _type, mySampleSet _class, int[] _drawClr, float ttlWidth) {	
		HashMap<mySampleSet, mySampleObs> classValsForType = grades.get(_type);
		if (classValsForType==null) {					return;	}// no values for this type for any classes set yet 		
		mySampleObs smplVal = classValsForType.get(_class);			//grade for class for specified type
		if (smplVal==null) {								return;	}//student has no transformed grade for this class	
		float gr = smplVal.getFValue();
		smplVal.drawMe(pa, rad, _drawClr, new myPointf(ttlWidth*gr, 0.0f, 0.0f), shrtName +":"+ String.format("%.1f",gr * 100.0f));		
	}//drawMeTransformed

	
	
	//compare based on name first, then sort by ObjID
	@Override
	public int compareTo(mySampleObs othr) {
		int res = this.name.toLowerCase().compareTo(othr.name.toLowerCase());
		return (res == 0 ? Integer.compare(ObjID, othr.ObjID) : res);
	}//compareTo
	
	
	@Override
	public String toString() {
		String res = "Name : " + name + " ";	
		return res;
	}
}//myStudent