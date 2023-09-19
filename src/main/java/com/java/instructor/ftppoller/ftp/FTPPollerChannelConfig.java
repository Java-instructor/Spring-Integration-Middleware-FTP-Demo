package com.java.instructor.ftppoller.ftp;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.support.PeriodicTrigger;

@Configuration
public class FTPPollerChannelConfig {

	private final Logger log = LoggerFactory.getLogger(FTPPollerChannelConfig.class);

	private final String REMOTE_GET_DIRECTORY = "1";
	private static String LOCAL_DOWNLOAD_DIRECTORY;
	private final String FILE_FILTER_RULE = "*.xml";
	private final String POLLER_FIX_RATE = "5000";
	static {
		String currentPath = Paths.get("").toAbsolutePath().toString();
		LOCAL_DOWNLOAD_DIRECTORY = currentPath + File.separator + "ftpdownload";
	}

	@Bean
	public PollableChannel ftpChannel() {
		QueueChannel channel = new QueueChannel();
		return channel;
	}

	@Bean
	public PollableChannel toFtpChannel() {
		QueueChannel channel = new QueueChannel();
		return channel;
	}

	@Bean
	public PollableChannel testLogHandlerChannel() {
		QueueChannel channel = new QueueChannel();
		return channel;
	}

	// https://dlptest.com/ftp-test/ connection details from here.
	// test https://ftptest.net/#result
	@Bean
	public SessionFactory<FTPFile> ftpSessionFactory() {
		DefaultFtpSessionFactory sf = new DefaultFtpSessionFactory();
		sf.setHost("ftp.dlptest.com");
		sf.setUsername("dlpuser");
		sf.setPassword("rNrKYTX9g7z3RgJRmxWuGHbeu");
		sf.setPort(21);
		return new CachingSessionFactory<FTPFile>(sf);
	}

	@Bean
	public FtpInboundFileSynchronizer ftpInboundFileSynchronizer(SessionFactory<FTPFile> ftpSessionFactory) {
		// FtpInboundFileSynchronizer fileSynchronizer = new
		// FtpInboundFileSynchronizer(ftpSessionFactory());
		FtpInboundFileSynchronizer fileSynchronizer = new FtpInboundFileSynchronizer(ftpSessionFactory);
		fileSynchronizer.setDeleteRemoteFiles(false);
		fileSynchronizer.setRemoteDirectory(REMOTE_GET_DIRECTORY);
		fileSynchronizer.setFilter(new FtpSimplePatternFileListFilter(FILE_FILTER_RULE));
		return fileSynchronizer;
	}

	@Bean
	@InboundChannelAdapter(value = "ftpChannel", poller = @Poller(fixedRate = POLLER_FIX_RATE))
	public MessageSource<File> ftpMessageSource(FtpInboundFileSynchronizer ftpInboundFileSynchronizer) {
		// FtpInboundFileSynchronizingMessageSource source = new
		// FtpInboundFileSynchronizingMessageSource(ftpInboundFileSynchronizer());
		FtpInboundFileSynchronizingMessageSource source = new FtpInboundFileSynchronizingMessageSource(
				ftpInboundFileSynchronizer);
		source.setLocalDirectory(new File(LOCAL_DOWNLOAD_DIRECTORY));
		source.setAutoCreateLocalDirectory(true);
		source.setLocalFilter(new AcceptOnceFileListFilter<>());
		source.setMaxFetchSize(10);
		return source;
	}

	@Bean
	@ServiceActivator(inputChannel = "ftpChannel", outputChannel = "testLogHandlerChannel")
	public MessageHandler inBoundHandler() {
		return new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				System.out.println(message.getPayload());
			}

		};
	}

	@Bean
	public FtpRemoteFileTemplate template() {
		return new FtpRemoteFileTemplate(ftpSessionFactory());
	}

	@Bean
	@ServiceActivator(inputChannel = "testLogHandlerChannel")
	public LoggingHandler logging1() {
		LoggingHandler adapter = new LoggingHandler(Level.DEBUG);
		adapter.setLoggerName("LogHandler1");
		adapter.setLogExpressionString("'log msg: ' + payload");
		return adapter;
	}

	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	public PollerMetadata defaultPoller() {

		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setTrigger(new PeriodicTrigger(1000));
		return pollerMetadata;
	}

}
