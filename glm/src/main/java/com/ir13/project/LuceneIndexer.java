package com.ir13.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class LuceneIndexer {

    public static void index(String docsPath, String indexPath, Analyzer analyzer) {

        boolean create = true;

        final Path docDir = Paths.get(docsPath);

        // Check if the specified document directory exists and if JVM has permissions to read files
        // in this directory
        if (!Files.isReadable(docDir)) {
            System.out.println("Document directory '" + docDir.toAbsolutePath() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        // Get the system time when indexing starts so that we can determine the number of seconds
        // it takes to index the documents
        Date start = new Date();

        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            FileUtils.cleanDirectory(new File(indexPath));
            Directory dir = FSDirectory.open(Paths.get(indexPath));

            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            if (create) {
                iwc.setOpenMode(OpenMode.CREATE);
            } else {
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            }

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                System.out.println("indexDocs");
                indexDocs(writer, docDir);
            }

            // Get the current system time and print the time it took to index all documents
            Date end = new Date();
            System.out.println("Documents Indexed In " + ((end.getTime() - start.getTime()) / 1000.0) + " Seconds");
        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass()
                    + "\n with message: " + e.getMessage());
        }

    }

    /*
	 * This method crawls the documents path folder and then creates Document objects
	 * with all the files inside. The Document object instances are then added to the IndexWriter
     */
    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                        ignore.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    /*
	 * Indexes a document
     */
    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {

        // Call the parseHTML() method to parse the html file
        org.jsoup.nodes.Document document = parseHTML(file);

        for (Element e : document.getElementsByTag("doc")) {
            String doc_number;
            String text;
            String publication;
            String title;
            String date;
            Document doc = new Document();
            Field pathField = new StringField("path", file.toString(), Field.Store.YES);
            doc.add(pathField);

            if (file.toString().contains("FBIS")) {

                publication = "fbis";
                doc_number = e.getElementsByTag("docno").text();
                text = e.getElementsByTag("text").text();
                title = e.getElementsByTag("h3").text();
                date = e.getElementsByTag("date1").text();
                System.out.println("Doc Number:" + doc_number + "\n" + "Abs Text:" + text + "\n" + "Title:" + title + "\n" + "Date:" + date + "\n\n");
                addDocumentToIndex(doc, doc_number, text, title, date, publication, writer, file);
            } // Reading from Los Angeles Times (1989, 1990)- latimes
            else if (file.toString().contains("LATIMES")) {

                publication = "latimes";
                doc_number = e.getElementsByTag("docno").text();
                text = e.getElementsByTag("text").text();
                title = e.getElementsByTag("headline").text();
                date = e.getElementsByTag("date").text();
                System.out.println("Doc Number:" + doc_number + "\n" + "Abs Text:" + text + "\n" + "Title:" + title + "\n" + "Date:" + date + "\n\n");
                addDocumentToIndex(doc, doc_number, text, title, date, publication, writer, file);

            } // Reading from Financial Times Limited (1991, 1992, 1993, 1994)- ft
            else if (file.toString().contains("FT")) {

                publication = "ft";
                doc_number = e.getElementsByTag("docno").text();
                text = e.getElementsByTag("text").text();
                title = e.getElementsByTag("headline").text();
                date = e.getElementsByTag("date").text();
                System.out.println("Doc Number:" + doc_number + "\n" + "Abs Text:" + text + "\n" + "Title:" + title + "\n" + "Date:" + date + "\n\n");
                addDocumentToIndex(doc, doc_number, text, title, date, publication, writer, file);
            } // Reading from Fr94
            else if (file.toString().contains("FR94")) {
                publication = "fr94";
                doc_number = e.getElementsByTag("docno").text();
                text = org.jsoup.parser.Parser.unescapeEntities(e.getElementsByTag("text").text(), true).replaceAll("\\b&[^&]*;\\b", "");
                System.out.println("Doc Number:" + doc_number + "\n" + "Abs Text:" + text + "\n\n");
                addDocumentToIndex(doc, doc_number, text, "", "", publication, writer, file);
            }

        }
    }


    /*
	 * This method creates a Scanner object to read all lines from the html file.
	 * The raw html contents gets stored in the String rawContents.
	 * Jsoup then parses rawContents and creates a parsed document and returns it.
     */
    private static org.jsoup.nodes.Document parseHTML(Path file) throws IOException {

        // Parse the raw html using Jsoup
        org.jsoup.nodes.Document document = Jsoup.parse(new File(file.toString()), "utf-8");
        return document;
    }

    private static void addDocumentToIndex(Document doc, String doc_number, String text, String title, String date, String publication, IndexWriter writer, Path file) throws IOException {
        doc.add(new TextField("text", text, Field.Store.YES));
        doc.add(new StringField("doc_number", doc_number, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("date", date, Field.Store.YES));
        doc.add(new TextField("publication", publication, Field.Store.YES));
        if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            System.out.println("Adding " + file);
            writer.addDocument(doc);
        } else {
            // An index already exists and an old version of the file might exist
            // so we update the file
            System.out.println("Updating " + file);
            writer.updateDocument(new Term("path", file.toString()), doc);
        }
    }
}
