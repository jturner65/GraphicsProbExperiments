package base_RayTracer.scene;

import java.util.concurrent.ThreadLocalRandom;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_RayTracer.myColor;
import base_RayTracer.myRay;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myFishEyeScene extends myScene{
	//current field of view
	public double fishEye, fishEyeRad;			//fisheye in degrees, radians	
	private double aperatureHlf;
	//public List<Future<Boolean>> callFishFutures;
	//public List<myFishCall> callFishCalcs;
	
	public myFishEyeScene(IRenderInterface _p, String _sceneName, int _numCols, int _numRows) {
		super(_p,_sceneName,_numCols,_numRows);	
	}
	
	public myFishEyeScene(myScene _s){
		super(_s);
	}

	@Override
	protected void initVarsPriv() {}

	@Override
	public void setSceneParams(double[] args) {
		fishEye = args[0];
		fishEyeRad = Math.PI*fishEye/180.0;
		aperatureHlf = fishEyeRad/2.0;
	}//setSceneParams

	@Override
	public myColor shootMultiRays(double xBseVal, double yBseVal) {
		myColor result,aaResultColor;
		double xVal, yVal, r, rSqTmp, theta, phi, redVal = 0, greenVal = 0, blueVal = 0;		
		myRay ray;
		for(int rayNum = 0; rayNum < numRaysPerPixel; ++rayNum){//vary by +/- .5
			yVal = (yBseVal + ThreadLocalRandom.current().nextDouble(-.5,.5)) *fishMult;
			xVal = (xBseVal + ThreadLocalRandom.current().nextDouble(-.5,.5)) *fishMult; 
			rSqTmp = yVal * yVal + xVal*xVal;
			if(rSqTmp <= 1){
				r = Math.sqrt(rSqTmp);
				theta = r * aperatureHlf;
				phi = Math.atan2(-yVal,xVal); 					
				double sTh = Math.sin(theta);
				ray = new myRay(this,this.eyeOrigin, new myVector(sTh * Math.cos(phi),sTh * Math.sin(phi),-Math.cos(theta)),0);
				aaResultColor = reflectRay(ray);
				redVal += aaResultColor.RGB.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
				greenVal += aaResultColor.RGB.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
				blueVal += aaResultColor.RGB.z;//(aaResultColor & 0xFF)/256.0;//gets blue value
			}			
		}//rayNum
		result = new myColor ( redVal/numRaysPerPixel, greenVal/numRaysPerPixel, blueVal/numRaysPerPixel); 
		return result;	  
	}//shootMultiRays
	
	@Override
	//distribution draw
	public void draw(){
		if (!scFlags[renderedIDX]){
			initRender();
			//index of currently written pixel
			int pixIDX = 0;
			int progressCount = 0;
			double r, phi, theta,yVal, xVal, ySq, sTh, rTmp;
			//fishRad2 = .5*fishEyeRad;
			myColor showColor;
			boolean skipPxl = false;
			int stepIter = 1;
			if(scFlags[glblRefineIDX]){
				stepIter = RefineIDX[curRefineStep++];
				skipPxl = curRefineStep != 1;			//skip 0,0 pxl on all sub-images except the first pass
			} 
			if(stepIter == 1){scFlags[renderedIDX] = true;			}
			//fisheye assumes plane is 1 away from eye
			if (numRaysPerPixel == 1){											//single ray into scene per pixel
				for (int row = 0; row < sceneRows; row+=stepIter){
					yVal = (row + yStart) * fishMult;
					ySq = yVal * yVal;
					for (int col = 0; col < sceneCols; col+=stepIter){
						if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl	
						xVal = (col + xStart)* fishMult;
						rTmp = xVal*xVal+ySq;
						if(rTmp > 1){	pixIDX = writePxlSpan(blkColor.getInt(),row,col,stepIter,rndrdImg.pixels);	} 
						else {
							r = Math.sqrt(rTmp);
							theta = r * aperatureHlf;
							phi = Math.atan2(-yVal,xVal); 					
							sTh = Math.sin(theta);
							showColor = reflectRay(new myRay(this,this.eyeOrigin, new myVector(sTh * Math.cos(phi),sTh * Math.sin(phi),-Math.cos(theta)),0)); 
							pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,rndrdImg.pixels);
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
						pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,rndrdImg.pixels);			
						if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");progressCount++;}//progressbar  
					}//for col
				}//for row  
			}//if antialiasing
			System.out.println("-");
			if(scFlags[renderedIDX]){			finishImage();	}	
		}		
		finalizeDraw();
//		pa.imageMode(PConstants.CORNER);
//		pa.image(rndrdImg,0,0);	
	}//draw
}//myFishEyeScene