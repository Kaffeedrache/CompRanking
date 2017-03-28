// (c) Wiltrud Kessler
// 19.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/



package de.uni_stuttgart.ims.compranking.rank;

import java.util.HashMap;

import de.uni_stuttgart.ims.util.HashMapHelpers;


/**
 * Keep track of numerical opinions (ratings) about a product.
 *
 * @author kesslewd
 *
 */
public class ProductOpinionCounterNumeric extends ProductOpinionCounter {


   /**
    * The ratings
    */
   private HashMap<String, Double> opinionRating;

   /**
    * Do normalization of scores?
    */
   private boolean normalize = false;


   /**
    * Keep track of numerical opinions (ratings) about a product.
    * @param normalize
    */
   public ProductOpinionCounterNumeric(boolean normalize) {
      opinionRating = new HashMap<String, Double>();
      this.normalize = normalize;
   }


   /**
    * Add a rating for a product from a given review.
    *
    * @param productID
    * @param reviewID
    * @param rating
    */
   public void addRating (String productID, String reviewID, String rating) {
      addRating(productID, reviewID, Double.parseDouble(rating));
   }

   /**
    * Add a rating for a product from a given review.
    *
    * @param productID
    * @param reviewID
    * @param rating
    */
   public void addRating (String productID, String reviewID, double rating) {
      HashMapHelpers.addOrCreate(opinionRating, productID, rating);
      addReview(productID, reviewID);
   }


   /**
    * Overall opinion depends on normalization:
    *
    * nonnormalized:
    * - sum of all ratings for a product
    *
    * normalized:
    * - sum of all ratings for a product
    * - divided by number of reviews
    *
    */
   public HashMap<String, Double> getOverallOpinion() {

      HashMap<String, Double> opinionsAll = new HashMap<String, Double>();

         // Final rating is (sum of ratings) / (number of reviews)
         // [i.e., average star rating]

         for (String prodID : opinionRating.keySet()) {
            double ratingsSum = opinionRating.get(prodID); // cannot be null
            if (normalize) {
               double numberReviews = numberOfReviews.get(prodID).size(); // cannot be null
               //System.out.println(prodID + " " + ratingsSum + " / " + numberReviews + " = "+ (ratingsSum / numberReviews) );
               opinionsAll.put(prodID, ratingsSum / numberReviews);
               comments.put(prodID,  ratingsSum + "\t" + numberReviews);
            } else {
               opinionsAll.put(prodID, ratingsSum);
            }

         }
         return opinionsAll;

   }


   /**
    * Adds a neutral opinion, but no review.
    */
   @Override
   public void addNeutral(String productID) {
      HashMapHelpers.addOrCreate(opinionRating, productID, 0);
      // do not add a review
   }



}
