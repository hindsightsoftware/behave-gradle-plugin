package com.hindsighttesting.behave.gradle;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class FeaturesTask extends DefaultTask {

    private String server;
    private String projectKey;
    private String username;
    private String password;
    private boolean includeManual;
    private String destinationDir;
    private String httpProxyAddress = "";
    private String httpProxyPort = "";
    private String httpProxyUsername = "";
    private String httpProxyPassword = "";
    private boolean useProxy = false;
    private boolean useProxyAuth = false;

    @TaskAction
    void doTask() {
        String urlPath = String.format("%s/rest/cucumber/1.0/project/%s/features?manual=%b", server.replaceAll("/$", ""), projectKey, includeManual);
        String filePath = System.getProperty("user.dir") + "/" + destinationDir.replaceAll("/$", "");

        new File(filePath).mkdirs();
        File features = new File(filePath + "/features.zip");

        if (httpProxyAddress.isEmpty() || httpProxyPort.isEmpty()) {
            useProxy = true;
        }

        if (httpProxyUsername.isEmpty() || httpProxyPassword.isEmpty()) {
            useProxyAuth = true;
        }

        downloadFile(urlPath, features);
        unzipFeatures(features);
        removeZip(features);
    }

    void downloadFile(String urlPath, File features) {
        URL url = null;

        try {
            url = new URL(urlPath);
        } catch (MalformedURLException e) {
            System.out.println("URL is not valid");
            e.printStackTrace();
            System.exit(1);
        }

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();

            if (useProxy) {
                System.setProperty("http.proxyHost", httpProxyAddress);
                System.setProperty("http.proxyPort", httpProxyPort);
                System.setProperty("https.proxyHost", httpProxyAddress);
                System.setProperty("https.proxyPort", httpProxyPort);
                if (useProxyAuth) {
                    String encoded = Base64.getEncoder().encodeToString((httpProxyUsername + ":" + httpProxyPassword).getBytes(StandardCharsets.UTF_8));
                    connection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
                    Authenticator.setDefault(new ProxyAuth(httpProxyUsername, httpProxyPassword));
                }
            }

            String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encoded);

            InputStream in = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(features.getAbsolutePath());

            byte[] b = new byte[1024];
            int count;
            while ((count = in.read(b)) >= 0) {
                out.write(b, 0, count);
            }

            out.flush();
            out.close();
            in.close();
        } catch (IOException e) {
            switch(getResponseCode(connection)) {
                case 401:
                    System.out.println("Username or password incorrect");
                    break;
                case 403:
                    System.out.println("Too many login failures. Please try again later");
                    break;
                case 404:
                    System.out.println("Project cannot be found");
                    break;
                case 405:
                case 406:
                    System.out.println("The version of Behave is not compatible with this version of the plugin");
                    break;
                default:
                    System.out.println("Error downloading features");
            }
            e.printStackTrace();
            System.exit(1);
        }
    }

    int getResponseCode(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch(IOException e) {
            return 500;
        }
    }

    void unzipFeatures(File features) {
        try {
            ZipFile zipFile = new ZipFile(features.getAbsolutePath());
            zipFile.extractAll(features.getParent());
        } catch (ZipException e) {
            System.out.println("Error unzipping features");
            e.printStackTrace();
            System.exit(1);
        }
    }

    void removeZip(File features) {
        features.delete();
    }

    /* Gradle Getters & Setters */

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isIncludeManual() {
        return includeManual;
    }

    public void setIncludeManual(boolean includeManual) {
        this.includeManual = includeManual;
    }

    public String getDestinationDir() {
        return destinationDir;
    }

    public void setDestinationDir(String destinationDir) {
        this.destinationDir = destinationDir;
    }

    public String getHttpProxyAddress() {
        return httpProxyAddress;
    }

    public void setHttpProxyAddress(String httpProxyAddress) {
        this.httpProxyAddress = httpProxyAddress;
    }

    public String getHttpProxyPort() {
        return httpProxyPort;
    }

    public void setHttpProxyPort(String httpProxyPort) {
        this.httpProxyPort = httpProxyPort;
    }

    public String getHttpProxyUsername() {
        return httpProxyUsername;
    }

    public void setHttpProxyUsername(String httpProxyUsername) {
        this.httpProxyUsername = httpProxyUsername;
    }

    public String getHttpProxyPassword() {
        return httpProxyPassword;
    }

    public void setHttpProxyPassword(String httpProxyPassword) {
        this.httpProxyPassword = httpProxyPassword;
    }

    /* Proxy Authenticator class */

    public class ProxyAuth extends Authenticator {
        private PasswordAuthentication auth;

        private ProxyAuth(String user, String password) {
            auth = new PasswordAuthentication(user, password == null ? new char[]{} : password.toCharArray());
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return auth;
        }
    }
}

