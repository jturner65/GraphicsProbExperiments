package graphProbExp_PKG;

/**
 * this class will provide I/O functionality for graphical representations of distributions
 * ultimately an instancing object of this class should be able to be placed somewhere on a 2D screen
 * and will provide interaction 
 * @author john
 *
 */
public abstract class myVisMgr {
	//owning probability experiment
	//public BaseProbExpMgr expMgr;
	public final int ObjID;
	private static int IDCnt = 0;
	//title string to display over visualiztion
	protected final String name;
	//check in this rectangle for a click in this object -> xStart,yStart (upper left corner), width,height
	protected final float[] startRect;
	//internal to base class state flags - bits in array holding relevant process info restricted to base class
	protected int[] stFlags;						
	protected static final int
			debugIDX 				= 0,
			isVisibleIDX			= 1;
	protected static final int numStFlags = 2;	
	
	//prebuilt colors that will be used often
	protected static final int[] 
			clr_black = new int[] {0,0,0,255},
			clr_white = new int[] {255,255,255,255},
			clr_red = new int[] {255,0,0,255}, 
			clr_green = new int[] {0,255,0,255},
			clr_grey = new int[] {100,100,100,255};	
	
	
	public myVisMgr(float[] _startRect, String _name) {
		ObjID = IDCnt++; startRect = _startRect;name=_name;
		initFlags();
		setDispWidth(startRect[2]);
	}//ctor
	
	//check if mouse has been clicked within the bounds of this visualization, and move the frame of the mouse click location to be the upper left corner of this object's clickable region
	public boolean checkMouseClick(int msx, int msy, int btn) {
		if(!getFlag(isVisibleIDX)) {return false;}
		//transform to top left corner of box region
		int msXLoc = (int) (msx - startRect[0]), mxYLoc = (int) (msy - startRect[1]);
		boolean inClassLineRegion = (msXLoc >= 0) && (mxYLoc >= 0) && (msXLoc <= startRect[2]) && (mxYLoc <= startRect[3]);
		if (!inClassLineRegion) {mouseRelease();return false;}
		//handle individual mouse click in the bounds of this object
		return _mouseClickIndiv(msXLoc, mxYLoc, btn);
	}//checkMouseClick
	
	//specifically if moved or dragged within the bounds of this visualization, and move the frame of the mouse current drag location to be the upper left corner of this object's clickable region
	//drag has btn > 0, mouse-over has button < 0
	public boolean checkMouseMoveDrag(int msx, int msy, int btn) {
		if(!getFlag(isVisibleIDX)) {return false;}
		//transform to top left corner of box region
		int msXLoc = (int) (msx - startRect[0]), mxYLoc = (int) (msy - startRect[1]);
		//System.out.println("ID : " + ObjID + " | Relative x : " + msXLoc + " | y : " + mxYLoc + " | orig x : " + msx + " | y : " + msy + " | Rect : ["+ startRect[0]+","+ startRect[1]+","+ startRect[2]+","+ startRect[3]+"] | mseBtn : " + btn);
		boolean inClassLineRegion = (msXLoc >= 0) && (mxYLoc >= 0) && (msXLoc <= startRect[2]) && (mxYLoc <= startRect[3]);
		if (!inClassLineRegion) { mouseRelease(); return false;}
		//if btn < 0 then mouse over within the bounds of this object
		if (btn < 0 ) {return _mouseOverIndiv(msXLoc, mxYLoc);}
		//if btn >= 0 then mouse drag in bounds of this object
		return _mouseDragIndiv(msXLoc, mxYLoc, btn);
	}//checkMouseMoveDrag
	
	//functionality when mouse is released
	public void mouseRelease(){
		//any base class functions for release
		_mouseReleaseIndiv();
	}//mouseRelease
	
	//set visible display width
	public void setDispWidth(float _dispWidth) {		
		startRect[2] = _dispWidth;	
		_setDispWidthIndiv(_dispWidth);
	}//setDispWidth
	
