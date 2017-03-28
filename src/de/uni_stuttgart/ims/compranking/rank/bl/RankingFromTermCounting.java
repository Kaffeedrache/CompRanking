// (c) Wiltrud Kessler
// 06.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/





package de.uni_stuttgart.ims.compranking.rank.bl;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.TreeSet;

import de.uni_stuttgart.ims.compranking.rank.ProductOpinionCounterPosNeg;
import de.uni_stuttgart.ims.compranking.rank.ProductOpinionCounterPosNeg.PolarityMode;
import de.uni_stuttgart.ims.compranking.rank.bl.SentimentDictionary.DictionaryType;
import de.uni_stuttgart.ims.nlpbase.tools.Tokenizer;
import de.uni_stuttgart.ims.nlpbase.tools.TokenizerStanford;
import de.uni_stuttgart.ims.util.Fileutils;


/**

Extract ranking from Amazon reviews by counting sentiment terms

**/
public class RankingFromTermCounting {



   // Debug/bookkeeping
   private int numberReviews = 0;
   private int numberTermsPos = 0;
   private int numberTermsNeg = 0;


   //private int correctpartslength = 8; // OLD amazon format
   private int correctpartslength = 10; // NEW amazon format
   private boolean ignoreformaterrors = true;


   private ProductOpinionCounterPosNeg counterTerms;

   private SentimentDictionary dictionary;


   private boolean useMyOwnID = true; // generate own ID, discard those found in Amazon file
   private boolean includeTitle;

   private Tokenizer tokenizer;


