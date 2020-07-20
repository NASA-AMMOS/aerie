package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

public interface CumulableResource<Delta> {
  void add(Delta delta);

  default void add(GettableResource<Delta> delta) {
    this.add(delta.get());
  }
}
