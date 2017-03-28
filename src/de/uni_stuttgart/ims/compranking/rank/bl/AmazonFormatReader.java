// (c) Wiltrud Kessler
// 21.03.16
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/





package de.uni_stuttgart.ims.compranking.rank.bl;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

public class AmazonFormatReader {


   //private int correctpartslength = 8; // OLD amazon format
   private int correctpartslength = 10; // NEW amazon format
   private boolean ignoreformaterrors = false;

   private boolean useMyOwnID = true; // generate own ID, discard those found in Amazon file


   String prefix;

   BufferedReader br = null;

   int lineno = 0;


   public AmazonFormatReader(String csvFilename) {

      // open input file
      try {
         br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(csvFilename)), Charset.forName("UTF-8")));
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }

      int tmp = csvFilename.lastIndexOf('/');
      prefix = csvFilename.substring(Math.max(0, tmp+1), Math.max(0, csvFilename.indexOf('.', tmp))) + "-";

      System.out.println("open file " + csvFilename);
   }

   public static class Review {
      public String id = null;
      public String productid = null;
      public String title = null;
      public String text = null;
   }




   public Review getLine () {

      String strLine = null;

      lineno++;

      // Read line (= 1 review)
      // If this fails or the line is null,
      // -> return null
      try {
         strLine = br.readLine();
         //System.out.println("get line =" + strLine + "=");
      } catch (IOException e1) {
         // TODO Auto-generated catch block
         e1.printStackTrace();
         return null;
      }
      if (strLine == null)
         return null;


      // Otherwise, let's create a review
      Review myreview = new Review();

      // Split at "," (delims)
      // if this doesn't give the correct number of parts,
      // try split at \t (for "all" format)
      String[] parts = strLine.split("\",\"");
      if (parts.length != correctpartslength) {
         parts = strLine.split("\t");
      }

      // Ignore things that are not reviews
      // (there are some errors in the extraction script)
      // -> return an empty review
      if (parts.length != correctpartslength ) {
         if (!ignoreformaterrors)
            System.out.println(parts.length + " parts -- ignore line " + Arrays.toString(parts));
         return myreview;
      }


      /// Format:
      // all:
      // Username \t Date \t Product ID \t Reviewer ID \t ? \t <empty> \t Rating \t Helpful \t Title \t \Review
      // = 9 parts
      // S. Pulgarin 25.09.2014  B00KKG1846  R28SZ7VYJM9XC6 84    5.0   0/0   Five Stars  Works as expected.

      // italy:
      // 0 \t XX \t Product ID \t Rating \t ? \t ? \t Date \t Reviewer ID \t Title \t \Review
      // = 10 parts
      // "0","XX","0061785679","5.0","0","0","February 9, 2015","A3GZG3F23DL9U1","Five Stars","Loved it"


      // Get parts (id, title, text, product)
      if (!parts[0].trim().isEmpty() & !useMyOwnID)
         myreview.id = parts[0].substring(1); // clip of first "
      else
         myreview.id = prefix + (lineno-1); // fallback id

      myreview.productid = parts[2];

      myreview.text = parts[parts.length-1];
      myreview.text = myreview.text.substring(0, myreview.text.length()-1); // clip of last "

      return myreview;

   }






}
