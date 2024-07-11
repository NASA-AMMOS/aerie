package gov.nasa.jpl.aerie.command_model.events;

import gov.nasa.jpl.aerie.contrib.streamline.debugging.SimpleLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.inContext;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.getName;

public class EventDispatcher<E> {
    private final List<Consumer<E>> eventListeners = new ArrayList<>();
    private final SimpleLogger logger;

    public EventDispatcher(SimpleLogger logger) {
        this.logger = logger;
    }

    public void registerEventListener(Consumer<E> listener) {
        eventListeners.add(listener);
    }

    public void registerEventListener(String name, Consumer<E> listener) {
        eventListeners.add(e -> inContext(name, () -> listener.accept(e)));
    }

    public void emit(E event) {
        String eventName = getName(event, null);
        logger.log(eventName);
        inContext(eventName, () -> eventListeners.forEach(listener -> listener.accept(event)));
    }
}
