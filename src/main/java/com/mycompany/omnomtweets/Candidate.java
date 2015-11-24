/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.omnomtweets;

/**
 *
 * @author Purcell7
 */
public enum Candidate {
    //CANDIDATE(name, party, account, campainStartDate, consumerKey, consumerSecret, accessToken, accessTokenSecret, aliases)
    
    
    public String[] aliases;
    public String name;
    public String account;
    public String party;
    public String startDate;
    public String consumerKey;
    public String consumerSecret;
    public String accessToken;
    public String accessTokenSecret;
    
    Candidate(String name, String party, String account, String startDate, String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret, String... aliases){
        this.name = name;
        this.party = party;
        this.account = account;
        this.aliases = aliases;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;
        this.startDate = startDate;
    }
}
