package gov.nasa.jpl.aerie.merlin.protocol;

public interface Querier<$Timeline> {
  <State> State getState(Query<? super $Timeline, ?, State> query);
}
