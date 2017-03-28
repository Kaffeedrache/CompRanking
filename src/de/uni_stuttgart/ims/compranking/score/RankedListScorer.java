// (c) Wiltrud Kessler
// 05.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/




package de.uni_stuttgart.ims.compranking.score;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.uni_stuttgart.ims.util.HashMapHelpers.Pair;


/**
 * Different methods for scoring ranked lists.
 *
 * @author kesslewd
 *
 */
public class RankedListScorer {


   /** Turn on debug outputs. **/
   public static boolean verbose = false;



   /**
    * Calculate Spearman correlation coefficient of the two ranked lists.
    * Rankings have to have the same size (checked)
    * and contain the same elements (not checked).
    *
    * Formula:
    * \rho = {1- \frac {6 \sum d_i^2}{n(n^2 - 1)}}.
    * where d_i is the distance btw the two ranks.
    *
    *
    *
    * @param rankingGold
    * @param rankingSystem
    * @return
    */
   public static double getSpearman (Ranking rankingGold, Ranking rankingSystem) {

      // Rankings must contain the same element, check if they have the same size.
      if (rankingGold.size() != rankingSystem.size()) {
         System.err.print("RankedListScorer.getSpearman() ERROR: Lists must have the same length!");
         return Double.NaN;
      }

      // n = number of items in the rankings
      int n = rankingGold.size();

      // dpart = sum of distances btw ranks
      double dpart = 0;

      double biggestDist = 0; // DEBUG
      String product = null; // DEBUG
      double theRankGold = 0; // DEBUG
      double theRankSystem = 0; // DEBUG

      //~ verbose = true;
      if (verbose) System.out.println("\n\n" + rankingGold.name + " -> " + rankingSystem.name);
      for (RankingElement thingy : rankingGold) {
         double rankGold = rankingGold.getRank(thingy.content);
         double rankSystem = rankingSystem.getRank(thingy.content);
         double distSQ = Math.pow(rankGold - rankSystem, 2);
         if (Math.abs(rankGold - rankSystem) > biggestDist) {
            biggestDist = Math.abs(rankGold - rankSystem);
            product = thingy.content;
            theRankGold = rankGold;
            theRankSystem = rankSystem;
         }
         if (verbose) System.out.println(thingy + " " + rankGold + " -> " + rankSystem
               + "    " + rankingSystem.getElement(thingy.content).toLongString() + " (d=" +  Math.abs(rankGold - rankSystem) + ")");

         dpart+= distSQ;
      }

      // Do the final calculation
      double rho =  (6 * dpart) / (n * (Math.pow(n,2) - 1));
      rho = 1 - rho;

      if (verbose) System.out.println("Biggest distance: " + biggestDist + " " + product + " " + theRankGold + " -> " + theRankSystem);

      return rho;

   }



   /**
    * Calculate Precision and Recall at a fixed k
    *
    * @param rankingGold
    * @param rankingSystem
    * @param goldK
    * @return
    */
   public static List<Pair<Double, Double>> getPRatFixedK (Ranking rankingGold, Ranking rankingSystem, int goldK) {

      if (rankingGold.size() < goldK) {
         System.err.print("RankedListScorer.getPRatFixedK() ERROR: Gold list is not long enough to use k=" + goldK + "!");
         return null;
      }

      // Add the topmost k items of the gold list as correct results
      Set<RankingElement> goldlist = new TreeSet<RankingElement>();
      for (int j=0; j<goldK; j++) {
         goldlist.add(rankingGold.get(j));
      }


      // For all k, calculate precision/recall at k
      int tp = 0;
      int fp = 0;
      int fn = 0;

      List<Pair<Double, Double>> prlist = new ArrayList<Pair<Double, Double>>();

      for (int j=0; j<rankingGold.size(); j++) {
         RankingElement system = rankingSystem.get(j);

         if (goldlist.contains(system)) {
            tp++;
         } else {
            fp++;
         }

         fn = goldK - tp;

         double p = getP(tp, fp);
         double r = getR(tp, fn);

         prlist.add(new Pair<Double,Double>(p,r));

         if (verbose)
            System.out.println(String.format("P at %d : %.4f  (TP: %d, FP: %d)",(j+1), p, tp, fp) + "\t" + String.format("R at %d : %.4f   (TP: %d, FN: %d)",(j+1), r, tp, fn));


      }

      return prlist;

   }


