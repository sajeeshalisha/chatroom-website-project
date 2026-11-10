package uta.cse3310;

import java.util.HashMap;
import java.util.Scanner;

public class ClientID {
    private static int counter = 1;
    private static HashMap<String, Integer> users = new HashMap<>();

    private int id;
    private String username;

    public ClientID(String username) {
        this.username = username;
        this.id = counter;
        counter++;
    }

    public void displayID() {
        System.out.println("Welcome " + username + ", Your Unique Client ID is: " + id);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("Enter username (or type 'exit' to quit): ");
            String username = sc.nextLine();

            if (username.trim().isEmpty()) {
                System.out.println("Error: Username cannot be empty.");
                continue;
            }

            if (username.equalsIgnoreCase("exit")) {
                break;
            }

            if (users.containsKey(username)) {
                System.out.println("Error: Username already taken.");
                continue;
            }

            ClientID newUser = new ClientID(username);
            users.put(username, newUser.id);
            newUser.displayID();
        }

        sc.close();
    }
}