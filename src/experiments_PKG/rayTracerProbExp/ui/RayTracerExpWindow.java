package experiments_PKG.rayTracerProbExp.ui;

import java.io.File;
import java.util.TreeMap;

import base_RayTracer.ui.base.Base_RayTracerUIUpdater;
import base_RayTracer.ui.base.Base_RayTracerWin;
import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import experiments_PKG.rayTracerProbExp.experiment.RayTracerExperiment;

/**
 * @author John Turner
 *
 */
public class RayTracerExpWindow extends Base_RayTracerWin {

	/**
	 * Ray tracer experiments
	 */
	private RayTracerExperiment RTExp;
	
	/**
	 * @param _p
	 * @param _AppMgr
	 * @param _winIdx
	 * @param _flagIdx
	 */
	public RayTracerExpWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
		super(_p, _AppMgr, _winIdx);
		super.initThisWin(false);
	}

	@Override
	protected void initMe_Indiv() {
		//build exps before visible screen with set
		RTExp = new RayTracerExperiment(this, curVisScrDims);
		//get initial UI values and send to experiment	
		updateCalcObjUIVals();
	}

	/**
	 * Retrieve the total number of defined privFlags booleans (application-specific state bools and interactive buttons)
	 */
	@Override
	public int getTotalNumOfPrivBools() {return numPrivFlags;	}
	/**
	 * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param tmpUIObjArray : map of object data, keyed by UI object idx, with array values being :                    
	 *           the first element double array of min/max/mod values                                                   
	 *           the 2nd element is starting value                                                                      
	 *           the 3rd elem is label for object                                                                       
	 *           the 4th element is object type (GUIObj_Type enum)
	 *           the 5th element is boolean array of : (unspecified values default to false)
	 *           	idx 0: value is sent to owning window,  
	 *           	idx 1: value is sent on any modifications (while being modified, not just on release), 
	 *           	idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
	 *           the 6th element is a boolean array of format values :(unspecified values default to false)
	 *           	idx 0: whether multi-line(stacked) or not                                                  
	 *              idx 1: if true, build prefix ornament                                                      
	 *              idx 2: if true and prefix ornament is built, make it the same color as the text fill color.
	 * @param tmpListObjVals : map of string arrays, keyed by UI object idx, with array values being each element in the list
	 * @param firstBtnIDX : first index to place button objects in @tmpBtnNamesArray 
	 * @param tmpBtnNamesArray : map of Object arrays to be built containing all button definitions, keyed by sequential value == objId
	 * 				the first element is true label
	 * 				the second element is false label
	 * 				the third element is integer flag idx 
	 */
	@Override
	protected void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals, int firstBtnIDX, TreeMap<Integer,Object[]> tmpBtnNamesArray) {}

	@Override
	protected Base_RayTracerUIUpdater buildUIDataUpdateObject_Indiv() {
		return new RayTracerExpUIUpdater(this);
	}

	/**
	 * This function is called on ui value update, to pass new ui values on to window-owned consumers
	 */
	@Override
	protected final void updateCalcObjUIVals() {
		RTExp.updateUIDataValues(getUIDataUpdater());
	}
	/**
	 * UI code-level Debug mode functionality. Called only from flags structure
	 * @param val
	 */
	@Override
	protected final void handleDispFlagsDebugMode_Indiv(boolean val) {}
	
	/**
	 * Application-specific Debug mode functionality (application-specific). Called only from privflags structure
	 * @param val
	 */
	@Override
	protected final void handlePrivFlagsDebugMode_Indiv(boolean val) {	}

	@Override
	protected void setPrivFlags_Indiv(int idx, boolean val) {		
	}

	@Override
	protected int[] getFlagIDXsToInitToTrue_Indiv(int[] baseFlags) {
		return null;
	}

	@Override
	protected boolean setUI_IntValsCustom_Indiv(int UIidx, int ival, int oldVal) {return false;}

	@Override
	protected boolean setUI_FloatValsCustom_Indiv(int UIidx, float val, float oldVal) {return false;}

	@Override
	protected void drawMe_Indiv(float animTimeMod) {
		ri.pushMatState();
		ri.translate(winInitVals.rectDim[0],0,0);
		//all drawing stuff goes here
		RTExp.drawExp();
		ri.popMatState();
	}

	@Override
	protected void drawRightSideInfoBarPriv_Indiv(float modAmtMillis) {}

	@Override
	protected void drawCustMenuObjs_Indiv() {}

	@Override
	protected void setVisScreenDimsPriv_Indiv() {}

	@Override
	public void hndlFileLoad_Indiv(File file, String[] vals, int[] stIdx) {
		
	}

	@Override
	protected boolean chkMouseMoveDragState2D(int mouseX, int mouseY, int btn) {return RTExp.checkMouseDragMoveInExp2D( mouseX-(int)winInitVals.rectDim[0], mouseY, btn);}
	@Override
	protected boolean chkMouseClick2D(int mouseX, int mouseY, int btn) {return RTExp.checkMouseClickInExp2D( mouseX-(int)winInitVals.rectDim[0], mouseY, btn);}
	@Override
	protected void setMouseReleaseState2D() {	RTExp.setMouseReleaseInExp2D();}//chkMouseOvr

}//class RayTracerExpWindow
