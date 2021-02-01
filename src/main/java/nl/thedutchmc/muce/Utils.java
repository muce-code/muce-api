package nl.thedutchmc.muce;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;

public class Utils {
	
	/**
	 * Get the stacktrace from a Throwable
	 * @param t Throwable to get the stacktrace from
	 * @return Returns the stacktrace as a String
	 */
	public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
	}
	
	public static boolean isPositiveLong(String test) {
		if(test.matches("-?\\d+")) {
			BigInteger bi = new BigInteger(test);
			if(bi.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
				return false;
			}
			
			if(Long.valueOf(test) <= 0) {
				return false;
			}
			
			return true;
		} else {
			return false;
		}
	}
}
