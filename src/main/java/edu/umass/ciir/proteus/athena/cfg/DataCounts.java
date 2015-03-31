package edu.umass.ciir.proteus.athena.cfg;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.facts.AmbiguousQuery;
import edu.umass.ciir.proteus.athena.facts.FactQuery;
import edu.umass.ciir.galagotools.utils.Util;
import org.lemurproject.galago.utility.Parameters;

import java.util.Collections;
import java.util.List;

/**
 * @author jfoley
 */
public class DataCounts implements Tool {
  @Override
  public String getName() {
    return "data-counts";
  }

  @Override
  public void run(final Parameters argp) throws Exception {
    DataSet dataset = Athena.init(argp).getDataset();

    if(argp.get("facts-per-split", false) || argp.get("fq-counts", false)) {
      for (String split : dataset.getSplits()) {
        System.out.printf("%s fqs: %d\n", split, dataset.getFacts(split).size());
      }
    }

    if(argp.get("aq-counts", false)) {
      for (String split : dataset.getSplits()) {
        System.out.printf("%s aqs: %d\n", split, dataset.getAmbiguousQueries(split).size());
      }
    }

    if(argp.get("aq-sample", false)) {
      List<AmbiguousQuery> fqs = Athena.init(argp).getDataset().getAllAmbiguousQueries();

      Collections.shuffle(fqs);
      List<AmbiguousQuery> sample = Util.take(fqs, (int) argp.get("n", 10));

      System.out.println("\\begin{tabular}{|l|l|}");
      System.out.println("\\hline");
      for (AmbiguousQuery q : sample) {
        System.out.printf("%s & %s \\\\ \\hline\n", q.years(), q.sharedTerms(argp));
      }
      System.out.println("\\end{tabular}");
      return;
    }

    if(argp.get("show-kinds", false)) {
      System.out.println(dataset.getKinds());
    }

    if(argp.isString("aqjson")) {
      List<AmbiguousQuery> train = AmbiguousQuery.load(argp.getString("aqjson"), "train");
      List<AmbiguousQuery> validate = AmbiguousQuery.load(argp.getString("aqjson"), "validate");
      List<AmbiguousQuery> test = AmbiguousQuery.load(argp.getString("aqjson"), "test");

      System.out.println("metric\ttrain\tvalidate\ttest");
      System.out.printf("count\t%d\t%d\t%d\n", train.size(), validate.size(), test.size());
      System.out.printf("meanDomain\t%.3f\t%.3f\t%.3f\n", AmbiguousQuery.meanDomain(train), AmbiguousQuery.meanDomain(validate), AmbiguousQuery.meanDomain(test));
    }


    if(argp.get("entities-per-fact", false)) {
      for (FactQuery factQuery : dataset.getSpecifiedFacts()) {
        System.out.println(factQuery.entities());
      }
    }

    if(argp.get("entities", false)) {
      for (FactQuery factQuery : dataset.getSpecifiedFacts()) {
        for(String entity : factQuery.entities()) {
          System.out.println(entity);
        }
      }
    }
  }
}
