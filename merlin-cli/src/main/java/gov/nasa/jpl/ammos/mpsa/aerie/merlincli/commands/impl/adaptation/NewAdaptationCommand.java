package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.Adaptation;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap.parseToken;

public class NewAdaptationCommand implements Command {

    private HttpHandler httpClient;
    private String path;
    private Adaptation adaptation;
    private int status;
    private String id;

    public NewAdaptationCommand(HttpHandler httpClient, String path, String[] tokens) throws InvalidTokenException {
        this.httpClient = httpClient;
        this.path = path;
        this.status = -1;
        this.adaptation = new Adaptation();

        for (String token : tokens) {
            TokenMap tokenMap = parseToken(token);
            switch(tokenMap.getName()) {
                case "name":
                    adaptation.setName(tokenMap.getValue());
                    break;
                case "version":
                    adaptation.setVersion(tokenMap.getValue());
                    break;
                case "mission":
                    adaptation.setMission(tokenMap.getValue());
                    break;
                case "owner":
                    adaptation.setOwner(tokenMap.getValue());
                    break;
                default:
                    throw new InvalidTokenException(token, String.format("'%s' is not a valid attribute", tokenMap.getName()));
            }
        }
    }

    @Override
    public void execute() {
        HttpPost request = new HttpPost("http://localhost:27182/api/adaptations");
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("name", adaptation.getName()));
        parameters.add(new BasicNameValuePair("version", adaptation.getVersion()));
        if (adaptation.getMission() != null) parameters.add(new BasicNameValuePair("mission", adaptation.getMission()));
        if (adaptation.getOwner() != null) parameters.add(new BasicNameValuePair("owner", adaptation.getOwner()));

        try {
            request.setEntity(EntityBuilder.create()
                    .setFile(new File(this.path))
                    .setParameters(parameters)
                    .build());
            request.setEntity(new FileEntity(new File(this.path)));

            HttpResponse response = this.httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

            if (status == 201 && response.containsHeader("location")) {
                this.id = response.getFirstHeader("location").toString();
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }
}
