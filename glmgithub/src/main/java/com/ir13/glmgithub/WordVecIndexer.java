/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ir13.glmgithub;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilter;
import org.apache.lucene.analysis.payloads.FloatEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

/**
 *
 * @author Debasis
 */
class TermFreq {

    String term;
    float ntf_d; // document component
    float nts_d;

    public TermFreq(String term1) {
        this.term = term1;
    }
}

class PayloadAnalyzer extends Analyzer {

    private final PayloadEncoder encoder;

    public PayloadAnalyzer() {
        this.encoder = new FloatEncoder();
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer source = new WhitespaceTokenizer(Version.LUCENE_4_9, reader);
        TokenStream filter = new DelimitedPayloadTokenFilter(source, WordVecIndexer.PAYLOAD_DELIM, encoder);
        return new Analyzer.TokenStreamComponents(source, filter);
    }
}

public class WordVecIndexer {

    File indexDir;
    File wvIndexDir;
    WordVecs wordvecs;
    IndexWriter writer;
    PerFieldAnalyzerWrapper wrapper;
    int indexingPass;
    Compute_Yass_Distance yass_obj;
    float lambda;

    static final public String FIELD_ID = "id";
    static final public String FIELD_TIME = "time";
    static final public String FIELD_BAG_OF_WORDS = "words";  // Baseline
    static final public String FIELD_P_WVEC_D = "wv_d";
    static final public String FIELD_P_WVEC_C = "wv_c";
    static final char PAYLOAD_DELIM = '|';

    HashMap<String, Integer> doc_id = new HashMap<>();

    public WordVecIndexer() throws Exception {

        String indexPath = "src/main/resources/index";

        // Load the word2vecs so as to prepare the analyzer
        Analyzer analyzer = new PayloadAnalyzer();

        // WhiteSpace analyzer (standard stopword)...
        // payload analyzers for the payloads
        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put(FIELD_BAG_OF_WORDS, new WhitespaceAnalyzer(Version.LUCENE_4_9));
        wrapper = new PerFieldAnalyzerWrapper(analyzer, analyzerPerField);

        indexDir = new File(indexPath);

        // This is for pass 1 indexing...
        /*IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LUCENE_4_9, wrapper);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(FSDirectory.open(indexDir), iwcfg);*/
        indexingPass = 2;
    }

    public Analyzer getAnalyzer() {
        return wrapper;
    }

    void indexAll() throws Exception {
        if (writer == null) {
            System.err.println("Skipping indexing... Index already exists at " + indexDir.getName() + "!!");
            return;
        }

        File topDir = new File("src/main/resources/docs");
        indexDirectory(topDir);
        writer.close();
    }

