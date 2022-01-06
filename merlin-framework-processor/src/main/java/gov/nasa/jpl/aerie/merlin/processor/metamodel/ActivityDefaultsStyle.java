package gov.nasa.jpl.aerie.merlin.processor.metamodel;

/**
 * An activity defaults "style" refers to how an activity's
 * default arguments have been defined within the mission model.
 */
public enum ActivityDefaultsStyle {
  AllDefined,            // All default arguments provided within @Parameter annotations
  AllStaticallyDefined,  // All default arguments provided within @Template static method
  SomeStaticallyDefined, // Some arguments provided within @WithDefaults static class
  NoneDefined            // No default arguments provided
}
