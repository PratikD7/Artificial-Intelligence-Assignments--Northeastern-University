import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.IntStream;

// Bayesian Tomatoes:
// Doing some Naive Bayes and Markov Models to do basic sentiment analysis.
//
// Input from train.tsv.zip at 
// https://www.kaggle.com/c/sentiment-analysis-on-movie-reviews
//
// itself gathered from Rotten Tomatoes.
//
// Format is PhraseID[unused]   SentenceID  Sentence[tokenized] Sentiment
//
// We'll only use the first line for each SentenceID, since the others are
// micro-analyzed phrases that would just mess up our counts.
//
// Sentiment is on a 5-point scale:
// 0 - negative
// 1 - somewhat negative
// 2 - neutral
// 3 - somewhat positive
// 4 - positive
//
// For each kind of model, we'll build one model per sentiment category.
// Following Bayesian logic, base rates matter for each category; if critics
// are often negative, that should be a good guess in the absence of other
// information.
//
// To play well with HackerRank, input is assumed to be the train.tsv
// format of training data until we encounter a line that starts with "---".
// All remaining lines, which should be just space-delimited words/tokens
// in a sentence, are assumed to be test data.
// Output is the following on four lines for each line of test data:
//
// Naive Bayes classification (0-4)
// Naive Bayes most likely class's log probability (with default double digits/precision)
// Markov Model classification (0-4)
// Markov Model most likely class's log probability

public class BayesianTomatoes {

    public static final int CLASSES = 5;
    // Assume sentence numbering starts with this number in the file
    public static final int FIRST_SENTENCE_NUM = 1;
    // Probability of either a unigram or bigram that hasn't been seen
    public static final double OUT_OF_VOCAB_PROB = 0.0000000001;

    // Sorry about the "global"ish variables here, but it's going to
    // make all the other signatures rather cleaner

    // Word counts for each sentiment label
    public static ArrayList<HashMap<String, Integer>> wordCounts;
    // Bigram counts for each sentiment label, with key a single string
    // separating the words with a space
    public static ArrayList<HashMap<String, Integer>> bigramCounts;
    // Overall sentence sentiment counts for taking the prior into account
    // (one is incremented once per sentence)

    // A subtle point:  if a word is at the end of the sentence, it's not
    // the beginning of any bigram.  So we need to keep separate track of
    // the number of times a word starts any bigram (ie is not the last word)
    public static ArrayList<HashMap<String, Integer>> bigramDenoms;

    public static int[] sentimentCounts;
    public static int[] totalWords;
    public static int[] totalBigrams;

    public static class Classification {
        public int rating;       // the maximum likelihood classification
        public double logProb;   // the log likelihood of that classification

        public Classification(int r, double lp) {
            rating = r;
            logProb = lp;
        }

        public String toString() {
            return String.format("%d\n%.5f\n", rating, logProb);
        }
    }

    public static void main(String[] args) {
        Scanner myScanner = new Scanner(System.in);
        getModels(myScanner);
        classifySentences(myScanner);
    }

