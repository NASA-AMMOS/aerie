package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.APGenStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.ConsumableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

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

    public static void print(String value)
    {
        System.out.println(value);
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


        socState.when((x) -> (x < 3.3));
    }

    public static void constraintsDemo(){
        //Equivalent to setup
        final var factory = new APGenStateFactory();
        String stateOfCharge = "soc";
        String dataRate = "dataRate";

        //Example use case by mission modeler
        ConsumableState socState = factory.createConsumableState(stateOfCharge, 10);
        SettableState dataRateState = factory.createSettableState(dataRate, 200);

        //Equivalent to events produced by Activities during a simulation
        //Print statements for demonstration purposes
        socState.add(5);
        socState.get();
        dataRateState.set(55.5);
        dataRateState.get();

        factory.step(Duration.of(100, TimeUnit.MICROSECONDS));
        socState.add(40);
        socState.get();
        dataRateState.set(5.5);
        dataRateState.get();

        factory.step(Duration.of(100, TimeUnit.MICROSECONDS));
        socState.add(-40);
        socState.get();
        dataRateState.set(55.5);
        dataRateState.get();

        factory.step(Duration.of(100, TimeUnit.MICROSECONDS));
        socState.add(80);
        socState.get();
        dataRateState.set(5.5);
        dataRateState.get();

        factory.step(Duration.of(100, TimeUnit.MICROSECONDS));

        //time      soc   datarate
        /*
        [0, 100]    15    55.5
        [100, 200]  55    5.5
        [200, 300]  15    55.5
        [300, 400]  95    5.5
         */

        var socConstraint = socState.when(x -> x > 22);
        var dataConstraint = dataRateState.when(x -> x > 10);

        //todo: address question - do we want to say Windows [100, 100], [200, 200], and [300, 300] count
        var andConstraint = socConstraint.and(dataConstraint);
        var orConstraint = socConstraint.or(dataConstraint);

        var socConstraintWrap = socState.whenGreaterThan(22);
        var dataConstraintWrap = dataRateState.whenGreaterThan(10);

        System.out.println(socConstraint.getWindows());
        System.out.println(dataConstraint.getWindows());

        System.out.println(socConstraintWrap.getWindows());
        System.out.println(dataConstraintWrap.getWindows());


        System.out.println(andConstraint.getWindows());
        System.out.println(orConstraint.getWindows());

    }

    public static void main(final String[] args) {
        System.out.println("Settable State Demo");
        if (true) settableStateDemo();

        System.out.println("\nConsumable State Demo");
        if (true) consumableStateDemo();

        System.out.println("\nConstraints Demo");
        if (true) constraintsDemo();
    }


}
