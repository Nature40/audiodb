package audio;

public class Terminal {	
	static { // needs to be positioned as first entry in class!
		System.setProperty("java.awt.headless", "true");
	}

	public static void main(String[] args) throws Exception {
		audio.server.Webserver.main(args);
	}

}
