package edu.upenn.cit5940.datamanagement;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

class StopWordLoaderTest {

	 @TempDir
	    Path stopWordFolder;

	    //Trim spaces, ignore blank lines, and keep each lowercase word once
	    @Test
	    public void testStopWordCleanup() throws IOException {

	        Path skipFile = stopWordFolder.resolve("words_to_skip.txt");
	        Files.writeString(skipFile, "  THE  \n\nAnd\n   \nthe\n WITH \n");

	        StopWordLoader loader = new StopWordLoader();
	        Set<String> loadedWords = loader.load(skipFile.toString());

	        assertEquals(3, loadedWords.size());
	        assertTrue(loadedWords.contains("the"));
	        assertTrue(loadedWords.contains("and"));
	        assertTrue(loadedWords.contains("with"));
	        assertFalse(loadedWords.contains(""));
	        assertFalse(loadedWords.contains("THE"));
	    }

	    //An empty file should give us an empty set
	    @Test
	    public void testEmptyStopWordFile() throws IOException {

	        Path emptyFile = stopWordFolder.resolve("empty_skip_words.txt");
	        Files.writeString(emptyFile, "");

	        StopWordLoader loader = new StopWordLoader();

	        assertTrue(loader.load(emptyFile.toString()).isEmpty());
	    }

	    //A missing stop-word file should report an error
	    @Test
	    public void testMissingStopWordFile() {

	        StopWordLoader loader = new StopWordLoader();

	        assertThrows(IOException.class, () -> loader.load(stopWordFolder.resolve("absent_skip_words.txt").toString()));
	    }

}
