package world.qode.app;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.desc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * A local-mode Spark job: word count over a bundled text (or the file named by
 * the first argument), printing the top words. Exits 0 on success.
 */
public final class WordCount {

	/** Lower-cases a line and splits it into words. */
	static final FlatMapFunction<String, String> WORDS = line -> words(line).iterator();

	static List<String> words(String line) {
		return Arrays.stream(line.toLowerCase(Locale.ROOT).split("[^\\p{L}']+"))
			.filter(w -> !w.isBlank())
			.toList();
	}

	public static void main(String[] args) throws IOException {
		SparkSession spark = SparkSession.builder()
			.appName("word-count")
			// SPARK_MASTER overrides, e.g. spark://host:7077 for a real cluster
			.master(System.getenv().getOrDefault("SPARK_MASTER", "local[*]"))
			.config("spark.ui.enabled", "false")
			.config("spark.sql.warehouse.dir", System.getProperty("java.io.tmpdir") + "/spark-warehouse")
			.getOrCreate();
		try {
			Dataset<String> lines = args.length > 0
				? spark.read().textFile(args[0])
				: spark.createDataset(bundledText(), Encoders.STRING());

			Dataset<Row> counts = lines.flatMap(WORDS, Encoders.STRING())
				.groupBy(col("value").as("word"))
				.count()
				.orderBy(desc("count"), col("word"));

			counts.show(10, false);
			System.out.println("distinct words: " + counts.count());
		} finally {
			spark.stop();
		}
	}

	private static List<String> bundledText() throws IOException {
		try (InputStream in = WordCount.class.getResourceAsStream("/input.txt")) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
		}
	}

	private WordCount() {
	}

}
