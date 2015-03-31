package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.galagotools.utils.SGML;
import edu.umass.ciir.galagotools.utils.StrUtil;
import edu.umass.ciir.galagotools.utils.Util;
import edu.umass.ciir.galagotools.utils.XML;
import edu.umass.ciir.proteus.athena.Tool;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.StreamCreator;
import org.lemurproject.galago.utility.ByteUtil;
import org.lemurproject.galago.utility.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * @author jfoley
 */
public class WikiCitationFinder implements Tool {
  @Override
  public String getName() {
    return "wiki-citation-finder";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    List<File> inputFiles = Util.checkAndExpandPaths(argp.getAsList("input", String.class));
    // write zip file:
    final ZipOutputStream zos = new ZipOutputStream(StreamCreator.openOutputStream(argp.getString("output")));

    try {
      for (File fp : inputFiles) {
        XML.forFieldsInSections(fp, "page", Arrays.asList("title", "text"), new XML.FieldsFunctor() {
          @Override
          public void process(Map<String, String> data) {
            String pageTitle = data.get("title").replace(' ', '_');
            if (pageTitle.isEmpty() || pageTitle.startsWith("Template") || pageTitle.startsWith("User"))
              return;
            String body = WikiCitationFinder.process(pageTitle, data.get("text"));
            String html = String.format("<html><head><title>%s</title></head><body>%s</body></html>", pageTitle, body);

            try {
              ZipUtil.write(zos, pageTitle+".html", ByteUtil.fromString(html));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
    } finally {
      zos.close();
    }

  }

  static String process(final String title, String text) {
    text = SGML.removeComments(text);
    text = WikiCleaner.unescapeAmpersandEscapes(text);
    text = WikiCleaner.cleanWikiTags(text);
    text = WikiCleaner.killWikiTables(text);
    text = text.replaceAll("''", ""); // ditch all italics
    text = StrUtil.transformBetween(text, WikiTemplateHack.templateStart, WikiTemplateHack.templateEnd, new StrUtil.Transform() {
      @Override
      public String process(String input) {
        return processTemplate(title, input);
      }
    });
    text = SGML.transformTag(text, "ref", new SGML.TransformTag() {
      @Override
      public String process(Map<String, String> attrs, String body) {
        return processRefTag(title, attrs, body);
      }
    });
    text = WikiCleaner.convertLinks(text);
    text = WikiCleaner.convertHeaders(text);

    return text;
  }

  private static String processRefTag(String title, Map<String, String> attrs, String input) {
    try {
      if(input.contains("|")) {
        return processCitation(title, input.split("\\|"));
      }
      if(input.contains("<parsed-citation>"))
        return input;

      return SGML.makeTag("text-citation", attrs, input);
    } catch (Exception ex) {
      System.err.println("#caught!");
      ex.printStackTrace(System.err);
    }
    return "";
  }

  private static String processTemplate(String title, String input) {
    try {
      if (input.isEmpty()) return "";
      String targs[] = input.split("\\|");
      if (targs.length == 0) return "";
      String templateName = StrUtil.compactSpaces(targs[0].toLowerCase());

      if (WikiTemplateHack.isSimpleIgnore(templateName))
        return "";
      if (WikiTemplateHack.isCitationNeeded(templateName))
        return "";
      if (WikiTemplateHack.isCitation(templateName))
        return processCitation(title, targs);

      if (templateName.equals("refbegin") && targs.length == 1) {
        return "<ref>";
      }
      if (templateName.equals("refend") && targs.length == 1) {
        return "</ref>";
      }

      String output = WikiTemplateHack.processStylisticTemplate(targs);
      if (output != null) return output;

      Map<String, String> args = WikiTemplateHack.templateArgs(targs);
      if (templateName.equals("citation needed span")) {
        return "<citation-needed>" + args.get("text") + "</citation-needed>";
      }

      if (targs.length == 2) {
        return WikiCleaner.internalLink(targs[0], targs[1]);
      }
    } catch (Exception ex) {
      System.err.println("#caught!");
      ex.printStackTrace(System.err);
    }

    System.out.println("#unk " + title + ": " + input);
    return " ";
  }

  private static String processCitation(String title, String[] targs) {
    try {
      if(targs.length == 0) return "";
      String templateName = targs[0].toLowerCase().trim();
      Map<String,String> args = WikiTemplateHack.templateArgs(targs);
      //System.out.println("#citation " + title + ": " + templateName + " " + args);
      Parameters citationInfo = Parameters.instance();
      citationInfo.put("template", templateName);
      citationInfo.put("args", Parameters.parseMap(args));
      return "<parsed-citation>"+citationInfo.toString()+"</parsed-citation>";
    } catch (Exception ex) {
      System.err.println("#caught!");
      ex.printStackTrace(System.err);
    }
    return "";
  }
}
