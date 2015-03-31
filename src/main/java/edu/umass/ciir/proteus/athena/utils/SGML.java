package edu.umass.ciir.proteus.athena.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author jfoley.
 */
public class SGML {
  public static String makeTag(String tagName, Map<String, String> attrs, String text) {

    Element el = new Element(Tag.valueOf(tagName), "/");
    for (String kv : attrs.keySet()) {
      el.attributes().put(kv, attrs.get(kv));
    }
    if(!text.isEmpty()) {
      el.appendText(text);
    }

    return StrUtil.compactSpaces(el.outerHtml());
  }

  public static interface TransformTag {
    public String process(Map<String,String> attrs, String body);
  }

  /**
   * Hack, doesn't support attributes.
   */
  public static String getTagContents(String xml, String tagName) {
    int startIndex = xml.indexOf("<"+tagName+">")+2+tagName.length();
    int endIndex = xml.indexOf("</"+tagName+">");
    assert(startIndex > 0);
    assert(endIndex > 0);
    return xml.substring(startIndex, endIndex);
  }

  public static String removeTag(String input, String tagName) {
    // do self-closing or content-based
    return StrUtil.removeBetween(input, Pattern.compile("<" + tagName), Pattern.compile("/\\s*>|</" + tagName + ">"));
  }

  public static Elements getTag(String input, String tagName) {
    return Jsoup.parseBodyFragment(input).select(tagName);
  }

  public static Map<String,String> getAttributesAsMap(Element el) {
    Attributes attrs = el.attributes();
    if(attrs.size() == 0) return Collections.emptyMap();

    HashMap<String,String> unorderedAttrs = new HashMap<>();
    for (Attribute attr : attrs.asList()) {
      unorderedAttrs.put(attr.getKey(), attr.getValue());
    }
    return unorderedAttrs;
  }

  public static String transformTag(String input, final String tagName, final TransformTag action) {
    return StrUtil.transformInclusive(input, Pattern.compile("<" + tagName), Pattern.compile("/\\s*>|</" + tagName + ">"), new StrUtil.Transform() {
      @Override
      public String process(String input) {
        StringBuilder output = new StringBuilder();
        Elements els = getTag(input, tagName);
        for(Element el : els) {
          output.append(action.process(getAttributesAsMap(el), el.text()));
        }
        return output.toString();
      }
    });
  }

  public static String removeTagsLeaveContents(String input) {
    return input.replaceAll("<[^>]+>", "");
  }

  public static String removeComments(String input) {
    return StrUtil.removeBetweenNested(input, "<!--", "-->");
  }


}
