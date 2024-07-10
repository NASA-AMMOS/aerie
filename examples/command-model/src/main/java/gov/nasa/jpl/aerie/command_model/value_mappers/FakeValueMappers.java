package gov.nasa.jpl.aerie.command_model.value_mappers;

import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;

public final class FakeValueMappers {
    private FakeValueMappers() {}

    public static ValueMapper<Runnable> runnable() {
        return new RunnableValueMapper();
    }
}
