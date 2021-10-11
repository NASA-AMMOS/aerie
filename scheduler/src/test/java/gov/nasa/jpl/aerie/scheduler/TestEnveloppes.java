package gov.nasa.jpl.aerie.scheduler;

import org.junit.jupiter.api.Test;

import java.util.List;

public class TestEnveloppes {
  @Test
  public void testEnveloppes() {

    var horizon = TimeWindows.of(List.of(new Range<Time>(new Time(0), new Time(20))));

    Range<Time> r1 = new Range<Time>(new Time(1), new Time(10));
    Range<Time> r2 = new Range<Time>(new Time(12), new Time(20));

    var resetExpr = new TimeRangeExpression.Builder().from(TimeWindows.of(List.of(r1, r2))).build();


    Range<Time> r5 = new Range<Time>(new Time(0), new Time(3));
    Range<Time> r6 = new Range<Time>(new Time(2), new Time(4));
    Range<Time> r3 = new Range<Time>(new Time(6), new Time(11));
    Range<Time> r4 = new Range<Time>(new Time(3), new Time(7));

    var firstType = new TimeRangeExpression.Builder().from(TimeWindows.of(List.of(r4, r6))).build();

    var secondType = new TimeRangeExpression.Builder().from(TimeWindows.of(List.of(r3, r5))).build();


    var enveloppe = new Transformers.EnveloppeBuilder()
        .withinEach(resetExpr)
        .when(firstType)
        .when(secondType)
        .build();

    TimeRangeExpression tre = new TimeRangeExpression.Builder()
        .from(resetExpr)
        .thenTransform(enveloppe)
        .name("encounter_envelopper_TRE")
        .build();

    System.out.println(tre.computeRange(null, horizon));

  }


}


