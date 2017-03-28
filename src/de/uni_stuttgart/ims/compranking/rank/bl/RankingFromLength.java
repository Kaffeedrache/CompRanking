// (c) Wiltrud Kessler
// 19.05.2015
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

import de.uni_stuttgart.ims.compranking.rank.ProductOpinionCounterNumeric;
import de.uni_stuttgart.ims.nlpbase.tools.TokenizerStanford;
import de.uni_stuttgart.ims.util.Fileutils;


/**

Extract ranking from Amazon reviews by review length

**/
public class RankingFromLength {


   // Debug/bookkeeping
   private int numberReviews = 0;
   private int numberTokens = 0;


   //private int correctpartslength = 8; // OLD amazon format
   private int correctpartslength = 10; // NEW amazon format
   private boolean ignoreformaterrors = true;

   private ProductOpinionCounterNumeric counterLengthNorm;
   private ProductOpinionCounterNumeric counterLengthNonNorm;

   private boolean useMyOwnID = true; // generate own ID, discard those found in Amazon file
   private TokenizerStanford tokenizer;

   /**
    * Extract ranking from Amazon reviews by review length
    *
    * Usage: RankingFromLength <output filename> <relevant products file> <input files (Amazon CSV)>*
    *
    * @param args
    */
   public static void main(String[] args) {


      String outputRankingLength = null;
      String[] files = null;
      String relevantProductsFile = null;


      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 2) {
            System.err.println("Usage: RankingFromLength <output filename> <relevant products file> <input files (Amazon CSV)>*");
            System.exit(1);
         } else {

            outputRankingLength = args[0];
            relevantProductsFile = args[1];
            files = Arrays.copyOfRange(args, 2, args.length);
         }

      } catch (Exception e) {
         System.err.println("ERROR !!! in initialization: " + e.getMessage());
         System.exit(1);
      }

      System.out.println("Print output to file: " + outputRankingLength);
      System.out.println("Take relevant products from file: " + relevantProductsFile);
      System.out.println("Take input files: " + Arrays.toString(files));


      // ===== PROCESS =====

      RankingFromLength a = new RankingFromLength();

      try {
         a.readRelevantProducts(relevantProductsFile);
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }


      for (String csvFile : files) {
         System.out.println("Process file: " + csvFile);
         a.analyze(csvFile);
      }


      // ===== OUTPUT, END =====

      a.writeToFileNorm(outputRankingLength.replace(".txt", "_avg.txt"));
      a.writeToFileNonNorm(outputRankingLength.replace(".txt", "_abs.txt"));

      a.endDocument();

      System.out.println("done.");
   }




   private RankingFromLength () {

      // Initialize product counter
      counterLengthNorm = new ProductOpinionCounterNumeric(true);
      counterLengthNonNorm = new ProductOpinionCounterNumeric(false);

      // Initialize tokenizer (Stanford)
      tokenizer = new TokenizerStanford();

   }


   private TreeSet<String> relevantProducts;


   private void readRelevantProducts (String filename) throws FileNotFoundException {

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
         e.printStackTrace();
      }

   }



   private void analyze (String csvFile) {


      // open input file
      BufferedReader br = null;
      try {
         br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(csvFile)), Charset.forName("UTF-8")));
      } catch (FileNotFoundException e) {
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


         // Tokenize
         String[] tokens = tokenizer.tokenize(text);
         numberTokens+=tokens.length;
         counterLengthNorm.addRating(product, id, tokens.length);
         counterLengthNonNorm.addRating(product, id, tokens.length);


         //if (lineno > 25) break; // DEBUG !!!


      }

      System.out.println("... processed " + lineno + " reviews.");


   }




   /**
    * Called at the end of processing, write ranking to file
    */
   public void writeToFileNorm (String outputRankingTerms) {
      counterLengthNorm.writeRankingToFile(outputRankingTerms);
   }

   /**
    * Called at the end of processing, write ranking to file
    */
   public void writeToFileNonNorm (String outputRankingTerms) {
      counterLengthNonNorm.writeRankingToFile(outputRankingTerms);
   }


   /**
    * Called at the end of the document, clean up.
    */
   public void endDocument () {
      System.out.println( "Processed " + numberReviews + " relevant reviews, with "
               + numberTokens + " tokens.");
   }

}
