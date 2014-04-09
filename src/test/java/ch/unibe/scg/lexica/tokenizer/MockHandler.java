package ch.unibe.scg.lexica.tokenizer;
import java.util.ArrayList;
import java.util.List;


public class MockHandler implements TokenizerHandler {
	public List<String> tokens = new ArrayList<String>();
	public List<TokenType> types = new ArrayList<TokenType>();
	
	@Override
	public void token(String token, TokenType type) {
		tokens.add(token);
		types.add(type);
	}

}
