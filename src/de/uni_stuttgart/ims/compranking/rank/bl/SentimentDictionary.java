// (c) Wiltrud Kessler
// 06.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/



package de.uni_stuttgart.ims.compranking.rank.bl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import de.uni_stuttgart.ims.util.Fileutils;



public class SentimentDictionary {

   TreeSet<String> positiveDictionary ;

   TreeSet<String> negativeDictionary ;

   public enum DictionaryType { mpqa, gi };

   public enum Polarity { positive, negative, neutral;

   public static Polarity getPolarity(String string) {
      if (string.equals("negative"))
         return Polarity.negative;
      if (string.equals("positive"))
         return Polarity.positive;
      if (string.equals("neutral"))
         return Polarity.neutral;
      return null;
   } };

   public static String wwhFilename = "subjclueslen1-HLTEMNLP05.tff";

   public static String giFilename = "gi_dictionary";


   public SentimentDictionary(DictionaryType dictionaryType) {
      int count = 0;
      if (dictionaryType == DictionaryType.mpqa)
         count = readWWH();
      else
         count = readGI();

      System.out.println("Read " + count + " entries.");
      System.out.println("Dictionary " + dictionaryType + ": "
               + (positiveDictionary.size()+ negativeDictionary.size()) + " terms (" + positiveDictionary.size() + " pos, " + negativeDictionary.size() + " neg).");


//      int a = 0;
//      for (String word : positiveDictionary) {
//         System.out.println("+ " + word);
//         a++;
//         if (a > 15) break;
//      }
//      int b = 0;
//      for (String word : negativeDictionary) {
//         System.out.println("- " + word);
//         b++;
//         if (b > 15) break;
//      }


   }


   public boolean isPositiveWord(String word) {
      return positiveDictionary.contains(word.toLowerCase());
   }


   public boolean isNegative(String word) {
      return negativeDictionary.contains(word.toLowerCase());
   }


   private int readWWH () {

//      Insert all words from the file into the dictionary with sentiment "label",
//      key is the word, value is 0 in all cases (useful only to check if a word
//      is in the dictionary or not).
//      Expected format: type=weaksubj len=1 word1=abandoned pos1=adj stemmed1=n priorpolarity=negative

      positiveDictionary = new TreeSet<String>();
      negativeDictionary = new TreeSet<String>();
      int i=0;

      try {
         BufferedReader br = Fileutils.getReadFile(wwhFilename);

         String strLine;
         while ((strLine = br.readLine()) != null) {
            String[] parts = strLine.split(" ");
            String word = null;
            Polarity pol = Polarity.neutral;

            for (String part : parts) {
               String[] components = part.split("=");

               if (components[0].equals("word1"))
                  word = components[1].toLowerCase();
               else if (components[0].equals("priorpolarity"))
                  pol = Polarity.getPolarity(components[1]);

            }

            if (word != null) {
               if (pol == Polarity.negative)
                  negativeDictionary.add(word);
               if (pol == Polarity.positive)
                  positiveDictionary.add(word);
            }

            i++;
            //if (i>10) break;


         }


      } catch (IOException e) {
         e.printStackTrace();
      }

      return i;

   }

   // See http://www.wjh.harvard.edu/~inquirer/homecat.htm for categories.
   // Categories as written in paper Choi & Cardie 2008
   // Write in lowercase.
   String[] categoriesPositiveArray = new String[] {"pos", "pstv", "posaff", "pleasur", "virtue", "increas"};
   String[] categoriesNegativeArray = new String[] {"negativ", "ngtv", "negaff" , "pain", "vice", "hostile" , "fail" , "enlloss" , "wlbloss" , "tran-loss"};


   private int readGI () {

//            Insert all words from the file into the dictionary with sentiment "label",
//            key is the word, value is the part of speech.
//            Expected format:
//            ABOUT#5 H4Lvd Handels  | 8% idiom-verb: ""Bring (brought) about""--handled by ""bring""
//            Word#Sense Categories* | Explanation
//            Words are normalized to lowercase.
//
//            @param dictionaryFile The file from which the dictionary will be read
//            @param label The label the words should have (only words with this label
//                  will be inserted into the dictionary)
//                  should be either utils.negativeLabel or utils.positiveLabel


      positiveDictionary = new TreeSet<String>();
      negativeDictionary = new TreeSet<String>();

      List<String> categoriesPositive = Arrays.asList(categoriesPositiveArray);
      List<String> categoriesNegative = Arrays.asList(categoriesNegativeArray);


      boolean firstLine = true;
      int i=0;

      try {
         BufferedReader br = Fileutils.getReadFile(giFilename);

         String strLine;
         while ((strLine = br.readLine()) != null) {

            // Skip the first line (explanation of format)
            if (firstLine) {
               firstLine = false;
               continue;
            }

            // Split into before and after description
            String[] parts = strLine.split("\\|");
            //System.out.println(strLine);

            // Split into words and categories
            String[] categories = parts[0].split(" ");
            //System.out.println(Arrays.toString(categories));

            // Get word
            String word = categories[0].toLowerCase();

            // Remove senses
            // Senses are separated by a # and a number,
            // e.g. happy#2
            // Split on #, set word to be only the first part
            String[] wordsplit = word.split("#");
            if (wordsplit.length>1)
               word = wordsplit[0];
            //System.out.println(word);

             // Check for categories
             // categories[0] is the word, so start with [1]
            boolean isPos = false;
            boolean isNeg = false;
            for (String category : categories) {

                // Check if the word is in one of the categories we want,
                if (categoriesPositive.contains(category.toLowerCase()))
                   isPos = true;
                if (categoriesNegative.contains(category.toLowerCase()))
                   isNeg = true;
            }
            ///System.out.println("Category: " + word + " " + isPos + " " + isNeg);


            if (isNeg)
               negativeDictionary.add(word);
            if (isPos)
               positiveDictionary.add(word);




            i++;
            //if (i>10) break;


         }


      } catch (IOException e) {
         e.printStackTrace();
      }

      return i;


   }



}
