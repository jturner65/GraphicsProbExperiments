package base_RayTracer.scene;

import java.util.concurrent.ThreadLocalRandom;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_RayTracer.myColor;
import base_RayTracer.myRay;
import base_RayTracer.rayHit;
import base_RayTracer.scene.geometry.myGeomBase;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myFOVScene extends myScene {
	//current field of view
	public double fov, fovRad, viewZ;			//degrees, radians,distance from eye the current view plane exists at, in -z direction
	//public List<Future<Boolean>> callFOVFutures;
	//public List<myFOVCall> callFOVCalcs;

	public myFOVScene(IRenderInterface _p, String _sceneName, int _numCols, int _numRows) {
		super(_p,_sceneName,_numCols,_numRows);
		//callFOVCalcs= new ArrayList<myFOVCall>();
		//callFOVFutures = new ArrayList<Future<Boolean>>(); 
	}
	public myFOVScene(myScene _scene) {
		super( _scene);
		viewZ = ((myFOVScene)(_scene)).viewZ;
		//callFOVCalcs= new ArrayList<myFOVCall>();
		//callFOVFutures = new ArrayList<Future<Boolean>>(); 
	}
	@Override
	protected void initVarsPriv() {
		viewZ = -1;		
	}
	

	@Override
	public void setSceneParams(double[] args){
		fov = args[0];
		fovRad = Math.PI*fov/180.0;
		if (fastAbs(fov - 180) < .001){//if illegal fov value, modify to prevent divide by 0
			fov -= .001;
			fovRad -= .0001;
		}
		//virtual view plane exists at some -z so that fov gives sceneCols x sceneRows x and y;-1 for negative z, makes coord system right handed
		//if depth of field scene, then use lens focal distance as z depth (??)
		viewZ = -1 *  (Math.max(sceneRows,sceneCols)/2.0)/Math.tan(fovRad/2);
		if(scFlags[hasDpthOfFldIDX]){//depth of field variables already set from reader - build focal plane
			focalPlane.setPlaneVals(0, 0, 1,  lens_focal_distance);  		
			System.out.println("View z : " + viewZ  + "\nfocal plane : "+ focalPlane);
		}
	}//setSceneParams

	//getDpthOfFldEyeLoc()
	//no anti aliasing for depth of field, instead, first find intersection of ray with focal plane, then 
	//then find start location of ray via getDpthOfFldEyeLoc(), and build multiple rays 
	protected myColor shootMultiDpthOfFldRays(double pRayX, double pRayY) {
		myColor result,aaResultColor;
		double redVal = 0, greenVal = 0, blueVal = 0;//, rayYOffset = sceneRows/2.0, rayXOffset = sceneCols/2.0;
		myVector lensCtrPoint = new myVector(pRayX,pRayY,viewZ);
		lensCtrPoint._normalize();
		myRay ray = new myRay(this, eyeOrigin, lensCtrPoint, 0);					//initial ray - find intersection with focal plane
		//find intersection point with focal plane, use this point to build lens rays
		rayHit hit = focalPlane.intersectCheck( ray, ray.getTransformedRay(ray, focalPlane.CTMara[myGeomBase.invIDX]),focalPlane.CTMara);						//should always hit
		myVector rayOrigin,														//
			focalPt = hit.hitLoc;
		for(int rayNum = 0; rayNum < numRaysPerPixel; ++rayNum){
			rayOrigin = this.getDpthOfFldEyeLoc(lensCtrPoint);										//get some random pt within the lens to use as the ray's origin
			ray = new myRay(this, rayOrigin, new myVector(rayOrigin, focalPt),0);
			aaResultColor = reflectRay(ray);
			redVal += aaResultColor.RGB.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
			greenVal += aaResultColor.RGB.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
			blueVal += aaResultColor.RGB.z;//(aaResultColor & 0xFF)/256.0;//gets blue value
		}//rayNum
		result = new myColor ( redVal/numRaysPerPixel, greenVal/numRaysPerPixel, blueVal/numRaysPerPixel); 
		return result;	  
	}//shootMultiRays	
	
	protected void drawDpthOfFld(){
		if (!scFlags[renderedIDX]){	
			initRender();
			//index of currently written pixel
			int pixIDX = 0;
			int progressCount = 0;
			double rayY, rayX;
			myColor showColor;
			//myRay ray;
			boolean skipPxl = false;
			int stepIter = 1;
			if(scFlags[glblRefineIDX]){
				stepIter = RefineIDX[curRefineStep++];
				skipPxl = curRefineStep != 1;			//skip 0,0 pxl on all sub-images except the first pass
			} 
			if(stepIter == 1){scFlags[renderedIDX] = true;			}
			for (int row = 0; row < sceneRows; row+=stepIter){
				rayY = (-1 * (row - rayYOffset));        
				for (int col = 0; col < sceneCols; col+=stepIter){
					if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl		
					rayX = col - rayXOffset;      
					showColor = shootMultiDpthOfFldRays(rayX,rayY);
					pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,rndrdImg.pixels);
					if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");progressCount++;}//progressbar  
				}//for col
			}//for row  
			System.out.println("-");
			//update the display based on the pixels array
			rndrdImg.updatePixels();
			if(scFlags[renderedIDX]){//only do this stuff when finished				
				finishImage();
			}	
		}
		finalizeDraw();
//		pa.imageMode(PConstants.CORNER);
//		pa.image(rndrdImg,0,0);	
	}//drawDpthOfFld
	
	
	@Override//calculates color based on multiple rays shot into scene
	public myColor shootMultiRays(double xBseVal, double yBseVal) {
		myColor result,aaResultColor;
		double redVal = 0, greenVal = 0, blueVal = 0, rayY, rayX;//, rayYOffset = sceneRows/2.0, rayXOffset = sceneCols/2.0;
		myRay ray;		
		for(int rayNum = 0; rayNum < numRaysPerPixel; ++rayNum){//vary by +/- .5
			rayY = yBseVal + ThreadLocalRandom.current().nextDouble(-.5,.5);
			rayX = xBseVal + ThreadLocalRandom.current().nextDouble(-.5,.5);
			ray = new myRay(this, this.eyeOrigin, new myVector(rayX,rayY,viewZ),0);
			aaResultColor = reflectRay(ray);
			redVal += aaResultColor.RGB.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
			greenVal += aaResultColor.RGB.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
			blueVal += aaResultColor.RGB.z;//(aaResultColor & 0xFF)/256.0;//gets blue value
		}//rayNum
		result = new myColor ( redVal/numRaysPerPixel, greenVal/numRaysPerPixel, blueVal/numRaysPerPixel); 
		return result;	  
	}//shootMultiRays	
