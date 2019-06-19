package base_RayTracer.scene;

import java.util.concurrent.ThreadLocalRandom;

import base_RayTracer.myColor;
import base_RayTracer.myRay;
import base_UI_Objects.my_procApplet;
import base_Utils_Objects.vectorObjs.myVector;
import processing.core.PConstants;

public class myOrthoScene extends myScene{
	//width and height of view - for ortho projection. for perspective will be screen width and height
	public double orthoWidth, orthoHeight;	
	private double orthPerRow, orthPerCol;			//normalizers for ortho projection
	//public List<Future<Boolean>> callOrthoFutures;
	//public List<myOrthoCall> callOrthoCalcs;

	public myOrthoScene(my_procApplet _p, String _sceneName,int _numCols, int _numRows) {
		super(_p,_sceneName,_numCols,_numRows);
	}
	public myOrthoScene(myScene _s, int _numCols, int _numRows){
		super(_s);
	}
	@Override
	protected void initVarsPriv() {
		orthoWidth = sceneCols;
		orthoHeight = sceneRows;
		setOrthoPerRow();
	}//initVarsPriv()	

	@Override
	public void setSceneParams(double[] args) {
		orthoWidth = args[0];
		orthoHeight = args[1];	
		setOrthoPerRow();
	}//setSceneParams
	
	private void setOrthoPerRow() {
		double div = Math.min(orthoWidth, orthoHeight);
		orthPerRow = orthoHeight/div;
		orthPerCol = orthoWidth/div;		
	}
	
	@Override
	public myColor shootMultiRays(double xBseVal, double yBseVal) {
		myColor result,aaResultColor;
		double redVal = 0, greenVal = 0, blueVal = 0, rayY, rayX;//,rayYOffset = 1.0/sceneRows, rayXOffset = 1.0/sceneCols;
		myRay ray;
		for(int rayNum = 0; rayNum < numRaysPerPixel; ++rayNum){//vary by +/- .5
			rayY = yBseVal + (orthPerRow*ThreadLocalRandom.current().nextDouble(-.5,.5));
			rayX = xBseVal + (orthPerCol*ThreadLocalRandom.current().nextDouble(-.5,.5));				
			aaResultColor = reflectRay(new myRay(this, new myVector(rayX,rayY,0), new myVector(0,0,-1),0));
			redVal += aaResultColor.RGB.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
			greenVal += aaResultColor.RGB.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
			blueVal += aaResultColor.RGB.z;//(aaResultColor & 0xFF)/256.0;//gets blue value	      
		}//aaliasR
		result = new myColor ( redVal/numRaysPerPixel, greenVal/numRaysPerPixel, blueVal/numRaysPerPixel); 
		return result;
	}//shootMultiRays	
	
	@Override
	public void draw(){
		if (!scFlags[renderedIDX]){	
			initRender();
			//we must shoot out rays and determine what is being hit by them
			//get pixels array, to be modified by ray-shooting
			//index of currently written pixel
			int pixIDX = 0;
			int progressCount = 0;
			//  double redVal, greenVal, blueVal, divVal;
			double rayY, rayX;
			double rayYOffset = sceneRows/2.0, rayXOffset = sceneCols/2.0;
			myColor showColor;
			boolean skipPxl = false;
			int stepIter = 1;
			if(scFlags[glblRefineIDX]){
				stepIter = RefineIDX[curRefineStep++];
				skipPxl = curRefineStep != 1;			//skip 0,0 pxl on all sub-images except the first pass
			} 
			if(stepIter == 1){scFlags[renderedIDX] = true;			}
			if (numRaysPerPixel == 1){											//single ray into scene per pixel
				for (int row = 0; row < sceneRows; row+=stepIter){
					rayY = orthPerRow * (-1 * (row - rayYOffset));         
					for (int col = 0; col < sceneCols; col+=stepIter){
						if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl					
						rayX = orthPerCol * (col - rayXOffset);
						showColor = reflectRay(new myRay(this,new myVector(rayX,rayY,0), new myVector(0,0,-1),0)); 
						pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,rndrdImg.pixels);
						if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");progressCount++;}//progressbar         
					}//for col
				}//for row	     
			} else{    //anti aliasing
				for (int row = 0; row < sceneRows; row+=stepIter){
					rayY = orthPerRow * ((-1 * (row - rayYOffset)) - .5);         
					for (int col = 0; col < sceneCols; col+=stepIter){
						if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl		
						rayX = orthPerCol * (col - rayXOffset - .5);      
						showColor = shootMultiRays(rayX,rayY); 
						pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,rndrdImg.pixels);
						if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");progressCount++;}//progressbar  
					}//for col
				}//for row  
			}//if antialiasing			
			System.out.println("-");
			//update the display based on the pixels array
			rndrdImg.updatePixels();
			if(scFlags[renderedIDX]){	finishImage();	}	
		}
		pa.imageMode(PConstants.CORNER);
		pa.image(rndrdImg,0,0);			
	}//draw	

}//myOrthoScene