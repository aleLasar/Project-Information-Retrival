/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ir13.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityQueryParser;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.benchmark.quality.utils.SimpleQQParser;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author matteo
 */
public class GLM {

    private final String queryfile;
    private final Analyzer analyzer;
    private final String indexDir;
    private final String[] topicFiles;

    public GLM(String queryfile, Analyzer analyzer, String indexDir, String[] topicFiles) {
        this.queryfile = queryfile;
        this.analyzer = analyzer;
        this.indexDir = indexDir;
        this.topicFiles = topicFiles;
    }

    public double score(double lambda, double alpha, double beta) {
        try (
                Directory dir = FSDirectory.open(Paths.get(indexDir)); IndexReader reader = DirectoryReader.open(dir)) {
            String topicTRECNames[] = {"title", "description", "narrative"};
            QualityQueryParser topicTRECParser = new SimpleQQParser(topicTRECNames, "text");
            for (String topicFile : topicFiles) {
                try (
                        BufferedReader topicFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(queryfile + topicFile))));) {

                    //leggo le query
                    QualityQuery[] topicsTREC = new TrecTopicsReader().readQueries(topicFileReader);
                    for (QualityQuery topicTREC : topicsTREC) {
                        Query query = topicTRECParser.parse(topicTREC);
                        HashSet<Term> terms = new HashSet<>();
                        query.extractTerms(terms);
                        Term t;
                        HashMap<Integer, Integer> termsFreq = new HashMap<>();
                        HashMap<Integer, Integer> docsLength = new HashMap<>();
                        for (Iterator<Term> it = terms.iterator(); it.hasNext();) {
                            //qui hai i termini del topic
                            t = it.next();
                            PostingsEnum posting = MultiFields.getTermDocsEnum(reader, MultiFields.getLiveDocs(reader), "text", t.bytes());
                            if (posting != null) {
                                while (posting.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                                    //qui ho la frequenza nel documento
                                    posting.freq();
                                    termsFreq.put(posting.docID(), posting.freq());
                                }
                            }
                        }
                        for (int docID : termsFreq.keySet()) {
                            System.out.println(docID);
                            TermsEnum termsEnum = reader.getTermVector(docID, "text").iterator(TermsEnum.EMPTY);
                            int doclen = 0;
                            while (termsEnum.next() != null) {
                                doclen += termsEnum.totalTermFreq();
                            }
                            docsLength.put(docID, doclen);
                        }
                        termsFreq.keySet().forEach((docID) -> {
                            System.out.println(termsFreq.get(docID) / docsLength.get(docID));
                        });
                        System.exit(0);
                    }
                } catch (ParseException ex) {
                    Logger.getLogger(GLM.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(GLM.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }
}
