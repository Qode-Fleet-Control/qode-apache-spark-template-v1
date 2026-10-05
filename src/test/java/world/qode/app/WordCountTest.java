package world.qode.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class WordCountTest {

	@Test
	void splitsAndLowerCases() {
		assertEquals(List.of("spark", "is", "a", "unified", "engine"), WordCount.words("Spark is a unified engine."));
	}

	@Test
	void ignoresBlankLines() {
		assertEquals(List.of(), WordCount.words("   "));
	}

}
