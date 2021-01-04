package gov.nasa.jpl.aerie.apgen.parser.utilities;

public class ParsingUtilities {
    public static String removeComment(String src) {
        if (src.length() == 0) return src;

        boolean in_quotes = false;
        boolean nextCharEscaped = false;
        boolean prevCharSlash = false;

        for (int i=0; i< src.length(); i++) {
            char curr = src.charAt(i);
            if (in_quotes) {
                if (curr == '\\') {
                    nextCharEscaped = !nextCharEscaped;
                } else {
                    if (curr =='"' && !nextCharEscaped) {
                        in_quotes = false;
                    }
                    nextCharEscaped = false;
                }
            } else {
                if (curr == '/') {
                    if (prevCharSlash) {
                        return src.substring(0, i-1);
                    } else {
                        prevCharSlash = true;
                    }
                } else {
                    prevCharSlash = false;
                    if (curr == '"') {
                        in_quotes = true;
                    } else if (curr == '#') {
                        return src.substring(0, i);
                    }
                }
            }
        }
        return src;
    }
}
