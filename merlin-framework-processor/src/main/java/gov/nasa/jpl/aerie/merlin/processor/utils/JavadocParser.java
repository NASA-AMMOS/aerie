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
  private static final String PARAMETERS_TAG = "@param";
  private static final String RESOURCE_TYPE_TAG = "@registeredState";
  private static final String UNITS_TAG = "@unit";

  /**
   * Parses a block comment searching for param + unit paris. This would be more common
   * on a record type.
   * @param elementUtils Utility functions for file parsing.
   * @param element The element we're parsing the comments for.
   * @return A map of parameters or resource types to their associated unit.
   */
  public static Map<String, String> parseUnitsFromJavadocs(Elements elementUtils, TypeElement element) {
    final var parsedTags = new HashMap<String, String>();

    // Parse the class javadoc.
    Optional.ofNullable(elementUtils.getDocComment(element))
        .map(JavadocParser::removeSingleLeadingSpaceFromEachLine)
        .map(comment -> {
          // Keep track of the last param or resource type that we came across.
          var lastItem = "";

          for (var splitComment : comment.split("\n")) {
            // The item being annotated is either a parameter or a resource type.
            if (splitComment.contains(PARAMETERS_TAG)) {
              lastItem = splitComment.split(" ")[1];
            } else if (splitComment.contains(RESOURCE_TYPE_TAG)) {
              lastItem = splitComment.split(" ")[1];
            }

            if (!lastItem.isEmpty() && splitComment.contains(UNITS_TAG)) {
              parsedTags.put(lastItem, extractTagValue(UNITS_TAG, splitComment));
            }
          }

          return comment;
        });

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

          if (value.contains(UNITS_TAG)) {
            // Prepend the registered state with a "/" as it's removed from the property name.
            if (value.contains(RESOURCE_TYPE_TAG)) {
              property = "/" + property;
            }

            parsedTags.put(property, extractTagValue(UNITS_TAG, value));
          }
        });

    return parsedTags;
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
   * @param tag The tag to search for.
   * @param comment The entire comment string that we're parsing the tag value from.
   * @return The comment value after the found tag.
   */
  private static String extractTagValue(String tag, String comment) {
    if (comment.isEmpty() || !comment.contains(tag)) {
      return "";
    }

    return comment.substring(comment.indexOf(tag) + tag.length()).trim();
  }
}
