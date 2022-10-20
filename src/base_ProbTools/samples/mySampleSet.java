package base_ProbTools.samples;

import java.util.*;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_ProbTools.randGenFunc.gens.myBoundedRandGen;
import base_ProbTools.randGenFunc.gens.base.myRandGen;
import base_Utils_Objects.io.messaging.MessageObject;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * a sample of multiple observations from a distribution
 * @author john
 *
 */
public abstract class mySampleSet implements Comparable<mySampleSet> {
	public static IRenderInterface pa;
	//experiment owning/using this sample set
	public static MessageObject msgObj;
	public final int ObjID;
	private static int IDCnt = 0;
	//sample set name
	public final String name;
	//rand gen used to model underlying grade distribution for this class
	protected HashMap<String, myRandGen> baseDistModels;
	//currently used dist model
	protected String curDistModel;

	public mySampleSet(IRenderInterface _pa, String _name) {
		pa =_pa;
		msgObj = MessageObject.buildMe();
		ObjID = IDCnt++;  name=_name;	
		curDistModel = "";
		baseDistModels = new HashMap<String, myRandGen>();
	}//ctor
	
	//when new transform added, need to clear out existing transformed grades
	public void setBaseDistModel(myRandGen _randGen) {
		curDistModel = _randGen.name;
		baseDistModels.put(curDistModel, _randGen);
		setBaseDistModel_Indiv();
	}//setRandGenAndType
	
	public void setCurDistModel(String desMdlName) {
		if(null==baseDistModels.get(desMdlName)) {
			msgObj.dispMessage("myClassRoster", "setCurDistModel", "Desired base dist model : " + desMdlName+" has not been set/is null.  Aborting",MsgCodes.warning1, true);	return;
		}
		curDistModel = desMdlName;
	}
	public String getCurDistModel() {return curDistModel;}
	//instance class specific functionality for setting base distribution model
	protected abstract void setBaseDistModel_Indiv();
	
	////////////////////////////////////////
	// underlying distribution config, evaluation and plotting functions
	public void setRVFOptionFlags(int[][] _opts) {
		myRandGen baseDistModel = baseDistModels.get(curDistModel);
		if(baseDistModel != null) {baseDistModel.setOptionFlags(_opts);}}
	
	//this will evaluate the cosine and the gaussian functions against a histogram, showing the performance of these functions when built from a histogram data
	public void evalCosAndNormWithHist(int numVals, int numBuckets, double low, double high) {
		//we wish to build a histogram of current gaussian distribution, then we wish to superimpose the gaussian pdf curve over the histogram, and then superimpose the cosine pdf curve
		myRandGen baseDistModel = baseDistModels.get(curDistModel);
		myBoundedRandGen cosGen = (myBoundedRandGen) baseDistModels.get("Bounded PDF Algorithm");
		baseDistModel.buildFuncHistCosPlot(numVals, numBuckets, low, high, cosGen);
		
	}//evalCosAndNormWithHist
	
	
	public void evalAndPlotFuncRes(int numVals, double low, double high, int funcType) {
		myRandGen baseDistModel = baseDistModels.get(curDistModel);
		if(baseDistModel == null) {			msgObj.dispMessage("myClassRoster", "evalAndPlotFuncRes", "curDistModel has not been set/is null.  Aborting",MsgCodes.warning1, true);	return;	}
		baseDistModel.calcFuncValsForDisp(numVals, low, high, funcType);		
	}
	public void evalAndPlotHistRes(int numVals, int numBuckets) {
		myRandGen baseDistModel = baseDistModels.get(curDistModel);
		if(baseDistModel == null) {			msgObj.dispMessage("myClassRoster", "evalAndPlotHistRes", "curDistModel has not been set/is null.  Aborting",MsgCodes.warning1, true);	return;	}		
		baseDistModel.calcHistValsForDisp(numVals, numBuckets);
	}
	
	public void clearPlotEval() {baseDistModels.get(curDistModel).clearPlotEval();	}	
	//draw plot results from functional histogram/evaluation of baseDistModel
	public void drawPlotRes() {baseDistModels.get(curDistModel).drawDist(pa);}	
	
	//incase we wish to store class sample sets in sorted mechanism
	@Override
	public int compareTo(mySampleSet othr) {
		int res = this.name.toLowerCase().compareTo(othr.name.toLowerCase());
		return (res == 0 ? Integer.compare(ObjID, othr.ObjID) : res);
	}//compareTo
	
	
	
}//class mySampleSet


