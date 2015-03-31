package edu.umass.ciir.proteus.athena.utils;

import org.lemurproject.galago.utility.StreamCreator;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jfoley
 */
public class XML {
  static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
  private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
  static {
    XML.xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
  }

  public static Map<String,String> getFields(XMLStreamReader xml, String endTag, List<String> fields) throws IOException, XMLStreamException {
    HashMap<String,StringBuilder> builders = new HashMap<>();
    for(String field : fields) {
      builders.put(field, new StringBuilder());
    }
    Set<String> keys = builders.keySet();

    // collect all listed tags
    String currentTag = null;
    while (xml.hasNext()) {
      int event = xml.next();

      if(event == XMLStreamConstants.START_ELEMENT) {
        String tagName = xml.getLocalName();
        if(keys.contains(tagName)) {
          currentTag = tagName;
        } else if(currentTag != null) {
          builders.get(currentTag).append("<").append(tagName).append(">");
        }
      } else if(event == XMLStreamConstants.END_ELEMENT) {
        String tagName = xml.getLocalName();
        if (tagName.equals(currentTag)) {
          currentTag = null;
        } else if(tagName.equals(endTag)) {
          break;
        } else if(currentTag != null) {
          builders.get(currentTag).append("</").append(tagName).append(">");
        }
      } else if(event == XMLStreamConstants.CDATA || event == XMLStreamConstants.CHARACTERS) {
        if(currentTag != null) {
          builders.get(currentTag).append(xml.getText());
        }
      }
    }

    // finish off builders
    HashMap<String,String> results = new HashMap<>();
    for(Map.Entry<String,StringBuilder> kv : builders.entrySet()) {
      results.put(kv.getKey(), kv.getValue().toString());
    }
    return results;
  }

  public static XMLStreamReader openXMLStream(InputStream is) throws IOException, XMLStreamException {
    return xmlInputFactory.createXMLStreamReader(is, "UTF-8");
  }
  public static XMLStreamReader openXMLStream(File fp) throws IOException, XMLStreamException {
    return openXMLStream(StreamCreator.openInputStream(fp));
  }

  public static XMLStreamWriter writeXMLStream(String output) throws IOException, XMLStreamException {
    return xmlOutputFactory.createXMLStreamWriter(StreamCreator.openOutputStream(output), "UTF-8");
  }
  public static XMLStreamWriter writeXMLStream(PrintStream out) throws XMLStreamException {
    return xmlOutputFactory.createXMLStreamWriter(out, "UTF-8");
  }
  public static XMLStreamWriter writeXMLStream(PrintWriter writer) throws XMLStreamException {
    return xmlOutputFactory.createXMLStreamWriter(writer);
  }

  public static org.w3c.dom.Document readFullXML(InputStream input) throws IOException {
    try {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
    } catch (SAXException | ParserConfigurationException e) {
      throw new IOException(e);
    }
  }

  public static interface IXMLAction {
    public void process(XMLStreamWriter xml) throws XMLStreamException, IOException;
  }
  public static String captureXML(IXMLAction action) throws XMLStreamException, IOException {
    StringWriter sw = new StringWriter();
    XMLStreamWriter xml = xmlOutputFactory.createXMLStreamWriter(sw);
    action.process(xml);
    xml.close();
    sw.close();
    return sw.toString();
  }

  public static interface FieldsFunctor {
    public void process(Map<String,String> fieldValues);
  }

  public static void forFieldsInSections(InputStream is,  String sectionTag, List<String> fields, FieldsFunctor operation) throws IOException, XMLStreamException {
    XMLStreamReader xml = null;
    try {
      xml = openXMLStream(is);

      while (xml.hasNext()) {
        Map<String,String> data = getFields(xml, sectionTag, fields);
        operation.process(data);
      }
    } finally {
      IO.close(xml);
    }
  }

  public static void forFieldsInSections(File fp, String sectionTag, List<String> fields, FieldsFunctor operation) throws IOException, XMLStreamException {
    forFieldsInSections(StreamCreator.openInputStream(fp), sectionTag, fields, operation);
  }

}
