package edu.umass.ciir.proteus.athena.parser;

import edu.umass.ciir.galagotools.parser.BufferedReaderParser;
import edu.umass.ciir.galagotools.utils.StrUtil;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;

/**
 * @author jfoley
 */
public class WikiYearFactParser extends BufferedReaderParser {
  int uid = 0;

  public WikiYearFactParser(DocumentSplit split, Parameters p) throws IOException {
    super(split, p);
  }

  @Override
  public Document nextDocument() throws IOException {
    String line = reader.readLine();
    if(line == null) return null;

    String date = StrUtil.takeBefore(line, "\t");
    String text = StrUtil.takeAfter(line, "\t");
    Document doc = new Document();
    doc.metadata.put("date", date);
    doc.text = text;
    doc.name = String.format("f%d_%s",uid++, date.replaceAll(" ", "_"));
    return doc;
  }
}
