package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry;



//TODO: Consider using Strings instead of enums
//Occultations: get online from NAIF cspice/gfoclt_c.html

public class Globals {

    /** indicates the aberration corrections to be applied to
     the state of each target body to account for one-way
     light time.  Stellar aberration corrections are
     ignored if specified, since these corrections don't
     improve the accuracy of the occultation determination.

     See the header of the SPICE routine spkezr_c for a
     detailed description of the aberration correction
     options. For convenience, the options supported by
     this routine are listed below:

     "NONE"     Apply no correction.

     "LT"       "Reception" case:  correct for
     one-way light time using a Newtonian
     formulation.

     "CN"       "Reception" case:  converged
     Newtonian light time correction.

     "XLT"      "Transmission" case:  correct for
     one-way light time using a Newtonian
     formulation.

     "XCN"      "Transmission" case:  converged
     Newtonian light time correction.

     Case and blanks are not significant in the string
     `abcorr'.
     */
    public enum ABCORR {
        LTS("LT+S"),
        LT("LT"),
        CN("CN");

        public final String type;
        ABCORR(final String type) {this.type = type; }
    }


    public enum Body {
        EUROPA, GANYMEDE, EARTH, SUN, CLIPPER, MOON;
    }

    public enum Apsis {
        APOAPSIS, PERIAPSIS;
    }

    public enum NAIFID{
        EUROPA("159"),
        GANYMEDE("503"),
        MOON("301");

        public final String naifId;
        NAIFID(final String naifId) { this.naifId = naifId; }
    }

    public enum OccultationType {
        FULL, ANNULAR, PARTIAL, ANY;
    }

    //geometric model used to represent the shape of the front target body in an occultation
    public enum Shape {
        ELLIPSOID, POINT;
    }

    //body-foxed, body-cenetered reference frame associated with the front target body
    //earth is ITRF93
    public enum ReferenceFrame {
        ITRF93, MOON_PA, IAU_MOON, IAU_SUN;
    }
}

