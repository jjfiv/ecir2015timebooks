package edu.umass.ciir.proteus.athena.facts;

import edu.umass.ciir.proteus.athena.galago.QueryUtil;
import edu.umass.ciir.proteus.athena.utils.IO;
import edu.umass.ciir.proteus.athena.utils.Util;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * @author jfoley.
 */
public class AmbiguousQuery {
  public final String id;
  private final List<FactQuery> facts;

  public AmbiguousQuery(String id, List<FactQuery> fqs) {
    this.id = id;
    this.facts = fqs;
  }

  public Set<Integer> years() {
    Set<Integer> years = new HashSet<Integer>();
    for(FactQuery fact : facts) {
      years.add(fact.getYear());
    }
    return years;
  }

  public Set<String> sharedTerms(Parameters argp) {
    ArrayList<Set<String>> terms = new ArrayList<Set<String>>();

    for(FactQuery fq : facts) {
      terms.add(fq.uniqTerms(argp));
    }

    return Util.intersection(terms);
  }

  public Node getQuery(Parameters argp) {
    String method = argp.getString("genQueryMethod");
    if(method.equals("intersection")) {
      return QueryUtil.genQuery(sharedTerms(argp), argp.get("queryOperation", "combine"));
    } else {
      throw new IllegalArgumentException("Unknown genQueryMethod="+method);
    }
  }

  public static double meanDomain(List<AmbiguousQuery> aqs) {
    double domain = 0.0;
    for(AmbiguousQuery aq : aqs) {
      Set<Integer> years = aq.years();
      int min = Collections.min(years);
      int max = Collections.max(years);
      domain += (max-min);
    }
    return domain / (double) aqs.size();
  }

  public static List<AmbiguousQuery> load(String fileName, final String split) {
    final ArrayList<AmbiguousQuery> queries = new ArrayList<AmbiguousQuery>();

    IO.forEachLine(new File(fileName), new IO.StringFunctor() {

			int index = 0;

			@Override
			public void process(String input) {
				if (input.startsWith("#")) return;

				String qid = String.format("aq-%s-%d", split, index++);

				Parameters p;
				try {
					p = Parameters.parseString(input);
				} catch (IOException e) {
					return;
				}
				if (!p.getString("split").equals(split)) {
					return;
				}

				List<FactQuery> inGroup = new ArrayList<FactQuery>();
				for (Parameters fp : p.getList("facts", Parameters.class)) {
					inGroup.add(new FactQuery(fp));
				}
				queries.add(new AmbiguousQuery(qid, inGroup));
			}
		});

    return queries;
  }

}
