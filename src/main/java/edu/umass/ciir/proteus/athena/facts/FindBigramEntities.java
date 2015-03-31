package edu.umass.ciir.proteus.athena.facts;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.Athena;
import edu.umass.ciir.proteus.athena.cfg.DataSet;
import edu.umass.ciir.proteus.athena.utils.Util;
import org.lemurproject.galago.utility.Parameters;

import java.util.*;

/**
 * @author jfoley.
 */
public class FindBigramEntities implements Tool {
  @Override
  public String getName() {
    return "find-bigram-entities";
  }

  public static class StrPair extends AbstractMap.SimpleImmutableEntry<String,String> {
    public StrPair(String key, String value) {
      super(key, value);
    }
    public String key() { return this.getKey(); }
    public String value() { return this.getValue(); }
    @Override
    public String toString() {
      return "<"+key()+","+value()+">";
    }
  }

  @Override
  public void run(Parameters argp) throws Exception {
    DataSet dataset = Athena.init(argp).getDataset();

    Map<StrPair,List<FactQuery>> bigrams = new HashMap<StrPair, List<FactQuery>>();

    for(FactQuery fq : dataset.getAllFacts()) {
      List<String> entities = new ArrayList<String>(fq.entities());
      Collections.sort(entities);
      for(int i=0; i<entities.size(); i++) {
        for(int j=i+1; j<entities.size(); j++) {
          StrPair bigram = new StrPair(entities.get(i), entities.get(j));
          if(!bigrams.containsKey(bigram)) {
            ArrayList<FactQuery> queries = new ArrayList<FactQuery>();
            queries.add(fq);
            bigrams.put(bigram, queries);
          } else {
            bigrams.get(bigram).add(fq);
          }
        }
      }
    }

    if(argp.get("justHistogram", false)) {
      for (Map.Entry<StrPair, List<FactQuery>> bigram : bigrams.entrySet()) {
        System.out.println(bigram.getValue().size());
      }
      return;
    }

    ArrayList<Parameters> groupedFacts = new ArrayList<Parameters>();

    for (Map.Entry<StrPair, List<FactQuery>> bigram : bigrams.entrySet()) {
      if(bigram.getValue().size() > 1) {
        Set<Integer> years = new HashSet<Integer>();
        List<Set<String>> terms = new ArrayList<Set<String>>();

        ArrayList<Parameters> fqjson = new ArrayList<Parameters>();

        for(FactQuery fq : bigram.getValue()) {
          years.add(fq.getYear());
          terms.add(fq.uniqTerms(argp));
          fqjson.add(fq.toJSON());
        }

        Set<String> uniqTerms = Util.intersection(terms);

        if(uniqTerms.isEmpty()) continue;
        if(years.size() == 1) continue;

        Parameters factGroup = Parameters.instance();
        factGroup.put("facts", fqjson);

        //System.out.println(bigram.getKey()+"\t"+uniqTerms+"\t"+years); //+" "+bigram.getValue());
        groupedFacts.add(factGroup);
      }
    }

    // assign randomly to 1 of three splits
    Collections.shuffle(groupedFacts);
    for (int i = 0; i < groupedFacts.size(); i++) {
      Parameters p = groupedFacts.get(i);
      if(i % 3 == 0) p.put("split", "train");
      if(i % 3 == 1) p.put("split", "validate");
      if(i % 3 == 2) p.put("split", "test");
      System.out.println(p);
    }

  }

}
