package graphProbExp_PKG;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * base class to instance a hashing object that works on objects of type T
 * @author john
 *
 */
public abstract class hashFuncBase<T> {
	/**
	 * universe size
	 */
	public static final int N = Integer.MAX_VALUE;
	
	public String name;
	/**
	 * # of hashes used by bloom filter
	 */
	protected int k;
	
	/**
	 * random # generator for hashing functions that require it
	 */
	private Random randGen;
	
	/**
	 * table size of owning bloom filter - used to determine pool to draw random values from
	 */
	protected int tableSize;
	/**
	 * initial seed to random generator to build hashes
	 */
	protected int initTimeOffset;
	
	public hashFuncBase(String _name, int _k, int _tOff, int _tableSize){
		name = _name;
		k=_k; 
		tableSize = _tableSize;
		//use _tOff as seed to randGen before generating hash coefficients		
		initTimeOffset = _tOff;
		randGen = new Random(_tOff);
		//initialize hash functions based on type of function
		initHashes();
	}
	
	/**
	 * this function will return a random integer from [0 .. tableSize-1]
	 */
	protected int getRandomValue(){
		return randGen.nextInt(tableSize);
	}
	/**
	 * this function will return a random integer from [0 .. _n-1]
	 * @param _n ceiling value for random generator result
	 */
	protected int getRandomValue(int _n){
		return randGen.nextInt(_n);
	}
	/**
	 * sets random generator's seed to _seed
	 * @param _seed seed to use for randGen
	 */
	protected void setRandSeed(int _seed){
		randGen.setSeed(_seed);
	}
	
	/**
	 * this function initializes any values related to the # of hashes this hash function is generating
	 */
	protected abstract void initHashes();

	/**
	 * get the i'th hash of obj
	 * @param obj : object of type T to be hashed
	 * @param i : number of the hash to be derived
	 * @return : i'th hash of obj
	 */
	protected abstract int getIthHashOfData(T obj, int i);
	
	/**
	 * builds an array of k hashes of the passed object
	 * @param obj : object of type T to be hashed
	 * @param k : # of hashes desired
	 * @return : array of size k of integer hashes of obj
	 */
	public int[] getKHashesOfData(T obj){
		int[] hashes = new int[k];
		for(int i=0;i<k;++i){			hashes[i] = getIthHashOfData(obj, i);		}
		return hashes;
	}
	
	/**
	 * builds an array of k hashes of the passed object for testing
	 * @param obj : object of type T to be hashed
	 * @param k : # of hashes desired
	 * @return : array of size k of integer hashes of obj
	 */
	public ArrayList<Integer> getKHashesOfData_HashTest(T obj){
		ArrayList<Integer> hashes = new ArrayList<Integer>(k);
		for(int i=0;i<k;++i){		hashes.add(i,getIthHashOfData(obj, i));	}
		return hashes;
	}
	
	/**
	 * get arraylist of strings of the values in this hash function
	 * @return
	 */	
	public final ArrayList<String> getHashDesc() {
		ArrayList<String> res = new ArrayList<String>(), res1;
		res.add("#Hash Function name/type");
		res.add("name="+name);
		res.add("#size of hash table (must be prime and should be n << N)");
		res.add("n="+tableSize);
		res.add("#number of hashes to use in bloom filter");
		res.add("k="+k);
		res1 = getCustHashDesc();
		res.addAll(res1);
		return res;
	}//getHashDesc
	
	/**
	 * get arraylist of strings of the values in this hash function
	 * @return
	 */
	protected abstract ArrayList<String> getCustHashDesc();

	
}//abstract class hashFuncBase<T>

//for each hash pick seed s_i and use rnd gen r(x), h_i(x) = r(x + s_i)
class hashType1_test extends hashFuncBase<Integer> {
	protected int[] hseeds;
	
