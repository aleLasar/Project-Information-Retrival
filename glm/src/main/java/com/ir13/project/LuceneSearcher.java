package com.ir13.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityBenchmark;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityQueryParser;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.benchmark.quality.trec.TrecJudge;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.benchmark.quality.utils.SimpleQQParser;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;

public class LuceneSearcher {

    public static void runQueries(String queryfile, Analyzer analyzer, String indexDir, String outputRun, String[] topicFiles, String qrelsPath, String outputStats) {

        try (
                Directory dir = FSDirectory.open(Paths.get(indexDir));
                IndexReader reader = DirectoryReader.open(dir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Similarity similarity = new LMJelinekMercerSimilarity((float) 0.2);
            searcher.setSimilarity(similarity);

            //aggiungo campi delle query
            QualityQueryParser topicTRECParser = new SimpleQQParser(new String[]{"title", "description", "narrative"}, "text");

            //per ogni file di query
            for (String topicFile : topicFiles) {
                try (
                        BufferedReader topicFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(queryfile + topicFile))));
                        BufferedReader qrelsReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(qrelsPath + topicFile))));
                        PrintWriter loggerRun = new PrintWriter(new File(outputStats + topicFile));
                        PrintWriter loggerStats = new PrintWriter(new File(outputRun + topicFile));) {

                    //leggo le query
                    QualityQuery[] topicsTREC = new TrecTopicsReader().readQueries(topicFileReader);

                    QualityBenchmark run = new QualityBenchmark(topicsTREC, topicTRECParser, searcher, "doc_number");

                    //Una volta calcolato va dato in pasto a execute
                    TrecJudge judge = new TrecJudge(qrelsReader);

                    // validate topics & judgments match each other
                    judge.validateData(topicsTREC, loggerRun);
                    QualityStats stats[] = run.execute(judge, new SubmissionReport(loggerStats, "RUN"), loggerRun);

                    QualityStats avg = QualityStats.average(stats);
                    System.out.println("Topics: " + topicFile + "\n*********");
                    System.out.println("MAP: " + avg.getAvp());
                    System.out.println("GMAP: " + Math.pow(LuceneSearcher.getProduct(stats), 1.0 / stats.length));
                    System.out.println("RECALL: " + avg.getRecall());
                    System.out.println("\n\n\n");

                } catch (Exception ex) {
                    Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static double getProduct(QualityStats[] stats) {
        double product = 1;
        for (QualityStats stat : stats) {
            product *= stat.getAvp();
        }
        return product;
    }

}
