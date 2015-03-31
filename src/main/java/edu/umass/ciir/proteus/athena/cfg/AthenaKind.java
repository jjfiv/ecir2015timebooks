package edu.umass.ciir.proteus.athena.cfg;

import edu.umass.ciir.proteus.athena.experiment.ScoredDate;
import org.lemurproject.galago.core.btree.simple.DiskMapWrapper;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @author jfoley
 */
public class AthenaKind {
  private final String index;

  public final String dateMethod;
  public final String rerankMethod;

  public final Parameters conf;
  public final String id;
  public final DataSet dataset;
  private Retrieval retrieval;
  private HashMap<String,DiskMapWrapper<String,String>> metadataCaches;

  public AthenaKind(DataSet dataset, String id, Parameters conf) {
    this.dataset = dataset;
    this.id = id;
    this.conf = conf;
    this.index = conf.getString("index");
    this.dateMethod = conf.get("dateMethod", "none");
    this.rerankMethod = conf.get("rerankMethod", "none");
    this.retrieval = null;
    this.metadataCaches = new HashMap<String, DiskMapWrapper<String, String>>();
  }

  public Retrieval getRetrieval() {
    if(retrieval == null) {
      try {
        retrieval = RetrievalFactory.instance(conf);
      } catch (Exception e) {
        throw new RuntimeException("Couldn't instantiate a retrieval from this kind: " + id, e);
      }
    }
    return retrieval;
  }

  public DiskMapWrapper<String,String> getFastMetadata(String field) throws IOException {
    if(!metadataCaches.containsKey(field)) {
      File indexPath = new File(index);
      File forField = new File(indexPath, "metadata."+field);
      if(!forField.exists())
        return null;

      metadataCaches.put(field, new DiskMapWrapper<String, String>(forField, new DiskMapWrapper.StringCodec(), new DiskMapWrapper.StringCodec()));
    }
    return metadataCaches.get(field);
  }

  public List<ScoredDate> extractDates(String dateMethod, List<ScoredDocument> docs) throws IOException {
    return ExtractDates.extract(this, dateMethod, docs);
  }

  public String getDateMethod() {
    return dateMethod;
  }

  public String getRerankMethod() {
    return rerankMethod;
  }
}