	public hashType1_test(String _name, int _k, int _tOff, int _tableSize) {
		super(_name, _k, _tOff,  _tableSize);
	}
	/**
	 * this function initializes the seed values for each hash function as per Type 1 hash functions in assignment
	 * Type 1 Hashes implement a per-function seed value, this function precalculates that seed value
	 */
	@Override
	protected void initHashes() {
		hseeds = new int[k];
		for(int i=0;i<k;++i){
			hseeds[i] = getRandomValue();
		}		
	}	
	
	/**
	 * Type 1 Hash functions (in assignment) : this should return a value from randGen once it has been seeded by x + s_i
	 */
	@Override
	protected int getIthHashOfData(Integer x, int i) {
		setRandSeed(x + hseeds[i]);
		return getRandomValue();
	}
	
	@Override
	protected ArrayList<String> getCustHashDesc() {
		ArrayList<String>res = new ArrayList<String>();
		res.add("#Seed used to generate hash function seeds");
		res.add("genSeed="+initTimeOffset);
		res.add("#list of "+hseeds.length+" seeds");
		for(int i=0;i<hseeds.length;++i){
			res.add("seed_"+i+"="+hseeds[i]);
		}		
		return res;
	}//getCustHashDesc()
	

}//class hashType1

//for each desired hash function, find two hashes, a and b, to then find the specific hash h_i(x) = a_i * x + b_i
//tableSize must be prime for this hash function
class hashType2_test extends hashFuncBase<Integer> {
	public int[] a;
	public int[] b;

	public hashType2_test(String _name, int _k, int _tOff, int _tableSize) {
		super(_name, _k, _tOff,  _tableSize);
	}
	/**
	 * this function initializes the two random hash values a and b for each hash function
	 * Type 2 Hash functions implement a per-function pair of random values
	 */
	@Override
	protected void initHashes() {
		a = new int[k];
		b = new int[k];
		for(int i=0;i<k;++i){
			a[i] = 1+getRandomValue(tableSize-1);	
			b[i] = getRandomValue();
		}	
	}	
	/**
	 * Type 2 Hash functions (linear congruent generator) : This should return a_i * x +b_i % tableSize
	 */
	@Override
	protected int getIthHashOfData(Integer x, int i) {
		int tmp = (int) Math.floorMod(((long)(a[i]) * x + b[i]),N);//N must be prime, or else must use next prime bigger than N
		return  tmp % tableSize;
	}
	@Override
	protected ArrayList<String> getCustHashDesc() {
		ArrayList<String> res = new ArrayList<String>();
		res.add("#Seed used to generate hash function seeds");
		res.add("genSeed="+initTimeOffset);
		res.add("#list of "+a.length+" a() seeds");
		for(int i=0;i<a.length;++i){
			res.add("a_"+i+"="+a[i]);
		}		
		res.add("#list of "+b.length+" b() seeds");
		for(int i=0;i<b.length;++i){
			res.add("b_"+i+"="+b[i]);
		}		
		return res;
	}//getCustHashDesc

}//class hashType2

/**
 * use murmur and support strings and integers
 * @author john turner
 *
 * @param <T>
 */
abstract class hashType_murmur<T> extends hashFuncBase<T> {
	/**
	 * time of start of program, represented in type T (either int or string)
	 */
	protected T MMseed;

	public hashType_murmur(String _name, int _k, int _tOff, int _tableSize) {
		super(_name, _k, _tOff,  _tableSize);
	}
	
	//based on type T, seed value is set for murmur
	public void setMMSeed(T _seed){
		MMseed = _seed;
	}
	
	/**
	 * Murmur Hash functions do not require any specific initialization
	 */
	@Override
	protected void initHashes() {}	
	
	/**
	 * instead uses murmur hash to provide long hash of obj
	 */
	@Override
	protected int getIthHashOfData(T obj, int i) { return -1;	}
	/**
	 * returns the long value of the murmur hash of the type T val
	 * @param val : type T data
	 * @param seed : type T time of program start offset
	 * @return Long hash of seed + val + seed
	 */
	protected abstract long getObjHash(T val, T seed);
	
