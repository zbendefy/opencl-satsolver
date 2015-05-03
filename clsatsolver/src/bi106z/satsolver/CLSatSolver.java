package bi106z.satsolver;

import static org.jocl.CL.CL_DEVICE_NAME;
import static org.jocl.CL.CL_DEVICE_VENDOR;
import static org.jocl.CL.CL_DEVICE_VERSION;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_WRITE_ONLY;
import static org.jocl.CL.CL_SUCCESS;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clSetKernelArg;

import java.util.ArrayList;
import java.util.List;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_mem;

import clframework.common.CLContext;
import clframework.common.CLDevice;
import clframework.common.CLKernel;
import clframework.common.CLUtils;

public class CLSatSolver {

	private static final boolean VERBOSE = false;
	private static final boolean BENCHMARK = false;
	private static int sleepBetweenExecution = 10;

	private CLContext clContext = null;
	private CLKernel clKernel = null;
	private cl_mem memObjects[] = null;
	private int platformid, deviceid;

	public static void setSolverStackDepth(int depth) {
		CLSources.setStackDepth(depth);
	}

	public CLSatSolver() throws Exception {
		this.platformid = 0;
		this.deviceid = 0;
	}

	public CLSatSolver(int platformid, int deviceid) throws Exception {
		this.platformid = platformid;
		this.deviceid = deviceid;
	}

	public CLSatSolver(CLDevice clDevice) {
		this.platformid = clDevice.getPlatformid();
		this.deviceid = clDevice.getDeviceid();
	}

	public String getComputeDeviceName() {
		return CLUtils
				.GetCLStringProperty(platformid, deviceid, CL_DEVICE_NAME)
				.trim();
	}

	public String getComputeDeviceVendor() {
		return CLUtils.GetCLStringProperty(platformid, deviceid,
				CL_DEVICE_VENDOR).trim();
	}

	public String getComputeDeviceOpenCLVersion() {
		return CLUtils.GetCLStringProperty(platformid, deviceid,
				CL_DEVICE_VERSION).trim();
	}

	public SolverResult runSolver(SolverInput input) throws Exception {
		if (input.getLiterals().size() == 0) {
			return new SolverResult("Problem was empty", input,
					new ArrayList<List<Boolean>>(), 0);
		}

		if (input.getLiterals().size() >= 9) {
			return runInfSolver(input);
		} else {
			return runSingleSolver(input);
		}
	}

