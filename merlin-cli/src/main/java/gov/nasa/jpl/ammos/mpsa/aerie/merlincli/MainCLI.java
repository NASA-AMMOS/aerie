package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

public class MainCLI {

    public static void main(String args[]) {
        new CommandOptions(args).parse();
    }
}
