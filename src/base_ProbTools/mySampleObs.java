package base_ProbTools;

import base_UI_Objects.*;
import base_Utils_Objects.*;
import base_Math_Objects.vectorObjs.floats.myPointf;
/**
 * a single observation belonging to a sample set - values specified by inheriting class
 * @author john
 */
public class mySampleObs{
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

	public mySampleObs(String _name, mySampleSet _smplSet, Double _value) {
		ObjID = IDCnt++;  name=_name;
		int tag = ObjID % 4;
		textLoc = new myPointf(-10, (tag < 2 ? 15 * (tag+1) : -10 * ( tag-1)) , 0);
		smplSet = _smplSet;
		value = _value;
	}//ctor
	
	//clip to be within 0->1 - should always be within this range
	public void setClippedValue(String _type, double _val, double _min, double _max) {
		//_type present for dbg messages only - add any debugging display code here if desired
		if(_val >= _max) {			_val = _max;} 
		else if (_val <= _min ) {	_val = _min;}
		value = _val;
	}//clipVal
	
	public void setValue(Double _newVal) {value=_newVal;}
	public float getFValue() {return value.floatValue();}
	public double getValue() {return value;}

	///////////////////////
	// sample draw function
	
	//pass location and display name of visualization for sample
	public void drawMe(my_procApplet pa, float rad, int[] _drawClr, myPointf _transLoc, String _dispName) {
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