	private final long _m = 0xc6a4a7935bd1e995L, _s=0xe17a1465L;
//	//64 bit murmurhash implementation
	protected long getMurmurHash(byte[] data) {
        
        int r = 31;
        long h = _s ^(data.length*_m);

        int len8 = data.length/8,
        	lenN7 = data.length&~7; //gives start of len8 * 8 values - only works with pwr2 values
        int i8 = 0;
        long k;
        for (int i=0; i<len8; ++i) {
            i8 = i*8;
            k = ((long)data[i8++]&0xff)+
        		(((long)data[i8++]&0xff)<<8)+
        		(((long)data[i8++]&0xff)<<16)+
        		(((long)data[i8++]&0xff)<<24)+
        		(((long)data[i8++]&0xff)<<32)+
        		(((long)data[i8++]&0xff)<<40)+
        		(((long)data[i8++]&0xff)<<48)+
        		(((long)data[i8++]&0xff)<<56);
            k *= _m; k ^= k >>> r;  k *= _m;  h ^= k; h *= _m; 
        }    
        //switch cascades based on leftover length not covered in above loop, processes every entry <= data.length%8, skipped if none
        switch (data.length%8) {//cascades
	        case 7: h ^= (long)(data[lenN7+6]&0xff) << 48;
	        case 6: h ^= (long)(data[lenN7+5]&0xff) << 40;
	        case 5: h ^= (long)(data[lenN7+4]&0xff) << 32;
	        case 4: h ^= (long)(data[lenN7+3]&0xff) << 24;
	        case 3: h ^= (long)(data[lenN7+2]&0xff) << 16;
	        case 2: h ^= (long)(data[lenN7+1]&0xff) << 8;
	        case 1: h ^= (long)(data[lenN7]&0xff);
	                h *= _m;
        };     
        h ^= h >>> r;       h *= _m;       h ^= h >>> r;//unsigned shift >>>
        return h;
    }//	getMurmurHash of byte array of data
	
	
    protected int[] getHashesForObj(T obj, T seed){
		long hash = getObjHash(obj,seed);
		return new int[]{(int) (hash >> 32),  (int)hash};   	
    }
		
	//get the nth hash through linear combinations of two predetermined hashes
	private int nthHash(long n, long hashA, long hashB) {	return ((int)((hashA + n * hashB) & 0x000000007fffffffL) % tableSize);}
		
	/**
	 * builds an array of k hashes of the passed object - overrides base class function
	 * @param obj : object of type T to be hashed
	 * @param k : # of hashes desired
	 * @return : array of size k of integer hashes of obj
	 */
	@Override
	public int[] getKHashesOfData(T obj){
		int[] hashes = new int[k];
		int[] tmp = getHashesForObj(obj, MMseed);
		for(int i=0;i<k;++i){
			hashes[i] = nthHash(i, tmp[0], tmp[1]);
		}
		return hashes;
	}//getKHashesOfData
	
	/**
	 * builds an array of k hashes of the passed object
	 * @param obj : object of type T to be hashed
	 * @param k : # of hashes desired
	 * @return : array of size k of integer hashes of obj
	 */
	@Override
	public ArrayList<Integer> getKHashesOfData_HashTest(T obj){
		ArrayList<Integer> hashes = new ArrayList<Integer>(k);
		int[] tmp = getHashesForObj(obj, MMseed);
		for(int i=0;i<k;++i){
			hashes.add(i,nthHash(i, tmp[0], tmp[1]));//((int)(((long)(tmp[0] + i * tmp[1])& 0x000000007fffffffL))) % tableSize);
		}
		return hashes;
	}
	
	@Override
	protected ArrayList<String> getCustHashDesc() {
		ArrayList<String> res = new ArrayList<String>();
		res.add("#Murmur calculation seed");
		res.add("mmseed="+MMseed.toString());
		return res;
	}
	
}//class hashType_murmur<T>

class strMurmurHashFunc extends hashType_murmur<String>{

