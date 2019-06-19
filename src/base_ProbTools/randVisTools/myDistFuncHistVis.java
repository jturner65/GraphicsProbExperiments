package base_ProbTools.randVisTools;

import java.util.TreeMap;

import base_ProbTools.randGenFunc.gens.myRandGen;
import base_UI_Objects.my_procApplet;

/**
 * this class will display the results of a random variable function/generator
 * @author john
 *
 */
public class myDistFuncHistVis extends myVisMgr {
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
		pa.setFill(clr_black,clr_black[3]);
		pa.setStroke(clr_white,clr_white[3]);
	
		//draw box around graph area
		pa.drawRect(frameDims);
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

