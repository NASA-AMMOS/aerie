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
        units.put(lastParam, splitComment.substring(splitComment.indexOf(UNITS_TAG) + UNITS_TAG.length()));
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
      units.put(parameter, comment.substring(comment.indexOf(UNITS_TAG) + UNITS_TAG.length()));
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
}
