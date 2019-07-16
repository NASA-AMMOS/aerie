package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static org.junit.Assert.assertEquals;

public class JavaStringEvaluator {
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
