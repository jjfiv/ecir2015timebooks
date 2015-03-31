package edu.umass.ciir.proteus.athena.parser;

import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.StrUtil;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author jfoley.
 */
public class DbpediaAbstractParser extends DocumentStreamParser {
  private BufferedReader reader;

  public DbpediaAbstractParser(DocumentSplit split, Parameters p) throws IOException {
    super(split, p);
    this.reader = DocumentStreamParser.getBufferedReader(split);
  }

  final static String RelationType = " <http://dbpedia.org/ontology/abstract> ";

  @Override
  public Document nextDocument() throws IOException {
    if(reader == null) return null;
    while(true) {
      String data = reader.readLine();
      //System.err.println("# "+data);
      if (data == null) return null;
      if (!data.contains(RelationType)) continue;

      String[] kv = data.split(RelationType);
      String title = StrUtil.removeSurrounding(StrUtil.compactSpaces(kv[0]), "<http://dbpedia.org/resource/", ">");
      String text = StrUtil.removeSurrounding(StrUtil.compactSpaces(kv[1]), "\"", "\"@en .");

      // don't generate documents for empty abstracts
      if(text.isEmpty()) continue;

      Document doc = new Document();
      doc.name = title;
      doc.text = "<title>"+title.replaceAll("_", " ")+"</title>\n<body>"+text+"</body>";

      return doc;
    }
  }

  @Override
  public void close() throws IOException {
    IO.close(reader);
    reader = null;
  }
}
