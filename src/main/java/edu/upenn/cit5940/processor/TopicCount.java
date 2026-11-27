package edu.upenn.cit5940.processor;

// a word and how many times it occurs; one row of the "top topics" result
public record TopicCount(String word, int count) { }
