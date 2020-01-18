package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        /*----------------------------- SAMPLE ADAPTOR WORK -------------------------------*/

        DataSystemModel dataSystemModel = new DataSystemModel();

        SettableState<Double> dataRate = new SettableState<>(GlobalPronouns.dataRate, dataSystemModel);
        SettableState<Double> dataVolume = new SettableState<>(GlobalPronouns.dataVolume, dataSystemModel);
        SettableState<String> dataProtocol = new SettableState<>(GlobalPronouns.UART, dataSystemModel);

        /*----------------------------- SAMPLE SIM ---------------------------------------*/

      /*  Time simStartTime = new Time();
        Time event1 = simStartTime.add(Duration.fromSeconds(10));
        Time event2 = event1.add(Duration.fromSeconds(10));
        Time event3 = event2.add(Duration.fromSeconds(20));
        Time event4 = event3.add(Duration.fromSeconds(1));
        Time event5 = event4.add(Duration.fromSeconds(5));

        dataRate.set(1.0, event1);
        dataRate.set(10.0, event2);
        dataRate.set(15.0, event3);

        List<Event> eventLog = MissionModelGlue.Registry.getEventLog(dataSystemModel);
        printEventLog(eventLog);*/

        System.out.println(dataRate.get());
    }

    /*----------------------------- UTILITY METHODS ------------------------------------------*/
    public static void printEventLog(List<Event> eventLog) {
        int i = 0;
        for (Event x : eventLog) {
            System.out.println("Event " + i + " : " + eventLog.get(i).time() + " " + eventLog.get(i).name() + " " + eventLog.get(i).value().toString());
            i++;
        }
    }
}
