package base_ProbTools.randGenFunc.transform;

import java.util.TreeMap;

import base_ProbTools.myProbSummary;
import base_ProbTools.randGenFunc.RandGenDesc;
import base_ProbTools.randGenFunc.gens.myRandGen;

//////////////////////////////////
//linear and uniform transformation classes
//	these are just mappers and will not be used to synthesize random values

public abstract class transform extends myRandGen{
	//func will be null for these, so all functionality that is dependent on func variable needs to be overridden
	public transform(String _name, myProbSummary _summary) {
		super(null, _name);
		setFuncSummary(_summary);
	}
	//overrding base class verison to remove refs to func
	@Override
	protected void initRandGen() {
		setFlag(funcSetIDX, func!=null);
		funcName=  "No Rand Func for Transform " + name; 
		desc = new RandGenDesc("No Quad Solver", "No Rand Func", this);
		distVisObj = null;
	}//initRandGen
	
	//override base class version to remove ref to func, which will be null
	@Override
	public void setFuncSummary(myProbSummary _summary) {
		summary = _summary;	
		 _setFuncSummaryIndiv();
	}//setFuncSummary
	
	//for a transform this is meaningless - transforms just remap given data to affine transformations, they don't model them
	public void calcDistValsForDisp(int numVals, int numBuckets) {}
	public void calcFuncValsForDisp(int numVals, double low, double high, int funcType ) {}
	
	//return string description of rand function
	@Override
	public String getFuncDataStr() {return "No Function for Transform RandGen - only has mapping";}

	@Override
	public String getTransformName() {		return name+"_"+ _getTransformNameIndiv();	}
	
	public abstract String _getTransformNameIndiv();


}//class transform



