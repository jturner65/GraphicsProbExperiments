package base_RayTracer.ui.base;

import java.util.Map;

import base_UI_Objects.windowUI.uiData.UIDataUpdater;


/**
 * Hold UI Update functionality for abstract base-class window for Ray Tracer
 * @author 7strb
 *
 */
public class Base_RayTracerUIUpdater extends UIDataUpdater {

	public Base_RayTracerUIUpdater(Base_RayTracerWin _win) {
		super(_win);
	}

	public Base_RayTracerUIUpdater(UIDataUpdater _otr) {
		super(_otr);
	}

	public Base_RayTracerUIUpdater(Base_RayTracerWin _win, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
			Map<Integer, Boolean> _bVals) {
		super(_win, _iVals, _fVals, _bVals);
	}
	/**
	 * access app-specific ints
	 */
	public int getNumSceneCols() {return intValues.get(Base_RayTracerWin.gIDX_SceneCols);}
	public int getNumSceneRows() {return intValues.get(Base_RayTracerWin.gIDX_SceneRows);}
	public int getCurrSceneCliFileIDX() {return intValues.get(Base_RayTracerWin.gIDX_CurrSceneCLI);}
	
	/**
	 * access app-specific floats
	 */

	/**
	 * access app-specific booleans
	 */		

		
}//class RayTracerUIUpdater
