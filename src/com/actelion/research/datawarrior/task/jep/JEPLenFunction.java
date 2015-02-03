/*
/**
 * An example custom function class for JEP.
 */
public class JEPLenFunction extends PostfixMathCommand {
	/**
	 * Constructor
	 */
	public JEPLenFunction() {
		numberOfParameters = 1;
	    }
	/**
	 * Runs the square root operation on the inStack. The parameter is popped
	 * off the <code>inStack</code>, and the square root of it's value is 
	 * pushed back to the top of <code>inStack</code>.
	 */
	public void run(Stack inStack) throws ParseException {
		// check the stack
		checkStack(inStack);
		// get the parameters from the stack
		Object param1 = inStack.pop();
		// check whether the argument is of the right type
			// calculate the result
			double len = ((String)param1).length();

			// push the result on the inStack
			inStack.push(new Double(len));
		    }
			throw new ParseException("Invalid parameter type");
		    }
	    }
    }