	//instance class-specific functionality
	public abstract boolean _mouseClickIndiv(int msXLoc, int mxYLoc, int btn);
	public abstract boolean _mouseDragIndiv(int msXLoc, int mxYLoc, int btn);
	public abstract boolean _mouseOverIndiv(int msXLoc, int mxYLoc);
	public abstract void _mouseReleaseIndiv();
	public abstract void _setDispWidthIndiv(float dispWidth);
	
	public void drawVis(GraphProbExpMain pa) {
		if(!getFlag(isVisibleIDX)) {return;}
		pa.pushMatrix();pa.pushStyle();
		pa.translate(startRect[0], startRect[1],0);
		pa.setFill(clr_white);
		pa.text(name, 0, 0);
		_drawVisIndiv(pa);
		pa.popStyle();pa.popMatrix();			
	}
	
	protected abstract void _drawVisIndiv(GraphProbExpMain pa);
	
	public void setIsVisible(boolean _isVis) {setFlag(isVisibleIDX, _isVis);}
	
	private void initFlags(){stFlags = new int[1 + numStFlags/32]; for(int i = 0; i<numStFlags; ++i){setFlag(i,false);}}
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag 
			case debugIDX 		  : {
				break;}	
			case isVisibleIDX	  : {
				break;}				
		}
	}//setFlag		
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}
}//class myDistributionDisplay

/**
 * this class holds the functionality to manage a class's grade bar display, along with the overall grade bar display
 * @author john
 */
class gradeBar extends myVisMgr {

	//class this bar is attached to - if none means overall grade
	private final myClassRoster owningClass;
	//string descriptor of type of grades that this bar displays
	private String gradeType;
	
	//% of visible width the class bars' width should be
	private static final float barWidthMult = .95f;	
	
	//click box x,y,w,h dims, relative to upper left corner TODO
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
	public gradeBar(myClassRoster _owningClass, float[] _dims, String _typ, int[] _barColor, String _name) {
		super(new float[] {_dims[0],_dims[1], _barStX + (_owningClass.gradeExp.getVisibleSreenWidth()*barWidthMult) ,_dims[2]}, _name);
		gradeType=_typ;
		setIsVisible(true);			//default bar to being visible
		_setDispWidthIndiv(_owningClass.gradeExp.getVisibleSreenWidth());
		_barStY = .5f * startRect[3];
		_clkBox = new float[] {0,-8+_barStY,16,16};
		barColor = _barColor;// getRndClr2 should be brighter colors
		owningClass = _owningClass;
	}//ctor
	//random color ctor - used by classes
	
	@Override
	public boolean _mouseClickIndiv(int msXLoc, int mxYLoc, int btn) {
		//clicked in enable/disable box
		if((msXLoc <= _clkBox[2]) && (mxYLoc >= _clkBox[1]) 
				&& (mxYLoc <= (_clkBox[1]+_clkBox[3]))) {	enabled = !enabled;	owningClass.setGradeBarEnabled(enabled, gradeType);	return true;	} 	//clicked box - toggle state
		//clicked near student bar - grab a student and attempt to move
		else if(msXLoc >= _barStX) {
			float clickScale = ((msXLoc-_barStX)/barWidth);
			//see if student grade location is being clicked on
			_modStudent = owningClass.findClosestStudent(clickScale, gradeType);
			//System.out.println("x:"+msXLoc + "| y:"+mxYLoc+ " | clickScale : " + clickScale + "| Closest Student :  " + _modStudent.name);
			return true;
		}		
		return false;
	}
	@Override
	public boolean _mouseDragIndiv(int msXLoc, int mxYLoc, int btn) {
		//x location of click
		float clickScale = ((msXLoc-_barStX)/barWidth);
		//if moving a student, update grade
		if(_modStudent != null) {
			_modStudent.setTransformedGrade(gradeType, owningClass,clickScale);
			return true;
		}
		return false;
	}//_mouseDragIndiv
	
	
	//if mouse over this bar - if near student, show student's name at mouse loc?
	@Override
	public boolean _mouseOverIndiv(int msXLoc, int msYLoc) {
		return false;
	}//_mouseDragIndiv

	
	//release student being dragged
	@Override
	public void _mouseReleaseIndiv() {		_modStudent = null;}//
	
