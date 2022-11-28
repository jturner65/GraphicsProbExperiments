package base_RayTracer.scene.textures.base;

import base_RayTracer.myRTColor;
import base_RayTracer.ray.rayHit;
import base_RayTracer.scene.base.Base_Scene;
import base_RayTracer.scene.shaders.myObjShader;

/**
 * class to handle base texture functionality
 * @author 7strb
 *
 */

public abstract class Base_TextureHandler {
	public Base_Scene scene;
	public myObjShader shdr;
	
	protected static final int hashPrime1 = 1572869;
	protected static final int hashPrime2 = 6291469;
	//used for worley txtrs
	protected final int[][] nghbrHdCells = new int[][]{
		{ 0,  0,  0},{ 0,  0,  1},{ 0,  0, -1},{ 0,  1,  0},{ 0,  1,  1},{ 0,  1, -1},{ 0, -1,  0},{ 0, -1,  1},{ 0, -1, -1},
		{ 1,  0,  0},{ 1,  0,  1},{ 1,  0, -1},{ 1,  1,  0},{ 1,  1,  1},{ 1,  1, -1},{ 1, -1,  0},{ 1, -1,  1},{ 1, -1, -1},
		{-1,  0,  0},{-1,  0,  1},{-1,  0, -1},{-1,  1,  0},{-1,  1,  1},{-1,  1, -1},{-1, -1,  0},{-1, -1,  1},{-1, -1, -1}		
	};
	
	public boolean[] txtFlags;					//various state-related flags for this object
	public static final int 
			txtrdTopIDX			= 0,
			txtrdBtmIDX			= 1;			
	public static final int numFlags = 2;	

	//used as array indices
	public static final int R = 0, G = 1, B = 2;
	
	public Base_TextureHandler(Base_Scene _scn, myObjShader _shdr) {
		scene = _scn;
		shdr = _shdr;
		initFlags();		
		initTextureVals();		
	}
	protected float fastAbs(float x) {return (x <= 0.0F) ? 0.0F - x : x;}
	protected double fastAbs(double x) {return (x <= 0.0D) ? 0.0D - x : x;}
	// This method is a *lot* faster than using (int)Math.floor(x)
	protected int fastfloor(float x) {	return x>0 ? (int)x : (int)x-1;}
	protected int fastfloor(double x) { return x>0 ? (int)x : (int)x-1;}
	
	
	public void initFlags(){txtFlags = new boolean[numFlags];for(int i=0; i<numFlags;++i){txtFlags[i]=false;}}
	protected abstract void initTextureVals();
	public abstract double[] getDiffTxtrColor(rayHit hit, myRTColor diffuseColor, double diffConst);  	
	public abstract String showUV();
	  	
	public String toString(){
		String res = "Shader Texture : ";
		return res;
	}
}//myTextureHandler