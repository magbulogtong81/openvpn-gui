/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package openvpn.gui;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 *
 * @author jil
 */
public class ExecuteCommand {

    private String result;
    private String queryResultPath;

    /**
     * will execute the given command, and returns the result.
     *
     * @param command the command to execute
     * @return the output of command
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public void executeCommand(String command) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ProcessBuilder builder = new ProcessBuilder();

        if (detecOS().matches("Windows")) {
            this.showDownloadLink("There's a better GUI app for Windows.\n"
                    + "click or open the link to download it.", 
                    "https://openvpn.net/client-connect-vpn-for-windows/",
                    "https://openvpn.net/client-connect-vpn-for-windows/");
            System.exit(0);

        } else {
            builder.command("sh", "-c", command);
            builder.directory(new File(System.getProperty("user.home")));
            Process process = builder.start();
            StreamGobbler streamGobbler
                    = new StreamGobbler(process.getInputStream());
            Future<?> future = Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();
            assert exitCode == 0;
            future.get(10, TimeUnit.SECONDS);
            result = streamGobbler.getResult();
            queryResultPath = streamGobbler.getQueryResultPath();

        }
    }

    public void showDownloadLink(String message, String link, String linkName) {
        JEditorPane ep = new JEditorPane("text/html",
                message
                + "<a href=\"" + link + "\">"
                + linkName + "</a>" //
                + "</body></html>");

        // handle link events
        ep.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI()); // roll your own link launcher or use Desktop if J6+
                    System.exit(0);
                } catch (URISyntaxException | IOException ex) {
                    Logger.getLogger(ExecuteCommand.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        ep.setEditable(false);
        JOptionPane.showMessageDialog(null, ep);
    }

    public String detecOS() {
        return System.getProperty("os.name");
    }

    /**
     *
     * will get the command output with newline carriage (\n) for readability
     *
     * @return command output.
     */
    public String getResult() {
        return this.result;
    }

    /**
     * same as getResult() without newline carriage (\n). intended to fetch the
     * openvpn path
     *
     * @return command output, same as getResult(), intended to get openvpn path
     */
    public String getQueryResultPath() {
        return this.queryResultPath;
    }

    public String detectPath() {
        String path = null;
        if (System.getProperty("os.name").equals("Linux")) {
            try {
                this.executeCommand("which openvpn3");
                if (queryResultPath.isEmpty()) {

                    this.showDownloadLink("openvpn3 binary not found.\n"
                            + "click or copy the link for instruction on how to install it"
                            , "https://openvpn.net/cloud-docs/openvpn-3-client-for-linux/", 
                            "https://openvpn.net/cloud-docs/openvpn-3-client-for-linux/");
                    

                } else {
                    path = queryResultPath;
                }
            } catch (IOException | InterruptedException | ExecutionException | TimeoutException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return path;

    }

    /**
     * use this method to check if the program is properly executing commands
     * will print the current user's home directory list
     */
    public void listDirectory() {
        try {
            ExecuteCommand test = new ExecuteCommand();
            test.executeCommand("dir");
            System.out.println(result);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException ex) {
            Logger.getLogger(ExecuteCommand.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /*
     *
     * public static void main(String[] args) { ExecuteCommand test = new
     * ExecuteCommand(); test.ls(); test.du();
     *
     * }
     *
     */
}

class StreamGobbler implements Runnable {

    private InputStream inputStream;
    private String result;
    private String queryResultPath;
    //private Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream) {
        this.inputStream = inputStream;
        //this.consumer = consumer;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String response1 = new String();
            String response2 = new String();
            for (String line; (line = br.readLine()) != null; response2 += line + "\n") {
                response1 += line;
            }
            this.result = response2;
            this.queryResultPath = response1;

        } catch (IOException ex) {
            Logger.getLogger(StreamGobbler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String getResult() {
        return result;
    }

    public String getQueryResultPath() {
        return this.queryResultPath;
    }

}
