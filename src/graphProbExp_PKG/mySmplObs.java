package graphProbExp_PKG;

import java.util.HashMap;

/**
 * a single observation belonging to a sample set - values specified by inheriting class
 * @author john
 */
public class mySmplObs{
	public final int ObjID;
	private static int IDCnt = 0;
	//sample name
	public final String name;
	//black for stroke
	public static final int[] blkStrk = new int[] {0,0,0,255};
	//grey for disabled
	public static final int[] greyOff = new int[] {100,100,100,255};
	//location to put point for text display
	protected final myPointf textLoc;
	//owning sample set - always belongs to same sample set
	protected final mySampleSet smplSet;
	//sample value
	protected Double value;

	public mySmplObs(String _name, mySampleSet _smplSet, Double _value) {
		ObjID = IDCnt++;  name=_name;
		int tag = ObjID % 4;
		textLoc = new myPointf(-10, (tag < 2 ? 15 * (tag+1) : -10 * ( tag-1)) , 0);
		smplSet = _smplSet;
		value = _value;
	}//ctor
	
	//clip to be within 0->1 - should always be within this range
	protected void setClippedValue(String _type, double _val, double _min, double _max) {
		//_type present for dbg messages only - add any debugging display code here if desired
		if(_val >= _max) {			_val = _max;} 
		else if (_val <= _min ) {	_val = _min;}
		value = _val;
	}//clipVal
	
	protected void setValue(Double _newVal) {value=_newVal;}
	protected float getFValue() {return value.floatValue();}
	protected double getValue() {return value;}

	///////////////////////
	// sample draw function
	
	//pass location and display name of visualization for sample
	protected void drawMe(GraphProbExpMain pa, float rad, int[] _drawClr, myPointf _transLoc, String _dispName) {
		pa.pushMatrix();pa.pushStyle();
		pa.translate(_transLoc);
		pa.setFill(_drawClr,255); pa.setStroke(blkStrk,255);			
		pa.ellipse(0,0,rad,rad); 
		pa.ellipse(0,0,2,2);
		pa.showOffsetText(textLoc,pa.gui_White, _dispName);
		pa.popStyle();pa.popMatrix();
	}//drawMe
	
	
	@Override
	public String toString() {
		String res = "Name : " + name;		
		return res;
	}

}//class mySmplObs


/**
 * this class will hold an instance of a student, a collection of sample observations from multiple class rosters. 
 * This class will record their grade values and map them to a line
 * @author john
 *
 */
class myStudent implements Comparable<mySmplObs>{
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
	private HashMap<String,HashMap<mySampleSet, mySmplObs>> grades;
	
	public myStudent(GraphProbExpMain _pa, String _name) {
		ObjID = IDCnt++;  name=_name;
		clr = _pa.getRndClr2(255);//should be brighter colors
		shrtName = ""+(char)((ObjID % 26) + 65) + (ObjID / 26);
		grades = new HashMap<String,HashMap<mySampleSet, mySmplObs>>();
	}//ctor

	
	//clear out current transformed grades, preserving raw grades
	public void clearTransformedGrades(mySampleSet _cls) {
		for (String typ : grades.keySet()) {
			if (typ.equals("raw")) {continue;}		//don't clear raw grades
			HashMap<mySampleSet, mySmplObs> typeToClearForClasses = grades.get(typ);
			typeToClearForClasses.put(_cls,null);
		}
	}//clearTransformedGrades
	
