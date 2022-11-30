package base_RayTracer.scene.shaders;


import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.ObjInstance;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.scene.geometry.sceneObjects.lights.base.Base_Light;
import base_RayTracer.scene.photonMapping.myPhoton;
import base_RayTracer.scene.textures.base.Base_TextureHandler;
import base_RayTracer.scene.textures.imageTextures.myImageTexture;

import java.util.PriorityQueue;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * 
 * @author 7strb
 *
 */
public class myObjShader {
	public Base_Scene scene;
	
	public String shType;		//what kind of shader - needed?
	//number of times a color is looked up on this object
	public int dbgRayHits;  
	
	public Base_TextureHandler txtr;
	
	 //phong exponent, reflective and transimitted contants
	public double phongExp, KRefl, KTrans, currPerm, diffConst;
	//set values of color contants
	protected myRTColor diffuseColor, ambientColor, specularColor, curPermClr, KReflClr;  
	
	public myPoint phtnDiffScl, phtnSpecScl, phtnPermClr;	//needs to go over 1
	
	public double avgDiffClr, avgSpecClr, avgPermClr;
	//used as array indices
	public static final int R = 0, G = 1, B = 2;
	
	private int[] shdrFlags;					//various state-related flags for this shader
	public static final int 
		hasCausticIDX		= 0,			//this shader will generate a caustic, either reflective or refractive
		usePhotonMapIDX		= 1,			//this shader should incorporate the effects of a photon map
		isCausticPhtnIDX 	= 2;
	public static final int numFlags = 3;	
	
	
	public myObjShader(Base_Scene _scn) {
		scene = _scn;
		initFlags();
	    dbgRayHits = 0;
	    shType="";
	    //these values need to be set by surface or diffuse command in cli
	    phtnDiffScl = new myPoint(0,0,0);
	    phtnSpecScl = new myPoint(0,0,0);
	    phtnPermClr = new myPoint(0,0,0);
	    
	    ambientColor = new myRTColor(0,0,0);
	    
	    diffuseColor = new myRTColor(0,0,0);
	    specularColor = new myRTColor(0,0,0);
	    curPermClr = new myRTColor(0,0,0);
	    setCurrColors();
	    txtr = new myImageTexture(scene, this);			//default is image texture
	}
	private final double third = 1.0/3.0;
	private double getAvgColor(myRTColor clr){ return third * (clr.x + clr.y + clr.z);}
	private myVector getAvgPhtnColor(myRTColor base, double avg){ return new myVector(base.x/avg,base.y/avg,base.z/avg);}
	protected void setCurrColors(){
	    ambientColor.set(scene.currAmbientColor);
	    
		diffuseColor.set(scene.currDiffuseColor);
	    avgDiffClr = getAvgColor(diffuseColor);
	    if(avgDiffClr != 0){  phtnDiffScl = getAvgPhtnColor(diffuseColor,avgDiffClr);}

	    specularColor.set(scene.currSpecularColor);
	    avgSpecClr = getAvgColor(specularColor);    
	    if(avgSpecClr != 0){  phtnSpecScl = getAvgPhtnColor(specularColor,avgSpecClr);}
	    
	    curPermClr = new myRTColor(scene.globCurPermClr);
	    avgPermClr = getAvgColor(curPermClr);
	    if(avgPermClr != 0){  phtnPermClr = getAvgPhtnColor(curPermClr,avgPermClr);}
	    
	    KRefl = scene.currKRefl;
	    KReflClr = new myRTColor(scene.currKReflClr);
	    KTrans = scene.currKTrans;
	    currPerm = scene.globRfrIdx;

    
	    setHasCaustic((KRefl > 0.0) || (currPerm > 0.0) || (KTrans > 0.0));
	    setUsePhotonMap(scene.scFlags[Base_Scene.usePhotonMapIDX]);
	    setIsCausticPtn(scene.scFlags[Base_Scene.isCausticPhtnIDX]);
	    
	    diffConst = 1 - currPerm;	        
	    phongExp = scene.currPhongExp;
  	}  	
  	
  /**
   * calculate the perpendicular component of the reflected amount of light (fresnel equations) - multiplied by the color from the recursive ray reflect equations
   * @param n1
   * @param n2
   * @param cosThetaI
   * @param cosThetaT
   * @return
   */
  	protected double calcFresPerp(double n1, double n2, double cosThetaI, double cosThetaT){
  		double n1cthI = n1*cosThetaI, n2cthT = n2*cosThetaT, nd = (n1cthI - n2cthT)/(n1cthI + n2cthT);
  		return nd*nd;
  	}
  	/**
  	 * calculate the parallel component of the refracted amount of light (fresnel equations) - multiplied by the color from the recursive ray refract equations
  	 * @param n1
  	 * @param n2
  	 * @param cosThetaI
  	 * @param cosThetaT
  	 * @return
  	 */
  	protected double calcFresPlel(double n1, double n2, double cosThetaI, double cosThetaT){
  		double  n1cthT = n1*cosThetaT, n2cthI = n2*cosThetaI, nd = (n1cthT - n2cthI)/(n1cthT + n2cthI);
  		return  nd*nd;
  	}
  	
