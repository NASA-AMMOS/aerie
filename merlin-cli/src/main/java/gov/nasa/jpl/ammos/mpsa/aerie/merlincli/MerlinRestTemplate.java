package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class MerlinRestTemplate extends RestTemplate {

    public MerlinRestTemplate() {
        super();
        this.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }
}
