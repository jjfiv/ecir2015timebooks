package edu.umass.ciir.galagotools.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

/**
 * @author jfoley.
 */
public class StrUtil {
  public static String removeBetween(String input, String start, String end) {
    StringBuilder text = new StringBuilder();
    int lastPos = 0;
    while(true) {
      int startPos = input.indexOf(start, lastPos);
      if(startPos == -1) break;
      int endPos = input.indexOf(end, startPos+start.length());
      if(endPos == -1) break;
      endPos += end.length();
      text.append(input.substring(lastPos, startPos));
      lastPos = endPos;
    }
    text.append(input.substring(lastPos));
    return text.toString();
  }

  public static String removeBetweenNested(String input, String start, String end) {
    StringBuilder text = new StringBuilder();
    int lastPos = 0;
    while(true) {
      int startPos = input.indexOf(start, lastPos);
      if(startPos == -1) break;
      int endPos = input.indexOf(end, startPos+start.length());
      if(endPos == -1) break;

      // check for nesting; remove largest matching start,end sequence
      while(true) {
        int nextStartPos = input.indexOf(start, startPos + start.length());
        if(nextStartPos == -1 || nextStartPos > endPos) {
          break;
        }
        int nextEndPos = input.indexOf(end, endPos+end.length());
        if(nextEndPos == -1) break;
        endPos = nextEndPos;
      }

      endPos += end.length();
      text.append(input.substring(lastPos, startPos));
      lastPos = endPos;
    }
    text.append(input.substring(lastPos));
    return text.toString();
  }

  /**
   * Calls transform on every string that exists between patterns start and end on input, and returns the result.
   */
  public static String transformRecursively(String input, Pattern start, Pattern end, Transform transform, boolean inclusive) {
    StringBuilder text = new StringBuilder();
    int lastPos = 0;

    boolean hasNested = false;
    while(true) {
      Match startMatch = Match.find(input, start, lastPos);
      if(startMatch == null) break;
      Match endMatch = Match.find(input, end, startMatch.end);
      if(endMatch == null) break;

      // check for nesting; do inner-most computation first
      while(true) {
        Match nextStartMatch = Match.find(input, start, startMatch.end);
        if(nextStartMatch == null || nextStartMatch.begin > endMatch.begin) {
          break;
        }
        hasNested = true;
        startMatch = nextStartMatch;
      }

      text.append(input.substring(lastPos, startMatch.begin));
      if(inclusive) {
        text.append(transform.process(input.substring(startMatch.begin, endMatch.end)));
      } else {
        text.append(transform.process(input.substring(startMatch.end, endMatch.begin)));
      }
      lastPos = endMatch.end;
    }
    text.append(input.substring(lastPos));

    // go again to grab the outer ones
    if(hasNested) {
      return transformRecursively(text.toString(), start, end, transform, inclusive);
    }
    return text.toString();
  }

  /**
   * Calls transform on every string that exists between patterns start and end on input, and returns the result.
   * Exclusive of the matching patterns
   */
  public static String transformBetween(String input, Pattern start, Pattern end, Transform transform) {
    return transformRecursively(input, start, end, transform, false);
  }

  /**
   * Calls transform on every string that exists between patterns start and end on input, and returns the result.
   * Inclusive of the matching patterns
   */
  public static String transformInclusive(String input, Pattern start, Pattern end, Transform transform) {
    return transformRecursively(input, start, end, transform, true);
  }