	//convert transformed uniform grade to span 0->1, using passed min and max values of observed uniform grades in a particular class
	public void setScaledUniformGrade(mySampleSet _cls, String _transType, String _scaledUnitype,  double min, double max) {
		HashMap<mySampleSet, mySmplObs> allClassesUniType = grades.get(_transType);
		HashMap<mySampleSet, mySmplObs> allClassesScaledType = grades.get(_scaledUnitype);
		mySmplObs uniObs = allClassesUniType.get(_cls);
		mySmplObs sclObs = allClassesScaledType.get(_cls);
		if(null==sclObs) {			sclObs = new mySmplObs (name+"|"+_cls.name,_cls, 0.0);}	
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
			HashMap<mySampleSet, mySmplObs> classValsForType = grades.get(typ);
			mySmplObs finalObs = classValsForType.get(_finalRoster);
			if(finalObs == null) {		finalObs = new mySmplObs (name+"|"+_finalRoster.name,_finalRoster, 0.0);}
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
			HashMap<mySampleSet, mySmplObs> classValsForType = grades.get(typ);
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
				mySmplObs clsSample = classValsForType.get(_class);
				clsSample.setValue(clsSample.getValue() * ratio);
				classValsForType.put(_class, clsSample);
			}
		}
	}//disperseFromTotalGrade
	
	//set transformed values for student, keyed by type of transformation and class
	public void setTransformedGrade(String _type, mySampleSet _class, double _gr) {	
		HashMap<mySampleSet, mySmplObs> classValsForType = grades.get(_type);	
		if (classValsForType==null) {// no values for this class set yet - build new map of types for this class
			classValsForType = new HashMap<mySampleSet, mySmplObs>(); 
			grades.put(_type,classValsForType);
		}		
		mySmplObs clsSample = classValsForType.get(_class);
		if(clsSample == null) {		clsSample = new mySmplObs (name+"|"+_class.name,_class, 0.0);}
		clsSample.setClippedValue(_type,_gr, 0.0, 1.0);
		classValsForType.put(_class,clsSample);
	}//setTransformedGrade
	
	public double getRawGrade(mySampleSet _class) {	return getTransformedGrade(myClassRoster.transTypes[myClassRoster.GB_rawGradeTypeIDX], _class);}
	
	public double getTransformedGrade(String _type,mySampleSet _class) {
		HashMap<mySampleSet, mySmplObs> classValsForType = grades.get(_type);
		if (classValsForType==null) {			return 0.0;	}// no values for this type for any classes set yet  - return 0		
		mySmplObs sample = classValsForType.get(_class);			//no grade for this particular class for specified type
		if (sample==null) {						return 0.0;	}//student has no transformed grade for this class, default to 0	- db message here if desired	
		return sample.getValue();
	}//getTransformedGrade

	public float getGradeXLoc(String _type,  mySampleSet _class, float ttlWidth) {
		HashMap<mySampleSet, mySmplObs> classValsForType = grades.get(_type);
		if (classValsForType==null) {				return 0.0f;		}// no values for this type for any classes set yet 	
		mySmplObs sample = classValsForType.get(_class);			//grade for class for specified type
		if (sample==null) {							return 0.0f;	 	}//student has no transformed grade for this class	
		return ttlWidth*sample.getFValue();
	}//getGradeXLoc
	
	//////////////////
	// draw this student on line of width ttlWidth - expected to be at start of line when this is called
	public void drawMeTransformed(GraphProbExpMain pa, String _type, mySampleSet _class, int[] _drawClr, float ttlWidth) {	
		HashMap<mySampleSet, mySmplObs> classValsForType = grades.get(_type);
		if (classValsForType==null) {					return;	}// no values for this type for any classes set yet 		
		mySmplObs smplVal = classValsForType.get(_class);			//grade for class for specified type
		if (smplVal==null) {								return;	}//student has no transformed grade for this class	
		float gr = smplVal.getFValue();
		smplVal.drawMe(pa, rad, _drawClr, new myPointf(ttlWidth*gr, 0.0f, 0.0f), shrtName +":"+ String.format("%.1f",gr * 100.0f));		
	}//drawMeTransformed

	
	
	//compare based on name first, then sort by ObjID
	@Override
	public int compareTo(mySmplObs othr) {
		int res = this.name.toLowerCase().compareTo(othr.name.toLowerCase());
		return (res == 0 ? Integer.compare(ObjID, othr.ObjID) : res);
	}//compareTo
	
	
	@Override
	public String toString() {
		String res = "Name : " + name + " ";	
		return res;
	}
}//myStudent