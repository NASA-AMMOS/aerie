package gov.nasa.jpl.europa.clipper.merlin.geometry.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import gov.nasa.jpl.ammos.mpsa.merlin.multimissionmodels.geometry.classes.GeometryEnums.Body;

import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.AttitudeMode;
import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.GNCControlMode;
import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.SolarArrayMode;
import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

/**
 * Models a partial eclipse
 *
 * This activity serves more as a geometric event rather than an activity useful for planning. Nevertheless,
 * it uses SPICE to compute the fraction of the sun not blocked by eclipse and sets the appropriate spacecraft state.
 *
 * @subsystem Geometry
 * @version 1.1.2
 * @contacts jdoe, fbar
 * @stakeholders Geometry, GNC
 * @labels geometry, gnc
 * @dateCreated 2019-07-30
 * @dateLastModified 2019-07-30
 * @refs https://example.com/ModelPartialEclipseActivity
 */
@ActivityType("ModelPartialEclipse")
public class ModelPartialEclipseActivity implements Activity {

    public ModelPartialEclipseActivity() {
    }

    /* ------------------------------- PARAMETERS ------------------------------- */

    @Parameter
    Body targetBody = Body.SUN;

    @Parameter
    Body occultingBody = Body.EUROPA;

    @Parameter
    Duration eclipseDuration = Duration.of(10, Duration.MINUTES);

    /* --------------------- EFFECT MODELING & DECOMPOSITION -------------------- */

    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        ClipperStates clipper = ctx.getStates();
        Instant eclipseEnd = ctx.now().plus(eclipseDuration);

        int segments = 10;
        Duration stepTime = Duration.of(eclipseDuration.durationInMicroseconds / segments, Duration.MICROSECONDS);

        for (int i = 0; i < segments; i++) {

            double fracSunNotBlocked;

            // Adapters can call some utility functions they've defined elsewhere
            // if they don't want all logic in the `modelEffects()` method
            EclipseType eclipseType = GeometryUtils.getWorstCurrentEclipse(ctx, clipper);

            // calculate the fraction of sun that isn't blocked by the eclipse
            switch (eclipseType) {
            case NONE:
                fracSunNotBlocked = 1.0;
                break;
            case FULL:
                fracSunNotBlocked = 0.0;
                break;
            case ANNULAR:
            case PARTIAL:
                // Adapters can use CSPICE methods
                fracSunNotBlocked = CSPICE.vzfrac(NAIF.ids.get(occultingBody),
                        NAIF.bodyFrames.get(occultingBody),GeometryUtils.bodyRadii.get(occultingBody),
                        NAIF.ids.get(targetBody), NAIF.bodyFrames.get(targetBody),
                        GeometryUtils.bodyRadii.get(targetBody), Config.spacecraftId, ctx.now());
                break;
            }
            // set a spacecraft state
            clipper.geometry.eclipseFactor.set(fracSunNotBlocked);

            // wait for step size OR until the end of the eclipse
            ctx.wait(Duration.min(stepTime, eclipseEnd.durationFrom(ctx.now())));
        }
    }

}
