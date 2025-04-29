package com.example.acknowledgment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainApp {
	public static void main(String[] args) throws Exception {
		EmailListProvider emailProvider = new EmailListProvider("emails.csv");
		MongoService mongoService = new MongoService();
		EmailService emailService = new EmailService();
		NanoHttpServer server = new NanoHttpServer(8080, mongoService);

		// Verify if the server is running before starting
		if (server.isServerRunning()) {
			System.out.println("Server is already running. Stopping it before restarting...");
			server.stopServer(); // Stop the server if already running
		} else {
			System.out.println("Server is not running. Starting the server...");
		}

		// Start the server
		server.start();
		// System.out.println("Server started at http://localhost:8080/");

		if (shouldRetrieveRecords()) {
			System.out.println("Not submitted timesheet :");
			List<String> emailsList = new ArrayList<String>();
			for (String email : emailProvider.getEmails()) {
				if (!mongoService.isAcknowledged(email)) {
					System.out.println(email);
					emailsList.add(email);

				}
			}
			emailService.sendEmailToManager(loadAppProperties().getProperty("manager_email"), emailsList);
			
			return;
		}

		if (shouldResetRecords()) {
			for (String email : emailProvider.getEmails()) {
				mongoService.resetEmail(email);
			}
			Thread.sleep(2000);
		}

		// Process emails and send acknowledgment emails if not already acknowledged
		for (String email : emailProvider.getEmails()) {
			if (!mongoService.isAcknowledged(email)) {
				System.out.println("Sending acknowledgment email to: " + email);
				emailService.sendAcknowledgmentEmail(email);
				// mongoService.acknowledge(email);
				System.out.println("Acknowledgment sent to: " + email);
			} else {
				System.out.println("Email already acknowledged: " + email);
			}
		}

		// Keep main thread alive for 2 hours (2 * 60 * 60 * 1000 ms)
		// Thread.sleep(2 * 60 * 60 * 1000);
		int getExpireTime = Integer.parseInt(loadAppProperties().getProperty("expire_time_in_seconds"));
		Thread.sleep(getExpireTime * 1000);

		server.stopServer();
		// Keep the main thread running to allow the server to handle incoming requests
		// Thread.currentThread().join();
	}

	private static boolean shouldResetRecords() {
		Properties props = new Properties();
		try (InputStream input = new FileInputStream("config.properties")) {
			props.load(input);
			return Boolean.parseBoolean(props.getProperty("reset_records", "false"));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static boolean shouldRetrieveRecords() {
		Properties props = new Properties();
		try (InputStream input = new FileInputStream("config.properties")) {
			props.load(input);
			return Boolean.parseBoolean(props.getProperty("retrieve_records", "false"));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static Properties loadAppProperties() {
		Properties props = new Properties();
		try (InputStream input = new FileInputStream("config.properties")) {
			props.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return props;
	}
}
