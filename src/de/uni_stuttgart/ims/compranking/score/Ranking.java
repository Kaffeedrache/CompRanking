// (c) Wiltrud Kessler
// 02.06.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.compranking.score;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.uni_stuttgart.ims.util.Fileutils;

/**
 * A ranking of products by score.
 *
 * @author kesslewd
 *
 */
public class Ranking implements Iterable<RankingElement> {

   /**
    * The ranking
    */
   List<RankingElement> ranking = new ArrayList<RankingElement>();

   /**
    * Number of non-zero entries.
    */
   int nonZeroEntries = 0;

   /**
    * Allow several elements with the same rank?
    */
   boolean considerTies;

   /**
    * Shuffle elements with a score of zero?
    */
   boolean shuffleZeroTies;

   /**
    * Name of this ranking.
    */
   String name;

   /**
    * Maximum score of any element in the ranking.
    */
   double maximumScore = 0;

   /**
    * Minimum score of any element in the ranking.
    */
   double minimumScore = 0;


   /**
    * Create a ranking from another ranking.
    *
    * @param otherRanking
    */
   public Ranking(Ranking otherRanking) {
      for (RankingElement e : otherRanking) {
         this.ranking.add(e);
      }
   }

   /**
    * Read a ranking from the file.
    *
    * @param file
    * @param considerTies
    * @param shuffleZeroTies
    */
   public Ranking(String filename, boolean considerTies, boolean shuffleZeroTies) {
      this(new File(filename), considerTies, shuffleZeroTies);
   }


   /**
    * Read a ranking from the file.
    *
    * @param file
    * @param considerTies
    * @param shuffleZeroTies
    */
   public Ranking(File file, boolean considerTies, boolean shuffleZeroTies) {


      this.considerTies = considerTies;
      this.shuffleZeroTies = shuffleZeroTies;

      BufferedReader br = null;

       try {
          br = Fileutils.getReadFile(file);
       } catch (IOException e) {
          e.printStackTrace();
          return;
       }

      //Identical values (rank ties or value duplicates) are assigned a rank equal to the average of their positions in the ascending order of the values.


      int lineno = 0;
      String nextLine = null;

      Double lastScore = null;

      List<RankingElement> tmpSameRank = new ArrayList<RankingElement>();

      try {
         while ((nextLine = br.readLine()) != null) {
            lineno++;

            String[] parts = nextLine.split("\t");
            String id = parts[0];
            String thisScore = parts[1];
            Double scoreNumeric = Double.parseDouble(thisScore);
            String comment = "";
            for (int j=2; j<parts.length; j++) {
               comment += " " + parts[j];
            }

            // Create the new element
            // (the rank is the lineno, this must be overwritten later!)
            // don't put it into the ranking yet
            RankingElement el = new RankingElement(id, lineno, scoreNumeric, comment);

            if (scoreNumeric != 0)
               nonZeroEntries++;

            if (scoreNumeric > maximumScore)
               maximumScore = scoreNumeric;

            if (scoreNumeric < minimumScore)
               minimumScore = scoreNumeric;



            if (considerTies) {
               if (lastScore == null || Double.compare(scoreNumeric, lastScore)==0) {
                  tmpSameRank.add(el);
               }
               else {
                  add(lineno, tmpSameRank, lastScore, true);
                  tmpSameRank = new ArrayList<RankingElement>();
                  tmpSameRank.add(el);
               }
               lastScore = scoreNumeric;
            }
            else {
               ranking.add(el);
            }


         }

         if (considerTies) {
            add(lineno, tmpSameRank, lastScore, true);
         }
      } catch (IOException e) {
         System.err.println("ERROR in line " + lineno + ": " + nextLine);
         e.printStackTrace();
      }
      Fileutils.closeSilently(br);

   }


   /**
    * Add an element to the ranking.
    *
    * @param lineno
    * @param tmpSameRank
    * @param score
    * @param doAdd set to false if the elemnet already exists and should only be updated
    */
   private void add (int lineno, List<RankingElement> tmpSameRank, double score, boolean doAdd) {


      if (shuffleZeroTies & (Double.compare(score,0.0) == 0)) {

         // Shuffle all zero-entries
         Collections.shuffle(tmpSameRank);
         // Add
         int thisRank = lineno-1-tmpSameRank.size();
         for (RankingElement id2 : tmpSameRank) {
            id2.rank = thisRank;
            if (doAdd)
               ranking.add(id2);
            thisRank++;
         }


      } else {

         // treat ties normally

         double thisRank;
         if (tmpSameRank.size() == 1)
            thisRank = lineno-1;
         else
            thisRank = (double) (lineno-1 + lineno-tmpSameRank.size()) / 2;

         for (RankingElement id2 : tmpSameRank) {
            id2.rank = thisRank;
            if (doAdd)
               ranking.add(id2);
         }

      }

   }




   /**
    * Reassign ranks after removing products.
    */
   public void reassignRanks() {

      nonZeroEntries = 0;
      Double lastScore = null;

      List<RankingElement> tmpSameRank = new ArrayList<RankingElement>();

      int i=1;
      for (RankingElement el : ranking) {

         if (Double.compare(el.score, 0.0)!=0)
            nonZeroEntries++;

         if (considerTies & (Double.compare(el.score, 0.0)!=0)) {
            if (lastScore == null || Double.compare(el.score, lastScore)==0) {
                  tmpSameRank.add(el);
            }
            else {
               add(i, tmpSameRank, lastScore, false);
               tmpSameRank = new ArrayList<RankingElement>();
               tmpSameRank.add(el);
            }
         }
         else {
            el.rank = i;
         }

         lastScore = el.score;
         i++;
      }


      if (considerTies) {
         add(i, tmpSameRank, lastScore, false);
      }

   }



   /**
    * Checks if an element with this name is contained in this ranking.
    */
   public boolean contains (String content) {
      return this.getElement(content) != null;
   }

   /**
    * Checks if the element is contained in this ranking.
    */
   public boolean contains (RankingElement element) {
      return ranking.contains(element);
   }

   /**
    * Iterate through the ranking.
    */
   @Override
   public Iterator<RankingElement> iterator() {
      return ranking.iterator();
   }

   /**
    * Number of products in the ranking.
    */
   public int size() {
      return ranking.size();
   }

   /**
    * Removes the element from the ranking.
    */
   public void remove(RankingElement element) {
      ranking.remove(element);
   }

   /**
    * Returns the rank of an element with this name or 0.
    */
   public double getRank(String thingy) {
      for (RankingElement el : ranking) {
         if (el.content.equals(thingy))
            return el.rank;
      }
      return 0;
   }

   /**
    * Get the element with this name.
    */
   public RankingElement getElement(String thingy) {
      for (RankingElement el : ranking) {
         if (el.content.equals(thingy))
            return el;
      }
      return null;
   }

   /**
    * Get the element with this rank.
    */
   public RankingElement get(int index) {
      return ranking.get(index);
   }

   /**
    * Get the minimum score of all products in this ranking.
    */
   public double getMinimumScore() {
      return minimumScore;
   }
   /**
    * Get the maximum score of all products in this ranking.
    */
   public double getMaximumScore() {
      return maximumScore;
   }



   /**
    * Print the ranking to the file.
    *
    * @param filename
    */
   public void printToFile (String filename) {

      try {
         BufferedWriter writer = Fileutils.getWriteFile(filename);

         for (RankingElement thingy : ranking) {
            writer.write(thingy + "\n");
         }

         writer.flush();
         writer.close();

      } catch (IOException e) {
          e.printStackTrace();
      }

   }




}
