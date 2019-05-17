package base_RayTracer.scene.shaders;

import java.util.*;

import base_RayTracer.myColor;
import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.geometry.myGeomBase;
import base_RayTracer.scene.geometry.sceneObjects.myInstance;
import base_RayTracer.scene.geometry.sceneObjects.lights.myLight;
import base_RayTracer.scene.geometry.sceneObjects.lights.myPhoton;
import base_RayTracer.scene.textures.myImageTexture;
import base_RayTracer.scene.textures.myTextureHandler;
import processing.core.PConstants;
import base_Utils_Objects.*;

public class myObjShader {
	public myScene scene;
	
	public String shType;		//what kind of shader - needed?
	//number of times a color is looked up on this object
	public int dbgRayHits;  
	
	public myTextureHandler txtr;
	public static final double epsVal = myScene.epsVal;//.0000001;

	 //phong exponent, reflective and transimitted contants
	public double phongExp, KRefl, KTrans, currPerm, diffConst;
	//set values of color contants
	public myColor diffuseColor, ambientColor, specularColor, curPermClr, KReflClr;  
	public myVector phtnDiffScl, phtnSpecScl, phtnPermClr;	//needs to go over 1
	public double avgDiffClr, avgSpecClr, avgPermClr;
	//used as array indices
	public static final int R = 0, G = 1, B = 2;
	
	public boolean[] shdrFlags;					//various state-related flags for this shader
	public static final int 
		hasCaustic			= 0,			//this shader will generate a caustic, either reflective or refractive
		usePhotonMap		= 1,			//this shader should incorporate the effects of a photon map
		isCausticPhtnIDX 	= 2;
	public static final int numFlags = 3;	
	
	
	public myObjShader(myScene _scn) {
		scene = _scn;
		initFlags();
	    dbgRayHits = 0;
	    shType="";
	    //these values need to be set by surface or diffuse command in cli
	    diffuseColor = new myColor(0,0,0);
	    phtnDiffScl = new myVector(0,0,0);
	    phtnSpecScl = new myVector(0,0,0);
	    phtnPermClr = new myVector(0,0,0);
	    ambientColor = new myColor(0,0,0);
	    specularColor = new myColor(0,0,0);
	    setCurrColors();
	    txtr = new myImageTexture(scene, this);			//default is image texture
	}
	public void initFlags(){shdrFlags = new boolean[numFlags];for(int i=0; i<numFlags;++i){shdrFlags[i]=false;}}
	private double getAvgColor(myColor clr){ return (1.0/3.0) * (clr.RGB.x + clr.RGB.y + clr.RGB.z);}
	private myVector getAvgPhtnColor(myColor base, double avg){ return new myVector(base.RGB.x/avg,base.RGB.y/avg,base.RGB.z/avg);}
	public void setCurrColors(){
	    diffuseColor.set(scene.currDiffuseColor);
	    avgDiffClr = getAvgColor(diffuseColor);
	    //if(avgDiffClr != 0){  phtnDiffScl.set(diffuseColor.RGB.x/avgDiffClr,diffuseColor.RGB.y/avgDiffClr,diffuseColor.RGB.z/avgDiffClr);}
	    if(avgDiffClr != 0){  phtnDiffScl = getAvgPhtnColor(diffuseColor,avgDiffClr);}
	    ambientColor.set(scene.currAmbientColor);
	    specularColor.set(scene.currSpecularColor);
	    avgSpecClr = getAvgColor(specularColor);    
	 //   if(avgSpecClr != 0){  phtnSpecScl.set(specularColor.RGB.x/avgSpecClr,specularColor.RGB.y/avgSpecClr,specularColor.RGB.z/avgSpecClr);}
	    if(avgSpecClr != 0){  phtnSpecScl = getAvgPhtnColor(specularColor,avgSpecClr);}
	    KRefl = scene.currKRefl;
	    KReflClr = new myColor(scene.currKReflClr.RGB.x,scene.currKReflClr.RGB.y,scene.currKReflClr.RGB.z);
	    KTrans = scene.currKTrans;
	    curPermClr = new myColor(scene.globCurPermClr.RGB.x,scene.globCurPermClr.RGB.y,scene.globCurPermClr.RGB.z);
	    avgPermClr = getAvgColor(curPermClr);
	    if(avgPermClr != 0){  phtnPermClr = getAvgPhtnColor(curPermClr,avgPermClr);}
	    currPerm = scene.globRfrIdx;
	    
	    shdrFlags[hasCaustic] = ((KRefl > 0.0) || (currPerm > 0.0) || (KTrans > 0.0));
	    shdrFlags[usePhotonMap] = scene.scFlags[scene.usePhotonMapIDX];
	    shdrFlags[isCausticPhtnIDX] = scene.scFlags[scene.isCausticPhtnIDX];
	    
	    diffConst = 1 - currPerm;	        
	    phongExp = scene.currPhongExp;
  	}  	
  	
