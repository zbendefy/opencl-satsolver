package bi106z.test;

import java.util.List;
import java.util.Scanner;

import bi106z.satsolver.CLSatSolver;
import bi106z.satsolver.SolverInput;
import bi106z.satsolver.SolverResult;
import clframework.common.CLDevice;

public class Test {

	public static void main(String[] args) {
		run();
	}
	
	private static void printResult(SolverResult result)
	{
		System.out.println("Message: " + result.getMessage()
				+ "   Completed in: "
				+ result.getCalculationTimeMs() + "ms");
		System.out.println("Solutions: ");
		for (List<Boolean> l : result.getSolutions()) {
			String line = "";
			for (int i = 0; i < l.size(); i++) {
				line += (result.getOriginalInput().getLiterals()
						.get(i))
						+ "="
						+ (l.get(i) ? "1" : "0")
						+ "  ";
			}
			System.out.println(line);
		}
	}
	
	private static void run()
	{
		List<CLDevice> deviceList = CLSatSolver.getAvailableDevices();
		CLSatSolver solver;
		try {
			solver = new CLSatSolver();
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		int currentDevice = 0;
		
		printHelp();
		System.out.println("");
		System.out.println("Enter command:");
		
		Scanner s = new Scanner(System.in);
		while (s.hasNext()) {
			String command = s.nextLine();
			try {
				
				if (command.startsWith("listdevices"))
				{
					System.out.println("Available OpenCL devices:");
					for (int i = 0; i < deviceList.size(); i++) {
						System.out.println("" + (i + 1) + ": "
								+ deviceList.get(i).getDeviceName());
					}
				} else if (command.startsWith("showdevice"))
				{
					CLDevice dev = deviceList.get(currentDevice);
					System.out.println("Device platform: " + dev.getPlatformName());
					System.out.println("Device name: " + dev.getDeviceName());
					System.out.println("OpenCL version: " + dev.getOpenCLVersion());
					System.out.println("Compute units: " + dev.getComputeUnitCount());
					System.out.println("Compute unit speed: " + dev.getClockFrequencyMhz() + " Mhz");
					System.out.println("Global memorysize: " + ((dev.getGlobalMemory()/1024)/1024) + " MB");
					
				} else if (command.startsWith("changedevice "))
				{
					String arg = command.replace("changedevice ", "");
					int did ;
					try {
						did = Integer.parseInt(arg) - 1;
					} catch (Exception e) {
						throw new Exception(arg + " is not a number!");
					}
					if (did < 0 || did >= deviceList.size())
						throw new Exception("There is no device with the given id!");
					
					currentDevice = did;
					CLDevice tmp = deviceList.get(did);
					solver = new CLSatSolver(tmp);
					System.out.println("Compute device changed to: " + tmp.getDeviceName());
				}else if (command.startsWith("loadfile ")) {
					String filename = command.replace("loadfile ", "");
					System.out.println("Computing started on device: " + deviceList.get(currentDevice).getDeviceName());
					SolverResult result = solver.runSolver(SolverInput
							.createFromCNFFile(filename.trim()));
					printResult(result);
				}
				else if (command.startsWith("solve ")) {
					String expression = command.replace("solve ", "");
					System.out.println("Computing started on device: " + deviceList.get(currentDevice).getDeviceName());
					SolverResult result = solver.runSolver(SolverInput
							.createFromExpression(expression));
					printResult(result);
				} else if (command.startsWith("help")) {
					printHelp();
				}  else if (command.startsWith("exit")) {
					System.exit(0);
				}
				else
				{
					System.out.println("Type 'help' to get a list of available commands!");
					throw new Exception("No such command");
				}
				
			} catch (Exception e) {
				System.out.println("Error: " + e.getMessage());
			}
			System.out.println("");
			System.out.println("Enter command:");
		}
		s.close();
	}

	
	private static void printHelp()
	{
		System.out.println("Available commands:");
		System.out.println("solve <expression>       Example: solve  (A | B) & (C | !D)");
		System.out.println("loadfile <filename>      Example: loadfile C:\\problem.cnf");
		System.out.println("listdevices              Lists all available OpenCL devices");
		System.out.println("changedevice <deviceid>  Switches the currently used device. Example: changedevice 3");
		System.out.println("showdevice               Prints info about current device");
		System.out.println("help                     Prints this help");
		System.out.println("exit                     Exits");
		System.out.println("");
	}
}
