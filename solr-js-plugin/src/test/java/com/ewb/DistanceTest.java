package com.ewb;

import org.junit.Test;

public class DistanceTest {
    @Test
    public void testJensenShannonDivergence1() {
        System.out.println("Starting test...");
        double[] p = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        double[] q = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        double score = 0;
        Distance d = new Distance();
        score = d.JensenShannonDivergence(p, q);

        //assertTrue(MathEx.KullbackLeiblerDivergence(prob, p) < 0.05);
        System.out.println(score);

    }

    @Test
    public void testJensenShannonDivergence2() {
        System.out.println("Starting test...");
        double[] p = {1, 2, 3, 4, 5, 6, 7};
        double[] q = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        double score = 0;
        Distance d = new Distance();
        try {
            score = d.JensenShannonDivergence(p, q);
            System.out.println(score);
        } catch (Exception e) {
            System.out.println(e);
        }
    }


}
