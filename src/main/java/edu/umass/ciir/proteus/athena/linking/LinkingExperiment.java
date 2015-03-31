package edu.umass.ciir.proteus.athena.linking;

import edu.umass.ciir.galagotools.err.NotHandledNow;
import edu.umass.ciir.galagotools.utils.DateUtil;
import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.RandUtil;
import edu.umass.ciir.galagotools.utils.StrUtil;
import edu.umass.ciir.proteus.athena.Tool;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.Tag;
import org.lemurproject.galago.core.parse.TagTokenizer;
import org.lemurproject.galago.core.retrieval.Results;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;

import java.util.*;

/**
 * @author jfoley
 */
public class LinkingExperiment implements Tool {
  @Override
  public String getName() {
    return "linking-experiment";
  }

  public static class DatedDoc {
    public int year;
    public String text;

    public DatedDoc(int year, String text) {
      this.year = year;
      this.text = text;
    }
  }

  public static List<DatedDoc> load(String path) {
    final ArrayList<DatedDoc> out = new ArrayList<DatedDoc>();
    IO.forEachLine(IO.file(path), new IO.StringFunctor() {
      @Override
      public void process(String input) {
        String[] cols = input.split("\t");
        out.add(new DatedDoc(DateUtil.YearFromString(cols[0]), cols[1]));
      }
    });
    return out;
  }

  public static String makeWikipediaTitle(String input) {
    if(input.isEmpty()) return "";
    String fixed = input.replaceAll("\\s+", "_");
    if(Character.isLowerCase(fixed.charAt(0))) {
      return Character.toUpperCase(fixed.charAt(0))+fixed.substring(1);
    }
    return StrUtil.removeBack(fixed, ".html");
  }

  public static boolean skipTitle(String title) {
    if(title.startsWith("List_of") || title.startsWith("Lists_of"))
      return true;
    if(title.startsWith("Wikipedia:")) return true;
    if(title.startsWith("File:")) return true;
    if(title.startsWith("Category:")) return true;
    if(title.contains("disambiguation")) return true;

    return false;
  }

  public List<ScoredDocument> runQuery(String model, Retrieval ret, Map<String, String> skips, List<String> terms, int num) throws Exception {
    Node iq;
    if(model.equals("sdm")) {
      iq = new Node("sdm");
      for (String term : terms) {
        iq.addChild(Node.Text(term));
      }
    } else if(model.equals("half-title")) {
      Node sdm = new Node("sdm");
      Node titleSDM = new Node("sdm");
      for (String term : terms) {
        sdm.addChild(Node.Text(term));
        titleSDM.addChild(new Node("inside", Arrays.asList(Node.Text(term), new Node("field", "title"))));
      }

      iq = new Node("combine");
      iq.addChild(titleSDM);
      iq.addChild(sdm.clone());
    } else throw new NotHandledNow("runQuery.model", model);

    Parameters qp = Parameters.parseArray("requested", Math.max(100,num));
    Node xq = ret.transformQuery(iq, qp);
    Results res = ret.executeQuery(xq, qp);
    return filter(res.scoredDocuments, skips, num);
  }

  private List<ScoredDocument> filter(List<ScoredDocument> scoredDocuments, Map<String, String> skips, int num) {
    ArrayList<ScoredDocument> output = new ArrayList<>();
    for (ScoredDocument document : scoredDocuments) {
      String title = makeWikipediaTitle(document.documentName);
      if(skipTitle(title) || skips.containsKey(title))
        continue;
      output.add(document);
      if(output.size() >= num) break;
    }
    return output;
  }

  @Override
  public void run(Parameters argp) throws Exception {
    List<DatedDoc> facts = load(argp.get("facts", "queries/wiki.year.events.tsv"));

    /*DiskMapWrapper<String,String> skips = null;
    if(argp.isString("skips")) {
      skips = new DiskMapWrapper<>(argp.getString("skips"), new DiskMapWrapper.StringCodec(), new DiskMapWrapper.StringCodec());
    }*/
    //final String model = argp.get("model", "sdm");
    //Retrieval wikipedia = RetrievalFactory.instance(argp.getString("index"), argp);

    Random rand = new Random(argp.get("randomSeed", 13));
    facts = RandUtil.sampleRandomly(facts, (int) argp.get("numFacts", 10), rand);

    TagTokenizer tok = new TagTokenizer();
    tok.addField("a");
    for (DatedDoc fact : facts) {
      Document doc = tok.tokenize(fact.text);

      List<String> nearByMentions = new ArrayList<>();
      List<Parameters> forSerialization = new ArrayList<>();

      for (Tag tag : doc.tags) {
        if(!tag.name.equals("a")) continue;
        if(!tag.attributes.containsKey("href")) continue;

        String ename = makeWikipediaTitle(tag.attributes.get("href").replace("https://en.wikipedia.org/wiki/", ""));
        String anchorText = doc.text.substring(tag.charBegin, tag.charEnd);

        Parameters task = Parameters.instance();
        task.put("gold-standard-entity", ename);
        task.put("gold-standard-year", fact.year);
        task.put("anchorText", doc.text.substring(tag.charBegin, tag.charEnd));
        task.put("surroundText", doc.text);

        forSerialization.add(task);
        nearByMentions.add(anchorText);

        /*
        List<String> terms = doc.terms.subList(tag.begin, tag.end);
        String reference = Utility.join(terms, " ");

        List<ScoredDocument> mentionSDM = runQuery(model, wikipedia, skips, terms, 1);

        if(mentionSDM.isEmpty()) {
          System.out.println(fact.year+", "+reference+", "+ename+", NIL, BAD");
        } else {
          String gotPage = makeWikipediaTitle(mentionSDM.get(0).documentName);
          boolean relevant = gotPage.equals(ename);
          System.out.println(fact.year+", "+reference+", "+ename+", "+gotPage+", "+(relevant ? "GOOD" : "BAD"));
        }
        */

      }

      for (Parameters json : forSerialization) {
        json.put("mention-neighbors", nearByMentions);
        System.out.println(json.toString());
      }
    }

    //IO.close(skips);
  }

  public static void main(String[] args) throws Exception {
    Tool lexp = new LinkingExperiment();
    lexp.run(Parameters.parseArray("index", "indices/jeff-wiki.galago"));
  }
}