	private SolverResult runInfSolver(SolverInput input) throws Exception {

		if (input.getLiterals().size() < 3) {
			throw new Exception("Too few variables! (Min 3 required)");
		}

		if (input.getLiterals().size() > 61) {
			throw new Exception("Too much variables! (Max 61 allowed)");
		}

		if (clContext == null) {
			clContext = CLContext.createContext(platformid, deviceid);
		}

		memObjects = new cl_mem[3];

		clKernel = new CLKernel(clContext, "solverMain",
				new String[] { CLSources.getMultiSolveKernel() },
				"-cl-fast-relaxed-math -cl-mad-enable");

		List<List<Boolean>> allSolutions = new ArrayList<List<Boolean>>();
		
		long solutionsize = (long) Math.pow(2, input.getLiterals().size()) / 8;
		long calculationsLeft = solutionsize;
		long maxThreads = 1;
		for (int i = 0; i < clContext.getWorkItemDimensions().length; i++) {
			maxThreads *= clContext.getWorkItemDimensions()[i];
		}

		if (VERBOSE) {
			System.out.println("     Solution size: " + solutionsize
					+ "   Literals: " + input.getLiterals().size());
		}

		long beginTime = System.currentTimeMillis();

		while (calculationsLeft > 0) {

			long localSolutionSize = Math.min(429496296, Math.min(maxThreads, calculationsLeft));

			byte dstArray[] = new byte[(int) localSolutionSize];
			Pointer dst = Pointer.to(dstArray);
			
			long sizeTmp = localSolutionSize;
			int dimensions = 1;
			long[] work_item_sizes = new long[clContext.getWorkItemDimensions().length];
			for (int i = 0; i < work_item_sizes.length; i++) {
				if (sizeTmp > clContext.getWorkItemDimensions()[i]) {
					work_item_sizes[i] = clContext.getWorkItemDimensions()[i];
					sizeTmp /= clContext.getWorkItemDimensions()[i];
					if (sizeTmp > 1)
						dimensions++;
					else
						break;
				} else {
					work_item_sizes[i] = sizeTmp;
					break;
				}
			}

			if (VERBOSE) {
				String wis = "[";
				for (int i = 0; i < work_item_sizes.length; i++) {
					wis += work_item_sizes[i] + " ";
				}
				wis += "]";
				System.out.println("Dimensions: " + dimensions
						+ "   Work item sizes: " + wis + "     Solution size: "
						+ solutionsize + "   Literals: "
						+ input.getLiterals().size()
						+ "    Calculations left: " + calculationsLeft);
			}

			int[] treearray = new int[input.getTree().size() + 2];
			treearray[0] = input.getTree().size();
			treearray[1] = input.getLiterals().size() - 1;
			for (int i = 2; i < treearray.length; i++) {
				treearray[i] = input.getTree().get(i - 2);
			}

			Pointer srcB = Pointer.to(treearray);
			long[] offset = new long[] { solutionsize - calculationsLeft };
			Pointer srcLong = Pointer.to(offset);

			memObjects[0] = clCreateBuffer(clContext.getContext(), CL_MEM_READ_ONLY
					| CL_MEM_COPY_HOST_PTR, Sizeof.cl_long * 1, srcLong, null);
			memObjects[1] = clCreateBuffer(clContext.getContext(), CL_MEM_READ_ONLY
					| CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * treearray.length,
					srcB, null);
			memObjects[2] = clCreateBuffer(clContext.getContext(),
					CL_MEM_WRITE_ONLY, Sizeof.cl_char * localSolutionSize,
					null, null);

			for (int i = 0; i < memObjects.length; i++) {
				clSetKernelArg(clKernel.getKernel(), i, Sizeof.cl_mem,
						Pointer.to(memObjects[i]));
			}

			//long[] local_size = new long[] {8,8};
			clEnqueueNDRangeKernel(clContext.getCommandQueue(), clKernel.getKernel(),
					dimensions, null, work_item_sizes, null, 0, null, null);

			int readResult = clEnqueueReadBuffer(clContext.getCommandQueue(),
					memObjects[2], CL_TRUE, 0, Sizeof.cl_char
							* localSolutionSize, dst,
					0, null, null);

			if (readResult != CL_SUCCESS && VERBOSE) {
				System.out.println("Error reading buffer! " + readResult);
			}
			
			
			for (int i = 0; i < dstArray.length; i++) {
				if (dstArray[i] == 0)
					continue;
				for (int k = 0; k < 8; k++) {

					byte result = (byte) ((dstArray[i] >> k) & 1);
					long threadId = ((i + offset[0]) * 8 + k);
					if (result == 1) {
						List<Boolean> solution = new ArrayList<Boolean>();
						for (int j = 0; j < input.getLiterals().size(); j++) {
							boolean clauseValue = (((threadId >> (input
									.getLiterals().size() - 1 - j)) & 1) == 1);
							solution.add(clauseValue);
						}
						allSolutions.add(solution);
					}
				}

			}
			

			calculationsLeft -= localSolutionSize;

			for (int i = 0; i < memObjects.length; i++) {
				clReleaseMemObject(memObjects[i]);
			}

			if (!BENCHMARK && sleepBetweenExecution > 0) {
				Thread.sleep(sleepBetweenExecution);
			}
		}

		long completionTime = System.currentTimeMillis() - beginTime;

		if (BENCHMARK) {
			System.out.println("Completed in "
					+ (System.currentTimeMillis() - beginTime)
					+ " milliseconds!");
		}

		String message = "Total solutions: " + allSolutions.size();

		return new SolverResult(message, input, allSolutions, completionTime);
	}

