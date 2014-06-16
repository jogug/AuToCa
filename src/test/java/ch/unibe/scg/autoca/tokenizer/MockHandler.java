package ch.unibe.scg.autoca.tokenizer;
import java.util.ArrayList;
import java.util.List;

import ch.unibe.scg.autoca.tokenizer.TokenType;
import ch.unibe.scg.autoca.tokenizer.TokenizerHandler;


public class MockHandler implements TokenizerHandler {
	public List<String> tokens = new ArrayList<String>();
	public List<TokenType> types = new ArrayList<TokenType>();
	
	@Override
	public void token(String token, TokenType type) {
		tokens.add(token);
		types.add(type);
	}

}