    private void indexDirectory(File dir) throws Exception {
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                System.out.println("Indexing directory " + f.getName());
                indexDirectory(f);  // recurse
            } else {
                indexFile(f);
            }
        }
    }

    Document constructDoc(String id, String content) {

        Document doc = new Document();
        doc.add(new Field(FIELD_ID, id, Field.Store.YES, Field.Index.NOT_ANALYZED));
        StringBuilder tokenizedContentBuff = new StringBuilder();

        try (TokenStream stream = wrapper.tokenStream(FIELD_BAG_OF_WORDS, new StringReader(content))) {
            CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            while (stream.incrementToken()) {
                String term = termAtt.toString();
                term = term.toLowerCase();
                tokenizedContentBuff.append(term).append(" ");
            }

            stream.end();
        } catch (IOException ex) {
            Logger.getLogger(WordVecIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }

        // For the 1st pass, use a standard analyzer to write out
        // the words (also store the term vector)
        doc.add(new Field(FIELD_BAG_OF_WORDS, content,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        return doc;
    }

    void indexFile(File file) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        Document doc;

        String docType = "trec";
        if (docType.equalsIgnoreCase("trec")) {
            StringBuilder txtbuff = new StringBuilder();
            while ((line = br.readLine()) != null) {
                txtbuff.append(line).append("\n");
            }
            String content = txtbuff.toString();

            org.jsoup.nodes.Document jdoc = Jsoup.parse(content);
            Elements docElts = jdoc.select("DOC");

            for (Element docElt : docElts) {

                Element docIdElt = docElt.select("DOCNO").first();
                String txt = "";
                if (docIdElt != null) {
                    txt = docIdElt.text();
                }
                Element docTextElt = docElt.select("TEXT").first();
                String txt1 = "";
                if (docTextElt != null) {
                    txt1 = docTextElt.text();
                }

                doc = constructDoc(txt, txt1);
                if (doc_id.get(txt) == null) {
                    writer.addDocument(doc);
                    doc_id.put(txt, 1);
                }
            }
        }
    }

    /*2nd pass: Read each document in the index
     and smooth each term generation probability
     by its neighbouring terms (as obtained from
     the abstract vector space of word2vec).*/
    public void expandIndex() throws Exception {
        String wvIndexPath = "src/main/resources/expand1";
        lambda = 0.5f;

        if (wvIndexPath == null) {
            System.out.println("Skipping expansion");
            return;
        } else {
            System.out.println("Saving the word-vector in: " + wvIndexPath);
        }

        wvIndexDir = new File(wvIndexPath);

        wordvecs = new WordVecs();
        yass_obj = new Compute_Yass_Distance();

        // Very Very Important For the second phase indexing change the path of NNDumpath to that of combined neighbour obtained from word-vec and yash similarity
        if (wordvecs.wordvecmap != null) {
            wordvecs.loadPrecomputedNNs();
        }

        // Open the new wv index for writing
        IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LUCENE_4_9, wrapper);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(FSDirectory.open(wvIndexDir), iwcfg);

        int start = 0;
        int end = -1;

        Document expDoc;

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir))) {
            int maxDoc = reader.maxDoc();
            end = Math.min(end, maxDoc);
            if (end == -1) {
                end = maxDoc;
            }

            for (int i = start; i < end; i++) {
                System.out.println("DocId: " + i);
                expDoc = expandDoc(reader, i);
                writer.addDocument(expDoc);
            }
        }

        writer.close();
    }

    boolean isNumber(String term) {
        int len = term.length();
        for (int i = 0; i < len; i++) {
            char ch = term.charAt(i);
            if (Character.isDigit(ch)) {
                return true;
            }
        }
        return false;
    }

    Document expandDoc(IndexReader reader, int docId) throws IOException {

        int N = reader.numDocs();
        ArrayList<TermFreq> tfvec = new ArrayList<>();

        Document newdoc = new Document();
        Document doc = reader.document(docId);

        StringBuilder buff = new StringBuilder();
        StringBuilder cbuff = new StringBuilder();

        //get terms vectors stored in 1st pass
        Terms terms = reader.getTermVector(docId, FIELD_BAG_OF_WORDS);
        if (terms == null || terms.size() == 0) {
            return doc;
        }

        TermsEnum termsEnum = terms.iterator(null); // access the terms for this field
        BytesRef term;
        int docLen = 0;

        // Calculate doc len
        while (termsEnum.next() != null) {// explore the terms for this field
            DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one

            while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                //get the term frequency in the document
                docLen += docsEnum.freq();
            }
        }

        // Construct the normalized tf vector
        termsEnum = terms.iterator(null); // access the terms for this field
        while ((term = termsEnum.next()) != null) { // explore the terms for this field
            String termStr = term.utf8ToString();
            if (isNumber(termStr)) {
                continue;
            }
            DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one
            while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                //get the term frequency in the document
                int tf = docsEnum.freq();
                float ntf = tf / (float) docLen;
                TermFreq tfObj = new TermFreq(termStr);
                tfObj.ntf_d = ntf;
                tfvec.add(tfObj);
            }
        }

        // P(t|t',d):
        // Iterate over the normalized tf vector
        int i, j, len = tfvec.size();
        float prob, sim, temp_sim, totalSim;

        //Let the change be checked by Dwaipayan da (we will be sending the sum factor only ? )
        for (i = 0; i < len; i++) {
            TermFreq tf_i = tfvec.get(i);
            totalSim = 0.0f;
            for (j = 0; j < len; j++) {
                if (i == j) {
                    continue;
                }
                TermFreq tf_j = tfvec.get(j);
                temp_sim = lambda * (this.wordvecs.getSim(tf_i.term, tf_j.term));
                char[] X = tf_i.term.toCharArray();
                char[] Y = tf_j.term.toCharArray();
                if (X[0] == Y[0]) {
                    temp_sim += (1 - lambda) * (this.yass_obj.yass_similarity(X, Y));
                }
                totalSim += temp_sim;
            }
            tf_i.nts_d = totalSim;
        }

        // Let the change be checked by Dwaipayan da (should the value be saved as sum ? )
        for (i = 0; i < len; i++) {
            TermFreq tf_i = tfvec.get(i);
            prob = 0.0f;
            for (j = 0; j < len; j++) {
                if (i == j) {
                    continue;
                }

                TermFreq tf_j = tfvec.get(j);
                // CHECK: Currently not checking if t'
                // is a near neighbour of t
                sim = lambda * (this.wordvecs.getSim(tf_i.term, tf_j.term));
                char[] X = tf_i.term.toCharArray();
                char[] Y = tf_j.term.toCharArray();
                if (X[0] == Y[0]) {
                    sim += (1 - lambda) * (this.yass_obj.yass_similarity(X, Y));
                }

                prob += tf_j.ntf_d * sim / tf_i.nts_d;
                //tf_i.ntf_d += prob;
            }
            if (!Float.isFinite(prob)) {
                prob = 0;
            }

            buff.append(tf_i.term.replace(String.valueOf(PAYLOAD_DELIM), "-")).append(PAYLOAD_DELIM).append(prob).append(" ");

        }

        // number of neighbors wanted
        final int K = 3;
        final float thresh = 0.6f;

        // P(t|t',C)
        for (i = 0;
                i < len;
                i++) {

            TermFreq tf_i = tfvec.get(i);

            // Get the nearest neighbours of tf_i
            // Discuss with dwaipayan da  : I am not doing any thresholding because its not done in D
            List<WordVec> nn_tf_i = wordvecs.getPrecomputedNNs(tf_i.term, K, thresh);
            if (nn_tf_i == null || nn_tf_i.isEmpty()) {
                continue;
            }

            // Add the term itself in the list (A word is also a neighbor
            // of itself). No need to maintain the sorted order here...
            // Why is a word added as its neighbour ?
            nn_tf_i.add(new WordVec(tf_i.term, 1.0f));

            float normalizer = 0.0f;
            normalizer = nn_tf_i.stream().map((nn) -> nn.querySim).reduce(normalizer, (accumulator, _item) -> accumulator + _item);

            // Again changes made in the code , cbuff moved out of the loop istn't it correct ?
            // Expand the current document by NN words (including itself)
            //float probNN=0.0f;
            for (WordVec nn : nn_tf_i) {
                // We can do this since it's postional indexing... no need
                // to add only one occurrence of term with its frequency
                // No need to incorporate the collection freq here because
                // it will any way be taken care of during retrieval.
                //probNN+= (float)(nn.querySim/normalizer);

                float probNN = (float) (nn.querySim / normalizer);
                if (Float.isNaN(probNN)) {
                    probNN = 0;
                }
                cbuff.append(nn.word).append(PAYLOAD_DELIM).append(probNN).append(" ");
            }
        }

        // Shouldnot the fields be all Unanalysed ??
        newdoc.add(
                new Field(FIELD_ID, doc.get(FIELD_ID), Field.Store.YES, Field.Index.NOT_ANALYZED));
        newdoc.add(
                new Field(FIELD_BAG_OF_WORDS, doc.get(FIELD_BAG_OF_WORDS),
                        Field.Store.YES, Field.Index.ANALYZED));

        // Two new additional fields
        // P(t|t';d)
        newdoc.add(
                new Field(FIELD_P_WVEC_D, buff.toString(),
                        Field.Store.YES, Field.Index.ANALYZED));
        // P(t|t';C)
        newdoc.add(
                new Field(FIELD_P_WVEC_C, cbuff.toString(),
                        Field.Store.YES, Field.Index.ANALYZED));
        /*System.out.println("FIELD_ID: " + newdoc.get(FIELD_ID));
        System.out.println("FIELD_BAG_OF_WORDS: " + newdoc.get(FIELD_BAG_OF_WORDS));
        System.out.println(
                "FIELD_P_WVEC_D: " + newdoc.get(FIELD_P_WVEC_D));
        System.out.println("FIELD_P_WVEC_C: " + newdoc.get(FIELD_P_WVEC_C));*/

        return newdoc;
    }

    void dumpIndex() {
        String dumpPath = "src/main/resources/trec.dump";
        if (dumpPath == null) {
            return;
        }

        System.out.println("Dumping the index in: " + dumpPath);
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir)); PrintWriter pout = new PrintWriter(dumpPath)) {
            int maxDoc = reader.maxDoc();
            for (int i = 0; i < maxDoc; i++) {
                Document d = reader.document(i);
                pout.print(d.get(FIELD_BAG_OF_WORDS) + " ");
            }
            System.out.println("Index dumped in: " + dumpPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        try {
            WordVecIndexer indexer = new WordVecIndexer();
            if (indexer.indexingPass == 1) {
                indexer.indexAll();
                indexer.dumpIndex();
            } else {
                indexer.expandIndex();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

