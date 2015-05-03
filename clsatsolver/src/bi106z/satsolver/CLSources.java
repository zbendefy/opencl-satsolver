package bi106z.satsolver;

import java.io.InputStream;

class CLSources {

	static String separator = System.getProperty("line.separator");

	private static int stackDepth = 48;
	private static boolean unrollLoops = true;

	public static int getStackDepth() {
		return stackDepth;
	}

	static void setStackDepth(int stackDepth) {
		CLSources.stackDepth = stackDepth;
	}

	public static boolean isUnrollLoops() {
		return unrollLoops;
	}

	public static void setUnrollLoops(boolean unrollLoops) {
		CLSources.unrollLoops = unrollLoops;
	}

	private static String readLocalFile(String fname) {
		InputStream input = CLSources.class.getResourceAsStream(fname);

		java.util.Scanner s = new java.util.Scanner(input);
		s.useDelimiter("\\A");
		String ret = s.hasNext() ? s.next() : "";
		s.close();
		return ret;
	}

	static String getMultiSolveKernel() {

		String source = readLocalFile("/clsrc/sat_multi.cl");
		source = source.replace("%stacksize%", String.valueOf(stackDepth))
				.replace("%unroll_directive%",
						unrollLoops ? "#pragma unroll 8" : "");

		return source;
	}

	static String getSimpleSolveKernel() {
		String source = readLocalFile("/clsrc/sat_simple.cl");
		source = source.replace("%stacksize%", String.valueOf(stackDepth));

		return source;
	}
	
	static String getDynamicSolveKernel() {
		String source = readLocalFile("/clsrc/sat_dynamic.cl");
		source = source.replace("%stacksize%", String.valueOf(stackDepth));

		return source;
	}
}
