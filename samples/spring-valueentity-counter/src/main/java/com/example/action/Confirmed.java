package com.example.action;

public class Confirmed {

    private static Confirmed instance = null;

    private Confirmed(){}

    public static Confirmed getDefaultInstance(){
        if(instance == null){
            instance = new Confirmed();
        }
        return instance;
    }

}
