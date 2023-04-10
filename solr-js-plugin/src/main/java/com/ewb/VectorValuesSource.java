package com.ewb;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
//import smile.math.distance.JensenShannonDistance;

/*
 * This class receives the query vector and computes its distance to the document vector by reading the vector values directly from the Lucene index. As distance metric, the Jensen-Shannon divergence is used.
 */
public class VectorValuesSource extends DoubleValuesSource {
    private final String field;
    private List<Double> vector;

    private Terms terms; // Access to the terms in a specific field
    private TermsEnum te; // Iterator to step through terms to obtain frequency information

    public VectorValuesSource(String field, String strVector) {
        this.field = field;
        this.vector = new ArrayList<>();

        // query is assumed to be given as:
        // http://localhost:8983/solr/{your-collection-name}/query?fl=name,score,vector&q={!vp
        // f=vector vector="0.1,4.75,0.3,1.2,0.7,4.0"}

        String[] vectorArray = strVector.split(",");
        for (String s : vectorArray) {
            double v = Double.parseDouble(s);
            vector.add(v);
        }
    }

    public DoubleValues getValues(LeafReaderContext leafReaderContext, DoubleValues doubleValues) throws IOException {

        final LeafReader reader = leafReaderContext.reader();

        return new DoubleValues() {

            // Retrieves the payload value for each term in the document and calculates the
            // core based on vector lookup
            public double doubleValue() throws IOException {
                double score = 0;
                double[] docProbabilities = new double[vector.size()];
                double[] queryProbabilities = new double[vector.size()];
                BytesRef text;
                String term = "";

                while ((text = te.next()) != null) {
                    term = text.utf8ToString();
                    if (term.isEmpty()) {
                        continue;
                    }
                }

                // Calculate the document probability distribution
                float payloadValue = 0f;
                PostingsEnum postings = te.postings(null, PostingsEnum.ALL);
                while (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    int freq = postings.freq();
                    while (freq-- > 0)
                        postings.nextPosition();

                    BytesRef payload = postings.getPayload();
                    payloadValue = PayloadHelper.decodeInt(payload.bytes, payload.offset);
                    docProbabilities[Integer.parseInt(term.substring(1))] += payloadValue;
                }

                // Calculate the query probability distribution
                for (int i = 0; i < vector.size(); i++) {
                    queryProbabilities[i] = vector.get(i);
                }

                //JensenShannonDistance jsd = new JensenShannonDistance();
                //score = jsd.d(docProbabilities, queryProbabilities);

                Distance d = new Distance();
                score = d.JensenShannonDivergence(docProbabilities, queryProbabilities);

                return score;
            }

            // Advance to next document (for each document in the LeafReaderContext)
            public boolean advanceExact(int doc) throws IOException {
                terms = reader.getTermVector(doc, field);
                if (terms == null) {
                    return false;
                }
                te = terms.iterator();
                return true;
            }
        };
    }

    public boolean needsScores() {
        return true;
    }

    public DoubleValuesSource rewrite(IndexSearcher indexSearcher) throws IOException {
        return this;
    }

    public int hashCode() {
        return 0;
    }

    public boolean equals(Object o) {
        return false;
    }

    public String toString() {
        return "JS(" + field + ",doc)";
    }

    public boolean isCacheable(LeafReaderContext leafReaderContext) {
        return false;
    }
}
