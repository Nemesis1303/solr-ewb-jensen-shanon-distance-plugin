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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

/*
 * This class receives the query vector and computes its distance to the document vector by reading the vector values directly from the Lucene index. As distance metric, the Jensen-Shannon divergence is used.
 */
public class VectorValuesSource extends DoubleValuesSource {
    private final String field;
    // private final String metric;

    private Terms terms; // Access to the terms in a specific field
    private TermsEnum te; // Iterator to step through terms to obtain frequency information
    private String[] query_comps;

    public VectorValuesSource(String field, String strVector) {
        /*
         * Document queries are assumed to be given as:
         * http://localhost:8983/solr/{your-corpus-collection-name}/query?fl=name,score,
         * vector&q={!vp f=doctpc_{your-model-name} vector="t0|43 t4|548 t5|6 t20|403"}
         * while topic queries as follows:
         * http://localhost:8983/solr/{your-model-collection-name}/query?fl=name,score,
         * vector&q={!vp f=betas
         * vector="high|43 research|548 development|6 neural_networks|403"}
         */
        this.field = field;
        this.query_comps = strVector.split(" ");
    }

    public DoubleValues getValues(LeafReaderContext leafReaderContext, DoubleValues doubleValues) throws IOException {

        final LeafReader reader = leafReaderContext.reader();

        return new DoubleValues() {

            // Retrieves the payload value for each term in the document and calculates the
            // core based on vector lookup
            public double doubleValue() throws IOException {
                double score = 0;
                BytesRef text;
                String term = "";
                List<Integer> doc_topics = new ArrayList<Integer>();
                List<Integer> doc_probs = new ArrayList<Integer>();
                while ((text = te.next()) != null) {
                    term = text.utf8ToString();
                    if (term.isEmpty()) {
                        continue;
                    }

                    // Get the document probability distribution
                    float payloadValue = 0f;
                    PostingsEnum postings = te.postings(null, PostingsEnum.ALL);
                    // And after we get TermsEnum instance te, we can compute the document vector by
                    // iterating all payload components (we will have as many components as topics
                    // the model has)
                    while (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        int freq = postings.freq();
                        while (freq-- > 0)
                            postings.nextPosition();

                        BytesRef payload = postings.getPayload();
                        payloadValue = PayloadHelper.decodeInt(payload.bytes, payload.offset);
                        doc_topics.add(Integer.parseInt(term.substring(1)));
                        doc_probs.add((int) payloadValue);
                    }
                }

                // Create maps containing the value after '|' for each t that is present in both
                // strings for the case of document queries, and for each word that is present
                // in both strings for the case of topic queries
                Map<Integer, Integer> doc_values = new HashMap<>();
                Map<Integer, Integer> query_values = new HashMap<>();

                // Create pattern to match the topic queries
                Pattern pattern = Pattern.compile("t(\\d+)\\|");

                for (String comp : query_comps) {
                    Matcher matcher = pattern.matcher(comp);
                    if (matcher.find()) {
                        int tpc_id = Integer.parseInt(matcher.group(1));
                        System.out.println(tpc_id);
                    }
                }

                for (String comp : query_comps) {
                    int tpc_id = -1;
                    // It is a document-topic distribution
                    if (comp.contains("t")) {
                        tpc_id = Integer.parseInt(comp.split("\\|")[0].split("t")[1]);
                        // It is a word-topic distribution
                    } else {
                        Matcher matcher = pattern.matcher(comp);
                        if (matcher.find()) {
                            tpc_id = Integer.parseInt(matcher.group(1));
                        }

                    }
                    if (doc_topics.contains(tpc_id)) {
                        query_values.put(tpc_id, Integer.parseInt(comp.split("\\|")[1]));
                        doc_values.put(tpc_id, doc_probs.get(doc_topics.indexOf(tpc_id)));
                    }
                }

                // Convert the maps into arrays
                List<Integer> sortedKeys = new ArrayList<>(doc_values.keySet());
                Collections.sort(sortedKeys);

                double[] docProbabilities = new double[sortedKeys.size()];
                double[] queryProbabilities = new double[sortedKeys.size()];

                for (int i = 0; i < sortedKeys.size(); i++) {
                    Integer t = sortedKeys.get(i);
                    docProbabilities[i] = doc_values.get(t);
                    queryProbabilities[i] = query_values.get(t);
                }

                System.out.println(Arrays.toString(docProbabilities));
                System.out.println(Arrays.toString(queryProbabilities));

                Distance d = new Distance();

                // if (metric == "jensen-shannon") {
                // score = d.JensenShannonDivergence(docProbabilities, queryProbabilities);
                // }
                // else if (metric == "bhattacharyya") {
                // score = d.bhattacharyyaDistance(docProbabilities, queryProbabilities);
                // }

                score = d.bhattacharyyaDistance(docProbabilities, queryProbabilities);

                return score;

                // return score;
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
