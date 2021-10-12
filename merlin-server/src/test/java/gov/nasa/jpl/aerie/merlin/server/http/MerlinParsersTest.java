package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraAction;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraMissionModelEvent;
import gov.nasa.jpl.aerie.merlin.server.services.CreateSimulationMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.createSimulationMessageP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.hasuraActivityActionP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.hasuraAdaptationActionP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.hasuraMissionModelEventTriggerP;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsersTest.NestedLists.nestedList;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;
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
        recursiveP((JsonParser<NestedLists> self) -> listP(self).map(Iso.of(NestedLists::new, $ -> $.lists)));

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
        . add("configuration", JsonValue.EMPTY_JSON_OBJECT)
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
        ),
        Map.of()
    );

    assertThat(
        createSimulationMessageP.parse(json).getSuccessOrThrow()
    ).isEqualTo(
        expected
    );
  }

  @Test
  public void testSerializedReal() {
    final var expected = SerializedValue.of(3.14);
    final var actual = serializedValueP.parse(Json.createValue(3.14)).getSuccessOrThrow();

    assertThat(expected).isEqualTo(actual);
  }

  @Test
  public void testRealIsNotALong() {
    assertThat(longP.parse(Json.createValue(3.14))).matches(JsonParseResult::isFailure);
  }

  @Test
  public void testHasuraActionParsers() {
    {
      final var json = Json
          .createObjectBuilder()
          .add("action", Json
              .createObjectBuilder()
              .add("name", "testAction")
              .build())
          .add("input", Json
              .createObjectBuilder()
              .add("adaptationId", "1")
              .build())
          .add("session_variables", Json
              .createObjectBuilder()
              .add("x-hasura-role", "admin")
              .build())
          .build();

      final var expected = new HasuraAction<>(
          "testAction",
          new HasuraAction.AdaptationInput("1"),
          new HasuraAction.Session("admin", ""));

      assertThat(hasuraAdaptationActionP.parse(json).getSuccessOrThrow()).isEqualTo(expected);
    }

    {
      final var json = Json
          .createObjectBuilder()
          .add("action", Json
              .createObjectBuilder()
              .add("name", "testAction")
              .build())
          .add("input", Json
              .createObjectBuilder()
              .add("adaptationId", "1")
              .build())
          .add("session_variables", Json
              .createObjectBuilder()
              .add("x-hasura-role", "admin")
              .add("x-hasura-user-id", "userId")
              .build())
          .build();

      final var expected = new HasuraAction<>(
          "testAction",
          new HasuraAction.AdaptationInput("1"),
          new HasuraAction.Session("admin", "userId"));

      assertThat(hasuraAdaptationActionP.parse(json).getSuccessOrThrow()).isEqualTo(expected);
    }

    {
      final var json = Json
          .createObjectBuilder()
          .add("action", Json
              .createObjectBuilder()
              .add("name", "testAction")
              .build())
          .add("input", Json
              .createObjectBuilder()
              .add("adaptationId", "1")
              .add("activityTypeId", "42")
              .build())
          .add("session_variables", Json
              .createObjectBuilder()
              .add("x-hasura-role", "admin")
              .build())
          .build();

      final var expected = new HasuraAction<>(
          "testAction",
          new HasuraAction.ActivityInput("1", "42"),
          new HasuraAction.Session("admin", ""));

      assertThat(hasuraActivityActionP.parse(json).getSuccessOrThrow()).isEqualTo(expected);
    }
  }

  @Test
  public void testHasuraMissionModelEventParser() {
    final var json = Json
        .createObjectBuilder()
        .add("event", Json
            .createObjectBuilder()
            .add("data", Json
            .createObjectBuilder()
                .add("new", Json
                    .createObjectBuilder()
                    .add("id", 1)
                    .build())
                .add("old", JsonValue.NULL)
                .build())
            .add("op", "INSERT")
            .build())
        .add("id", "8907a407-28a5-440a-8de6-240b80c58a8b")
        .build();

    final var expected = new HasuraMissionModelEvent("1");

    assertThat(hasuraMissionModelEventTriggerP.parse(json).getSuccessOrThrow()).isEqualTo(expected);
  }
}
