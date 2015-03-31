package edu.umass.ciir.proteus.athena.cfg;

import edu.umass.ciir.proteus.athena.facts.AmbiguousQuery;
import edu.umass.ciir.proteus.athena.facts.FactQuery;
import edu.umass.ciir.proteus.athena.galago.StrIntDiskMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.lemurproject.galago.core.btree.simple.DiskMapWrapper;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.util.*;

/**
 * @author jfoley
 */
public class DataSet {
  public String id;
  private final Parameters cfg;
  private Map<String, AthenaKind> kinds;
  private Map<String, String> facts;
  private HashMap<String, Set<Integer>> factQRel;
  private HashMap<String, Set<Integer>> ambiguousQRel;
  public String currentSplit;
  public String currentKind;
  private HashMap<String,DiskMapWrapper<String,String>> dateSources;

  public DataSet(String name, Parameters argp) {
    this.id = name;
    this.cfg = argp.getMap("datasets").getMap(name);
    this.dateSources = new HashMap<String, DiskMapWrapper<String, String>>();
    currentSplit = argp.get("split", (String) null);

    kinds = new HashMap<String, AthenaKind>();
    if (cfg.isMap("kinds")) {
      Parameters kindCfg = cfg.getMap("kinds");
      for (String id : kindCfg.keySet()) {
        kinds.put(id, new AthenaKind(this, id, kindCfg.getMap(id)));
      }
    }
    currentKind = argp.get("kind", (String) null);

    facts = new HashMap<String, String>();
    if (cfg.isMap("facts")) {
      Parameters factCfg = cfg.getMap("facts");
      for (String fact : factCfg.keySet()) {
        facts.put(fact, factCfg.getString(fact));
      }
    }

    factQRel = null;
  }

  private TObjectIntHashMap<String> pubdates = null;
  private TObjectIntHashMap<String> _getPubDateMap() throws IOException {
    TObjectIntHashMap<String> data = new TObjectIntHashMap<String>();
    StrIntDiskMap sidm = new StrIntDiskMap(cfg.getString("pubdate"));
    data.putAll(sidm);
    sidm.close();
    return data;
  }

  public TObjectIntHashMap<String> getPubDateMap() {
    if (pubdates == null) {
      try {
        pubdates = _getPubDateMap();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return pubdates;
  }

  public DiskMapWrapper<String, String> getDates(String id) throws IOException {
    if(!dateSources.containsKey(id)) {
      // lazy load
      String path = this.cfg.getString(id);
      dateSources.put(id, new DiskMapWrapper<String, String>(path, new DiskMapWrapper.StringCodec(), new DiskMapWrapper.StringCodec()));
    }
    return dateSources.get(id);
  }

  public AthenaKind getKind(String kind) {
    if (!kinds.containsKey(kind))
      throw new IllegalArgumentException("No such kind `" + kind + "' in dataset=" + id);
    return kinds.get(kind);
  }

  public Set<String> getSplits() {
    return facts.keySet();
  }

  public List<FactQuery> getFacts(String split) {
    return FactQuery.load(facts.get(split), split, this.id, cfg);
  }

  public List<FactQuery> getSpecifiedFacts() {
    if (currentSplit != null) {
      return getFacts(currentSplit);
    }
    throw new IllegalArgumentException("No current split");
  }

  public List<FactQuery> getAllFacts() {
    List<FactQuery> fqs = new ArrayList<FactQuery>();
    for( String split :facts.keySet()) {
      fqs.addAll(getFacts(split));
    }
    return fqs;
  }

  public List<AmbiguousQuery> getAmbiguousQueries(String split) {
    return AmbiguousQuery.load(this.cfg.getString("ambiguousQueries"), split);
  }

  public List<AmbiguousQuery> getSpecifiedAmbiguousQueries() {
    if(currentSplit != null) {
      return getAmbiguousQueries(currentSplit);
    }
    throw new IllegalArgumentException("No current split");
  }

  public Map<String,Set<Integer>> getFactQRel() {
    if(factQRel == null) {
      // lazy init
      factQRel = new HashMap<String, Set<Integer>>();
      for(FactQuery fq : getSpecifiedFacts()) {
        factQRel.put(fq.id, Collections.singleton(fq.getYear()));
      }
    }
    return factQRel;
  }

  public Map<String,Set<Integer>> getAmbiguousQRel() {
    if(ambiguousQRel == null) {
      // lazy init
      ambiguousQRel = new HashMap<String, Set<Integer>>();
      for(AmbiguousQuery aq : getSpecifiedAmbiguousQueries()) {
        ambiguousQRel.put(aq.id, aq.years());
      }
    }
    return ambiguousQRel;
  }

  public Map<String,Set<Integer>> getQRel(String qrelKind) {
    if(qrelKind.equals("facts")) {
      return getFactQRel();
    } else if(qrelKind.equals("ambiguous")) {
      return getAmbiguousQRel();
    } else {
      throw new IllegalArgumentException("Unknown qrelKind="+qrelKind);
    }
  }


  public static boolean yearMatches(int year, String dataset) {
    if(dataset.equals("books")) {
      return year >= 1000 && year <= 1925;
    } else if(dataset.equals("books35")) {
      return year >= 1860 && year <= 1894;
    } else if(dataset.equals("robust04")) {
      return year >= 1960 && year <= 1994;
    } else if(dataset.equals("none")) {
      return true;
    } else {
      throw new IllegalArgumentException("Unknown dataset=`"+dataset+"'");
    }
  }

  public Retrieval getRetrieval(String kind) {
    if(!this.kinds.containsKey(kind)) {
      throw new IllegalArgumentException("No such kind=`"+kind+"' for dataset="+id+" try one of "+this.kinds.keySet());
    }
    return this.kinds.get(kind).getRetrieval();
  }

  public Set<String> getKinds() {
    return kinds.keySet();
  }

  public int yearSpan() {
    if(id.equals("books")) {
      return 1925 - 1000 + 1;
    } else if(id.equals("books35")) {
      return 35;
    } else if(id.equals("robust04")) {
      return 1994 - 1960 + 1;
    } else {
      throw new IllegalArgumentException("Unknown dataset=`" + id + "'");
    }
  }

  public List<AmbiguousQuery> getAllAmbiguousQueries() {
    List<AmbiguousQuery> aqs = new ArrayList<AmbiguousQuery>();
    for( String split : getSplits()) {
      aqs.addAll(getAmbiguousQueries(split));
    }
    return aqs;
  }
}
