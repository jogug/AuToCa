package ch.unibe.scg.autoca.tokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenPositions {
	private static final int NOT_FOUND = -1;
	private Matcher m;
	private boolean found;
	@SuppressWarnings("unused")
	private int position;

	public TokenPositions(String word, String s) {

		m = Pattern.compile(word).matcher(s);
		found = m.find();
	}

	public String token() {
		if (found) {
			return m.group();
		}
		return null;
	}

	public int find(int currentPosition) {
		if (found && (currentPosition > m.start())) {
			next(currentPosition);
		}

		if (found) {
			return m.start();
		}

		return NOT_FOUND;
	}

	public int begin() {
		if (found) {
			return m.start();
		}

		return NOT_FOUND;
	}


	public int end() {
		if (found) {
			return m.end();
		}

		return NOT_FOUND;
	}

	private void next(int position) {
		this.position = position;
		found = m.find(position);
	}

}
