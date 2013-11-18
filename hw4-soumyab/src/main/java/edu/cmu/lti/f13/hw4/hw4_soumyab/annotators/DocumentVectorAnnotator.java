package edu.cmu.lti.f13.hw4.hw4_soumyab.annotators;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f13.hw4.hw4_soumyab.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_soumyab.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_soumyab.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      try {
        createTermFreqVector(jcas, doc);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

  }

  /**
   * Constructs a vector of tokens and update the tokenList in CAS
   * 
   * @param jcas
   *          - JCAS object
   * @param doc
   *          - Document object
   * @throws IOException
   */

  private void createTermFreqVector(JCas jcas, Document doc) throws IOException {

    // Get text of the document
    String docText = doc.getText();

    // Get stop words
    ArrayList<String> stopWords = new ArrayList<String>();
    stopWords = getStopWords();

    // Create temporary structures to store tokens
    ArrayList<Token> toklist = new ArrayList<Token>();
    HashMap<String, Integer> tlist = new HashMap<String, Integer>();

    // Tokenizing based on possible word separators
    StringTokenizer st = new StringTokenizer(docText, " .,!?");

    // Putting token strings corresponding to their frequencies in the Hashmap
    Integer i = 0;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();

      if (!(stopWords.contains(token))) {
        if ((i = tlist.get(token)) != null)
          tlist.put(token.toLowerCase(), i + 1);
        else
          tlist.put(token.toLowerCase(), 1);
     }
    }

    // Converting from Hashmap to an ArrayList of tokens
    for (String t : tlist.keySet()) {
      Token tok = new Token(jcas);
      tok.setText(t);
      tok.setFrequency(tlist.get(t));
      toklist.add(tok);
    }

    // Finally, setting the token list of a document after converting array list to FS List
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, toklist));

  }

  private ArrayList<String> getStopWords() throws IOException {
    ArrayList<String> list = new ArrayList<String>();
    BufferedReader br = new BufferedReader(new FileReader(
            "C:/eclipse/Soumya/workspace/hw4-soumyab/src/main/resources/stopwords.txt"));

    String line = br.readLine();
    try {
      for (int i = 0; i < 2; i++)
        line = br.readLine();

      while (line != null) {
        list.add(line);
        line = br.readLine();
      }

    } finally {
      if (br != null)
        br.close();
    }

    return list;
  }

}
