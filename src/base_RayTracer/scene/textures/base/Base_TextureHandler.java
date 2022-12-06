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
	
	private int[] txtFlags;					//various state-related flags for this object
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

	/**
	 * base class flags init
	 */
	public final void initFlags(){txtFlags = new int[1 + numFlags/32];for(int i =0; i<numFlags;++i){setFlags(i,false);}}			
	/**
	 * get baseclass flag
	 * @param idx
	 * @return
	 */
	public final boolean getFlags(int idx){int bitLoc = 1<<(idx%32);return (txtFlags[idx/32] & bitLoc) == bitLoc;}	
	
	public final boolean getHasTxtrdTop() {return getFlags(txtrdTopIDX);}
	public final boolean getHasTxtrdBtm() {return getFlags(txtrdBtmIDX);}
	
	public final void setHasTxtrdTop(boolean _val) {setFlags(txtrdTopIDX, _val);}
	public final void setHasTxtrdBtm(boolean _val) {setFlags(txtrdBtmIDX, _val);}
	
	/**
	 * check list of flags
	 * @param idxs
	 * @return
	 */
	public final boolean getAllFlags(int [] idxs){int bitLoc; for(int idx =0;idx<idxs.length;++idx){bitLoc = 1<<(idx%32);if ((txtFlags[idx/32] & bitLoc) != bitLoc){return false;}} return true;}
	public final boolean getAnyFlags(int [] idxs){int bitLoc; for(int idx =0;idx<idxs.length;++idx){bitLoc = 1<<(idx%32);if ((txtFlags[idx/32] & bitLoc) == bitLoc){return true;}} return false;}

	
	/**
	 * set texture flags  //setFlags(showIDX, 
	 * @param idx
	 * @param val
	 */
	public final void setFlags(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		txtFlags[flIDX] = (val ?  txtFlags[flIDX] | mask : txtFlags[flIDX] & ~mask);
		switch(idx){	
		case txtrdTopIDX :{break;}
		case txtrdBtmIDX :{break;}	
		}
	}
	
	protected abstract void initTextureVals();
	public abstract double[] getDiffTxtrColor(rayHit hit, myRTColor diffuseColor, double diffConst);  	
	public abstract String showUV();
	  	
	public String toString(){
		String res = "Shader Texture : ";
		return res;
	}
}//myTextureHandler