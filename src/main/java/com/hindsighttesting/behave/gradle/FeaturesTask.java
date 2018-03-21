package com.hindsighttesting.behave.gradle;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class FeaturesTask extends DefaultTask {

    private String server;
    private String projectKey;
    private String username;
    private String password;
    private boolean includeManual;
    private String destinationDir;
    private String httpProxyAddress;
    private String httpProxyUsername;
    private String httpProxyPassword;

    @TaskAction
    void doTask() {
        String urlPath = String.format("%s/rest/cucumber/1.0/project/%s/features?manual=%b", server.replaceAll("/$", ""), projectKey, includeManual);
        String filePath = System.getProperty("user.dir") + "/" + destinationDir.replaceAll("/$", "");

        new File(filePath).mkdirs();
        File features = new File(filePath + "/features.zip");

        try {
            downloadFile(urlPath, features);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        unzipFeatures(features);
        removeZip(features);
    }

    public void downloadFile(String urlPath, File features) throws MalformedURLException {
        URL url = new URL(urlPath);

        try {
            URLConnection conn = url.openConnection();
            String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encoded);
            InputStream in = conn.getInputStream();

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
            e.printStackTrace();
        }
    }

    public void unzipFeatures(File features) {
        try {
            ZipFile zipFile = new ZipFile(features.getAbsolutePath());
            zipFile.extractAll(features.getParent());
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    public void removeZip(File features) {
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
}
