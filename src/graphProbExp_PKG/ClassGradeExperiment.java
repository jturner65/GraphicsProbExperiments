package graphProbExp_PKG;

import java.util.HashSet;

/**
 * this class is a specialized type fo probability experiment manager, specifically for the class grades project
 * @author john
 *
 */
public class ClassGradeExperiment extends BaseProbExpMgr{
	//structure holding all classes
	public HashSet<myClassRoster> classes;
	public int numClasses;
	
	//structure holding all students
	public HashSet<myStudent> students;
	public int numStudents;
	//types of different transformed grades
	public static final int
		rawGradeIDX			= 0,
		normTransGradeIDX	= 1;
	public static final int numGradeTypes = 2;
	
	
	//experiment-specific state flag idxs - bits in array holding relevant process info
	public static final int
			debugIDX 					= 0;		
	public static final int numFlags = 1;	
	
	public ClassGradeExperiment(myDispWindow _win) {
		super(_win);
	
		initExp();
	}//ctor
	
	public void initClassExp() {
		classes = new HashSet<myClassRoster>();
		numClasses = 0;
		students = new HashSet<myStudent>();
		numStudents = 0;
	}
	
	//called at end of ctor and whenever experiment needs to be re-instanced
	@Override
	public void initExp() {
		
		
	}	
	
	//called by base class call to buildSolvers, during base class ctor
	@Override
	protected void buildSolvers_indiv() {
		// TODO Auto-generated method stub
		
	}
	
	//public void initEx
	
	/////////////////////////////	
	//init and manage state flags
	@Override
	protected void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	@Override
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	@Override
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX : {break;}	
			
		}
	}//setFlag		
	@Override
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}

}//class ClassGradeExperiment

