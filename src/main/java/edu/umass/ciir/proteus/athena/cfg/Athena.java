package edu.umass.ciir.proteus.athena.cfg;

import org.lemurproject.galago.utility.Parameters;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jfoley
 */
public class Athena {
  private final Map<String,DataSet> datasets;
  private final String currentDataset;
  public Parameters argp;

  public Athena(Parameters argp) throws Exception {
    this.argp = argp;
    this.datasets = new HashMap<String,DataSet>();

    for(String id : argp.getMap("datasets").keySet()) {
      datasets.put(id, new DataSet(id, argp));
    }

    this.currentDataset = argp.getString("dataset");
    if(!datasets.containsKey(currentDataset))
      throw new IllegalArgumentException("No such dataset: "+currentDataset);

    if(argp.get("echoConfig", true)) {
      System.out.println("### Athena Config");
      System.out.println(argp.toPrettyString("###"));
    }
  }

  public DataSet getDataset() {
    return datasets.get(currentDataset);
  }

  public static Athena init(Parameters argp) throws Exception {
    return new Athena(argp);
  }
}
