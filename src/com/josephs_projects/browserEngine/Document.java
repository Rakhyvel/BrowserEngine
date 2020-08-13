package com.josephs_projects.browserEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Document {
	public String filename;
	public URL url;
	public DOMNode domTree;
	static List<String> emptyTags = Arrays
			.asList(new String[] { "area", "base", "basefront", "col", "frame", "hr", "input", "link", "isindex",
					"!doctype", "!--", "meta", "img", "br", "source", "!", "!--", "!--[if", "![endif]--" });
	public List<DOMNode> cssomCandidates = new ArrayList<>();
	public HashMap<String, HashMap<String, String>> cssRules = new HashMap<>();
	static HashMap<String, HashMap<String, String>> defaultCSS = new HashMap<>();

	static {
		InputStream in = Document.class.getResourceAsStream("/com/josephs_projects/browserEngine/defaults.css");
		String inputLine = "";
		String defaultCSSDoc = "";
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			while ((inputLine = reader.readLine()) != null) {
				defaultCSSDoc += inputLine;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		addRules(defaultCSS, defaultCSSDoc);
	}

	public Document(String filename) throws IOException {
		this.filename = filename;
		this.url = new URL(filename);
		// Download and tokenize HTML, parse DOM tree
		String document = downloadDocument(filename);
		if (!document.isEmpty()) {
			this.domTree = parseDOMTree(tokenizeHTML(cleanHTMLComments(document)));
			// CSSOM Tree
			constructCSSOM();
			// Layout Tree
			// Layout Calculation
		} else {
			System.out.println("Nothing to show!");
		}
	}

	private static String downloadDocument(String path) throws IOException {
		System.setProperty("http.agent", "Tangerine");
		char[] retval = new char[1024];
		int j = 0;
		URL url = new URL(path);
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			retval = reallocateArray(retval, j + inputLine.length());
			for (int i = 0; i < inputLine.length(); i++) {
				retval[j + i] = inputLine.charAt(i);
			}
			j += inputLine.length();
		}
		in.close();

		return new String(retval);
	}

	/**
	 * Use this method to reallocate arrays to a bigger size
	 * 
	 * @param array   Array to reallocate
	 * @param newSize Length of new stuff to add on
	 * @return Reference to new array, likely to be same reference
	 */
	private static char[] reallocateArray(char[] array, int newSize) {
		while (array.length < newSize) {
			char[] tempArray = new char[array.length * 2];
			for (int i = 0; i < array.length; i++) {
				tempArray[i] = array[i];
			}
			array = tempArray;
		}
		return array;
	}

	/**
	 * Removes pesky comments, including <!-- if[]> <endif --> tags
	 * 
	 * @param text Pre-comment text
	 * @return Text without comments
	 */
	private static String cleanHTMLComments(String text) {
		byte[] retval = new byte[text.length()];
		int j = 0; // retval byte length pointer
		boolean withinComment = false;
		// Go through each character, add character to retval unless its inside a
		// comment
		for (int i = 0; i < text.length(); i++) {
			if (!withinComment) {
				// Start comment
				if (i < text.length() - 3 && text.charAt(i) == '<' && text.charAt(i + 1) == '!'
						&& text.charAt(i + 2) == '-' && text.charAt(i + 3) == '-') {
					i += 3;
					withinComment = true;
				}
				// Regular text
				else {
					// I use a byte array here because it's faster than string contatenation
					// This way it doesn't allocate memory
					retval[j] = (byte) text.charAt(i);
					j++;
				}
			} else {
				// End comment
				if (i < text.length() - 2 && text.charAt(i) == '-' && text.charAt(i + 1) == '-'
						&& text.charAt(i + 2) == '>') {
					i += 2;
					withinComment = false;
				}
			}
		}
		return new String(retval);
	}

	/**
	 * Cleans up CSS documents and removes comments;
	 * 
	 * @param text Pre-comment text
	 * @return Post-comment text without comments
	 */
	private static String cleanCSSComments(String text) {
		byte[] retval = new byte[text.length()];
		int j = 0; // retval byte array pointer
		boolean withinComment = false;
		// Go through each character, add character to retval unless its inside a
		// comment
		for (int i = 0; i < text.length() - 1; i++) {
			if (!withinComment) {
				// Start comment
				if (text.charAt(i) == '/' && text.charAt(i + 1) == '*') {
					i += 1;
					withinComment = true;
				}
				// Regular text
				else {
					// I use a byte array here because it's faster than string contatenation
					// This way it doesn't allocate memory
					retval[j] = (byte) text.charAt(i);
					j++;
				}
			} else {
				// End comment
				if (text.charAt(i) == '*' && text.charAt(i + 1) == '/') {
					i += 1;
					withinComment = false;
				}
			}
		}
		return new String(retval);
	}

	/**
	 * Turns HTML document into tokens which are easier to manipulate
	 * 
	 * @param text Text document
	 * @return Queue of tokens
	 */
	private static Queue<Token> tokenizeHTML(String text) {
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
					tokens.add(new Token(Token.Type.TEXT, detab(token)));
				break;
			}
		}
		tokens.remove();
		return tokens;
	}

	// Replace rising edge of tab string with space, remove all tabs
	public static String detab(String s) {
		char[] retval = new char[s.length()];
		int j = 0;
		boolean withinTabString = false;

		for (int i = 0; i < s.length(); i++) {
			if (withinTabString) {
				if (s.charAt(i) != '\t' && s.charAt(i) != ' ' && s.charAt(i) != '\n') {
					withinTabString = false;
					retval[j] = s.charAt(i);
					j++;
				}
			} else {
				if (s.charAt(i) == '\t' || s.charAt(i) == ' ' || s.charAt(i) == '\n') {
					withinTabString = true;
					retval[j] = ' ';
					j++;
				} else {
					retval[j] = s.charAt(i);
					j++;
				}
			}
		}

		return new String(retval);
	}

	private static Queue<Token> tokenizeCSS(String text) {
		Queue<Token> tokens = new LinkedList<>();
		String[] split = text.split("(?<=(\\{|\\}|;))|(?=(\\{|\\}|;))");
		for (int i = 0; i < split.length; i++) {
			String token = split[i];
			switch (token) {
			case "{":
				tokens.add(new Token(Token.Type.LBRACE, "{"));
				break;
			case ";":
				tokens.add(new Token(Token.Type.SEMICOLON, ";"));
				break;
			case "}":
				tokens.add(new Token(Token.Type.RBRACE, "{"));
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
		if (root.tagname().toLowerCase().equals("!doctype")) {
			tokens.remove(); // Remove the <
			root = parseDOMTree(tokens);
		}

		if (root.tagname().equals("link") || root.tagname().equals("style"))
			cssomCandidates.add(root);

		// If empty tag, don't go through adding children
		if (emptyTags.contains(root.tagname().toLowerCase())) {
			return root;
		}

		while (!tokens.isEmpty()) {
			Token token = tokens.peek();
			switch (token.type) {
			case STARTCLOSE:
				// Normal end tag, matches begining
				if (((LinkedList<Token>) tokens).get(1).data.toLowerCase().equals(root.tagname())) {
					tokens.remove(); // Remove the </
					tokens.remove(); // Remove the data
					tokens.remove(); // Remove the >
					return root;
				}
				// End tag does not match
				// head, body, and html end tags skip to STARTOPEN case as they are 'protected'
				// Script and style skip to text, as they cannot hold inner DOM Nodes
				else if (!root.tagname().equals("script") && !root.tagname().equals("body")
						&& !root.tagname().equals("head") && !root.tagname().equals("html")) {
					return root;
				}
			case STARTOPEN:
				// Script and style do not have inner DOM nodes, they skip to TEXT case
				if (!root.tagname().equals("script") && !root.tagname().equals("style")) {
					tokens.remove();
					DOMNode node = parseDOMTree(tokens);
					root.children.add(node);
					break;
				}
			case TEXT:
				tokens.remove();
				// Combine text
				if (root.children.size() > 0 && root.children.get(root.children.size() - 1) instanceof String) {
					root.children.set(root.children.size() - 1,
							root.children.get(root.children.size() - 1) + token.data);
				}
				// Add text to end
				else {
					root.children.add(token.data);
				}
				break;
			default:
				tokens.remove();
				root.children.add(token.data);
			}
		}
		return root;
	}

	private void constructCSSOM() {
		cssRules.putAll(defaultCSS);
		for (DOMNode node : cssomCandidates) {
			switch (node.tagname()) {
			case "style":
				if (node.children.size() >= 1)
					addRules(cssRules, (String) node.children.get(0));
				break;
			case "link":
				String rel = node.attributes.get("rel");
				String path = "";
				if (rel == null || !rel.toLowerCase().equals("stylesheet")
						|| (path = node.attributes.get("href")) == null)
					break;

				try {
					path = cleanURL(path, url.getProtocol() + "://" + url.getHost());
					addRules(cssRules, downloadDocument(path));
				} catch (IOException e) {
				}
				break;
			}
		}
	}

	private static void addRules(HashMap<String, HashMap<String, String>> rules, String cssDocument) {
		Queue<Token> tokens = tokenizeCSS(cleanCSSComments(cssDocument));
		boolean addingRules = false;
		String owner = "";
		while (!tokens.isEmpty()) {
			Token currToken = tokens.remove();
			if (tokens.peek() == null || currToken == null)
				continue;
			if (!addingRules) {
				if (tokens.peek().type == Token.Type.LBRACE) {
					owner = currToken.data.trim();
					addingRules = true;
					ArrayList<String> owners = new ArrayList<>(Arrays.asList(owner.split(",")));
					for (String s : owners) {
						rules.put(s.trim(), new HashMap<String, String>());
					}
					tokens.remove(); // Get rid of opening bracket
				}
			} else {
				// Extract CSS property : value;
				// CSS is never important
				String[] property = currToken.data.replace("!important", "").trim().split(":");
				if (property.length < 2)
					continue;

				String key = property[0].trim();
				String value = property[1].trim();

				ArrayList<String> owners = new ArrayList<>(Arrays.asList(owner.split(",")));
				for (String s : owners) {
					rules.get(s.trim()).put(key, value);
				}
				tokens.remove(); // Get rid of semicolon

				if (tokens.size() > 2
						&& (currToken.type == Token.Type.RBRACE || tokens.peek().type == Token.Type.RBRACE)) {
					addingRules = false;
					owner = "";
				}
			}
		}
	}

	public static String cleanURL(String url, String domain) {
		if (!url.contains("://")) {
			if (url.length() > 0 && url.charAt(0) == '/' || domain.charAt(domain.length() - 1) == '/') {
				return domain + url;
			} else {
				return domain + "/" + url;
			}
		}
		return url;
	}

	// For testing purposes
	public void printCSSRules() {
		for (Map.Entry<String, HashMap<String, String>> rule : cssRules.entrySet()) {
			System.out.println(rule.getKey());
			for (Map.Entry<String, String> pair : rule.getValue().entrySet()) {
				System.out.println("    " + pair.getKey() + " : " + pair.getValue());
			}
			System.out.println();
			System.out.println();
		}
	}
}
