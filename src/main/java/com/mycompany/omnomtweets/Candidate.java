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
public class Candidate {
    public String[] aliases;
    public String name;
    public String account;
    public String party;
    
    public Candidate(String name, String party, String account){
        this.name = name;
        this.party = party;
        this.account = account;
    }

    //Default to Donald Trump, for... reasons
    Candidate() {
        this.name = "Donald Trump";
        this.party = "Republican";
        this.account = "@realDonaldTrump";
        this.aliases = new String[]{"Donald Trump", "@realDonaldTrump"};
    }
}
