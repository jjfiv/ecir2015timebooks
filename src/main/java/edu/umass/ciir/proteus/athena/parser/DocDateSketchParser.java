package edu.umass.ciir.proteus.athena.parser;

import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;

/**
 * @author jfoley.
 */
public class DocDateSketchParser extends BufferedReaderParser {
  public DocDateSketchParser(DocumentSplit split, Parameters p) throws IOException {
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

    String[] cols = line.split("\t");

    Document doc = new Document();
    String bookId = cols[0];
    String year = cols[1];
    String text = cols[2];

    doc.name = bookId+ "_in_" +year;
    doc.metadata.put("year", year);
    doc.metadata.put("book", bookId);
    doc.text = text;
    return doc;
  }
}
