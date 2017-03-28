// (c) Wiltrud Kessler
// 18.03.2015
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
import java.util.List;
import java.util.TreeSet;
import java.util.Map.Entry;

import de.uni_stuttgart.ims.nlpbase.nlp.POSUtils;
import de.uni_stuttgart.ims.nlpbase.nlp.Word;
import de.uni_stuttgart.ims.util.Fileutils;


/**
 * Map a name to a product ID,
 * map a review ID to the reviewed product,
 * and check if a product is in the list of relevant products.
 *
 * @author kesslewd
 *
 */
public class ProductMapper {


   /**
    * List of relevant products.
    */
   private TreeSet<String> relevantProducts = null;


   /**
    * Relation reviews to products.
    */
   private HashMap<String, String> relationReviewsToProducts = new HashMap<String, String>();



   /**
    * Product IDs to product names.
    */
   private HashMap<String, String> names = new HashMap<String, String>();

   /**
    * Product names to product IDs.
    */
   private HashMap<String, List<String>> namesInv = new HashMap<String, List<String>>();

   /**
    * Product ids of products with the same name
    */
   private HashMap<String, List<String>> sameProdNames = new HashMap<String, List<String>>();

   /**
    * Lookup words of product names to product ids (inverted index).
    */
   private HashMap<String, List<String>> corefs = new HashMap<String, List<String>>();



   // RELEVANT PRODUCTS


   /**
    * Read relevant products from a file.
    *
    * @param filename
    * @throws FileNotFoundException
    */
   public void readRelevantProducts (String filename) throws FileNotFoundException {

      DataInputStream fstream = new DataInputStream(new FileInputStream(filename));
      BufferedReader brRevProds = new BufferedReader(new InputStreamReader(fstream, Charset.forName("UTF-8")));

      relevantProducts = new TreeSet<String>();

      String nextLine = null;
      int lineno = 0;

      try {
         while ((nextLine = brRevProds.readLine()) != null) {
            lineno++;
            String[] parts = nextLine.split("\t");
            // productID \t maybe other stuff
            relevantProducts.add(parts[0]);
         }

         Fileutils.closeSilently(brRevProds);
      } catch (Exception e) {
         System.out.println("ERROR in line " + lineno + " : " + nextLine);
         e.printStackTrace();
      }

   }


   /**
    * Check whether this product is in the list of relevant products.
    * @param productID
    * @return
    */
   public boolean isRelevantProduct (String productID) {
      return (relevantProducts == null || relevantProducts.contains(productID));

   }

   /**
    * Get the IDs of all relevant products.
    * @return
    */
   public TreeSet<String> getRelevantProducts () {
      return relevantProducts;
   }

   /**
    * Print list of relevant products.
    */
   public void printRelevantProucts () {
      for (String prodID : relevantProducts) {
         System.out.println(prodID);
      }
   }



   // RELATIONS PRODUCTS-REVIEWS


   /**
    * Read relatoins of products with reviews.
    *
    * @param filename
    * @throws FileNotFoundException
    */
   public void readRelations (String filename) throws FileNotFoundException {

      DataInputStream fstream = new DataInputStream(new FileInputStream(filename));
      BufferedReader brRevProds = new BufferedReader(new InputStreamReader(fstream, Charset.forName("UTF-8")));

      String nextLine = null;
      int lineno = 0;
      try {
         while ((nextLine = brRevProds.readLine()) != null) {
            lineno++;
            String[] parts = nextLine.split("\t");
            // reviewID \t productID
            String productID = parts[1];
            if (relevantProducts == null || relevantProducts.contains(productID))
               relationReviewsToProducts.put(parts[0], productID);
         }


         Fileutils.closeSilently(brRevProds);
      } catch (Exception e) {
         System.out.println("ERROR in line " + lineno + " : " + nextLine);
         e.printStackTrace();
      }

   }


   public String getProductIdForReview (String reviewID) {
      return relationReviewsToProducts.get(reviewID);
   }





