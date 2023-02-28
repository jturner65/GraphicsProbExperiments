package experiments_PKG.rayTracerProbExp.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_RayTracer.ui.base.Base_RayTracerUIUpdater;
import base_RayTracer.ui.base.Base_RayTracerWin;
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

	@Override
	protected int initAllPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray) {
		return numPrivFlags;
	}

	@Override
	protected void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray,
			TreeMap<Integer, String[]> tmpListObjVals) {}

	@Override
	protected Base_RayTracerUIUpdater buildUIDataUpdateObject_Indiv() {
		return new RayTracerExpUIUpdater(this);
	}

	/**
	 * This function is called on ui value update, to pass new ui values on to window-owned consumers
	 */
	@Override
	protected final void updateCalcObjUIVals() {
		RTExp.updateUIDataValues(uiUpdateData);
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
