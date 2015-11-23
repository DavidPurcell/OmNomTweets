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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 *
 * @author Purcell7
 */
public class TweetsAboutCandidates {
    Candidate candidate;
    private final Twitter twitter;
    
    public TweetsAboutCandidates(Candidate candidate, Twitter twitter){
        this.candidate = candidate;
        this.twitter = twitter;
    }
    
    public long run(String FileName, int calls, long maxId) throws TwitterException, InterruptedException{
        String query = "";
        for(String alias:candidate.aliases){
            query += alias + " OR ";
        }
        query = query.substring(0,query.length() - 3);
        while(true){
            if(calls > 0){
                System.out.println("Searching before maxId " + maxId);
                List<Status> tweets = search(query, maxId);
                maxId = tweets.get(tweets.size()-1).getId();
                writeTweetsToFile(tweets, FileName);   
                calls-=1;
            } else {
                System.out.println("TweetsAboutCandidates ran out of calls");
                System.out.println("Current MaxId " + maxId);
                System.out.println("Removing duplicates");
                try {
                    removeDuplicates(FileName);
                } catch (IOException ex) {
                    System.out.println("Duplicate removal failed.");
                }
                return maxId;
            }
        }
    }
    
    public List<Status> search(String str, long maxId){
        List<Status> tweets = null;
        try {
            Query query = new Query(str);
            query.setCount(100);
            QueryResult result;
            query.setMaxId(maxId);
            result = twitter.search(query);
            tweets = result.getTweets();
        } catch (TwitterException te) {
            System.out.println("Failed to search tweets: " + te.getMessage());
        }
        return tweets;
    }
    
    /**
     * Method to write the tweets to file, base 64 encoded tweet text.
     * @param tweets the tweets to be written
     * @param filename the file to write the tweets into
     * @return true unless something bad happens
     */
    public boolean writeTweetsToFile(List<Status> tweets, String filename){
        System.out.println("Writing " + tweets.size() + " tweets");
        boolean success = true;
        try {
            FileWriter addTweets = new FileWriter(new File(filename), true);
            if(tweets!= null && tweets.size()>0){
                for(Status tweet : tweets){
                    String encodedText = tweet.getText().replaceAll("\"", "\"\"");
                    addTweets.write("\"" + encodedText + "\"," + tweet.getUser().getId() +
                            "," + tweet.getId() + ","+ candidate.name + 
                            "," + tweet.getCreatedAt() + "\n");
                }
            }
            addTweets.close();
        } catch (IOException ex) {
            System.out.println("Something broke lol");
            success = false;
        }
        return success;
    }
    
    public void removeDuplicates(String filename) throws FileNotFoundException, IOException{
        System.out.println("Die duplicates");
        File f1 = new File(filename);
        FileReader fr = new FileReader(f1);
        BufferedReader br = new BufferedReader(fr);
        String line;
        boolean hasPrint = false;
        Map<String, String> unique = new HashMap<>();
        while ((line = br.readLine()) != null) {
            String[] temp = line.split(",");
            //System.out.println(temp[0] + "  " + temp[1]);
            if(!hasPrint){
                //System.out.println(temp[2]);  
                hasPrint = true;
            }
            if(temp.length >= 3){
                unique.put(temp[2], line);    
            }
        }
        fr.close();
        br.close();

        FileWriter fw = new FileWriter(f1);
        BufferedWriter out = new BufferedWriter(fw);
        for(String key : unique.keySet())
            //System.out.println(unique.get(key));
            out.write(unique.get(key) + "\n");
        out.flush();
        out.close();
    }
}
