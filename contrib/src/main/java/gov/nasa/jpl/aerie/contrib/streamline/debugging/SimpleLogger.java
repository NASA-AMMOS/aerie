package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.string;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Unit.UNIT;

public class SimpleLogger {
    private final CellRef<String, Unit> cellRef = CellRef.allocate(UNIT, new CellType<>() {
        @Override
        public EffectTrait<String> getEffectType() {
            return new EffectTrait<>() {
                @Override
                public String empty() {
                    return null;
                }

                @Override
                public String sequentially(String prefix, String suffix) {
                    return null;
                }

                @Override
                public String concurrently(String left, String right) {
                    return null;
                }
            };
        }

        @Override
        public Unit duplicate(Unit unit) {
            return unit;
        }

        @Override
        public void apply(Unit unit, String s) {
        }
    });

    public SimpleLogger(String name, Registrar registrar) {
        registrar.topic(name, cellRef, string());
    }

    public void log(String message) {
        cellRef.emit(message);
    }
}
