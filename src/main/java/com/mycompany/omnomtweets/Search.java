/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.omnomtweets;

import java.text.SimpleDateFormat;
import java.util.Date;
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
        //Get your own keys!
        ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
              .setOAuthConsumerKey("")
              .setOAuthConsumerSecret("")
              .setOAuthAccessToken("")
              .setOAuthAccessTokenSecret("");
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
        
        Candidate candidate = new Candidate();
        
        while(true){
            //Setup and running the tweet gatherer.
            System.out.println("Tweets About Candidate " + candidate.name);
            TweetsAboutCandidates trumpTweets = new TweetsAboutCandidates(candidate, twitter);
            Map<String, RateLimitStatus> temp = twitter.getRateLimitStatus("search");
            RateLimitStatus rateLimit = temp.get("/search/tweets");
            int remaining = rateLimit.getRemaining();
            //change this manually unless I get a better method... maybe some file read thing...
            long maxId = Long.MAX_VALUE;
            trumpTweets.run("TweetsAbout" + candidate.name.replace(" ", "") + ".txt", remaining, maxId);

            //Setup and running the retweeter gatherer.
            System.out.println("Retweeters Of Candidate " + candidate.name);
            RetweetersOfCandidate retweeters = new RetweetersOfCandidate(candidate, twitter);
            temp = twitter.getRateLimitStatus("statuses");
            rateLimit = temp.get("/statuses/retweeters/ids");
            remaining = rateLimit.getRemaining();
            retweeters.run((candidate.name+"Retweets.txt").replace(" ",""), remaining);
                
            //Both systems have finished running, sleep for the minimum wait time
            System.out.println("Sleep time");
            temp = twitter.getRateLimitStatus("statuses");
            rateLimit = temp.get("/statuses/retweeters/ids");
            int remainingTime1 = rateLimit.getSecondsUntilReset();
            System.out.println("Time until retweeters " + remainingTime1);
            temp = twitter.getRateLimitStatus("search");
            rateLimit = temp.get("/search/tweets");
            int remainingTime2 = rateLimit.getSecondsUntilReset();
            //No negatives please.
            int wait = Math.max(Math.min(remainingTime1, remainingTime2),0);
            System.out.println("Wait "+wait);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS",Locale.US);
            long finishAt = System.currentTimeMillis() + wait *1000;
            System.out.println("Should resume at " + sdf.format(new Date(finishAt)));
            Thread.sleep(wait*1000);
        }
    }
}
