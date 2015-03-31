package edu.umass.ciir.proteus.athena.cfg;

import edu.umass.ciir.galagotools.utils.DateUtil;
import edu.umass.ciir.proteus.athena.experiment.ScoredDate;
import edu.umass.ciir.galagotools.galago.GalagoUtil;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import org.lemurproject.galago.core.btree.simple.DiskMapWrapper;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jfoley
 */
public class ExtractDates {
  public static List<ScoredDocument> collectPubDates(List<ScoredDocument> docs, TObjectIntHashMap<String> pubDates) {
    ArrayList<ScoredDocument> yearScores = new ArrayList<ScoredDocument>();

    for (ScoredDocument doc : docs) {
      if (pubDates.containsKey(doc.documentName)) {
        ScoredDocument faked = new ScoredDocument();
        faked.documentName = DateUtil.YearToString(pubDates.get(doc.documentName));
        faked.score = doc.score;
        faked.rank = doc.rank;
        yearScores.add(faked);
      }
    }

    return yearScores;
  }

  /**
   * Together with the logic in DateReranker, this code hacks together the relevance model for the years occurring in the stuff.
   * It does this by outputting a fake document for date in every originally scored document using the rank and socre of the outer document.
   * This causes each sub-year to have the same "score". Neat.
   */
  public static List<ScoredDocument> collectDocDates(List<ScoredDocument> docs, DiskMapWrapper<String, String> dateSource) throws IOException {
    final ArrayList<ScoredDocument> yearScores = new ArrayList<ScoredDocument>();

    // collect all doc -> [(date,count)...]
    Map<String, TIntIntHashMap> docParams = new HashMap<String, TIntIntHashMap>();
    Map<String,String> entries = dateSource.bulkGet(GalagoUtil.names(docs));

    for (Map.Entry<String, String> kv : entries.entrySet()) {
      Parameters mangledDateSet = Parameters.parseString(kv.getValue());
      TIntIntHashMap yearCounts = new TIntIntHashMap();
      for (String key : mangledDateSet.keySet()) {
        int year = Integer.parseInt(key);
        int count = (int) mangledDateSet.getLong(key);
        yearCounts.put(year, count);
      }
      docParams.put(kv.getKey(), yearCounts);
    }

    // output a fake document for each date on each book page in order
    for (final ScoredDocument doc : docs) {
      TIntIntHashMap yearCounts = docParams.get(doc.documentName);
      if(yearCounts == null) continue;

      yearCounts.forEachEntry(new TIntIntProcedure() {
        @Override
        public boolean execute(int year, int count) {
          // generate count faked documents per year
          for(int i=0; i<count; i++) {
            ScoredDocument faked = new ScoredDocument();
            faked.documentName = DateUtil.YearToString(year);
            faked.score = doc.score;
            faked.rank = doc.rank;
            yearScores.add(faked);
          }
          return true;
        }
      });
    }

    return yearScores;
  }

  public static List<ScoredDocument> fakeDocuments(List<ScoredDocument> original, Map<String,String> newNames) {
    List<ScoredDocument> fake = new ArrayList<ScoredDocument>();
    int rank = 1;
    for(ScoredDocument doc : original) {
      String newName = newNames.get(doc.documentName);
      if(newName == null) continue;

      ScoredDocument fdoc = new ScoredDocument();
      fdoc.documentName = newName;
      fdoc.score = doc.score;
      fdoc.rank = rank++;
      fake.add(fdoc);
    }

    return fake;
  }

  public static Map<String,String> getMetadataField(AthenaKind kind, String field, List<String> documentNames) throws IOException {
    DiskMapWrapper<String,String> fieldMap = kind.getFastMetadata(field);
    if (fieldMap != null) {
      return fieldMap.bulkGet(documentNames);
    } else {
      throw new IllegalArgumentException("Forgot to build metadata for field="+field);
      /*
      for (Document doc : kind.getRetrieval().getDocuments(documentNames, Document.DocumentComponents.JustMetadata).values()) {
        if (doc == null) continue;
        String value = doc.metadata.get(field);
        if (value == null) continue;
        output.put(doc.name, value);
      }
      */
    }
  }

  public static List<ScoredDate> toDates(List<ScoredDocument> docs) {
    ArrayList<ScoredDate> dates = new ArrayList<ScoredDate>(docs.size());
    for(ScoredDocument sdoc : docs) {
      dates.add(new ScoredDate(sdoc));
    }
    return dates;
  }

  public static List<ScoredDate> extract(AthenaKind kind, String method, List<ScoredDocument> docs) throws IOException {
    if("pubdate".equals(method)) {
      assert(kind.id.equals("books") || kind.id.equals("docs"));
      return toDates(collectPubDates(docs, kind.dataset.getPubDateMap()));
    } else if("page-dates".equals(method)) {
      assert(kind.id.equals("pages"));
      return toDates(collectDocDates(docs, kind.dataset.getDates(method)));
    } else if("book-dates".equals(method)) {
      assert(kind.id.equals("books"));
      return toDates(collectDocDates(docs, kind.dataset.getDates(method)));
    } else if("doc-dates".equals(method)) {
      assert(kind.id.equals("docs"));
      return toDates(collectDocDates(docs, kind.dataset.getDates(method)));
    } else if("metaYear".equals(method)) {
      return toDates(fakeDocuments(docs, getMetadataField(kind, "year", GalagoUtil.names(docs))));
    } else if("title".equals(method)) {
      return toDates(docs);
    } else {
      throw new IllegalArgumentException("No date extraction method for '"+method+"'");
    }
  }
}
