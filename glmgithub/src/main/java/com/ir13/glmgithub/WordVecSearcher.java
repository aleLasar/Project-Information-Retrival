/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ir13.glmgithub;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.benchmark.quality.QualityBenchmark;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityQueryParser;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.benchmark.quality.trec.TrecJudge;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.benchmark.quality.utils.SimpleQQParser;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.PerFieldSimilarityWrapper;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Debasis
 */
class CombinationSimilarity extends PerFieldSimilarityWrapper {

    float lambda;

    public CombinationSimilarity() {
        lambda = 0.2f;
    }

    @Override
    public Similarity get(String fieldName) {
        Similarity[] sims = {
            new LMJelinekMercerSimilarity(lambda),
            new LMDirichletSimilarity(),
            new BM25Similarity()};

        if (fieldName.equals(WordVecIndexer.FIELD_BAG_OF_WORDS)) {
            return new MultiSimilarity(sims);
        } else {
            return new GeneralizedLMSimilarity();
        }
    }
}

public class WordVecSearcher {

    IndexReader reader;
    IndexSearcher searcher;
    int numWanted;      // number of result to be retrieved
    HashMap<Integer, Float> docScorePredictionMap;
    boolean isSupervised;
    WordVecIndexer wvIndexer;
    String runName;     // name of the run
    float lambda, mu, alpha, beta;   // mu < 1; lambda + alpha < 1
    String stop_file;
    HashMap<String, Integer> stop_map;

    String topicsFile;

    public WordVecSearcher(String topicsFile, float alpha, float beta) throws Exception {

        // Why is the indexer opened in write mode here ?
        wvIndexer = new WordVecIndexer();
        String wvIndex_dir = "src/main/resources/expand1";
        this.topicsFile = topicsFile;

        stop_file = "src/main/resources/smart-stopwords";
        stop_map = new HashMap<>();

        String line;

        try {
            int count = 1;
            try (FileReader fr = new FileReader(stop_file); BufferedReader br = new BufferedReader(fr)) {
                while ((line = br.readLine()) != null) {
                    stop_map.put(line.trim(), count);
                    count = count + 1;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("Running queries against index: " + wvIndex_dir);
        try {
            File indexDir;
            indexDir = new File(wvIndex_dir);
            reader = DirectoryReader.open(FSDirectory.open(indexDir));
            searcher = new IndexSearcher(reader);

            runName = "glm_word2vec";

            // Is the similarity function called after the below statement if so then how ??
            searcher.setSimilarity(new GeneralizedLMSimilarity());
            numWanted = 1000;

            lambda = 0.2f;
            mu = 0;
            this.alpha = alpha;
            this.beta = beta;

        } catch (IOException | NumberFormatException ex) {
            ex.printStackTrace();
        }
    }

    List<TRECQuery> constructQueries() throws Exception {
        TRECQueryParser parser = new TRECQueryParser("src/main/resources/query/topics." + topicsFile, this);
        parser.parse();
        return parser.queries;
    }

    public void retrieveAll() throws Exception {
        ScoreDoc[] hits;
        TopDocs topDocs;

        String resultsFile = "src/main/resources/res/" + topicsFile + alpha + beta;
        String runFile = "src/main/resources/run/" + topicsFile + alpha + beta;
        String statsFile = "src/main/resources/stats/" + topicsFile + alpha + beta;
        String qrelsFile = "src/main/resources/qrels/qrels." + topicsFile;
        try (
                FileWriter fw = new FileWriter(resultsFile); PrintWriter loggerRun = new PrintWriter(new File(runFile));
                PrintWriter loggerStats = new PrintWriter(new File(statsFile));
                BufferedReader qrelsReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(qrelsFile)))); BufferedReader topicFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("src/main/resources/query/topics." + topicsFile))));) {
            List<TRECQuery> queries = constructQueries();
            ArrayList<QualityQuery> topicsTREC = new ArrayList<>();
            for (TRECQuery query : queries) {
                TopScoreDocCollector collector = TopScoreDocCollector.create(numWanted, true);
                Query luceneQry = query.getWVQuery(lambda, mu, alpha, beta, wvIndexer.getAnalyzer(), stop_map);
                //System.out.println(luceneQry);

                searcher.search(luceneQry, collector);
                topDocs = collector.topDocs();
                hits = topDocs.scoreDocs;
                if (hits == null) {
                    System.out.println("Nothing found");
                }

                System.out.println("Retrieved results for query: " + query.id);
                StringBuilder buff = new StringBuilder();
                int hits_length = hits.length;
                System.out.println("Retrieved Length: " + hits_length);
                for (int i = 0; i < hits_length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
                    buff.append(query.id).append("\tQ0\t").
                            append(d.get(WordVecIndexer.FIELD_ID)).append("\t").
                            append((i)).append("\t").
                            append(hits[i].score).append("\t").
                            append(runName).append("\n");
                }
                fw.write(buff.toString());

                /*HashMap<String, String> valori = new HashMap<>();
                valori.put("title", query.title);
                valori.put("description", query.desc);
                valori.put("narrative", query.narr);
                QualityQuery topicTREC = new QualityQuery(query.id, valori);
                topicsTREC.add(topicTREC);*/
            }
            /*QualityQueryParser topicTRECParser = new SimpleQQParser(new String[]{"title", "description", "narrative"}, "wv_d");
            QualityQuery[] topicsTRECArray = topicsTREC.toArray(new QualityQuery[topicsTREC.size()]);

            /*new TrecTopicsReader().readQueries(topicFileReader);
            QualityBenchmark run = new QualityBenchmark(topicsTRECArray, topicTRECParser, searcher, "id");

            //Una volta calcolato va dato in pasto a execute
            TrecJudge judge = new TrecJudge(qrelsReader);

            // validate topics & judgments match each other
            judge.validateData(topicsTRECArray, loggerRun);

            QualityStats stats[] = run.execute(judge, new SubmissionReport(loggerRun, "RUN"), loggerStats);

            QualityStats avg = QualityStats.average(stats);
            System.out.println("Topics: " + topicsFile + "\n*********");
            System.out.println("MAP: " + avg.getAvp());
            System.out.println("GMAP: " + Math.pow(WordVecSearcher.getProduct(stats), 1.0 / stats.length));
            System.out.println("RECALL: " + avg.getRecall());
            System.out.println("\n\n\n");*/
        }

    }

    private static double getProduct(QualityStats[] stats) {
        double product = 1;
        for (QualityStats stat : stats) {
            product *= stat.getAvp();
        }
        return product;
    }

    public void closeReader() throws IOException {
        reader.close();
    }

    public static void main(String[] args) {
        try {
            for (String topic : TOPICS) {
                for (float alpha : ALPHA) {
                    for (float beta : BETA) {
                        WordVecSearcher searcher = new WordVecSearcher(topic, alpha, beta);
                        searcher.retrieveAll();
                        searcher.closeReader();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static final String[] TOPICS = {"trec6.txt", "trec7.txt", "trec8.txt", "robust.2004.txt"};
    public static final float[] ALPHA = {0.1f, 0.2f, 0.3f, 0.4f};
    public static final float[] BETA = {0.1f, 0.2f, 0.3f, 0.4f};

}