    public static void getModels(Scanner sc) {
        int nextFresh = FIRST_SENTENCE_NUM;
        initializeStructures();
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("---")) {
                return;
            }
            String[] fields = line.split("\\s\\s\\s\\s");
            try {
                Integer sentenceNum = Integer.parseInt(fields[1]);
                if (sentenceNum != nextFresh) {
                    continue;
                }
                nextFresh++;
                Integer sentiment = Integer.parseInt(fields[3]);
                sentimentCounts[sentiment]++;
                updateWordCounts(fields[2], wordCounts.get(sentiment),
                                 bigramCounts.get(sentiment), 
                                 bigramDenoms.get(sentiment),
                                 sentiment);
            } catch (Exception e) {
                // We probably just read the header of the file.
                // Or some other junk.  Ignore.
            }
        }
    }

    // Initialize the global count data structures
    public static void initializeStructures() {
        sentimentCounts = new int[CLASSES];
        totalWords = new int[CLASSES];
        totalBigrams = new int[CLASSES];
        wordCounts = new ArrayList<HashMap<String, Integer>>();
        bigramCounts = new ArrayList<HashMap<String, Integer>>();
        bigramDenoms = new ArrayList<HashMap<String, Integer>>();
        for (int i = 0; i < CLASSES; i++) {
            wordCounts.add(new HashMap<String, Integer>());
            bigramCounts.add(new HashMap<String, Integer>());
            bigramDenoms.add(new HashMap<String, Integer>());
        }
    }

    // updateWordCounts:  assume space-delimited words/tokens
    // notice that we are shadowing the globals with sentiment-specific
    // hashmaps
    public static void updateWordCounts(String sentence, 
                                        HashMap<String, Integer> wordCounts, 
                                        HashMap<String, Integer> bigramCounts, 
                                        HashMap<String, Integer> bigramDenoms,
                                        int sentiment) {
        String[] tokenized = sentence.split(" ");
        for (int i = 0; i < tokenized.length; i++) {
            totalWords[sentiment]++;
            String standardized = tokenized[i].toLowerCase();
            if (wordCounts.containsKey(standardized)) {
                wordCounts.put(standardized, wordCounts.get(standardized)+1);
            } else {
                wordCounts.put(standardized, 1);
            }
            if (i > 0) {
                String bigram = (tokenized[i-1] + " " + tokenized[i]).toLowerCase();
                if (bigramCounts.containsKey(bigram)) {
                    bigramCounts.put(bigram, bigramCounts.get(bigram) + 1);
                } else {
                    bigramCounts.put(bigram, 1);
                }

                String standardizedPrev = tokenized[i-1].toLowerCase();
                if (bigramDenoms.containsKey(standardizedPrev)) {
                    bigramDenoms.put(standardizedPrev, bigramDenoms.get(standardizedPrev) + 1);
                } else {
                    bigramDenoms.put(standardizedPrev, 1);
                }
                totalBigrams[sentiment]++;
            }
        }
    }

    // Assume test data consists of just space-delimited words in sentence
    public static void classifySentences(Scanner sc) {
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
//            Classification nbClass = naiveBayesClassify(line);
            Classification mmClass = markovModelClassify(line);
//            System.out.print(nbClass.toString() + mmClass.toString());
	        System.out.print(mmClass.toString());
        }
    }

    // Classify a new sentence using the data and a Naive Bayes model.
    // Assume every token in the sentence is space-delimited, as the input
    // was.
    public static Classification naiveBayesClassify(String sentence) {
        // TODO
	    float[] log_probs = new float[CLASSES];

        for (int i=0; i< CLASSES; i++){
            float prior_prob;
            float posterior_prob;

            prior_prob = (float)sentimentCounts[i]/ IntStream.of(sentimentCounts).sum();
            log_probs[i] = (float) Math.log(prior_prob);

            // Naive Bayes Rule
            String[] tokenized = sentence.split("\\s");
            for (int j = 0; j < tokenized.length; j++){
				try {
					float posterior = (float)(wordCounts.get(i).get(tokenized[j]))/(totalWords[i]);
					posterior_prob = (float) Math.log(posterior);
				}
				// If the word doesnt exist in the training data
				catch (NullPointerException e){
					posterior_prob = (float) Math.log(OUT_OF_VOCAB_PROB);
				}
            	log_probs[i] += posterior_prob;
            }
        }

		float max=log_probs[0];
        int index=0;
        for (int i=1;i<log_probs.length;i++){
        	    if (log_probs[i] > max){
        	    	max = log_probs[i];
        	    	index=i;
	            }
        }
	    return new Classification(index, max);
    }

    // Like naiveBayesClassify, but each word is conditionally dependent
    // on the preceding word.
    public static Classification markovModelClassify(String sentence) {
        // TODO

	    float[] log_probs = new float[CLASSES];

	    for (int i=0; i< CLASSES; i++){
		    float prior_prob;
		    float posterior_prob;

		    prior_prob = (float)sentimentCounts[i]/ IntStream.of(sentimentCounts).sum();
		    log_probs[i] = (float) Math.log(prior_prob);

		    // Naive Bayes Rule
		    String[] tokenized = sentence.split("\\s");
		    for (int j = 0; j < tokenized.length; j++){

		    	if (j==0) {
				    try {
					    float posterior = (float) (wordCounts.get(i).get(tokenized[j])) / (totalWords[i]);
					    posterior_prob = (float) Math.log(posterior);
					    // If the word doesnt exist in the training data
				    } catch (NullPointerException e) {
					    posterior_prob = (float) Math.log(OUT_OF_VOCAB_PROB);
				    }
				    log_probs[i] += posterior_prob;
			    }
			    //for Bigrams
			    else{
				    try {
					    float posterior = (float) (bigramCounts.get(i).get(tokenized[j-1]+" "+tokenized[j])) / (bigramDenoms.get(i).get(tokenized[j-1]));
					    posterior_prob = (float) Math.log(posterior);
				    }
				    // If the word bigram exist in the training data
				    catch(NullPointerException e){
					    posterior_prob = (float) Math.log(OUT_OF_VOCAB_PROB);
				    }
				    log_probs[i] += posterior_prob;
			    }
		    }
	    }

	    float max=log_probs[0];
	    int index=0;
	    for (int i=1;i<log_probs.length;i++){
		    if (log_probs[i] > max){
			    max = log_probs[i];
			    index=i;
		    }
	    }
	    return new Classification(index, max);

    }
}