   // PRODUCT NAMES


   /**
    * Read the list of product names
    * (only for relevant products)
    *
    * @param filename
    * @throws FileNotFoundException
    */
   public void readNames (String filename) throws FileNotFoundException {

      // open input
      BufferedReader brProductNames = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(filename)), Charset.forName("UTF-8")));

      String nextLine = null;
      int lineno = 0;
      try {
         while ((nextLine = brProductNames.readLine()) != null) {
            lineno++;

            // productID \t name
            String[] parts = nextLine.split("\t");

            // Check if this is a relevant product (or we do all)
            // if not skip entry
            String productID = parts[0];
            if (relevantProducts != null && !relevantProducts.contains(productID)) {
               continue;
            }


            // For every token insert a coref to the product
            String[] tokens = parts[1].split(" ");
            String name = "";
            int i=0;
            for (String token : tokens) {

               String word = token.toLowerCase();

               // Skip determiners and conjunctions
               if (isDet(word) | isConj(word))
                  continue;

               // If this word starts a description, stop
               // e.g., 'XY with 10 MP' or 'XY without AF'
               // -> take only 'XY'
               if (isDescriptionOpener(word))
                  break;

               // Add this product id to the list of possible corefs
               // Add only once, even if the word is contained twice
               List<String> idlist = corefs.get(word);
               if (idlist == null) {
                  idlist = new ArrayList<String>();
                  corefs.put(word, idlist);
               }
               if (!idlist.contains(productID)) {
                  idlist.add(productID);
               }
               name += word + " ";

               // The model name is the final part of the actual name
               if (isModelName(word) & i>0)
                  break;

               // Take at most 6 tokens as the name
               if (i>=5)
                  break;
               i++;
            }


            // add to names list
            //    product->name
            names.put(productID, name);

            // add to inverted name list
            //    name -> product ids that have this
            List<String> prodIDList = namesInv.get(name);
            if (prodIDList != null) {
               prodIDList.add(productID);
            } else {
               prodIDList = new ArrayList<String>();
               prodIDList.add(productID);
               namesInv.put(name, prodIDList);
            }

         }

         Fileutils.closeSilently(brProductNames);


         // Get a list of products that have the same name
         for (String name : namesInv.keySet()) {
            List<String> productIDs = namesInv.get(name);
            if (productIDs.size() > 1) {
               for (String product : productIDs) {
                  sameProdNames.put(product, productIDs);
               }
            }
         }

         // Give a warning if there is some relevant product that
         // we haven't found a name for
         if (relevantProducts != null)
            for (String id : relevantProducts)
               if (names.get(id) == null)
                  System.out.println("WARNING: No name found for product id " + id );



      } catch (Exception e) {
         System.out.println("ERROR in line " + lineno + " : " + nextLine);
         e.printStackTrace();
      }


   }


   /**
    * Print list of products and their names.
    */
   public void printProductNames () {
      for (String prodID : names.keySet()) {
         System.out.println(prodID + "\t" + names.get(prodID));
      }
   }


   /**
    *
    * Get name of this product.
    * @param productID
    * @return
    */
   public String getProductName (String productID) {
      return names.get(productID);
   }


   /**
    * From a set of words, get to the product it is referring to.
    *
    * Done by calculating word overlap btw the words and the name of the product.
    *
    * @param words
    * @return
    */
   public  List<String> getMappingToProduct(Word[] words, String productUnderReview) {

      // Count the corefs for the different products
      // key: product ID
      // value: number of overlapping words
      HashMap<String, Integer> counts = new HashMap<String, Integer>();


      counts.put(productUnderReview, 0);


      int tokenno = 0;
      int addvalue = 1;

      for (Word tok : words) {

         // Skip determiners
         if (POSUtils.isDeterminerPOS(tok.getPOS()) | POSUtils.isConjunctionPOS(tok.getPOS()))
            continue;

         // TODO: should use lemma here, but this is empty!!
         String word = tok.getForm().toLowerCase();


         if (word.equals("camera") || word.equals("this") || word.equals("it")) {

            Integer value = counts.get(productUnderReview);
            counts.put(productUnderReview,value+addvalue);
            continue;
         }


         // Skip generic words
         // TODO probably good, but how to do in a more universal way??
//         if (isGenericWord(word))
//            continue;

         // TODO: let model numbers count more?
         if (isModelName(word)) {
            addvalue = 3;
         }
         else
            addvalue = 1;



         // Which names of products contain this word?
         List<String> products = corefs.get(word);
         if (products != null) {
            //System.out.println(tok + " " + products);
            for (String product : products) {
               Integer value = counts.get(product);
               if (value == null)
                  value = 0;
               counts.put(product,value+addvalue);
            }
         }

         tokenno++;

      }


      // If no product matches any word, just return null
      if (counts.isEmpty())
         return null;



      String mostCountProduct = null;
      int mostCounts = 0;
      boolean moreThanOne = false;
      List<String> otherCandidates = new ArrayList<String>();
      for (Entry<String, Integer> entry : counts.entrySet()) {
         if (entry.getValue() > mostCounts) {
            mostCounts = entry.getValue();
            mostCountProduct = entry.getKey();
            moreThanOne = false;
            otherCandidates = new ArrayList<String>();
         } else if (entry.getValue() == mostCounts) {
            if (sameProdNames.get(mostCountProduct) == null || !sameProdNames.get(mostCountProduct).contains(entry.getKey())) {
            moreThanOne = true;
            otherCandidates.add(entry.getKey());
            }
         }
      }

      if (mostCountProduct == null)
         return null;


      // Calculate which percentage of the string is matched,
      // reject if this is too low
      // TODO tune here
      double overlap = (double)mostCounts / (double)tokenno;


      // DEBUG
      //if (mostCountProduct != null)
         //System.out.println(mostCountProduct + " " + mostCounts + " word overlap " + moreThanOne + " "+ overlap + " " + otherCandidates);

      if (moreThanOne | overlap < 0.3){
         return null;

         // alternative: return all -> very bad
//         List<String> result =  new ArrayList<String>();
//         otherCandidates.add(mostCountProduct);
//         for (String product : otherCandidates) {
//            List<String> samies = sameProdNames.get(product);
//            if (samies == null)
//               result.add(product);
//            else
//               result.addAll(samies);
//         }
//         return result;
      }
      else {
         List<String> result = sameProdNames.get(mostCountProduct);
         if (result == null) {
            result = new ArrayList<String>();
            result.add(mostCountProduct);
         }
         return result;
      }

   }


   private static boolean isModelName(String form) {

      boolean hasDigit = false;
      boolean hasLetter = false;

      for (Character letter : form.toCharArray()) {
         if (Character.isDigit(letter)) {
            hasDigit = true;
         }

         if (Character.isLetter(letter)) {
            hasLetter = true;
         }
      }

      return hasDigit & hasLetter;
   }


   private boolean isDet(String form) {
      if (form.equalsIgnoreCase("the"))
         return true;
      if (form.equalsIgnoreCase("a"))
         return true;
      if (form.equalsIgnoreCase("an"))
         return true;
      if (form.equalsIgnoreCase("my"))
         return true;
      if (form.equalsIgnoreCase("this"))
         return true;

      return false;

   }



   private boolean isConj(String form) {
      if (form.equalsIgnoreCase("or"))
         return true;
      if (form.equalsIgnoreCase("and"))
         return true;

      return false;
   }



   private boolean isDescriptionOpener(String form) {
      if (form.equalsIgnoreCase("with"))
         return true;
      if (form.equalsIgnoreCase("w/"))
         return true;
      if (form.equalsIgnoreCase("without"))
         return true;
      if (form.equalsIgnoreCase("w/o"))
         return true;
      if (form.equalsIgnoreCase("+"))
         return true;
      if (form.equalsIgnoreCase("for"))
         return true;

      return false;
     }


}
