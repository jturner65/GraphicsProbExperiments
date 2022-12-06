package base_RayTracer.scene;

import java.util.concurrent.ThreadLocalRandom;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_RayTracer.ray.rayCast;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.ui.base.Base_RayTracerWin;
import base_RayTracer.utils.myRTColor;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myFishEyeScene extends Base_Scene{
	//current field of view
	public double fishEye, fishEyeRad;			//fisheye in degrees, radians	
	private double aperatureHlf;
	//public List<Future<Boolean>> callFishFutures;
	//public List<myFishCall> callFishCalcs;
	
	public myFishEyeScene(IRenderInterface _p, Base_RayTracerWin _win, String _sceneName, int _numCols, int _numRows, double _fishEye) {
		super(_p,_win,_sceneName,_numCols,_numRows);	
		setFishEye(_fishEye);
	}
	
	public myFishEyeScene(Base_Scene _s, double _fishEye){
		super(_s);
		setFishEye(_fishEye);
	}
	
	/**
	 * After image size is changed, recalculate essential scene-specific values that depend on image size
	 */
	@Override
	protected final void setImageSize_Indiv() {}
	
	private void setFishEye(double _fishEye) {
		fishEye = _fishEye;
		fishEyeRad = MyMathUtils.DEG_TO_RAD*fishEye;
		aperatureHlf = fishEyeRad/2.0;		
	}
	
	private myRTColor getColorFromRayCast(double xVal, double yVal, double rSqTmp) {
		double theta = Math.sqrt(rSqTmp) * aperatureHlf;
		double phi = Math.atan2(-yVal,xVal); 					
		double sTh = Math.sin(theta);
		rayCast ray = new rayCast(this,this.eyeOrigin, new myVector(sTh * Math.cos(phi),sTh * Math.sin(phi),-Math.cos(theta)),0);
		return reflectRay(ray);
	}


	@Override
	public myRTColor shootMultiRays(double xBseVal, double yBseVal) {
		myRTColor result,aaResultColor;
		double xVal, yVal, rSqTmp, redVal = 0, greenVal = 0, blueVal = 0;
		//first ray can be straight in
		yVal = yBseVal *fishMult;
		xVal = xBseVal *fishMult; 
		rSqTmp = yVal*yVal + xVal*xVal;
		if(rSqTmp <= 1){
			aaResultColor = getColorFromRayCast(xVal, yVal, rSqTmp);
			redVal += aaResultColor.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
			greenVal += aaResultColor.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
			blueVal += aaResultColor.z;//(aaResultColor & 0xFF)/256.0;//gets blue value
		}
		for(int rayNum = 1; rayNum < numRaysPerPixel; ++rayNum){//vary by +/- .5
			yVal = (yBseVal + ThreadLocalRandom.current().nextDouble(-.5,.5)) *fishMult;
			xVal = (xBseVal + ThreadLocalRandom.current().nextDouble(-.5,.5)) *fishMult; 
			rSqTmp = yVal*yVal + xVal*xVal;
			if(rSqTmp <= 1){
				aaResultColor = getColorFromRayCast(xVal, yVal, rSqTmp);
				redVal += aaResultColor.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
				greenVal += aaResultColor.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
				blueVal += aaResultColor.z;//(aaResultColor & 0xFF)/256.0;//gets blue value
			}			
		}//rayNum
		result = new myRTColor ( redVal/numRaysPerPixel, greenVal/numRaysPerPixel, blueVal/numRaysPerPixel); 
		return result;	  
	}//shootMultiRays
	
	@Override
	//distribution draw
	public void renderScene(int stepIter, boolean skipPxl, int[] pixels){
		//index of currently written pixel
		int pixIDX = 0;
		int progressCount = 0;
		myRTColor showColor;
		//fisheye assumes plane is 1 away from eye
		double yVal, xVal, ySq, rTmp;
		if (numRaysPerPixel == 1){											//single ray into scene per pixel
			for (int row = 0; row < sceneRows; row+=stepIter){
				yVal = (row + yStart) * fishMult;
				ySq = yVal * yVal;
				for (int col = 0; col < sceneCols; col+=stepIter){
					if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl	
					xVal = (col + xStart)* fishMult;
					rTmp = xVal*xVal+ySq;
					if(rTmp > 1){	pixIDX = writePxlSpan(blkColor.getInt(),row,col,stepIter,pixels);	} 
					else {
						showColor = getColorFromRayCast(xVal, yVal, rTmp);
						pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,pixels);
					}
					if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");progressCount++;}//progressbar  
				}//for col
			}//for row	     
		} else{    
			for (int row = 0; row < sceneRows; row+=stepIter){
				yVal = (row + yStart);
				for (int col = 0; col < sceneCols; col+=stepIter){
					if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl		
					xVal = (col + xStart);
					showColor = shootMultiRays(xVal, yVal); 			//replace by base radian amt of max(x,y) 
					pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,pixels);			
					if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");progressCount++;}//progressbar  
				}//for col
			}//for row  
		}//if antialiasing
		System.out.println("-");
	}//renderScene
}//myFishEyeScene