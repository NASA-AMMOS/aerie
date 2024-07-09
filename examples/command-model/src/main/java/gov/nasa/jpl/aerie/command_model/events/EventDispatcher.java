package gov.nasa.jpl.aerie.command_model.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.inContext;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.getName;

public class EventDispatcher<E> {
    // TODO - think about some way of logging the events themselves perhaps?
    //   Maybe we'd create a new topic (CellRef) for each event type and emit to it?
    //   Or, maybe we re-write the logging utilities themselves to use topics, and just log events?

    private final List<Consumer<E>> eventListeners = new ArrayList<>();

    public void registerEventListener(Consumer<E> listener) {
        eventListeners.add(listener);
    }

    public void registerEventListener(String name, Consumer<E> listener) {
        eventListeners.add(e -> inContext(name, () -> listener.accept(e)));
    }

    public void emit(E event) {
        inContext(getName(event, null), () -> eventListeners.forEach(listener -> listener.accept(event)));
    }
}
