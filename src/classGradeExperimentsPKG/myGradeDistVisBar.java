package classGradeExperimentsPKG;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_ProbTools.mySampleObs;
import base_ProbTools.randVisTools.myVisMgr;


/**
 * this class holds the functionality to manage a 1 dimensional 
 * @author john
 */

public class myGradeDistVisBar extends myVisMgr {

	//class this bar is attached to - if none means overall grade
	private final myClassRoster owningClass;
	//string descriptor of type of grades that this bar displays
	private String gradeType;
	
	//% of visible width the class bars' width should be
	private static final float barWidthMult = .95f;	
	
	//click box x,y,w,h dims, relative to upper left corner 
	private static float[] _clkBox;
	//this bar's color
	private final int[] barColor;
	//relative x for where bar starts from beginning of click-in box (i.e. how far over bar is)
	private static final float _barStX = 30;
	//y location of start of grade bar
	private final float _barStY;
	//specifically set color ctor
	public float barWidth;
	//this bar is enabled/disabled
	private boolean enabled;
	//student being moved by mouse click
	protected myStudent _modStudent;
	
	//specific color constructor - used to set up overall grade bar for a single class
	public myGradeDistVisBar(myClassRoster _owningClass, float[] _dims, String _typ, int[] _barColor, String _name) {
		super(new float[] {_dims[0],_dims[1], _barStX + (_owningClass.distPlotDimRect[2]*barWidthMult) ,_dims[2]}, _name);
		gradeType=_typ;
		setIsVisible(true);			//default bar to being visible
		_setDispWidthIndiv(_owningClass.distPlotDimRect[2]);
		_barStY = .5f * startRect[3];
		_clkBox = new float[] {0,-8+_barStY,16,16};
		barColor = _barColor;// getRndClr2 should be brighter colors
		owningClass = _owningClass;
	}//ctor
	//random color ctor - used by classes		
	
	@Override
	protected boolean _mouseClickIndiv(int msXLoc, int mxYLoc, int btn) {
		//clicked in enable/disable box
		if((msXLoc <= _clkBox[2]) && (mxYLoc >= _clkBox[1]) && (mxYLoc <= (_clkBox[1]+_clkBox[3]))) {	//clicked box - toggle state
			enabled = !enabled;	
			owningClass.setGradeBarEnabled(enabled, gradeType);	
			return true;	
		} else if(msXLoc >= _barStX) {		//clicked near student bar - grab a student and attempt to move
			//find nearest student
			float clickScale = ((msXLoc-_barStX)/barWidth);
			//see if student grade location is being clicked on
			_modStudent = owningClass.findClosestStudent(clickScale, gradeType);
			//System.out.println("x:"+msXLoc + "| y:"+mxYLoc+ " | clickScale : " + clickScale + "| Closest Student :  " + _modStudent.name);
			return true;
		}		
		return false;
	}//_mouseClickIndiv
	
	@Override
	protected boolean _mouseDragIndiv(int msXLoc, int mxYLoc, int btn) {
		//x location of click
		float clickScale = ((msXLoc-_barStX)/barWidth);
		//if moving a student, update grade
		if(_modStudent != null) {
			//System.out.println("_mouseDragIndiv for bar : " + ObjID + " clickScale : " + clickScale);
			_modStudent.setTransformedGrade(gradeType, owningClass,clickScale);
			return true;
		}
		return false;
	}//_mouseDragIndiv
	
	//if mouse over this bar - if near student, show student's name at mouse loc?
	@Override
	protected boolean _mouseOverIndiv(int msXLoc, int msYLoc) {
		return false;
	}//_mouseDragIndiv
	
	//release student being dragged
	@Override
	protected void _mouseReleaseIndiv() {		_modStudent = null;}//
	
	public float getAbsYLoc() {return startRect[1]+_barStY;}
	
	//translate to where the par part of this par starts, so the lines connecting grades for same students can be drawn
	public void transToBarStart(IRenderInterface pa) {pa.translate(startRect[0]+_barStX,startRect[1]+_barStY,0);}	
	//draw grade bar and student locations
	protected void _drawVisIndiv(IRenderInterface pa) {
		if(enabled) {
			pa.pushMatState();
			_drawBoxAndBar(pa,clr_green,barColor);
			for (myStudent s : owningClass.students.values()) {		s.drawMeTransformed(pa, gradeType, owningClass, s.clr, barWidth);	}
			pa.popMatState();					
		} else {							
			pa.pushMatState();
			_drawBoxAndBar(pa,clr_red,clr_grey);
			for (myStudent s : owningClass.students.values()) {		s.drawMeTransformed(pa, gradeType, owningClass, mySampleObs.greyOff, barWidth);	}		
			pa.popMatState();
		}
	}//_drawVisIndiv
	
	//draw box and bar with appropriate colors
	private void _drawBoxAndBar(IRenderInterface pa, int[] fClr, int[] _bClr) {
		pa.setFill(fClr,fClr[3]);
		pa.setStroke(clr_black,clr_black[3]);
		pa.drawRect(_clkBox);
		//move to where bar starts
		pa.translate(_barStX,_barStY,0);
		pa.setStroke(_bClr,_bClr[3]);
		pa.setStrokeWt(2.0f);
		pa.drawLine(0,0,0,barWidth,0,0);		
	}
	
	//set bar width for bar display
	@Override
	protected void _setDispWidthIndiv(float dispWidth) {
		barWidth = (dispWidth - _barStX) * barWidthMult;		
	}
	
	public boolean isBarEnabled() {return enabled;}
	public void setBarEnabled(boolean _en) {enabled=_en;}
	public void setType(String _typ) {gradeType=_typ;}
	public String getType() {return gradeType;}

		
}//class my1D_DistVis