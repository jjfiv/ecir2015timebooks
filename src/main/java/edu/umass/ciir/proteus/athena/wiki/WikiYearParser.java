package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.galagotools.utils.*;
import edu.umass.ciir.proteus.athena.Tool;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.tupleflow.Utility;

import java.io.File;
import java.util.*;

/**
 * @author jfoley.
 */
public class WikiYearParser implements Tool {

  @Override
  public String getName() {
    return "wiki-year-parser";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    List<File> inputFiles = Util.checkAndExpandPaths(argp.getAsList("input", String.class));

    for(File fp : inputFiles) {
      XML.forFieldsInSections(fp, "page", Arrays.asList("title", "text"), new XML.FieldsFunctor() {
        @Override
        public void process(Map<String, String> data) {
          WikiYearParser.process(data.get("title"), data.get("text"));
        }
      });
    }
  }


  public static int getHeaderLevel(String input) {
    if(!input.startsWith("=")) return 0;
    if(input.startsWith("====")) return 4;
    if(input.startsWith("===")) return 3;
    if(input.startsWith("==")) return 2;
    if(input.startsWith("=")) return 1;
    System.err.println("Unknown header level: "+input);
    return -1;
  }
  public static int getListLevel(String input) {
    if(!input.startsWith("*")) return 0;
    if(input.startsWith("****")) return 4;
    if(input.startsWith("***")) return 3;
    if(input.startsWith("**")) return 2;
    if(input.startsWith("*")) return 1;
    System.err.println("Unknown list level: "+input);
    return -1;
  }

  public static class WikiForm {
    public int listLevel;
    public int headerLevel;
    public String text;
    String kind;
    public WikiForm(String text, int headerLevel, int listLevel, String kind) {
      this.text = text;
      this.headerLevel = headerLevel;
      this.listLevel = listLevel;
      this.kind = kind;
    }

    public boolean important() {
      return kind.equals("list") || kind.equals("header");
    }

    public String level() {
      return headerLevel+"."+listLevel;
    }

    @Override
    public String toString() {
      return headerLevel+"."+listLevel+". "+text;
    }
  }

  public static ArrayList<WikiForm> tokenize(String title, String eventSection) {
    String section = WikiCleaner.clean(title, eventSection);

    final ArrayList<WikiForm> lines = new ArrayList<WikiForm>();
    IO.forEachLineInStr(section, new WikiLineHandler(lines));

    return lines;
  }

  public static WikiSection parseTree(String title, List<WikiForm> forms, String currentLevel) {
    WikiSection top = new WikiSection();
    top.text = title;

    List<Boolean> done = new ArrayList<Boolean>(forms.size());
    for (WikiForm ignored : forms) {
      done.add(false);
    }

    // next level is first or first header/list
    String nextLevel = forms.get(0).level();
    for(WikiForm fm : forms) {
      if(fm.important()) {
        nextLevel = fm.level();
        break;
      }
    }

    List<Integer> atNextLevel = new ArrayList<Integer>();
    for (int i = 0; i < forms.size(); i++) {
      WikiForm form = forms.get(i);
      //System.out.println(currentLevel + " " +form.level() + " " +form.text);
      if (form.level().equals(currentLevel)) {
        if (form.kind.equals("paragraph")) {
          top.paragraphs.add(form.text);
          done.set(i, true);
          //System.out.println("Assign: " + form.text + " to: " + title);
        }
      } else if(form.level().equals(nextLevel) && form.important()) {
        atNextLevel.add(i);
      }
    }
    //System.out.println("Headers: "+atNextLevel);

    // reduce the forms and recurse
    for(int headerIdx = 0; headerIdx<atNextLevel.size(); headerIdx++) {
      int headerStart = atNextLevel.get(headerIdx);
      int headerEnd;
      if(headerIdx+1 == atNextLevel.size()) {
        headerEnd = forms.size();
      } else {
        headerEnd = atNextLevel.get(headerIdx+1);
      }

      //System.out.println(String.format("(%d,%d)", headerStart, headerEnd));
      WikiForm headerForm = forms.get(headerStart);

      // no children
      List<WikiForm> childForms = new ArrayList<WikiForm>();
      for (int i = headerStart + 1; i < headerEnd; i++) {
        if (!done.get(i)) childForms.add(forms.get(i));
      }

      WikiSection child;
      if(childForms.isEmpty()) {
        child = new WikiSection();
        child.text = headerForm.text;
      } else {
        child = parseTree(headerForm.text, childForms, headerForm.level());
      }
      top.children.add(child);
    }

    return top;
  }

