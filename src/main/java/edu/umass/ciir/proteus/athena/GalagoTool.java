package edu.umass.ciir.proteus.athena;

import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.utility.Parameters;

/**
 * @author jfoley
 */
public class GalagoTool implements Tool {
  private final AppFunction appfn;

  public GalagoTool(AppFunction appfn) {
    this.appfn = appfn;
  }

  @Override
  public String getName() {
    return appfn.getName();
  }

  @Override
  public void run(Parameters argp) throws Exception {
    appfn.run(argp, System.out);
  }
}
