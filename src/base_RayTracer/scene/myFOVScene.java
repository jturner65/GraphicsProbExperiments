package base_RayTracer.scene;

import java.util.concurrent.ThreadLocalRandom;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_RayTracer.ray.rayCast;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.geometry.base.Base_Geometry;
import base_RayTracer.ui.base.Base_RayTracerWin;
import base_RayTracer.utils.myRTColor;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myFOVScene extends Base_Scene {
	//current field of view
	public double fov, fovRad, viewZ;			//degrees, radians,distance from eye the current view plane exists at, in -z direction
	//public List<Future<Boolean>> callFOVFutures;
	//public List<myFOVCall> callFOVCalcs;

	public myFOVScene(IRenderInterface _p,Base_RayTracerWin _win, String _sceneName, int _numCols, int _numRows, double _fov) {
		super(_p,_win,_sceneName,_numCols,_numRows);
		setFOVVals(_fov);
		//callFOVCalcs= new ArrayList<myFOVCall>();
		//callFOVFutures = new ArrayList<Future<Boolean>>(); 
	}
	public myFOVScene(Base_Scene _scene, double _fov) {
		super( _scene);
		setFOVVals(_fov);
		//callFOVCalcs= new ArrayList<myFOVCall>();
		//callFOVFutures = new ArrayList<Future<Boolean>>(); 
	}

	/**
	 * After image size is changed, recalculate essential scene-specific values that depend on image size
	 */
	@Override
	protected final void setImageSize_Indiv() {
		//Re calc FOV since size has changed
		setFOVVals(fov);
	}
	
	private void setFOVVals(double _fov) {
		fov = _fov;
		fovRad = MyMathUtils.DEG_TO_RAD*fov;
		if (Math.abs(fov - 180) < MyMathUtils.EPS){//if illegal fov value, modify to prevent divide by 0
			fov -= .001;
			fovRad -= .0001;
		}
		//virtual view plane exists at some -z so that fov gives sceneCols x sceneRows x and y;-1 for negative z, makes coord system right handed
		//if depth of field scene, then use lens focal distance as z depth (??)
		viewZ = -0.5 * (MyMathUtils.max(sceneRows,sceneCols)/Math.tan(fovRad/2.0f));
		if(hasDpthOfFld()){//depth of field variables already set from reader - build focal plane
			focalPlane.setPlaneVals(0, 0, 1,  lens_focal_distance, 1.0);  		
			System.out.println("View z : " + viewZ  + "\nfocal plane : "+ focalPlane);
		}		
	}

	//getDpthOfFldEyeLoc()
	//no anti aliasing for depth of field, instead, first find intersection of ray with focal plane, then 
	//then find start location of ray via getDpthOfFldEyeLoc(), and build multiple rays 
	protected myRTColor shootMultiDpthOfFldRays(double pRayX, double pRayY) {
		myRTColor result,aaResultColor;
		double redVal = 0, greenVal = 0, blueVal = 0;//, rayYOffset = sceneRows/2.0, rayXOffset = sceneCols/2.0;
		myVector lensCtrPoint = new myVector(pRayX,pRayY,viewZ);
		lensCtrPoint._normalize();
		rayCast ray = new rayCast(this, eyeOrigin, lensCtrPoint, 0);					//initial ray - find intersection with focal plane
		//find intersection point with focal plane, use this point to build lens rays
		rayHit hit = focalPlane.intersectCheck( ray, ray.getTransformedRay(focalPlane.CTMara[Base_Geometry.invIDX]),focalPlane.CTMara);						//should always hit
		myPoint rayOrigin;
		myPoint focalPt = hit.hitLoc;
		for(int rayNum = 0; rayNum < numRaysPerPixel; ++rayNum){
			rayOrigin = this.getDpthOfFldEyeLoc(lensCtrPoint);										//get some random pt within the lens to use as the ray's origin
			ray = new rayCast(this, rayOrigin, new myVector(rayOrigin, focalPt),0);
			aaResultColor = reflectRay(ray);
			redVal += aaResultColor.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
			greenVal += aaResultColor.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
			blueVal += aaResultColor.z;//(aaResultColor & 0xFF)/256.0;//gets blue value
		}//rayNum
		result = new myRTColor ( redVal/numRaysPerPixel, greenVal/numRaysPerPixel, blueVal/numRaysPerPixel); 
		return result;	  
	}//shootMultiRays	
	
	private void renderDepthOfField(int stepIter, boolean skipPxl, int[] pixels){
		//index of currently written pixel
		int pixIDX = 0;
		int progressCount = 0;
		double rayY, rayX;
		myRTColor showColor;

		//always uses multi rays in depthOfFld image
		for (int row = 0; row < sceneRows; row+=stepIter){
			rayY = (-1 * (row - rayYOffset));        
			for (int col = 0; col < sceneCols; col+=stepIter){
				if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl		
				rayX = col - rayXOffset;      
				showColor = shootMultiDpthOfFldRays(rayX,rayY);
				pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,pixels);
				progressCount = dispProgressBar(pixIDX, progressCount);//if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");++progressCount;}//progressbar  
			}//for col
		}//for row  
	}//drawDpthOfFld
	
	
	@Override//calculates color based on multiple rays shot into scene
	public myRTColor shootMultiRays(double xBseVal, double yBseVal) {
		myRTColor result,aaResultColor;
		double redVal = 0, greenVal = 0, blueVal = 0, rayY, rayX;//, rayYOffset = sceneRows/2.0, rayXOffset = sceneCols/2.0;
		//first ray can be straight in
		aaResultColor = reflectRay(new rayCast(this, this.eyeOrigin, new myVector(xBseVal,yBseVal,viewZ),0));
		redVal += aaResultColor.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
		greenVal += aaResultColor.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
		blueVal += aaResultColor.z;//(aaResultColor & 0xFF)/256.0;//gets blue value
	
		for(int rayNum = 1; rayNum < numRaysPerPixel; ++rayNum){//vary by +/- .5
			rayY = yBseVal + ThreadLocalRandom.current().nextDouble(-.5,.5);
			rayX = xBseVal + ThreadLocalRandom.current().nextDouble(-.5,.5);
			aaResultColor = reflectRay(new rayCast(this, this.eyeOrigin, new myVector(rayX,rayY,viewZ),0));
			redVal += aaResultColor.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
			greenVal += aaResultColor.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
			blueVal += aaResultColor.z;//(aaResultColor & 0xFF)/256.0;//gets blue value
		}//rayNum
		result = new myRTColor ( redVal/numRaysPerPixel, greenVal/numRaysPerPixel, blueVal/numRaysPerPixel); 
		return result;	  
	}//shootMultiRays	
	
	private void renderFOVScene(int stepIter, boolean skipPxl, int[] pixels) {
		//index of currently written pixel
		int pixIDX = 0;
		int progressCount = 0;
		myRTColor showColor;

		double rayY, rayX;
		if (numRaysPerPixel == 1){//only single ray shot into scene for each pixel
			for (int row = 0; row < sceneRows; row+=stepIter){
				rayY = (-1 * (row - rayYOffset));         
				for (int col = 0; col < sceneCols; col+=stepIter){
					if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl					
					rayX = col - rayXOffset;
					showColor = reflectRay(new rayCast(this,this.eyeOrigin, new myVector(rayX,rayY,viewZ),0)); 
					pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,pixels);
					progressCount = dispProgressBar(pixIDX, progressCount);//if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");++progressCount;}//progressbar         
				}//for col
			}//for row	     
		} else{    //multiple rays shot into scene per pxl
			for (int row = 0; row < sceneRows; row+=stepIter){
				rayY = (-1 * (row - rayYOffset));        
				for (int col = 0; col < sceneCols; col+=stepIter){
					if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl		
					rayX = col - rayXOffset;      
					showColor = shootMultiRays(rayX,rayY);
					pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,pixels);
					progressCount = dispProgressBar(pixIDX, progressCount);//if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");++progressCount;}//progressbar  
				}//for col
			}//for row  
		}//if antialiasing
	}//renderFOVScene
	
	@Override
	//distribution render
	public void renderScene(int stepIter, boolean skipPxl, int[] pixels){
		if(hasDpthOfFld()){
			renderDepthOfField(stepIter, skipPxl, pixels); 
		} else {
			renderFOVScene(stepIter, skipPxl, pixels);
		}
		System.out.println("-");
	}//renderScene

}//myFOVScene