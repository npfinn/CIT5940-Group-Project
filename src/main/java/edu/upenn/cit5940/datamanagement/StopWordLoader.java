package edu.upenn.cit5940.datamanagement;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class StopWordLoader {
	
	//Will read stop words from the text file and returns a set containing the words
	
	public Set<String> load(String filePath) throws IOException{
		
		
		Set<String> stopWords = new HashSet<>();
		
		//open the file and read it line by line
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;

            while ((line = reader.readLine()) != null) {

                //Remove extra spaces and make everything lowercase
                String word = line.trim().toLowerCase();

                //Don't add empty lines
                if (!word.isEmpty()) {
                    stopWords.add(word);
                }
            }
        }

        return stopWords;
    }

    //Temporary test method
    public static void main(String[] args) throws IOException {

        StopWordLoader loader = new StopWordLoader();

        Set<String> stopWords = loader.load("data/stop_words.txt");

        System.out.println("Total stop words: " + stopWords.size());
        System.out.println("Contains 'the': " + stopWords.contains("the"));
        System.out.println("Contains 'and': " + stopWords.contains("and"));
    
	}

}
