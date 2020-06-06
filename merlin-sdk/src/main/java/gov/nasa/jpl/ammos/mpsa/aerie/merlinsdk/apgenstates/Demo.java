package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.APGenStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.ConsumableState;

public class Demo {

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
        if (true) consumableStateDemo();
    }


}
