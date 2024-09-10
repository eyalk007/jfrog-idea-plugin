package com.jfrog.ide.idea.log;

import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.utils.HttpClientWrapper;
import com.jfrog.ide.idea.utils.Utils;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;

public class Analytics {
    private final String XSC_MIN_VERSION = "1.7.1";
    private final ServerConfigImpl serverConfig = GlobalSettings.getInstance().getServerConfig();
    private final String baseApiUrl = serverConfig.getUrl() + "/xsc/api/v1";
    private final String eventEndpoint = baseApiUrl + "/event";
    private final HttpClientWrapper httpClient;
    private String xscVersion = null;
    private Date startTime = new Date();


    public Analytics() {
        HttpClientWrapper.HttpConfig config = new HttpClientWrapper.HttpConfig(
                this.serverConfig.getUrl(),
                this.serverConfig.getUsername(),
                this.serverConfig.getPassword(),
                this.serverConfig.getAccessToken()
        );
        this.httpClient = new HttpClientWrapper(config, null);
        this.setXscVersion();
    }

    public void setXscVersion() {
        try {
            String response = this.httpClient.doGetRequest(baseApiUrl + "/system/version", null).body();
            this.xscVersion = this.extractValueFromResponse(response, "xsc_version");
        } catch (IOException | InterruptedException e) {
            this.xscVersion = "";
            System.out.println(e.getMessage());
        }

    }

    public void endScan(boolean isScanSuccessful, Integer totalFindings, Integer totalIgnoredFindings, String multiScanId ) {
        Integer totalScanTime = Math.toIntExact(new Date().getTime() - this.startTime.getTime());
        ScanEventStatus eventStatus = isScanSuccessful ? ScanEventStatus.COMPLETED : ScanEventStatus.FAILED;
        ScanRequest endScanRequest = new EndScanRequest(
                ScanEventType.SOURCE_CODE,
                eventStatus,
                Utils.getPluginName(),
                Utils.getPluginVersion(),
                "3.6.4",
                this.serverConfig.getUsername(),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                this.getMachineId(),
                Utils.AM_VERSION,
                false,
                totalFindings,
                totalIgnoredFindings,
                totalScanTime,
                multiScanId
        );

        HttpClientWrapper.RequestParams params = new HttpClientWrapper.RequestParams(
                eventEndpoint,
                "PUT",
                endScanRequest + multiScanId
        );
        try {
            String a = this.httpClient.doRequest(params).body();
        }
        catch(IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public String startScan() throws IOException, InterruptedException {

        ScanRequest startScanRequest = new ScanRequest(
                ScanEventType.SOURCE_CODE,
                ScanEventStatus.STARTED,
                Utils.getPluginName(),
                Utils.getPluginVersion(),
                "3.6.4",
                this.serverConfig.getUsername(),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                this.getMachineId(),
                Utils.AM_VERSION,
                false
        );


        HttpClientWrapper.RequestParams params = new HttpClientWrapper.RequestParams(
                eventEndpoint,
                "POST",
                startScanRequest.toString()
        );

        if (this.isVersionGreaterOrEqual(this.xscVersion, XSC_MIN_VERSION)) {
            String response = httpClient.doRequest(params).body();
            this.startTime = new Date();
            return this.extractValueFromResponse(response, "multi_scan_id");
        }
        System.out.println("Sorry, your version is unsupported");
        return null;
    }

    private String getMachineId() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                byte[] hardwareAddress = networkInterface.getHardwareAddress();

                if (hardwareAddress != null) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hardwareAddress) {
                        sb.append(String.format("%02X:", b));
                    }

                    // Remove trailing colon
                    if (sb.length() > 0) {
                        sb.setLength(sb.length() - 1);
                    }

                    return sb.toString();
                }
            }
        }
        catch (SocketException e) {
            System.out.println(e.getMessage());
        }
        return "";
    }

    private String extractValueFromResponse(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            return null; // Key not found
        }
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return null;
        }
        return json.substring(startIndex, endIndex);
    }


    private boolean isVersionGreaterOrEqual(String version, String minVersion) {
        String[] versionParts = version.split("\\.");
        String[] minVersionParts = minVersion.split("\\.");

        for (int i = 0; i < Math.max(versionParts.length, minVersionParts.length); i++) {
            int vPart = i < versionParts.length ? Integer.parseInt(versionParts[i]) : 0;
            int mvPart = i < minVersionParts.length ? Integer.parseInt(minVersionParts[i]) : 0;

            if (vPart > mvPart) {
                return true;
            } else if (vPart < mvPart) {
                return false;
            }
        }
        return true; // They are equal
    }
}

