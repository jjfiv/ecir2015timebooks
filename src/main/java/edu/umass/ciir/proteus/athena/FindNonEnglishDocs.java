package edu.umass.ciir.proteus.athena;

import edu.umass.ciir.galagotools.galago.GalagoUtil;
import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.StrUtil;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.PrintStream;
import java.util.List;

/**
 * @author jfoley
 */
public class FindNonEnglishDocs implements Tool {

  @Override
  public String getName() {
    return "find-non-english-docs";
  }

  public static String trecDocument(String docno, String text) {
    return "<DOC>\n<DOCNO>" + docno + "</DOCNO>\n"
      + "<TEXT>\n" + text + "</TEXT>\n</DOC>\n";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    PrintStream out = IO.printStream(argp.getString("output"));
    List<DocumentSplit> splits = GalagoUtil.getDocumentSplits(argp.getAsList("input", String.class), argp);
    for (DocumentSplit split : splits) {
      DocumentStreamParser parser = DocumentStreamParser.instance(split, argp);
      for (Document document : GalagoUtil.documentsStreamIterable(parser)) {
        String title = StrUtil.removeBack(document.name, ".html");
        if(!StrUtil.containsAscii(title)) {
          System.err.println(title);
          out.println(trecDocument(title, document.text));
        }
      }
      parser.close();
      out.flush();
    }
    out.close();
  }
}
