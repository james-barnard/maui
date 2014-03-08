package maui.main;

/*
 *    MauiModelBuilder.java
 *    Copyright (C) 2009 Olena Medelyan
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
import gnu.trove.TIntHashSet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import maui.stemmers.FrenchStemmer;
import maui.stemmers.PorterStemmer;
import maui.stemmers.Stemmer;
import maui.stopwords.Stopwords;
import maui.stopwords.StopwordsEnglish;
import maui.stopwords.StopwordsFrench;

import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.ProgressNotifier;
import org.wikipedia.miner.util.text.CaseFolder;
import org.wikipedia.miner.util.text.TextProcessor;

/**
 * The beginnings of a tagging service - this will drive the tagger
 * @author James Barnard
 *
 */
public class Tagger {

  private MauiTopicExtractor topicExtractor;
  private MauiModelBuilder modelBuilder;

  private Wikipedia wikipedia;

  private String server;
  private String database;
  private String dataDirectory;
  private boolean cache = false;

  public Tagger (String server, String database, String dataDirectory, boolean cache) throws Exception  {
    this.server = server;
    this.database = database;
    this.dataDirectory = dataDirectory;
    System.err.println("==============================>loadWiki");
    this.cache = cache;
    loadWikipedia();
    System.err.println("==============================>loadWiki:finish");
    /*
    */
  }

  public Tagger ()  {  }

  private void loadWikipedia() throws Exception {

    wikipedia = new Wikipedia(server, database, "root", null);

    TextProcessor textProcessor = new CaseFolder();

    File dataDir = new File(dataDirectory);

    if (cache) {
      ProgressNotifier progress = new ProgressNotifier(5);
      // cache tables that will be used extensively
      TIntHashSet validPageIds = wikipedia.getDatabase().getValidPageIds( dataDir, 2, progress);
      wikipedia.getDatabase().cachePages(dataDir, validPageIds, progress);
      wikipedia.getDatabase().cacheAnchors(dataDir, textProcessor, validPageIds, 2, progress);
      wikipedia.getDatabase().cacheInLinks(dataDir, validPageIds, progress);
      wikipedia.getDatabase().cacheGenerality(dataDir, validPageIds, progress);
    }
  }

  /**
   * Sets general parameters: debugging printout, language specific options
   * like stemmer, stopwords.
   * @throws Exception
   */
  private void setGeneralOptions()  {


    modelBuilder.debugMode = true;
    modelBuilder.wikipedia = wikipedia;

    /* language specific options
    Stemmer stemmer = new FrenchStemmer();
    Stopwords stopwords = new StopwordsFrench();
    String language = "fr";
    String encoding = "UTF-8";
    modelBuilder.stemmer = stemmer;
    modelBuilder.stopwords = stopwords;
    modelBuilder.documentLanguage = language;
    modelBuilder.documentEncoding = encoding;
    topicExtractor.stemmer = stemmer;
    topicExtractor.stopwords = stopwords;
    topicExtractor.documentLanguage = language;
    */

    /* specificity options
    modelBuilder.minPhraseLength = 1;
    modelBuilder.maxPhraseLength = 5;
    */

    topicExtractor.debugMode = true;
    topicExtractor.topicsPerDocument = 10;
    topicExtractor.wikipedia = wikipedia;
  }

  /**
   * Set which features to use
   */
  private void setFeatures() {
    modelBuilder.setBasicFeatures(true);
    modelBuilder.setKeyphrasenessFeature(true);
    modelBuilder.setFrequencyFeatures(true);
    modelBuilder.setPositionsFeatures(true);
    modelBuilder.setLengthFeature(true);
    modelBuilder.setNodeDegreeFeature(true);
    modelBuilder.setBasicWikipediaFeatures(false);
    modelBuilder.setAllWikipediaFeatures(false);
  }

  /**
   * Demonstrates how to perform automatic tagging. Also applicable to
   * keyphrase extraction.
   *
   * @throws Exception
   */
  public void testAutomaticTagging() throws Exception {
    topicExtractor = new MauiTopicExtractor();
    modelBuilder = new MauiModelBuilder();

    setGeneralOptions();
    setFeatures();

    String trainDir  = "data/automatic_tagging/train";
    String testDir   = "data/automatic_tagging/test";
    String modelName = "tagging_model";

    modelBuilder.inputDirectoryName = trainDir;
    modelBuilder.modelName = modelName;

    // change to 1 for short documents
    modelBuilder.minNumOccur = 2;

    HashSet<String> fileNames = modelBuilder.collectStems();
    modelBuilder.buildModel(fileNames);
    modelBuilder.saveModel();

    topicExtractor.inputDirectoryName = testDir;
    topicExtractor.modelName = modelName;
    topicExtractor.loadModel();
    fileNames = topicExtractor.collectStems();
    topicExtractor.extractKeyphrases(fileNames);
  }

  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    SimpleDateFormat formatter = new SimpleDateFormat(
        "EEE, dd-MMM-yyyy HH:mm:ss");

    Date todaysDate = new java.util.Date();
    String formattedDate1 = formatter.format(todaysDate);
    Tagger tagger;

    tagger = new Tagger();
    tagger.testAutomaticTagging();

    todaysDate = new java.util.Date();
    String formattedDate2 = formatter.format(todaysDate);
    System.err.print("Run from " + formattedDate1);
    System.err.println(" to " + formattedDate2);
  }

}
