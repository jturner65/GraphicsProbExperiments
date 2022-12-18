package experiments_PKG.rayTracerProbExp;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_RayTracer.ui.base.Base_RayTracerUIUpdater;
import base_RayTracer.ui.base.Base_RayTracerWin;
import base_UI_Objects.GUI_AppManager;

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
	public RayTracerExpWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx, int _flagIdx) {
		super(_p, _AppMgr, _winIdx, _flagIdx);
		super.initThisWin(false);
	}

	@Override
	protected void initMe_Indiv() {
		//build exps before visible screen with set
		RTExp = new RayTracerExperiment(pa, this, curVisScrDims);
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
		pa.pushMatState();
		pa.translate(this.rectDim[0],0,0);
		//all drawing stuff goes here
		RTExp.drawExp();
		pa.popMatState();
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
	protected boolean chkMouseMoveDragState2D(int mouseX, int mouseY, int btn) {return RTExp.checkMouseDragMoveInExp2D( mouseX-(int)this.rectDim[0], mouseY, btn);}
	@Override
	protected boolean chkMouseClick2D(int mouseX, int mouseY, int btn) {return RTExp.checkMouseClickInExp2D( mouseX-(int)this.rectDim[0], mouseY, btn);}
	@Override
	protected void setMouseReleaseState2D() {	RTExp.setMouseReleaseInExp2D();}//chkMouseOvr

}//class RayTracerExpWindow
