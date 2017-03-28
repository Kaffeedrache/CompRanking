// (c) Wiltrud Kessler
// 05.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/





package de.uni_stuttgart.ims.compranking.score;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import de.uni_stuttgart.ims.util.HashMapHelpers;
import de.uni_stuttgart.ims.util.HashMapHelpers.Pair;



/**
 * Read two rankings of products and compare them by P/R at k and Spearman coefficient.
 *
 * @author kesslewd
 *
 */
public class CompareRankings {




   private static int[] fixedKs = new int[] {};




   static boolean filterAllCommon = true;

   static boolean shuffleZeroTies = true;
   static int numberShuffles = 50;
   static boolean considerTies = true;



   /**
    * Read two rankings of products and compare them by P/R at k and Spearman coefficient.
    * Usage: CompareRankings <ranking file gold> <ranking file system>*
    *
    * @param args
    */
   public static void main(String[] args) {



      // ===== GET USER-DEFINED OPTIONS =====

      // Config all
      //boolean sortByRho = false;
      //boolean intercorrelations = true;

      // Config aspects
      boolean sortByRho = true;
      boolean intercorrelations = false;


      String rankingFilenameGold = null;

      if (args.length > 2) {
         rankingFilenameGold = args[0];
      } else {
         System.err.println("Usage: CompareRankings <ranking file gold> <ranking file system>*\n");
         System.exit(1);


      }

      String inFolder = "rankings";
      String prefix = "";

      File myfolder = new File(inFolder);
      File[] rankingFilesSystems = myfolder.listFiles(new FileFilter() {
          public boolean accept(File file) {
              return file.isFile() && file.getName().endsWith(".txt") && file.getName().startsWith(prefix);
          }
      });


      // ===== PROCESS =====

      CompareRankings blubb = new CompareRankings();

      Ranking rankingGold = new Ranking(rankingFilenameGold, false, false);
      System.out.println("rankingGold " + rankingFilenameGold + " (" + rankingGold.size() + " products)" );

      LinkedHashMap<String, List<Ranking>> rankingsSystem = new LinkedHashMap<String, List<Ranking>>();


      int items = 0;

      if (!shuffleZeroTies)
         numberShuffles = 1;


      // Add all rankings from the files to the list to be processed.
      // If shuffles are on, add several version of each, otherwise add one
      for (File rankingFileSystem : rankingFilesSystems) {

         //String nameSystem = rankingFilenameSystem.substring(rankingFilenameSystem.lastIndexOf('/')+1, rankingFilenameSystem.lastIndexOf('.')).replaceAll("_", "-");
         String nameSystem = rankingFileSystem.getName().replaceAll("_", "-");

         System.out.println(nameSystem);

         ArrayList<Ranking> thisSystemList = new ArrayList<Ranking>();
         rankingsSystem.put(nameSystem, thisSystemList);

         for (int i=0; i<numberShuffles; i++) {
            Ranking rankingSystem = new Ranking(rankingFileSystem, considerTies, shuffleZeroTies);

            if (filterAllCommon) { // filter common elements
               items = filterCommon(rankingGold, rankingSystem);
            } else { // check how much items the longest ranking has (just for output)
               if (rankingSystem.size() > items)
                  items = rankingSystem.size();
            }

            thisSystemList.add(rankingSystem);

         }
      }
      System.out.println("Found " + items + " common items.");





      String maplatex = "";
      String c = "";
      for (int index=0; index<fixedKs.length; index++) {
         int fixedK = fixedKs[index];
         if (fixedK > items)
            break;
         maplatex += String.format("& %d", fixedK);
         c += "c";
      }

      LinkedHashMap<String, Double> resultsAveraged = new LinkedHashMap<String, Double>();


      for (String systemName : rankingsSystem.keySet()) {

         List<ComparisonResult> results = new ArrayList<ComparisonResult>();
         int nonZeroItems = 0;

         double avgSpearman = 0;

         for (Ranking rankingSystem : rankingsSystem.get(systemName)) {

            // Compare the system list to the gold ranking
            ComparisonResult res = null;
            if (filterAllCommon)
               // (take as is, all filtering has been done before)
               res = blubb.compare(rankingGold, "gold", rankingSystem, rankingSystem.name);
            else {
               // copy all gold items to a new list, because they will be deleted
               // when the list is filtered for common items
               Ranking b = new Ranking(rankingGold);
               res = blubb.compare(b, "gold", rankingSystem, rankingSystem.name);
            }
            results.add(res);

            nonZeroItems = rankingSystem.nonZeroEntries;

            avgSpearman += res.spearman;

         }


         // Calculate average spearman value
         avgSpearman = avgSpearman / results.size();

         // Calculate standard deviation and averages of all other arrays
         double[] mapavg = new double[ComparisonResult.arraySize];
         double[] pat10avg = new double[ComparisonResult.arraySize];
         double[] pat20avg = new double[ComparisonResult.arraySize];
         double sigma = 0;
         for (ComparisonResult res : results) {
            sigma += Math.pow(res.spearman - avgSpearman, 2);
            for (int i=0; i<ComparisonResult.arraySize; i++) {
               mapavg[i] += res.map[i];
               pat10avg[i] += res.pat10[i];
               pat20avg[i] += res.pat20[i];
            }

         }
         sigma = sigma / results.size();
         sigma = Math.sqrt(sigma);

         String formatString = "";
         for (int i=0; i<ComparisonResult.arraySize; i++) {
            // MAP at k averaged
            mapavg[i] = mapavg[i] / results.size();
            formatString += String.format(" & %.1f", mapavg[i]);
         }


         String latexString = String.format("%s & %d & %.4f & %.4f %s \\\\", systemName, nonZeroItems, avgSpearman, sigma, formatString);

         resultsAveraged.put(latexString, avgSpearman);


      }


      // Average results



      // Print results latex table
      System.out.println(String.format("\\begin{tabular}{lc|rr%s}", c));
      System.out.println(String.format("name & n & $\\rho$ & $\\sigma$  %s \\\\", maplatex));
      System.out.println("\\hline");

      // DEBUG for significance
      String theRhos = "0.00";
      String theInterRhos = "";

      if (sortByRho) {
         for (Entry<String, Double> res : HashMapHelpers.sortHashMapByValueDescending(resultsAveraged)) {
            System.out.println(res.getKey());
            theRhos += String.format(" %.4f", res.getValue());
            theInterRhos += String.format(" %.4f", res.getValue());
         }
      } else {
         for (Entry<String, Double> res : resultsAveraged.entrySet()) {
            System.out.println(res.getKey());
            theRhos += String.format(" %.4f", res.getValue());
            theInterRhos += String.format(" %.4f", res.getValue());
         }

      }

      System.out.println("\\end{tabular}");



      // Correlations of systems with each other

      if (filterAllCommon & intercorrelations ) {

         System.out.println("\n-----------------\n");
         theInterRhos += "\n";

         int i=0;
         int len=6;
         String[] str = new String[rankingsSystem.size() / len + 1];


         for (int k=0;k<str.length; k++)
            str[k] = "";

         //for (Pair<String, Ranking> rankingSystem2 : rankingsSystem) {
         for (String systemName : rankingsSystem.keySet()) {
            str[i/len] += String.format(" & %s", systemName) ;
            i++;
         }

         for (int k=0;k<str.length; k++)
            str[k] += " \\\\ \n \\hline \n";


         // TODO:
         // I calculate spearman here for only one random ranking,
         // should iterate over all and average
         int o=0;
         for (String systemName1 : rankingsSystem.keySet()) {
            for (int k=0;k<str.length; k++)
               str[k] += systemName1;

            i=0;
            Ranking rankingSystem1 = rankingsSystem.get(systemName1).get(0);
            int f=0;
            for (String systemName2 : rankingsSystem.keySet()) {
               Ranking rankingSystem2 = rankingsSystem.get(systemName2).get(0);
               double spearman = RankedListScorer.getSpearman(rankingSystem1, rankingSystem2);
               if (spearman > 0.8)
                  str[i/len] += String.format(" & \\bf %.02f", spearman) ;
               else if (spearman < 0.3)
                  str[i/len] += String.format(" & \\it %.02f", spearman) ;
               else
                  str[i/len] += String.format(" & %.02f", spearman) ;

               if (f>o)
                  theInterRhos += String.format(" %.04f",spearman);

               i++;
               f++;
            }
            theInterRhos += "\n";
            o++;

            for (int k=0;k<str.length; k++)
               str[k] += " \\\\ \n";
         }


         for (String str2 : str) {
            System.out.println("\\begin{tabular}{l|ccccc}");
            System.out.println(str2);
            System.out.println("\\end{tabular}\n\n");

         }

      }

      System.out.println("RHOS=("+ theRhos + ")");
      System.out.println("PARIWISERHOS=(" + theInterRhos + ")");


   }