  	//calculate the perpendicular component of the reflected amount of light (fresnel equations) - multiplied by the color from the recursive ray reflect equations
  	protected double calcFresPerp(double n1, double n2, double cosThetaI, double cosThetaT){
  		double n1cthI = n1*cosThetaI, n2cthT = n2*cosThetaT, nd = (n1cthI - n2cthT)/(n1cthI + n2cthT);
  		return nd*nd;
  	}
  	//calculate the parallel component of the refracted amount of light (fresnel equations) - multiplied by the color from the recursive ray refract equations
  	protected double calcFresPlel(double n1, double n2, double cosThetaI, double cosThetaT){
  		double  n1cthT = n1*cosThetaT, n2cthI = n2*cosThetaI, nd = (n1cthT - n2cthI)/(n1cthT + n2cthI);
  		return  nd*nd;
  	}
  	
  	// computes the reflected vector - assumes the incoming eye/light vector is pointed away from the point of intersection (same dir as normal)
  	public myVector compReflDir (myVector eyeDir, myVector objNorm ) {
  		myVector reflDir = new myVector(0,0,0);
  		double dotProd = 2 * (eyeDir._dot(objNorm));
  		myVector tempVect = new myVector(objNorm.x * dotProd,objNorm.y * dotProd,objNorm.z * dotProd);
  		
  		reflDir = new myVector(eyeDir,tempVect);
  		reflDir._normalize();
  		return reflDir;  
  	}//compute reflection direction vector
 	
