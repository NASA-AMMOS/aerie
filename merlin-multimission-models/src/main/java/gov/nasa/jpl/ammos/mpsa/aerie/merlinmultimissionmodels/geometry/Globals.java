package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry;


//TODO: Consider using Strings instead of enums
public class Globals {

    public enum Body {
        EUROPA, GANYMEDE, EARTH, SUN, CLIPPER;
    }

    public enum ABCORR {
        LTS("LT+S");

        public final String type;
        ABCORR(final String type) {this.type = type; }
    }

    public enum Apsis {
        APOAPSIS, PERIAPSIS;
    }

    public enum NAIFID{
        EUROPA("159"),
        GANYMEDE("503");

        public final String naifId;
        NAIFID(final String naifId) { this.naifId = naifId; }
    }
}