   /**
    * Delete all elements that are only in one of the rankings.
    */
   private static int filterCommon (Ranking rankingGold, Ranking rankingSystem) {

      List<RankingElement> toDeleteGold = new ArrayList<RankingElement>();
      List<RankingElement> toDeleteSystem = new ArrayList<RankingElement>();

      for (RankingElement gold : rankingGold) {
         if (!rankingSystem.contains(gold))
            toDeleteGold.add(gold);
      }

      for (RankingElement sys : rankingSystem) {
         if (!rankingGold.contains(sys))
            toDeleteSystem.add(sys);
      }

      for (RankingElement gold : toDeleteGold) {
         rankingGold.remove(gold);
      }

      for (RankingElement sys : toDeleteSystem) {
         rankingSystem.remove(sys);
      }

      if (toDeleteGold.size() != 0) {
         rankingGold.reassignRanks();
      }

      if (toDeleteSystem.size() != 0) {
         rankingSystem.reassignRanks();
      }

      return rankingGold.size();

   }



   /**
    * Compare two rankings.
    */
   private ComparisonResult compare(Ranking rankingGold, String nameGold,  Ranking rankingSystem, String nameSystem) {


      if (rankingGold == null | rankingSystem == null) {
         System.err.println("ERROR, one of the systems is zero: " + nameGold + " " + nameSystem);
         return null;
      }

      if (rankingGold.size() != rankingSystem.size()) {
         System.err.println("ERROR, length is not the same: " + nameGold + "(" + rankingGold.size() + "), " + nameSystem + "(" + rankingSystem.size() + "), " );
         return null;
      }

      ComparisonResult result = new ComparisonResult();

      result.spearman = RankedListScorer.getSpearman(rankingGold, rankingSystem);


      for (int index=0; index<fixedKs.length; index++) {
         int fixedK = fixedKs[index];

         if (rankingGold.size() < fixedK)
            continue;

         List<Pair<Double, Double>> prlist = RankedListScorer.getPRatFixedK(rankingGold, rankingSystem, fixedK);

         double map = RankedListScorer.getMAP(prlist);

         result.map[index] = map;
      }


      return result;

   }



   static class ComparisonResult {

      static final int arraySize = fixedKs.length;

      double spearman;
      double[] map;
      double[] pat10;
      double[] rat10;
      double[] pat20;
      double[] rat20;

      public ComparisonResult () {
         map = new double[arraySize];
         pat10 = new double[arraySize];
         rat10 = new double[arraySize];
         pat20 = new double[arraySize];
         rat20 = new double[arraySize];
      }
   }





}