	private SolverResult runSingleSolver(SolverInput input) throws Exception {

		if (clContext == null) {
			clContext = CLContext.createContext(platformid, deviceid);
		}

		memObjects = new cl_mem[2];
		clKernel = new CLKernel(clContext, "solverMain",
				new String[] { CLSources.getSimpleSolveKernel() },
				"-cl-fast-relaxed-math -cl-mad-enable");

		long beginTime = System.currentTimeMillis();

		long solutionsize = (long) Math.pow(2, input.getLiterals().size());
		byte dstArray[] = new byte[(int) solutionsize];

		long sizeTmp = solutionsize;
		int dimensions = 1;
		long[] work_item_sizes = new long[clContext.getWorkItemDimensions().length];
		for (int i = 0; i < work_item_sizes.length; i++) {
			if (sizeTmp > clContext.getWorkItemDimensions()[i]) {
				work_item_sizes[i] = clContext.getWorkItemDimensions()[i];
				sizeTmp /= clContext.getWorkItemDimensions()[i];
				if (sizeTmp > 1)
					dimensions++;
				else
					break;
			} else {
				work_item_sizes[i] = sizeTmp;
				break;
			}
		}

		if (VERBOSE) {
			String wis = "[";
			for (int i = 0; i < work_item_sizes.length; i++) {
				wis += work_item_sizes[i] + " ";
			}
			wis += "]";
			System.out.println("Dimensions: " + dimensions
					+ "   Work item sizes: " + wis + "     Solution size: "
					+ solutionsize + "   Literals: "
					+ input.getLiterals().size());
		}

		if (dimensions > clContext.getWorkItemDimensions().length) {
			throw new Exception("Too big state space! (" + solutionsize
					+ " states)");
		}

		int[] treearray = new int[input.getTree().size() + 2];
		treearray[0] = input.getTree().size();
		treearray[1] = input.getLiterals().size() - 1;
		for (int i = 2; i < treearray.length; i++) {
			treearray[i] = input.getTree().get(i - 2);
		}

		Pointer srcB = Pointer.to(treearray);
		memObjects[0] = clCreateBuffer(clContext.getContext(), CL_MEM_READ_ONLY
				| CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * treearray.length, srcB,
				null);
		memObjects[1] = clCreateBuffer(clContext.getContext(), CL_MEM_WRITE_ONLY,
				Sizeof.cl_char * solutionsize, null, null);

		for (int i = 0; i < memObjects.length; i++) {
			clSetKernelArg(clKernel.getKernel(), i, Sizeof.cl_mem,
					Pointer.to(memObjects[i]));
		}

		clEnqueueNDRangeKernel(clContext.getCommandQueue(), clKernel.getKernel(),
				dimensions, null, work_item_sizes, null, 0, null, null);

		Pointer dst = Pointer.to(dstArray);

		clEnqueueReadBuffer(clContext.getCommandQueue(), memObjects[1], CL_TRUE, 0,
				Sizeof.cl_char * solutionsize, dst, 0, null, null);

		long completionTime = System.currentTimeMillis() - beginTime;

		List<List<Boolean>> allSolutions = new ArrayList<List<Boolean>>();
		for (int i = 0; i < dstArray.length; i++) {

			if (dstArray[i] == 1) {
				List<Boolean> solution = new ArrayList<Boolean>();
				for (int j = 0; j < input.getLiterals().size(); j++) {
					boolean clauseValue = (((i >> (input.getLiterals().size() - 1 - j)) & 1) == 1);
					solution.add(clauseValue);
				}
				allSolutions.add(solution);
			}
		}

		for (int i = 0; i < memObjects.length; i++) {
			clReleaseMemObject(memObjects[i]);
		}

		String message = "Total solutions: " + allSolutions.size();
		return new SolverResult(message, input, allSolutions, completionTime);
	}

	private void delete() {
		if (memObjects != null) {
			/*
			 * for (int i = 0; i < memObjects.length; i++) {
			 * clReleaseMemObject(memObjects[i]); }
			 */
			memObjects = null;
		}

		if (clKernel != null) {
			clKernel.delete();
			clKernel = null;
		}

		if (clContext != null) {
			clContext.delete();
			clContext = null;
		}
	}

	public static int getSleepBetweenExecution() {
		return sleepBetweenExecution;
	}

	public static void setSleepBetweenExecution(int sleepBetweenExecution) {
		CLSatSolver.sleepBetweenExecution = sleepBetweenExecution;
	}

	public static List<CLDevice> getAvailableDevices()
	{
		return CLUtils.GetAllAvailableDevices();
	}
	
}
