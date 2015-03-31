package edu.umass.ciir.proteus.athena.parser;

import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.proteus.athena.linking.LinkingExperiment;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author jfoley.
 */
public class AnchorTextParser extends DocumentStreamParser {
  private BufferedReader reader;
  String current;
  StringBuilder relatedTitles;
  StringBuilder linkText;

  public AnchorTextParser(DocumentSplit split, Parameters p) throws IOException {
    super(split, p);
    this.reader = getBufferedReader(split);
    this.relatedTitles = new StringBuilder();
    this.linkText = new StringBuilder();
  }

  public Document flush() {
    if(current == null) return null;

    Document doc = new Document();
    doc.name = current;
    doc.text = String.format(
        "<link-pages>%s</link-pages>\n" +
        "<link-text>%s</link-text>\n",
        relatedTitles.toString(), linkText.toString());

    current = null;
    this.relatedTitles = new StringBuilder();
    this.linkText = new StringBuilder();

    if(doc.name.length() > 40) return null;
    if(LinkingExperiment.skipTitle(doc.name)) return null;

    return doc;
  }

  @Override
  public Document nextDocument() throws IOException {
    while(true) {
      String nextLine = reader.readLine();
      if (nextLine == null) {
        return flush();
      }
      if(nextLine.trim().isEmpty()) continue;
      String[] cols = nextLine.split("\t");
      if(cols.length > 3 || cols.length == 1) {
        System.err.println(Arrays.asList(cols));
        continue;
      }
      String src = cols[0];
      String dest = LinkingExperiment.makeWikipediaTitle(cols[1]);
      String text;
      if(cols.length == 2) {
        text = dest;
      } else {
        text = cols[2];
      }
      if(current == null) {
        current = dest;
      }

      if (!dest.equals(current)) {
        Document result = flush();

        current = dest;
        relatedTitles.append(src).append('\n');
        linkText.append(text).append('\n');

        if(result == null) continue;

        return result;
      }

      relatedTitles.append(src).append('\n');
      linkText.append(text).append('\n');
    }
  }

  @Override
  public void close() throws IOException {
    IO.close(reader);
  }
}
