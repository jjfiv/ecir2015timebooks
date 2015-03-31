package edu.umass.ciir.proteus.athena;

import ciir.proteus.system.ProteusMain;
import edu.umass.ciir.galagotools.fns.AddMetadataPart;
import edu.umass.ciir.galagotools.fns.FindInterestingTerms;
import edu.umass.ciir.proteus.athena.cfg.DataCounts;
import edu.umass.ciir.proteus.athena.cfg.GetPubdate;
import edu.umass.ciir.proteus.athena.dates.*;
import edu.umass.ciir.proteus.athena.experiment.CompareRuns;
import edu.umass.ciir.proteus.athena.experiment.EvaluateRun;
import edu.umass.ciir.proteus.athena.experiment.GenerateFactRun;
import edu.umass.ciir.proteus.athena.experiment.GenerateQueryRun;
import edu.umass.ciir.proteus.athena.facts.*;
import edu.umass.ciir.proteus.athena.linking.LinkingExperiment;
import edu.umass.ciir.proteus.athena.preprocess.CollectPubDates;
import edu.umass.ciir.proteus.athena.preprocess.RobustCollectPubDates;
import edu.umass.ciir.proteus.athena.utils.StanfordNERJSONLines;
import edu.umass.ciir.proteus.athena.wiki.*;
import org.lemurproject.galago.core.tools.apps.BuildIndex;
import org.lemurproject.galago.core.tools.apps.DumpKeysFn;
import org.lemurproject.galago.core.tools.apps.DumpNamesLengths;
import org.lemurproject.galago.utility.Parameters;

/**
 * @author jfoley
 */
public class Main {

  public static void main(String [] args) throws Exception {
    final Tool[] tools = {
      new ExtractTimexSentences(),
      new ExtractWebTimexSentences(),

      new SentenceCollector(),
      new DocDateLMCollector(),
      new DocDatesBuilder(),

      // citations
      new WikiCitationFinder(),
      new WikiLinkFinder(),
      new CitationContextExtractor(),


      new WikipediaToHTML(),
      new FindNonEnglishDocs(),

      new WikipediaYearFinder(),
      new WikiYearParser(),

      new DateStatsExtractor(),
      new FactsToQrels(),
      new DataCounts(),
      new SampleFacts(),
      new TermCounts(), // aka stopword detection
      new GalagoTool(new FindInterestingTerms()),
      new DateDeltaExtractor(),
      new GetPubdate(),

      new CreateFactIndex(),
      new GalagoTool(new AddMetadataPart()),

      // evaluate
      new GenerateFactRun(),
      new GenerateQueryRun(),
      new EvaluateRun(),
      new CompareRuns(),

      // collect publication dates
      new CollectPubDates(),
      new RobustCollectPubDates(),

      // generate ambiguous queries
      new FindBigramEntities(),
      new EntityLinkExtractor(),

      new PageToSentenceTSV(),
      new GalagoTool(new BuildIndex()),
      new GalagoTool(new DumpNamesLengths()),
      new GalagoTool(new DumpKeysFn()),
      new GalagoTool(new ProteusMain()),

      // linking
      new LinkingExperiment(),
      new ExtractTextTagFromIndex(),
      new ExtractDatedSentences(),


      // new standard-annotation
      new StanfordNERJSONLines(),
    };

    Parameters argp = Parameters.parseArgs(args);

    if(!argp.containsKey("tool")) {
      showHelp(tools);
      return;
    }

    String toolName = argp.getString("tool");
    for(Tool tool : tools) {
      if(tool.getName().equals(toolName)) {
        tool.run(argp);
        return;
      }
    }

    showHelp(tools);
    throw new IllegalArgumentException("No tool found for `"+toolName+"'");
  }

  private static void showHelp(Tool[] tools) {
    for(Tool tool : tools) {
      System.out.println("--tool="+tool.getName());
    }
  }


}
