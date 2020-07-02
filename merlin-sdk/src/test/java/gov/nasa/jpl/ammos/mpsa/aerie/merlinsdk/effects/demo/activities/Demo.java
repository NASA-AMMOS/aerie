package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.UtilityMethods;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Demo {

    @Test()
    public void activitySameStartEndConcurrent(){
        final var mapper = new MyActivityMapper();
        Activity a = new ActivityA();
        Activity b = new ActivityB();
        Activity c = new Activity() {};

        String aID = "A1";
        String bID = "B1";
        String cID = "C1";

        final var concurrentGraph =
                concurrently(
                        sequentially(
                                atom(ActivityEvent.startActivity(aID, mapper.serializeActivity(a).get())),
                                atom(ActivityEvent.endActivity(aID))),
                        atom(ActivityEvent.startActivity(cID, mapper.serializeActivity(c).get())));

        final var evaluator = new ActivityEffectEvaluator();
        final var applicator = new ActivityModelApplicator();
        final ActivityModel model = applicator.initial();

        applicator.apply(model, concurrentGraph.evaluate(evaluator));

        assertEquals(model.getInstanceWindow(cID).get(0), (Window.between(Duration.of(0, TimeUnit.SECONDS),Duration.of(0, TimeUnit.SECONDS))));
        assertEquals(model.getInstanceWindow(aID).get(0), (Window.between(Duration.of(0, TimeUnit.SECONDS),Duration.of(0, TimeUnit.SECONDS))));
    }

    @Test(expected = RuntimeException.class)
    public void sameStartEndSequential(){
        final var mapper = new MyActivityMapper();
        Activity a = new ActivityA();
        String aID = "A1";

        final var graph = EventGraph.concurrently(
                                atom(ActivityEvent.startActivity(aID, mapper.serializeActivity(a).get())),
                                atom(ActivityEvent.endActivity(aID)));

        final var evaluator = new ActivityEffectEvaluator();

        graph.evaluate(evaluator);
    }

    @Test(expected = RuntimeException.class)
    public void twoConcurrentStarts(){
        final var mapper = new MyActivityMapper();
        Activity a = new ActivityA();
        String aID = "A1";

        final var graph = EventGraph.concurrently(
                atom(ActivityEvent.startActivity(aID, mapper.serializeActivity(a).get())),
                atom(ActivityEvent.startActivity(aID, mapper.serializeActivity(a).get())));

        final var evaluator = new ActivityEffectEvaluator();
        graph.evaluate(evaluator);
    }

    @Test(expected = RuntimeException.class)
    public void twoConcurrentEnds(){
        final var mapper = new MyActivityMapper();
        Activity a = new ActivityA();
        String aID = "A1";

        final var graph = sequentially(
                atom(ActivityEvent.startActivity(aID, mapper.serializeActivity(a).get())),
                    EventGraph.concurrently(
                            atom(ActivityEvent.endActivity(aID)),
                            atom(ActivityEvent.endActivity(aID))));

        final var evaluator = new ActivityEffectEvaluator();
        graph.evaluate(evaluator);
    }

    @Test
    public void aSequentialTestForA(){
        final var mapper = new MyActivityMapper();
        Activity a = new ActivityA();
        Activity b = new ActivityB();

        String aID = "A1";
        String bID = "B1";

        final var seqGraph =
                sequentially(
                        EventGraph.concurrently(
                                atom(ActivityEvent.startActivity(aID, mapper.serializeActivity(a).get())),
                                atom(ActivityEvent.startActivity(bID, mapper.serializeActivity(b).get()))),
                        atom(ActivityEvent.endActivity(aID)));

        final var evaluator = new ActivityEffectEvaluator();
        final var applicator = new ActivityModelApplicator();
        final ActivityModel model = applicator.initial();

        applicator.apply(model, seqGraph.evaluate(evaluator));

        assertEquals(model.getInstanceWindow(aID).get(0), (Window.between(Duration.of(0, TimeUnit.SECONDS),Duration.of(0, TimeUnit.SECONDS))));
        assertEquals(model.getInstanceWindow(bID).get(0), (Window.between(Duration.of(0, TimeUnit.SECONDS),Duration.of(0, TimeUnit.SECONDS))));
    }

    @Test
    public void activityTypeWindowsTest() {
        final var mapper = new MyActivityMapper();
        Activity a = new ActivityA();
        String a1ID = "A1";
        String a2ID = "A2";
        String a3ID = "A3";
        String a4ID = "A4";

        final var startA1Graph = atom(ActivityEvent.startActivity(a1ID, mapper.serializeActivity(a).get()));
        final var startA2Graph = atom(ActivityEvent.startActivity(a2ID, mapper.serializeActivity(a).get()));
        final var startA3Graph = atom(ActivityEvent.startActivity(a3ID, mapper.serializeActivity(a).get()));
        final var startA4Graph = atom(ActivityEvent.startActivity(a4ID, mapper.serializeActivity(a).get()));

        final var endA1Graph = atom(ActivityEvent.endActivity(a1ID));
        final var endA2Graph = atom(ActivityEvent.endActivity(a2ID));
        final var endA3Graph = atom(ActivityEvent.endActivity(a3ID));
        final var endA4Graph = atom(ActivityEvent.endActivity(a4ID));

        final var evaluator = new ActivityEffectEvaluator();
        final var applicator = new ActivityModelApplicator();
        final ActivityModel model = applicator.initial();

        applicator.apply(model, startA1Graph.evaluate(evaluator));
        applicator.step(model, Duration.of(5, TimeUnit.SECONDS));
        applicator.apply(model, endA1Graph.evaluate(evaluator));

        var aWindows = model.getTypeWindows(mapper.serializeActivity(a).get().getTypeName());
        assertEquals(aWindows.get(0), Window.between(Duration.of(0, TimeUnit.SECONDS),Duration.of(5, TimeUnit.SECONDS)));

        applicator.apply(model, startA2Graph.evaluate(evaluator));
        applicator.step(model, Duration.of(5, TimeUnit.SECONDS));
        applicator.apply(model, endA2Graph.evaluate(evaluator));
        aWindows = model.getTypeWindows(mapper.serializeActivity(a).get().getTypeName());
        assertEquals(aWindows.get(0), Window.between(Duration.of(0, TimeUnit.SECONDS),Duration.of(10, TimeUnit.SECONDS)));

        applicator.step(model, Duration.of(1, TimeUnit.SECONDS));

        applicator.apply(model, startA3Graph.evaluate(evaluator));
        applicator.apply(model, startA4Graph.evaluate(evaluator));
        applicator.step(model, Duration.of(5, TimeUnit.SECONDS));
        applicator.apply(model, endA3Graph.evaluate(evaluator));
        aWindows = model.getTypeWindows(mapper.serializeActivity(a).get().getTypeName());
        assertTrue(aWindows.contains(Window.between(Duration.of(0, TimeUnit.SECONDS),Duration.of(10, TimeUnit.SECONDS))));
        assertTrue(aWindows.contains(Window.between(Duration.of(11, TimeUnit.SECONDS),Duration.of(16, TimeUnit.SECONDS))));

        applicator.step(model, Duration.of(11, TimeUnit.SECONDS));
        applicator.apply(model, endA4Graph.evaluate(evaluator));
        aWindows = model.getTypeWindows(mapper.serializeActivity(a).get().getTypeName());
        assertTrue(aWindows.contains(Window.between(Duration.of(0, TimeUnit.SECONDS),Duration.of(10, TimeUnit.SECONDS))));
        assertTrue(aWindows.contains(Window.between(Duration.of(11, TimeUnit.SECONDS),Duration.of(27, TimeUnit.SECONDS))));
    }

    @Test
    public void activityModelExample() {
        final var mapper = new MyActivityMapper();
        Activity a = new ActivityA();
        Activity b = new ActivityB();
        Activity anotherA = new ActivityA();
        Activity anotherB = new ActivityB();
        Activity c = new Activity() {};

        String aID = "A1";
        String bID = "B1";
        String anotherAID = "A2";
        String anotherBID = "B2";
        String cID = "C1";

        final var graph1 =
                concurrently(
                        concurrently(
                                atom(ActivityEvent.startActivity(aID, mapper.serializeActivity(a).get())),
                                atom(ActivityEvent.startActivity(cID, mapper.serializeActivity(c).get()))),
                        atom(ActivityEvent.startActivity(bID, mapper.serializeActivity(b).get())));

        final var evaluator = new ActivityEffectEvaluator();
        final var applicator = new ActivityModelApplicator();
        final ActivityModel model = applicator.initial();


        applicator.step(model, Duration.of(5, TimeUnit.SECONDS));
        applicator.apply(model, graph1.evaluate(evaluator));

        assertEquals(model.getInstanceWindow(cID).get(0), (Window.between(Duration.of(5, TimeUnit.SECONDS),Duration.of(5, TimeUnit.SECONDS))));
        assertEquals(model.getInstanceWindow(aID).get(0), (Window.between(Duration.of(5, TimeUnit.SECONDS),Duration.of(5, TimeUnit.SECONDS))));
        assertEquals(model.getInstanceWindow(bID).get(0), (Window.between(Duration.of(5, TimeUnit.SECONDS),Duration.of(5, TimeUnit.SECONDS))));

        applicator.step(model, Duration.of(5, TimeUnit.SECONDS));

        assertEquals(model.getInstanceWindow(cID).get(0), (Window.between(Duration.of(5, TimeUnit.SECONDS),Duration.of(10, TimeUnit.SECONDS))));
        assertEquals(model.getInstanceWindow(aID).get(0), (Window.between(Duration.of(5, TimeUnit.SECONDS),Duration.of(10, TimeUnit.SECONDS))));
        assertEquals(model.getInstanceWindow(bID).get(0), (Window.between(Duration.of(5, TimeUnit.SECONDS),Duration.of(10, TimeUnit.SECONDS))));

        final var graph2 =
                EventGraph.sequentially(
                        atom(ActivityEvent.startActivity(anotherAID, mapper.serializeActivity(anotherA).get())),
                        atom(ActivityEvent.startActivity(anotherBID, mapper.serializeActivity(anotherB).get())));

        applicator.apply(model, graph2.evaluate(evaluator));

        var aWindows = model.getTypeWindows(mapper.serializeActivity(a).get().getTypeName());
        var bWindows = model.getTypeWindows(mapper.serializeActivity(b).get().getTypeName());
        assertEquals(aWindows.get(0), Window.between(Duration.of(5, TimeUnit.SECONDS),Duration.of(10, TimeUnit.SECONDS)));
        assertEquals(bWindows.get(0), Window.between(Duration.of(5, TimeUnit.SECONDS),Duration.of(10, TimeUnit.SECONDS)));
    }

    @Test
    public void typeNameTest() {
        final var model = new ActivityModel();

        model.activityStart("x1", new SerializedActivity("x", Map.of()));

        model.step(Duration.of(1, TimeUnit.MICROSECONDS));

        model.activityEnd("x1");
        model.activityStart("x2", new SerializedActivity("x", Map.of("a", SerializedParameter.of(10.0))));

        model.step(Duration.of(1, TimeUnit.MICROSECONDS));

        model.activityEnd("x2");
        model.activityStart("x3", new SerializedActivity("x", Map.of()));
        model.activityEnd("x3");

        final var windows = model.getTypeWindows("x");

        assertEquals(windows.get(0), Window.between(Duration.of(0, TimeUnit.MICROSECONDS),Duration.of(2, TimeUnit.MICROSECONDS)));
    }

    @Test
    public void typeWindowsTest() {
        final var model = new ActivityModel();

        model.step(Duration.of(1, TimeUnit.MICROSECONDS));

        model.activityStart("x1", new SerializedActivity("x", Map.of()));

        model.step(Duration.of(1, TimeUnit.MICROSECONDS));

        model.activityStart("x2", new SerializedActivity("x", Map.of()));

        model.step(Duration.of(1, TimeUnit.MICROSECONDS));

        model.activityEnd("x1");

        model.step(Duration.of(1, TimeUnit.MICROSECONDS));

        model.activityStart("x3", new SerializedActivity("x", Map.of()));

        model.step(Duration.of(1, TimeUnit.MICROSECONDS));

        model.activityEnd("x2");

        model.step(Duration.of(1, TimeUnit.MICROSECONDS));

        model.activityEnd("x3");

        final var windows = model.getTypeWindows("x");
        assertEquals(windows.get(0), Window.between(Duration.of(1, TimeUnit.MICROSECONDS),Duration.of(6, TimeUnit.MICROSECONDS)));
    }

    @Test
    public void collapseOverlappingWindowsTest() {
        var x = new ArrayList<Window>();

        x.add(Window.between(Duration.of(10, TimeUnit.MICROSECONDS), Duration.of(13, TimeUnit.MICROSECONDS)));
        x.add(Window.between(Duration.of(4, TimeUnit.MICROSECONDS), Duration.of(16, TimeUnit.MICROSECONDS)));
        x.add(Window.between(Duration.of(15, TimeUnit.MICROSECONDS), Duration.of(20, TimeUnit.MICROSECONDS)));
        x.add(Window.between(Duration.of(2, TimeUnit.MICROSECONDS), Duration.of(4, TimeUnit.MICROSECONDS)));
        x.add(Window.between(Duration.of(1, TimeUnit.MICROSECONDS), Duration.of(3, TimeUnit.MICROSECONDS)));
        x.add(Window.between(Duration.of(11, TimeUnit.MICROSECONDS), Duration.of(13, TimeUnit.MICROSECONDS)));

        var res = UtilityMethods.collapseOverlapping(x);
        assertEquals(res.get(0), Window.between(Duration.of(1, TimeUnit.MICROSECONDS),Duration.of(20, TimeUnit.MICROSECONDS)));
        assertEquals(res.size(), 1);
    }

    @Test
    public void collapseOverlappingWindowsTest2(){
        var x = new ArrayList<Window>();

        x.add(Window.between(Duration.of(2, TimeUnit.MICROSECONDS), Duration.of(5, TimeUnit.MICROSECONDS)));
        x.add(Window.between(Duration.of(1, TimeUnit.MICROSECONDS), Duration.of(3, TimeUnit.MICROSECONDS)));
        x.add(Window.between(Duration.of(4, TimeUnit.MICROSECONDS), Duration.of(6, TimeUnit.MICROSECONDS)));

        var res = UtilityMethods.collapseOverlapping(x);
        assertEquals(res.size(), 1);
    }
}
