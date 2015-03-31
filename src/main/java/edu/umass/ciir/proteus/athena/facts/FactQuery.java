package edu.umass.ciir.proteus.athena.facts;

import edu.umass.ciir.galagotools.utils.DateUtil;
import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.SGML;
import edu.umass.ciir.galagotools.utils.StrUtil;
import edu.umass.ciir.proteus.athena.cfg.DataSet;
import edu.umass.ciir.galagotools.galago.QueryUtil;
import org.lemurproject.galago.core.eval.QueryJudgments;
import org.lemurproject.galago.core.eval.QuerySetJudgments;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.Tag;
import org.lemurproject.galago.core.parse.TagTokenizer;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.tokenize.Tokenizer;
import org.lemurproject.galago.tupleflow.FakeParameters;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.util.*;

/**
* @author jfoley
*/
public class FactQuery {
  public final String id;
  public final String text;
  public final String rel;

  public FactQuery(String id, String text, String rel) {
    this.id = id;
    this.text = text;
    this.rel = rel;
  }

  public FactQuery(Parameters fp) {
    this.id = fp.getString("number");
    this.text = fp.getString("text");
    this.rel = fp.getString("rel");
  }

  public Parameters toJSON() {
    Parameters p = Parameters.instance();
    p.put("number", id);
    p.put("text", text);
    p.put("rel", rel);
    return p;
  }

  public List<String> getTerms(Parameters config) {
    Tokenizer tok = Tokenizer.instance(config);
    List<String> terms = tok.tokenize(text.replace('\u2013', '-')).terms;
    return QueryUtil.filterTerms(config, terms);
  }

  public Set<String> uniqTerms(Parameters config) {
    return new HashSet<String>(getTerms(config));
  }

  public Node getQuery(Parameters config) {
    List<String> terms = getTerms(config);
    if (terms.isEmpty())
      return null;
    Node combine = QueryUtil.genQuery(terms, config.get("queryOperation", "combine"));
    return combine;
  }

  // TIMEX normalizes 1 AD to int(1) and 1 BC to int(0) and 2 BC to int(-1) etc.
  public int getYear() {
    return DateUtil.YearFromString(rel);
  }

  public static List<FactQuery> load(String fileName, final String split, final String dataset, final Parameters cfg) {
    final ArrayList<FactQuery> queries = new ArrayList<FactQuery>();
    final boolean lengthTrimQueries = cfg.get("lengthTrimQueries", true);
    final boolean filterByEntity = cfg.get("filterByEntity", true);

    IO.forEachLine(new File(fileName), new IO.StringFunctor() {
      int index = 0;

      @Override
      public void process(String input) {
        String year = StrUtil.removeSpaces(StrUtil.takeBefore(input, "\t"));
        String text = StrUtil.takeAfter(input, "\t");
        FactQuery q = new FactQuery(String.format("wyf-%s-%d", split, index++), text, year);
        if(!keepFactQuery(q, cfg, dataset, lengthTrimQueries, filterByEntity))
          return;
        queries.add(q);
      }
    });

    return queries;
  }

  public Set<String> entities() {
    Set<String> results = new HashSet<String>();

    TagTokenizer tok = new TagTokenizer(new FakeParameters(Parameters.parseArray("fields", "a")));
    Document doc = tok.tokenize(text);

    for(Tag tag : doc.tags) {
      String entity = EntityLinkExtractor.entityFromAnchor(tag);
      if(entity == null) continue;
      if(EntityLinkExtractor.isStopLink(entity)) continue;

      // see if there are any non-stopwords inside this link
      boolean usefulTerm = false;
      for(String term : doc.terms.subList(tag.begin, tag.end)) {
        if(QueryUtil.keepTerm(term, false, true)) {
          usefulTerm = true;
        }
      }
      if(!usefulTerm) {
        return Collections.emptySet();
      }

      results.add(entity);
    }

    return results;
  }

  public static boolean keepFactQuery(FactQuery q, Parameters cfg, String dataset, boolean lengthTrimQueries, boolean filterByEntity) {
    // filter by year
    if(!DataSet.yearMatches(q.getYear(), dataset))
      return false;

    // filter by length
    if(lengthTrimQueries) {
      int length = q.getTerms(cfg).size();
      if (length <= 3 || length > 30)
        return false;
    }

    if(filterByEntity) {
      if(q.entities().isEmpty())
        return false;
    }

    return true;
  }

  public static QuerySetJudgments qrels(List<FactQuery> facts) {
    HashMap<String, QueryJudgments> qj = new HashMap<String, QueryJudgments>();
    for (FactQuery fq : facts) {
      qj.put(fq.id, new QueryJudgments(fq.id, Collections.singletonMap(fq.rel, 1)));
    }
    return new QuerySetJudgments(qj);
  }

  public String getTextWithoutLinks() {
    return SGML.removeTagsLeaveContents(this.text);
  }
}
