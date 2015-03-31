package edu.umass.ciir.proteus.athena.parser;

import edu.umass.ciir.galagotools.parser.BufferedReaderParser;
import edu.umass.ciir.proteus.athena.preprocess.SentenceIO;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;

/**
 * @author jfoley.
 */
public class DatedSentenceParser extends BufferedReaderParser {
  private final String dataset;

  public DatedSentenceParser(DocumentSplit split, Parameters p) throws IOException {
    super(split, p);
    this.dataset = conf.getString("dataset");
  }

  @Override
  public Document nextDocument() throws IOException {
    while (true) {
      String line = reader.readLine();
      if (line == null) return null;

      SentenceIO.Data data = SentenceIO.parseTSV(line, this.dataset);
      if(data == null) continue;

      Document doc = new Document();
      doc.name = String.format("%s_%d_%d", data.bookId, data.pageNum, data.sentenceNum);
      doc.metadata.put("book", data.bookId);
      doc.metadata.put("page", Integer.toString(data.pageNum));
      doc.metadata.put("sentence", Integer.toString(data.sentenceNum));
      doc.metadata.put("year", Integer.toString(data.year));
      doc.metadata.put("date", data.timex);
      doc.text = data.sentenceText;
      return doc;
    }
  }
}
