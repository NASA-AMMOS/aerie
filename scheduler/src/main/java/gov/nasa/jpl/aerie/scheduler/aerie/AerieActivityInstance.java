package gov.nasa.jpl.aerie.scheduler.aerie;

import gov.nasa.jpl.aerie.scheduler.ActivityInstance;

import java.lang.reflect.Field;

public class AerieActivityInstance extends ActivityInstance {
    public AerieActivityInstance(String name, AerieActivityType type) {
        super(name, type);

        try {
            for (final Field field : type.getClass().getFields()) {
                if (field.getDeclaringClass().getSuperclass().equals(AerieActivityType.class)) {
                    Object param = field.get(type);
                    if (param != null) {
                        addParameter(field.getName(), param);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
