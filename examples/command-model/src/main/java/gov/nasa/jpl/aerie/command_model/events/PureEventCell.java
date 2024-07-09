package gov.nasa.jpl.aerie.command_model.events;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

// The idea of this class was to use it for the event dispatcher, but I couldn't quite figure it out.
// In particular, I couldn't see an easy way to "listen" to the events themselves on the topic.
// Conditions can monitor the state of the cell, and thus indirectly monitor the events, but it's awkward at best.

/**
 * Represents a cell storing no actual state, used instead to emit "pure events" of type E.
 *
 * @see {@link gov.nasa.jpl.aerie.merlin.framework.Registrar#topic}
 */
public class PureEventCell<E> {
    private final CellRef<E, Unit> cell;

    public PureEventCell() {
        cell = CellRef.allocate(Unit.UNIT, new CellType<>() {
            @Override
            public EffectTrait<E> getEffectType() {
                return new EffectTrait<>() {
                    @Override
                    public E empty() {
                        return null;
                    }

                    @Override
                    public E sequentially(E prefix, E suffix) {
                        return null;
                    }

                    @Override
                    public E concurrently(E left, E right) {
                        return null;
                    }
                };
            }

            @Override
            public Unit duplicate(Unit unit) {
                return unit;
            }

            @Override
            public void apply(Unit unit, E e) {}
        });
    }

    public Topic<E> topic() {
        return cell.topic;
    }
}
