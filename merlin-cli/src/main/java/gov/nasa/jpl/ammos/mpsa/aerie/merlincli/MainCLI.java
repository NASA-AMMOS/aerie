package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpClientHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.RemotePlanRepository;
import org.apache.http.impl.client.HttpClients;

public class MainCLI {

    public static void main(String args[]) {
        final PlanRepository planRepository = new RemotePlanRepository(new HttpClientHandler(HttpClients.createDefault()));
        CommandOptions commandOptions = new CommandOptions(args, planRepository);
        commandOptions.parse();
    }
}
