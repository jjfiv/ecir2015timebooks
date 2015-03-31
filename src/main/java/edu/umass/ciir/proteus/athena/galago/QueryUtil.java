package edu.umass.ciir.proteus.athena.galago;

import edu.umass.ciir.proteus.athena.NotHandledNow;
import edu.umass.ciir.proteus.athena.utils.DateUtil;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.traversal.Traversal;
import org.lemurproject.galago.core.util.WordLists;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.util.*;

/**
 * @author jfoley
 */
public class QueryUtil {
  public static final Set<String> customStop = new TreeSet<>(Arrays.asList(new String[]{
    "b", "d", "c", // born, died, circa
    "\u2013", // &ndash;
  }));
  private static Set<String> stopwords;

  public static Set<String> getStopwords() {
    if(stopwords == null) {
      try {
        stopwords = new HashSet<>();
        stopwords.addAll(WordLists.getWordList("inquery"));
        stopwords.addAll(customStop);
      } catch(IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
    return stopwords;
  }

  public static Node genQuery(Collection<String> terms, String operation) {
    Node op;
    switch (operation) {
      case "combine":
      case "ql":
        op = new Node("combine");
        break;
      case "sdm":
        op = new Node("sdm");
        //op.getNodeParameters().set("default", 20);
        break;
      default:
        throw new NotHandledNow("queryOperation", operation);
    }

    for(String term : terms) {
      op.addChild(Node.Text(term));
    }
    //Node scorer = new Node("dirichlet");
    //scorer.addChild(op);
    //return scorer;
    return op;
  }

  public static List<String> filterTerms(Parameters config, List<String> terms) {
    boolean stop = config.get("stopQueries", true);
    boolean removeDates = config.get("stopDates", true);

    ArrayList<String> resultTerms = new ArrayList<>(terms.size());
    for(String term : terms) {
      if(keepTerm(term, stop, removeDates))
        resultTerms.add(term);
    }
    return resultTerms;
  }

  public static boolean keepTerm(String term, boolean stop, boolean removeDates) {
    if(stop && getStopwords().contains(term))
      return false;
    if(removeDates && (DateUtil.isMonth(term) || DateUtil.isYear(term)))
      return false;
    return true;
  }

  /**
   * This function walks the query tree and collects all text nodes
   */
  public static List<String> termsFromQuery(Node query) {
    final ArrayList<String> terms = new ArrayList<>();

    Traversal collectTerms = new Traversal() {
      @Override
      public void beforeNode(Node original, Parameters queryParameters) throws Exception {
        if(original.isText()) {
          terms.add(original.getDefaultParameter());
        }
      }

      @Override
      public Node afterNode(Node original, Parameters queryParameters) throws Exception {
        return original;
      }
    };

    try {
      collectTerms.traverse(query, Parameters.instance());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return terms;
  }
}
