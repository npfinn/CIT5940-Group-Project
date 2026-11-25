package edu.upenn.cit5940.datamanagement;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import edu.upenn.cit5940.common.dto.Article;
import java.io.IOException;

public class CustomTrie {

    //inner class
    private class Node {
        private HashMap<Character, Node> children = new HashMap<>();

        // TODO (provide starting value)
        private boolean endOfWord = false;

    }

    //root node (has no value)
    private Node root = new Node();

    // TODO
    public void insertWord(String word) {
    	 //skip null or empty words
        if (word == null || word.isEmpty()) {
            return;
        }

        Node current = root;

        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);

            //add a node if this letter's path doesn't exist yet
            if (!current.children.containsKey(letter)) {
                current.children.put(letter, new Node());
            }

            //move to that letter's node
            current = current.children.get(letter);
        }

        //we reached the end of a complete word
        current.endOfWord = true;
    }

    //this implementation is given to students in the starter code
    public void insertList(String[] wordList) {
        for (String string : wordList) {
            insertWord(string);
        }
    }

    // TODO
    public boolean findWord(String word) {
    	//null and empty strings aren't words we're storing
        if (word == null || word.isEmpty()) {
            return false;
        }

        Node current = root;

        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);

            //if the path is missing, the word isn't here
            if (!current.children.containsKey(letter)) {
                return false;
            }

            //move to the next letter
            current = current.children.get(letter);
        }

        //make sure this is a whole word
        return current.endOfWord;
    }

    // TODO
    public void deleteWord(String word) {
    	//skip null or empty words
        if (word == null || word.isEmpty()) {
            return;
        }

        //check that we actually have this word
        if (!findWord(word)) {
            return;
        }

        //start at the root with the first letter
        deleteWordHelper(word, 0, root);
    }

    // TODO
    private boolean deleteWordHelper(String word, int index, Node curNode) {
    	 //we made it past the last letter
        if (index == word.length()) {
            curNode.endOfWord = false;

            //only remove this node if nothing comes after it
            return curNode.children.isEmpty();
        }

        char letter = word.charAt(index);
        Node nextNode = curNode.children.get(letter);

        //stop if this letter isn't here
        if (nextNode == null) {
            return false;
        }

        //check the next letter before deciding what to remove
        boolean removeNextNode = deleteWordHelper(word, index + 1, nextNode);

        if (removeNextNode) {
            curNode.children.remove(letter);
        }

        //keep this node if another word still needs it
        return curNode.children.isEmpty() && !curNode.endOfWord;
    }

    // TODO
    public List<String> allWords() {
    	//make a list to hold our words
        List<String> words = new ArrayList<>();

        //start with no letters
        StringBuilder letters = new StringBuilder();

        //start at the root and collect the words
        allWordsHelper(root, letters, words);

        return words;
    }

    // TODO
    public void allWordsHelper(Node node, StringBuilder accumulated, List<String> myList) {
    	//if we reached a whole word, add it to the list
        if (node.endOfWord) {
            myList.add(accumulated.toString());
        }

        for (char letter : node.children.keySet()) {
            //add this letter to the word we're building
            accumulated.append(letter);

            Node nextNode = node.children.get(letter);

            //keep going down this path
            allWordsHelper(nextNode, accumulated, myList);

            //take off the last letter before trying another path
            accumulated.deleteCharAt(accumulated.length() - 1);
        }
    }
    
  //Find words that start with the given prefix
    public List<String> autocomplete(String prefix, int limit) {

        List<String> matchingWords = new ArrayList<>();

        if (prefix == null || prefix.isEmpty() || limit <= 0) {
            return matchingWords;
        }

        prefix = prefix.toLowerCase();

        //Only allow up to 10 suggestions
        int maxSuggestions = Math.min(limit, 10);

        Node currentNode = root;

        //Find where the prefix ends in our Trie
        for (int i = 0; i < prefix.length(); i++) {

            char currentLetter = prefix.charAt(i);

            if (!currentNode.children.containsKey(currentLetter)) {
                return matchingWords;
            }

            currentNode = currentNode.children.get(currentLetter);
        }

        //Find complete words starting from this spot
        StringBuilder currentWord = new StringBuilder(prefix);
        collectSuggestions(currentNode, currentWord, matchingWords, maxSuggestions);

        return matchingWords;
    }

    //Keep collecting words until we reach our limit
    private void collectSuggestions(Node currentNode, StringBuilder currentWord, List<String> matchingWords, int limit) {

        if (matchingWords.size() >= limit) {
            return;
        }

        //Add the word if we reached a complete one
        if (currentNode.endOfWord) {
            matchingWords.add(currentWord.toString());
        }

        //Put the next letters in alphabetical order
        List<Character> nextLetters = new ArrayList<>(currentNode.children.keySet());
        Collections.sort(nextLetters);

        for (char currentLetter : nextLetters) {

            if (matchingWords.size() >= limit) {
                return;
            }

            currentWord.append(currentLetter);

            Node nextNode = currentNode.children.get(currentLetter);
            collectSuggestions(nextNode, currentWord, matchingWords, limit);

            //Remove the letter before trying the next one
            currentWord.deleteCharAt(currentWord.length() - 1);
        }
    }
    
    //Add words from article titles and bodies
    public void addArticleWords(List<Article> articles) {

        //Go through each article
        for (Article currentArticle : articles) {

            //Put the title and body together
            String articleText = currentArticle.getTitle() + " " + currentArticle.getBody();

            //Make everything lowercase
            articleText = articleText.toLowerCase();

            //Split the text into separate words
            String[] wordsInArticle = articleText.split("[^a-z0-9]+");

            //Add each word to our Trie
            for (String currentWord : wordsInArticle) {

                if (!currentWord.isEmpty()) {
                    insertWord(currentWord);
                }
            }
        }
    }
    
    //Temporary test for our autocomplete
    public static void main(String[] args) throws IOException {

        CustomTrie trie = new CustomTrie();

        trie.insertWord("program");
        trie.insertWord("programming");
        trie.insertWord("progress");
        trie.insertWord("project");
        trie.insertWord("python");

        System.out.println("Suggestions for pro: " + trie.autocomplete("pro", 10));
        System.out.println("Suggestions for py: " + trie.autocomplete("py", 10));
        System.out.println("Suggestions for xyz: " + trie.autocomplete("xyz", 10));
        System.out.println("Only 2 suggestions: " + trie.autocomplete("pro", 2));
        
        //Test autocomplete using our real articles
        ArticleLoader articleLoader = new ArticleLoader();
        List<Article> articles = articleLoader.load("data/articles_small.csv");

        CustomTrie articleTrie = new CustomTrie();
        articleTrie.addArticleWords(articles);

        System.out.println("Articles loaded: " + articles.size());
        System.out.println("Suggestions for tech: " + articleTrie.autocomplete("tech", 10));
        System.out.println("Suggestions for comp: " + articleTrie.autocomplete("comp", 10));
        System.out.println("Suggestions for twit: " + articleTrie.autocomplete("twit", 10));
        
      //Check that uppercase and lowercase searches match
        System.out.println("Uppercase works: " + articleTrie.autocomplete("TECH", 10).equals(articleTrie.autocomplete("tech", 10)));

        //Check that we can limit suggestions to just one
        System.out.println("Only 1 suggestion: " + articleTrie.autocomplete("tech", 1));

        //Check that a limit of zero returns nothing
        System.out.println("Zero limit is empty: " + articleTrie.autocomplete("tech", 0).isEmpty());
    }

}