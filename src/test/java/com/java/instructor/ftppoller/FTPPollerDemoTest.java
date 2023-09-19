package com.java.instructor.ftppoller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class FTPPollerDemoTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(FTPPollerDemoTest.class);

	@Autowired
	private ApplicationContext applicationContext;

	public static String server = "ftp.dlptest.com";
	public static int port = 21;
	public static String user = "dlpuser";
	public static String pass = "rNrKYTX9g7z3RgJRmxWuGHbeu";

	public static String TEST_FILE_DIR;

	// preparing the directories structure
	@BeforeClass
	public static void prepareDirectoryStructure() throws IOException {
		String currentPath = Paths.get("").toAbsolutePath().toString();
		TEST_FILE_DIR = currentPath + File.separator + "testfiles";
	}

	// Cleanup directories
	@AfterClass
	public static void cleanup() throws IOException {
	}

	@Test(timeout = 10000) // timeout may differ number of channels flows the messages.
	public void applicationSetupTest() throws Exception {

	}

	@Test(timeout = 30000) // timeout may differ number of channels flows the messages.
	public void copyFileToFTPSourceDirAndTest() throws Exception {
		PollableChannel testLogHandlerChannel = applicationContext.getBean("testLogHandlerChannel",
				PollableChannel.class);

		FTPClient ftpClient = new FTPClient();
		try {

			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
			ftpClient.enterLocalPassiveMode();

			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			// LOGGER.info("Listing FTP files before uploading...");
			// listDirectory(ftpClient, "", "", 0);
			File firstLocalFile = new File(TEST_FILE_DIR + File.separator + "empRequest.xml");

			String firstRemoteFile = "1/empRequest.xml";
			InputStream inputStream = new FileInputStream(firstLocalFile);

			LOGGER.info("Start uploading file");
			boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
			inputStream.close();
			if (done) {
				LOGGER.info("file uploaded successfully.");
			}
			// LOGGER.info("Listing FTP files after uploaded...");
			listDirectory(ftpClient, "", "", 0);
		} catch (IOException ex) {
			System.out.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.logout();
					ftpClient.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		Message<?> msg = testLogHandlerChannel.receive();
		LOGGER.info(" Junit test verification for payload ::" + msg.getPayload());
		assertThat(msg.getPayload(), is(notNullValue()));

	}

	public void listDirectory(FTPClient ftpClient, String parentDir, String currentDir, int level) throws IOException {
		String dirToList = parentDir;
		if (!currentDir.equals("")) {
			dirToList += "/" + currentDir;
		}
		FTPFile[] subFiles = ftpClient.listFiles(dirToList);
		if (subFiles != null && subFiles.length > 0) {
			for (FTPFile aFile : subFiles) {
				String currentFileName = aFile.getName();
				if (currentFileName.equals(".") || currentFileName.equals("..")) {
					// skip parent directory and directory itself
					continue;
				}
				for (int i = 0; i < level; i++) {
					System.out.print("\t");
				}
				if (aFile.isDirectory()) {
					System.out.println("[" + currentFileName + "]");
					listDirectory(ftpClient, dirToList, currentFileName, level + 1);
				} else {
					System.out.println(currentFileName);
				}
			}
		}
	}

}
