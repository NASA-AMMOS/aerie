package gov.nasa.jpl.aerie.merlin.processor.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JavadocParser {
  private static final String PARAMETERS_TAG = "@param";

  private static final String UNITS_TAG = "@unit";

  /**
   * Parses a block comment searching for param + unit paris. This would be more common
   * on a record type.
   * @param comment The block jsdoc comment to parse.
   * @return A map of parameters to their associated unit.
   */
  public static Map<String, String> parseUnitsFromBlockComment(final String comment) {
    final var units = new HashMap<String, String>();
    // Keep track of the last parameter we saw, so we know what the units tag is referring to.
    var lastParam = "";

    for (var splitComment : comment.split("\n")) {
      if (splitComment.contains(PARAMETERS_TAG)) {
        lastParam = splitComment.split(" ")[1];
      }

      // If we've come across a parameter, and then we see a units tag, keep track of it.
      if (!lastParam.equals("") && splitComment.contains(UNITS_TAG)) {
        units.put(lastParam, extractTagValue(UNITS_TAG, splitComment));
      }
    }

    return units;
  }

  /**
   * Parses the units from a comment for a specific parameter.
   * @param parameter The name of the parameter that has been commented.
   * @param comment The comment text that has been parsed.
   * @return A map of parameters to their associated unit.
   */
  public static Map<String, String> parseUnitsFromParameterComment(final String parameter, final String comment) {
    final var units = new HashMap<String, String>();

    if (comment.contains(UNITS_TAG)) {
      units.put(parameter, extractTagValue(UNITS_TAG, comment));
    }

    return units;
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
    if (comment.equals("") || !comment.contains(tag)) {
      return "";
    }

    return comment.substring(comment.indexOf(tag) + tag.length()).trim();
  }
}
