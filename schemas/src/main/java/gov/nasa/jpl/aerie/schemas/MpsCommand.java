
package gov.nasa.jpl.aerie.schemas;

import java.util.ArrayList;
import java.util.List;


/**
 * MpsCommand
 * <p>
 * 
 * 
 */
public class MpsCommand {

    /**
     * 
     * (Required)
     * 
     */
    private String name;
    /**
     * 
     * (Required)
     * 
     */
    private List<MpsCommandParameter> parameters = new ArrayList<MpsCommandParameter>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public MpsCommand() {
    }

    /**
     * 
     * @param name
     * @param parameters
     */
    public MpsCommand(String name, List<MpsCommandParameter> parameters) {
        super();
        this.name = name;
        this.parameters = parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * (Required)
     * 
     */
    public List<MpsCommandParameter> getParameters() {
        return parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setParameters(List<MpsCommandParameter> parameters) {
        this.parameters = parameters;
    }

}