   /**
    * Calculate Precision
    *
    * @param tp
    * @param fp
    * @return
    */
   private static double getP (int tp, int fp) {
      if (tp == 0)
         return 0;
      return (double) (tp * 100) / (double)(tp+fp);
   }

   /**
    * Calculate Recall
    *
    * @param tp
    * @param fn
    * @return
    */
   private static double getR (int tp, int fn) {
      if (tp == 0)
         return 0;
      return (double)(tp * 100) / (double)(tp+fn);
   }






   /**
    *
    * For all k, calculate precision/recall at k
    * Rankings have to have the same size (checked).
    *
    * @param rankingGold
    * @param rankingSystem
    * @return
    */
   public static List<Double> getPRatK (Ranking rankingGold, Ranking rankingSystem) {

      if (rankingGold.size() != rankingSystem.size()) {
         System.err.print("RankedListScorer.getPRatK() ERROR: Lists must have the same length, have " + rankingGold.size() + " vs " + rankingSystem.size() + " !");
         return null;
      }

      int tp = 0; // true positives
      int fp = 0; // false positives
      int fn = 0; // false negatives

      // List of P values at different k
      List<Double> plist = new ArrayList<Double>();

      // List of false negatives found until now
      Set<RankingElement> fnlist = new TreeSet<RankingElement>();

      // List of false positives found until now
      Set<RankingElement> fplist = new TreeSet<RankingElement>();

      for (int k=0; k<rankingGold.size(); k++) {
         RankingElement gold = rankingGold.get(k);
         RankingElement system = rankingSystem.get(k);

         if (gold.equals(system)) { // correct, TP
            tp++;
         }
         else {

            // if system element has already been seen
            // in the gold list
            //    -> this is actually correct (belatedly)
            //    -> add TP, remove from FN list
            // if not
            //    -> this is a false positive
            //    -> add FP, add to FP list
            if (fnlist.contains(system)) {
               tp++;
               fn--;
               fnlist.remove(system);
            } else {
               fp++;
               fplist.add(system);
            }

            // if gold element has already been seen
            // in the system list
            //    -> this is actually correct (belatedly)
            //    -> add TP, remove from FP list
            // if not
            //    -> this is a false negative
            //    -> add FN, add to FN list
            if (fplist.contains(gold)) {
               tp++;
               fp--;
               fplist.remove(gold);
            } else {
               fn++;
               fnlist.add(gold);
            }

         }

         double p = getP(tp, fp);
         double r = getR(tp, fn);

         plist.add(p);

         if (verbose) System.out.println(String.format("P at %d : %.2f  (TP: %d, FP: %d)",(k+1), p, tp, fp));
         if (verbose) System.out.println(String.format("R at %d : %.2f   (TP: %d, FP: %d)",(k+1), r, tp, fp));

      }

      return plist;


   }



   /**
    * Get Mean Average Precision at different cutoffs of the gold list
    *
    * @param prlist
    * @return
    */
   public static Double getMAP (List<Pair<Double, Double>> prlist) {


      // # relevant docs = mj
      // ( 1/mj sum_k=1..mj ( Precision(doc k found) )
      double currentR = 0;
      double map = 0;
      int foundRelevant = 0;
      for (Pair<Double, Double> pr : prlist) {
         if (pr.y != currentR) { // R changes = have retrieved one more doc,
            map += pr.x;    // precision here = pr.x
            foundRelevant ++; // have found one more relevant
            currentR = pr.y; // set new recall level
         }

      }

      return map/foundRelevant;

   }






}
