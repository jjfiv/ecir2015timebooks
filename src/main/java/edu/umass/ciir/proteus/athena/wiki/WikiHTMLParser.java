package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.StrUtil;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;

/**
 * @author jfoley
 */
public class WikiHTMLParser extends DocumentStreamParser {
  public Document doc;

  public WikiHTMLParser(DocumentSplit split, Parameters p) throws IOException {
    super(split, p);
    String name = DocumentStreamParser.getFileName(split);
    name = StrUtil.removeBack(name, ".html");
    while(name.contains("/")) {
      name = StrUtil.takeAfter(name, "/");
    }

    if(!StrUtil.containsAscii(name))
      return;

    doc = new Document();
    doc.name = name;
    doc.text = IO.slurp(getBufferedReader(split));
    if(startsWithIgnoreSpaces(doc.text, "#REDIRECT")) {
      doc = null;
    }
  }

  public boolean startsWithIgnoreSpaces(String input, String prefix) {
    if(input.length() < prefix.length()) return false;
    int i;
    for(i=0; i<input.length(); i++) {
      if(!Character.isWhitespace(input.charAt(i)))
        break;
    }
    return input.substring(i).startsWith(prefix);
  }

  @Override
  public Document nextDocument() throws IOException {
    Document prev = doc;
    doc = null;
    return prev;
  }

  @Override
  public void close() throws IOException {
    doc = null;
  }
}
