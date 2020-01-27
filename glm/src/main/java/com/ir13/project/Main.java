/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ir13.project;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 *
 * @author matteo
 */
public class Main {

    public static final String DOCS_PATH = "src/main/resources/docs";
    public static final String INDEX_PATH = "src/main/resources/index";
    public static final String REPORT_PATH = "src/main/resources/report/topics.301-350.trec6.txt";
    public static final String QUERY_PATH = "src/main/resources/query/topics.301-350.trec6.txt";
    public static final String SVM_PATH = "src/main/resources/svm/topics.301-350.trec6.txt";

    public static void main(String[] args) {
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
        //LuceneIndexer.index(DOCS_PATH, INDEX_PATH, standardAnalyzer);
        LuceneSearcher.runQueries(QUERY_PATH, standardAnalyzer, INDEX_PATH, REPORT_PATH, SVM_PATH);
        /*NgramAnalyzer ngramAnalyzer = new NgramAnalyzer();
        EnglishSynonymCustomAnalyzer synAnalyzer1 = new EnglishSynonymCustomAnalyzer();
        EnglishSynonymWNAnalyzer wn = new EnglishSynonymWNAnalyzer();
        EnglishStopAnalyzer ea = new EnglishStopAnalyzer();
        CustomTestAnalyzer cust = new CustomTestAnalyzer();
        LuceneIndexer.index(systemPath + INPUT_DOC_PATH, systemPath + INDEX_PATH_OLD, ea);*/
        System.out.println("Done..");
        /*LuceneIndexer.index(systemPath + INPUT_DOC_PATH, systemPath + INDEX_PATH, ea);
        System.out.println("Done..");
        LuceneIndexer.index(systemPath + INPUT_DOC_PATH, systemPath + INDEX_PATH_WN, wn);
        System.out.println("Done..");
        LuceneIndexer.index(systemPath + INPUT_DOC_PATH, systemPath + INDEX_PATH_Cust, cust);
        System.out.println("Done..");
        LuceneSearcher.runQueries(systemPath + QUERY_PATH, wn, systemPath + INDEX_PATH_WN, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_WN);
        LuceneSearcher.runQueries(systemPath + QUERY_PATH, cust, systemPath + INDEX_PATH_Cust, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_Cust);
        LuceneSearcher.runQueries(systemPath + QUERY_PATH, ea, systemPath + INDEX_PATH, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_EA);
        //LuceneSearcher.runQueries(systemPath + QUERY_PATH, ea, systemPath + INDEX_PATH, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_EA);
        //LuceneSearcher.runQueries(systemPath + QUERY_PATH, synAnalyzer2, systemPath + INDEX_PATH, systemPath + OUTPUT_PATH_NGRAM2, systemPath + OUTPUT_PATH_NGRAM3);
        LuceneSearcher.runQueries(systemPath + QUERY_PATH, cust, systemPath + INDEX_PATH, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_EA_Cust);
        LuceneSearcher.runQueries(systemPath + QUERY_PATH, wn, systemPath + INDEX_PATH, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_EA_WN);
        LuceneSearcher.runQueries(systemPath + QUERY_PATH, wn, systemPath + INDEX_PATH_Cust, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_WN_Cust);
        LuceneSearcher.runQueries(systemPath + QUERY_PATH, cust, systemPath + INDEX_PATH_WN, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_CUST_WN);
        LuceneSearcher.runQueries(systemPath + QUERY_PATH, cust, systemPath + INDEX_PATH_OLD, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_OLD);
        LuceneSearcher.runQueries(systemPath + QUERY_PATH, cust, systemPath + INDEX_PATH, systemPath + OUTPUT_PATH_NGRAM, systemPath + OUTPUT_PATH_TEST);
        System.out.println("DONE!!");*/
    }

}
