// (c) Wiltrud Kessler
// 18.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/





package de.uni_stuttgart.ims.compranking.rank.csrl;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_stuttgart.ims.nlpbase.nlp.ArgumentType;
import de.uni_stuttgart.ims.compranking.rank.AspectFilter;
import de.uni_stuttgart.ims.compranking.rank.AspectFilter.NormAspect;
import de.uni_stuttgart.ims.compranking.rank.ProductMapper;
import de.uni_stuttgart.ims.compranking.rank.ProductOpinionCounterPosNeg;
import de.uni_stuttgart.ims.compranking.rank.ProductOpinionCounterPosNeg.PolarityMode;
import de.uni_stuttgart.ims.nlpbase.io.ParseReaderCoNLL;
import de.uni_stuttgart.ims.nlpbase.nlp.POSUtils;
import de.uni_stuttgart.ims.nlpbase.nlp.SRLSentence;
import de.uni_stuttgart.ims.nlpbase.nlp.Word;
import de.uni_stuttgart.ims.util.Fileutils;
import de.uni_stuttgart.ims.util.SubTreeFinder;



/**
 * Ranks products by the ranking depending on the analysis of CSRL.
 *
 * @author kesslewd
 *
 */
public class CSRLRanker {



   /**
    * Use only reviews that were written for a product
    * that we want to create a ranking for.
    */
   static boolean useOnlyRelevantProductReviews = true; // PAPER: use true

   /**
    * Create aspect-specific rankings or just take all
    */
   static boolean useAspects = true;


