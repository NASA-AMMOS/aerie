package gov.nasa.jpl.aerie.merlin.processor.utils;

import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JavadocParser {
  private static final String COMPUTED_ATTRIBUTE_TAG = "@computedAttribute";
  private static final String RESOURCE_TYPE_TAG = "@resourceName";
  private static final String UNITS_TAG = "@unit";

  /**
   * Parses javadocs searching for resource type + unit paris.
   * @param elementUtils Utility functions for file parsing.
   * @param element The element we're parsing the comments for.
   * @return A map of resource types to their associated unit.
   */
  public static Map<String, String> parseMissionModelUnits(Elements elementUtils, TypeElement element) {
    final var parsedUnits = new HashMap<String, String>();

    // Parse the class javadoc.
    Optional.ofNullable(elementUtils.getDocComment(element))
        .map(JavadocParser::removeSingleLeadingSpaceFromEachLine)
        .map(comment -> {
          parseComment(comment, parsedUnits, RESOURCE_TYPE_TAG);

          return comment;
        });

    element
        .getEnclosedElements()
        .stream()
        .flatMap(e -> Optional
            .ofNullable(elementUtils.getDocComment(e))
            .map(JavadocParser::removeSingleLeadingSpaceFromEachLine)
            .map(comment -> Pair.of(e.getSimpleName().toString(), comment))
            .stream())
        .forEach($ -> {
          final var value = $.getValue();
          var property = $.getKey();

          if (value.contains(UNITS_TAG)) {
            // Prepend the registered state with a "/" as it's removed from the property name.
            if (value.contains(RESOURCE_TYPE_TAG)) {
              property = "/" + property;
            }

            parsedUnits.put(property, extractTagValue(value));
          }
        });

    return parsedUnits;
  }

  /**
   * Parses javadocs searching for parameters + unit paris and computed attributes + unit pairs.
   * @param elementUtils Utility functions for file parsing.
   * @param element The element we're parsing the comments for.
   * @return A pair of maps, one with the parameter units and one with the computed attribute units.
   */
  public static Pair<Map<String, String>, Map<String, String>> parseActivityTypeUnits(
      Elements elementUtils,
      TypeElement element
  ) {
    final var parameterUnits = new HashMap<String, String>();
    final var computedAttributeUnits = new HashMap<String, String>();

    // Parse the parameter javadocs.
    element
        .getEnclosedElements()
        .stream()
        .flatMap(e -> Optional
            .ofNullable(elementUtils.getDocComment(e))
            .map(JavadocParser::removeSingleLeadingSpaceFromEachLine)
            .map(comment -> Pair.of(e.getSimpleName().toString(), comment))
            .stream())
        .forEach($ -> {
          final var value = $.getValue();
          var property = $.getKey();

          if (value.contains(UNITS_TAG) && !value.contains(COMPUTED_ATTRIBUTE_TAG)) {
            parameterUnits.put(property, extractTagValue(value));
          }
        });

    // Parse the computed attributes
    element
        .getEnclosedElements()
        .stream()
        // Computed Attribute units will be documented a record.
        .filter(e -> e.getKind() == ElementKind.RECORD)
        .flatMap(e -> Optional
            .ofNullable(elementUtils.getDocComment(e))
            .map(JavadocParser::removeSingleLeadingSpaceFromEachLine)
            .map(comment -> Pair.of(e.getSimpleName().toString(), comment))
            .stream())
        .forEach($ -> {
          final var comment = $.getValue();

          parseComment(comment, computedAttributeUnits, COMPUTED_ATTRIBUTE_TAG);
        });

    return Pair.of(parameterUnits, computedAttributeUnits);
  }

  /**
   * It is common for Javadoc to be written with every line indented by one space.
   * This method removes that space, if it exists, from every line.
   */
  public static String removeSingleLeadingSpaceFromEachLine(final String s) {
    final var lines = new ArrayList<String>();
    for (final var line : s.split("\n")){
      if (line.startsWith(" ")) {
        lines.add(line.substring(1));
      } else {
        lines.add(line);
      }
    }
    return String.join("\n", lines);
  }

  /**
   * Extracts the tag value from a given tag and comment.
   * Ex: "@unit mass in grams" will return "mass in grams".
   *
   * @param comment The entire comment string that we're parsing the tag value from.
   * @return The comment value after the found tag.
   */
  private static String extractTagValue(String comment) {
    if (!comment.contains(JavadocParser.UNITS_TAG)) {
      return "";
    }

    return comment.substring(comment.indexOf(JavadocParser.UNITS_TAG) + JavadocParser.UNITS_TAG.length()).trim();
  }

  private static void parseComment(String comment, Map<String, String> parsedUnits, String tag) {
    // Keep track of the last computed attribute that we came across.
    var lastItem = "";

    for (var splitComment : comment.split("\n")) {
      if (splitComment.contains(tag)) {
        lastItem = splitComment.split(" ")[1];
      }

      if (!lastItem.isEmpty() && splitComment.contains(UNITS_TAG)) {
        parsedUnits.put(lastItem, extractTagValue(splitComment));
      }
    }
  }
}
