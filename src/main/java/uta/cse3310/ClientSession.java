package uta.cse3310;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class ClientSession {
    private static final String DATA_FILE = "/tmp/group_5.txt";

    // read/write functions for the tmp file
    public void saveMessage(String msg) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE, true))) {
			bw.write(msg);
            bw.newLine();
		} catch (IOException e) {
            System.out.println("error saving messages: " + e);
        }
    }
    public List<String> getPrevMessages() {
        List<String> prevMessages = new ArrayList<>();
        File file = new File(DATA_FILE);

        if (!file.exists()) return prevMessages;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            do {
				line = br.readLine(); // Do while loop: if line is not null, add to the list of lines
				if (line == null) break;
                if (!line.trim().isEmpty()) prevMessages.add(line);
			} while (line != null);
        } catch (IOException e) {
            System.out.println("error getting past messages: "+e);
        }
        return prevMessages;
    }
}