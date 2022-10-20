package base_ProbTools.randGenFunc;

import base_ProbTools.randGenFunc.gens.base.myRandGen;

/**
 * this class holds a description of a random number generator, including the momments and algorithms it uses
 * @author john
 */
public class RandGenDesc implements Comparable<RandGenDesc>{
	//owning rand gen
	public final myRandGen randGen;
	//names of quadrature algorithm and random number distribution name and algorithm name 
	public final String quadName, distName, algName;
	
	public RandGenDesc(String _quadName, String _distName, myRandGen _randGen) {
		quadName = _quadName; 
		randGen = _randGen;		
		distName = _distName;
		algName = randGen.name;
	}//ctor

	@Override
	public int compareTo(RandGenDesc othr) {
		int res = distName.toLowerCase().compareTo(othr.distName.toLowerCase());
		res = (res == 0 ? quadName.toLowerCase().compareTo(othr.quadName.toLowerCase()) : res);
		res = (res == 0 ? algName.toLowerCase().compareTo(othr.algName.toLowerCase()) : res);
		return (res == 0 ? Integer.compare(randGen.ObjID, othr.randGen.ObjID) : res);
	}//compareTo	
	
	@Override
	public String toString() {
		String res = "Alg Name : " + algName + " | Dist : " + distName + " | Quad Alg : " + quadName;
		return res;
	}
}//class RandGenDesc
