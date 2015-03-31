package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.galagotools.utils.Util;
import edu.umass.ciir.galagotools.utils.XML;
import edu.umass.ciir.proteus.athena.Tool;
import org.lemurproject.galago.utility.Parameters;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jfoley
 */
public class WikipediaYearFinder implements Tool {
  // 1783 and 650_BC are all okay.
  public static Pattern wikipediaYearPageTitle = Pattern.compile("^\\d{1,4}(\\sBC)?$");

  @Override
  public String getName() {
    return "wiki-year-finder";
  }

  public static boolean wantTitle(String title) {
    if(!Character.isDigit(title.charAt(0)))
      return false;
    Matcher m = wikipediaYearPageTitle.matcher(title);
    return m.matches();
  }

  private void collectYearDocuments(File fp, XMLStreamWriter out) throws IOException, XMLStreamException {
    XMLStreamReader xml = XML.openXMLStream(fp);

    int numSkipped = 0;
    int numCollected = 0;
    boolean inPage = false;
    boolean inTitle = false;
    boolean wantData = false;
    StringBuilder titleBuilder = new StringBuilder();

    while(xml.hasNext()) {
      int event = xml.next();

      if(event == XMLStreamConstants.START_ELEMENT) {
        String tagName = xml.getLocalName();
        if (tagName.equals("page")) {
          inPage = true;
        } else if (tagName.equals("title")) {
          inTitle = true;
          titleBuilder = new StringBuilder();
        } else if(wantData) {
          out.writeStartElement(tagName);
        }
      } else if(event == XMLStreamConstants.END_ELEMENT) {
        String tagName = xml.getLocalName();
        if (inPage && tagName.equals("page")) {
          inPage = false;
          if(wantData) {
            numCollected++;
            out.writeEndElement(); // </page>
          } else {
            numSkipped++;
          }

          if(numCollected+numSkipped % 100 == 0) {
            System.err.println("# collected: " + numCollected + " skipped: " + numSkipped);
          }
          wantData = false;
        } else if (tagName.equals("title")) {
          inTitle = false;
          if(wantTitle(titleBuilder.toString())) {
            wantData = true;
            out.writeStartElement("page");
            out.writeStartElement("title");
            out.writeCharacters(titleBuilder.toString());
            out.writeEndElement(); // </title>
          }
        } else if(wantData) {
          out.writeEndElement();
        }
      } else if(event == XMLStreamConstants.CDATA || event == XMLStreamConstants.CHARACTERS) {
        if(inPage && inTitle) {
          titleBuilder.append(xml.getText());
        } else if(wantData) {
          out.writeCharacters(xml.getText());
        }
      }
    } // xml loop
  }

  @Override
  public void run(Parameters argp) throws Exception {
    List<File> inputFiles = Util.checkAndExpandPaths(argp.getAsList("input", String.class));
    XMLStreamWriter out = XML.writeXMLStream(argp.getString("output"));

    out.writeStartDocument();
    out.writeStartElement("pages");

    try {
      // assume each of these is a Wikipedia dump
      for (File fp : inputFiles) {
        collectYearDocuments(fp, out);
      }
    } finally {
      out.writeEndElement(); //</pages>
      out.writeEndDocument();
      out.close();
    }
  }

}
