package graphProbExp_PKG;

import base_UI_Objects.*;
import base_UI_Objects.windowUI.BaseBarMenu;

//instance of sidebar menu, holding descriptions of application-specifc sidebar buttons
public class mySideBarMenu extends BaseBarMenu {

	public mySideBarMenu(my_procApplet _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt);
		
	}//mySideBarMenu
	
	//set up the side bar menu buttons for this instance of the UI
	@Override
	protected void initSideBarMenuBtns_Priv() {
		System.out.println("Init me priv sideBarMenu");	
		/**
		 * set row names for each row of ui action buttons getMouseOverSelBtnNames()
		 * @param _funcRowNames array of names for each row of functional buttons 
		 * @param _numBtnsPerFuncRow array of # of buttons per row of functional buttons
		 * @param _numDbgBtns # of debug buttons
		 * @param _inclWinNames include the names of all the instanced windows
		 * @param _inclMseOvValues include a row for possible mouse over values
		 */
		//protected void setBtnData(String[] _funcRowNames, int[] _numBtnsPerFuncRow, int _numDbgBtns, boolean _inclWinNames, boolean _inclMseOvValues) {

		setBtnData(new String[]{"Special Functions 1","Special Functions 2"}, new int[] {3,5}, 5, true, true);
		

		
	}//_initMe_Priv



}//class mySideBarMenu
