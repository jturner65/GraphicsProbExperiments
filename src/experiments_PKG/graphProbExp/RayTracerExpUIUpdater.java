package experiments_PKG.graphProbExp;

import java.util.Map;

import base_RayTracer.ui.base.Base_RayTracerUIUpdater;
import base_RayTracer.ui.base.Base_RayTracerWin;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;

public class RayTracerExpUIUpdater extends Base_RayTracerUIUpdater {

	public RayTracerExpUIUpdater(Base_RayTracerWin _win) {
		super(_win);
	}

	public RayTracerExpUIUpdater(UIDataUpdater _otr) {
		super(_otr);
	}

	public RayTracerExpUIUpdater(Base_RayTracerWin _win, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
			Map<Integer, Boolean> _bVals) {
		super(_win, _iVals, _fVals, _bVals);
	}

}//class RayTracerExpUIUpdater
