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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityQueryParser;
import org.apache.lucene.benchmark.quality.trec.*;
import org.apache.lucene.benchmark.quality.utils.SimpleQQParser;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
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

    public static void runQueries(String queryfile, Analyzer analyzer, String indexDir, String outputReport, String outputVSM) {
        //apro il queryfile
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(queryfile))))) {
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            Similarity similarity[] = {new LMJelinekMercerSimilarity((float) 0.2)};
            // Set the similarity metrics to searcher
            searcher.setSimilarity(new MultiSimilarity(similarity));
            TrecTopicsReader ttr = new TrecTopicsReader();
            QualityQuery[] qqs = ttr.readQueries(br);

            //aggiungo nomi query
            Set<String> fieldSet = new HashSet<>();
            fieldSet.add("title");
            fieldSet.add("description");
            fieldSet.add("narrative");
            String qqNames[] = fieldSet.toArray(new String[0]);

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

            try (BufferedWriter bufferedWriterVSM = new BufferedWriter(new FileWriter(new File(outputVSM)))) {
                SubmissionReport submitLog = new SubmissionReport(new PrintWriter(outputReport, IOUtils.UTF_8), "lucene");
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
                    if (submitLog != null) {
                        submitLog.report(qq, td, "doc_number", searcher);
                    }
                }
                /*QualityBenchmark qrun = new QualityBenchmark(qqs, qqParser, searcher, "text");
                qrun.setMaxResults(1000);
                qrun.setMaxQueries(50);
                PrintWriter logger = new PrintWriter(new OutputStreamWriter(System.out, Charset.defaultCharset()), true);
                QualityStats stats[] = qrun.execute(null, submitLog, logger);*/
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
