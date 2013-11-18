package edu.cmu.lti.f13.hw4.hw4_soumyab.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_soumyab.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_soumyab.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_soumyab.utils.Utils;

// All tasks and BONUS tasks are completed
public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  // Queries for each query ID
  private HashMap<Integer, HashMap<String, Integer>> queries;

  // All answers for a query ID
  private ArrayList<HashMap<String, Integer>> answers;

  // All correct answer sentences for a query ID
  private HashMap<Integer, String> sentences;

  // Ranks for all query IDs
  private ArrayList<Integer> ranks;

  /** Initializes all data variables **/
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();
    relList = new ArrayList<Integer>();
    queries = new HashMap<Integer, HashMap<String, Integer>>();
    answers = new ArrayList<HashMap<String, Integer>>();
    sentences = new HashMap<Integer, String>();
    ranks = new ArrayList<Integer>();

  }

  // Private class containing similarity score and relevance ID for a query ID
  private class SimCoeff {

    double sim; // Similarity score

    int relID; // Relevance ID

    // Constructor
    SimCoeff(double s, int r) {
      sim = s;
      relID = r;
    }

    // Returns similarity score
    public double getSim() {
      return sim;
    }

    // Returns relevance ID
    public int getRelID() {
      return relID;
    }
  }

  // Comparator class to compare two Similarity Score Coefficient objects
  class CompareSim implements Comparator<SimCoeff> {
    public int compare(SimCoeff a1, SimCoeff a2) {
      // Returns -1 if score of first object is greater than that of second object
      if (a1.getSim() > a2.getSim())
        return -1;
      else
        return 1;
    }
  }

  // Comparator object
  Comparator<SimCoeff> simComparator = new CompareSim();

  /**
   * 1. constructs the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator<Annotation> it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {

      Document doc = (Document) it.next();

      // Get query ID and relevance ID
      int queryID = doc.getQueryID();
      int relID = doc.getRelevanceValue();

      // Pre-populated by previous annotator
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);

      qIdList.add(queryID);
      relList.add(relID);

      // Populate queries and correct sentences array list
      if (relID == 99)
        queries.put(queryID, fromTokenListToMap(tokenList));
      else if (relID == 1)
        sentences.put(queryID, doc.getText());

      // Add all answers to an arraylist
      answers.add(fromTokenListToMap(tokenList));

    }
  }

  /**
   * 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    // Current query
    HashMap<String, Integer> query = new HashMap<String, Integer>();
    ArrayList<SimCoeff> simAll = new ArrayList<SimCoeff>();

    int cur = 0, prev = 0, rank = 0;
    double sim = 0.0, score = 0.0;

    super.collectionProcessComplete(arg0);

    prev = qIdList.get(0);
    query = queries.get(prev);

    for (int i = 0; i < qIdList.size(); i++) {
      cur = qIdList.get(i);

      // Time to compare and print the result once similarity scores for all sentences of a single
      // query ID are calculated
      if (cur != prev) {
        Collections.sort(simAll, simComparator);

        for (int j = 0; j < simAll.size(); j++) {
          if (simAll.get(j).getRelID() == 1) {
            rank = j + 1;
            score = simAll.get(j).getSim();
            break;
          }
        }

        ranks.add(rank);
        System.out.println("Score: " + score + "\trank=" + rank + "\trel=1" + " qid=" + prev + " "
                + sentences.get(prev));
        query = queries.get(cur);
        simAll = new ArrayList<SimCoeff>();

      }

      prev = cur;

      // Cosine Similarity is computed by default
      if (relList.get(i) == 99)
        continue;
      else {
        sim = computeCosineSimilarity(query, answers.get(i));
        simAll.add(new SimCoeff(sim, relList.get(i)));
      }

    }

    // Do the above processing for the last query
    Collections.sort(simAll, simComparator);

    for (int j = 0; j < simAll.size(); j++) {
      if (simAll.get(j).getRelID() == 1) {
        rank = j + 1;
        score = simAll.get(j).getSim();
        break;
      }
    }

    ranks.add(rank);
    System.out.println("Score: " + score + "\trank=" + rank + "\trel=1" + " qid=" + prev + " "
            + sentences.get(prev));

    // computes the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr();
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
  }

  private HashMap<String, Integer> fromTokenListToMap(ArrayList<Token> TokenList) {
    // Converts token list to hash map

    HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
    for (Token t : TokenList) {
      hashMap.put(t.getText(), t.getFrequency());
    }
    return hashMap;
  }

  /**
   * Computes cosine similarity between two sentences
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;

    double normvq = 0.0;
    double normvd = 0.0;
    double sim = 0.0;

    for (String query : queryVector.keySet()) {
      int f = queryVector.get(query);
      normvq += (f * f);

      for (String dquery : docVector.keySet()) {
        int g = docVector.get(dquery);
        normvd += (g * g);

        if (query.equals(dquery))
          sim += (f * g);
      }
    }

    cosine_similarity = sim / (Math.sqrt(normvq) + Math.sqrt(normvd));

    return cosine_similarity;
  }

  /**
   * Computes Dice coefficient between two sentences
   * 
   * @return dice_coefficient
   */
  private double computeDiceCoefficient(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double dice_coefficient = 0.0;

    int sumvq = 0, sumvd = 0, sim = 0;

    for (String query : queryVector.keySet()) {
      sumvq++;

      for (String dquery : docVector.keySet()) {
        sumvd++;

        if (query.equals(dquery))
          sim++;
      }
    }
    dice_coefficient = (double) (2 * sim) / (sumvq + sumvd);

    return dice_coefficient;
  }

  /**
   * Computes Jaccard coefficient between two sentences
   * 
   * @return jaccard_coefficient
   */
  private double computeJaccardCoefficient(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double jaccard_coefficient = 0.0;

    ArrayList<String> q = new ArrayList<String>();
    ArrayList<String> d = new ArrayList<String>();

    int sumvq = 0, sumvd = 0, sim = 0;

    for (String query : queryVector.keySet()) {
      sumvq++;

      for (String dquery : docVector.keySet()) {
        sumvd++;

        if (query.equals(dquery))
          sim++;
      }
    }
    jaccard_coefficient = (double) sim / (sumvq + sumvd - sim);

    return jaccard_coefficient;
  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr() {
    double metric_mrr = 0.0;

    // computes Mean Reciprocal Rank (MRR) of the text collection
    for (double r : ranks)
      metric_mrr += (1 / r);

    metric_mrr /= ranks.size();

    return metric_mrr;
  }

}
