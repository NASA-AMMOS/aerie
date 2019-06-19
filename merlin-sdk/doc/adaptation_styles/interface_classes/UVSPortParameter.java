public class UVSPortParameter extends MerlinEnumParameter<UVS.Ports> {

    public String getBrief() {
        return "the instrument port to use to collect data";
    }

    public String getDocumentation() {
        return "specifies the port on the uvs instrument that should be opened and "
                +" used to collect photon data for the calbiration, selected from "
                +" the set of available uvs instrument ports"
        ;
    }
}