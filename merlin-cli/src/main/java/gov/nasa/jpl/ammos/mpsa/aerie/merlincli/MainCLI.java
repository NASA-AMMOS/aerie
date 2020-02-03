package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpClientHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.RemoteAdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.RemotePlanRepository;
import org.apache.http.impl.client.HttpClients;

import java.util.Scanner;

public class MainCLI {
    public static void main(String[] args) {
        final PlanRepository planRepository = new RemotePlanRepository(new HttpClientHandler(HttpClients.createDefault()));
        final AdaptationRepository adaptationRepository = new RemoteAdaptationRepository(new HttpClientHandler(HttpClients.createDefault()));
        final MerlinCommandReceiver commandReceiver = new AerieCommandReceiver(planRepository, adaptationRepository);

        final CommandOptions commandOptions = new CommandOptions();
        if (args.length > 0) {
            runOne(commandOptions, commandReceiver, args);
        } else {
            runRepl(commandOptions, commandReceiver);
        }
    }

    private static void runOne(CommandOptions commandOptions, MerlinCommandReceiver commandReceiver, String[] args) {
        if (!commandOptions.parse(commandReceiver, args)) {
            commandOptions.printUsage();
        }
    }

    private static void runRepl(CommandOptions commandOptions, MerlinCommandReceiver commandReceiver) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("\nEnter command: ");
            String line = sc.nextLine();
            commandOptions.parse(commandReceiver, parseArguments(line));
        }
    }

    private static String[] parseArguments(String line) {
        // TODO: Handle strings properly in arguments (may contain spaces)
        return line.split("\\s+");
    }
}