//		
//	public void drawTmp (){
//		callInitBoidCalcs.clear();
//		myBoid[] tmpList;
//		for(int c = 0; c < boidFlock.length; c+=mtFrameSize){
//			int finalLen = (c+mtFrameSize < boidFlock.length ? mtFrameSize : boidFlock.length - c);
//			tmpList = new myBoid[finalLen];
//			System.arraycopy(boidFlock, c, tmpList, 0, finalLen);			
//			callInitBoidCalcs.add(new myInitPredPreyMaps(p, this, preyFlock, predFlock, fv, tmpList));
//		}							//find next turn's motion for every creature by finding total force to act on creature
//		try {callInitFutures = th_exec.invokeAll(callInitBoidCalcs);for(Future<Boolean> f: callInitFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }			
//
//		
//	}
	
	
	@Override
	//distribution draw
	public void draw(){
		if(scFlags[hasDpthOfFldIDX]){drawDpthOfFld(); return;}
		if (!scFlags[renderedIDX]){	
			initRender();
			//index of currently written pixel
			int pixIDX = 0;
			int progressCount = 0;
			double rayY, rayX;
			myColor showColor;
			//myRay ray;
			boolean skipPxl = false;
			int stepIter = 1;
			if(scFlags[glblRefineIDX]){
				stepIter = RefineIDX[curRefineStep++];
				skipPxl = curRefineStep != 1;			//skip 0,0 pxl on all sub-images except the first pass
			} 
			if(stepIter == 1){scFlags[renderedIDX] = true;			}
			if (numRaysPerPixel == 1){//only single ray shot into scene for each pixel
				for (int row = 0; row < sceneRows; row+=stepIter){
					rayY = (-1 * (row - rayYOffset));         
					for (int col = 0; col < sceneCols; col+=stepIter){
						if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl					
						rayX = col - rayXOffset;
						showColor = reflectRay(new myRay(this,this.eyeOrigin, new myVector(rayX,rayY,viewZ),0)); 
						pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,rndrdImg.pixels);
						if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");progressCount++;}//progressbar         
					}//for col
				}//for row	     
			} else{    //multiple rays shot into scene per pxl
				for (int row = 0; row < sceneRows; row+=stepIter){
					rayY = (-1 * (row - rayYOffset));        
					for (int col = 0; col < sceneCols; col+=stepIter){
						if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl		
						rayX = col - rayXOffset;      
						showColor = shootMultiRays(rayX,rayY);
						pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,rndrdImg.pixels);
						if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");progressCount++;}//progressbar  
					}//for col
				}//for row  
			}//if antialiasing
			
			System.out.println("-");
			//update the display based on the pixels array
			rndrdImg.updatePixels();
			if(scFlags[renderedIDX]){//only do this stuff when finished				
				finishImage();
			}	
		}
		finalizeDraw();
//		pa.imageMode(PConstants.CORNER);
//		pa.image(rndrdImg,0,0);		
	}

}//myFOVScene