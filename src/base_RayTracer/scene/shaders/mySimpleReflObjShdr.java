package base_RayTracer.scene.shaders;

import base_RayTracer.myColor;
import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

//simplified transparent shader, for project 1
public class mySimpleReflObjShdr extends myObjShader{

	public mySimpleReflObjShdr(myScene _scn) {
		super(_scn);
	}

	//"simplified" transparent color from assignment 1 TODO verify this is appropriate
	protected double[] calcSimpleTransClr(rayHit hit){
		double r=0,g=0,b=0;
		myVector objRayN = hit.objNorm,refrEyeDir = new myVector(hit.fwdTransRayDir); 
		myPoint hitLoc = hit.fwdTransHitLoc;
		refrEyeDir._mult(-1);
		myVector reflDir = compReflDir(refrEyeDir, objRayN);	  

		//reflection happens, calculate results
		//direction vector of refraction
		myVector refractDir = new myVector(0,0,0);
		//incoming angle in radians, critical angle,ray angle upon exiting material
		//n is ratio of refraction indicies for current material/new material (n1/n2),
		//use n1 and n2 to denote material refraction indicies - n1 is source material, n2 is destination refraction material
		double thetaIncident = 0, thetaCrit = 0, //thetaExit = 0, 
				n = 1, n1 = 0, n2 = 0;
		//constant multipliers for reflection perp and parallel - average of rperp and rpar = transreflratio/
		//ratio of resulting transmission vs reflection at surface point - 0 means complete refraction, 1 means complete reflection
		//eventually want to implement a method to handle exiting one material to another with a non-unity index of refraction (this is exitMaterialTrans)
		//1 if refrDotProd is positive, -1 if refrDotProd is negative
		double rPerp = 0, rPar = 0, transReflRatio = 0, oneMTransReflRatio = 1, exitMaterialTrans = 1, refractNormMult = 1.0;
		//if TIR then true
		boolean TIR = false;
		//dot product gives cos of angle of incidence 
		//cross gives sine of angle - need both to verify normal direction
		//myVector V = new myVector(refrEyeDir);
		myVector N = new myVector(objRayN);		//need to copy normal incase direction flips
		double cosTheta1 = refrEyeDir._dot(N), cosTheta2 = 0;//calculated below
		//thetaIncident = scene.pa._angleBetween(refrEyeDir, N);
		thetaIncident = refrEyeDir.angleWithMe(N);
		//the only way the ray doting the normal would be less than 0 is if the incident ray was coming from behind the normal (pointing in the same direction
		//then the "eye"dir would form an angle greater than 90 degrees.  the only way this can happen is from inside the object
		//this means the normal is pointing the same direction as the refrsurfdir (i.e. we are leaving a transparent object)
		//we need to reverse the direction of the normal in this case
		if (cosTheta1 < MyMathUtils.EPS){
			//flip direction of normal used for detecting reflection if theta incident greater than 90 degrees
			//use this to reverse the direction of the final refracted vector
			refractNormMult = -1.0; 
			N._mult(-1);
		}
		//recalculate in case N switched directions
		cosTheta1 = refrEyeDir._dot(N);
		//thetaIncident = scene.pa._angleBetween(refrEyeDir, N);
		thetaIncident = refrEyeDir.angleWithMe(N);
		reflDir = compReflDir(refrEyeDir, N);
		
		if (refractNormMult < 0){//exit
			//means ray is in direction of normal, so we had to flip normal for calculations - leaving object, entering air
			thetaCrit = Math.asin(exitMaterialTrans/currPerm);
			if (thetaIncident < thetaCrit){//determine refracting exit angle
				n1 = currPerm;
				n2 = exitMaterialTrans;
				n = (n1/n2); 
				//thetaExit = Math.asin(n * Math.sin(thetaIncident));
				double tmpResultC2 = 1.0 - (n*n) * (1.0 - (cosTheta1*cosTheta1));
				if (tmpResultC2 < 0){System.out.println("\tdanger #1 : refraction bad : " +  tmpResultC2);}
				cosTheta2 = Math.pow(tmpResultC2,.5);
			} else {//total internal reflection
				transReflRatio = 1;
				oneMTransReflRatio = 1 - transReflRatio;
				TIR = true;
				cosTheta2 = 0;
			}          
		} else {//entering this new material - determine refraction angle
			n1 = hit.transRay.currKTrans[1];//"perm" ->using as idx of refraction here
			n2 = currPerm;
			//println("  entering material : " + n2);
			n = (n1/n2);
			//thetaExit = Math.asin(n  * Math.sin(thetaIncident)); 
			double tmpResultC2 = 1.0 - (n*n) * (1.0 - (cosTheta1*cosTheta1));
			if (tmpResultC2 < 0){System.out.println("\tdanger #2 :  refraction bad : " +  tmpResultC2 + " ray cur idx : " + hit.transRay.currKTrans[1]);}
			cosTheta2 = Math.pow(tmpResultC2,.5);
		} 
    
		//if not tir, calculate transreflratio to determine how much is transmitted, how much is reflected ala fresnel
		if (!TIR){					
			double sinAcos = (Math.sin(Math.acos(cosTheta1))),  resCosThetT = Math.pow(1.0 - ((n1/n2) * sinAcos * sinAcos),.5);					
			rPerp = calcFresPerp(n1, n2, cosTheta1,resCosThetT);
			rPar = calcFresPlel(n1, n2, cosTheta1,resCosThetT);
			transReflRatio = (rPerp + rPar)/2.0;    
			oneMTransReflRatio = 1 - transReflRatio;
			if (oneMTransReflRatio < MyMathUtils.EPS) { System.out.println("tr = 1");}      
		}        
		//sanity check
		if (transReflRatio > 1){ System.out.println("impossible result - treflRat, rPerp, rPar : " + transReflRatio + " | " + rPerp + " | " + rPar);}
    
		if (oneMTransReflRatio > 0){//if transReflRatio == 1 then no refraction
			//incident ray is in direction u, normal is in direction n, 
			//refracted direction is (ni/nr) u + ((ni/nr)cos(incident angle) - cos(refelcted angle))n
			//Rr = (n * V) + (n * c1 - c2) * N 
			myVector uVec = new myVector(refrEyeDir);
			//uVec.set(V);
			//mult by -1 to account for using to-eye dir - equation we are using 
			uVec._mult(n * -1);
     
			myVector nVec = new myVector(N);
			//nVec.set(N);
	      	nVec._mult((n * cosTheta1) - cosTheta2);
	      	uVec._add(nVec);
	      	refractDir.set(uVec);
	      	refractDir._normalize();    
	        //myRay refrRay = getFwdTransRay(hit, hitLoc, refractDir, hit.transRay.gen+1, hit.CTMara[hit.obj.glblIDX], false);
	      	myRay refrRay = new myRay(scene, hitLoc, refractDir, hit.transRay.gen+1);
	      	//need to set ktrans for the material this ray is in
	      	refrRay.setCurrKTrans(KTrans, currPerm, curPermClr);
	      	//color where ray hits
	      	myColor refractColor = scene.reflectRay(refrRay);
	      	double preMult1MKtrans = oneMTransReflRatio * KTrans;
	      	r += preMult1MKtrans * (refractColor.RGB.x);
	      	g += preMult1MKtrans * (refractColor.RGB.y);
	      	b += preMult1MKtrans * (refractColor.RGB.z);
	      
	      	scene.refrRays++;
		}//if refracting
    
		if (transReflRatio > 0) {         
			//reflecting ray off surface
			//add more than 1 for ray generation to decrease number of internal reflection rays
			reflDir._mult(refractNormMult);
			//myRay reflRay = getFwdTransRay(hit, hitLoc, reflDir, hit.transRay.gen+1, hit.CTMara[hit.obj.glblIDX], false);
			myRay reflRay = new myRay(scene, hitLoc, reflDir, hit.transRay.gen+1);
			//myRay reflRay = new myRay(intX, intY, intZ, reflDir, numRays - 2);
			//color where ray hits
			myColor reflectColor = scene.reflectRay(reflRay);
			//println("internal reflection color r g b : " + red(reflectColor) + "|" + green(reflectColor) + "|" + blue(reflectColor));
			//added color component for reflection
	      	double preMultKtrans = transReflRatio * KRefl;
			r += preMultKtrans * (reflectColor.RGB.x);
			g += preMultKtrans * (reflectColor.RGB.y);
			b += preMultKtrans * (reflectColor.RGB.z);          
			++scene.reflRays;
		}			
		return new double[]{r,g,b};			
	}//calcSimpleTransClr
	
