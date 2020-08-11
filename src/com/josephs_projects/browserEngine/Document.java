package com.josephs_projects.browserEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Document {
	public String filename;
	public DOMNode domTree;
	static List<String> emptyTags = Arrays
			.asList(new String[] { "area", "base", "basefront", "col", "frame", "hr", "input", "link", "isindex",
					"!doctype", "!--", "meta", "img", "br", "source", "!", "!--", "!--[if", "![endif]--" });

	public Document(String filename) throws IOException {
		this.filename = filename;
		// Download and tokenize HTML, parse DOM tree
		this.domTree = parseDOMTree(tokenizeHTML(downloadDocument("https://eclipse.com")));
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

	/**
	 * Turns HTML document into tokens which are easier to manipulate
	 * 
	 * @param text Text document
	 * @return Queue of tokens
	 */
	private Queue<Token> tokenizeHTML(String text) {
		Queue<Token> tokens = new LinkedList<>();
		String[] split = text.split("(?<=(<\\/)|<|>)|(?=(<\\/)|<|>)");
		for (int i = 0; i < split.length; i++) {
			String token = split[i];
			switch (token) {
			// Start of tag
			case "<":
				if (split[i + 1].equals("/")) {
					tokens.add(new Token(Token.Type.STARTCLOSE, "</"));
					i++;
				} else {
					tokens.add(new Token(Token.Type.STARTOPEN, "<"));
				}
				break;
			// End of tag
			case ">":
				tokens.add(new Token(Token.Type.END, ">"));
				break;
			default:
				if (token.replace("\t", "").replace(" ", "").length() > 0)
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

		// If reach !doctype tag, just go to the html tag
		if (root.tagname().toLowerCase().equals("!doctype") || root.tagname().toLowerCase().equals("!--[if")) {
			tokens.remove(); // Remove the <
			root = parseDOMTree(tokens);
		}

		// If empty tag, don't go through adding children
		if (emptyTags.contains(root.tagname().toLowerCase())) {
			return root;
		}

		// Comment tag
		if (root.tagname().contains("!")) {
			return root;
		}

		while (!tokens.isEmpty()) {
			Token token = tokens.remove();
			if (token.type == Token.Type.STARTCLOSE && tokens.peek().data.equals(root.tagname())) {
				tokens.remove(); // Remove the data
				tokens.remove(); // Remove the >
				return root;
			} else if (token.type == Token.Type.TEXT || root.tagname().equals("script")
					|| root.tagname().equals("style")) {
				// Add text, script is always text
				root.children.add(token.data);
			} else if (token.type == Token.Type.STARTOPEN) {
				root.children.add(parseDOMTree(tokens));
			} else {
				System.out.println("END token found where it was not expected " + root.tagname());
			}
		}
		return root;
	}
}
