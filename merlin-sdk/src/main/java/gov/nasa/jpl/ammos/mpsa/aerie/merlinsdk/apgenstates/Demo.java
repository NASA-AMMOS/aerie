package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.APGenStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.ConsumableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.SettableState;

public class Demo {

    public static void settableStateDemo() {

        //Equivalent to setup
        final var factory = new APGenStateFactory();
        String apertureNAC = "NAC aperture";
        String dataRateEIS = "EIS data rate";

        //Example use case by mission modeler
        SettableState nacAperture = factory.createSettableState(apertureNAC, 2.8);
        SettableState eisDataRate = factory.createSettableState(dataRateEIS, 6);

        //Equivalent to events produced by Activities during a simulation
        //Print statements for demonstration purposes
        nacAperture.set(1.4);
        eisDataRate.set(12);
        eisDataRate.set(6);

        System.out.println(factory.graph());
        System.out.println(apertureNAC + " : " + nacAperture.get());
        System.out.println(dataRateEIS + " : " + eisDataRate.get());
        System.out.println();

        nacAperture.set(16);
        eisDataRate.set(14.5);

        System.out.println(factory.graph());
        System.out.println(apertureNAC + " : " + nacAperture.get());
        System.out.println(dataRateEIS + " : " + eisDataRate.get());
        System.out.println();


    }

    public static void consumableStateDemo(){

        //Equivalent to setup
        final var factory = new APGenStateFactory();
        String stateOfCharge = "soc";
        String powerDraw = "powerDraw";

        //Example use case by mission modeler
        ConsumableState socState = factory.createConsumableState(stateOfCharge, 10);
        ConsumableState powerDrawState = factory.createConsumableState(powerDraw, 200);

        //Equivalent to events produced by Activities during a simulation
        //Print statements for demonstration purposes
        socState.add(5);
        socState.add(10);
        socState.add(-3);

        System.out.println(factory.graph());
        System.out.println(stateOfCharge + " : " + socState.get());
        System.out.println(powerDraw + " : " + powerDrawState.get());
        System.out.println();

        powerDrawState.add(50.5);
        socState.add(-0.5);

        System.out.println(factory.graph());
        System.out.println(stateOfCharge + " : " + socState.get());
        System.out.println(powerDraw + " : " + powerDrawState.get());
        System.out.println();
    }

    public static void main(final String[] args) {
        System.out.println("Settable State Demo");
        if (true) settableStateDemo();

        System.out.println("\nConsumable State Demo");
        if (true) consumableStateDemo();
    }


}
