package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class CreateTimelineSegmentAction implements AutoCloseable {
  private static final @Language("SQL") String createSegment = """
    insert into timeline_segment
        (simulation_dataset_id, simulation_time)
        values (?, ?)
        returning id;
  """;

  private static final @Language("SQL") String createEvent = """
    insert into lifecycle_event
        (timeline_segment_id, activity_id, event)
        values (?, ?, ?);
  """;

  private static final @Language("SQL") String insertProfile = """
    insert into profile_segment
        (dataset_id, profile_id, timeline_segment_id, start_offset, dynamics)
        values (?, ?, ?, ?, ?);
  """;

  private final Connection con;
  private final PreparedStatement segmentStatement;
  private final PreparedStatement eventStatement;
  private final PreparedStatement profileStatment;

  public CreateTimelineSegmentAction(final Connection connection) throws SQLException {
    this.con = connection;
    this.segmentStatement = connection.prepareStatement(createSegment);
    this.eventStatement = connection.prepareStatement(createEvent);
    this.profileStatment = connection.prepareStatement(insertProfile);
  }

  public long apply(TimelineSegment timelineSegment) throws SQLException {
    // insert everything in one transaction, so that Hasura doesn't
    // stream it out too soon
    this.con.setAutoCommit(false);

    this.segmentStatement.setLong(1, timelineSegment.simulationDatasetId());
    this.segmentStatement.setString(2, timelineSegment.simulationTime());

    final var results = this.segmentStatement.executeQuery();

    if (!results.next()) throw new FailedInsertException("timeline_segment");
    long timelineSegmentId = results.getLong(1);

    this.con.commit();

    return timelineSegmentId;
  }

  @Override
  public void close() throws SQLException {
    this.segmentStatement.close();
    this.eventStatement.close();
    this.profileStatment.close();
    this.con.close();
  }
}
