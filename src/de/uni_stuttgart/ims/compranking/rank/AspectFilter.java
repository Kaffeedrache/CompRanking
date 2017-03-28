// (c) Wiltrud Kessler
// 12.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/




package de.uni_stuttgart.ims.compranking.rank;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import de.uni_stuttgart.ims.util.Fileutils;



/**
 * Determine whether a found aspect phrase belongs to a top-level aspect.
 *
 * @author kesslewd
 *
 */
public class AspectFilter {

   public static class NormAspect {

      private String mystring;

      public NormAspect (String mystring) {
         this.mystring = mystring;
      }

      public String toString() {
         return mystring;
      }

   }


   /**
    * Determines when a phrase is considered to refer to a norm aspect.
    * If set to true, overlap with one the aspect terms is enough,
    * if set to false, there needs to be an exact match.
    */
   boolean useOverlap = true;


   /**
    * The special top-level aspect 'all aspects'
    */
   public static NormAspect topLevelAspect = new NormAspect("all");


   /**
    * The current relevant aspect
    */
   private NormAspect relevantAspect = topLevelAspect;



   /**
    * Mappings from aspect phrases to normalized aspects.
    */
   private Map<NormAspect, TreeSet<String>> aspectMappings = null;




   /**
    * Don't filter or aggregate, just consider everything.
    */
   public AspectFilter () {}

   /**
    * Filter and aggregate according to list of aspects in file.
    */
   public AspectFilter (String filename) throws FileNotFoundException {
      createAspectsMap(filename);
   }

   /**
    * Filter for one aspect and according to list of aspects in file.
    */
   public AspectFilter (String filename, String normaspect) throws FileNotFoundException {
      createAspectsMap(filename);
   }




   /**
    * Read list of aspects from file.
    *
    * @param filename
    * @throws FileNotFoundException
    */
   public void createAspectsMap (String filename) throws FileNotFoundException {

      DataInputStream fstream = new DataInputStream(new FileInputStream(filename));
      BufferedReader br = new BufferedReader(new InputStreamReader(fstream, Charset.forName("UTF-8")));

      aspectMappings = new HashMap<NormAspect, TreeSet<String>>();

      String nextLine = null;
      int lineno = 0;

      try {
         while ((nextLine = br.readLine()) != null) {
            lineno++;
            String[] parts = nextLine.split("\t");
            // normname \t other term \t other term
            TreeSet<String> listy = new TreeSet<String>();
            NormAspect normy = new NormAspect(parts[0]);
            aspectMappings.put(normy, listy);
            for (String term : parts) {
               listy.add(term);
            }
         }

         Fileutils.closeSilently(br);
      } catch (Exception e) {
         System.out.println("ERROR in line " + lineno + " : " + nextLine);
         e.printStackTrace();
      }
   }


   /**
    * Print list of aspects.
    *
    * @param filename
    * @throws FileNotFoundException
    */
   public void printAspectLists () {
      for (Entry<NormAspect, TreeSet<String>> entry : aspectMappings.entrySet()) {
         System.out.println(entry.getKey() + "\t" + entry.getValue().size()  + "\t" + entry.getValue());
      }
   }

   /**
    * Get list of all norm aspects.
    */
   public Set<NormAspect> getAllNormAspects () {
      if (aspectMappings != null)
         return aspectMappings.keySet();
      else {
         HashSet<NormAspect> a = new HashSet<NormAspect>();
         a.add(topLevelAspect);
         return a;
      }
   }

   /**
    * Set the aspect we are currently interested in.
    */
   public void setRelevantAspect(NormAspect aspect) {
      this.relevantAspect = aspect;
   }

   /**
    * Determine whether the given String is a possible
    * expression of the asepect we are looking for.
    *
    * @param aspect
    * @return
    */
   public boolean isRelevantAspectphrase(String phrase) {
      return hasMapping(this.relevantAspect, phrase);
   }

   /**
    * Determine whether the given Aspect is the one we are looking for.
    *
    * @param aspect
    * @return
    */
   public boolean isRelevantAspect(NormAspect aspect) {
      return (this.relevantAspect == topLevelAspect | this.relevantAspect == aspect);
   }


   /**
    * Determine whether the given String is a possible
    * expression of the given norm aspect.
    */
   public boolean hasMapping (NormAspect normalizedAspect, String phrase) {


      // If we want all aspects, always return true
      // (even in the case where no aspect is found)
      if (this.aspectMappings == null)
         return true;

      // Else we are checking for something specific, null not included
      if (phrase == null)
         return false;

      phrase = phrase.toLowerCase();

      TreeSet<String> listy = aspectMappings.get(normalizedAspect);
      if (listy != null) {
         if (this.useOverlap) {
            String[] aspectTokens = phrase.split(" ");
            for (String aspectToken : aspectTokens) {
               for (String aspectListWord : listy) {
               if (aspectListWord.equalsIgnoreCase(aspectToken))
                  return true;
               }
            }
            return false;
         } else // exact match
            return listy.contains(phrase);
      }
      else
         return false;
   }


   /**
    * Determine to which norm aspect[s] the string belongs (if any).
    *
    * If the phrase does not express any aspect, an empty list
    * is returned.
    *
    * @param aspect
    * @return
    */
   public List<NormAspect> getNormAspect (String phrase) {

      if (phrase == null)
         return null;

      List<NormAspect> result = new ArrayList<NormAspect>();

      // If we want all aspects, always return the top level aspect
      // (even in the case where no aspect is found)
      if (this.aspectMappings == null) {
         result.add(topLevelAspect);
         return result;

      }


      String myphrase = phrase.toLowerCase();

      for (NormAspect normAsp : this.getAllNormAspects()) {
         if (hasMapping(normAsp, myphrase)) {
            result.add(normAsp);
         }
      }

      return result;
   }



}
