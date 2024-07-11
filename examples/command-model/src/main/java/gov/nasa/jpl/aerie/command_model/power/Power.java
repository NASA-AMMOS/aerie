package gov.nasa.jpl.aerie.command_model.power;

import gov.nasa.jpl.aerie.command_model.activities.commands.CMD_POWER_OFF;
import gov.nasa.jpl.aerie.command_model.activities.commands.CMD_POWER_ON;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequencing;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$boolean;
import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$double;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.turnOff;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.turnOn;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;

public class Power {
    public final MutableResource<Discrete<Boolean>> device1state = discreteResource(false);
    public final Resource<Discrete<Double>> device1powerDraw_W = choose(device1state, constant(30.0), constant(0.0));
    public final MutableResource<Discrete<Boolean>> device2state = discreteResource(false);
    public final Resource<Discrete<Double>> device2powerDraw_W = choose(device2state, constant(50.0), constant(0.0));
    public final Resource<Discrete<Double>> totalPowerDraw_W = add(device1powerDraw_W, device2powerDraw_W);

    public Power(Sequencing sequencing, Registrar registrar) {
        sequencing.listenForCommand(CMD_POWER_ON.class, event -> {
            var deviceState = switch (event.command().arguments().get(0)) {
                case "DEVICE_1" -> device1state;
                case "DEVICE_2" -> device2state;
                default -> null;
            };
            if (deviceState != null) turnOn(deviceState);
        });
        sequencing.listenForCommand(CMD_POWER_OFF.class, event -> {
            var deviceState = switch (event.command().arguments().get(0)) {
                case "DEVICE_1" -> device1state;
                case "DEVICE_2" -> device2state;
                default -> null;
            };
            if (deviceState != null) turnOff(deviceState);
        });

        registrar.discrete("device1state", device1state, $boolean());
        registrar.discrete("device2state", device2state, $boolean());
        registrar.discrete("device1powerDraw_W", device1powerDraw_W, $double());
        registrar.discrete("device2powerDraw_W", device2powerDraw_W, $double());
        registrar.discrete("totalPowerDraw_W", totalPowerDraw_W, $double());
    }
}
