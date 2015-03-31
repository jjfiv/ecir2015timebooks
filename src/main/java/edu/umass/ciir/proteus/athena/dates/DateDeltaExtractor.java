package edu.umass.ciir.proteus.athena.dates;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.DataSet;
import gnu.trove.list.array.TDoubleArrayList;
import org.lemurproject.galago.core.btree.simple.DiskMapWrapper;
import org.lemurproject.galago.utility.Parameters;

/**
 * @author jfoley.
 */
public class DateDeltaExtractor implements Tool {
  @Override
  public String getName() {
    return "date-delta-extractor";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    final String input = argp.getString("input");
    final String dataset = argp.get("dataset", (String) null);
    DiskMapWrapper<String,Parameters> data = new DiskMapWrapper<String, Parameters>(input, new DiskMapWrapper.StringCodec(), new DiskMapWrapper.JSONCodec());

    System.err.println(data.size());

    data.forEach(new DiskMapWrapper.IAction<String, Parameters>() {
      @Override
      public void process(String docName, Parameters yearMap) {
        TDoubleArrayList years = new TDoubleArrayList();
        for (String key : yearMap.keySet()) {
          int year = Integer.parseInt(key);
          int count = (int) yearMap.getLong(key);
          if(dataset != null && !DataSet.yearMatches(year, dataset)) continue;

          for(int i=0; i<count; i++) {
            years.add(year);
          }
        }

        double mean = years.sum() / (double) years.size();
        for (int idx = 0; idx < years.size(); idx++) {
          double year = years.get(idx);
          System.out.printf("%.3f\n", Math.abs(year - mean));
        }
      }
    });
  }

  public static void main(String[] args) throws Exception {
    Tool dde = new DateDeltaExtractor();
    dde.run(Parameters.parseArray(
        "input", "indices/books/book-dates"
    ));
  }
}
