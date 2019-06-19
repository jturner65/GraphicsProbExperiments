package base_RayTracer;

import base_Utils_Objects.vectorObjs.myVector;

public class myColor {
	public myVector RGB;
	public myColor(double _r, double _g, double _b) {					RGB = new myVector( Math.min(1, _r), Math.min(1,_g), Math.min(1,_b) );	}//alpha = Math.min(1,_alpha);	}
//	public myColor(myColor _c){											RGB = new myVector(Math.min(1, _c.RGB.x), Math.min(1,_c.RGB.y), Math.min(1,_c.RGB.z)); }//alpha  = Math.min(1,_c.alpha);}
	public myColor(int _color){											this(((_color >> 16) & 0xFF)/255.0,((_color >> 8) & 0xFF)/255.0,(_color & 0xFF)/255.0);}
	public myColor(){													this(0,0,0);}
	
	//interpolate this color with passed color
	public myColor interpColor(double t, myColor B){	return new myColor((RGB.x + t*(B.RGB.x - RGB.x)), (RGB.y + t*(B.RGB.y - RGB.y)), (RGB.z + t*(B.RGB.z - RGB.z)));}
	public void set(myColor _c){										RGB.set(Math.min(1, _c.RGB.x), Math.min(1,_c.RGB.y), Math.min(1,_c.RGB.z));}// alpha  = Math.min(1,_c.alpha);}
	public void set(double _r, double _g, double _b){					RGB.set(Math.min(1, _r), Math.min(1,_g), Math.min(1,_b)); }//alpha  = 1.0;}
	
	public int getInt(){int retVal = ((int)(255) << 24) + ((int)(RGB.x * 255) << 16) + ((int)(RGB.y * 255) << 8) + (int)(RGB.z * 255);return retVal;}
	public String toString(){	String res = "Color : r : "+ RGB.x+" | g : "+RGB.y+" | b : "+RGB.z;//+" | alpha : "+alpha;	
		return res;	}
}//mycolor class
