package edu.umass.ciir.proteus.athena.experiment;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.Athena;
import edu.umass.ciir.proteus.athena.cfg.DataSet;
import edu.umass.ciir.proteus.athena.facts.AmbiguousQuery;
import edu.umass.ciir.proteus.athena.utils.IO;
import org.lemurproject.galago.core.retrieval.Results;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;

import java.io.PrintWriter;
import java.util.List;

/**
 * @author jfoley.
 */
public class GenerateQueryRun implements Tool {
  @Override
  public String getName() {
    return "generate-query-run";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    final boolean progress = argp.get("progress", true);
    final String kind = argp.getString("kind");
    final DataSet dataset = Athena.init(argp).getDataset();
    final Retrieval ret = dataset.getRetrieval(kind);
    final List<AmbiguousQuery> aqs = dataset.getAmbiguousQueries(dataset.currentSplit);

    if(!argp.containsKey("requested")) {
      argp.put("requested", 1000);
    }

    final PrintWriter output = IO.printWriter(argp.getString("output"));
    // output run parameters for posterity
    output.println("###\t"+argp);

    for (int i = 0; i < aqs.size(); i++) {
      AmbiguousQuery aq = aqs.get(i);
      if (progress) {
        System.err.println("# query " + (i + 1) + "/" + aqs.size());
      }
      Node query = aq.getQuery(argp);
      if(query == null) {
        throw new RuntimeException("# empty query after stopping: " + aq.id);
      }

      Node finalQuery = ret.transformQuery(query, argp);
      Results res = ret.executeQuery(finalQuery, argp);
      List<ScoredDocument> docs = res.scoredDocuments;

      GenerateFactRun.saveRun(output, kind, aq.id, docs);
    }

    output.close();
    System.err.println("# done "+getName());

  }
}
