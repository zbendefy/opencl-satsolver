package bi106z.satsolver;

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
}
