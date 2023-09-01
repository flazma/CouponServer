package kr.co.smenglish.coreclone.util;

public class NullToValue {
	
	public static Object NVL(Object val1, String replace) {
		Object returnVal = null;
		if(replace == null) replace = "";
		if(val1 == null) {
			 returnVal = replace;
		} else {
			returnVal = val1;
		}
		
		return returnVal;
	}
}
