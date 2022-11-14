Example Activity Mapper
=======================

Below is an example of an Activity Type and its Activity mapper for
reference:

Activity Type
~~~~~~~~~~~~~

.. code:: java

   @ActivityType("foo")
   public final class FooActivity {
     @Parameter
     public int x = 0;

     @Parameter
     public String y = "test";

     @Parameter
     public List<Vector3D> vecs = List.of(new Vector3D(0.0, 0.0, 0.0));

     @Validation("x cannot be exactly 99")
     public boolean validateX() {
       return (x != 99);
     }

     @Validation("y cannot be 'bad'")
     public boolean validateY() {
       return !y.equals("bad");
     }

     @EffectModel
     public void run(final Mission mission) {
       // ...
     }
   }

Generated Activity Mapper Example
---------------------------------

.. code:: java

   package gov.nasa.jpl.aerie.foomissionmodel.generated.activities;

   import gov.nasa.jpl.aerie.contrib.serialization.mappers.NullableValueMapper;
   import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
   import gov.nasa.jpl.aerie.foomissionmodel.Mission;
   import gov.nasa.jpl.aerie.foomissionmodel.activities.FooActivity;
   import gov.nasa.jpl.aerie.foomissionmodel.mappers.FooValueMappers;
   import gov.nasa.jpl.aerie.merlin.framework.ActivityMapper;
   import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
   import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
   import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
   import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
   import gov.nasa.jpl.aerie.merlin.protocol.model.InputType;
   import gov.nasa.jpl.aerie.merlin.protocol.model.OutputType;
   import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
   import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
   import gov.nasa.jpl.aerie.merlin.protocol.types.UnconstructableArgumentException;
   import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
   import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
   import java.util.ArrayList;
   import java.util.HashMap;
   import java.util.List;
   import java.util.Map;
   import java.util.Optional;
   import javax.annotation.processing.Generated;
   import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

   @Generated("gov.nasa.jpl.aerie.merlin.processor.MissionModelProcessor")
   public final class FooActivityMapper implements ActivityMapper<Mission, FooActivity, Unit> {
     private final Topic<FooActivity> inputTopic = new Topic<>();

     private final Topic<Unit> outputTopic = new Topic<>();

     @Override
     public InputType<FooActivity> getInputType() {
       return new InputMapper();
     }

     @Override
     public OutputType<Unit> getOutputType() {
       return new OutputMapper();
     }

     @Override
     public Topic<FooActivity> getInputTopic() {
       return this.inputTopic;
     }

     @Override
     public Topic<Unit> getOutputTopic() {
       return this.outputTopic;
     }

     @Override
     public Initializer.TaskFactory<Unit> getTaskFactory(final Mission model,
         final FooActivity activity) {
       return ModelActions.threaded(() -> {
         ModelActions.emit(activity, this.inputTopic);
         activity.run(model);
         ModelActions.emit(Unit.UNIT, this.outputTopic);
         return Unit.UNIT;
       });
     }

     @Generated("gov.nasa.jpl.aerie.merlin.processor.MissionModelProcessor")
     public final class InputMapper implements InputType<FooActivity> {
       private final ValueMapper<Integer> mapper_x;

       private final ValueMapper<String> mapper_y;

       private final ValueMapper<Integer> mapper_z;

       private final ValueMapper<List<Vector3D>> mapper_vecs;

       @SuppressWarnings("unchecked")
       public InputMapper() {
         this.mapper_x =
             BasicValueMappers.$int();
         this.mapper_y =
             new NullableValueMapper<>(
                 BasicValueMappers.string());
         this.mapper_z =
             new NullableValueMapper<>(
                 BasicValueMappers.$int());
         this.mapper_vecs =
             new NullableValueMapper<>(
                 BasicValueMappers.list(
                     FooValueMappers.vector3d(
                         BasicValueMappers.$double())));
       }

       @Override
       public List<String> getRequiredParameters() {
         return List.of();
       }

       @Override
       public ArrayList<InputType.Parameter> getParameters() {
         final var parameters = new ArrayList<InputType.Parameter>();
         parameters.add(new InputType.Parameter("x", this.mapper_x.getValueSchema()));
         parameters.add(new InputType.Parameter("y", this.mapper_y.getValueSchema()));
         parameters.add(new InputType.Parameter("z", this.mapper_z.getValueSchema()));
         parameters.add(new InputType.Parameter("vecs", this.mapper_vecs.getValueSchema()));
         return parameters;
       }

       @Override
       public Map<String, SerializedValue> getArguments(final FooActivity input) {
         final var arguments = new HashMap<String, SerializedValue>();
         arguments.put("x", this.mapper_x.serializeValue(input.x));
         arguments.put("y", this.mapper_y.serializeValue(input.y));
         arguments.put("z", this.mapper_z.serializeValue(input.z));
         arguments.put("vecs", this.mapper_vecs.serializeValue(input.vecs));
         return arguments;
       }

       @Override
       public FooActivity instantiate(final Map<String, SerializedValue> arguments) throws
           InstantiationException {
         final var template = new FooActivity();
         Optional<Integer> x = Optional.ofNullable(template.x);
         Optional<String> y = Optional.ofNullable(template.y);
         Optional<Integer> z = Optional.ofNullable(template.z);
         Optional<List<Vector3D>> vecs = Optional.ofNullable(template.vecs);

         final var instantiationExBuilder = new InstantiationException.Builder("foo");

         for (final var entry : arguments.entrySet()) {
           try {
             switch (entry.getKey()) {
               case "x":
                 x = Optional.ofNullable(template.x = this.mapper_x.deserializeValue(entry.getValue())
                     .getSuccessOrThrow(failure -> new UnconstructableArgumentException("x", failure)));
                 break;
               case "y":
                 y = Optional.ofNullable(template.y = this.mapper_y.deserializeValue(entry.getValue())
                     .getSuccessOrThrow(failure -> new UnconstructableArgumentException("y", failure)));
                 break;
               case "z":
                 z = Optional.ofNullable(template.z = this.mapper_z.deserializeValue(entry.getValue())
                     .getSuccessOrThrow(failure -> new UnconstructableArgumentException("z", failure)));
                 break;
               case "vecs":
                 vecs = Optional.ofNullable(template.vecs = this.mapper_vecs.deserializeValue(entry.getValue())
                     .getSuccessOrThrow(failure -> new UnconstructableArgumentException("vecs", failure)));
                 break;
               default:
                 instantiationExBuilder.withExtraneousArgument(entry.getKey());
             }
           } catch (final UnconstructableArgumentException e) {
             instantiationExBuilder.withUnconstructableArgument(e.parameterName, e.failure);
           }
         }

         x.ifPresentOrElse(
             value -> instantiationExBuilder.withValidArgument("x", this.mapper_x.serializeValue(value)),
             () -> instantiationExBuilder.withMissingArgument("x", this.mapper_x.getValueSchema()));
         y.ifPresentOrElse(
             value -> instantiationExBuilder.withValidArgument("y", this.mapper_y.serializeValue(value)),
             () -> instantiationExBuilder.withMissingArgument("y", this.mapper_y.getValueSchema()));
         z.ifPresentOrElse(
             value -> instantiationExBuilder.withValidArgument("z", this.mapper_z.serializeValue(value)),
             () -> instantiationExBuilder.withMissingArgument("z", this.mapper_z.getValueSchema()));
         vecs.ifPresentOrElse(
             value -> instantiationExBuilder.withValidArgument("vecs", this.mapper_vecs.serializeValue(value)),
             () -> instantiationExBuilder.withMissingArgument("vecs", this.mapper_vecs.getValueSchema()));

         instantiationExBuilder.throwIfAny();
         return template;
       }

       @Override
       public List<InputType.ValidationNotice> getValidationFailures(final FooActivity input) {
         final var notices = new ArrayList<InputType.ValidationNotice>();
         if (!input.validateX()) notices.add(new InputType.ValidationNotice(List.of("x"), "x cannot be exactly 99"));
         if (!input.validateY()) notices.add(new InputType.ValidationNotice(List.of("y"), "y cannot be 'bad'"));
         return notices;
       }
     }

     public static final class OutputMapper implements OutputType<Unit> {
       private final ValueMapper<Unit> computedAttributesValueMapper = BasicValueMappers.$unit();

       @Override
       public ValueSchema getSchema() {
         return this.computedAttributesValueMapper.getValueSchema();
       }

       @Override
       public SerializedValue serialize(final Unit returnValue) {
         return this.computedAttributesValueMapper.serializeValue(returnValue);
       }
     }
   }
