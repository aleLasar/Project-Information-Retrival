/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ir13.project;

import java.util.Locale;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMSimilarity;

/**
 *
 * @author matteo
 */
public class GLMSimilarity extends LMSimilarity {

    private final double lambda, alpha, beta;

    public GLMSimilarity(double lambda, double alpha, double beta, CollectionModel collectionModel) {
        super(collectionModel);
        this.lambda = lambda;
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public String getName() {
        return String.format(Locale.ROOT, "GLM");
    }

    @Override
    protected float score(BasicStats stats, float freq, float docLen) {
        return 0;
    }

}