  public static String transformLines(String input, Transform transform) {
    StringBuilder output = new StringBuilder();
    BufferedReader reader = new BufferedReader(new StringReader(input));
    try {
      while(true) {
        String line = reader.readLine();
        if(line == null) break;
        output.append(transform.process(line));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IO.close(reader);
    }
    return output.toString();
  }

  public static String removeBetween(String input, Pattern start, Pattern end) {
    StringBuilder text = new StringBuilder();
    int lastPos = 0;
    while(true) {
      Match startMatch = Match.find(input, start, lastPos);
      if(startMatch == null) break;
      Match endMatch = Match.find(input, end, startMatch.end);
      if(endMatch == null) break;
      text.append(input.substring(lastPos, startMatch.begin));
      lastPos = endMatch.end;
    }
    text.append(input.substring(lastPos));
    return text.toString();
  }

  public static String takeBefore(String input, String delim) {
    int pos = input.indexOf(delim);
    if(pos == -1) {
      return input;
    }
    return input.substring(0, pos);
  }

  public static String takeAfter(String input, String delim) {
    int pos = input.indexOf(delim);
    if(pos == -1) {
      return input;
    }
    return input.substring(pos + delim.length());
  }

  public static String takeBetween(String input, String prefix, String suffix) {
    return takeAfter(takeBefore(input, suffix), prefix);
  }

  public static String preview(String input, int len) {
    if(input.length() < len) {
      return input;
    } else {
      return input.substring(0, len-2)+"..";
    }
  }

  public static String firstWord(String text) {
    for(int i=0; i<text.length(); i++) {
      if(Character.isWhitespace(text.charAt(i)))
        return text.substring(0,i);
    }
    return text;
  }

  public static boolean looksLikeInt(String str, int numDigits) {
    if(str.isEmpty()) return false;
    if(str.length() > numDigits)
      return false;
    for(char c : str.toCharArray()) {
      if(!Character.isDigit(c))
        return false;
    }
    return true;
  }

  public static String removeSpaces(String input) {
    StringBuilder output = new StringBuilder();
    for(char c : input.toCharArray()) {
      if(Character.isWhitespace(c))
        continue;
      output.append(c);
    }
    return output.toString();
  }

  public static String filterToAscii(String input) {
    StringBuilder ascii = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      if (input.codePointAt(i) <= 127) {
        ascii.append(input.charAt(i));
      }
    }
    return ascii.toString();
  }

  public static boolean isAscii(String input) {
    for (int i = 0; i < input.length(); i++) {
      if (input.codePointAt(i) > 127) {
        return false;
      }
    }
    return true;
  }

  /** Simplify input string in terms of spaces; all space characters -> ' ' and a maximum width of 1 space */
  public static String compactSpaces(String input) {
    StringBuilder sb = new StringBuilder();
    boolean lastWasSpace = true;
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i);
      if(Character.isWhitespace(ch)) {
        if(lastWasSpace) continue;
        sb.append(' ');
        lastWasSpace = true;
        continue;
      }
      lastWasSpace = false;
      sb.append(ch);
    }
    if(lastWasSpace) {
      return sb.toString().trim();
    }
    return sb.toString();
  }

  /** Remove ending from input string */
  public static String removeBack(String input, String suffix) {
    if(!input.endsWith(suffix)) return input;
    return input.substring(0, input.length() - suffix.length());
  }

  /** Remove prefix from input string */
  public static String removeFront(String input, String prefix) {
    if(!input.startsWith(prefix)) return input;
    return input.substring(prefix.length());
  }

  /** Remove prefix and suffix from input string */
  public static String removeSurrounding(String input, String prefix, String suffix) {
    if(!input.endsWith(suffix)) return removeFront(input, prefix);
    if(!input.startsWith(prefix)) return removeBack(input, suffix);
    return input.substring(prefix.length(), input.length() - suffix.length());
  }

  public static boolean containsAscii(String title) {
    for (int i = 0; i < title.length(); i++) {
      int code = title.codePointAt(i);
      if(code < 128) {
        return true;
      }
    }
    return false;
  }

  public static interface Transform {
    public String process(String input);
  }

  public static String[] pretendTokenize(String input) {
    String cleaned = input
      .toLowerCase()
      .replaceAll("<script[^>]*>[^<]*</script>", " ")
      .replaceAll("<style[^>]*>[^<]*</style>", " ")
      .replaceAll("<!--.*-->", "")
      .replaceAll("&nbsp;", " ")
      .replaceAll("<[^>]*>", " ")
      .replaceAll("\\p{Punct}", " ")
      .replace(']', ' ')
      .replace('?', ' ')
      .replaceAll("^\\p{Alnum}", " ");
    return StrUtil.filterToAscii(cleaned).split("\\s+");
  }
}
