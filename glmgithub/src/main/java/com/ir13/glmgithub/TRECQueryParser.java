/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ir13.glmgithub;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;

/**
 *
 * @author Debasis
 */
public class TRECQueryParser {

    String fileName;
    TRECQuery query;
    WordVecSearcher parent;

    public List<TRECQuery> queries;
    final static String[] TAGS = {"num", "title", "desc", "narr"};

    public TRECQueryParser(String fileName, WordVecSearcher parent) {
        this.fileName = fileName;
        queries = new LinkedList<>();
        this.parent = parent;
    }

    public TRECQueryParser(String fileName) {
        this.fileName = fileName;
        queries = new LinkedList<>();
    }

    public void parse() {
        try (
                BufferedReader topicFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileName))))) {
            QualityQuery[] topicsTREC = new TrecTopicsReader().readQueries(topicFileReader);
            for (QualityQuery topicTREC : topicsTREC) {
                query = new TRECQuery(parent);
                query.title = topicTREC.getValue("title");
                query.desc = topicTREC.getValue("description");
                query.id = topicTREC.getQueryID();
                query.narr = topicTREC.getValue("narrative");
                queries.add(query);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TRECQueryParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TRECQueryParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*@Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("top")) {
            query = new TRECQuery(this.parent);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("title")) {
            query.title = buff.toString().trim();
            buff.setLength(0);
        } else if (qName.equalsIgnoreCase("desc")) {
            query.desc = buff.toString().trim();
            buff.setLength(0);
        } else if (qName.equalsIgnoreCase("num")) {
            query.id = buff.toString().trim();
            buff.setLength(0);
        } else if (qName.equalsIgnoreCase("narr")) {
            query.narr = buff.toString().trim();
            buff.setLength(0);
        } else if (qName.equalsIgnoreCase("top")) {
            queries.add(query);
        }
    }
    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        buff.append(new String(ch, start, length));
    }*/
    public static void main(String[] args) {

        try {
            TRECQueryParser parser = new TRECQueryParser("src/main/resources/topics/topics.trec6.txt", null);
            parser.parse();

            parser.queries.stream().map((query) -> {
                System.out.println("ID: " + query.id);
                return query;
            }).forEachOrdered((query) -> {
                System.out.println("Desc: " + query.desc);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

