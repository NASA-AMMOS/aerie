package gov.nasa.jpl.europa.clipper.merlin.geometry.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import gov.nasa.jpl.ammos.mpsa.merlin.multimissionmodels.geometry.classes.GeometryEnums.Body;

import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.AttitudeMode;
import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.GNCControlMode;
import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.SolarArrayMode;
import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

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
    Duration eclipseDuration = new Duration(10, Time.Minute);

    /* --------------------- EFFECT MODELING & DECOMPOSITION -------------------- */

    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        clipper = ctx.getStates();
        Time eclipseEnd = ctx.now().plus(eclipseDuration);

        int segments = 10;
        Duration stepTime = eclipseDuration.dividedBy(segments);

        for (int i = 0; i < segments; i++) {

            Double fracSunNotBlocked;

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
            if (ctx.now().plus(stepTime).isBefore(eclipseEnd)) {
                ctx.wait(stepTime);
            } else {
                ctx.wait(eclipseEnd.minus(ctx.now()));
            }
        }
    }

}