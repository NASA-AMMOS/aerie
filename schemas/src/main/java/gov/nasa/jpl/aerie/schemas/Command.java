
package gov.nasa.jpl.aerie.schemas;

import java.util.ArrayList;
import java.util.List;


/**
 * Command
 * <p>
 * 
 * 
 */
public class Command {

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
    private List<CommandParameter> parameters = new ArrayList<CommandParameter>();
    private String template;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Command() {
    }

    /**
     * 
     * @param template
     * @param name
     * @param parameters
     */
    public Command(String name, List<CommandParameter> parameters, String template) {
        super();
        this.name = name;
        this.parameters = parameters;
        this.template = template;
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
    public List<CommandParameter> getParameters() {
        return parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setParameters(List<CommandParameter> parameters) {
        this.parameters = parameters;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

}
