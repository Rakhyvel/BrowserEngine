package com.josephs_projects.browserEngine;

public class Token {
	public enum Type {
		STARTOPEN, END, STARTCLOSE, TEXT, LBRACE, RBRACE, SEMICOLON
	}
	
	Type type;
	String data;
	
	public Token(Type type, String data) {
		this.type = type;
		this.data = data;
	}
	
	@Override
	public String toString() {
		return data;
	}
}