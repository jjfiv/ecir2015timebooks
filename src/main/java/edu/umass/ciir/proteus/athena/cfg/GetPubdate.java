package edu.umass.ciir.proteus.athena.cfg;

import edu.umass.ciir.proteus.athena.Tool;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.lemurproject.galago.utility.Parameters;

/**
 * @author jfoley.
 */
public class GetPubdate implements Tool {
  @Override
  public String getName() {
    return "pubdate";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    TObjectIntHashMap<String> pubdateMap = Athena.init(argp).getDataset().getPubDateMap();
    String key = argp.getString("input");
    int year = pubdateMap.get(key);
    System.out.println(year);
  }
}
