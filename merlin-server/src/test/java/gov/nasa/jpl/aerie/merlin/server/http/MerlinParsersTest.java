package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraAction;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraActivityDirectiveEvent;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraMissionModelEvent;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
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
import static gov.nasa.jpl.aerie.merlin.server.http.HasuraParsers.*;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsersTest.NestedLists.nestedList;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.pgTimestampP;
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
        recursiveP((JsonParser<NestedLists> self) -> listP(self).map(NestedLists::new, $ -> $.lists));

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
              .add("missionModelId", "1")
              .build())
          .add("session_variables", Json
              .createObjectBuilder()
              .add("x-hasura-role", "admin")
              .build())
          .add("request_query", "query { someValue }")
          .build();

      final var expected = new HasuraAction<>(
          "testAction",
          new HasuraAction.MissionModelInput("1"),
          new HasuraAction.Session("admin", ""));

      assertThat(hasuraMissionModelActionP.parse(json).getSuccessOrThrow()).isEqualTo(expected);
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
              .add("missionModelId", "1")
              .build())
          .add("session_variables", Json
              .createObjectBuilder()
              .add("x-hasura-role", "admin")
              .add("x-hasura-user-id", "userId")
              .build())
          .add("request_query", "query { someValue }")
          .build();

      final var expected = new HasuraAction<>(
          "testAction",
          new HasuraAction.MissionModelInput("1"),
          new HasuraAction.Session("admin", "userId"));

      assertThat(hasuraMissionModelActionP.parse(json).getSuccessOrThrow()).isEqualTo(expected);
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

  @Test
  public void testHasuraActivityDirectiveEventParser() {
    final var now = new Timestamp(Instant.now());

    final var json = Json
        .createObjectBuilder()
        .add("event", Json
            .createObjectBuilder()
            .add("data", Json
                .createObjectBuilder()
                .add("new", Json
                    .createObjectBuilder()
                    .add("plan_id", 1)
                    .add("id", 1)
                    .add("type", "Test")
                    .add("arguments", Json.createObjectBuilder().add("A", 42).build())
                    .add("last_modified_arguments_at", pgTimestampP.unparse(now))
                    .build())
                .add("old", JsonValue.NULL)
                .build())
            .add("op", "INSERT")
            .build())
        .add("id", "8907a407-28a5-440a-8de6-240b80c58a8b")
        .build();

    final var expected = new HasuraActivityDirectiveEvent(new PlanId(1), new ActivityDirectiveId(1), "Test", Map.of("A", SerializedValue.of(42)), now);

    assertThat(hasuraActivityDirectiveEventTriggerP.parse(json).getSuccessOrThrow()).isEqualTo(expected);
  }
}
