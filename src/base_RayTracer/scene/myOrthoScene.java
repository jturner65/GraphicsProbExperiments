package base_RayTracer.scene;

import java.util.concurrent.ThreadLocalRandom;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_RayTracer.ray.rayCast;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.ui.base.Base_RayTracerWin;
import base_RayTracer.utils.myRTColor;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myOrthoScene extends Base_Scene{
	//width and height of view - for ortho projection. for perspective will be screen width and height
	public double orthoWidth, orthoHeight;	
	private double orthPerRow, orthPerCol;			//normalizers for ortho projection
	//public List<Future<Boolean>> callOrthoFutures;
	//public List<myOrthoCall> callOrthoCalcs;

	public myOrthoScene(IRenderInterface _p, Base_RayTracerWin _win, String _sceneName,int _numCols, int _numRows, double _orthoWidth, double _orthoHeight) {
		super(_p,_win,_sceneName,_numCols,_numRows);
		setOrthoVals(_orthoWidth,_orthoHeight);
	}
	public myOrthoScene(Base_Scene _s, double _orthoWidth, double _orthoHeight){
		super(_s);
		setOrthoVals(_orthoWidth,_orthoHeight);
	}
	
	/**
	 * After image size is changed, recalculate essential scene-specific values that depend on image size
	 */
	@Override
	protected final void setImageSize_Indiv() {}
	
	private void setOrthoVals(double _orthoWidth, double _orthoHeight) {
		orthoWidth = _orthoWidth;
		orthoHeight = _orthoHeight;
	
		double div = Math.min(orthoWidth, orthoHeight);
		orthPerRow = orthoHeight/div;
		orthPerCol = orthoWidth/div;		
	}
	
	@Override
	public myRTColor shootMultiRays(double xBseVal, double yBseVal) {
		myRTColor result,aaResultColor;
		double redVal = 0, greenVal = 0, blueVal = 0, rayY, rayX;//,rayYOffset = 1.0/sceneRows, rayXOffset = 1.0/sceneCols;
		//first ray can be straight in
		aaResultColor = reflectRay(new rayCast(this, new myVector(xBseVal,yBseVal,0), new myVector(0,0,-1),0));
		redVal += aaResultColor.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
		greenVal += aaResultColor.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
		blueVal += aaResultColor.z;//(aaResultColor & 0xFF)/256.0;//gets blue value	      
		
		for(int rayNum = 1; rayNum < numRaysPerPixel; ++rayNum){//vary by +/- .5
			rayY = yBseVal + (orthPerRow*ThreadLocalRandom.current().nextDouble(-.5,.5));
			rayX = xBseVal + (orthPerCol*ThreadLocalRandom.current().nextDouble(-.5,.5));				
			aaResultColor = reflectRay(new rayCast(this, new myVector(rayX,rayY,0), new myVector(0,0,-1),0));
			redVal += aaResultColor.x; //(aaResultColor >> 16 & 0xFF)/256.0;//gets red value
			greenVal += aaResultColor.y; // (aaResultColor >> 8 & 0xFF)/256.0;//gets green value
			blueVal += aaResultColor.z;//(aaResultColor & 0xFF)/256.0;//gets blue value	      
		}//aaliasR
		result = new myRTColor ( redVal/numRaysPerPixel, greenVal/numRaysPerPixel, blueVal/numRaysPerPixel); 
		return result;
	}//shootMultiRays	
	
	@Override
	public void renderScene(int stepIter, boolean skipPxl, int[] pixels){
		//we must shoot out rays and determine what is being hit by them
		//get pixels array, to be modified by ray-shooting
		//index of currently written pixel
		int pixIDX = 0;
		int progressCount = 0;
		myRTColor showColor;
		double rayY, rayX;

		if (numRaysPerPixel == 1){											//single ray into scene per pixel
			for (int row = 0; row < sceneRows; row+=stepIter){
				rayY = orthPerRow * (-1 * (row - rayYOffset));         
				for (int col = 0; col < sceneCols; col+=stepIter){
					if(skipPxl){skipPxl = false;continue;}			//skip only 0,0 pxl					
					rayX = orthPerCol * (col - rayXOffset);
					showColor = reflectRay(new rayCast(this,new myVector(rayX,rayY,0), new myVector(0,0,-1),0)); 
					pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,pixels);
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
					pixIDX = writePxlSpan(showColor.getInt(),row,col,stepIter,pixels);
					if ((1.0 * pixIDX)/(numPxls) > (progressCount * .02)){System.out.print("-|");progressCount++;}//progressbar  
				}//for col
			}//for row  
		}//if antialiasing			
		System.out.println("-");
	
	}//draw	

}//myOrthoScene