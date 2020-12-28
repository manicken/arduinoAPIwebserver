
package com.manicken;

public class Helper {
    public static String GetSubStringBetween(String in, String startToken, String endToken)
	{
		int beginIndex = in.indexOf("(");
		if (beginIndex == -1) { return null; }
		int endIndex = in.indexOf(")");
		if (endIndex == -1) { return null; }
		return in.substring(beginIndex+1, endIndex);
	}
}
