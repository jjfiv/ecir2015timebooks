package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.StrUtil;
import edu.umass.ciir.proteus.athena.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.ZipUtil;

import java.util.zip.ZipFile;

/**
 * @author jfoley.
 */
public class CitationContextExtractor implements Tool {
  @Override
  public String getName() {
    return "citation-context-extractor";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    ZipFile input = ZipUtil.open(IO.file(argp.getString("input")));
    for (String page : ZipUtil.listZipFile(input)) {
      String pageId = StrUtil.removeBack(page, ".html");
      if(!isWikiContentPage(pageId)) continue;
      process(pageId, IO.slurp(ZipUtil.readZipEntry(input, page)));
    }
  }

  public static boolean isWikiContentPage(String pageId) {
    return !(pageId.startsWith("Category:") ||
        pageId.startsWith("File:") ||
        pageId.startsWith("User:") ||
        pageId.startsWith("Talk:") ||
        pageId.startsWith("Wikipedia:") ||
        pageId.startsWith("Portal:"));
  }

  private void process(String pageId, String html) {
    // is this page worth processing?
    if(html.contains("#redirect")) return;
    if(!html.contains("citation")) return;

    Document doc = Jsoup.parse(html);
    System.out.println(pageId+"\ttc\t"+doc.select("text-citation").size());
    System.out.println(pageId+"\tpc\t"+doc.select("parsed-citation").size());
    System.out.println(pageId+"\tcn\t"+doc.select("citation-needed").size());
  }
}
