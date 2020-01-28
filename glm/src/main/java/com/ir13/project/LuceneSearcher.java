package com.ir13.project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityBenchmark;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityQueryParser;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.benchmark.quality.trec.*;
import org.apache.lucene.benchmark.quality.utils.SimpleQQParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;

public class LuceneSearcher {

    public static void runQueries(String queryfile, Analyzer analyzer, String indexDir, String outputVSM, String[] topicFiles, String qrelsPath) {
        try {
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            try (IndexReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Similarity similarity = new LMJelinekMercerSimilarity((float) 0.2);
                searcher.setSimilarity(similarity);

                //aggiungo campi delle query
                String topicTRECNames[] = {"title", "description", "narrative"};
                QualityQueryParser topicTRECParser = new SimpleQQParser(topicTRECNames, "text");

                //query parser
                String[] fields = {"text", "title"};
                MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                        fields,
                        analyzer
                );

                for (String topicFile : topicFiles) {
                    //try-with-resources
                    try (
                            BufferedReader topicFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(queryfile + topicFile))));
                            BufferedWriter writerVSM = new BufferedWriter(new FileWriter(new File(outputVSM + topicFile)));
                            BufferedReader qrelsReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(qrelsPath + topicFile))))) {

                        QualityQuery[] topicsTREC = new TrecTopicsReader().readQueries(topicFileReader);
                        //Questo calcola i file con le run
                        for (QualityQuery topicTREC : topicsTREC) {
                            System.out.println("Parsing Query ID:" + topicTREC.getQueryID());
                            BooleanQuery bq = new BooleanQuery();
                            for (String topicTRECName : topicTRECNames) {
                                bq.add(queryParser.parse(QueryParserBase.escape(topicTREC.getValue(topicTRECName))), BooleanClause.Occur.SHOULD);
                            }
                            //per ogni query, quali sono i top 1000 documenti?
                            TopDocs td = searcher.search(bq, 1000);
                            generateTrecResultsFile(writerVSM, td.scoreDocs, topicTREC.getQueryID(), searcher);
                        }

                        QualityBenchmark run = new QualityBenchmark(topicsTREC, topicTRECParser, searcher, "text");
                        run.setMaxResults(1000);
                        run.setMaxQueries(50);
                        PrintWriter logger = new PrintWriter(new File("src/main/resources/logger/" + topicFile));
                        //Una volta calcolato va dato in pasto a execute
                        TrecJudge judge = new TrecJudge(qrelsReader);

                        // validate topics & judgments match each other
                        judge.validateData(topicsTREC, logger);
                        QualityStats stats[] = run.execute(judge, null, logger);
                        QualityStats avg = QualityStats.average(stats);
                        avg.log("SUMMARY", 2, logger, "  ");
                        break;
                    } catch (ParseException ex) {
                        Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void generateTrecResultsFile(BufferedWriter writer, ScoreDoc[] hits, String queryIndex, IndexSearcher searcher)
            throws IOException {
        int hitCounter = 0;

        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document doc = searcher.doc(docId);
            writer.write(queryIndex + " Q0 " + doc.get("doc_number") + " " + hitCounter + " " + hit.score
                    + " lucene" + "\n");
            hitCounter++;
        }
    }

}
