/*
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
/**
 * An example custom function class for JEP.
 */
public class JEPContainsFunction extends PostfixMathCommand {
	/**
	 * Constructor
	 */
	public JEPContainsFunction() {
		numberOfParameters = 2;
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
		Object param2 = inStack.pop();
		// check whether the argument is of the right type
			// calculate the result

			// push the result on the inStack
			inStack.push(new Double(value));
		    }
			throw new ParseException("Invalid parameter type");
		    }
	    }
    }