   /**
    *
    * Ranks products by the ranking depending on the analysis of CSRL.
    *
    * Usage: CSRLRanker <output filename>  <input file folder (SRL annotated CoNLL output format)>  <relevant prducts> <product relation files>*
    *
    * @param args
    */
   public static void main(String[] args) {


      // ===== GET USER-DEFINED OPTIONS =====

      String outFilename = null;
      String inFolder = null;
      String productNameFile = null;
      String relevantProductsFile = null;
      String aspectsFile = null;


      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 4) {
            System.err.println("Usage: CSRLRanker <output filename>  <input file folder (SRL annotated CoNLL output format)>  <relevant prducts> <product relation files>*");
            System.exit(1);
         } else {

            outFilename = args[0];
            inFolder = args[1];
            relevantProductsFile = args[2];
            aspectsFile = args[3];
            productNameFile = args[4];
         }

      } catch (Exception e) {
         System.err.println("ERROR !!! while parsing options: " + e.getMessage());
         System.exit(1);
      }


      File myfolder = new File(inFolder);
      File[] files = myfolder.listFiles(new FileFilter() {
          public boolean accept(File file) {
              return file.isFile() && file.getName().endsWith(".out");
          }
      });

      System.out.println("Print output to file: " + outFilename);
      System.out.println("Take input files from folder: " + inFolder + " (" + files.length + " files)");
      System.out.println("Take relevant products from file: " + relevantProductsFile);
      System.out.println("Take aspect from file: " + aspectsFile);




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

      System.out.println("Aspects:");
      af.printAspectLists();



      ProductMapper prodmapper = new ProductMapper();

      try {
         prodmapper.readRelevantProducts(relevantProductsFile);

      } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }


      try {
         prodmapper.readNames(productNameFile);
      } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      //prodmapper.printProductNames();




      // ===== PROCESS =====

      CSRLRanker blubb = new CSRLRanker(af, prodmapper);

      Set<NormAspect> allAspects = new HashSet<NormAspect>();
      allAspects.add(AspectFilter.topLevelAspect);
      if (useAspects)
         allAspects = af.getAllNormAspects();



      int i=0;
      for (NormAspect useAspect2 : allAspects) {
         System.out.println(useAspect2);
         i++;
         blubb.reset(useAspect2);
         for (File csvFile : files) {
           System.out.println("Process file: " + csvFile.getName() + " (" + i + " of " + files.length + ")");
            blubb.analyzeFile(csvFile.getAbsolutePath(), csvFile.getAbsolutePath().replace(".sentences.parsed.txt.out", ".sentencesID.csv"));
         }


         blubb.writeToFile(outFilename.substring(0, outFilename.lastIndexOf(".")) + "_" + useAspect2 + outFilename.substring(outFilename.lastIndexOf("."), outFilename.length()));


         blubb.endIt();


      }

      System.out.println("done.");


   }



   private ProductMapper prodmapper;
   private AspectFilter aspectFilter;


   private ProductOpinionCounterPosNeg counter;

   private int alllineno = 0;
   private int compno = 0;
   private int entityno = 0;
   private int aspectsno = 0;
   private int aspectsmapped = 0;

   private int entityMapped = 0;


   private boolean addAsNeutral = true;




   /**
    * Ranks products by the ranking depending on the analysis of CSRL.
    *
    * @param aspectFilter Aspect-specific analysis.
    * @param prodmapper Map product names to product IDs.
    */
   public CSRLRanker (AspectFilter aspectFilter, ProductMapper prodmapper)  {
      this.prodmapper = prodmapper;
      this.aspectFilter = aspectFilter;
   }


   /**
    * Reset the counter (for use several times).
    *
    * @param useNormalization
    */
   public void reset(NormAspect useNormAspect) {

      // Normalization = false
      counter = new ProductOpinionCounterPosNeg(PolarityMode.both, false);

      aspectFilter.setRelevantAspect(useNormAspect);

      compno = 0;
      entityno = 0;
      aspectsno = 0;
      alllineno = 0;
      entityMapped = 0;
      aspectsmapped = 0;



      if (addAsNeutral) {
         for (String productID : prodmapper.getRelevantProducts()) {
            counter.addNeutral(productID);
         }
      }
   }






   /**
    * Analyze this file
    *
    * @param filenameParsed
    * @param filenameCSV
    */
   public void analyzeFile (String filenameParsed, String filenameCSV) {


      // ===== INITIALIZATION =====

      BufferedReader brAnnotations = null;

      ParseReaderCoNLL parseReader = null;

      try {

         // open input file with csv info about product and review
         //System.out.println("annotations input file: " + filename);
        brAnnotations = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(filenameCSV)), Charset.forName("UTF-8")));


         // Open input parsed sentences file
         parseReader = new ParseReaderCoNLL(filenameParsed);
         parseReader.openFile();
         //System.out.println("Analyze : " + filename);

      } catch (Exception e) {
         System.err.println("ERROR in initialization: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }


      // ===== PROCESSING =====

      int tmp = filenameParsed.lastIndexOf('/');
      String prefix = filenameParsed.substring(Math.max(0, tmp+1), Math.max(0, filenameParsed.indexOf('.', tmp))) + "-";


      int lineno = 0;
      SRLSentence sentence;
      try {

         while (!(sentence = parseReader.readParseSRL()).isEmpty()) {

            String annotation = brAnnotations.readLine();


            // Format:
            // product id \t review id \t sentence id \t sentence
            String[] parts = annotation.split("\t");
            String productID = parts[0];
            if (useOnlyRelevantProductReviews && !prodmapper.isRelevantProduct(productID)) {
               continue;
            }


            lineno++;
            alllineno++;

            analyzeSentence(prefix, lineno, sentence, productID);


            //if (compno > 100) break; // DEBUG

         }



      } catch (Exception e) {
         System.out.println("ERROR in line " + lineno + " : ");
         // TODO Auto-generated catch block
         e.printStackTrace();
      }


      Fileutils.closeSilently(parseReader);

      //System.out.println("Went through " + lineno + " lines with annotations.");

   }




   /**
    *
    * For a given SRL sentence,
    * get the parts and add to the counter.
    *
    * @param prefix
    * @param lineno
    * @param sentence
    * @param productID
    */
   private void analyzeSentence (String prefix, int lineno, SRLSentence sentence, String productID) {


      for (Word pred : sentence.getPredicates()) {

         //System.out.println(sentence); // DEBUG
         //System.out.println(pred);    // DEBUG

         compno++;


         String lineID = prefix+lineno+"-"+pred.getId();

         // Map the aspect phrases to NormAspects
         // will be 'null' if there is no aspect
         // will contain 'topLevelAspect' if the aspect cannot be mapped
         List<NormAspect> aspects = mapAspect (sentence, pred, sentence.getArgument(pred, ArgumentType.aspect));

         // Map the entities to products
         // In both cases, if the list is empty or nothing is found, the empty list is retunred
         List<String> products1 = mapEntity (sentence, pred, sentence.getArgument(pred, ArgumentType.entity1), productID);
         List<String> products2 = mapEntity (sentence, pred, sentence.getArgument(pred, ArgumentType.entity2), productID);

         // Add a point for every found product if
         // - there is no aspect ('aspects' == null)
         // - there a relevant aspect

         // Add the entity1 as positive
         if (products1 != null)
            for (String id : products1) {
               if (aspects == null) {
                  if (!useAspects)
                     counter.addPos(id, lineID);
               } else {
                  for (NormAspect a : aspects)
                     if (aspectFilter.isRelevantAspect(a))
                        counter.addPos(id, lineID);
               }
            }

         // Add the entity2 as negative
         if (products2 != null)
            for (String id : products2) {
               if (aspects == null) {
                  if (!useAspects)
                     counter.addNeg(id, lineID);
               } else {
                  for (NormAspect a : aspects)
                     if (aspectFilter.isRelevantAspect(a))
                        counter.addNeg(id, lineID);
               }
            }



      }


   }


   /**
    * Map a recognized phrase to a product ID.
    *
    * @param lineid
    * @param sentence
    * @param pred
    * @param argType
    * @param aspies
    * @param productUnderReview
    */
   private List<String> mapEntity (SRLSentence sentence, Word pred, List<Word> entitywordlist, String productUnderReview) {



      List<String> mappedProducts = new ArrayList<String>();

      if (entitywordlist == null || entitywordlist.isEmpty())
         return mappedProducts;

      // Get the entity (list of heads)
      for (Word entity2 : entitywordlist) {
         entityno++;
         List<String> productID = prodmapper.getMappingToProduct(SubTreeFinder.getSubTree(sentence, entity2, ArgumentType.entity2), productUnderReview);

         // could not be mapped
         if (productID == null || productID.isEmpty())
            continue;

         mappedProducts.addAll(productID);

         // DEBUG
//         String blubb = "";
//         for (Word word : sentence.getSubTree(entity2, ArgumentType.entity)) {
//            blubb += word.getForm() + " ";
//         }
//         System.out.println("mapped: " + entity2 + " " + blubb + " " + productID + " " + prodmapper.getProductName(productID.get(0)));
//         System.out.println();
         // end DEBUG

         entityMapped++;

      }

      return mappedProducts;

   }


   /**
    * Map CSRL aspect to a top-level aspect.
    *
    * @param sentence
    * @param pred
    * @param aspectlist
    * @return
    */
   private List<NormAspect> mapAspect (SRLSentence sentence, Word pred, List<Word> aspectlist) {

      if (aspectlist == null || aspectlist.isEmpty())
         return null;

      List<NormAspect> mappedAspects = new ArrayList<NormAspect>();

      // Get the aspect(s) of the comparison
      for (Word aspHead : aspectlist) { // there could be several aspects to one comparison

         // Annotated by the tool is the head word,
         // get the corresponding complete phrase (tokens)
         Word[] aspTokens = SubTreeFinder.getSubTree(sentence, aspHead, ArgumentType.aspect);
         String str = "";
         for (Word tok : aspTokens) {
            if (!POSUtils.isConjunctionPOS(tok.getPOS()) | !POSUtils.isDeterminerPOS(tok.getPOS()) | !POSUtils.isPunctuationPOS(tok.getPOS()))
               str += tok.getForm().toLowerCase() + " "; // would like to take lemma, but doesn't work!!
         }
         aspectsno++;

         List<NormAspect> mappy = aspectFilter.getNormAspect(str.trim());


        //System.out.println(aspHead + " mapped to " + mappy);

         if (mappy.isEmpty()) {
            mappedAspects.add(AspectFilter.topLevelAspect);
         } else {

            aspectsmapped++;
            mappedAspects.addAll(mappy);
         }


      }

      return mappedAspects;

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
      System.out.println("Went through " + alllineno + " sentences.");
      System.out.println("Found " + compno + " comparisons.");
      System.out.println("Found " + entityno + " entities, " + entityMapped + " could be mapped to a product.");
      System.out.println("Found " + aspectsno + " aspects, " + aspectsmapped + " could be mapped to a normalized aspect.");
   }



}
