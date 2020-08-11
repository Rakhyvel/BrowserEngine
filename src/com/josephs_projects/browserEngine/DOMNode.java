package com.josephs_projects.browserEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class DOMNode {
	ArrayList<Object> children = new ArrayList<>();
	HashMap<String, String> attributes = new HashMap<>();

	public DOMNode(String data) {
		Queue<String> tokens = removeWhiteSpace(data);
		attributes.put("tagname", tokens.remove());
		
		// Goes through tokens, maps values to attr. names
		while (tokens.size() >= 3) {
			String attributeName = tokens.remove();
			String attributeValue = "";

			tokens.remove(); // Remove the =
			if (tokens.peek().equals("\"")) { // Check if quotation
				tokens.remove(); // Remove the begin "
				while (!tokens.isEmpty() && !tokens.peek().equals("\"")) {
					attributeValue += tokens.remove(); // Add all between quotes
				}
				tokens.poll(); // Remove the end "
			} else {
				attributeValue += tokens.remove(); // Add next after =
			}
			attributes.put(attributeName, attributeValue);
		}
	}

	/**
	 * Split string on spaces, equal signs and quotations. Add all tokens to new
	 * list without whitespace, unless between quotations
	 * 
	 * @param data Attribute string
	 * @return List of tokens without whitespace except between quotes
	 */
	public Queue<String> removeWhiteSpace(String data) {
		LinkedList<String> retval = new LinkedList<>();
		String[] array = data.split("(?=[ \\=\"])|(?<=[ \\=\"])");
		boolean quotation = false;
		
		for (int i = 0; i < array.length; i++) {
			quotation = array[i].equals("\"");
			if (!array[i].equals(" ") || quotation) {
				retval.add(array[i]);
			}
		}
		
		return (Queue<String>) retval;
	}
	
	public String tagname() {
		return attributes.get("tagname");
	}

	public void print(String buffer) {
		System.out.println(buffer + "<" + attributes.get("tagname") + " " + attributes + ">");
		buffer += ".  ";
		for (Object child : children) {
			if (child instanceof String) {
				System.out.println(buffer.replace("-", " ") + (String) child);
			} else if (child instanceof DOMNode) {
				((DOMNode) child).print(buffer);
			}
		}
		System.out.println(buffer.substring(0, buffer.length() - 3) + "end " + attributes.get("tagname"));
	}
}