	public float getAbsYLoc() {return startRect[1]+_barStY;}
	
	//translate to where the par part of this par starts, so the lines connecting grades for same students can be drawn
	public void transToBarStart(GraphProbExpMain pa) {pa.translate(startRect[0]+_barStX,startRect[1]+_barStY,0);}	
	//draw grade bar and student locations
	protected void _drawVisIndiv(GraphProbExpMain pa) {
		if(enabled) {
			pa.pushMatrix();pa.pushStyle();
			_drawBoxAndBar(pa,clr_green,barColor);
			for (myStudent s : owningClass.students.values()) {		s.drawMeTransformed(pa, gradeType, owningClass, barWidth);	}
			pa.popStyle();pa.popMatrix();					
		} else {							
			pa.pushMatrix();pa.pushStyle();
			_drawBoxAndBar(pa,clr_red,clr_grey);
			for (myStudent s : owningClass.students.values()) {		s.drawMeTransformedOff(pa, gradeType, owningClass, barWidth);	}		
			pa.popStyle();pa.popMatrix();
		}
	}//_drawVisIndiv
	
	//draw box and bar with appropriate colors
	private void _drawBoxAndBar(GraphProbExpMain pa, int[] fClr, int[] _bClr) {
		pa.setFill(fClr);
		pa.setStroke(clr_black);
		pa.rect(_clkBox);
		//move to where bar starts
		pa.translate(_barStX,_barStY,0);
		pa.setStroke(_bClr);
		pa.strokeWeight(2.0f);
		pa.line(0,0,0,barWidth,0,0);		
	}
	
	//set bar width for bar display
	@Override
	public void _setDispWidthIndiv(float dispWidth) {
		barWidth = (dispWidth - _barStX) * barWidthMult;		
	}
	
	public boolean isBarEnabled() {return enabled;}
	public void setBarEnabled(boolean _en) {enabled=_en;}
	public void setType(String _typ) {gradeType=_typ;}
	public String getType() {return gradeType;}

		
}//class gradeBar

/**
 * this class will display the results of a random variable function 
 * @author john
 *
 */
class myDistVis extends myVisMgr {
	//the func to draw
	private myRandVarFunc func;
	//graph frame dims
	private float[] frameDims = new float[4];
	//bounds for graph box - left, top, right, bottom
	private static final float[] frmBnds = new float[] {10.0f, 30.0f, 10.0f, 20.0f};
	
	
	public myDistVis(float[] _startRect, myRandVarFunc _func) {
		super(_startRect,"Visualization of " + _func.name);
		setGraphFrameDims();
		func=_func;
	}//ctor

	private void setGraphFrameDims() {
		frameDims = new float[] {frmBnds[0], frmBnds[1], startRect[2]-frmBnds[0]-frmBnds[2], startRect[3]-frmBnds[1]-frmBnds[3]};  		
	}
	
	//set values to display
	public void setValsToDisp(int numVals, double low, double high) {
		
	}
	
	
	
	@Override
	public boolean _mouseClickIndiv(int msXLoc, int mxYLoc, int btn) {
		// TODO Auto-generated method stub
		return false;
	}//_mouseClickIndiv

	@Override
	public boolean _mouseDragIndiv(int msXLoc, int mxYLoc, int btn) {
		// TODO Auto-generated method stub
		return false;
	}//_mouseDragIndiv

	@Override
	public boolean _mouseOverIndiv(int msXLoc, int mxYLoc) {
		// TODO Auto-generated method stub
		return false;
	}//_mouseOverIndiv

	@Override
	public void _mouseReleaseIndiv() {
		// TODO Auto-generated method stub
		
	}//_mouseReleaseIndiv

	@Override
	public void _setDispWidthIndiv(float dispWidth) {
		//resize frame
		setGraphFrameDims();
		
	}//_setDispWidthIndiv

	@Override
	public void _drawVisIndiv(GraphProbExpMain pa) {
		pa.setFill(clr_white);
		pa.setStroke(clr_black);
		pa.rect(frameDims);
		
	}//_drawVisIndiv
	
}