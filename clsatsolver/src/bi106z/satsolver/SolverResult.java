package bi106z.satsolver;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class SolverResult {
	private String message;
	private SolverInput originalInput;
	private List<List<Boolean>> solutions;
	private long calculationTime;

	public String getMessage() {
		return message;
	}

	public long getCalculationTimeMs() {
		return calculationTime;
	}

	public SolverResult(String message, SolverInput originalInput,
			List<List<Boolean>> solutions, long calculationTime) {
		super();
		this.message = message;
		this.originalInput = originalInput;
		this.solutions = solutions;
		this.calculationTime = calculationTime;
	}

	public SolverInput getOriginalInput() {
		return originalInput;
	}

	public List<List<Boolean>> getSolutions() {
		return solutions;
	}

	public void saveCsv(String filename) throws IOException {
		final String csvSeparator = ";";

		PrintWriter bw = new PrintWriter(new FileWriter(filename));
		
		bw.write("\"Input problem:\"");
		bw.println();

		bw.write("\"Expression: \"" + csvSeparator + "\""
				+ originalInput.getExpression() + "\"");
		bw.println();
		bw.println();
		bw.write("\"Solution:\"");
		bw.println();
		bw.write("\"" + message + "\"");
		bw.println();
		bw.write("\"Calculation time: " + calculationTime + " ms\"");
		bw.println();
		bw.println();

		for (int i = 0; i < originalInput.getLiterals().size(); i++) {
			bw.write("\"" + originalInput.getLiterals().get(i) + "\""
					+ csvSeparator);
		}
		bw.println();

		for (List<Boolean> list : solutions) {
			for (Boolean val : list) {
				bw.write("\"" + (val ? "1" : "0") + "\""
						+ csvSeparator);
			}
		}

		bw.close();
	}
}
