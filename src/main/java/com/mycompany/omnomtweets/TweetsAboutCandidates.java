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
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import twitter4j.MediaEntity;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.URLEntity;

/**
 *
 * @author Purcell7
 */
public class TweetsAboutCandidates {
    Candidate candidate;
    private final Twitter twitter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
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
                //System.out.println("Searching before maxId " + maxId);
                List<Status> tweets = search(query, maxId);
                //System.out.println(tweets.size());
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
    
    public String getRandomDate(String after){
        long beginTime = dateFormat.parse(after, new ParsePosition(0)).getTime();
        long endTime = System.currentTimeMillis();
        long diff = endTime - beginTime + 1;
        long randomTime = beginTime + (long) (Math.random() * diff);
        Date randomDate;
       
        randomDate = new Date(randomTime);
        
        return dateFormat.format(randomDate);
    }
    
    /**
     * Searches for tweets using the given query string.
     * @param str the query to use
     * @return List of the tweet statuses that match the query.
     */
    public List<Status> search(String str, long maxId){
        List<Status> tweets = null;
        try {
            Query query = new Query(str);
            query.setCount(100);
            //English only.
            query.setLang("en");
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
        //System.out.println("Writing " + tweets.size() + " tweets");
        boolean success = true;
        try {
            FileWriter addTweets = new FileWriter(new File(filename), true);
            if(tweets!= null && tweets.size()>0){
                for(Status tweet : tweets){
                    String tweetText;
                    String idOfRetweetee = "";
                    if(tweet.getRetweetedStatus()!=null){
                        tweetText = "RT " + tweet.getRetweetedStatus().getText();
                        idOfRetweetee = "" + tweet.getRetweetedStatus().getUser().getScreenName();
                        //System.out.println("retweeted" + tweetText);
                    } else {
                        tweetText = tweet.getText();
                    }
                    String urlText = "";
                    if(tweet.getURLEntities().length > 0){
                        for(URLEntity url:tweet.getURLEntities()){
                            if(url.getExpandedURL() != null){
                                urlText += url.getExpandedURL() + " ";
                                //System.out.println("Expanded URL " + url.getExpandedURL());
                            } else {
                                urlText += url.getURL() + " ";
                                //System.out.println("URL " + url.getURL());    
                            }
                        }
                    }
                    if(tweet.getMediaEntities().length > 0){
                        for(MediaEntity media:tweet.getMediaEntities()){
                            if(media.getExpandedURL() != null){
                                urlText += media.getExpandedURL() + " ";
                                //System.out.println("Expanded URL " + media.getExpandedURL());
                            } else {
                                urlText += media.getMediaURL() + " ";
                                //System.out.println("URL " + media.getMediaURL());    
                            }
                        }
                    }
                    String encodedText = tweet.getText().replaceAll("\"", "\"\"");
                    String writeMe = "\"" + encodedText + "\"," + urlText + "," + 
                            tweet.getUser().getId() + "," + tweet.getId() + ","+ candidate.name + 
                            "," + tweet.getCreatedAt() + "," + idOfRetweetee + "\n";
                    //System.out.println(writeMe);
                    addTweets.write(writeMe);
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
        //System.out.println("Die duplicates");
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