  	protected double[] calcShadowColor(rayHit hit, double[] texTopColor){
  		double r=0,  g=0, b=0;
  		myVector lightNorm = new myVector(0,0,0), 
//  				hitLoc = hit.hitLoc,						//point on inv-obj-transformed ray where hit occurs, transformed to world coords
  		  				hitLoc = hit.fwdTransHitLoc,						//point on inv-obj-transformed ray where hit occurs, transformed to world coords
  				hNorm = new myVector(0,0,0);
  		//find contributions for each light
  		for (myGeomBase lightObj : scene.lightList){
  			myLight light;
  			if(lightObj instanceof myLight){light = (myLight)lightObj;} 
  			else {							light = (myLight)((myInstance)lightObj).obj;}			//possibly an instance of a light
  			//lightnormal is light origin minus ray-object intersection point normalized, in direction of object to light 
  			//transform the light's origin (fwd), use this object's fwd transformed hit location
  			//uncomment below for instances of bvh - need to fix this TODO
  			//lightNorm.set(hit.transRay.getTransformedPt(hit.transRay.getTransformedPt(light.getOrigin(hit.transRay.time),light.CTMara[light.glblIDX]), hit.CTMara[hit.obj.invIDX]));		
  			//comment below for instances of bvh - need to fix this TODO  			
  			lightNorm.set(hit.transRay.getTransformedPt(light.getOrigin(hit.transRay.getTime()),light.CTMara[light.glblIDX]));		
  			lightNorm._sub(hitLoc);				//this is point on transformed ray 
  			lightNorm._normalize();
  			//to calculate shadows at this spot, need to check for intersections again making a new ray out of light norm
  			//myRay shadowRay = hit.transRay.getTransformedRay(new myRay(scene, hitLoc, lightNorm, hit.transRay.gen+1), hit.CTMara[hit.obj.glblIDX], false);
  			myRay shadowRay = new myRay(scene, hitLoc, lightNorm, hit.transRay.gen+1);
  			rayHit light_hit = light.intersectCheck(shadowRay, shadowRay, light.CTMara);//get t value of light intersection
  			double t=light_hit.t, ltMult=light_hit.ltMult;
  			if(ltMult == 0){ continue;}//this light has no contribution to light - out of field of light 			
  			//need to check if this ray intersects anything on the way to the light.  if so, this light will have no contribution at this point 
  			//0 - no object between light and origin of ray; 1 - opaque object between light and origin of ray; 2 - transparent object between light and origin?  possibly used for something - color of area "in shadow"? TODO
  			int lightBlocked = scene.calcShadow(shadowRay, t);     
  			if (lightBlocked==0){//light is not blocked so...
  				//get contribution from light by taking dotproduct of light's normal with surface normal
  				shadowRay.direction._normalize();
  				double lightDotProd = shadowRay.direction._dot(hit.objNorm)*ltMult;		//ltMult handles spot light penumbra
  				if (lightDotProd > epsVal){//hitting top of object
  					r += texTopColor[R] * light.lightColor.RGB.x * lightDotProd;
  					g += texTopColor[G] * light.lightColor.RGB.y * lightDotProd;
  					b += texTopColor[B] * light.lightColor.RGB.z * lightDotProd;
  				}//if light greater than 0	
  				//if phongExp set to 0 then ignore spec color calc
  				if(phongExp == 0){continue;}
  				//now calc phong shading/specularit : halfvector will then be light vector plus eye vector (- ray direction), normalized
  				hNorm.set(shadowRay.direction);
  				//hNorm._sub(hit.transRay.direction);
  				hNorm._sub(hit.fwdTransRayDir);
  				hNorm._normalize();
  				double hDotProd = hNorm._dot(hit.objNorm)*ltMult;
  				if (hDotProd > epsVal){
  					double  phHdotSq = Math.pow(hDotProd * hDotProd,phongExp);
  					//double phong exponent since we're using the half-angle vector formula
  					r += specularColor.RGB.x * light.lightColor.RGB.x * phHdotSq;
  					g += specularColor.RGB.y * light.lightColor.RGB.y * phHdotSq;
  					b += specularColor.RGB.z * light.lightColor.RGB.z * phHdotSq;
  				}//if dot prod>0
  			}//if light not blocked
  		}//for each light
  		return new double[]{r,g,b};
  	}//calcShadowColor
  	
 	
  	//calculate transparent/transmitted color and reflected color at surface
  	protected double[] calcTransClr(rayHit hit, myVector permClr){
  		double r=0,g=0,b=0;  		
  		myVector hitLoc = hit.fwdTransHitLoc,
  				backToEyeDir = new myVector(hit.fwdTransRayDir);
  		backToEyeDir._mult(-1);
  		
  		myVector reflDir ;//= compReflDir(backToEyeDir, objRayN);		  
  		
  		//incoming angle in radians, critical angle,ray angle upon exiting material
  		//n is ratio of refraction indicies for current material/new material (n1/n2)-use n1 and n2 to denote material refraction indicies - n1 is source material, n2 is destination refraction material
  		double thetaIncident = 0, thetaCrit = 0, thetaExit = 0, n = 1, n1 = 0, n2 = 0;
  		
  		double rPerp = 0, rPar = 0, 		//constant multipliers for reflection perp and parallel - average of rperp and rpar = transreflratio
  				transReflRatio = 0, 		//ratio of resulting transmission vs reflection at surface point - 0 means complete refraction, 1 means complete reflection
  				oneMTransReflRatio = 1,   				
  				exitMaterialTrans = 1, 		//eventually want to implement a method to handle exiting one material to another with a non-unity index of refraction (this is exitMaterialTrans)	
  				refractNormMult = 1.0;		//1 if refrDotProd is positive, -1 if refrDotProd is negative
  	
  		//dot product gives cos of angle of incidence 
  		//cross gives sine of angle - need both to verify normal direction
  		myVector N = new myVector(hit.objNorm);		//need to copy normal incase direction flips
  		double cosTheta1 = backToEyeDir._dot(N), cosTheta2 = 0;//calculated below
  		//thetaIncident = scene.pa._angleBetween(backToEyeDir, N);
  		thetaIncident = backToEyeDir.angleWithMe(N);
  		//the only way the ray doting the normal would be less than 0 is if the incident ray was coming from behind the normal (pointing in the same direction
  		//then the "eye"dir would form an angle greater than 90 degrees.  the only way this can happen is from inside the object
  		//this means the normal is pointing the same direction as the refrsurfdir (i.e. we are leaving a transparent object)
  		//we need to reverse the direction of the normal in this case
  		if (cosTheta1 < epsVal){
  			//flip direction of normal used for detecting reflection if theta incident greater than 90 degrees - use this to reverse the direction of the final refracted vector
  			refractNormMult = -1.0; 
  			N._mult(-1);
  		}
  		//recalculate in case N switched directions
  		cosTheta1 = backToEyeDir._dot(N);
  		//thetaIncident = scene.pa._angleBetween(backToEyeDir, N);
  		thetaIncident = backToEyeDir.angleWithMe(N);
  		//now find ratio of reflected to transmitted light using fresnel equations
  		//refractNormMult < 0 means we swapped direction of normal, means we're leaving an object          
  		//TODO : handle exiting to non-air (non-1 external ktrans)
  		boolean TIR = false;
  		if (refractNormMult < 0){//exit
  			//means ray is in direction of normal, so we had to flip normal for calculations - leaving object, entering air
  			thetaCrit = Math.asin(exitMaterialTrans/KTrans);
  			if (thetaIncident < thetaCrit){//determine refracting exit angle
  				n1 = KTrans;
  				n2 = exitMaterialTrans;
  				n = (n1/n2); 
  				thetaExit = Math.asin(n * Math.sin(thetaIncident));
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
  			n1 = hit.transRay.currKTrans[0];
  			n2 = KTrans;
  			//println("  entering material : " + n2);
  			n = (n1/n2);
  			thetaExit = Math.asin(n  * Math.sin(thetaIncident)); 
  			double tmpResultC2 = 1.0 - (n*n) * (1.0 - (cosTheta1*cosTheta1));
  			if (tmpResultC2 < 0){System.out.println("\tdanger #2 :  refraction bad : " +  tmpResultC2 + " ray cur ktrans : " + hit.transRay.currKTrans[0]);}
  			cosTheta2 = Math.pow(tmpResultC2,.5);
  		}       
  		//if not tir, calculate transreflratio to determine how much is transmitted, how much is reflected ala fresnel
  		if (!TIR){					
  			double sinAcos = (Math.sin(Math.acos(cosTheta1))),  resCosThetT = Math.pow(1.0 - ((n1/n2) * sinAcos * sinAcos),.5);					
  			rPerp = calcFresPerp(n1, n2, cosTheta1,resCosThetT);
  			rPar = calcFresPlel(n1, n2, cosTheta1,resCosThetT);
  			transReflRatio = (rPerp + rPar)/2.0;    
  			oneMTransReflRatio = 1 - transReflRatio;
  			if (oneMTransReflRatio < epsVal) { System.out.println("one minus tr = 1");}      
  		}        
  		//sanity check
  		if (transReflRatio > 1){ System.out.println("impossible result - treflRat, rPerp, rPar : " + transReflRatio + " | " + rPerp + " | " + rPar);}
      
  		if (oneMTransReflRatio > epsVal){//if 1 then no refraction
  			//incident ray is in direction u, normal is in direction n, 
  			//refracted direction is (ni/nr) u + ((ni/nr)cos(incident angle) - cos(refelcted angle))n
  			//Rr = (n * V) + (n * c1 - c2) * N 
  			myVector uVec = new myVector(backToEyeDir);
  			//mult by -1 to account for using to-eye dir - equation we are using 
  			uVec._mult(n * -1);       
  			myVector nVec = new myVector(N);
  	      	nVec._mult((n * cosTheta1) - cosTheta2);
  	      	uVec._add(nVec);
  	      	myVector refractDir = new myVector(uVec);
  	      	refractDir._normalize();    
  	      	myRay refrRay = new myRay(scene, hitLoc, refractDir, hit.transRay.gen+1);  	      	
  	      	refrRay.setCurrKTrans(KTrans, currPerm, curPermClr);//need to set ktrans for the material this ray is in
  	      	//color where ray hits
  	      	myColor refractColor = scene.reflectRay(refrRay);
  	      	r += (oneMTransReflRatio) * permClr.x * (refractColor.RGB.x);
  	      	g += (oneMTransReflRatio) * permClr.y * (refractColor.RGB.y);
  	      	b += (oneMTransReflRatio) * permClr.z * (refractColor.RGB.z);
  	      
  	      	scene.refrRays++;
  		}//if refracting
      
  		if (transReflRatio > epsVal) {         
  			//reflecting ray off surface
  			//add more than 1 for ray generation to decrease number of internal reflection rays
  	  		reflDir = compReflDir(backToEyeDir, N);
 			reflDir._mult(refractNormMult);		//for leaving material
  			myRay reflRay = new myRay(scene, hitLoc, reflDir, hit.transRay.gen+1);
  			reflRay.setCurrKTrans(KTrans, currPerm, curPermClr);  	      	
  			//color where ray hits
  			myColor reflectColor = scene.reflectRay(reflRay);
  			//println("internal reflection color r g b : " + red(reflectColor) + "|" + green(reflectColor) + "|" + blue(reflectColor));
  			//added color component for reflection
  			r += (transReflRatio) *  permClr.x * (reflectColor.RGB.x);
  			g += (transReflRatio) *  permClr.y * (reflectColor.RGB.y);
  			b += (transReflRatio) *  permClr.z * (reflectColor.RGB.z);          
  			scene.reflRays++;
  		}	
  		
  		return new double[]{r,g,b};		
  	}//calcTransClr()	
  	//calc reflected color - simple reflection
  	protected double[] calcReflClr(rayHit hit, myVector reflClrVec){
 		double r=0,g=0,b=0;
  		myVector hitLoc = hit.fwdTransHitLoc, backEyeDir = new myVector(hit.fwdTransRayDir);  		
  		backEyeDir._mult(-1);
  		myVector reflDir = compReflDir(backEyeDir, hit.objNorm);	  
  		if (reflDir._dot(hit.objNorm) >= 0){//reflections from behind can't happen 
  			//reflecting ray off surface
  			myRay reflRay = new myRay(scene, hitLoc, reflDir, hit.transRay.gen+1);
  			//color where ray hits
  			myColor reflectColor = scene.reflectRay(reflRay);
  			//added color component for reflection
  			r += (reflClrVec.x * reflectColor.RGB.x); 
  			g += (reflClrVec.y * reflectColor.RGB.y); 
  			b += (reflClrVec.z * reflectColor.RGB.z); 
  		}//reflections from behind can't happen
  		return new double[]{r,g,b};	
  	}//calcReflClr
 	
	//calculate transmitted ray
  	protected myRay calcTransRay(rayHit hit){
  		myVector hitLoc = hit.fwdTransHitLoc,
  				backToEyeDir = new myVector(hit.fwdTransRayDir);
  		backToEyeDir._mult(-1);
  		
  		myVector reflDir ;//= compReflDir(backToEyeDir, objRayN);		  
  		
  		//incoming angle in radians, critical angle,ray angle upon exiting material
  		//n is ratio of refraction indicies for current material/new material (n1/n2)-use n1 and n2 to denote material refraction indicies - n1 is source material, n2 is destination refraction material
  		double thetaIncident = 0, thetaCrit = 0, thetaExit = 0, n = 1, n1 = 0, n2 = 0;
  		
  		double rPerp = 0, rPar = 0, 		//constant multipliers for reflection perp and parallel - average of rperp and rpar = transreflratio
  				transReflRatio = 0, 		//ratio of resulting transmission vs reflection at surface point - 0 means complete refraction, 1 means complete reflection
  				oneMTransReflRatio = 1,   				
  				exitMaterialTrans = 1, 		//eventually want to implement a method to handle exiting one material to another with a non-unity index of refraction (this is exitMaterialTrans)	
  				refractNormMult = 1.0;		//1 if refrDotProd is positive, -1 if refrDotProd is negative
  	
  		//dot product gives cos of angle of incidence 
  		//cross gives sine of angle - need both to verify normal direction
  		myVector N = new myVector(hit.objNorm);		//need to copy normal incase direction flips
  		double cosTheta1 = backToEyeDir._dot(N), cosTheta2 = 0;//calculated below
  		//thetaIncident = scene.pa._angleBetween(backToEyeDir, N);
  		thetaIncident = backToEyeDir.angleWithMe(N);
 		//the only way the ray doting the normal would be less than 0 is if the incident ray was coming from behind the normal (pointing in the same direction
  		//then the "eye"dir would form an angle greater than 90 degrees.  the only way this can happen is from inside the object
  		//this means the normal is pointing the same direction as the refrsurfdir (i.e. we are leaving a transparent object)
  		//we need to reverse the direction of the normal in this case
  		if (cosTheta1 < epsVal){
  			//flip direction of normal used for detecting reflection if theta incident greater than 90 degrees - use this to reverse the direction of the final refracted vector
  			refractNormMult = -1.0; 
  			N._mult(-1);
  		}
  		//recalculate in case N switched directions
  		cosTheta1 = backToEyeDir._dot(N);
  		//thetaIncident = scene.pa._angleBetween(backToEyeDir, N);
  		thetaIncident = backToEyeDir.angleWithMe(N);
  		//now find ratio of reflected to transmitted light using fresnel equations
  		//refractNormMult < 0 means we swapped direction of normal, means we're leaving an object          
  		//TODO : handle exiting to non-air (non-1 external ktrans)
  		boolean TIR = false;
  		if (refractNormMult < 0){//exit
  			//means ray is in direction of normal, so we had to flip normal for calculations - leaving object, entering air
  			thetaCrit = Math.asin(exitMaterialTrans/KTrans);
  			if (thetaIncident < thetaCrit){//determine refracting exit angle
  				n1 = KTrans;
  				n2 = exitMaterialTrans;
  				n = (n1/n2); 
  				thetaExit = Math.asin(n * Math.sin(thetaIncident));
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
  			n1 = hit.transRay.currKTrans[0];
  			n2 = KTrans;
  			//println("  entering material : " + n2);
  			n = (n1/n2);
  			thetaExit = Math.asin(n  * Math.sin(thetaIncident)); 
  			double tmpResultC2 = 1.0 - (n*n) * (1.0 - (cosTheta1*cosTheta1));
  			if (tmpResultC2 < 0){System.out.println("\tdanger #2 :  refraction bad : " +  tmpResultC2 + " ray cur ktrans : " + hit.transRay.currKTrans[0]);}
  			cosTheta2 = Math.pow(tmpResultC2,.5);
  		}       
  		//if not tir, calculate transreflratio to determine how much is transmitted, how much is reflected ala fresnel
  		if (!TIR){					
  			double sinAcos = (Math.sin(Math.acos(cosTheta1))),  resCosThetT = Math.pow(1.0 - ((n1/n2) * sinAcos * sinAcos),.5);					
  			rPerp = calcFresPerp(n1, n2, cosTheta1,resCosThetT);
  			rPar = calcFresPlel(n1, n2, cosTheta1,resCosThetT);
  			transReflRatio = (rPerp + rPar)/2.0;    
  			oneMTransReflRatio = 1 - transReflRatio;
  			if (oneMTransReflRatio < epsVal) { System.out.println("one minus tr = 1");}      
  		}        
  		//sanity check
  		if (transReflRatio > 1){ System.out.println("impossible result - treflRat, rPerp, rPar : " + transReflRatio + " | " + rPerp + " | " + rPar);}
      
  		if (oneMTransReflRatio > epsVal){//if transReflRatio = 1 then no refraction
  			//incident ray is in direction u, normal is in direction n, 
  			//refracted direction is (ni/nr) u + ((ni/nr)cos(incident angle) - cos(refelcted angle))n
  			//Rr = (n * V) + (n * c1 - c2) * N 
  			myVector uVec = new myVector(backToEyeDir);
  			//mult by -1 to account for using to-eye dir - equation we are using 
  			uVec._mult(n * -1);       
  			myVector nVec = new myVector(N);
  	      	nVec._mult((n * cosTheta1) - cosTheta2);
  	      	uVec._add(nVec);
  	      	myVector refractDir = new myVector(uVec);
  	      	refractDir._normalize();    
  	      	myRay refrRay = new myRay(scene, hitLoc, refractDir, hit.transRay.gen+1);  	      	
  	      	refrRay.setCurrKTrans(KTrans, currPerm, curPermClr);//need to set ktrans for the material this ray is in
  	      	return refrRay;
  		}//if refracting
			//reflecting ray off surface
		//add more than 1 for ray generation to decrease number of internal reflection rays
  		reflDir = compReflDir(backToEyeDir, N);
		reflDir._mult(refractNormMult);		//for leaving material
		myRay reflRay = new myRay(scene, hitLoc, reflDir, hit.transRay.gen+1);
		reflRay.setCurrKTrans(KTrans, currPerm, curPermClr);  	      	
		return reflRay;
  	}//calcTransRay()	
  	
  	//calc reflected color - simple reflection
  	protected myRay calcReflRay(rayHit hit){
  		myVector hitLoc = hit.fwdTransHitLoc, backEyeDir = new myVector(hit.fwdTransRayDir);  		
  		backEyeDir._mult(-1);
  		myVector reflDir = compReflDir(backEyeDir, hit.objNorm);	  
  			//reflecting ray off surface
  		return new myRay(scene, hitLoc, reflDir, hit.transRay.gen+1);
   	}//calcReflClr

  	//this returns the color value at a particular point on the object, based on where the incident view ray hits it. 
  	public myColor getColorAtPos(rayHit hit){
  		//need to get color from photon map
  		dbgRayHits++;		
  		double r = ambientColor.RGB.x, g = ambientColor.RGB.y, b = ambientColor.RGB.z;
  		double[] phtnIrr;
  		//TODO separate caustics and indirect into 2 processes
  		if((KRefl == 0.0) && (shdrFlags[usePhotonMap])){
 			//r += diffuseColor.RGB.x * phtnIrr[0]; 		g += diffuseColor.RGB.y * phtnIrr[1]; 		b += diffuseColor.RGB.z * phtnIrr[2]; 
  			phtnIrr = getIrradianceFromPhtnTree(hit);
  			if(shdrFlags[isCausticPhtnIDX]){//visualize photons directly for caustics
  				r += phtnIrr[0]; 		g += phtnIrr[1]; 		b += phtnIrr[2];
  			} else {					// indirect illumination effects
 				r += diffuseColor.RGB.x * phtnIrr[0]; 		g += diffuseColor.RGB.y * phtnIrr[1]; 		b += diffuseColor.RGB.z * phtnIrr[2]; 
  			}
  		}  	     		
  		//find contributions for each light
  		double[] shadowRes = calcShadowColor(hit, txtr.getDiffTxtrColor(hit, diffuseColor, diffConst));
  		r += shadowRes[0]; 		g += shadowRes[1]; 		b += shadowRes[2];
  		//now need kRefl factor - need to be careful with reflection - don't want to go further than 
  		//recursive depth of numRays - need to leave room for one more for shadows, that's why -2 not -1
  		if ((hit.transRay.gen < scene.numRays-2) && shdrFlags[hasCaustic]){
  			//replace with either/or for transparent or reflective			
  			double[] res = new double[]{0,0,0};
  			if ((KTrans > 0) || (currPerm > 0.0)){				res = calcTransClr(hit, curPermClr.RGB); 			}//TODO clean this up : if refraction happens (also handles reflection - splits rays)
  			else if (KRefl > 0.0){								res = calcReflClr(hit, KReflClr.RGB); 			}//if reflection happens	
  			r += res[0]; 	g += res[1]; 	b += res[2];
  		}//if enough rays to recurse and this material reflects/refracts
 	
  		return new myColor(r,g,b);
  	}//getcoloratpos method  	
  	
	//find irradiance of a particular location from photon tree using neighborhood  
	protected double[] getIrradianceFromPhtnTree(rayHit hit){
		//idx 0 is photon dir, idx 1 is phtn pwr
		double[] res = new double[]{0,0,0};		
		ArrayList<myPhoton> hood = scene.photonTree.find_near(hit.fwdTransHitLoc.x, hit.fwdTransHitLoc.y, hit.fwdTransHitLoc.z);//location in world coords
		//ArrayList<Photon> hood = scene.photonTree.find_near((float)hit.fwdTransHitLoc.x, (float)hit.fwdTransHitLoc.y, (float)hit.fwdTransHitLoc.z, scene.kNhood, scene.ph_max_near_dist);//location in world coords
		int hoodSize = hood.size();
		if ((hoodSize == 0) || (hood.get(0) == null)){return res;}
		double rSq = hood.get(0).pos[3];				//furthest photon is in first idx of hood, sqdist is 3rd position of pos ara
		double area = PConstants.PI * rSq;// * Math.sqrt(rSq) * 1.33333;//vol of differential hemi-sphere
		for(int i=0;i<hoodSize;++i){
			myPhoton phtn = hood.get(i);
			res[0] += phtn.pwr[0];
			res[1] += phtn.pwr[1];
			res[2] += phtn.pwr[2];
		}
		res[0] /= area;	res[1] /= area;	res[2] /= area;		
		return res;
	}//getIrradianceFromPhtnTree   	
  	
  	//call this when a caustic-generating object is hit, it will return the ray hit of the reflected/refracted ray
  	public myRay findCausticRayHit(rayHit hit, double[] phtn_pwr){ 
  		myRay res = null;
 		if ((hit.transRay.gen < scene.numPhotonRays) && shdrFlags[hasCaustic]){
 			double[] pwrMult = new double[]{1.0f,1.0f,1.0f};
  			if ((KTrans > 0.0) || (currPerm > 0.0)){	
  				pwrMult[0] = phtnPermClr.x;
  				pwrMult[1] = phtnPermClr.y;
  				pwrMult[2] = phtnPermClr.z;
  				res = calcTransRay(hit); 			
  			}
  			else if (KRefl > 0.0){								
  				pwrMult[0] = KRefl;	pwrMult[1] = KRefl;	pwrMult[2] = KRefl;
 				res = calcReflRay(hit); 			
  			}//if reflection happens	
  			for(int i=0;i<3;++i){hit.phtnPwr[i] = phtn_pwr[i] * pwrMult[i];}
   		}//if enough rays to recurse and this material reflects/refracts
 		return res;
  	}//keep reflect/refract until hit diffuse object
 
  	public String showUV(){	return txtr.showUV();}//showUV

	public String toString(){
		String result = "Shader type : " + shType +  " |\t ray hits on obj : " + dbgRayHits;
		result += "\n\tDIFFUSE : " + diffuseColor.toString() + " diff const : " + diffConst + " | " ;
		result += "AMBIENT : " + ambientColor.toString() + " | ";
		result += "SPECULAR : " + specularColor.toString();
		result += "\n\tphongExp : " + phongExp;
		result += " krefl : " + KRefl;
		result += " ktrans : " + KTrans;
		result += "\n\tcurrent 'permiability' : " + currPerm + " perm for r g b : " + curPermClr.toString();
		return result;
	} 
}//myObjShader


