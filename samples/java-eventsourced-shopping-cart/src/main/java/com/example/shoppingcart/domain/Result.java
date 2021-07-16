package com.akkaserverless.javasdk.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

public class Result<Reply> {

       public Result(Reply reply, Queue<Object> events) {
           this.reply = reply;
           this.events = events;
       }

       private Reply reply;
       private Queue<Object> events = new LinkedList<Object>();

       public Reply getReply() {
           return reply;
       }
       public List<Object> getEvents() {
           return new ArrayList(events);
       }
       public <Event> Event getEvent(Class<Event> expectedClass){
           return (Event)events.remove();
       }
}