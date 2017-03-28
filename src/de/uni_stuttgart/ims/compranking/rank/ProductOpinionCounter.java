// (c) Wiltrud Kessler
// 06.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/



package de.uni_stuttgart.ims.compranking.rank;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import de.uni_stuttgart.ims.util.Fileutils;
import de.uni_stuttgart.ims.util.HashMapHelpers;


/**
 * Keep track of opinions about a product.
 *
 * @author kesslewd
 *
 */
public abstract class ProductOpinionCounter {


   /**
    * List of products (keys)
    * and the corresponding reviews (values).
    */
   protected HashMap<String, TreeSet<String>> numberOfReviews;


   /**
    * DEBUG/HACK
    * List of products (keys)
    * and interesting things/debug/analysis about each product (value).
    * Fill this in 'getOverallOpinion' if you want it printed in 'writeRankingToFile'.
    */
   protected HashMap<String, String> comments;


   /**
    * Use this to count opinions about a product.
    */
   public ProductOpinionCounter() {
      numberOfReviews = new HashMap<String, TreeSet<String>>();
      comments = new HashMap<String, String>();
   }



   /**
    * Add a review to the list of reviews (unique)
    *
    * @param productID
    * @param reviewID
    */
   public void addReview (String productID, String reviewID) {
      HashMapHelpers.addOrCreate(numberOfReviews, productID, reviewID);
   }


   /**
    * (Implementation specific)
    * Add a neutral opinion for this product.
    * @param productID
    */
   public abstract void addNeutral (String productID);


   /**
    * (Implementation specific)
    * From all the collected information, return one overall score
    * per product that indicates the overall opinion.
    *
    * @return Map with key=product , value=score
    */
   public abstract HashMap<String, Double> getOverallOpinion() ;



   /**
    * Write ranking to file. File is overwritten if it exists.
    * Format:
    * Product \t Rankscore [ \t comment]
    *
    *
    * @param outputFileName
    */
   public void writeRankingToFile (String outputFileName) {

      // Open output file
      System.out.println("Output ranking by terms to file " + outputFileName);
      BufferedWriter outRankingTerms = null;
      try {
         outRankingTerms = Fileutils.getWriteFile(outputFileName);
      } catch (IOException e) {
         e.printStackTrace();
      }


      // Write ranking
      HashMap<String, Double> mentionRanking = this.getOverallOpinion();
      int x=0;
      for (Entry<String, Double> entry : HashMapHelpers.sortHashMapByValueDescending(mentionRanking)) {

         // Debug output
         x++;
         if (x < 10)  // print first 10 entries
            System.out.println(entry.getKey() + ": " + entry.getValue());
         if (x == 10) // print a few dots
            System.out.println("...");
         if (x > mentionRanking.size() - 10 & x > 10) // and last 10 entries
            System.out.println(entry.getKey() + ": " + entry.getValue());

         String comment = comments.get(entry.getKey());

         if (comment != null)
            Fileutils.writeLine(outRankingTerms, entry.getKey() + "\t" + entry.getValue() + "\t" + comment);
         else
            Fileutils.writeLine(outRankingTerms, entry.getKey() + "\t" + entry.getValue());
      }

      // Close file
      Fileutils.closeSilently(outRankingTerms);

   }



}
