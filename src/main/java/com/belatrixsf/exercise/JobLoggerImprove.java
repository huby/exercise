package com.belatrixsf.exercise;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.*;

public class JobLoggerImprove {

    private static Logger logger = Logger.getLogger("com.belatrixsf.exercise");
    private static Properties properties = new Properties();
    private static final String SPACE = " ";

    public static final int CONSOLE_HANDLER_LOG = 1;
    public static final int FILE_HANDLER_LOG = 2;
    public static final int DATABASE_HANDLER_LOG = 3;

    public static final int WARNING_TYPE = 3;
    public static final int MESSAGE_TYPE = 1;
    public static final int ERROR_TYPE = 2;

    public static final String PREFIX_ERROR = "error";
    public static final String PREFIX_MESSAGE = "message";
    public static final String PREFIX_WARNING = "warning";

    private static final String SQL_STATEMENT = "INSERT INTO TABLE_LOG_MESSAGE(LOG_MESSAGE, LOG_TYPE) VALUES (?, ?)";

    static {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try(InputStream resourceStream = loader.getResourceAsStream("application.properties")) {
            properties.load(resourceStream);
        } catch (Exception e) {
            logger.severe("Error loading properties file");
        }
    }

    private JobLoggerImprove() {
        super();
    }

    public static void logMessage(String messageText) throws Exception {
        logMessage(CONSOLE_HANDLER_LOG, MESSAGE_TYPE, messageText);
    }

    public static void logMessage(int handlerLog, int logType, String messageText) throws Exception {

        Level level;
        String prefix;

        if (messageText == null || messageText.length() == 0) {
            throw new BlankMessageLogException("Message log must be not blank");
        }

        messageText = messageText.trim();

        if (logType == WARNING_TYPE) {
            level = Level.WARNING;
            prefix = PREFIX_WARNING;
        } else if (logType == MESSAGE_TYPE){
            level = Level.INFO;
            prefix = PREFIX_MESSAGE;
        } else if (logType == ERROR_TYPE) {
            level = Level.SEVERE;
            prefix = PREFIX_ERROR;
        } else {
            throw new IllegalArgumentException("Illegal log type argument");
        }



        if (DATABASE_HANDLER_LOG == handlerLog) {

            StringBuilder sb = new StringBuilder();
            sb.append(prefix);
            sb.append(SPACE);
            sb.append(DateFormat.getDateInstance(DateFormat.LONG).format(new Date()));
            sb.append(messageText);

            Connection connection = DriverManager.getConnection(properties.getProperty("database.url"), properties.getProperty("database.username"), properties.getProperty("database.password"));

            PreparedStatement ps = connection.prepareStatement(SQL_STATEMENT);
            ps.setString(1, sb.toString());
            ps.setInt(2, logType);
            ps.executeUpdate();

            ps.close();
            connection.close();

        } else if (FILE_HANDLER_LOG == handlerLog) {

            File logFile = new File(properties.getProperty("log.file.directory") + "/logFile.txt");

            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            FileHandler fh = new FileHandler(properties.getProperty("log.file.directory") + "/logFile.txt");

            fh.setEncoding("UTF-8");
            fh.setFormatter(new SimpleFormatter());

            logger.addHandler(fh);
            logger.log(level, messageText);

        } else if (CONSOLE_HANDLER_LOG == handlerLog) {

            ConsoleHandler ch = new ConsoleHandler();
            ch.setEncoding("UTF-8");
            ch.setFormatter(new SimpleFormatter());

            logger.addHandler(ch);
            logger.log(level, messageText);

        } else {
            throw new IllegalArgumentException("Illegal log handler argument");
        }

    }

}
