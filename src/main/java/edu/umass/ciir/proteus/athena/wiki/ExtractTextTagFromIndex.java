package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.galagotools.galago.GalagoUtil;
import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.StrUtil;
import edu.umass.ciir.proteus.athena.Tool;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.utility.Parameters;

import java.io.PrintWriter;

/**
 * @author jfoley
 */
public class ExtractTextTagFromIndex implements Tool {

  @Override
  public String getName() {
    return "extract-text-tag-from-index";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    String path = argp.getString("input");
    String output = argp.getString("output");

    PrintWriter out = IO.printWriter(output);
    for (Document doc : GalagoUtil.documentIterable(new DiskIndex(path), Document.DocumentComponents.JustText)) {
      Parameters ofDoc = Parameters.instance();
      String usefulText = StrUtil.takeBetween(doc.text, "<text>", "</text>");
      if(usefulText.equals("\\N")) continue;
      ofDoc.put("name", doc.name);
      ofDoc.put("text", usefulText);
      out.println(ofDoc.toString());
    }
  }
}