	//this returns the color value at a particular point on the object, based on where the incident view ray hits it. 	
	@Override
	public myColor getColorAtPos(rayHit hit){
		dbgRayHits++;		
		double r = ambientColor.RGB.x, g = ambientColor.RGB.y, b = ambientColor.RGB.z;
		double [] shadowRes = calcShadowColor(hit, txtr.getDiffTxtrColor(hit, diffuseColor, 1.0));
		r += shadowRes[0]; 	g += shadowRes[1]; 	b += shadowRes[2];
		   
		//now need kRefl factor - need to be careful with reflection - don't want to go further than 
		//recursive depth of numRays - need to leave room for one more for shadows, that's why -2 not -1
		if ((hit.transRay.gen < scene.numRays-2) && shdrFlags[hasCaustic]){
			//replace with either/or for transparent or reflective			
			double[] res = new double[]{0,0,0};
			if (KTrans > 0){	 				res = calcSimpleTransClr(hit);  			}//if refraction happens
			else if (KRefl > 0.0){ 				res = calcReflClr(hit, KReflClr.RGB);						}//if reflection happens	
			r += res[0]; 	g += res[1]; 	b += res[2];
		}//if enough rays to recurse   		
		return new myColor(r,g,b);
	}//getcoloratpos method	
	public String toString(){
		String res = "Simple shdr model from proj 1 : " + super.toString();
		return res;
	}
}//mySimpleReflObjShdr