// (c) Wiltrud Kessler
// 12.05.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license 
// http://creativecommons.org/licenses/by-nc-sa/3.0/



package de.uni_stuttgart.ims.compranking.rank.jfsa;

public class JFSAAnnotation {

   public int startIndex = 0;
   public int endIndex = 0;
   public String phrase = null;
   public String sentiment = null;
   

   public JFSAAnnotation(int startIndex, int endIndex, String phrase, String sentiment) {
      this(startIndex, endIndex, phrase);
      this.sentiment = sentiment;
   }

   public JFSAAnnotation(int startIndex, int endIndex, String phrase) {
      //System.out.println("add " + startIndex + "-" + endIndex + " ("+ phrase);
      this.startIndex = startIndex;
      this.endIndex = endIndex;
      this.phrase = phrase;
   }
   
   
   public String toString () {
      String dep = (sentiment!=null)?sentiment+",":"";
      return phrase + " (" + dep + startIndex + "-" + endIndex + ")";
   }
      


}
