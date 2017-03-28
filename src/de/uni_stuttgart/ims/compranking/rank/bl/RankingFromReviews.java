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

import de.uni_stuttgart.ims.compranking.rank.ProductOpinionCounterNumeric;
import de.uni_stuttgart.ims.util.Fileutils;


/**

Extract ranking from Amazon reviews by star ratings

**/
public class RankingFromReviews {


   // Debug/bookkeeping
   private int numberReviews = 0;
   int[] ratingDistribution;
   int numberReviewsRelevant = 0;

   //private int correctpartslength = 8; // OLD amazon format
   private int correctpartslength = 10; // NEW amazon format
   private boolean ignoreformaterrors = true;

   private ProductOpinionCounterNumeric counterStars;
   private ProductOpinionCounterNumeric counterMentions;


   private boolean useMyOwnID = true; // generate own ID, discard those found in Amazon file


   /**
    * Extract ranking from Amazon reviews by star ratings
    *
    * Usage: RankingFromLength <output filename stars> <output filename reviews> <input files (Amazon CSV)>*
    *
    * @param args
    */
   public static void main(String[] args) {

      String outputRankingStars = null;
      String outputRankingMentions = null;
      String[] files = null;
      String relevantProductsFile = null;



      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 3) {
            System.err.println("Usage: RankingFromLength <output filename stars> <output filename reviews> <input files (Amazon CSV)>*");
            System.exit(1);
         } else {

            outputRankingStars = args[0];
            outputRankingMentions = args[1];
            relevantProductsFile = args[2];
            files = Arrays.copyOfRange(args, 3, args.length);
         }

      } catch (Exception e) {
         System.err.println("ERROR !!! in initialization: " + e.getMessage());
         System.exit(1);
      }

      System.out.println("Print output to files: " + outputRankingStars + " (stars), " + outputRankingMentions + " (reviews)");
      System.out.println("Take relevant products from file: " + relevantProductsFile);
      System.out.println("Take input files: " + Arrays.toString(files));



      // ===== PROCESS =====

      RankingFromReviews a = new RankingFromReviews();


      try {
         a.readRelevantProducts(relevantProductsFile);
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }

      for (String csvFile : files) {
         System.out.println("Process file: " + csvFile);
         a.analyze(csvFile);
      }

      a.writeToFileStars(outputRankingStars);
      a.writeToFileMentions(outputRankingMentions);

      a.endDocument();

      System.out.println("done.");

   }






   private RankingFromReviews () {


      // Initialize product counter
      counterStars = new ProductOpinionCounterNumeric(true);
      counterMentions = new ProductOpinionCounterNumeric(false);

      ratingDistribution = new int[6];

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
         numberReviews++;
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
         boolean splitTab = false;
         String[] parts = strLine.split("\",\"");
         if (parts.length != correctpartslength) {
            parts = strLine.split("\t");
            splitTab = true;
         }

         // Ignore things that are not reviews
         // (there are some errors in the extraction script)
         if (parts.length != correctpartslength) {
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

         numberReviewsRelevant++;


         String rating = parts[3];
         if (splitTab)
            rating = parts[6];

         // DEBUG
         ratingDistribution[(int) Double.parseDouble(rating)]++;

         // TODO throw exception if rating is out of range??

         counterStars.addRating(product, id, rating);
         counterMentions.addRating(product,id, 1);

         //System.out.println(product + " - " + rating );

         //if (lineno > 25) break; // DEBUG !!!


      }

      System.out.println("... processed " + lineno + " reviews.");


   }




   /**
    * Called at the end of processing, write ranking to file
    */
   public void writeToFileStars (String outputRankingFile) {
      counterStars.writeRankingToFile(outputRankingFile);
   }



   /**
    * Called at the end of processing, write ranking to file
    */
   public void writeToFileMentions (String outputRankingFile) {
      counterMentions.writeRankingToFile(outputRankingFile);
   }





   /**
    * Called at the end of the document, clean up.
    */
   public void endDocument () {
      System.out.println( "Processed " + numberReviews + " reviews, "
            + numberReviewsRelevant + " relevant reviews  "
            );
      System.out.println( "Star rating distribution: " + Arrays.toString(ratingDistribution));

   }

}
