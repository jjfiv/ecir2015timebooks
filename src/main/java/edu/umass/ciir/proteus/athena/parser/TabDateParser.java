package edu.umass.ciir.proteus.athena.parser;

import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;

/**
 * @author jfoley.
 */
public class TabDateParser extends BufferedReaderParser {

  public TabDateParser(DocumentSplit split, Parameters p) throws IOException {
    super(split, p);
  }

  @Override
  public Document nextDocument() throws IOException {
    String line;
    while(true) {
      line = reader.readLine();
      // end on EOF, skip blank lines
      if (line == null) return null;
      if (!line.trim().isEmpty()) break;
    }

    int firstTab = line.indexOf('\t');
    if(firstTab == -1) {
      throw new IllegalArgumentException("Bad input line!: "+line);
    }

    Document doc = new Document();
    doc.name = line.substring(0, firstTab);
    doc.text = line.substring(firstTab+1);
    return doc;
  }
}
