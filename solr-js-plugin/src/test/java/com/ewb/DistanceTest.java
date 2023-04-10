package com.ewb;

import org.junit.Test;

public class DistanceTest {
    @Test
    public void testJensenShannonDivergence1() {
        System.out.println("Starting test 1...");
        double[] p = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        double[] q = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        double score = 0;
        Distance d = new Distance();
        score = d.JensenShannonDivergence(p, q);

        // assertTrue(MathEx.KullbackLeiblerDivergence(prob, p) < 0.05);
        System.out.println(score);

    }

    @Test
    public void testJensenShannonDivergence2() {
        System.out.println("Starting test 2...");
        double[] p = { 1, 2, 3, 4, 5, 6, 7 };
        double[] q = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        double score = 0;
        Distance d = new Distance();
        try {
            score = d.JensenShannonDivergence(p, q);
            System.out.println(score);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Test
    public void testJensenShannonDivergence3() {
        System.out.println("Starting test 3...");
        double[] p = { 0, 105, 0, 0, 0, 0, 471, 0, 15, 0, 0, 120, 0, 0, 71, 0, 0, 0, 0, 0, 218, 0, 0, 0, 0 };
        double[] q = { 0, 4, 0, 1, 0, 4, 0, 4, 0, 1, 0, 4, 0, 4, 0, 1, 0, 4, 5, 3, 4, 3, 2, 0, 0};

        double score = 0;
        Distance d = new Distance();
        score = d.JensenShannonDivergence(p, q);

        // assertTrue(MathEx.KullbackLeiblerDivergence(prob, p) < 0.05);
        System.out.println(score);

    }

}
