package base_ProbTools.randVisTools;


import java.util.*;

import base_ProbTools.randGenFunc.gens.myRandGen;
import base_UI_Objects.*;

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
		pa.setFill(clr_white, clr_white[3]);
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






