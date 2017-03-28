// (c) Wiltrud Kessler
// 02.06.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license 
// http://creativecommons.org/licenses/by-nc-sa/3.0/



package de.uni_stuttgart.ims.compranking.score;

public class RankingElement implements Comparable<RankingElement> {

   public String content; 
   
   public double rank;
   
   public double score = Double.NEGATIVE_INFINITY;
   
   public String comment = "";
   

   public RankingElement(String content, double rank) {
      this.content = content;
      this.rank = rank;
   }
   
   public RankingElement(String content, double rank, double score) {
      this.content = content;
      this.rank = rank;
      this.score = score;
   }
   
   
   public RankingElement(String content, double rank, double score, String comment) {
      this.content = content;
      this.rank = rank;
      this.score = score;
      this.comment = comment;
   }

   @Override
   public boolean equals (Object otherElement) {
      if (otherElement == null)
         return false;
      if (!(otherElement instanceof RankingElement))
         return false;
      RankingElement r = (RankingElement) otherElement;
      return this.content.equals(r.content);
   }


   @Override
   public int compareTo(RankingElement o) {
      return content.compareTo(o.content);
   }

   public String toString () {
      //~ return this.content + " " + this.rank + " " + this.score;
      return this.content ;
   }

   public String toLongString () {
      return String.format("%s %.1f [%.2f %s]", this.content, this.rank, this.score, this.comment);
      //~ return this.content ;
   }
   
}