  private static void process(String title, String page) {
    if(title.trim().isEmpty()) return;
    if(!title.contains("BC")) {
      int year = Integer.parseInt(title);
      if(year > 2013) // skip the future
        return;
    }

    int offset = EventSectionFinder.findEventSection(page);
    if(offset < 0) {
      return;
    }

    //System.out.println("# "+title);
    List<WikiForm> lines = tokenize(title, page.substring(offset));
    WikiSection parsed = parseTree(title, lines, "page");
    cleanSections(parsed);
    //System.out.println(parsed);
    List<String> facts = convertToFacts(parsed);
    for(String f : facts) {
      System.out.println(f);
    }
  }

  public static String bulletJoin(List<String> prefixes, String bulletText) {
    String word = StrUtil.firstWord(bulletText);
    String last = Util.last(prefixes);
    int lastUsefulPrefix = prefixes.size();
    if (DateUtil.isMonth(word)) {
      if (DateUtil.isMonth(last) || DateUtil.isMonthDay(last)) {
        lastUsefulPrefix--;
      }
    }

    ArrayList<String> usefulPrefixes = new ArrayList<String>();
    usefulPrefixes.addAll(prefixes.subList(0,Math.max(0,lastUsefulPrefix)));

    String firstPrefix = Util.first(usefulPrefixes);
    List<String> restPrefixes = Util.rest(usefulPrefixes);

    if(firstPrefix == null) {
      System.err.println("# "+prefixes+" ``"+bulletText+"''");
    }

    String somePrefix = firstPrefix+"\t"+Utility.join(restPrefixes, " ");
    if(restPrefixes.isEmpty()) {
      return somePrefix + bulletText;
    } else return somePrefix+". "+bulletText;
  }

  /** Linearize trees - recursive step */
  private static void recursiveGenerateFacts(List<String> facts, WikiSection current, List<String> prefix) {
    if(current.isLeaf()) {
      String text = current.text;
      for(String para : current.paragraphs) {
        text += ' ' + para;
      }
      facts.add(bulletJoin(prefix, text));
      return;
    }
    ArrayList<String> newPrefix = new ArrayList<String>(prefix);
    if (!isStopHeader(current.text)) {
      newPrefix.add(current.text);
      // recurse to children only on "Events"
      if(current.text.toLowerCase().contains("birth") || current.text.toLowerCase().contains("death"))
        return;
    }
    for(WikiSection child : current.children) {
      recursiveGenerateFacts(facts, child, newPrefix);
    }
  }
  /** Linearize trees - initial step */
  public static List<String> convertToFacts(WikiSection parsed) {
    if(parsed.isLeaf()) return Collections.emptyList();

    ArrayList<String> facts = new ArrayList<String>();
    recursiveGenerateFacts(facts, parsed, new ArrayList<String>());
    return facts;
  }

