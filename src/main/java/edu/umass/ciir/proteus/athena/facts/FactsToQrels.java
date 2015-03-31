package edu.umass.ciir.proteus.athena.facts;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.Util;
import org.lemurproject.galago.core.eval.QuerySetJudgments;
import org.lemurproject.galago.utility.Parameters;

import java.io.PrintWriter;
import java.util.List;

/**
 * @author jfoley
 */
public class FactsToQrels implements Tool {

  @Override
  public String getName() {
    return "facts-to-qrels";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    String splitId = argp.getString("split");
    PrintWriter outputQueries = IO.printWriter(argp.getString("output-queries"));
    String queryFormat = argp.get("queryFormat", "tsv");

    List<FactQuery> queries = FactQuery.load(argp.getString("input"), splitId, argp.getString("dataset"), argp);

    // qrelFromFacts
    QuerySetJudgments qsj = FactQuery.qrels(queries);
    //qsj.saveJudgments(argp.getString("output-qrels"));

    if(queryFormat.equals("json")) {
      List<Parameters> parameters = Util.map(queries, new Util.Transform<FactQuery, Parameters>() {
        @Override
        public Parameters process(FactQuery input) {
          return input.toJSON();
        }
      });
      Parameters queryJSON = Parameters.instance();
      queryJSON.put("queries", parameters);
      outputQueries.println(queryJSON.toPrettyString());
    } else if(queryFormat.equals("tsv")) {
      for(FactQuery q : queries) {
        outputQueries.println(q.id+"\t"+q.text);
      }
    } else throw new IllegalArgumentException("Query Format: "+queryFormat);
    outputQueries.close();

    assert(QuerySetJudgments.loadJudgments(argp.getString("output-qrels"), true, true).size() == queries.size());

    System.err.println("# done writing qrels and queries");
  }
}
