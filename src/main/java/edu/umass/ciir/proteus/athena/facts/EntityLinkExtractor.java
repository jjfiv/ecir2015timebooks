package edu.umass.ciir.proteus.athena.facts;

import edu.umass.ciir.proteus.athena.Main;
import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.Athena;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.Tag;
import org.lemurproject.galago.core.parse.TagTokenizer;
import org.lemurproject.galago.tupleflow.FakeParameters;
import org.lemurproject.galago.utility.Parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author jfoley
 */
public class EntityLinkExtractor implements Tool {

  @Override
  public String getName() {
    return "entity-link-extractor";
  }

  public static class EntityRef {
    public final ArrayList<String> anchorText;
    public final String name;
    public final List<String> qids;
    public final List<FactQuery> queries;
    public final List<Integer> dates;

    public EntityRef(String name) {
      this.name = name;
      qids = new ArrayList<String>();
      anchorText = new ArrayList<String>();
      dates = new ArrayList<Integer>();
      queries = new ArrayList<FactQuery>();
    }

    public int count() {
      return qids.size();
    }

    public void add(FactQuery fq, String text) {
      qids.add(fq.id);
      anchorText.add(text);
      dates.add(fq.getYear());
      queries.add(fq);
    }

    @Override
    public String toString() {
      Collections.sort(dates);
      return Parameters.parseArray(
        "id", name,
        "qids", qids,
        "atext", anchorText,
        "dates", dates).toString();
    }

  }

  public static final String[] stopLinks = {
    "people",
    "world_population",
  };

  public static boolean isStopLink(String ename) {
    String page = ename.toLowerCase();
    for(String stopLink : stopLinks) {
      if(page.equals(stopLink))
        return true;
    }
    return false;
  }

  public static String entityFromAnchor(Tag tag) {
    if(tag.name.equals("a") && tag.attributes.containsKey("href")) {
      return tag.attributes.get("href").replace("https://en.wikipedia.org/wiki/", "");
    }
    return null;
  }

  public static List<EntityRef> collectEntityRefs(List<FactQuery> input, Parameters argp, int atLeast) {
    HashMap<String, EntityRef> entities = new HashMap<String,EntityRef>();

    Parameters forTok = argp.clone();
    forTok.put("fields", "a");
    TagTokenizer tok = new TagTokenizer(new FakeParameters(forTok));

    for(FactQuery fq : input) {
      Document doc = tok.tokenize(fq.text);

      for(Tag tag : doc.tags) {
        if(tag.name.equals("a") && tag.attributes.containsKey("href")) {
          String ename = tag.attributes.get("href").replace("https://en.wikipedia.org/wiki/", "");
          if(isStopLink(ename)) continue;

          String anchorText = doc.text.substring(tag.charBegin, tag.charEnd);

          // collect them
          if(entities.containsKey(ename)) {
            entities.get(ename).add(fq, anchorText);
          } else {
            EntityRef ref = new EntityRef(ename);
            ref.add(fq, anchorText);
            entities.put(ename, ref);
          }
        }
      }
    }

    List<EntityRef> keep = new ArrayList<EntityRef>(entities.size());
    for(EntityRef eref : entities.values()) {
      if(eref.count() >= atLeast) {
        keep.add(eref);
      }
    }
    return keep;
  }


  @Override
  public void run(Parameters argp) throws Exception {
    List<FactQuery> facts = Athena.init(argp).getDataset().getSpecifiedFacts();
    List<EntityRef> ambiguousRefs = collectEntityRefs(facts, argp, 2);

    //System.out.println(ambiguousRefs.size());
    boolean printTopEntities = argp.get("printTopEntities", false);
    boolean printRefs = argp.get("printRefs", true);
    boolean printHistogram = argp.get("printHistogram", false);

    int maxFactFrequency = (int) argp.get("maxFactFrequency", 30);

    if(printHistogram) {
      TIntIntHashMap hist = new TIntIntHashMap();
      for(EntityRef ref : ambiguousRefs) {
        hist.adjustOrPutValue(ref.count(), 1, 1);
        //System.out.println(ref);
        if(printTopEntities) {
          if (ref.count() > 100) {
            System.out.println(ref.name + "\t" + ref.count());
          }
        }
      }

      hist.forEachEntry(new TIntIntProcedure() {
        @Override
        public boolean execute(int numJudgments, int freq) {
          System.out.printf("%d\t%d\n", numJudgments, freq);
          return true;
        }
      });
    }

    if(printRefs) {
      for(EntityRef ref : ambiguousRefs) {
        if(ref.count() <= maxFactFrequency) {
          System.out.println(ref);
        }
      }
    }

  }

  public static void main(String[] args) throws Exception {
    Main.main(new String[]{"--tool=entity-link-extractor", "seagate.json", "--dataset=none", "--printTopEntities"});
  }
}
