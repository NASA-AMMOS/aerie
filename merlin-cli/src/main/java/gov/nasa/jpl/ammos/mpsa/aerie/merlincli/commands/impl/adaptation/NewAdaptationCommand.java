package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.adaptation;

import com.google.gson.Gson;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Adaptation;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap.parseToken;

public class NewAdaptationCommand implements Command {

    private RestTemplate restTemplate;
    private String path;
    private Adaptation adaptation;
    private int status;
    private String id;

    public NewAdaptationCommand(RestTemplate restTemplate, String path, String[] tokens) throws InvalidTokenException {
        this.restTemplate = restTemplate;
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(path));
        body.add("name", adaptation.getName());
        body.add("version", adaptation.getVersion());
        if (adaptation.getMission() != null) body.add("mission", adaptation.getMission());
        if (adaptation.getOwner() != null) body.add("owner", adaptation.getOwner());

        HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity(body, headers);
        
        try {
            ResponseEntity response = restTemplate.exchange("http://localhost:27182/api/adaptations", HttpMethod.POST, requestBody, String.class);
            this.status = response.getStatusCodeValue();
            this.id = response.getHeaders().getFirst("location");

        }
        catch (HttpClientErrorException | HttpServerErrorException e) {
            this.status = e.getStatusCode().value();
        }
    }

    public int getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }
}