	public strMurmurHashFunc(String _name, int _k, int _tOff, String _tOffStr, int _tableSize) {
		super(_name, _k, _tOff,  _tableSize);
		setMMSeed(_tOffStr);		
	}
	@Override
	protected long getObjHash(String val, String seed) {
    	String text = seed+val+seed;
    	return getMurmurHash(text.getBytes());	
    }
	
}//strMurmurHashFunc

class intMurmurHashFunc extends hashType_murmur<Integer>{
	/**
	 * so as to not repeatedly re-create buffer
	 */
	protected static ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);    

	public intMurmurHashFunc(String _name, int _k, int _tOff, int _tableSize) {
		super(_name, _k, _tOff,  _tableSize);
		setMMSeed(_tOff);
	}

	@Override
	protected long getObjHash(Integer val, Integer seed) {
    	buffer.putInt(0, val+seed);
    	return getMurmurHash(buffer.array());	
    }

}//intMurmurHashFunc


//class randomGen {
//	/*
//	   This Random Number Generator is based on the algorithm in a FORTRAN
//	   version published by George Marsaglia and Arif Zaman, Florida State
//	   University; ref.: see original comments below.
//	   At the fhw (Fachhochschule Wiesbaden, W.Germany), Dept. of Computer
//	   Science, we have written sources in further languages (C, Modula-2
//	   Turbo-Pascal(3.0, 5.0), Basic and Ada) to get exactly the same test
//	   results compared with the original FORTRAN version.
//	   April 1989
//	   Karl-L. Noell <NOELL@DWIFH1.BITNET>
//	      and  Helmut  Weber <WEBER@DWIFH1.BITNET>
//
//	   This random number generator originally appeared in "Toward a Universal
//	   Random Number Generator" by George Marsaglia and Arif Zaman.
//	   Florida State University Report: FSU-SCRI-87-50 (1987)
//	   It was later modified by F. James and published in "A Review of Pseudo-
//	   random Number Generators"
//	   THIS IS THE BEST KNOWN RANDOM NUMBER GENERATOR AVAILABLE.
//	   (However, a newly discovered technique can yield
//	   a period of 10^600. But that is still in the development stage.)
//	   It passes ALL of the tests for random number generators and has a period
//	   of 2^144, is completely portable (gives bit identical results on all
//	   machines with at least 24-bit mantissas in the floating point
//	   representation).
//	   The algorithm is a combination of a Fibonacci sequence (with lags of 97
//	   and 33, and operation "subtraction plus one, modulo one") and an
//	   "arithmetic sequence" (using subtraction).
//
//	   Use IJ = 1802 & KL = 9373 to test the random number generator. The
//	   subroutine RANMAR should be used to generate 20000 random numbers.
//	   Then display the next six random numbers generated multiplied by 4096*4096
//	   If the random number generator is working properly, the random numbers
//	   should be:
//	           6533892.0  14220222.0  7275067.0
//	           6172232.0  8354498.0   10633180.0
//	*/
//
//	/* Globals */
//	private double[] u = new double[97];
//	private double c,cd,cm;
//	private int i97,j97;
//	private boolean test;
//
//	/*
//	   This is the initialization routine for the random number generator.
//	   NOTE: The seed variables can have values between:    0 <= IJ <= 31328
//	                                                        0 <= KL <= 30081
//	   The random number sequences created by these two seeds are of sufficient
//	   length to complete an entire calculation with. For example, if sveral
//	   different groups are working on different parts of the same calculation,
//	   each group could be assigned its own IJ seed. This would leave each group
//	   with 30000 choices for the second seed. That is to say, this random
//	   number generator can create 900 million different subsequences -- with
//	   each subsequence having a length of approximately 10^30.
//	*/
//	void RandomInitialise(int ij,int kl) {
//	   double s,t;
//	   int ii,i,j,k,l,jj,m;
//
//	   /*
//	      Handle the seed range errors
//	         First random number seed must be between 0 and 31328
//	         Second seed must have a value between 0 and 30081
//	   */
//	   if (ij < 0 || ij > 31328 || kl < 0 || kl > 30081) {
//			ij = 1802;
//			kl = 9373;
//	   }
//
//	   i = (ij / 177) % 177 + 2;
//	   j = (ij % 177)       + 2;
//	   k = (kl / 169) % 178 + 1;
//	   l = (kl % 169);
//
//	   for (ii=0; ii<97; ii++) {
//	      s = 0.0;
//	      t = 0.5;
//	      for (jj=0; jj<24; jj++) {
//	         m = (((i * j) % 179) * k) % 179;
//	         i = j;
//	         j = k;
//	         k = m;
//	         l = (53 * l + 1) % 169;
//	         if (((l * m % 64)) >= 32)
//	            s += t;
//	         t *= 0.5;
//	      }
//	      u[ii] = s;
//	   }
//
//	   c    = 362436.0 / 16777216.0;
//	   cd   = 7654321.0 / 16777216.0;
//	   cm   = 16777213.0 / 16777216.0;
//	   i97  = 97;
//	   j97  = 33;
//	   test = true;
//	}
//
//	/* 
//	   This is the random number generator proposed by George Marsaglia in
//	   Florida State University Report: FSU-SCRI-87-50
//	*/
//	double RandomUniform(){
//	   double uni;
//
//	   /* Make sure the initialisation routine has been called */
//	   if (!test) {  	RandomInitialise(1802,9373);}
//
//	   uni = u[i97-1] - u[j97-1];
//	   if (uni <= 0.0) { ++uni;}
//	   u[i97-1] = uni;
//	   --i97;
//	   if (i97 == 0) { i97 = 97;}
//	   --j97;
//	   if (j97 == 0) { j97 = 97;}
//	   c -= cd;
//	   if (c < 0.0) {   c += cm;}
//	   uni -= c;
//	   if (uni < 0.0) { ++uni;}
//	   return uni;
//	}
//
//	/*
//	  ALGORITHM 712, COLLECTED ALGORITHMS FROM ACM.
//	  THIS WORK PUBLISHED IN TRANSACTIONS ON MATHEMATICAL SOFTWARE,
//	  VOL. 18, NO. 4, DECEMBER, 1992, PP. 434-435.
//	  The function returns a normally distributed pseudo-random number
//	  with a given mean and standard devaiation.  Calls are made to a
//	  function subprogram which must return independent random
//	  numbers uniform in the interval (0,1).
//	  The algorithm uses the ratio of uniforms method of A.J. Kinderman
//	  and J.F. Monahan augmented with quadratic bounding curves.
//	*/
//	double RandomGaussian(double mean, double stddev){
//	   double  q,u,v,x,y;
//		/*  
//			Generate P = (u,v) uniform in rect. enclosing acceptance region 
//	      Make sure that any random numbers <= 0 are rejected, since
//	      gaussian() requires uniforms > 0, but RandomUniform() delivers >= 0.
//		*/
//	   do {
//		   u = RandomUniform();
//		   v = RandomUniform();
//	   		if (u <= 0.0 || v <= 0.0) {
//	   			u = 1.0;
//	   			v = 1.0;
//	   		}
//	   		v = 1.7156 * (v - 0.5);
//
//	   		//Evaluate the quadratic form */
//	   		x = u - 0.449871;
//	   		y = Math.abs(v) + 0.386595;
//	   		q = x * x + y * (0.19600 * y - 0.25472 * x);
//
//	   		/* Accept P if inside inner ellipse */
//	   		if (q < 0.27597) {break;}
//
//	      /*  Reject P if outside outer ellipse, or outside acceptance region */
//	    } while ((q > 0.27846) || (v * v > -4.0 * Math.log(u) * u * u));
//
//	    /*  Return ratio of P's coordinates as the normal deviate */
//	    return (mean + stddev * v / u);
//	}
//
//	//return random integer within a range, lower -> upper INCLUSIVE
//	public int RandomInt(int lower,int upper){  return((int)(RandomUniform() * (upper - lower + 1)) + lower);}
//
//	//return random float within a range, lower -> upper
//	public double RandomDouble(double lower,double upper){  return((upper - lower) * RandomUniform() + lower);}
//
//}