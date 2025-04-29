package com.example.acknowledgment;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.mail.*;
import javax.mail.internet.*;
import com.mongodb.client.*;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;

public class EmailService {

	// private final String fromEmail = "bikashmohantymca@gmail.com";
	// private final String password = "xxx";
	// private final Properties appProperties;
	private final String tunnelUrl; // <-- NEW: Save the tunnel URL at object creation!

	public EmailService() {
		// appProperties = loadAppProperties();

		tunnelUrl = startFreshCloudflareTunnel();

		if (tunnelUrl == null) {
			System.out.println("Could not get Cloudflare tunnel URL. Email not sent.");
			return;
		}

//        // Start Cloudflare tunnel once during object initialization
//        try {
//            startFreshCloudflareTunnel();
//            tunnelUrl = getCloudflareTunnelUrl();
//            if (tunnelUrl == null) {
//                System.out.println("Tunnel URL could not be initialized.");
//            } else {
//                System.out.println("Tunnel URL initialized: " + tunnelUrl);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Failed to initialize tunnel", e);
//        }
	}

	public void sendAcknowledgmentEmail(String toEmail) {
		try {
			if (tunnelUrl == null) {
				System.out.println("Tunnel URL is not available. Email not sent.");
				return;
			}

			Properties props = new Properties();
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", "587");

			Session session = Session.getInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(MainApp.loadAppProperties().getProperty("fromEmail"),
							MainApp.loadAppProperties().getProperty("password"));
				}
			});

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(MainApp.loadAppProperties().getProperty("fromEmail")));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
			message.setSubject("Submit Your Timesheet");

			String htmlContent = "<html><body>" + "Please acknowledge by clicking the link: " + "<a href=\"" + tunnelUrl
					+ "/acknowledge?email=" + toEmail + "\">Click Here</a>" + "</body></html>";

			message.setContent(htmlContent, "text/html; charset=utf-8");
			Transport.send(message);

			System.out.println("Email sent to: " + toEmail);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//    private Properties loadAppProperties() {
//        Properties props = new Properties();
//        try (InputStream input = new FileInputStream("config.properties")) {
//            props.load(input);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        return props;
//    }

	private void startFreshCloudflareTunnel3() throws IOException, InterruptedException {
		killOldCloudflareProcesses();

		String cloudflarePath = MainApp.loadAppProperties().getProperty("cloudflare_path");
		if (cloudflarePath == null || cloudflarePath.isEmpty()) {
			throw new IllegalStateException("cloudflare.path is not configured.");
		}

		ProcessBuilder pb = new ProcessBuilder(cloudflarePath, "tunnel", "--url", "http://localhost:8080");
		pb.redirectErrorStream(true);
		pb.start();
		System.out.println("Started new Cloudflare tunnel...");
		Thread.sleep(8000); // <-- increased sleep time to make sure tunnel gets ready
	}

	private void killOldCloudflareProcesses() {
		try {
			String command = "taskkill /F /IM cloudflared-windows-amd64.exe";
			Runtime.getRuntime().exec(command);
			System.out.println("Killed old cloudflared process if running.");
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getCloudflareTunnelUrl() {
		try {
			Process process = Runtime.getRuntime().exec("curl http://127.0.0.1:4040/api/tunnels");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			StringBuilder response = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				response.append(line);
			}

			String json = response.toString();
			int urlStart = json.indexOf("\"public_url\":\"https://") + 15;
			int urlEnd = json.indexOf("\"", urlStart);
			if (urlStart > 14 && urlEnd > urlStart) {
				return json.substring(urlStart, urlEnd);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void resetAcknowledgedFlags() {
		if ("true".equalsIgnoreCase(MainApp.loadAppProperties().getProperty("reset_records"))) {
			try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
				MongoDatabase db = mongoClient.getDatabase("acknowledgment");
				MongoCollection<Document> collection = db.getCollection("records");
				collection.updateMany(new Document(), new Document("$set", new Document("acknowledged", false)));
				System.out.println("Acknowledged flags reset to false.");
			}
		}
	}

	public List<String> retrievePendingEmails() {
		List<String> emails = new ArrayList<>();
		if ("true".equalsIgnoreCase(MainApp.loadAppProperties().getProperty("retrieve_records"))) {
			try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
				MongoDatabase db = mongoClient.getDatabase("acknowledgment");
				MongoCollection<Document> collection = db.getCollection("records");
				for (Document doc : collection.find(eq("acknowledged", false))) {
					emails.add(doc.getString("email"));
				}
				System.out.println("Retrieved pending emails: " + emails);
			}
		}
		return emails;
	}

	private String startFreshCloudflareTunnel() {
		try {
			// 1. Kill old cloudflared if running
			killCloudflared();

			// 2. Start new cloudflared process
			ProcessBuilder builder = new ProcessBuilder(MainApp.loadAppProperties().getProperty("cloudflare_path"),
					"tunnel", "--url", "http://localhost:8080");
			builder.redirectErrorStream(true); // merge stdout and stderr
			Process process = builder.start();

			// 3. Read output to get URL
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			String tunnelUrl = null;

			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<String> future = executor.submit(() -> {
				String outputLine;
				while ((outputLine = reader.readLine()) != null) {
					if (outputLine.contains("trycloudflare.com")) {
						int start = outputLine.indexOf("https://");
						int end = outputLine.indexOf(".trycloudflare.com") + ".trycloudflare.com".length();
						if (start != -1 && end != -1) {
							return outputLine.substring(start, end);
						}
					}
				}
				return null;
			});

			try {
				// Wait max 10 seconds for tunnel to come up
				tunnelUrl = future.get(10, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				System.out.println("Timed out waiting for tunnel URL.");
				process.destroy();
			} finally {
				executor.shutdown();
			}

			return tunnelUrl;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void killCloudflared() {
		try {
			ProcessBuilder builder = new ProcessBuilder("taskkill", "/F", "/IM", "cloudflared.exe");
			Process process = builder.start();
			process.waitFor();
			System.out.println("Killed old cloudflared process if running.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendEmailToManager(String toEmail, List<String> emails) {
		try {
			if (tunnelUrl == null) {
				System.out.println("Tunnel URL is not available. Email not sent.");
				return;
			}

			Properties props = new Properties();
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", "587");

			Session session = Session.getInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(MainApp.loadAppProperties().getProperty("fromEmail"),
							MainApp.loadAppProperties().getProperty("password"));
				}
			});

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(MainApp.loadAppProperties().getProperty("fromEmail")));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
			message.setSubject("Not Submitted Timesheet");

			// Build the email list as HTML
			StringBuilder emailListHtml = new StringBuilder();
			emailListHtml.append("<ul>");
			for (String email : emails) {
				emailListHtml.append("<li>").append(email).append("</li>");
			}
			emailListHtml.append("</ul>");
			String htmlContent = "";
			if (emails.size() == 0) {
				htmlContent = "<html><body>" + "All have submitted their timesheets.<br><br>" + "</body></html>";
			} else {
				htmlContent = "<html><body>"
						+ "Please find below the list of emails that have not submitted their timesheets:<br><br>"
						+ emailListHtml + "</body></html>";
			}

			message.setContent(htmlContent, "text/html; charset=utf-8");
			Transport.send(message);

			System.out.println("Email sent to: " + toEmail);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
