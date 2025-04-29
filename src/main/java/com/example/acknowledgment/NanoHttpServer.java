package com.example.acknowledgment;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;

public class NanoHttpServer extends NanoHTTPD {
	private final MongoService mongoService;
	private boolean serverRunning = false;

	public NanoHttpServer(int port, MongoService mongoService) {
		super(port);
		this.mongoService = mongoService;
	}

	@Override
	public Response serve(IHTTPSession session) {
		String uri = session.getUri();

		if (session.getMethod() == Method.GET && uri.equals("/acknowledge")) {
			Map<String, String> params = session.getParms();
			String email = params.get("email");
			if (email != null && !email.isEmpty()) {

				// Check if already acknowledged
				if (mongoService.isAcknowledged(email)) {
					return newFixedLengthResponse("This email has already been acknowledged.");
				}

				return newFixedLengthResponse("<html><body>" + "<form method='POST' action='/acknowledge'>"
						+ "<input type='hidden' name='email' value='" + email + "'/>"
						+ "<input type='checkbox' name='confirm'/> I acknowledge<br/>"
						+ "<input type='submit' value='Submit'/>" + "</form></body></html>");
			} else {
				return newFixedLengthResponse("Email is missing in the request.");
			}
		}

		// Handle form submission (POST request)
		if (session.getMethod() == Method.POST && uri.equals("/acknowledge")) {
			try {
				session.parseBody(null);
				String email = session.getParms().get("email");
				String confirm = session.getParms().get("confirm");

				if (email != null && confirm != null) {
					// Debugging logs
					System.out.println("Received email: " + email);
					System.out.println("Checkbox checked: " + confirm);

					mongoService.acknowledge(email);
					return newFixedLengthResponse("Acknowledged! Thank you.");
				} else {
					return newFixedLengthResponse("Missing confirmation or email.");
				}
			} catch (Exception e) {
				e.printStackTrace();
				return newFixedLengthResponse("Error processing request.");
			}
		}

		return newFixedLengthResponse("Unsupported request.");
	}

	@Override
	public void start() throws IOException {
		super.start();
		serverRunning = true; // Set the flag when the server is started
	}

	public void stopServer() {
		if (serverRunning) {
			// Gracefully stop the server if it's running
			stop();
			serverRunning = false;
		}
	}

	public boolean isServerRunning() {
		return serverRunning;
	}

	public boolean isPortAvailable(int port) {
		try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
			socket.setReuseAddress(true); // Allow re-binding to the address if already bound
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	
}
