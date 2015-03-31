package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.galagotools.utils.DateUtil;
import edu.umass.ciir.galagotools.utils.SGML;
import edu.umass.ciir.galagotools.utils.StrUtil;
import edu.umass.ciir.proteus.athena.linking.LinkingExperiment;

import java.util.regex.Pattern;

/**
 * @author jfoley.
 */
public class WikiCleaner {
  public static String removeReferences(String input) {
    input = SGML.removeTag(input, "ref");
    input = StrUtil.removeBetweenNested(input, "{{refbegin", "{{refend}}");
    return input;
  }

  public static String cleanWikiTags(String input) {
    return input.replaceAll("</?(onlyinclude|includeonly)>", "");
  }

  public static String killWikiTables(String input) {
    return StrUtil.removeBetweenNested(input, "{|", "|}");
  }

  public static String convertLinks(String input) {
    return convertExternalLinks(convertInternalLinks(input));
  }

  /** Do internal first [[ ]], then external [ ] */
  public static String convertExternalLinks(String input) {
    return StrUtil.transformBetween(input, Pattern.compile("\\["), Pattern.compile("\\]"), new StrUtil.Transform() {
      @Override
      public String process(String input) {
        String url = input;
        String text = "link";
        if(input.contains(" ")) {
          url = StrUtil.takeBefore(input, " ");
          text = StrUtil.takeAfter(input, " ");
        }
        return String.format("<a href=\"%s\">%s</a>", url, text);
      }
    });
  }

  /** Do internal first [[ ]], then external [ ] */
  public static String convertInternalLinks(String input) {
    return StrUtil.transformBetween(input, Pattern.compile("\\[\\["), Pattern.compile("\\]\\]"), new StrUtil.Transform() {
      @Override
      public String process(String input) {
        if(input.isEmpty()) return "";
        if(input.charAt(0) == ':') { // special category sort of link
          return "";
        } else if(input.startsWith("File:") || input.startsWith("Image:")) {
          return "";
        } else if(input.startsWith("Category:")) {
          return "<category>"+StrUtil.takeAfter(input, ":")+"</category>";
        }

        String url;
        String text;

        if(input.contains("|")) {
          url = StrUtil.takeBefore(input, "|");
          text = StrUtil.takeAfter(input, "|");
        } else {
          url = input;
          text = input;
        }

        if(DateUtil.isMonthDay(text))
          return text;

        return internalLink(url, text);
      }
    });
  }

  public static int getHeaderLevel(String input) {
    input = StrUtil.compactSpaces(input);
    if(!input.startsWith("=")) return 0;
    if(input.startsWith("====")) return 4;
    if(input.startsWith("===")) return 3;
    if(input.startsWith("==")) return 2;
    if(input.startsWith("=")) return 1;
    return 0;
  }
  public static String convertHeaders(String input) {
    return StrUtil.transformLines(input, new StrUtil.Transform() {
      @Override
      public String process(String input) {
        int headerLevel = getHeaderLevel(input);
        if(headerLevel <= 0) return input+'\n';
        String cleaned = StrUtil.compactSpaces(input.replace('=', ' '));
        return String.format("<h%d>%s</h%d>\n", headerLevel, cleaned, headerLevel);
      }
    });
  }

  public static String internalLink(String page, String text) {
    String url = page.replaceAll("\\s", "_");
    return String.format("<a href=\"https://en.wikipedia.org/wiki/%s\">%s</a>", url, text);
  }

  public static String stripWikiUrlToTitle(String url) {
    return LinkingExperiment.makeWikipediaTitle(StrUtil.removeFront(url, "https://en.wikipedia.org/wiki/"));
  }

  public static String unescapeAmpersandEscapes(String input) {
    return input.replaceAll("&(n|m)dash;", "-");
  }

  public static String clean(String input) {
    return clean("test", input);
  }

  public static String clean(String title, String input) {
    input = removeReferences(input);
    input = unescapeAmpersandEscapes(input);
    input = cleanWikiTags(input);
    input = killWikiTables(input);
    input = SGML.removeComments(input);
    input = input.replaceAll("'{2,3}", ""); // ditch all italics
    input = WikiTemplateHack.convertTemplates(title, input);
    input = convertLinks(input);
    return input;
  }
}
