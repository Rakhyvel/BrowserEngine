package com.josephs_projects.test;

import java.io.IOException;

import com.josephs_projects.browserEngine.Document;

class Test {

	@org.junit.jupiter.api.Test
	void test() {
		try {
			Document doc = new Document("http://josephs-projects.com");
			doc.equals(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
