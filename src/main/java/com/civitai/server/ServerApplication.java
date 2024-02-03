package com.civitai.server;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.civitai.server.utils.ConfigUtils;

//TODO
//Change back to 3306 after finish testing

@SpringBootApplication
public class ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);

		//downloadFile("326326");

	}

	public static void downloadFile(String modelVersionId) {
		String apikey = "5da4bc786d73ff8b30d4defe45a33911";
		String urlString = "https://civitai.com/api/download/models/" + modelVersionId + "?token=" + apikey;
		URL url;
		HttpURLConnection connection = null;

		try {
			url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			String contentDisposition = connection.getHeaderField("Content-Disposition");
			String fileName = "";

			if (contentDisposition != null && contentDisposition.contains("filename=")) {
				fileName = contentDisposition.split("filename=")[1].replaceAll("\"", "");
			} else {
				// Fallback to default name or parse the URL
				fileName = "defaultFileName";
			}

			// Download the file
			try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
					FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
				byte dataBuffer[] = new byte[1024];
				int bytesRead;

				while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
					fileOutputStream.write(dataBuffer, 0, bytesRead);
				}
			}

			System.out.println("File downloaded: " + fileName);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
