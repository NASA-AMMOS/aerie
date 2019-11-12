//leveraged from the multi-mission time handling tools in M20-Surface-Ops-Tools
//credit to Forrest Ridenhour, Chris Lawler, et al
//
//ref: https://github.jpl.nasa.gov/M20-Surface-Ops-Tools/jplTime/blob/master/src/main/java/gov/nasa/jpl/serialization/ConvertableFromString.java

package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities;

public interface ConvertableFromString {
    public abstract void valueOf(String s);
}
