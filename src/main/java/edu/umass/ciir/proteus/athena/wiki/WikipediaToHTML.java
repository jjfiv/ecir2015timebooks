package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.utils.SGML;
import edu.umass.ciir.proteus.athena.utils.StrUtil;
import edu.umass.ciir.proteus.athena.utils.Util;
import edu.umass.ciir.proteus.athena.utils.XML;
import org.lemurproject.galago.utility.ByteUtil;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.StreamCreator;
import org.lemurproject.galago.utility.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;


/**
 * @author jfoley.
 */
public class WikipediaToHTML implements Tool {
  @Override
  public String getName() {
    return "wikipedia-to-html";
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
						String body = WikipediaToHTML.process(pageTitle, data.get("text"));
						String html = String.format("<html><head><title>%s</title></head><body>%s</body></html>", pageTitle, body);

						try {
							ZipUtil.write(zos, pageTitle + ".html", ByteUtil.fromString(html));
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

  private static String process(final String title, String text) {
    text = SGML.removeComments(text);
    text = SGML.removeTag(text, "ref");
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
    text = WikiCleaner.convertLinks(text);
    text = WikiCleaner.convertHeaders(text);

    return text;
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
        return "";

      if (templateName.equals("refbegin") || templateName.equals("refend")) {
        return "";
      }

      String output = WikiTemplateHack.processStylisticTemplate(targs);
      if (output != null) return output;

      if (targs.length == 2) {
        return WikiCleaner.internalLink(targs[0], targs[1]);
      }
    } catch (Exception ex) {
      System.err.println("#caught!");
      ex.printStackTrace(System.err);
    }

    System.err.println("#unk "  + title + ": " + input);
    return " ";
  }
}
