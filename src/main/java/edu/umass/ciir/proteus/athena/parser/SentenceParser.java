package edu.umass.ciir.proteus.athena.parser;

import edu.umass.ciir.galagotools.parser.BufferedReaderParser;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;

/**
 * @author jfoley
 */
public class SentenceParser extends BufferedReaderParser {
  public SentenceParser(DocumentSplit split, Parameters p) throws IOException {
    super(split, p);
  }

  @Override
  public Document nextDocument() throws IOException {
    while (true) {
      String line = reader.readLine();
      if (line == null) return null;

      String cols[] = line.split("\t");
      int page;
      int sentence;
      try {
        page = Integer.parseInt(cols[1]);
        sentence = Integer.parseInt(cols[2]);
      } catch (NumberFormatException nfe) {
        continue;
      }
      String book = cols[0];
      String text = cols[3];

      Document doc = new Document();
      doc.name = String.format("%s_%d_%d", book, page, sentence);
      doc.metadata.put("book", book);
      doc.metadata.put("page", Integer.toString(page));
      doc.metadata.put("sentence", Integer.toString(sentence));
      doc.text = text;
      return doc;
    }
  }
}
