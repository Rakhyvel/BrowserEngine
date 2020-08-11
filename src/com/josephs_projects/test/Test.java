package com.josephs_projects.test;

import java.io.IOException;

import com.josephs_projects.browserEngine.Document;

class Test {

	public static void main(String[] args) {
		try {
			Document doc = new Document("http://josephs-projects.com");
			doc.domTree.print("|");
			doc.equals(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