  	/**
  	 * computes the reflected vector - assumes the incoming eye/light vector is pointed away from the point of intersection (same dir as normal)
  	 * @param eyeDir
  	 * @param objNorm
  	 * @return
  	 */
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
  		myVector lightNorm = new myVector(0,0,0);
//  				hitLoc = hit.hitLoc,						//point on inv-obj-transformed ray where hit occurs, transformed to world coords
  		myPoint hitLoc = hit.fwdTransHitLoc;						//point on inv-obj-transformed ray where hit occurs, transformed to world coords
  		myVector hNorm = new myVector(0,0,0);
  		//find contributions for each light
  		for (Base_Geometry lightObj : scene.lightList){
  			Base_Light light;
  			if(lightObj instanceof Base_Light){light = (Base_Light)lightObj;} 
  			else {							light = (Base_Light)((ObjInstance)lightObj).obj;}			//possibly an instance of a light
  			//lightnormal is light origin minus ray-object intersection point normalized, in direction of object to light 
  			//transform the light's origin (fwd), use this object's fwd transformed hit location
  			//uncomment below for instances of bvh - need to fix this TODO
  			//lightNorm.set(hit.transRay.getTransformedPt(hit.transRay.getTransformedPt(light.getOrigin(hit.transRay.time),light.CTMara[light.glblIDX]), hit.CTMara[hit.obj.invIDX]));		
  			//comment below for instances of bvh - need to fix this TODO  			
  			lightNorm.set(hit.transRay.getTransformedPt(light.getOrigin(hit.transRay.getTime()),light.CTMara[Base_Geometry.glblIDX]));		
  			lightNorm._sub(hitLoc);				//this is point on transformed ray 
  			lightNorm._normalize();
  			//to calculate shadows at this spot, need to check for intersections again making a new ray out of light norm
  			//myRay shadowRay = hit.transRay.getTransformedRay(new myRay(scene, hitLoc, lightNorm, hit.transRay.gen+1), hit.CTMara[hit.obj.glblIDX], false);
  			rayCast shadowRay = new rayCast(scene, hitLoc, lightNorm, hit.transRay.gen+1);
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
  				if (lightDotProd > MyMathUtils.EPS){//hitting top of object
  					r += texTopColor[R] * light.lightColor.x * lightDotProd;
  					g += texTopColor[G] * light.lightColor.y * lightDotProd;
  					b += texTopColor[B] * light.lightColor.z * lightDotProd;
  				}//if light greater than 0	
  				//if phongExp set to 0 then ignore spec color calc
  				if(phongExp == 0){continue;}
  				//now calc phong shading/specularit : halfvector will then be light vector plus eye vector (- ray direction), normalized
  				hNorm.set(shadowRay.direction);
  				//hNorm._sub(hit.transRay.direction);
  				hNorm._sub(hit.fwdTransRayDir);
  				hNorm._normalize();
  				double hDotProd = hNorm._dot(hit.objNorm)*ltMult;
  				if (hDotProd > MyMathUtils.EPS){
  					double  phHdotSq = Math.pow(hDotProd * hDotProd,phongExp);
  					//double phong exponent since we're using the half-angle vector formula
  					r += specularColor.x * light.lightColor.x * phHdotSq;
  					g += specularColor.y * light.lightColor.y * phHdotSq;
  					b += specularColor.z * light.lightColor.z * phHdotSq;
  				}//if dot prod>0
  			}//if light not blocked
  		}//for each light
  		return new double[]{r,g,b};
  	}//calcShadowColor
  	
 	
  	/**
  	 * calculate transparent/transmitted color and reflected color at surface
  	 * @param hit
  	 * @param permClr
  	 * @return
  	 */
  	protected double[] calcTransClr(rayHit hit, myPoint permClr){
  		double r=0,g=0,b=0;  		
  		myPoint hitLoc = hit.fwdTransHitLoc;
  		myVector backToEyeDir = new myVector(hit.fwdTransRayDir);
  		backToEyeDir._mult(-1);
  		
  		myVector reflDir ;//= compReflDir(backToEyeDir, objRayN);		  
  		
  		//incoming angle in radians, critical angle,ray angle upon exiting material
  		//n is ratio of refraction indicies for current material/new material (n1/n2)-use n1 and n2 to denote material refraction indicies - n1 is source material, n2 is destination refraction material
  		double thetaIncident = 0, thetaCrit = 0, //thetaExit = 0, 
  				n = 1, n1 = 0, n2 = 0;
  		
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
  		if (cosTheta1 < MyMathUtils.EPS){
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
  			n1 = hit.transRay.currKTrans[0];
  			n2 = KTrans;
  			//println("  entering material : " + n2);
  			n = (n1/n2);
  			//thetaExit = Math.asin(n  * Math.sin(thetaIncident)); 
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
  			if (oneMTransReflRatio < MyMathUtils.EPS) { System.out.println("one minus tr = 1");}      
  		}        
  		//sanity check
  		if (transReflRatio > 1){ System.out.println("impossible result - treflRat, rPerp, rPar : " + transReflRatio + " | " + rPerp + " | " + rPar);}
      
  		if (oneMTransReflRatio > MyMathUtils.EPS){//if 1 then no refraction
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
  	      	rayCast refrRay = new rayCast(scene, hitLoc, refractDir, hit.transRay.gen+1);  	      	
  	      	refrRay.setCurrKTrans(KTrans, currPerm, curPermClr);//need to set ktrans for the material this ray is in
  	      	//color where ray hits
  	      	myRTColor refractColor = scene.reflectRay(refrRay);
  	      	r += (oneMTransReflRatio) * permClr.x * (refractColor.x);
  	      	g += (oneMTransReflRatio) * permClr.y * (refractColor.y);
  	      	b += (oneMTransReflRatio) * permClr.z * (refractColor.z);
  	      
  	      	scene.refrRays++;
  		}//if refracting
      
  		if (transReflRatio > MyMathUtils.EPS) {         
  			//reflecting ray off surface
  			//add more than 1 for ray generation to decrease number of internal reflection rays
  	  		reflDir = compReflDir(backToEyeDir, N);
 			reflDir._mult(refractNormMult);		//for leaving material
  			rayCast reflRay = new rayCast(scene, hitLoc, reflDir, hit.transRay.gen+1);
  			reflRay.setCurrKTrans(KTrans, currPerm, curPermClr);  	      	
  			//color where ray hits
  			myRTColor reflectColor = scene.reflectRay(reflRay);
  			//println("internal reflection color r g b : " + red(reflectColor) + "|" + green(reflectColor) + "|" + blue(reflectColor));
  			//added color component for reflection
  			r += (transReflRatio) *  permClr.x * (reflectColor.x);
  			g += (transReflRatio) *  permClr.y * (reflectColor.y);
  			b += (transReflRatio) *  permClr.z * (reflectColor.z);          
  			scene.reflRays++;
  		}	
  		
  		return new double[]{r,g,b};		
  	}//calcTransClr()	
  	//calc reflected color - simple reflection
  	protected double[] calcReflClr(rayHit hit, myPoint reflClrVec){
 		double r=0,g=0,b=0;
  		myPoint hitLoc = hit.fwdTransHitLoc;
  		myVector backEyeDir = new myVector(hit.fwdTransRayDir);  		
  		backEyeDir._mult(-1);
  		myVector reflDir = compReflDir(backEyeDir, hit.objNorm);	  
  		if (reflDir._dot(hit.objNorm) >= 0){//reflections from behind can't happen 
  			//reflecting ray off surface
  			rayCast reflRay = new rayCast(scene, hitLoc, reflDir, hit.transRay.gen+1);
  			//color where ray hits
  			myRTColor reflectColor = scene.reflectRay(reflRay);
  			//added color component for reflection
  			r += (reflClrVec.x * reflectColor.x); 
  			g += (reflClrVec.y * reflectColor.y); 
  			b += (reflClrVec.z * reflectColor.z); 
  		}//reflections from behind can't happen
  		return new double[]{r,g,b};	
  	}//calcReflClr
 	
	/**
	 * calculate transmitted ray
	 * @param hit
	 * @return
	 */
  	protected rayCast calcTransRay(rayHit hit){
  		myPoint hitLoc = hit.fwdTransHitLoc;
  		myVector backToEyeDir = new myVector(hit.fwdTransRayDir);
  		backToEyeDir._mult(-1);
  		
  		myVector reflDir ;//= compReflDir(backToEyeDir, objRayN);		  
  		
  		//incoming angle in radians, critical angle,ray angle upon exiting material
  		//n is ratio of refraction indicies for current material/new material (n1/n2)-use n1 and n2 to denote material refraction indicies - n1 is source material, n2 is destination refraction material
  		double thetaIncident = 0, thetaCrit = 0, //thetaExit = 0, 
  				n = 1, n1 = 0, n2 = 0;
  		
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
  		if (cosTheta1 < MyMathUtils.EPS){
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
  			n1 = hit.transRay.currKTrans[0];
  			n2 = KTrans;
  			//println("  entering material : " + n2);
  			n = (n1/n2);
  			//thetaExit = Math.asin(n  * Math.sin(thetaIncident)); 
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
  			if (oneMTransReflRatio < MyMathUtils.EPS) { System.out.println("one minus tr = 1");}      
  		}        
  		//sanity check
  		if (transReflRatio > 1){ System.out.println("impossible result - treflRat, rPerp, rPar : " + transReflRatio + " | " + rPerp + " | " + rPar);}
      
  		if (oneMTransReflRatio > MyMathUtils.EPS){//if transReflRatio = 1 then no refraction
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
  	      	rayCast refrRay = new rayCast(scene, hitLoc, refractDir, hit.transRay.gen+1);  	      	
  	      	refrRay.setCurrKTrans(KTrans, currPerm, curPermClr);//need to set ktrans for the material this ray is in
  	      	return refrRay;
  		}//if refracting
			//reflecting ray off surface
		//add more than 1 for ray generation to decrease number of internal reflection rays
  		reflDir = compReflDir(backToEyeDir, N);
		reflDir._mult(refractNormMult);		//for leaving material
		rayCast reflRay = new rayCast(scene, hitLoc, reflDir, hit.transRay.gen+1);
		reflRay.setCurrKTrans(KTrans, currPerm, curPermClr);  	      	
		return reflRay;
  	}//calcTransRay()	
  	
  	//calc reflected color - simple reflection
  	protected rayCast calcReflRay(rayHit hit){
  		myPoint hitLoc = hit.fwdTransHitLoc;
  		myVector backEyeDir = new myVector(hit.fwdTransRayDir);  		
  		backEyeDir._mult(-1);
  		myVector reflDir = compReflDir(backEyeDir, hit.objNorm);	  
  			//reflecting ray off surface
  		return new rayCast(scene, hitLoc, reflDir, hit.transRay.gen+1);
   	}//calcReflClr

  	//this returns the color value at a particular point on the object, based on where the incident view ray hits it. 
  	public myRTColor getColorAtPos(rayHit hit){
  		//need to get color from photon map
  		++dbgRayHits;		
  		double r = ambientColor.x, g = ambientColor.y, b = ambientColor.z;
  		double[] phtnIrr;
  		//TODO separate caustics and indirect into 2 processes
  		if((KRefl == 0.0) && getUsePhotonMap()){
 			//r += diffuseColor.x * phtnIrr[0]; 		g += diffuseColor.y * phtnIrr[1]; 		b += diffuseColor.z * phtnIrr[2]; 
  			phtnIrr = getIrradianceFromPhtnTree(hit);
  			if(getIsCausticPtn()){//visualize photons directly for caustics
  				r += phtnIrr[0]; 		g += phtnIrr[1]; 		b += phtnIrr[2];
  			} else {					// indirect illumination effects
 				r += diffuseColor.x * phtnIrr[0]; 		g += diffuseColor.y * phtnIrr[1]; 		b += diffuseColor.z * phtnIrr[2]; 
  			}
  		}  	     		
  		//find contributions for each light
  		double[] shadowRes = calcShadowColor(hit, txtr.getDiffTxtrColor(hit, diffuseColor, diffConst));
  		r += shadowRes[0]; 		g += shadowRes[1]; 		b += shadowRes[2];
  		//now need kRefl factor - need to be careful with reflection - don't want to go further than 
  		//recursive depth of numRays - need to leave room for one more for shadows, that's why -2 not -1
  		if ((hit.transRay.gen < scene.numRays-2) && getHasCaustic()){
  			//replace with either/or for transparent or reflective			
  			double[] res = new double[]{0,0,0};
  			if ((KTrans > 0) || (currPerm > 0.0)){				res = calcTransClr(hit, curPermClr); 			}//TODO clean this up : if refraction happens (also handles reflection - splits rays)
  			else if (KRefl > 0.0){								res = calcReflClr(hit, KReflClr); 			}//if reflection happens	
  			r += res[0]; 	g += res[1]; 	b += res[2];
  		}//if enough rays to recurse and this material reflects/refracts
 	
  		return new myRTColor(r,g,b);
  	}//getcoloratpos method  	
  	
	/**
	 * find irradiance of a particular location from photon tree using neighborhood  
	 * @param hit
	 * @return
	 */
	protected double[] getIrradianceFromPhtnTree(rayHit hit){
		//idx 0 is photon dir, idx 1 is phtn pwr
		double[] res = new double[]{0,0,0};		
		PriorityQueue<myPhoton> hood = scene.photonTree.findNeighborhood(hit.fwdTransHitLoc.x, hit.fwdTransHitLoc.y, hit.fwdTransHitLoc.z);//location in world coords
		//ArrayList<Photon> hood = scene.photonTree.find_near((float)hit.fwdTransHitLoc.x, (float)hit.fwdTransHitLoc.y, (float)hit.fwdTransHitLoc.z, scene.kNhood, scene.ph_max_near_dist);//location in world coords
		int hoodSize = hood.size();
		if ((hoodSize == 0) || (hood.peek() == null)){return res;}
		double rSq = hood.peek().getSqDist();				//furthest photon is first element of hood, sqdist is distance this photon is from ray hit
		double area = MyMathUtils.PI * rSq;// * Math.sqrt(rSq) * 1.33333;//vol of differential hemi-sphere
		
		for(myPhoton phtn : hood) {
			//myPhoton phtn = hood[i];
			res[0] += phtn.pwr[0];
			res[1] += phtn.pwr[1];
			res[2] += phtn.pwr[2];
		}
		res[0] /= area;	res[1] /= area;	res[2] /= area;		
		return res;
	}//getIrradianceFromPhtnTree   	
  	
  	//call this when a caustic-generating object is hit, it will return the ray hit of the reflected/refracted ray
  	public rayCast findCausticRayHit(rayHit hit, double[] phtn_pwr){ 
  		rayCast res = null;
 		if ((hit.transRay.gen < scene.numPhotonRays) && getHasCaustic()){
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
  	
  	
  	
	/**
	 * base class flags init
	 */
	private final void initFlags(){shdrFlags = new int[1 + numFlags/32];for(int i =0; i<numFlags;++i){setFlags(i,false);}}			
	/**
	 * get baseclass flag
	 * @param idx
	 * @return
	 */
	public final boolean getFlags(int idx){int bitLoc = 1<<(idx%32);return (shdrFlags[idx/32] & bitLoc) == bitLoc;}	
	
	/**
	 * check list of flags
	 * @param idxs
	 * @return
	 */
	public final boolean getAllFlags(int [] idxs){int bitLoc; for(int idx =0;idx<idxs.length;++idx){bitLoc = 1<<(idx%32);if ((shdrFlags[idx/32] & bitLoc) != bitLoc){return false;}} return true;}
	public final boolean getAnyFlags(int [] idxs){int bitLoc; for(int idx =0;idx<idxs.length;++idx){bitLoc = 1<<(idx%32);if ((shdrFlags[idx/32] & bitLoc) == bitLoc){return true;}} return false;}
		
	public final boolean getHasCaustic() {return getFlags(hasCausticIDX);}
	public final void setHasCaustic(boolean val) {setFlags(hasCausticIDX, val);}
	public final boolean getUsePhotonMap() {return getFlags(usePhotonMapIDX);}
	public final void setUsePhotonMap(boolean val) {setFlags(usePhotonMapIDX, val);}
	public final boolean getIsCausticPtn() {return getFlags(isCausticPhtnIDX);}
	public final void setIsCausticPtn(boolean val) {setFlags(isCausticPhtnIDX, val);}
	/**
	 * set baseclass flags  //setFlags(showIDX, 
	 * @param idx
	 * @param val
	 */
	public final void setFlags(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		shdrFlags[flIDX] = (val ?  shdrFlags[flIDX] | mask : shdrFlags[flIDX] & ~mask);
		switch(idx){
			case hasCausticIDX			: {	break;}	
			case usePhotonMapIDX		: {	break;}	
			case isCausticPhtnIDX  		: {	break;}	
		}				
	}//setFlags
  	

	public String toString(){
		String result = "Shader type : " + shType +  " |\t ray hits on obj : " + dbgRayHits;
		result += "\n\tDIFFUSE : " + diffuseColor.toString() + " | AMBIENT : " + ambientColor.toString() + " | SPECULAR : " + specularColor.toString();
		result += "\n\tDiffuse const : " + diffConst + " | phongExp : " + phongExp + " | krefl : " + KRefl+ " | ktrans : " + KTrans;
		result += "\n\tcurrent 'permiability' : " + currPerm + " perm for r g b : " + curPermClr.toString()+"\n";
		return result;
	} 
}//myObjShader


