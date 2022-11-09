package com.example.actions;

public class Confirmed {

	private static Confirmed instance = null;

	private Confirmed(){}
	
	public static Confirmed defaultInstance(){
	  if(instance == null) instance = new Confirmed();
	  return instance;	
	}
}