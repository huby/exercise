package com.belatrixsf.exercise;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class JobLoggerImproveTest {

    private static final String CREATE_TABLE_LOG_MESSAGE = "CREATE TABLE TABLE_LOG_MESSAGE (LOG_ID INT AUTO_INCREMENT PRIMARY KEY NOT NULL, LOG_MESSAGE VARCHAR(250), LOG_TYPE INT)";
    private static final String DROP_TABLE_LOG_MESSAGE = "DROP TABLE TABLE_LOG_MESSAGE";

    private static Logger logger = Logger.getLogger("com.belatrixsf.exercise");
    private static OutputStream logCapturingStream;
    private static StreamHandler customLogHandler;
    private Connection connection;

    @Before
    public void setUp() throws Exception {
        logCapturingStream = new ByteArrayOutputStream();
        Handler[] handlers = logger.getParent().getHandlers();
        customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
        logger.addHandler(customLogHandler);

        DriverManager.registerDriver(new org.h2.Driver());
        this.connection = DriverManager.getConnection("jdbc:h2:mem:db_log", "root", "root");
        PreparedStatement stmt = this.connection.prepareStatement(CREATE_TABLE_LOG_MESSAGE);
        stmt.execute();
    }

    @Test(expected = BlankMessageLogException.class)
    public void logMessage_null_error() throws Exception {
        JobLoggerImprove.logMessage(JobLoggerImprove.CONSOLE_HANDLER_LOG, JobLoggerImprove.MESSAGE_TYPE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void logMessage_illegal_handlerLog_error() throws Exception {
        JobLoggerImprove.logMessage(10, JobLoggerImprove.MESSAGE_TYPE, "Log message testing");
    }

    @Test(expected = IllegalArgumentException.class)
    public void logMessage_illegal_message_error() throws Exception{
        JobLoggerImprove.logMessage(JobLoggerImprove.CONSOLE_HANDLER_LOG, 30, "Log message testing");
    }

    @Test
    public void logMessage_consoleHandler_messageType_thenCorrect() throws Exception{
        String message = "Log test message";
        JobLoggerImprove.logMessage(JobLoggerImprove.CONSOLE_HANDLER_LOG, JobLoggerImprove.MESSAGE_TYPE, message);
        String captureLog = getCapturedLog();
        Assert.assertTrue(captureLog.contains(Level.INFO.getLocalizedName()));
        Assert.assertTrue(captureLog.contains(message));
    }

    @Test
    public void logMessage_consoleHandler_warningType_thenCorrect() throws Exception{
        String message = "Log test warning";
        JobLoggerImprove.logMessage(JobLoggerImprove.CONSOLE_HANDLER_LOG, JobLoggerImprove.WARNING_TYPE, message);
        String captureLog = getCapturedLog();
        Assert.assertTrue(captureLog.contains(Level.WARNING.getLocalizedName()));
        Assert.assertTrue(captureLog.contains(message));
    }

    @Test
    public void logMessage_consoleHandler_errorType_thenCorrect() throws Exception{
        String message = "Log test error";
        JobLoggerImprove.logMessage(JobLoggerImprove.CONSOLE_HANDLER_LOG, JobLoggerImprove.ERROR_TYPE, message);
        String captureLog = getCapturedLog();
        Assert.assertTrue(captureLog.contains(Level.SEVERE.getLocalizedName()));
        Assert.assertTrue(captureLog.contains(message));
    }

    @Test
    public void logMessage_fileHandler_messageType_thenCorrect() throws Exception {
        String message = "Log test message";
        JobLoggerImprove.logMessage(JobLoggerImprove.FILE_HANDLER_LOG, JobLoggerImprove.MESSAGE_TYPE, message);
        String captureLog = getCapturedFileLog();
        Assert.assertTrue(captureLog.contains(Level.INFO.getLocalizedName()));
        Assert.assertTrue(captureLog.contains(message));
    }

    @Test
    public void logMessage_databaseHandler_messageType_theCorrect() throws Exception {
        String message = "Log test message";
        JobLoggerImprove.logMessage(JobLoggerImprove.DATABASE_HANDLER_LOG, JobLoggerImprove.MESSAGE_TYPE, message);
        String captureLog = getMessageFromDatabase();
        Assert.assertTrue(captureLog.contains(JobLoggerImprove.PREFIX_MESSAGE));
        Assert.assertTrue(captureLog.contains(message));
    }

    @Test
    public void logMessage_databaseHandler_warningType_theCorrect() throws Exception {
        String message = "Log test warning";
        JobLoggerImprove.logMessage(JobLoggerImprove.DATABASE_HANDLER_LOG, JobLoggerImprove.WARNING_TYPE, message);
        String captureLog = getMessageFromDatabase();
        Assert.assertTrue(captureLog.contains(JobLoggerImprove.PREFIX_WARNING));
        Assert.assertTrue(captureLog.contains(message));
    }

    @Test
    public void logMessage_databaseHandler_errorType_theCorrect() throws Exception {
        String message = "Log test error";
        JobLoggerImprove.logMessage(JobLoggerImprove.DATABASE_HANDLER_LOG, JobLoggerImprove.ERROR_TYPE, message);
        String captureLog = getMessageFromDatabase();
        Assert.assertTrue(captureLog.contains(JobLoggerImprove.PREFIX_ERROR));
        Assert.assertTrue(captureLog.contains(message));
    }

    private String getCapturedLog() throws IOException {
        customLogHandler.flush();
        return logCapturingStream.toString();
    }

    private String getCapturedFileLog() throws Exception {
        Charset charset = Charset.forName("UTF-8");
        List<String> result = Files.readAllLines(Paths.get("/Users/hectorhuby/Documents/logs/logFile.txt"), charset);
        return result.get(1);
    }

    private String getMessageFromDatabase() throws Exception {

        Statement stmt = connection.createStatement();
        String sql = "Select * from TABLE_LOG_MESSAGE";
        ResultSet rs = stmt.executeQuery(sql);

        String message = "";

        while(rs.next()){
            message  = rs.getString("LOG_MESSAGE");
            break;
        }

        stmt.close();
        return message;
    }

    @After
    public void destroy() throws Exception {

        PreparedStatement stmt = connection.prepareStatement(DROP_TABLE_LOG_MESSAGE);
        stmt.execute();
        stmt.close();
        connection.close();

    }

}
