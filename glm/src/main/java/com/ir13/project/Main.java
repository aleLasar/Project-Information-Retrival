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
    public static final String QUERY_PATH = "src/main/resources/query/topics.";
    public static final String VSM_PATH = "src/main/resources/vsm/vsm.";
    public static final String QRELS_PATH = "src/main/resources/qrels/qrels.";
    public static final String[] TOPICS = {"trec6.txt", "trec7.txt", "trec8.txt", "robust.2004.txt"};

    public static void main(String[] args) {
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
        //LuceneIndexer.index(DOCS_PATH, INDEX_PATH, standardAnalyzer);
        LuceneSearcher.runQueries(QUERY_PATH, standardAnalyzer, INDEX_PATH, VSM_PATH, TOPICS, QRELS_PATH);

        System.out.println("Done..");
    }

}
