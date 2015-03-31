package edu.umass.ciir.proteus.athena.parser;

import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;

/**
 * @author jfoley.
 */
public class JSONDocParser extends BufferedReaderParser {
  public JSONDocParser(DocumentSplit split, Parameters p) throws IOException {
    super(split, p);
  }

  @Override
  public Document nextDocument() throws IOException {
    while(true) {
      String nextLine = reader.readLine();
      if (nextLine == null) return null;
      if (nextLine.isEmpty()) continue;

      Parameters json = Parameters.parseString(nextLine);
      Document doc = new Document();
      doc.name = json.getAsString("name");
      doc.text = json.getAsString("text");
      if(json.isMap("meta")) {
        Parameters metajson = json.getMap("meta");
        for(String key : metajson.keySet()) {
          doc.metadata.put(key, metajson.getAsString(key));
        }
      }
      return doc;
    }
  }
}
