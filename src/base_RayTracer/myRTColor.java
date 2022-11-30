package base_RayTracer;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;

/**
 * Alias for a myPoint to be used for color values.  Will force values to be smaller than 1 and greater than 0
 * @author 7strb
 *
 */
public class myRTColor extends myPoint {
	public myRTColor(double _r, double _g, double _b) {	super( gate(_r), gate(_g), gate(_b) );	}//alpha = Math.min(1,_alpha);	}
	public myRTColor(myRTColor _clr){super(_clr);}
	
	public myRTColor(int _color){super(((_color >> 16) & 0xFF)/255.0,((_color >> 8) & 0xFF)/255.0,(_color & 0xFF)/255.0);}
	public myRTColor(){super(0,0,0);}
	public myRTColor(myRTColor a, double t, myRTColor b) {super(a,t,b);}
	//interpolate this color with passed color
	public myRTColor interpColor(double t, myRTColor B){				return new myRTColor(this, t, B);}
	public void set(myRTColor _c){										super.set(gate( _c.x), gate(_c.y), gate(_c.z));}// alpha  = Math.min(1,_c.alpha);}
	public void set(double _r, double _g, double _b){					super.set(gate( _r), gate(_g), gate(_b)); }//alpha  = 1.0;}
	
	
	private static double gate(double val) {return MyMathUtils.max(0, MyMathUtils.min(1.0,val));}
	
	public int getInt(){int retVal = ((int)(255) << 24) + ((int)(x * 255) << 16) + ((int)(y * 255) << 8) + (int)(z * 255);return retVal;}
	public String toString(){	String res = "Color : r : "+ x+" | g : "+y+" | b : "+z;//+" | alpha : "+alpha;	
		return res;	}
}//mycolor class
