// (c) Wiltrud Kessler
// 06.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/



package de.uni_stuttgart.ims.compranking.rank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import de.uni_stuttgart.ims.util.HashMapHelpers;


/**
 * Keep track of discrete polar (positive/negative) opinions about a product.
 *
 * @author kesslewd
 *
 */
public class ProductOpinionCounterPosNeg extends ProductOpinionCounter {


   /**
    * Scoring mode.
    *
    * @author kesslewd
    *
    */
   public enum PolarityMode { agnostic, onlyPos, both };

   /**
    * Used scoring mode.
    */
   private PolarityMode usePolarityMode = PolarityMode.agnostic;


   /**
    * The collected positive opinions for all products.
    */
   private HashMap<String, Integer> opinionPositive ;

   /**
    * The collected negative opinions for all products.
    */
   private HashMap<String, Integer> opinionNegative;

   /**
    * The collected review lengths for all products
    * (used in nromalization).
    */
   private HashMap<String, Integer> reviewLengths;

   /**
    * Do normalization of scores?
    */
   private boolean doNormalize = true;



   /**
    * Keep track of discrete polar (positive/negative) opinions about a product.
    *
    * @param usePolarityMode
    * @param normalize
    */
   public ProductOpinionCounterPosNeg(PolarityMode usePolarityMode, boolean normalize) {

      this.usePolarityMode = usePolarityMode;

      this.doNormalize = normalize;

      opinionPositive = new HashMap<String, Integer>();
      opinionNegative = new HashMap<String, Integer>();

      reviewLengths = new HashMap<String, Integer>();

   }



   /**
    * Add a review to the list of reviews (unique)
    * and its legnth
    *
    * @param productID
    * @param reviewID
    */
   public void addReviewWithLength (String productID, String reviewID, int reviewLength) {
      super.addReview(productID, reviewID);
      HashMapHelpers.addOrCreate(reviewLengths, productID, reviewLength);

   }



   /**
    * Add a neutral review
    * (i.e., positive score 0)
    */
   @Override
   public void addNeutral(String productID) {
      HashMapHelpers.addOrCreate(opinionPositive, productID, 0);
   }

   /**
    * Add a review and positive score+1 for the product.
    *
    * @param productID
    * @param reviewID
    */
   public void addPos (String productID, String reviewID) {
      HashMapHelpers.addOrCreate(opinionPositive, productID, 1);
      addReview(productID, reviewID);
   }

   /**
    * Add a review and depending on the polarity mode
    * - agnostic: count an opinion as positive
    * - onlyPos: ignore this negative opinion
    * - both: add a negative opinion
    *
    * @param productID
    * @param reviewID
    */
   public void addNeg (String productID, String reviewID) {
      HashMap<String, Integer> useMap = null;
      switch (usePolarityMode) {
      case agnostic : useMap = opinionPositive; // count as positive (don't care about polarity)
         break;
      case onlyPos : return; // ignore negative opinions
      case both : useMap = opinionNegative; // count as negative
         break;
      }
      HashMapHelpers.addOrCreate(useMap, productID, 1);
      addReview(productID, reviewID);
   }



   /**
    * Overall opinion depends on normalization:
    *
    * nonnormalized:
    * - sum of all ratings for a product
    *
    * normalized:
    * - positive opinions minus negative opinions
    * - divided by length of reviews (in tokens)
    *
    */
   public HashMap<String, Double> getOverallOpinion() {
      // have treated differences in pos/neg already when inputting,
      // this is all identical for all except for normalization

      HashMap<String, Double> opinionsAll = new HashMap<String, Double>();

      Set<String> allIDs = new HashSet<String>(opinionPositive.keySet());
      System.out.println(allIDs.size());
      allIDs.addAll(opinionNegative.keySet());
      System.out.println(allIDs.size());

      for (String prodID : allIDs) {
         Integer valuePos = opinionPositive.get(prodID);
         Integer valueNeg = opinionNegative.get(prodID);

         if (valuePos == null)
            valuePos = 0;
         if (valueNeg == null)
            valueNeg = 0;


         TreeSet<String> reviews = numberOfReviews.get(prodID);
         int numberReviews = 0;
         if (reviews != null)
            numberReviews = reviews.size();

         String comment = valuePos + "\t" + valueNeg + "\t" + numberReviews;


         if (doNormalize) {

            Integer numberTokens = reviewLengths.get(prodID); // cannot be null
            if (numberTokens == null) {
               numberTokens = 0;
               opinionsAll.put(prodID, 0.0);
            } else {
               opinionsAll.put(prodID, (double) (valuePos - valueNeg) / numberTokens);
            }
            comment += "\t" + (int)numberTokens;

         }
         else
            opinionsAll.put(prodID, (double) (valuePos - valueNeg));



         comments.put(prodID, comment);
      }

      return opinionsAll;
   }




}
