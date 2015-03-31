package edu.umass.ciir.proteus.athena;

import edu.umass.ciir.galagotools.galago.GalagoUtil;
import edu.umass.ciir.proteus.athena.linking.LinkingExperiment;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.lemurproject.galago.core.btree.simple.DiskMapBuilder;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.utility.ByteUtil;
import org.lemurproject.galago.utility.Parameters;

/**
 * @author jfoley
 */
public class FindDisambiguationPages implements Tool {

  @Override
  public String getName() {
    return "find-disambiguation-pages";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    DiskMapBuilder output = new DiskMapBuilder(argp.getString("output"));

    byte[] redirect = ByteUtil.fromString("R");
    byte[] disambiguation = ByteUtil.fromString("D");

    for (Document input : GalagoUtil.documentIterable(new DiskIndex(argp.getString("input")), Document.DocumentComponents.All)) {
      String title = LinkingExperiment.makeWikipediaTitle(input.name);
      if(LinkingExperiment.skipTitle(title)) continue;
      Element body = Jsoup.parse(input.text).body();
      String ownText = body.ownText().toLowerCase();
      if(ownText.toLowerCase().contains("#redirect")) {
        output.put(ByteUtil.fromString(title), redirect);
      } else if(ownText.toLowerCase().contains("may refer to:")) {
        output.put(ByteUtil.fromString(title), disambiguation);
      }
    }
    output.close();
  }

  public static void main(String[] args) throws Exception {
    Tool fdp = new FindDisambiguationPages();
    fdp.run(Parameters.parseArray(
      "input", "indices/simple-wiki-title.galago",
      "output", "swt.dr.diskmap"
    ));
  }
}