  public static void cleanSections(WikiSection input) {
    ArrayList<String> newParagraphs = new ArrayList<String>(input.paragraphs.size());
    for(String para : input.paragraphs) {
      if(para.trim().isEmpty())
        continue;
      newParagraphs.add(para);
    }
    input.paragraphs = newParagraphs;

    ArrayList<WikiSection> nonEmptyChildren = new ArrayList<WikiSection>(input.children.size());
    // tree flatten as we collect non-empty-children
    Queue<WikiSection> childrenToProcess = new LinkedList<WikiSection>();
    childrenToProcess.addAll(input.children);
    while(!childrenToProcess.isEmpty()) {
      WikiSection ws = childrenToProcess.poll();
      String node = ws.text.trim();
      if(node.trim().isEmpty()) {
        input.paragraphs.addAll(ws.paragraphs);
        childrenToProcess.addAll(ws.children);
        continue;
      }
      nonEmptyChildren.add(ws);
    }

    // filter the children
    ArrayList<WikiSection> newChildren = new ArrayList<WikiSection>(nonEmptyChildren.size());
    for(WikiSection ws : nonEmptyChildren) {
      String node = ws.text.toLowerCase();
      if(node.equals("external links"))
        continue;
      if(node.equals("references"))
        continue;
      if(node.equals("in fiction"))
        continue;
      if(node.equals("see also"))
        continue;
      if(node.equals("nobel prizes"))
        continue;
      if(node.trim().isEmpty()) {
        input.paragraphs.addAll(ws.paragraphs);
        continue;
      }
      newChildren.add(ws);
    }

    // recurse over tree; keeping meaningful children
    ArrayList<WikiSection> finalChildren = new ArrayList<WikiSection>(newChildren.size());
    for(WikiSection section : newChildren) {
      cleanSections(section);
      if(section.children.isEmpty() && (section.text.isEmpty() || isHeader(section.text)) && section.paragraphs.isEmpty())
        continue;
      finalChildren.add(section);
    }
    input.children = finalChildren;
  }

  private static String[] stopHeader = {
    "event",
    "events",
    "by place",
    "by topic",
    "unknown",
    "date unknown",
    "dates unknown",
    "unknown date",
    "unknown dates",
    "january-december", // This is a whole year, wikipedia! COME ON!
  };

  private static String[] justHeader = {
    "births",
    "birth",
    "deaths",
    "death",
    "specific date of death unknown"
  };

  public static boolean isStopHeader(String text) {
    String lt = text.toLowerCase();
    for(String stop : stopHeader) {
      if(lt.equals(stop))
        return true;
    }
    return false;
  }

  public static boolean isHeader(String text) {
    String lt = text.toLowerCase();
    for(String header : justHeader) {
      if(lt.equals(header))
        return true;
    }
    return isStopHeader(text);
  }

  public static class WikiSection {
    public String text;
    public List<WikiSection> children;
    public List<String> paragraphs;

    public WikiSection() {
      paragraphs = new ArrayList<String>();
      children = new ArrayList<WikiSection>();
    }

    public WikiSection get(int index) {
      return children.get(index);
    }

    public int size() {
      return children.size();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      toStr(sb, "", this);
      return sb.toString();
    }

    public static void toStr(StringBuilder sb, String indent, WikiSection which) {
      sb.append(indent).append("- ").append(StrUtil.preview(which.text, 60)).append('\n');
      for(String para : which.paragraphs) {
        sb.append(indent).append("p ").append(StrUtil.preview(para, 60)).append('\n');
      }
      for(WikiSection child : which.children) {
        toStr(sb, indent+"  ", child);
      }
    }

    public boolean isLeaf() {
      return children.size() == 0;
    }
  }

  private static class WikiLineHandler implements IO.StringFunctor {
    private final ArrayList<WikiForm> lines;
    boolean lastLineBlank;
    int listLevel;
    int headerLevel;
    WikiForm lastForm;

    public WikiLineHandler(ArrayList<WikiForm> lines) {
      this.lines = lines;
      lastLineBlank = false;
      listLevel = 0;
      headerLevel = 0;
      lastForm = null;
    }

    public void add(WikiForm form) {
      lastForm = form;
      this.listLevel = form.listLevel;
      this.headerLevel = form.headerLevel;
      lines.add(form);
    }

    @Override
    public void process(String input) {
      input = input.trim();
      if(input.isEmpty()) {
        lastLineBlank = true;
        return;
      }

      boolean prevLineBlank = lastLineBlank;
      lastLineBlank = false;

      int level = getHeaderLevel(input);
      if(level > 0) {
        String name = input.replaceAll("=", "").trim();
        add(new WikiForm(name, level, 0, "header"));
        return;
      }
      level = getListLevel(input);
      if(level > 0) {
        String name = input.replaceAll("\\*", "").trim();
        add(new WikiForm(name, headerLevel, level, "list"));
        return;
      }
      // either add to last or start paragraph
      if(prevLineBlank || lastForm == null || lastForm.kind.equals("header")) {
        add(new WikiForm(input, headerLevel, listLevel, "paragraph"));
      } else {
        lastForm.text += " " + input;
      }
    }
  }
}
