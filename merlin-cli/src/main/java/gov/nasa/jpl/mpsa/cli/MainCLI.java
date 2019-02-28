package gov.nasa.jpl.mpsa.cli;

public class MainCLI {

    public static void main(String args[]) {
        new CommandOptions(args).parse();
    }
}
