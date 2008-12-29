/*******************************************************************************
 * Copyright (c) 2008 Laurent Petit.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *    Laurent PETIT - initial API and implementation
 *******************************************************************************/
package clojuredev.utils.editors.antlrbased;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.Token;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;

import clojuredev.ClojuredevPlugin;
import clojuredev.editors.antlrbased.TokenData;

abstract public class AntlrBasedTokenScanner implements ITokenScanner {
	private static int ANTLR_EOF = -1;
	private Lexer lexer;
	private final List<TokenData> tokensData;
	private int currentTokenIndex;
	private final Map<Integer, IToken> antlrTokenTypeToJFaceToken;
	private String text;
	private boolean initialized = false;

	public AntlrBasedTokenScanner(Lexer lexer) {
		this.lexer = lexer;
		
		tokensData = new ArrayList<TokenData>();
		
		antlrTokenTypeToJFaceToken = new HashMap<Integer, IToken>();
		initAntlrTokenTypeToJFaceTokenMap();
		antlrTokenTypeToJFaceToken.put(ANTLR_EOF, org.eclipse.jface.text.rules.Token.EOF);
		initialized = true;
	}
	
	abstract protected void initAntlrTokenTypeToJFaceTokenMap();
	
	public final void addTokenType(int tokenIndex, org.eclipse.jface.text.rules.Token token) {
		if (initialized) throw lifeCycleError();
		antlrTokenTypeToJFaceToken.put(tokenIndex, token);
	}
	
	public final void addTokenType(int tokenIndex, TextAttribute textAttribute) {
		if (initialized) throw lifeCycleError();
		addTokenType(tokenIndex, new org.eclipse.jface.text.rules.Token(textAttribute));
	}

	public final void addToken(int tokenIndex, String tokenData) {
		if (initialized) throw lifeCycleError();
		addTokenType(tokenIndex, new org.eclipse.jface.text.rules.Token(tokenData));
	}
	
	private RuntimeException lifeCycleError() {
		return new RuntimeException("Object Lifecycle error: method called at an inappropriate time");
	}

	public final int getTokenLength() {
		return tokensData.get(currentTokenIndex).length;
	}

	public final int getTokenOffset() {
		return tokensData.get(currentTokenIndex).offset;
	}

	public final IToken nextToken() {
		int nextIndex = currentTokenIndex + 1;
		if ( nextIndex >= tokensData.size() ) {
			return org.eclipse.jface.text.rules.Token.EOF;
		}
		currentTokenIndex = nextIndex;
		TokenData token = tokensData.get(currentTokenIndex);
		if( token != null ){
			return token.iToken;
		} else {
			ClojuredevPlugin.logError("nextToken called but null token retrieved ? ! Returning UNDEFINED");
			return org.eclipse.jface.text.rules.Token.UNDEFINED;
		}
	}

	public final void setRange(IDocument document, int offset, int length) {
		if (!document.get().equals(text)) {
			tokensData.clear();
			currentTokenIndex = -1;
			text = document.get();
			
			lexer.setCharStream(new ANTLRStringStream(text));

			while (true) {
				Token token = lexer.nextToken();
				if( token.getType() == ANTLR_EOF ){
					break;
				}
				addTokenInfo((CommonToken) token);
			}
		}
		repositionCurrentTokenAtOffset(offset);
	}

	private void repositionCurrentTokenAtOffset(int offset) {
		for( int i = tokensData.size() - 1 ; i >= 0 ; i-- ){
			TokenData tokenInfo = tokensData.get(i);
			if( tokenInfo.offset < offset ){
				currentTokenIndex = i;
				break;
			}
		}
	}

	private void addTokenInfo(CommonToken token){
		assert token != null;
		IToken retToken = antlrTokenTypeToJFaceToken.get(token.getType());
		if( retToken == null ) {
			retToken = org.eclipse.jface.text.rules.Token.UNDEFINED; 
		}
		tokensData.add(new TokenData(token, retToken));
	}
}
