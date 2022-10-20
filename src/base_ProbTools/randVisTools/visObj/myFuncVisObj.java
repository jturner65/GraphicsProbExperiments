package base_ProbTools.randVisTools.visObj;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_ProbTools.randVisTools.myDistFuncHistVisMgr;
import base_ProbTools.randVisTools.visObj.base.baseDistVisObj;

 
/**
 * visualize a functional object evaluation - draws a line
 * @author 7strb
 *
 */
public class myFuncVisObj extends baseDistVisObj{
	public myFuncVisObj(myDistFuncHistVisMgr _owner, int[][] _clrs) {
		super(_owner, _clrs);
	}
	
	@Override
	protected void _drawCurve(IRenderInterface pa, float offset) {
		pa.drawEllipse2D(dispVals[0][0], dispVals[0][1], 5.0f,5.0f);
		for (int idx = 1; idx <dispVals.length;++idx) {	
			//draw point 			
			pa.drawEllipse2D(dispVals[idx][0], dispVals[idx][1], 5.0f,5.0f);
			//draw line between points
			pa.drawLine(dispVals[idx-1][0], dispVals[idx-1][1], 0, dispVals[idx][0], dispVals[idx][1], 0);
		}		
		drawAxes(pa, 0);
	}//_drawCurve

}//myFuncVisObj