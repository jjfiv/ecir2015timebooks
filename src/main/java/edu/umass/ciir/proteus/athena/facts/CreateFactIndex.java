package edu.umass.ciir.proteus.athena.facts;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.Athena;
import edu.umass.ciir.galagotools.parser.JSONDocParser;
import org.lemurproject.galago.core.tools.apps.BuildIndex;
import org.lemurproject.galago.tupleflow.FileUtility;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author jfoley.
 */
public class CreateFactIndex implements Tool {
  @Override
  public String getName() {
    return "create-fact-index";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    System.err.println(argp.getString("indexPath"));
    List<FactQuery> facts = Athena.init(argp).getDataset().getAllFacts();

    File temp = FileUtility.createTemporary();
    PrintWriter pw = new PrintWriter(temp);

    for(FactQuery fq: facts) {
      pw.println(Parameters.parseArray(
          "name", fq.id,
          "text", fq.text,
          "meta", Parameters.parseArray(
              "year", fq.rel
          )
      ));
    }

    pw.close();

    Parameters buildP = argp.clone();
    buildP.put("fields", "a");
    buildP.put("filetype", JSONDocParser.class.getName());
    buildP.put("inputPath", temp.getAbsolutePath());

    BuildIndex build = new BuildIndex();
    build.run(buildP, System.out);

    System.out.println("Cleaned up temporary file:"+temp.delete());
  }
}
