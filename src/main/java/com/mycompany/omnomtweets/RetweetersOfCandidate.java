/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.omnomtweets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 *
 * @author Purcell7
 */
public class RetweetersOfCandidate {
    Candidate candidate;
    private final Twitter twitter;
    private int calls;
    
    public RetweetersOfCandidate(Candidate candidate, Twitter twitter){
        this.candidate = candidate;
        this.twitter = twitter;
    }
    
    /**
     * Gets the retweeters of a candidate and appends them to specified file.
     * @param filename name of file to append to.
     * @param calls number of API calls left
     */
    public void run(String filename, int totalCalls) throws InterruptedException{
        //System.out.println(filename);
        this.calls = totalCalls;
        boolean success = true;
        List<String> candidateTweetIds = getCandidateTweetIds();
        for(String tweetId : candidateTweetIds){
            //System.out.println(tweetId);
            try {
                //System.out.println(temp.keySet());
                //System.out.println("remaining queries: " + calls);
                if(calls <= 0){
                    System.out.println("Retweeers out of calls");
                    return;
                }
                List<String> retweets = getRetweeters(tweetId);
                if(retweets != null && retweets.size() > 0){
                    String retweetFile = (candidate.name+"Retweets.txt").replace(" ", "");
                    writeRetweetersToFile(retweets, retweetFile, tweetId);
                    markTweetAsProcessed(tweetId);
                } else {
                    //System.out.println("Removing duplicates");
                    try {
                        removeDuplicates(filename);
                    } catch (IOException ex) {
                        System.out.println("Duplicate removal failed. " + ex.getMessage());
                    }
                }
            } catch (TwitterException ex) {
                System.out.println(ex);
                break;
            } catch (Exception ex) {
                System.out.println("OOPS " + ex.getMessage());
            }
        }
    }
    
    /**
     * Method to get the 200 most recent tweets from a candidate.
     * @return List of 200 most recent tweet ids.
     */
    public List<String> getCandidateTweetIds(){
        List<String> candidateTweetIds = new ArrayList<>();
        
        //First try to read in unprocessed tweets
        List<String> processedIds = new ArrayList<>();
        String tempFileName = (candidate.name+"Tweets.txt").replace(" ", "");
        File f1 = new File(tempFileName);
        FileReader fr;
        try {
            fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Processed")){
                    processedIds.add(line.split(",")[0]);
                } else {
                    candidateTweetIds.add(line);
                }
            }
            fr.close();
            br.close();
        } catch (Exception ex) {
            System.out.println("Something went wrong with file IO");
        }
        
        //If there weren't any unproccessed ones, get some new ones.
        if(candidateTweetIds.isEmpty()){
            try {
                List<Status> statuses;

                Paging paging = new Paging(1, 200);
                statuses = twitter.getUserTimeline(candidate.account, paging);
                //System.out.println("Gathered: " + statuses.size() + " tweet ids");
                for (Status status : statuses) {
                    candidateTweetIds.add(String.valueOf(status.getId()));
                }

                //File to store tweetIds in as an intermediary.
                FileWriter writeCandidateTweets = new FileWriter(new File(tempFileName), true);
                for(String tweetId : candidateTweetIds){
                    writeCandidateTweets.write(tweetId + "\n");
                }
                writeCandidateTweets.close();

            } catch (TwitterException te) {
                System.out.println("Failed to get timeline: " + te.getMessage());
            } catch (IOException ex) {
                Logger.getLogger("FileWriting sucks");
            }
        }
        return candidateTweetIds;
    }
    
    /**
     * Method user IDs of retweeters.  Paginates on cursor.
     * @param tweetId id of the tweet we are getting retweeters of
     * @param calls number of API calls left
     * @return List of users that retweeted the given tweet.
     */
    public List<String> getRetweeters(String tweetId){
        //System.out.println("Getting retweeters of " + tweetId);
        List<String> retweeters = new ArrayList<>();
        IDs currentIDs;
        try {
            if(calls > 0){
                calls-=1;
                currentIDs = twitter.getRetweeterIds(Long.parseLong(tweetId), -1);
                long[] longIDs = currentIDs.getIDs();
                //System.out.println("Got " + longIDs.length + " retweeters");
                for(int i=0; i<longIDs.length; i++){
                    retweeters.add(""+longIDs[i]);
                }
                while(calls > 0 && currentIDs.hasNext()){
                    calls -= 1;
                    currentIDs = twitter.getRetweeterIds(Long.parseLong(tweetId), currentIDs.getNextCursor());
                    longIDs = currentIDs.getIDs();
                    //System.out.println("Adding " + longIDs.length + " retweeters");
                    for(int i=0; i<longIDs.length; i++){
                        retweeters.add(""+longIDs[i]);
                    }
                }
            } else {
                System.out.println("Cut off early");
                return retweeters;
            }
        } catch (TwitterException ex) {
            System.out.println("Failed to get rate limit status: " + ex.getMessage());
            return retweeters;
        }
        return retweeters;
    }
    
    /**
     * Method to write the retweeters to file.
     * @param retweeters the retweeters to be written
     * @param filename the file to write the retweeters into
     * @param sourceTweetId id of the tweet that these were retweets of
     * @return List of users that retweeted the given tweet.
     */
    public boolean writeRetweetersToFile(List<String> retweeters, String filename, String sourceTweetId){
        boolean success = true;
        try {
            FileWriter addTweets = new FileWriter(new File(filename), true);
            for(String retweeter : retweeters){
                addTweets.write(retweeter + "," + sourceTweetId + "\n");
            }
            addTweets.close();
        } catch (IOException ex) {
            System.out.println("Something broke lol");
            success = false;
        }
        return success;
    }

    //EEW, fileIO
    private void markTweetAsProcessed(String tweetId) throws Exception {
        String tempFileName = (candidate.name+"Tweets.txt").replace(" ", "");
        File f1 = new File(tempFileName);
        FileReader fr = new FileReader(f1);
        BufferedReader br = new BufferedReader(fr);
        String line;
        List<String> lines = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            if (line.equals(tweetId))
                line += ", Processed";
            lines.add(line);
        }
        fr.close();
        br.close();

        FileWriter fw = new FileWriter(f1);
        BufferedWriter out = new BufferedWriter(fw);
        for(String s : lines)
             out.write(s + "\n");
        out.flush();
        out.close();
    }
    
    public void removeDuplicates(String filename) throws FileNotFoundException, IOException{
        //System.out.println("Die duplicate retweeters");
        File f1 = new File(filename);
        FileReader fr = new FileReader(f1);
        BufferedReader br = new BufferedReader(fr);
        String line;
        Map<String, String> unique = new HashMap<>();
        while ((line = br.readLine()) != null) {
            unique.put(line, line);
        }
        fr.close();
        br.close();

        FileWriter fw = new FileWriter(f1);
        BufferedWriter out = new BufferedWriter(fw);
        for(String key : unique.keySet())
            out.write(unique.get(key) + "\n");
        out.flush();
        out.close();
    }
}
