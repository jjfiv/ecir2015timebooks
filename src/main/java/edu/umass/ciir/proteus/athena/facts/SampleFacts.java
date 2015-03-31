package edu.umass.ciir.proteus.athena.facts;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.Athena;
import edu.umass.ciir.proteus.athena.utils.Util;
import org.lemurproject.galago.utility.Parameters;

import java.util.Collections;
import java.util.List;

/**
 * @author jfoley.
 */
public class SampleFacts implements Tool {
  @Override
  public String getName() {
    return "sample-facts";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    List<FactQuery> fqs = Athena.init(argp).getDataset().getSpecifiedFacts();

    Collections.shuffle(fqs);
    List<FactQuery> sample = Util.take(fqs, (int) argp.get("n", 10));

    System.out.println("\\begin{tabular}{|l|l|}");
    System.out.println("\\hline");
    for(FactQuery q : sample) {
      System.out.printf("%s & %s \\\\ \\hline\n", q.rel, q.getTextWithoutLinks());
    }
    System.out.println("\\end{tabular}");
  }
}
