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
public class distance_storage implements Comparable<distance_storage> {

    String word;
    float yass_similarity;

    @Override
    public int compareTo(distance_storage that) {
        return this.yass_similarity > that.yass_similarity ? -1 : this.yass_similarity == that.yass_similarity ? 0 : 1;
    }

    public distance_storage(String str, float val) {
        word = str;
        yass_similarity = val;
    }

}
