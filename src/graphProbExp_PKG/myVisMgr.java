package graphProbExp_PKG;

import java.util.*;
import base_UI_Objects.*;
import base_Utils_Objects.*;

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
	protected String name;
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
			clr_clearWite = new int[] {255,255,255,50},
			clr_red = new int[] {255,0,0,255}, 
			clr_green = new int[] {0,255,0,255},
			clr_cyan = new int[] {0,255,255,255},
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
		boolean inClickRegion = (msXLoc >= 0) && (mxYLoc >= 0) && (msXLoc <= startRect[2]) && (mxYLoc <= startRect[3]);
		if (!inClickRegion) {mouseRelease();return false;}
		//handle individual mouse click in the bounds of this object
		return _mouseClickIndiv(msXLoc, mxYLoc, btn);
	}//checkMouseClick
	
	//specifically if moved or dragged within the bounds of this visualization, and move the frame of the mouse current drag location to be the upper left corner of this object's clickable region
	//drag has btn > 0, mouse-over has button < 0
	public boolean checkMouseMoveDrag(int msx, int msy, int btn) {
		if(!getFlag(isVisibleIDX)) {return false;}
		//transform to top left corner of box region
		int msXLoc = (int) (msx - startRect[0]), mxYLoc = (int) (msy - startRect[1]);
		//exists in clickable region
		boolean inClickRegion = (msXLoc >= 0) && (mxYLoc >= 0) && (msXLoc <= startRect[2]) && (mxYLoc <= startRect[3]);
		//System.out.println("checkMouseMoveDrag ID : " + ObjID + " inClickRegion : " + inClickRegion + " | Relative x : " + msXLoc + " | y : " + mxYLoc + " | orig x : " + msx + " | y : " + msy + " | Rect : ["+ startRect[0]+","+ startRect[1]+","+ startRect[2]+","+ startRect[3]+"] | mseBtn : " + btn);
		if (!inClickRegion) { mouseRelease(); return false;}
		//if btn < 0 then mouse over within the bounds of this object
		if (btn < 0 ) {return _mouseOverIndiv(msXLoc, mxYLoc);}
		//if btn >= 0 then mouse drag in bounds of this object
		return _mouseDragIndiv(msXLoc, mxYLoc, btn);
	}//checkMouseMoveDrag
	
	//modify name to reflect changes in underlying data/distribution
	public void updateName(String _newName) {
		name = _newName;
	}
	
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
	protected abstract boolean _mouseClickIndiv(int msXLoc, int mxYLoc, int btn);
	protected abstract boolean _mouseDragIndiv(int msXLoc, int mxYLoc, int btn);
	protected abstract boolean _mouseOverIndiv(int msXLoc, int mxYLoc);
	protected abstract void _mouseReleaseIndiv();
	protected abstract void _setDispWidthIndiv(float dispWidth);
	
	public void drawVis(my_procApplet pa) {
		if(!getFlag(isVisibleIDX)) {return;}
		pa.pushMatrix();pa.pushStyle();
		pa.translate(startRect[0], startRect[1],0);
		pa.setFill(clr_white);
		pa.text(name, 0, 0);
		_drawVisIndiv(pa);
		pa.popStyle();pa.popMatrix();			
	}//drawVis
	
	protected abstract void _drawVisIndiv(my_procApplet pa);
	
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
	public gradeBar(myClassRoster _owningClass, float[] _dims, String _typ, int[] _barColor, String _name) {
		super(new float[] {_dims[0],_dims[1], _barStX + (_owningClass.probExp.getVisibleSreenWidth()*barWidthMult) ,_dims[2]}, _name);
		gradeType=_typ;
		setIsVisible(true);			//default bar to being visible
		_setDispWidthIndiv(_owningClass.probExp.getVisibleSreenWidth());
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
	public void transToBarStart(my_procApplet pa) {pa.translate(startRect[0]+_barStX,startRect[1]+_barStY,0);}	
	//draw grade bar and student locations
	protected void _drawVisIndiv(my_procApplet pa) {
		if(enabled) {
			pa.pushMatrix();pa.pushStyle();
			_drawBoxAndBar(pa,clr_green,barColor);
			for (myStudent s : owningClass.students.values()) {		s.drawMeTransformed(pa, gradeType, owningClass, s.clr, barWidth);	}
			pa.popStyle();pa.popMatrix();					
		} else {							
			pa.pushMatrix();pa.pushStyle();
			_drawBoxAndBar(pa,clr_red,clr_grey);
			for (myStudent s : owningClass.students.values()) {		s.drawMeTransformed(pa, gradeType, owningClass, mySmplObs.greyOff, barWidth);	}		
			pa.popStyle();pa.popMatrix();
		}
	}//_drawVisIndiv
	
	//draw box and bar with appropriate colors
	private void _drawBoxAndBar(my_procApplet pa, int[] fClr, int[] _bClr) {
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
	protected void _setDispWidthIndiv(float dispWidth) {
		barWidth = (dispWidth - _barStX) * barWidthMult;		
	}
	
	public boolean isBarEnabled() {return enabled;}
	public void setBarEnabled(boolean _en) {enabled=_en;}
	public void setType(String _typ) {gradeType=_typ;}
	public String getType() {return gradeType;}

		
}//class gradeBar

/**
 * this class will display the results of a random variable function/generator
 * @author john
 *
 */
class myDistFuncHistVis extends myVisMgr {
	//the func to draw that owns this visMgr
	private final myRandGen randGen;
	//graph frame dims
	protected float[] frameDims = new float[4];
	//bounds for graph box - left, top, right, bottom
	protected static final float[] frmBnds = new float[] {60.0f, 30.0f, 20.0f, 20.0f};
		
	//whether this is currently display function values or histogram values; show specific plots
	private boolean showHist, showSpecifiedPlots;
	//which plots to show
	private String[] specifiedPlots;
	//vis objects to render each function/histogram graph
	private TreeMap<String, myBaseDistVisObj> distVisObjs;
	//string keys representing current function and hist keys for plots to show
	private String funcKey, histKey;
	
	public myDistFuncHistVis(float[] _dims, myRandGen _gen) {
		super(new float[] {_dims[0],_dims[1],_dims[2], _dims[3]},"Vis of " + _gen.name);
		initDistVisObjs();		
		setGraphFrameDims();
		randGen=_gen;
		
	}//ctor
	
	private void initDistVisObjs() {
		distVisObjs = new TreeMap<String, myBaseDistVisObj>();
		specifiedPlots = new String[0];
	}//initDistVisObjs

	private void setGraphFrameDims() {//start x, start y, width, height
		frameDims = new float[] {frmBnds[0], frmBnds[1], startRect[2]-frmBnds[0]-frmBnds[2], startRect[3]-frmBnds[1]-frmBnds[3]}; 
		if(distVisObjs!=null) {
			for (String key : distVisObjs.keySet()) {distVisObjs.get(key).setFrameDims(frameDims);}
		}
	}
	
	//set the current strings to display for multi disp
	public void setCurMultiDispVis(String[] _distStrList, double[][] _minMaxDiff) {
		specifiedPlots = _distStrList;
		if(_minMaxDiff != null) {
			for(String key : specifiedPlots) {	
				if(key.equals(histKey)) {continue;}
				distVisObjs.get(key).setMinMaxDiffVals(_minMaxDiff);	
				
			}
		}
		showSpecifiedPlots = true;	
	}//setCurDispVis
	//clear out list of multi-dist plots to show, and turn off function
	public void clearCurDispVis() {
		specifiedPlots = new String[0];
		showSpecifiedPlots = false;			
	}//clearCurDispVis
	
//
	//set function and display values from randGen; scale function values to properly display in frame
	//_funcVals : x and y values of function to be plotted; 
	//_minMaxDiffFuncVals : min, max, diff y values of function to be plotted, for scaling
	public void setValuesFunc(String _funcKey, int[][] dispClrs, double[][] _funcVals, double[][] _minMaxDiffFuncVals) {
		funcKey = _funcKey;
		myBaseDistVisObj funcObj = distVisObjs.get(funcKey);
		if(funcObj == null) { 
			funcObj = new myFuncVisObj(this, dispClrs);distVisObjs.put(funcKey,funcObj);
		} 		
		funcObj.setVals(_funcVals, _minMaxDiffFuncVals);
		showHist = false;
		showSpecifiedPlots = false;			
		setIsVisible(true);
	}//setValuesFunc
	
	public void setColorVals(String _functype,String _clrtype, int[] _clr) {
		switch(_clrtype.toLowerCase()) {
		case "fill":{		distVisObjs.get(_functype).setFillColor(_clr);		break;}
		case "stroke":{		distVisObjs.get(_functype).setStrkColor(_clr);		break;}
		}
	}
	
	//set values to display a distribution result - display histogram
	//_bucketVals : n buckets(1st idx); idx2 : idx 0 is lower x value of bucket, y value is count; last entry should always have 0 count
	//_minMaxFuncVals : 1st array is x min,max, diff; 2nd array is y axis min, max, diff
	public void setValuesHist(String _histKey, int[][] dispClrs, double[][] _bucketVals, double[][] _minMaxDiffHistVals) {
		histKey = _histKey;
		myBaseDistVisObj histObj = distVisObjs.get(histKey);
		if(histObj == null) { histObj = new myHistVisObj(this, dispClrs);distVisObjs.put(histKey,histObj);} 		
		histObj.setVals(_bucketVals, _minMaxDiffHistVals);
		showHist = true;
		showSpecifiedPlots = false;			
		setIsVisible(true);
	}//setValuesHist
	
	//clear precalced values for visualization
	public void clearEvalVals() {
		System.out.println("clearEvalVals called");
		for (String key : distVisObjs.keySet()) {distVisObjs.get(key).clearEvalVals();}
		showHist = false;
		clearCurDispVis();
		setIsVisible(false);
	}//clearVals
	
	@Override
	protected boolean _mouseClickIndiv(int msXLoc, int mxYLoc, int btn) {
		return false;
	}//_mouseClickIndiv

	@Override
	protected boolean _mouseDragIndiv(int msXLoc, int mxYLoc, int btn) {
		return false;
	}//_mouseDragIndiv

	@Override
	protected boolean _mouseOverIndiv(int msXLoc, int mxYLoc) {
		return false;
	}//_mouseOverIndiv

	@Override
	protected void _mouseReleaseIndiv() {		
	}//_mouseReleaseIndiv

	@Override
	protected void _setDispWidthIndiv(float dispWidth) {	
		//resize frame and pass on to disp objects
		setGraphFrameDims();	
	}//_setDispWidthIndiv
	
	public void setSpecificMinMaxDiff(String key, double[][] _minMaxDiff) {
		myBaseDistVisObj obj = distVisObjs.get(key);
		if(null==obj) {System.out.println("Error attempting to set minMaxDiff ara for vis obj key "+ key +" : Object doesn't exist.  Aborting"); return;}
		obj.setMinMaxDiffVals(_minMaxDiff);
	}
	
	public double[][] getSpecificMinMaxDiff(String key){
		myBaseDistVisObj obj = distVisObjs.get(key);
		if(null==obj) {System.out.println("Error attempting to get minMaxDiff ara for vis obj key "+ key +" : Object doesn't exist.  Aborting"); return new double[0][];}
		return obj.getMinMaxDiffVals();
	}
		
	@Override
	public void _drawVisIndiv(my_procApplet pa) {
		pa.setFill(clr_black);
		pa.setStroke(clr_white);
	
		//draw box around graph area
		pa.rect(frameDims);
		pa.pushMatrix();pa.pushStyle();
		pa.translate(frameDims[0],frameDims[1]+frameDims[3], 0.0f);
		//pa.sphere(3.0f);	
		if(showSpecifiedPlots) {
			for(String key : specifiedPlots) {			distVisObjs.get(key).drawMe(pa, true);		}
//			float _yLocOfXZero = baseObj.zeroAxisVals[0], _xLocOfYZero = baseObj.zeroAxisVals[1];
//			for(String key : specifiedPlots) {distVisObjs.get(key).drawMeAligned(pa, _yLocOfXZero, _xLocOfYZero);}
			
		} 
		else if (showHist) {			distVisObjs.get(histKey).drawMe(pa, false);			} 
		else {							distVisObjs.get(funcKey).drawMe(pa, false);		}

		pa.popStyle();pa.popMatrix();
	}//_drawVisIndiv
	
}//myDistFuncHistVis

//manage the visualization of a single distribution evaluation, either a histogram or a functional evaluation
abstract class myBaseDistVisObj{
	protected myDistFuncHistVis owner;
	//location of axis ticks
	private float[][] axisVals;
	//axis tick value to display
	private double[][] axisDispVals;
	//x,y vals for calculation - n x 2 array, n points of x=0 idx, y=1 idx values; min, max, diff values of func eval (in x=idx 0 and in y = idx 1)
	private double[][] vals, minMaxDiffVals;
	//x,y vals for display - n x 2 array, n points of x=0 idx, y=1 idx values - x=0 -> dispWidth; y=0->dispHeight; axis values, to be displayed at equally space intervals along axis
	protected float[][] dispVals;
	//y and x values for display of x and y "0" axes, respectively
	protected float[] zeroAxisVals;	
	//whether or not to draw special axes (if shown in graph)
	private boolean[] drawZeroAxes;
	//graph frame dims
	private float[] frameDims = new float[4];
	//format strings for x and y values to display on graphs
	private final String fmtXStr = "%3.4f", fmtYStr = "%3.4f";
	//axis tick dim on either side of axis
	private static final float tic = 5.0f;
	//# of values to display on axis
	protected static final int numAxisVals = 21;
	
	//colors for display
	protected int[] fillClr, strkClr;

	
	public myBaseDistVisObj(myDistFuncHistVis _owner, int[][] _clrs) {
		owner=_owner;
		clearEvalVals();
		setFrameDims(owner.frameDims);
		fillClr = _clrs[0];
		strkClr = _clrs[1];
	}//ctor

	public void setFrameDims(float[] _fd) {frameDims = _fd;rescaleDispValues(minMaxDiffVals);}
	
	public void setVals(double[][] _Vals, double[][] _minMaxDiffVals) {
		vals = _Vals;
		minMaxDiffVals = _minMaxDiffVals;
		rescaleDispValues(minMaxDiffVals);		
	}
	
	public void setFillColor(int[] _clr) {fillClr =_clr;}
	public void setStrkColor(int[] _clr) {strkClr =_clr;}
	
	private float _calcScale(double x, double min, double diff) {return	(float)((x-min)/diff);}
	
	public void clearEvalVals() {
		vals = new double[0][0];
		dispVals = new float[0][0];
		axisVals = new float[0][0];
		axisDispVals = new double[0][0]; 
		minMaxDiffVals = new double[2][3];
	}//clearVals
	
	public void rescaleDispValues(double[][] _minMaxDiffVals) {	
		dispVals = new float[vals.length][2];//x,y values for each point
		drawZeroAxes = new boolean[2];
		for(int i=0;i<vals.length;++i) {	
			//float scaleX = (float) ((funcVals[i][0] - minMaxDiffFuncVals[0][0])/minMaxDiffFuncVals[0][2]);
			float scaleX = _calcScale(vals[i][0], _minMaxDiffVals[0][0],_minMaxDiffVals[0][2]);
			dispVals[i][0] = scaleX*frameDims[2];
			//set y values to be negative so will display properly (up instead of down)
			//how much to scale height
			//float scaleY = -(float) ((funcVals[i][1] - minMaxDiffFuncVals[1][0])/minMaxDiffFuncVals[1][2]);
			float scaleY = -_calcScale(vals[i][1], _minMaxDiffVals[1][0],_minMaxDiffVals[1][2]);
			dispVals[i][1] =  scaleY*frameDims[3]*.95f;		
		}	
		//check whether or not we will build display axes
		for (int i=0;i<drawZeroAxes.length;++i) {		drawZeroAxes[(i+1)%2] = ((_minMaxDiffVals[i][0] < 0) && (_minMaxDiffVals[i][1] > 0));		}
		//build values for axes display - location and value - now that 
		buildAxisVals(_minMaxDiffVals);
	}//rescaleDispValues
	
	//build axis values to display along axes - min/max/diff vals need to be built
	private void buildAxisVals(double[][] _minMaxDiffVals) {
		axisVals = new float[numAxisVals][2];
		axisDispVals = new double[numAxisVals][2]; 
		zeroAxisVals = new float[2];
		float _denom = (1.0f*numAxisVals-1);
		//width between ticks for x and y
		float[] rawDimAra = new float[] {frameDims[2], -(frameDims[3]*.95f)};
		float[] denomAra = new float[] {rawDimAra[0]/_denom, rawDimAra[1]/_denom};
		for(int i=0;i<axisVals.length;++i) {
			float iterDenom = i/_denom;
			for (int j=0;j<2;++j) {//j == x=0,y=1 
				//location of tick line
				axisVals[i][j] = i*denomAra[j];	
				//value to display
				axisDispVals[i][j] = _minMaxDiffVals[j][0] + (iterDenom *_minMaxDiffVals[j][2]);	
			}	
		}	
		for(int i=0;i<2;++i) {
			//zeroAxisVals[(i+1)%2] = (float) ((-minMaxDiffFuncVals[i][0]/minMaxDiffFuncVals[i][2])*rawDimAra[i]);
			zeroAxisVals[(i+1)%2] = _calcScale(0, _minMaxDiffVals[i][0],_minMaxDiffVals[i][2])*rawDimAra[i];
		}
	}//buildAxisVals
	
	public double[][] getMinMaxDiffVals(){ return minMaxDiffVals;}	
	public void setMinMaxDiffVals(double[][] _minMaxDiffVals) {
		minMaxDiffVals = new double[_minMaxDiffVals.length][];
		for(int i=0;i<minMaxDiffVals.length;++i) {
			int len = _minMaxDiffVals[i].length;
			minMaxDiffVals[i] = new double[len];
			System.arraycopy(_minMaxDiffVals[i], 0, minMaxDiffVals[i], 0, len);
		}
		rescaleDispValues(minMaxDiffVals);	
	}
	
	///////////////////
	// drawing routines

	//draw axis lines through 0,0 and give tags
	private void _drawZeroLines(my_procApplet pa) {
		pa.pushMatrix();pa.pushStyle();
		pa.strokeWeight(2.0f);
		pa.setFill(owner.clr_cyan);
		if (drawZeroAxes[0]) {//draw x==0 axis
			float yVal = zeroAxisVals[0];
			//draw line @ y Val
			pa.setStroke(owner.clr_white);
			pa.line(-tic, yVal, 0, tic, yVal, 0);
			//draw line to other side
			pa.setStroke(owner.clr_cyan);
			pa.line(-tic, yVal, 0, frameDims[2], yVal, 0);
			//draw text for display
			pa.setStroke(owner.clr_white);
			pa.pushMatrix();pa.pushStyle();
			pa.translate(-tic-10, yVal+5.0f,0);
			pa.scale(1.4f);
			pa.text("0", 0,0);
			pa.popStyle();pa.popMatrix();
		}
		
		if (drawZeroAxes[1]) {//draw y==0 axis
			float xVal = zeroAxisVals[1];
			//draw tick line @ x Val
			pa.setStroke(owner.clr_white);
			pa.line(xVal, -tic, 0, xVal, tic, 0);	
			//draw line to other side
			pa.setStroke(owner.clr_cyan);
			pa.line(xVal, -frameDims[3], 0, xVal, tic, 0);	
			//draw text for display
			pa.setStroke(owner.clr_white);
			pa.pushMatrix();pa.pushStyle();
			pa.translate( xVal - 4.0f, tic+20.0f,0);
			pa.scale(1.4f);
			pa.text("0", 0,0);
			pa.popStyle();pa.popMatrix();
		}		
		pa.popStyle();pa.popMatrix();
	}//
	
	//draw x and y axis values
	//offset == 0 for axes on left, offset == frameDims[2] for offset on right
	protected void drawAxes(my_procApplet pa, float offset) {
		pa.pushMatrix();pa.pushStyle();
		pa.setFill(owner.clr_white);
		float yAxisTxtXOffset = offset -tic-owner.frmBnds[0]+10 ,
				yAxisTxtYOffset = (offset == 0.0)? 5.0f : -4.0f;
		for (int idx = 0; idx <axisVals.length;++idx) {
			float xVal = axisVals[idx][0];
			String dispX = String.format(fmtXStr, axisDispVals[idx][0]); 
			//draw tick line @ x Val
			pa.setStroke(owner.clr_white);
			pa.line(xVal, -tic, 0, xVal, tic, 0);	
			//draw line to other side
			pa.setStroke(owner.clr_clearWite);
			pa.line(xVal, -frameDims[3], 0, xVal, tic, 0);	
			//draw text for display
			pa.text(dispX, xVal - 20.0f, tic+10.0f);
			if(idx%2==0) {//only draw every other y tick
				float yVal = axisVals[idx][1];
				String dispY = String.format(fmtYStr, axisDispVals[idx][1]);
				//draw line @ y Val
				pa.setStroke(owner.clr_white);
				pa.line(-tic + offset, yVal, 0, tic + offset, yVal, 0);
				//draw line to other side
				pa.setStroke(owner.clr_clearWite);
				pa.line(-tic, yVal, 0, frameDims[2], yVal, 0);
				//draw text for display
				pa.text(dispY, yAxisTxtXOffset, yVal+yAxisTxtYOffset);
			}
		}
		_drawZeroLines(pa);
		pa.popStyle();pa.popMatrix();
	}//drawAxes
	
	public final void drawMe(my_procApplet pa, boolean isMulti) {
		pa.setFill(fillClr);
		pa.setStroke(strkClr);
		_drawCurve(pa,isMulti ? frameDims[2] : 0);
	}//drawMe

	protected abstract void _drawCurve(my_procApplet pa, float offset);
	
}//myBaseDistVisObj

//visualize a functional object evaluation - draws a line
class myFuncVisObj extends myBaseDistVisObj{
	public myFuncVisObj(myDistFuncHistVis _owner, int[][] _clrs) {
		super(_owner, _clrs);
	}
	
	@Override
	protected void _drawCurve(my_procApplet pa, float offset) {
		pa.point(dispVals[0][0], dispVals[0][1], 0);
		for (int idx = 1; idx <dispVals.length;++idx) {	
			//draw point 			
			pa.point(dispVals[idx][0], dispVals[idx][1], 0);
			//draw line between points
			pa.line(dispVals[idx-1][0], dispVals[idx-1][1], 0, dispVals[idx][0], dispVals[idx][1], 0);
		}		
		drawAxes(pa, 0);
	}//_drawCurve

}//myFuncVisObj

//histogram evaluation of a pdf - draws buckets
class myHistVisObj extends myBaseDistVisObj{
	
	public myHistVisObj(myDistFuncHistVis _owner, int[][] _clrs) {
		super(_owner, _clrs);
	}
	
	protected void _drawCurve(my_procApplet pa, float offset) {
		for (int idx = 0; idx <dispVals.length-1;++idx) {	
			pa.rect(dispVals[idx][0], 0, (dispVals[idx+1][0]-dispVals[idx][0]), dispVals[idx][1]);			
		}		
		drawAxes(pa, offset);
	}//_drawCurve

	
}//myHistVisObj