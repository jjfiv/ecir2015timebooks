package edu.umass.ciir.proteus.athena.galago;

import org.lemurproject.galago.core.index.Index;
import org.lemurproject.galago.core.index.KeyIterator;
import org.lemurproject.galago.core.index.corpus.CorpusReader;
import org.lemurproject.galago.core.index.corpus.CorpusReaderSource;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.index.source.DataSource;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentSource;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.iterator.DataIterator;
import org.lemurproject.galago.core.retrieval.processing.ScoringContext;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.core.util.DocumentSplitFactory;
import org.lemurproject.galago.utility.Parameters;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author jfoley
 */
public class GalagoUtil {
  public static <T extends KeyIterator> void forEachKey(T keyIter, Operation<T> action) throws IOException {
    while(!keyIter.isDone()) {
      action.process(keyIter);
      keyIter.nextKey();
    }
  }

  public static DocumentSplit split(String path) {
    return DocumentSplitFactory.file(path);
  }

  public static void forEachDocument(DocumentStreamParser parser, Operation<Document> action) throws IOException {
    try {
      while (true) {
        Document doc = parser.nextDocument();
        if (doc == null) break;
        action.process(doc);
      }
    } finally {
      parser.close();
    }
  }

  public static void readEach(List<DocumentSplit> splits, Operation<BufferedReader> action) throws IOException {
    for (DocumentSplit split : splits) {
      try (BufferedReader reader = DocumentStreamParser.getBufferedReader(split)) {
        action.process(reader);
      }
    }
  }

  public static void forCorpusDocument(CorpusReaderSource corpusReaderSource, Operation<Document> action) throws IOException {
    while(!corpusReaderSource.isDone()) {
      long curId = corpusReaderSource.currentCandidate();
      Document doc = corpusReaderSource.data(curId);
      action.process(doc);
      corpusReaderSource.movePast(curId);
    }
  }

  public static List<String> names(Retrieval ret) throws IOException {
    ArrayList<String> out = new ArrayList<>();
    assert(ret instanceof LocalRetrieval);
    Index index = ((LocalRetrieval) ret).getIndex();
    int numNames = (int) index.getIndexPart("names").getManifest().getLong("keyCount");
    out.ensureCapacity(numNames);

    for (String name : asIterable(index.getNamesIterator())) {
      out.add(name);
    }
    return out;
  }

  public static List<String> names(List<ScoredDocument> documents) {
    ArrayList<String> names = new ArrayList<>(documents.size());
    for(ScoredDocument sdoc : documents) {
      names.add(sdoc.documentName);
    }
    return names;
  }

  private static <T> Iterator<T> asIterator(final DataIterator<T> galagoDataIter) {
    final ScoringContext ctx = new ScoringContext();
    return new Iterator<T>() {
      @Override
      public boolean hasNext() {
        return !galagoDataIter.isDone();
      }

      @Override
      public T next() {
        ctx.document = galagoDataIter.currentCandidate();
        T obj = galagoDataIter.data(ctx);
        try {
          galagoDataIter.movePast(ctx.document);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return obj;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Read-only iterator");
      }
    };
  }

  public static <T> Iterable<T> asIterable(final DataIterator<T> galagoDataIter) {
    return new Iterable<T>() {
      boolean used = false;
      @Override
      public Iterator<T> iterator() {
        if(used) throw new IllegalStateException("Used DataIterator as Iterable twice!");
        used=true;
        return asIterator(galagoDataIter);
      }
    };
  }

  private static <T> Iterator<T> asIterator(final DataSource<T> source) {
    return new ReadOnlyIterator<T>() {
			@Override
			public void close() throws Exception {  }

			@Override
      public boolean hasNext() {
        return !source.isDone();
      }

      @Override
      public T next() {
        try {
          long doc = source.currentCandidate();
          T obj = source.data(doc);
          source.movePast(doc);
          return obj;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  public static <T> Iterable<T> asIterable(final DataSource<T> src) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return asIterator(src);
      }
    };
  }

  public static Iterable<Document> documentIterable(DiskIndex index, Document.DocumentComponents opts) throws IOException {
    CorpusReader corpus = (CorpusReader) index.getIndexPart("corpus");

    CorpusReaderSource source = corpus.getIterator().getSource(opts);
    return asIterable(source);
  }

  /** Do processing similar to that found in DocumentSource.run() of Galago */
  public static List<DocumentSplit> getDocumentSplits(Collection<String> inputPaths, Parameters argp) throws IOException {
    List<DocumentSplit> inputs = new ArrayList<>(inputPaths.size());
    for (String inputPath : inputPaths) {
      File fp = new File(inputPath);
      if(fp.isDirectory()) {
        inputs.addAll(DocumentSource.processDirectory(fp, argp));
      } else if(fp.isFile()) {
        inputs.addAll(DocumentSource.processFile(fp, argp));
      }
    }
    return inputs;
  }

  public static Iterable<Document> documentsStreamIterable(final DocumentStreamParser parser) {
    return new Iterable<Document>() {
      @Override
      public Iterator<Document> iterator() {
        try {
          return new DocumentStreamIterator(parser);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }




  private static class DocumentStreamIterator extends ReadOnlyIterator<Document> implements Closeable {
    private DocumentStreamParser parser;
    private Document current;

    public DocumentStreamIterator(DocumentStreamParser parser) throws IOException {
      this.parser = parser;
      this.current = parser.nextDocument();
    }

    @Override
    public boolean hasNext() {
      return current != null;
    }

    @Override
    public Document next() {
      try {
        Document prev = current;
        current = parser.nextDocument();
        return prev;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void close() throws IOException {
      parser.close();
    }
  }

	public static abstract class ReadOnlyIterator<T> implements Iterator<T>, AutoCloseable {
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}

