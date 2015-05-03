package bi106z.satsolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SolverInput {

	private String expression;
	private String PostFixExpression;
	private List<String> literals;
	private List<Integer> tree;

	private static boolean VERBOSE = false;

	public static SolverInput createFromExpression(String expression)
			throws Exception {

		List<String> literals = new ArrayList<String>();
		List<Integer> tree = new ArrayList<Integer>();

		// Gathering literals4
		List<Character> text = new ArrayList<Character>();
		for (char c : expression.toCharArray()) {
			text.add(c);
		}

		String currentLiteral = "";
		for (int i = 0; i < text.size(); i++) {
			char c = text.get(i);

			if (isLiteralCharacter(c)) {
				if (currentLiteral.length() == 0) {
					text.add(i, '\'');
					i++;
				}
				currentLiteral = currentLiteral.concat(String.valueOf(c));
			} else {
				if (currentLiteral.length() > 0) {
					text.add(i, '\'');
					i++;
					if (!literals.contains(currentLiteral)) {
						literals.add(currentLiteral);
					}
				}
				currentLiteral = "";
			}
		}
		if (currentLiteral.length() > 0) {
			text.add(text.size(), '\'');
			if (!literals.contains(currentLiteral)) {
				literals.add(currentLiteral);
			}
		}

		expression = "";
		for (Character c : text) {
			expression += c;
		}

		if (expression.contains("&&")) {
			throw new Exception("&& is not a valid operator (use & instead)!");
		}
		if (expression.contains("||")) {
			throw new Exception("|| is not a valid operator (use | instead)!");
		}

		String PostFixExpression;

		// TODO convert to infix format
		try {
			PostFixExpression = shuntingYard(expression);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw e;
		}

		if (VERBOSE)
			System.out.println(expression);
		if (VERBOSE)
			System.out.println(PostFixExpression);
		PostFixExpression = PreProcess(PostFixExpression);
		if (VERBOSE)
			System.out.println(PostFixExpression);

		PostFixExpression = PostFixExpression.replace("&", "-1 ");
		PostFixExpression = PostFixExpression.replace("|", "-2 ");
		PostFixExpression = PostFixExpression.replace("!", "-3 ");
		// PostFixExpression = PostFixExpression.replace("~", "-4 ");
		for (int i = 0; i < literals.size(); i++) {
			PostFixExpression = PostFixExpression.replace("'" + literals.get(i)
					+ "'", Integer.toString(i) + " ");
		}
		PostFixExpression = PostFixExpression.trim();

		String[] elements = PostFixExpression.split(" ");
		for (String string : elements) {
			if (string.trim().length() > 0) {
				tree.add(Integer.parseInt(string));
			}
		}

		SolverInput ret = new SolverInput(expression, PostFixExpression,
				literals, tree);

		return ret;
	}

	private static String PreProcess(String PostFixExpression) {

		// Replaces A&A --> A and A|A --> A
		String prev = "";
		while (!prev.equals(PostFixExpression)) {
			prev = new String(PostFixExpression);
			PostFixExpression = PostFixExpression.replaceAll(
					"'([a-z|A-Z|0-9]+)''\\1'\\|", "'$1'");
			PostFixExpression = PostFixExpression.replaceAll(
					"'([a-z|A-Z|0-9]+)''\\1'&", "'$1'");
		}
		return PostFixExpression;
	}

	public static SolverInput createFromCNFFile(String filepath)
			throws Exception {
		Scanner s = new Scanner( new File(filepath) );
		String text = s.useDelimiter("\\A").next();
		s.close();
		
		return createFromCNFText(text);
	}

	public static SolverInput createFromCNFText(String text) throws Exception {

		int nVariables, nClauses;
		String Expression = "";

		Scanner scanner = new Scanner(text);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();

			line = line.trim();

			int size = line.length();
			line = line.replace("  ", " ");
			while (line.length() != size) {
				size = line.length();
				line = line.replace("  ", " ");
			}

			if (line.length() == 0) {
				continue;
			} else if (line.charAt(0) == 'c') {
				continue;
			} else if (line.charAt(0) == 'p') {
				String[] problem = line.split(" ");
				if (problem.length < 4) {
					// throw new
					// Exception("CNF problem definition is too short!");
					// continue;
				}

				if (problem[1].toLowerCase().equals("cnf")) {
					nVariables = Integer.parseInt(problem[2]);
					nClauses = Integer.parseInt(problem[3]);
				} else {
					// throw new
					// Exception("Problem is not defined as 'CNF'!");
				}
			} else if (line.charAt(0) == '%' || line.charAt(0) == '0') {
				continue;
			} else {
				String[] clause = line.split(" ");

				Expression += (Expression.length() == 0 ? "(" : " & (");

				for (int i = 0; i < clause.length
						- (clause[clause.length - 1].equals("0") ? 1 : 0); i++) {

					Integer val = Integer.parseInt(clause[i]);
					Expression += String.valueOf(val).replace("-", "!")
							+ (i == (clause.length - 2) ? ")" : " | ");
				}
			}

		}
		scanner.close();

		if (Expression.length() != 0) {
			return createFromExpression(Expression);
		}
		throw new Exception("Failed to load cnf file!");
	}

	public SolverInput(String expression, String postFixExpression,
			List<String> literals, List<Integer> tree) {
		super();
		this.expression = expression;
		PostFixExpression = postFixExpression;
		this.literals = literals;
		this.tree = tree;
	}

	// converts infix expressions to postfix
	private static String shuntingYard(String input) throws Exception {
		String outputQueue = "";
		String stack = "";

		String infixExpr = input.replace(" ", "");

		char[] text = infixExpr.toCharArray();
		for (char c : text) {
			if (isLiteralCharacter(c) || c == '\'') {
				outputQueue += c;
			} else if (isOperator(c)) {
				while (stack.length() != 0
						&& isOperator(stack.charAt(stack.length() - 1))) {
					if (getPreced(c) <= getPreced(stack
							.charAt(stack.length() - 1))) {
						outputQueue += stack.charAt(stack.length() - 1);
						stack = stack.substring(0, stack.length() - 1);
					} else {
						break;
					}
				}

				stack += c;

			} else if (c == '(') {
				stack += '(';
			} else if (c == ')') {
				while (stack.charAt(stack.length() - 1) != '(') {
					outputQueue += stack.charAt(stack.length() - 1);
					stack = stack.substring(0, stack.length() - 1);
					if (stack.length() == 0)
						throw new Exception("A closing bracket is missing!");
				}
				stack = stack.substring(0, stack.length() - 1);
			} else {
				throw new Exception("Invalid character: " + c);
			}
		}

		while (stack.length() != 0) {
			if (stack.charAt(stack.length() - 1) == '('
					|| stack.charAt(stack.length() - 1) == ')')
				throw new Exception("A closing bracket is missing!"); // /hibás
																		// zárójelek
			else {
				outputQueue += stack.charAt(stack.length() - 1);
				stack = stack.substring(0, stack.length() - 1);
			}
		}

		return outputQueue;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	static boolean isLiteralCharacter(char c) {
		if (c >= '0' && c <= '9')
			return true;

		if (c >= 'a' && c <= 'z')
			return true;

		if (c >= 'A' && c <= 'Z')
			return true;

		return false;
	}

	static int getPreced(char c) {
		switch (c) {
		case '!':
			return 4;
		case '&':
			return 3;
		case '|':
			// case '~':
			return 2;
		}
		return 0;
	}

	static boolean isOperator(char c) {
		if (c == '&' || c == '|' || c == '!'/* || c == '~' */)
			return true;

		return false;
	}

	public List<String> getLiterals() {
		return literals;
	}

	public List<Integer> getTree() {
		return tree;
	}

	public String getPostFixExpression() {
		return PostFixExpression;
	}

}
