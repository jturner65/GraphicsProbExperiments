package base_RayTracer.scene.textures.imageTextures;

import base_RayTracer.myColor;
import base_RayTracer.rayHit;
import base_RayTracer.scene.myScene;
import base_RayTracer.scene.shaders.myObjShader;
import base_RayTracer.scene.textures.base.Base_TextureHandler;
import processing.core.PImage;

public class myImageTexture extends Base_TextureHandler{
	//the image to be used as a texture to cover this object
	public PImage myTextureTop, myTextureBottom;

	public myImageTexture(myScene _scn, myObjShader _shdr) {
		super(_scn,_shdr);
	}
	//initialize constants used to store extremal texture map conversion coords
	@Override
	protected void initTextureVals(){
		txtFlags[txtrdTopIDX] = scene.scFlags[myScene.glblTxtrdTopIDX];
		txtFlags[txtrdBtmIDX] =  scene.scFlags[myScene.glblTxtrdBtmIDX];
	    if (txtFlags[txtrdTopIDX]){
	    	myTextureTop = scene.currTextureTop;
	    	myTextureTop.loadPixels();		    } 
	    else {					       myTextureTop = null;	    }
	    
	    if (txtFlags[txtrdBtmIDX]){      	
	    	myTextureBottom =  scene.currTextureBottom;		
	    	myTextureBottom.loadPixels();	     } 
	    else {	       					myTextureBottom = null;	    }
  	}
  	/**
  	 * interpolated via UV
  	 * @param hit
  	 * @param myTexture
  	 * @return
  	 */
	protected double[] getTextureColor(rayHit hit, PImage myTexture){
  		double [] texColor = new double[3];
  		
  		double[] tCoords = hit.obj.findTxtrCoords(hit.hitLoc, myTexture, hit.transRay.getTime());
  		double v = tCoords[1],u = tCoords[0];
  		//texColorRC - R is row(v), C is column(u)
  		
  		int uInt = (int)u, vInt = (int)v,
  		idx00 = vInt * myTexture.width + uInt, idx10 = idx00 + myTexture.width,idx01 = idx00 + 1,idx11 = idx10 + 1;
  		myColor texColorVal00 = new myColor(myTexture.pixels[idx00]), texColorVal10 = new myColor(myTexture.pixels[idx10]), 
  				texColorVal01 = new myColor(myTexture.pixels[idx01]), texColorVal11 = new myColor(myTexture.pixels[idx11]);
  	  
  		double uMFlU = u - uInt, vMFlV = v - vInt;  		
 		myColor texColorVal0 = texColorVal00.interpColor(uMFlU, texColorVal01), texColorVal1 = texColorVal10.interpColor(uMFlU,texColorVal11),texColorVal = texColorVal0.interpColor(vMFlV, texColorVal1);
 		
  		texColor[R] = texColorVal.RGB.x;
  		texColor[G] = texColorVal.RGB.y;
  		texColor[B] = texColorVal.RGB.z;   
  		return texColor;   
  	}//get texture color at pixel
  	
  	public double [] getDiffTxtrColor(rayHit hit, myColor diffuseColor, double diffConst){
  		double[] texTopColor = {0, 0 ,0}, texBotColor = {0, 0, 0};	
		if (txtFlags[txtrdBtmIDX]){//get color information from texture on b of object at specific point of intersection
			texBotColor = getTextureColor(hit,myTextureBottom);
			texBotColor[R] *= diffConst;texBotColor[G] *= diffConst;texBotColor[B] *= diffConst;    
			return texBotColor;
		}   		
  		//get color information from texture on top of object at specific point of intersection
		if (txtFlags[txtrdTopIDX]){	texTopColor = getTextureColor(hit,myTextureTop);	} 
		//add if checking for texturedBottom
		else {						texTopColor[R] = diffuseColor.RGB.x;texTopColor[G] = diffuseColor.RGB.y;texTopColor[B] = diffuseColor.RGB.z;}
		//decreasing diffuse color by this constant, reflecting how transparent the object is - only use with complex refraction calc
		texTopColor[R] *= diffConst;texTopColor[G] *= diffConst;texTopColor[B] *= diffConst;
		return texTopColor;  	
  	}//getDiffTxtrColor
  	
	public void setMyTextureTop(PImage myTexture){    myTextureTop = myTexture;      myTextureTop.loadPixels();  }
	public void setMyTextureBottom(PImage myTexture){    myTextureBottom = myTexture;      myTextureBottom.loadPixels();  }  
	
	public String showUV(){
		String result = "";
		if (myTextureTop != null){		result += " | texture w,h :" +  myTextureTop.width + ", " + myTextureTop.height;	}
		if (myTextureBottom != null){		result += " | texture (bottom) w,h :" +  myTextureBottom.width + ", " + myTextureBottom.height;	}
		return result;
	}//showUV
	public String toString(){
		String result = "";
		if (myTextureTop != null){		result += " | texture w,h :" +  myTextureTop.width + ", " + myTextureTop.height;	}
		if (myTextureBottom != null){		result += " | texture (bottom) w,h :" +  myTextureBottom.width + ", " + myTextureBottom.height;	}
		return result;
	}//toString

	
}//myImageTexture