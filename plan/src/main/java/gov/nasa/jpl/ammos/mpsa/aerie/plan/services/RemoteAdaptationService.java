package gov.nasa.jpl.ammos.mpsa.aerie.plan.services;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoteAdaptationService implements AdaptationService {
    private final String adaptationUri;

    public RemoteAdaptationService(final String adaptationUri) {
        this.adaptationUri = adaptationUri;
    }

    @Override
    public Map<String, ActivityType> getActivityTypes(String adaptationId) {
        RestTemplate restTemplate = new RestTemplate();
        String uri = String.format("%s/%s/activities", adaptationUri, adaptationId);

        ResponseEntity<List<ActivityType>> response =
                restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<ActivityType>>() {
                        });

        // TODO: Check that the request succeeded

        List<ActivityType> typeList = response.getBody();
        Map<String, ActivityType> activityTypes = typeList
                .stream()
                .collect(Collectors.toMap(type -> type.getName(), type -> type));

        return activityTypes;
    }
}
