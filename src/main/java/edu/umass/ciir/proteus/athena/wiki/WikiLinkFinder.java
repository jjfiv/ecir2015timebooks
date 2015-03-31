package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.galagotools.galago.GalagoUtil;
import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.StrUtil;
import edu.umass.ciir.proteus.athena.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.PrintWriter;
import java.util.List;

/**
 * @author jfoley.
 */
public class WikiLinkFinder implements Tool {
  @Override
  public String getName() {
    return "wiki-link-finder";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    List<DocumentSplit> splits = GalagoUtil.getDocumentSplits(argp.getAsList("input", String.class), argp);

    PrintWriter out = IO.printWriter(argp.getString("output"));

    for (DocumentSplit split : splits) {
      DocumentStreamParser parser = DocumentStreamParser.instance(split, argp);
      try {
        for (Document document : GalagoUtil.documentsStreamIterable(parser)) {
          org.jsoup.nodes.Document html = Jsoup.parseBodyFragment(document.text);
          String wikiTitle = html.select("title").get(0).ownText();

          for (Element anchor : html.select("a")) {
            String dest = anchor.attr("href");
            // skip external links for now
            if(!dest.startsWith("https://en.wikipedia.org/wiki/")) {
              continue;
            }
            dest = WikiCleaner.stripWikiUrlToTitle(dest);
            // grab the text referring to this
            String innerText = StrUtil.compactSpaces(anchor.html());
            out.printf("%s\t%s\t%s\n", wikiTitle, dest, innerText);
          }
        }
      } catch (Exception e) {
        e.printStackTrace(System.err);
      } finally {
        parser.close();
        out.flush();
      }
    }
    out.close();
  }

  public static void main(String[] args) throws Exception {
    Tool wlf = new WikiLinkFinder();
    wlf.run(Parameters.parseArray(
        "input", "Houston_Southwest_Airport.html",
        "output", "-"
    ));
  }
}
