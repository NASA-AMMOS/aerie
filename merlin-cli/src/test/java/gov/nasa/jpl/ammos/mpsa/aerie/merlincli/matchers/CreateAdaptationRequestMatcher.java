package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.MultiValueMap;

/**
 * Used to match the body of a request to create an Adaptation
 * with the expected information
 */
public class CreateAdaptationRequestMatcher extends BaseMatcher {

    private String path;
    private String name;
    private String version;
    private String owner;
    private String mission;

    public CreateAdaptationRequestMatcher(String path, String name, String version, String owner, String mission) {
        this.path = path;
        this.name = name;
        this.version = version;
        this.owner = owner;
        this.mission = mission;
    }

    @Override
    public boolean matches(Object o) {
        if (!(o instanceof MultiValueMap)) return false;

        MultiValueMap<String, Object> obj = (MultiValueMap<String,Object>)o;
        FileSystemResource ofile = (FileSystemResource)(obj.getFirst("file"));
        String oname = (String)obj.getFirst("name");
        String oversion = (String)obj.getFirst("version");
        String omission = (String)obj.getFirst("mission");
        String oowner = (String)obj.getFirst("owner");

        if (!ofile.getPath().equals(path)) return false;
        if ((name == null && oname != null) || (name != null && !name.equals(oname))) return false;
        if ((version == null && oversion != null) || (version != null && !version.equals(oversion))) return false;
        if ((mission == null && omission != null) || (mission != null && !mission.equals(omission))) return false;
        if ((owner == null && oowner != null) || (owner != null && !owner.equals(oowner))) return false;

        return true;
    }

    @Override
    public void describeTo(Description description) {

    }
}
