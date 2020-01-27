package com.ir13.project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityBenchmark;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityQueryParser;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.benchmark.quality.trec.*;
import org.apache.lucene.benchmark.quality.utils.SimpleQQParser;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;

public class LuceneSearcher {

    public static void runQueries(String queryfile, Analyzer analyzer, String indexDir, String outputVSM, String[] topics) {
        try {
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            Similarity similarity[] = {new LMJelinekMercerSimilarity((float) 0.2)};
            // Set the similarity metrics to searcher
            searcher.setSimilarity(new MultiSimilarity(similarity));

            //aggiungo nomi query
            Set<String> fieldSet = new HashSet<>();
            fieldSet.add("title");
            fieldSet.add("description");
            fieldSet.add("narrative");
            String qqNames[] = fieldSet.toArray(new String[0]);

            //TODO vedere se serve
            //Query qqParser = MultiFieldQueryParser.parse(fieldSet.toArray(new String[0]),new String[]{"text", "title"},analyzer);
            QualityQueryParser qqParser = new SimpleQQParser(fieldSet.toArray(new String[0]), "text");

            QueryParser qp = new QueryParser("text", analyzer);
            QueryParser qp1 = new QueryParser("title", analyzer);

            String[] fields = new String[]{"text", "title"};
            HashMap<String, Float> boosts = new HashMap<>();
            boosts.put("text", (float) 12);
            boosts.put("title", (float) 2);
            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                    fields,
                    analyzer,
                    boosts
            );

            for (String topic : topics) {
                //TODO abilitare se serve un log completo del sistema
                /*SubmissionReport submitLog = new SubmissionReport(new PrintWriter(outputReport + "/" + topic, IOUtils.UTF_8), "lucene");*/
                try (
                        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(queryfile + "/" + topic))));
                        BufferedWriter bufferedWriterVSM = new BufferedWriter(new FileWriter(new File(outputVSM + "/" + topic)));) {
                    TrecTopicsReader ttr = new TrecTopicsReader();
                    QualityQuery[] qqs = ttr.readQueries(br);
                    //Questo calcola i file con le run
                    for (QualityQuery qq : qqs) {
                        System.out.println("Parsing Query ID:" + qq.getQueryID());
                        // generate query

                        BooleanQuery bq = new BooleanQuery();
//				    Query q = bq.build();
//				    BooleanQuery.Builder bq1 = new BooleanQuery.Builder();
//				    BooleanQuery.Builder bqt = new BooleanQuery.Builder();
                        for (String qqName : qqNames) {
                            bq.add(queryParser.parse(QueryParserBase.escape(qq.getValue(qqName))), BooleanClause.Occur.SHOULD);
                            //bq1.add(qp1.parse(QueryParserBase.escape(qq.getValue(qqNames[j]))), BooleanClause.Occur.SHOULD);
                        }
//				    bq.build().createWeight(searcher, true, 10);
//				    bq1.build().createWeight(searcher, true, 5);
//				    bqt.add(bq.build(),BooleanClause.Occur.SHOULD);
//				    bqt.add(bq1.build(),BooleanClause.Occur.SHOULD);
// search with this query
                        long t1 = System.currentTimeMillis();
                        TopDocs td = searcher.search(bq, 1000);
                        generateTrecResultsFile(bufferedWriterVSM, td.scoreDocs, qq.getQueryID(), searcher);
                        long searchTime = System.currentTimeMillis() - t1;
//most likely we either submit or judge, but check both
                        /*if (submitLog != null) {
                            submitLog.report(qq, td, "doc_number", searcher);
                        }*/
                    }

                    QualityBenchmark qrun = new QualityBenchmark(qqs, qqParser, searcher, "text");
                    qrun.setMaxResults(1000);
                    qrun.setMaxQueries(50);
                    PrintWriter logger = new PrintWriter(new OutputStreamWriter(System.out, Charset.defaultCharset()), true);

                    //Qui va calcolato TrecJudge.
                    //TrecJudge fa il parsing delle qrels.
                    /*
            public TrecJudge(BufferedReader reader)
            throws IOException
            Constructor from a reader.
            Expected input format:

            qnum  0   doc-name     is-relevant

            Two sample lines:

            19    0   doc303       1
            19    0   doc7295      0

            Parameters:
            reader - where judgments are read from.
            Throws:
            IOException - If there is a low-level I/O error.
                     */
                    //Una volta calcolato va dato in pasto a execute
                    QualityStats stats[] = qrun.execute(null/*qui va messo TrecJudge*/, null, logger);

                    //Qui possiamo calcolare recall, map e gmap, facendo la media tra i vari topic in stats.
                    double recall;
                    for (QualityStats stat : stats) {
                        //qui si calcolano le medie
                    }
                } catch (IOException ex) {
                    Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParseException ex) {
                    Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(LuceneSearcher.class.getName()).log(Level.SEVERE, null, ex);
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
