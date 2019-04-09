package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static org.junit.Assert.assertEquals;

public class JavaStringEvaluator {

    @Test
    public void testStringOne(){
        DoubleEvaluator evaluator = new DoubleEvaluator();
        String expression = "(2^3-1)*sin(pi/4)/ln(pi^2)";
        Double result = evaluator.evaluate(expression);
        Double x = (Math.pow(2.0, 3.0) - 1) * (Math.sin(Math.PI/4.0))/Math.log(Math.pow(Math.PI,2));
        assertEquals(result, x);
    }

    @Test
    public void testStringTwo(){
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        Boolean a = false;
        Boolean b = true;
        Boolean result = null;
        try {
            result = (Boolean) engine.eval(a.toString() +  "||"  + b.toString());
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        assertEquals(result, true);
    }
}
