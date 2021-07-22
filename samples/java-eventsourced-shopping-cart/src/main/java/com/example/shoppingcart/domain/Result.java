package com.example.shoppingcart.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.lang.Class;
import java.lang.String;
import java.util.NoSuchElementException;
public class Result<Reply> {

       public Result(Reply reply, Queue<Object> events) {
           this.reply = reply;
           this.events = events;
       }

       private Reply reply;
       private Queue<Object> events;

       public Reply getReply() {
           return reply;
       }

       public List<Object> getEvents() {
           return new ArrayList(events); //Q copy to avoid consumption?
       }
       public Object getEvent(){
           return events.remove();
       }

       public <Event> Event getEventOfType(Class<Event> expectedClass){
            if(events.peek() == null){
                throw new NoSuchElementException("There are no events left");
            }
            if( expectedClass.isInstance(events.peek())){
                return (Event) events.poll();
            } else {//Q how to signal is not of that class and show the class
                throw new NoSuchElementException("Next event ["+events.peek()+"] is of class ["+ events.peek().getClass()+"] not of class ["+expectedClass+"]");
            }
       }


}