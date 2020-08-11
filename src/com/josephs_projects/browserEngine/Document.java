package com.josephs_projects.browserEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

public class Document {
	private String filename;
	private DOMNode domTree;
	
	public Document(String filename) throws IOException {
		this.filename = filename;
		// Download and tokenize HTML, parse DOM tree
		this.domTree = parseDOMTree(tokenizeHTML("<html></html>"));
		// CSSOM Tree
		// Layout Tree
		// Layout Calculation
	}
	
	private String downloadDocument(String path) throws IOException {
		String html = "";
		URL url = new URL(path);
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			html += inputLine;
		}
		in.close();

		return html;
	}
	
	private Queue<Token> tokenizeHTML(String text) {
		Queue<Token> tokens = new LinkedList<>();
		String[] split = text.split("(?=[\\</>])|(?<=[\\</>])");
		for(int i = 0; i < split.length; i++) {
			String token = split[i];
			switch(token) {
			case "<":
				if(split[i + 1].equals("/")) {
					tokens.add(new Token(Token.Type.STARTCLOSE, "</"));
					i++;
				} else {
					tokens.add(new Token(Token.Type.STARTOPEN, "<"));
				}
				break;
			case ">":
				tokens.add(new Token(Token.Type.END, ">"));
				break;
			default:
				tokens.add(new Token(Token.Type.TEXT, token));
				break;
			}
		}
		tokens.remove();
		return tokens;
	}
	
	private DOMNode parseDOMTree(Queue<Token> tokens) {
		// Next token is node info
		DOMNode root = new DOMNode(tokens.remove().data);

		tokens.remove(); // Remove the >
		
		while(!tokens.isEmpty()) {
			Token token = tokens.remove();
			switch(token.type) {
			case TEXT:
				root.children.add(token.data);
				break;
			case STARTOPEN:
				root.children.add(parseDOMTree(tokens));
				break;
			case STARTCLOSE:
				tokens.remove(); // Remove the data
				tokens.remove(); // Remove the >
				return root;
			default:
				System.out.println("UNWANTED TOKEN TYPE " + token.type);
			}
		}
		return root;
	}
}
