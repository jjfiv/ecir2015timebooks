package edu.umass.ciir.proteus.athena.wiki;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author jfoley
 */
public class WikiTemplateHack {
  public static final Pattern templateStart = Pattern.compile("\\{\\{");
  public static final Pattern templateEnd = Pattern.compile("\\}\\}");

  public static final String[] citationTemplates = {
    "citation", "cite book", "cite web", "cite news", "cite journal", "cite encyclopedia", "cite press release", "cite video", "cite episode", "cite magazine", "cite work",
    "citebook", "citeweb", "citenews", "citejournal", "citeenclyclopedia", "citepressrelease", "citevideo", "citeepisode", "citemagazine",
    "cite hansard",
    "un document", // UN Document
    "harv", "harvnb", "harvtxt", "harvard citation no brackets", "harvard citation", "harvard citation text", "harvcol", "harvcolnb", "harvs",
    "shortened footnote template", "sfn", "sfnp", "sfnm",
    "webcite",
  };

  public static final String[] citationNeededTemplates = {
    "where", "verify", "clarify", "why?", "fact", "dubious", "cn", "citation needed", "nomention",
    "verify credibility", "by whom", "which", "when",
    "unreferenced",
    "which?", "vague", "dn", "disambiguation needed", "who", "specify",
    "as of", "as of?",
  };

  public static final String[] ignoredTemplates = {
    "numberdis",
    "pp-move-indef", "dablink",
    "good article",
    "largethumb",
    "for",
    "redirect",
    "about",
    "short pages monitor",
    "reflist",
    "portal", "portal bar",
    "use mdy dates", "use dmy dates",
    "defaultsort",
    "further",
    "main",
    "lang",
    "empty section",
    "events by month links",
    "tooltip",
    "year dab", "year in other calendars", "year nav",
    "r_to_decade", "r to section",
    "m1 year in topic",
    "-", "clear left",
    "fr icon", "lt icon", "es icon", "sp icon", "ru icon",
    "nl", // note language
    "wayback", "waybackdate", // wayback machine links
    "sfn", // short footnote notation
    "see also",
    "year-stub",
    "oedsub", "odnbsub", //uk library subject
    "dead link",
    "commonscat", "wikinewscat", "commons and category", "wikinews category", "commons category-inline",
    "wikiquote"
  };

  public static String[] ignoredStartsWith = {
    "defaultsort",
    "lang-",
    "link ",
    "infobox"
  };

  public static String[] ignoredContains = {
    "year in topic",
  };

  public static final String[] shipTemplates = {
    "mv", "hms", "ss", "uss", "rms", "ps", "ms", "hmas"
  };

  public static final Set<String> ignoredTemplateSet = new HashSet<String>(Arrays.asList(ignoredTemplates));
  public static final Set<String> shipTemplateSet = new HashSet<String>(Arrays.asList(shipTemplates));

  public static Map<String,String> templateArgs(String[] split) {
    TreeMap<String,String> args = new TreeMap<String, String>();
    // skip the first one because it's the name of the template
    for (int i=1; i<split.length; i++) {
      String key = StrUtil.takeBefore(split[i], "=");
      String value = StrUtil.takeAfter(split[i], "=");
      args.put(key, value);
    }
    return args;
  }

  public static String processStylisticTemplate(String targs[]) {
    final String templateName = StrUtil.compactSpaces(targs[0].toLowerCase());

    if(shipTemplateSet.contains(templateName)) {
      if(targs.length == 1) { return templateName; }
      String text = templateName.toUpperCase()+" "+targs[1];
      if(targs.length > 2) {
        text += " ("+targs[2]+")";
      }
      return WikiCleaner.internalLink(text, text);
    }
    if(templateName.equals("sclass")) {
      return targs[1]+"-class "+targs[2];
    }
    if(templateName.equals("smu")) {
      String text = "SMU "+targs[1];
      if(targs.length > 2) {
        text += " ("+targs[2]+")";
      }
      return WikiCleaner.internalLink(text, text);
    }
    if(templateName.equals("ship")) {
      String honorific = targs[1];
      String name = targs[2];
      String text = honorific+" "+name;
      if(targs.length > 3) {
        text += " ("+targs[2]+")";
      }
      return WikiCleaner.internalLink(text, text);
    }
    if(templateName.equals("gs")) {
      String text = "German Submarine "+targs[1];
      if(targs.length > 2) {
        text += " ("+targs[2]+")";
      }
      return WikiCleaner.internalLink(text, text);
    }
    if(templateName.equals("us patent")) {
      return "US Patent "+targs[1];
    }
    // don't convert units, drop original ones here
    if(templateName.equals("convert")) {
      if(targs.length < 3) { return ""; }
      return targs[1]+" "+targs[2];
    }
    if(templateName.equals("age in years and days")) {
      return "really old";
    }
    if(templateName.equals("nowrap")) {
      if(targs.length == 2) {
        return targs[1];
      } else return "";
    }
    if(templateName.equals("ill")) {
      if(targs.length == 3) {
        return targs[2];
      }
    }
    if(templateName.equals("'")) {
      return "'";
    }
    if(templateName.equals("'s")) {
      return "'s";
    }
    if(templateName.equals("oldstyledate")) {
      return targs[1]+" "+targs[2];
    }
    return null;
  }

  public static String processTemplate(String title, String input) {
    try {
      String targs[] = input.split("\\|");
      String templateName = StrUtil.compactSpaces(targs[0].toLowerCase());

      if (isSimpleIgnore(templateName))
        return "";
      if (isCitationNeeded(templateName))
        return "";
      if (isCitation(templateName))
        return "";

      // ships and other silly stuff
      String output = processStylisticTemplate(targs);
      if (output != null) return output;

      Map<String, String> args = templateArgs(targs);
      if (templateName.equals("citation needed span")) {
        return args.get("text");
      }

      if (targs.length == 2) {
        return WikiCleaner.internalLink(targs[0], targs[1]);
      }
    } catch (ArrayIndexOutOfBoundsException ex) {
      ex.printStackTrace(System.err);
    }

    System.err.println(title+": "+input);
    return input;
  }

  public static boolean isCitation(String templateName) {
    for(String ignored : citationTemplates) {
      if(templateName.equals(ignored))
        return true;
    }
    return false;
  }

  public static boolean isCitationNeeded(String templateName) {
    for(String ignored : citationNeededTemplates) {
      if(templateName.equals(ignored))
        return true;
    }
    return false;
  }

  public static boolean isSimpleIgnore(String templateName) {
    if(ignoredTemplateSet.contains(templateName))
      return true;

    for(String ignored : ignoredStartsWith) {
      if(templateName.startsWith(ignored))
        return true;
    }

    for(String ignored : ignoredContains) {
      if(templateName.contains(ignored)) {
        return true;
      }
    }

    return false;
  }

  public static String convertTemplates(final String title, String input) {
    return StrUtil.transformBetween(input, templateStart, templateEnd, new StrUtil.Transform() {
      @Override
      public String process(String input) {
        return processTemplate(title, input);
      }
    });
  }
}
