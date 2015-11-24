/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.omnomtweets;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

/**
 *
 * @author Purcell7
 */
public class Search {
    public static Twitter twitter;
    public static void main(String[] args) throws Exception {
        Map<Candidate, Long> maxIdLookup = new HashMap<>();
        for(Candidate candidate:Candidate.values()){
            System.out.println("Candidate: " + candidate.name);
            
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
              .setOAuthConsumerKey(candidate.consumerKey)
              .setOAuthConsumerSecret(candidate.consumerSecret)
              .setOAuthAccessToken(candidate.accessToken)
              .setOAuthAccessTokenSecret(candidate.accessTokenSecret);
            TwitterFactory tf = new TwitterFactory(cb.build());
            twitter = tf.getInstance();

            //Check to see if we can run a cycle, if we can't, wait.
            Map<String, RateLimitStatus> temp = twitter.getRateLimitStatus("statuses");
            RateLimitStatus rateLimit = temp.get("/statuses/retweeters/ids");
            boolean waitForRetweeters = rateLimit.getRemaining() <= 0;
            int remainingTime1 = rateLimit.getSecondsUntilReset();
            //System.out.println("Time until retweeters " + remainingTime1);
            temp = twitter.getRateLimitStatus("search");
            rateLimit = temp.get("/search/tweets");
            boolean waitForTweets = rateLimit.getRemaining() <= 0;
            //System.out.println(rateLimit.getRemaining());
            int remainingTime2 = rateLimit.getSecondsUntilReset();
            //No negatives please.
            
            if(waitForRetweeters && waitForTweets){
                int wait = Math.max(Math.min(remainingTime1, remainingTime2),0);
                //System.out.println("Wait "+wait);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS",Locale.US);
                long finishAt = System.currentTimeMillis() + wait *1000;
                System.out.println("Should resume at " + sdf.format(new Date(finishAt)));
                Thread.sleep(wait*1000);
            }
            
            //Setup and running the tweet gatherer.
            System.out.println("Tweets About Candidate " + candidate.name);
            TweetsAboutCandidates trumpTweets = new TweetsAboutCandidates(candidate, twitter);
            temp = twitter.getRateLimitStatus("search");
            rateLimit = temp.get("/search/tweets");
            int remaining = rateLimit.getRemaining();
            if(!maxIdLookup.containsKey(candidate)){
                maxIdLookup.put(candidate, Long.MAX_VALUE); 
            }
            long maxId = maxIdLookup.get(candidate).longValue();
            //change this manually unless I get a better method... maybe some file read thing...
            trumpTweets.run("TweetsAbout" + candidate.name.replace(" ", "") + ".txt", remaining, maxId);

            //Setup and running the retweeter gatherer.
            System.out.println("Retweeters Of Candidate " + candidate.name);
            RetweetersOfCandidate retweeters = new RetweetersOfCandidate(candidate, twitter);
            temp = twitter.getRateLimitStatus("statuses");
            rateLimit = temp.get("/statuses/retweeters/ids");
            remaining = rateLimit.getRemaining();
            retweeters.run((candidate.name+"Retweets.txt").replace(" ",""), remaining);
        }
    }
}
