package gov.nasa.jpl.aerie.services.plan.http;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.services.plan.services.CreateSimulationMessage;
import gov.nasa.jpl.aerie.time.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import javax.json.Json;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.services.plan.http.MerlinParsers.createSimulationMessageP;
import static gov.nasa.jpl.aerie.services.plan.http.MerlinParsersTest.NestedLists.nestedList;
import static org.assertj.core.api.Assertions.assertThat;

public final class MerlinParsersTest {
  public static final class NestedLists {
    public final List<NestedLists> lists;

    public NestedLists(final List<NestedLists> lists) {
      this.lists = lists;
    }

    @Override
    public boolean equals(final Object obj) {
      if (!(obj instanceof NestedLists)) return false;
      final var other = (NestedLists) obj;
      return Objects.equals(this.lists, other.lists);
    }

    @Override
    public int hashCode() {
      return this.lists.hashCode();
    }

    public static NestedLists nestedList(NestedLists... lists) {
      return new NestedLists(List.of(lists));
    }
  }

  @Test
  public void testRecursiveList() {
    final var listsP =
        recursiveP((JsonParser<NestedLists> self) -> listP(self).map(NestedLists::new));

    final var foo = Json
        . createArrayBuilder()
        . add(Json
            . createArrayBuilder()
            . add(Json.createArrayBuilder().build())
            . build())
        . add(Json
            . createArrayBuilder()
            . add(Json.createArrayBuilder().build())
            . add(Json.createArrayBuilder().build())
            . add(Json.createArrayBuilder().build())
            . build())
        . build();

    assertThat(
        listsP.parse(foo).getSuccessOrThrow()
    ).isEqualTo(
        nestedList(
            nestedList(nestedList()),
            nestedList(nestedList(), nestedList(), nestedList()))
    );
  }

  @Test
  public void testCreateSimulationMessageParser() {
    final var json = Json
        . createObjectBuilder()
        . add("adaptationId", "hello")
        . add("startTime", "1992-224T01:30:00")
        . add("samplingDuration", 5_000_000 /* microseconds */)
        . add("activities", Json
            . createObjectBuilder()
            . add("0", Json
                . createObjectBuilder()
                . add("type", "BiteBanana")
                . add("defer", 10_000_000 /* microseconds */)
                . add("parameters", Json
                    . createObjectBuilder()
                    . add("biteSize", 10.0)
                    . add("complex", Json
                        . createObjectBuilder()
                        . add("int", 10)
                        . add("str", "hi")
                        . build())
                    . build())
                . build())
            . build())
        . build();

    final var expected = new CreateSimulationMessage(
        "hello",
        Instant.parse("1992-08-11T01:30:00Z"),
        Duration.of(5, Duration.SECONDS),
        Map.of(
            "0", Pair.of(
                Duration.of(10, Duration.SECONDS),
                new SerializedActivity("BiteBanana", Map.of(
                    "biteSize", SerializedValue.of(10.0),
                    "complex", SerializedValue.of(Map.of(
                        "int", SerializedValue.of(10),
                        "str", SerializedValue.of("hi")))
                ))
            )
        )
    );

    assertThat(
        createSimulationMessageP.parse(json).getSuccessOrThrow()
    ).isEqualTo(
        expected
    );
  }
}
