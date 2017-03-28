# CompRanking

Ranking of products with sentiment expressions.
Code for paper (Kessler, Klinger and Kuhn, 2015).

__WARNING__: This is research code, it was not written with anybody else in mind nor with the goal of applying it "in real life". So it is hacky and may not be usable at all for you.



## Prerequisites

To run the code you will need:

- [CompBase](https://github.com/WiltrudKessler/CompBase): 
   Basic data structures, in-/output and just general helpful stuff for my project.
- Lots of Amazon data in the format provided by [Andrea Esuli's Amazon Downloader](https://github.com/aesuli/Amazon-downloader)
- files processed with [CSRL](https://github.com/WiltrudKessler/Csrl) and/or [JFSA](https://bitbucket.org/rklinger/jfsa)


## Running the code


### Create rankings

Run the following class to get the rankings:

- Baseline for scoring by number of reviews: `de.uni_stuttgart.ims.compranking.bl.RankingFromReviews`
  - input: Reviews in the Amazon Downloader format
- Baseline for scoring by review length: `de.uni_stuttgart.ims.compranking.bl.RankingFromLength`
  - input: Reviews in the Amazon Downloader format
- Baseline for scoring by sentiment terms from a dictionary: `de.uni_stuttgart.ims.compranking.bl.RankingFromTermCounting`
  - input: Reviews in the Amazon Downloader format
  - additional resource: either the MPQA or the GI dictionary

- Ranking by expressions found with JFSA: `de.uni_stuttgart.ims.compranking.jfsa.JFSARanker`
  - input: sentiment and target annotations that result from processing reviews with JFSA
- Ranking by expressions found with CSRL: `de.uni_stuttgart.ims.compranking.csrl.CSRLRanker`
  - input: parsed annotated sentences that result from processing review sentences with CSRL

Result of each execution will be a file that lists the products according to their ranks (with the scores).


### Score rankings

Use `de.uni_stuttgart.ims.compranking.csrl.CompareRankings` on the generated rank files.


## More information

### What does it actually do?

Read the paper!


### Configurations and settings for the paper

- Term counting: `polarityMode = PolarityMode.both`
   run both with and without normalization

- JFSA: `polarityMode = PolarityMode.both`
   run both with and without normalization



## Licence and References

(c) Wiltrud Kessler

This code is distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported license
[http://creativecommons.org/licenses/by-nc-sa/3.0/](http://creativecommons.org/licenses/by-nc-sa/3.0/)

Please cite:
Wiltrud Kessler, Roman Klinger and Jonas Kuhn (2015)
Towards Opinion Mining from Reviews for the Prediction of Product Rankings.
In Proceedings of the 6th Workshop on Computational Approaches to Subjectivity, Sentiment and Social Media Analysis (WASSA 2015). Lisboa, Portugal, 17. September 2015, pages 51-57.