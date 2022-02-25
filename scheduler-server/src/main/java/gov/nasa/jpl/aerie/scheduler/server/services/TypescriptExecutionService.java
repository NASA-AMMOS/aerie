package gov.nasa.jpl.aerie.scheduler.server.services;

import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class TypescriptExecutionService {
  public static Object executeTypescript(final String typescriptCode)
  throws TypescriptExecutionException, TypescriptCompilationException
  {
    try {
      return executeJavascript(compileTypescriptToJavascript(typescriptCode));
    } catch (IOException | InterruptedException | ScriptException e) {
      throw new TypescriptExecutionException(e);
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static String compileTypescriptToJavascript(final String typescriptCode)
  throws IOException, InterruptedException, TypescriptCompilationException
  {
    final var typescriptFile = File.createTempFile("temporary-typescript-file", ".ts");
    final var javascriptFile = File.createTempFile("temporary-javascript-file", ".js");

    createTemporaryTypescriptFile(typescriptCode, typescriptFile.getAbsolutePath());
    runTypescriptCompiler(typescriptFile.getAbsolutePath(), javascriptFile.getAbsolutePath());
    final var result = readGeneratedJavascriptFile(javascriptFile.getAbsolutePath());

    // it's okay if these deletes fail, so we do not check the return status
    typescriptFile.delete();
    javascriptFile.delete();

    return result;
  }

  private static String readGeneratedJavascriptFile(final String generatedJavascriptFilename) {
    final String javascriptCode;
    try (final var br = new BufferedReader(new FileReader(generatedJavascriptFilename))) {
      javascriptCode = bufferedReaderToString(br);
    } catch (IOException e) {
      throw new Error("Error reading generated javascript file", e);
    }
    return javascriptCode;
  }

  private static String bufferedReaderToString(BufferedReader br) throws IOException {
    final String javascriptCode;
    final var sb = new StringBuilder();
    var line = br.readLine();

    while (line != null) {
      sb.append(line);
      sb.append(System.lineSeparator());
      line = br.readLine();
    }
    javascriptCode = sb.toString();
    return javascriptCode;
  }

  private static void runTypescriptCompiler(final String temporaryTypescriptFilename, final String outfile)
  throws IOException, InterruptedException, TypescriptCompilationException
  {
    final var schedulingDslCompilerPath = System.getenv("SCHEDULING_DSL_COMPILER_PATH");
    System.out.println("SCHEDULING_DSL_COMPILER_PATH: " + schedulingDslCompilerPath);
    final var rt = Runtime.getRuntime();
    final var commands = new String[] {"node", schedulingDslCompilerPath, temporaryTypescriptFilename, "--outfile", outfile};
    final var pr = rt.exec(commands);
    final var i = pr.waitFor();
    final var console = bufferedReaderToString(pr.errorReader()) + bufferedReaderToString(pr.inputReader());
    System.out.println(console);
    if (i != 0) {
      throw new TypescriptCompilationException(console);
    }
  }

  private static void createTemporaryTypescriptFile(
      final String typescriptCode,
      final String temporaryTypescriptFilename)
  {
    try (final var out = new PrintWriter(temporaryTypescriptFilename)) {
      out.println(typescriptCode);
    } catch (FileNotFoundException e) {
      throw new Error("Error writing to temporary typescript file", e);
    }
  }

  private static Object executeJavascript(final String goalDefinitionJavascript) throws ScriptException {
    final var factory = new NashornScriptEngineFactory();
    final var scriptEngine = factory.getScriptEngine();
    return scriptEngine.eval(goalDefinitionJavascript);
  }

  public static class TypescriptExecutionException extends Exception {
    private TypescriptExecutionException(final Exception e) {
      super(e);
    }
  }

  public static class TypescriptCompilationException extends Exception {
    TypescriptCompilationException(String s) {
      super(s);
    }
  }
}
