// (c) Wiltrud Kessler
// 8.04.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/





package de.uni_stuttgart.ims.compranking.rank.jfsa;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_stuttgart.ims.compranking.rank.AspectFilter;
import de.uni_stuttgart.ims.compranking.rank.AspectFilter.NormAspect;
import de.uni_stuttgart.ims.compranking.rank.ProductOpinionCounterPosNeg;
import de.uni_stuttgart.ims.compranking.rank.ProductOpinionCounterPosNeg.PolarityMode;
import de.uni_stuttgart.ims.compranking.rank.ProductMapper;
import de.uni_stuttgart.ims.nlpbase.tools.Tokenizer;
import de.uni_stuttgart.ims.nlpbase.tools.TokenizerStanford;
import de.uni_stuttgart.ims.util.Fileutils;



/**
 * Ranks products by the ranking depending on the analysis of JFSA.
 *
 * @author kesslewd
 *
 */
public class JFSARanker {


   /**
    * Create aspect-specific rankings or just take all
    */
   static boolean useAspects = true;


   /**
    * Ranks products by the ranking depending on the analysis of JFSA.
    *
    * Usage: JFSARanker <output filename>  <input file folder (JFSA output format)> <product relation files>*
    *
    * @param args
    */
   public static void main(String[] args) {

      // files that have relations between product id and review id,
      // i.e., which review talks about which product
      String[] productRelationFiles = null;
      String outFilename = null;
      String inFolder = null;
      String relevantProductsFile = null;
      String aspectsFile = null;

      PolarityMode[] usePolarityMode;
      boolean[] useNormalization;

      // Test normalization, counting stuff
      //usePolarityMode = new PolarityMode[] { PolarityMode.both, PolarityMode.onlyPos, PolarityMode.agnostic };
      //useNormalization = new boolean[] {true, false};


      // Test aspects, set to best working setting else
      usePolarityMode = new PolarityMode[] {PolarityMode.both};
      useNormalization = new boolean[] {false};


      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 3) {
            System.err.println("Usage: JFSARanker <output filename>  <input file folder (JFSA output format)> <product relation files>*");
            System.exit(1);
         } else {
            outFilename = args[0];
            inFolder = args[1];
            relevantProductsFile = args[2];
            aspectsFile = args[3];
            productRelationFiles = Arrays.copyOfRange(args, 4, args.length);

         }


      } catch (Exception e) {
         System.err.println("ERROR !!! while parsing options: " + e.getMessage());
         System.exit(1);
      }

      File myfolder = new File(inFolder);
      File[] files = myfolder.listFiles(new FileFilter() {
          public boolean accept(File file) {
              return file.isFile() && file.getName().endsWith(".csv");
          }
      });

      System.out.println("Print output to file: " + outFilename);
      System.out.println("Take input files from folder: " + inFolder + " (" + files.length + " files)");
      System.out.println("Take relevant products from file: " + relevantProductsFile);
      System.out.println("Take aspects from file: " + aspectsFile);




      // ===== LOAD STUFF =====

      AspectFilter af = null;
      try {
         if (useAspects)
            af = new AspectFilter(aspectsFile);
         else
            af = new AspectFilter();
      } catch (Exception e) {
         System.err.println("ERROR !!! while creating aspect filter: " + e.getMessage());
         af = new AspectFilter();
      }

      if (useAspects) {
         System.out.println("Aspects:");
         af.printAspectLists();
      }


      Set<NormAspect> allAspects = new HashSet<NormAspect>();
      allAspects.add(AspectFilter.topLevelAspect);
      if (useAspects)
         allAspects = af.getAllNormAspects();

      ProductMapper prodmapper = new ProductMapper();

      try {
         prodmapper.readRelevantProducts(relevantProductsFile);

      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }


      for (String csvFile : productRelationFiles) {
         System.out.println("read relations from file: " + csvFile);
         try {
            prodmapper.readRelations(csvFile);
         } catch (FileNotFoundException e) {
            e.printStackTrace();
         }
      }






      // ===== PROCESS =====




      JFSARanker blubb = new JFSARanker(af, prodmapper);
      for (PolarityMode usePolarityMode2 : usePolarityMode) {
         for (NormAspect useAspect2 : allAspects) {
            for (boolean useNormalization2 : useNormalization) {

               blubb.reset(usePolarityMode2, useAspect2, useNormalization2);

               System.out.println("Polarity mode: " + usePolarityMode2 + ", use aspect: " + useAspect2 + ", use normalization: " + useNormalization2);
               for (File csvFile : files) {
                  blubb.analyze(csvFile, csvFile.getAbsolutePath().replace(".csv", ".txt"));
               }

               String f = (useNormalization2)?"Norm":"NN";
               blubb.writeToFile(outFilename.substring(0, outFilename.lastIndexOf(".")) + "_" + useAspect2.toString().replace(" ","") + "_" + usePolarityMode2  + f + outFilename.substring(outFilename.lastIndexOf("."), outFilename.length()));
               blubb.endIt();

               //System.exit(1);
            }
         }
      }

      System.out.println("done!");


   }




   private ProductMapper prodmapper;
   private AspectFilter aspectFilter;

   private ProductOpinionCounterPosNeg counter;

   private int alllineno = 0;
   private int subjno = 0;
   private int subjPosno = 0;
   private int subjNegno = 0;
   private int aspectsno = 0;
   private int sentsNull = 0;

   private boolean addAsNeutral  = true;


   /**
    * Ranks products by the ranking depending on the analysis of JFSA.
    *
    * @param aspectFilter Aspect-specific analysis.
    * @param prodmapper Map reviews to products.
    */
   public JFSARanker (AspectFilter aspectFilter, ProductMapper prodmapper)  {
      this.prodmapper = prodmapper;
      this.aspectFilter = aspectFilter;
   }


   /**
    * Ranks products by the ranking depending on the analysis of JFSA.
    *
    * @param aspectFilter Aspect-specific analysis.
    * @param prodmapper Map reviews to products.
    * @param usePolarityMode Counting of negative phrases.
    * @param useNormAspect Which aspect to consider.
    * @param useNormalization Normalization of scores.
    */
   public JFSARanker (AspectFilter aspectFilter, ProductMapper prodmapper, PolarityMode usePolarityMode, NormAspect useNormAspect, boolean useNormalization)  {
      this(aspectFilter, prodmapper);
      reset(usePolarityMode, useNormAspect, useNormalization);
   }


   /**
    * Reset the counter (for use several times).
    *
    * @param usePolarityMode
    * @param useNormAspect
    * @param useNormalization
    */
   public void reset(PolarityMode usePolarityMode, NormAspect useNormAspect, boolean useNormalization) {

      counter = new ProductOpinionCounterPosNeg(usePolarityMode, useNormalization);

      aspectFilter.setRelevantAspect(useNormAspect);

      alllineno = 0;
      subjno = 0;
      subjPosno = 0;
      subjNegno = 0;
      aspectsno = 0;
      sentsNull = 0;


      if (addAsNeutral) {
         for (String productID : prodmapper.getRelevantProducts()) {
            counter.addNeutral(productID);
         }
      }

   }






   /**
    * Analyze this file
    *
    * @param filename
    * @param filenameAllText
    */
   private void analyze (File filename, String filenameAllText) {


      // ===== READ REVIEWS =====

      HashMap<String, Integer> reviewLengths = new HashMap<String, Integer>();

      try {

         BufferedReader brReviews = Fileutils.getReadFile(filenameAllText);

         Tokenizer tokenizer = new TokenizerStanford();
         String nextLine;
         while ((nextLine = brReviews.readLine()) != null) {
            String[] parts = nextLine.split("\t");
            if (parts.length >= 3) {
               String[] spans = tokenizer.tokenize(parts[2]);
               reviewLengths.put(parts[0], spans.length);
            }
         }
         Fileutils.closeSilently(tokenizer);
         Fileutils.closeSilently(brReviews);

      } catch (Exception e) {
         System.err.println("ERROR in initialization: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }



      // ===== INITIALIZATION =====

      BufferedReader brAnnotations = null;

      try {

         // open input file with annotations
         //System.out.println("annotations input file: " + filename);
         brAnnotations = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(filename)), Charset.forName("UTF-8")));

      } catch (Exception e) {
         System.err.println("ERROR in initialization: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }

      // ===== PROCESSING =====



      int lineno = 0;
      String nextLine = null;
      String lastReviewID = null;
      List<JFSAAnnotation> sentiments = new ArrayList<JFSAAnnotation>();
      List<JFSAAnnotation> aspects = new ArrayList<JFSAAnnotation>();
      try {
         while ((nextLine = brAnnotations.readLine()) != null) {

            if (nextLine.isEmpty())
               continue;

            String[] parts = nextLine.split("\t");
            lineno++;

            if (parts.length != 8) {
               System.out.println("ERROR in file " + filename + ", line " + lineno + " has format error: " + nextLine);
               continue;
            }


            alllineno++;
            String reviewID = parts[1];


            // if we don't know which product this review is about
            // -> skip
            if (prodmapper.getProductIdForReview(reviewID) == null)
               continue;


            // Check if we are still inside the same review.
            // If yes - only collect annotations (see below)
            // If no - process annotations
            if (!reviewID.equals(lastReviewID)) { // new review - process

               if (lastReviewID != null)
                  processAnnotations(lastReviewID, sentiments, aspects, reviewLengths.get(lastReviewID));

               // reset
               lastReviewID = reviewID;
               sentiments = new ArrayList<JFSAAnnotation>();
               aspects = new ArrayList<JFSAAnnotation>();

            }

            // Collect annotations
            // Format:
            // target all-64   502   504   it DUMMID   Negative DUMMYRELATED
            // subj  all-64   174   178   good  DUMMID   Positive DUMMYRELATED
            int startIndex = Integer.parseInt(parts[2]);
            int endIndex = Integer.parseInt(parts[3]);
            String phrase = parts[4];
            if (parts[0].equalsIgnoreCase("subj")) { // subj = sentiment phrase
               subjno++;
               sentiments.add(new JFSAAnnotation(startIndex, endIndex, phrase, parts[6]));
            } else if (parts[0].equalsIgnoreCase("target")) { // target = aspect
               aspectsno++;
               aspects.add(new JFSAAnnotation(startIndex, endIndex, phrase));
            }



         }

         // Write the last review
         // (check against empty file or no relevant review found!)
         if (lastReviewID != null)
            processAnnotations(lastReviewID, sentiments, aspects, reviewLengths.get(lastReviewID));


      } catch (Exception e) {
         System.out.println("ERROR in line " + lineno + " : " + nextLine);
         e.printStackTrace();
         System.exit(1);
      }


      Fileutils.closeSilently(brAnnotations);

      //System.out.println("Went through " + lineno + " lines with annotations.");

   }



   /**
    * Process the JFSA annotations in a review.
    *
    * @param reviewID
    * @param sentiments
    * @param aspects
    * @param reviewLength
    */
   public void processAnnotations (String reviewID, List<JFSAAnnotation> sentiments, List<JFSAAnnotation> aspects, int reviewLength) {

      // Skip uninitialized calls at beginning of processing
      if (reviewID == null)
         return;

      String productID = prodmapper.getProductIdForReview(reviewID);

      // Skip products we are not interested in
      if (productID == null)
         return;

      // Add the length of the review
      counter.addReviewWithLength(productID, reviewID, reviewLength);

      //System.out.println(reviewID + "\t"  + productID  + "\t"  + reviewLength + "\t" + sentiments.size() + "\t" + aspects.size());

      // Skip further processing if there are no sentiment expressions found
      if (sentiments == null | sentiments.isEmpty()) {
         sentsNull++;
         return;
      }

      //System.out.println("sentiments: " + sentiments );
      //System.out.println("aspects: " + aspects );

      if (aspects == null || aspects.isEmpty()) {// Add sentiment without aspect

         for (JFSAAnnotation sentann : sentiments) {

            // add sentiment
            if (sentann.sentiment.equalsIgnoreCase("negative")) {
               subjNegno++;
               counter.addNeg(productID, reviewID);
            } else {
               subjPosno++;
               counter.addPos(productID, reviewID);
            }
         }


      } else { // Add sentiment with closest aspect

         for (JFSAAnnotation sentann : sentiments) {

            // find closest aspect
            JFSAAnnotation aspect = null;
            int minDist = Integer.MAX_VALUE;
            for (JFSAAnnotation aspann : aspects) {
               int thisMinDist = Math.min(Math.abs(sentann.startIndex - aspann.endIndex), Math.abs(sentann.endIndex - aspann.startIndex));
               if (thisMinDist < minDist) {
                  minDist = thisMinDist;
                  aspect = aspann;
               }
            }

            // add sentiment
            if (sentann.sentiment.equalsIgnoreCase("negative")) {
               subjNegno++;
               if (!useAspects | aspectFilter.isRelevantAspectphrase(aspect.phrase))
                  counter.addNeg(productID, reviewID);
            } else {
               subjPosno++;
               if (!useAspects | aspectFilter.isRelevantAspectphrase(aspect.phrase))
                  counter.addPos(productID, reviewID);
            }
         }
      }
   }




   /**
    * Called at the end of processing, write ranking to file
    */
   public void writeToFile (String outputRankingFile) {
      counter.writeRankingToFile(outputRankingFile);
   }


   /**
    * Called at the end of the document, clean up, print some statistics.
    */
   public void endIt () {
      System.out.println("Went through " + alllineno + " lines of annotations (total).");
      System.out.println("Found " + subjno + " sentiment phrases, " + subjPosno + " positive, " + subjNegno + " negative.");
      System.out.println("Found " + sentsNull + " reviews where no sentiment is found.");
      System.out.println("Found " + aspectsno + " aspects.");
   }



}
