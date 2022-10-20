package base_ProbTools.randVisTools.visObj;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_ProbTools.randVisTools.myDistFuncHistVisMgr;
import base_ProbTools.randVisTools.visObj.base.baseDistVisObj;

//histogram evaluation of a pdf - draws buckets
public class myHistVisObj extends baseDistVisObj{
	
	public myHistVisObj(myDistFuncHistVisMgr _owner, int[][] _clrs) {
		super(_owner, _clrs);
	}
	
	protected void _drawCurve(IRenderInterface pa, float offset) {
		for (int idx = 0; idx <dispVals.length-1;++idx) {	
			pa.drawRect(dispVals[idx][0], 0, (dispVals[idx+1][0]-dispVals[idx][0]), dispVals[idx][1]);			
		}		
		drawAxes(pa, offset);
	}//_drawCurve

	
}//myHistVisObj