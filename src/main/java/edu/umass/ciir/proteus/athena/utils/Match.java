package edu.umass.ciir.proteus.athena.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* @author jfoley.
*/
public final class Match {
  public final int begin;
  public final int end;
  public final Matcher matcher;

  public Match(Matcher matcher) {
    this.matcher = matcher;
    begin = matcher.start();
    end = matcher.end();
  }

  public static Match find(String input, Pattern pattern) {
    return find(input, pattern, 0);
  }
  public static Match find(String input, Pattern pattern, int start) {
    Matcher match = pattern.matcher(input);
    if(match.find(start)) {
      return new Match(match);
    }
    return null;
  }

  public String get(String parentStr) {
    return parentStr.substring(begin, end);
  }

  public String firstGroup() {
    return matcher.group(1);
  }
}