   /**
    * Extract ranking from Amazon reviews by counting sentiment terms.
    *
    * Usage: RankingFromLength <output filename> <input files (Amazon CSV)>*
    *
    * @param args
    */
   public static void main(String[] args) {


      String outputRankingTerms = null;
      String[] files = null;
      String relevantProductsFile = null;


      boolean[] useNormalization = new boolean[] {true, false};;
      DictionaryType[] useDictionaries = DictionaryType.values();



      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 2) {
            System.err.println("Usage: RankingFromTermCounting <output filename> <input files (Amazon CSV)>*");
            System.exit(1);
         } else {

            outputRankingTerms = args[0];
            relevantProductsFile = args[1];
            files = Arrays.copyOfRange(args, 2, args.length);
         }

      } catch (Exception e) {
         System.err.println("ERROR !!! in initialization: " + e.getMessage());
         System.exit(1);
      }

      System.out.println("Print output to file: " + outputRankingTerms);
      System.out.println("Take relevant products from file: " + relevantProductsFile);
      System.out.println("Take input files: " + Arrays.toString(files));



      // ===== PROCESS =====


      RankingFromTermCounting a = new RankingFromTermCounting();


      try {
         a.readRelevantProducts(relevantProductsFile, true);
      } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }


      for (DictionaryType dictionarytype : useDictionaries) {

         for (boolean normalization : useNormalization) {


            a.reset(dictionarytype, normalization);

            for (String csvFile : files) {
               System.out.println("Process file: " + csvFile);
               a.analyze(csvFile);
            }

            String f = (normalization)?"Norm":"NN";
            a.writeToFile(outputRankingTerms.replace(".txt", "_" + dictionarytype + f + ".txt"));

            a.endDocument();
         }
      }

      System.out.println("done.");
   }




   private RankingFromTermCounting () {
      this(DictionaryType.mpqa, true);
   }

   private RankingFromTermCounting (DictionaryType dictionarytype, boolean normalization) {

      // Initialize tokenizer (Stanford)
      tokenizer = new TokenizerStanford();

      reset(dictionarytype, normalization);
   }


   private void reset (DictionaryType dictionarytype, boolean normalization)  {

      // Initialize polarity score counter
      // (do both polarities, normalize)
      counterTerms = new ProductOpinionCounterPosNeg(PolarityMode.both, normalization);


      // Initialize sentiment dictionary
      dictionary = new SentimentDictionary(dictionarytype);


      if (addAsNeutral) {
         System.out.println("add neutral");
         for (String productID : relevantProducts) {
            counterTerms.addNeutral(productID);
         }
      }
   }




   private TreeSet<String> relevantProducts;
   private boolean addAsNeutral;


   private void readRelevantProducts (String filename, boolean addAsNeutral) throws FileNotFoundException {

      DataInputStream fstream = new DataInputStream(new FileInputStream(filename));
      BufferedReader brRevProds = new BufferedReader(new InputStreamReader(fstream, Charset.forName("UTF-8")));

      relevantProducts = new TreeSet<String>();

      String nextLine = null;
      int lineno = 0;
      try {

         while ((nextLine = brRevProds.readLine()) != null) {
            lineno++;
            String[] parts = nextLine.split("\t");
            // productID \t maybe other stuff
            relevantProducts.add(parts[0]);
         }

         Fileutils.closeSilently(brRevProds);
      } catch (Exception e) {
         System.out.println("ERROR in line " + lineno + " : " + nextLine);
         // TODO Auto-generated catch block
         e.printStackTrace();
      }


      this.addAsNeutral = addAsNeutral;

   }




   private void analyze (String csvFile) {


      // open input file
      BufferedReader br = null;
      try {
         br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(csvFile)), Charset.forName("UTF-8")));
      } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      int tmp = csvFile.lastIndexOf('/');
      String prefix = csvFile.substring(Math.max(0, tmp+1), Math.max(0, csvFile.indexOf('.', tmp))) + "-";

      String strLine = "";
      int lineno = 0;

      while (strLine != null) {

         String id = prefix + lineno; // fallback id
         lineno++;

         // Read line (= 1 review)
         try {
            strLine = br.readLine();
         } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }
         if (strLine == null) break;


         // Split at "," (delims)
         // for "all" format - split at \t
         String[] parts = strLine.split("\",\"");
         if (parts.length != correctpartslength) {
            parts = strLine.split("\t");
         }

         // Ignore things that are not reviews
         // (there are some errors in the extraction script)
         if (parts.length != correctpartslength ) {
            if (!ignoreformaterrors)
               System.out.println(parts.length + " parts -- ignore line " + Arrays.toString(parts));
            continue;
         } else {
            //System.out.println(parts.length + " parts correct " + Arrays.toString(parts));

         }



         /// Format:
         // all:
         // Username \t Date \t Product ID \t Reviewer ID \t ? \t <empty> \t Rating \t Helpful \t Title \t \Review
         // = 9 parts
         // S. Pulgarin 25.09.2014  B00KKG1846  R28SZ7VYJM9XC6 84    5.0   0/0   Five Stars  Works as expected.

         // italy:
         // 0 \t XX \t Product ID \t Rating \t ? \t ? \t Date \t Reviewer ID \t Title \t \Review
         // = 10 parts
         // "0","XX","0061785679","5.0","0","0","February 9, 2015","A3GZG3F23DL9U1","Five Stars","Loved it"


         // Get parts (id, title, text, product)
         if (!parts[0].trim().isEmpty() & !useMyOwnID)
            id = parts[0].substring(1); // clip of first "

         String product = parts[2];


         // consider only relevant products
         if (!relevantProducts.contains(product))
            continue;

         numberReviews++;


         String text = parts[parts.length-1];
         text = text.substring(0, text.length()-1); // clip of last "
         processText(text, product, id);

         if (includeTitle) {
            String title = parts[parts.length-2];
            processText(title, product, id);

         }

         //System.out.println(product + " - " + rating );

         //if (lineno > 25) break; // DEBUG !!!


      }

      //System.out.println("... processed " + lineno + " reviews.");


   }



   private void processText (String text, String product, String id) {

      //for (String word : text.split(" ")) { // todo real tokenization??


      String[] tokens = tokenizer.tokenize(text);


      counterTerms.addReviewWithLength(product, id, tokens.length);
      //System.out.println("process: " + id + "   proct: " + product  + "   add len: " + tokens.length );

      for (int i=0; i<tokens.length; i++) {

         String token = tokens[i].toLowerCase();

         if (dictionary.isPositiveWord(token)) {
            counterTerms.addPos(product,id);
            numberTermsPos++;
            //System.out.println("add pos " + word);
         }
         if (dictionary.isNegative(token)) {
            counterTerms.addNeg(product,id);
            numberTermsNeg++;
            //System.out.println("add neg " + word);
         }

      }
   }



   /**
    * Called at the end of processing, write ranking to file
    */
   public void writeToFile (String outputRankingTerms) {
      counterTerms.writeRankingToFile(outputRankingTerms);
   }


   /**
    * Called at the end of the document, clean up.
    */
   public void endDocument () {
      System.out.println( "Processed " + numberReviews + " reviews, with "
               + (numberTermsPos+numberTermsNeg) + " terms (" + numberTermsPos + " pos, " + numberTermsNeg + " neg).");

   }

}
