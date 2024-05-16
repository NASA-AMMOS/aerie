package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

public class Warnings {
  public final CellRef<String, String> ref;

  public Warnings() {
    this.ref = CellRef.allocate("", new CellType<String , String>() {
      @Override
      public EffectTrait<String> getEffectType() {
        return new EffectTrait<String>() {
          @Override
          public String empty() {
            return "";
          }

          @Override
          public String sequentially(final String prefix, final String suffix) {
            return "";
          }

          @Override
          public String concurrently(final String left, final String right) {
            return "";
          }
        };
      }

      @Override
      public String duplicate(final String s) {
        return "";
      }

      @Override
      public void apply(final String s, final String o) {

      }
    });
  }

  public void log(String message) {
    this.ref.emit(message);
  }
}
