/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ir13.glmgithub;

/**
 *
 * @author riya
 */
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Float.min;
import static java.lang.Integer.max;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Compute_Yass_Distance {

    int k;
    HashMap<Integer, String> Unique_word_list;
    HashMap<String, List<distance_storage>> nearestWordMap; // Store the pre-computed NNs after read from file

    public float yass_similarity(char[] X, char[] Y) {

        int m = X.length;
        int n = Y.length;
        int common = max(m, n);
        int minn = (int) min(m, n);
        int i, j, k = 0;
        float d1 = 0.0f, dd1 = 1.0f;

        for (i = 0; i < common; i++) {
            if (i < minn && X[i] != Y[i]) {
                k = i;
                for (j = i; j < common; j++) {
                    d1 = d1 + dd1;
                    dd1 = (float) (dd1 * 0.5);

                }
                break;
            }
            if (i == minn) {
                k = i;
                for (j = i; j < common; j++) {
                    d1 = d1 + dd1;
                    dd1 = (float) (dd1 * 0.5);

                }
                break;
            }
        }

        dd1 = (float) k / (common - k);
        d1 = d1 / dd1;

        float val = (1 - (float) (d1 / (m * n)));
        return val;
    }

    public float yass_similarity_mod(String x, String y) {
        char[] X = x.toCharArray();
        char[] Y = y.toCharArray();

        if (X[0] != Y[0]) {
            return 0;
        }

        int m = X.length;
        int n = Y.length;
        int common = max(m, n);
        int minn = (int) min(m, n);
        int i, j, k = 0;
        float d1 = 0.0f, dd1 = 1.0f;

        for (i = 0; i < common; i++) {
            if (i < minn && X[i] != Y[i]) {
                k = i;
                for (j = i; j < common; j++) {
                    d1 = d1 + dd1;
                    dd1 = (float) (dd1 * 0.5);

                }
                break;
            }
            if (i == minn) {
                k = i;
                for (j = i; j < common; j++) {
                    d1 = d1 + dd1;
                    dd1 = (float) (dd1 * 0.5);

                }
                break;
            }
        }

        dd1 = (float) k / (common - k);
        d1 = d1 / dd1;

        float val = (1 - (float) (d1 / (m * n)));
        return val;
    }

    /*public Compute_Yass_Distance() {

    }*/
    public Compute_Yass_Distance() throws FileNotFoundException, IOException {

        Unique_word_list = new HashMap<>();
        k = 30;
        String str = "src/main/resources/unique_list_small.txt";
        try {
            FileInputStream fstream = new FileInputStream(str);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                int count = 1;
                while ((strLine = br.readLine()) != null) {
                    String[] tokens = strLine.split(" ");
                    Unique_word_list.put(count, tokens[0]);
                    count = count + 1;
                }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }

    }

    public void PreComputeYassDistance() throws FileNotFoundException {
        int i, j;
        int len = Unique_word_list.size();
        //System.out.println(len);
        String NNDumpPath = "src/main/resources/NN_yass.txt";
        if (NNDumpPath != null) {
            System.out.println("File found...");
        } else {
            System.out.println("Null found");
            return;
        }

        System.out.println("Dumping the NN in: " + NNDumpPath);
        try (PrintWriter pout = new PrintWriter(NNDumpPath)) {
            System.out.println("Precomputing NNs for each word");
            for (i = 1; i <= len; i++) {
                String wd = Unique_word_list.get(i);
                System.out.println("Precomputing NNs for " + wd);
                List<distance_storage> nn = getNearestNeighbour(wd);
                if (nn != null) {
                    pout.print(wd + "\t");
                    for (j = 0; j < nn.size(); j++) {
                        distance_storage nns = nn.get(j);
                        pout.print(nns.word + ":" + nns.yass_similarity + "\t");
                    }
                    pout.print("\n");
                }
            }
        }

    }

    public List<distance_storage> getNearestNeighbour(String wd) {

        ArrayList<distance_storage> distList = new ArrayList<>();
        int i;
        for (i = 1; i <= Unique_word_list.size(); i++) {
            String neigh = Unique_word_list.get(i);
            if (neigh.equals(wd)) {
                continue;
            }

            char[] X = wd.toCharArray();
            char[] Y = neigh.toCharArray();

            if (X[0] != Y[0] && Y[0] < X[0]) {
                continue;
            }
            if (X[0] != Y[0] && Y[0] > X[0]) {
                break;
            }

            float sim = yass_similarity(X, Y);
            distance_storage nn = new distance_storage(neigh, sim);
            distList.add(nn);

        }
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));

    }

    public void loadPrecomputedNNs() throws FileNotFoundException, IOException {
        nearestWordMap = new HashMap<>();

        String NNDumpPath = "src/main/resources/NN_yass.txt";
        if (NNDumpPath == null) {
            System.out.println("NNDumpPath not specified in configuration...");
            return;
        }
        System.out.println("Reading from the NN dump at: " + NNDumpPath);
        File NNDumpFile = new File(NNDumpPath);

        try (FileReader fr = new FileReader(NNDumpFile);
                BufferedReader br = new BufferedReader(fr)) {
            String line;

            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, " \t:");
                List<String> tokens = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    tokens.add(token);
                }
                List<distance_storage> nns = new LinkedList();
                int len = tokens.size();
                for (int i = 1; i < len - 1; i += 2) {
                    nns.add(new distance_storage(tokens.get(i), Float.parseFloat(tokens.get(i + 1))));
                }
                nearestWordMap.put(tokens.get(0), nns);
            }
            System.out.println("NN dump has been reloaded");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Compute_Yass_Distance qe = new Compute_Yass_Distance();
            qe.PreComputeYassDistance();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Compute_Yass_Distance.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Compute_Yass_Distance.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
