package de.itzrealshiki.provisuals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MusicMediaControls {
    public enum Action { PREVIOUS, PLAY_PAUSE, NEXT }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ProVisuals-Media-Control");
        thread.setDaemon(true);
        return thread;
    });

    private MusicMediaControls() {}

    public static void send(Action action) {
        WORKER.submit(() -> {
            try {
                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.startsWith("windows")) windows(action);
                else if (os.contains("linux") || os.contains("bsd")) linux(action);
                else Diagnostics.warn("Playback controls are not supported on this operating system.");
            } catch (Exception exception) {
                Diagnostics.error("Media control failed: " + action, exception);
            }
        });
    }

    private static void windows(Action action) throws IOException, InterruptedException {
        int key = switch (action) {
            case PREVIOUS -> 0xB1;
            case PLAY_PAUSE -> 0xB3;
            case NEXT -> 0xB0;
        };
        String source = "using System; using System.Runtime.InteropServices; public static class PvMediaKey { " +
                "[DllImport(\"user32.dll\")] public static extern void keybd_event(byte key, byte scan, uint flags, UIntPtr extra); }";
        String command = "Add-Type -TypeDefinition '" + source + "'; " +
                "[PvMediaKey]::keybd_event([byte]" + key + ",0,0,[UIntPtr]::Zero); " +
                "Start-Sleep -Milliseconds 70; " +
                "[PvMediaKey]::keybd_event([byte]" + key + ",0,2,[UIntPtr]::Zero)";
        Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(4, TimeUnit.SECONDS)) process.destroyForcibly();
        else if (process.exitValue() != 0) throw new IOException("Windows media key returned " + process.exitValue());
    }

    private static void linux(Action action) throws IOException, InterruptedException {
        String names = run(List.of("gdbus", "call", "--session", "--dest", "org.freedesktop.DBus",
                "--object-path", "/org/freedesktop/DBus", "--method", "org.freedesktop.DBus.ListNames"));
        Matcher matcher = Pattern.compile("'(org\\.mpris\\.MediaPlayer2\\.[^']+)'").matcher(names);
        List<String> players = new ArrayList<>();
        while (matcher.find()) players.add(matcher.group(1));
        String selected = null;
        for (String player : players) {
            String status = run(List.of("gdbus", "call", "--session", "--dest", player,
                    "--object-path", "/org/mpris/MediaPlayer2", "--method",
                    "org.freedesktop.DBus.Properties.Get", "org.mpris.MediaPlayer2.Player", "PlaybackStatus"));
            if (status.contains("Playing")) { selected = player; break; }
            if (selected == null) selected = player;
        }
        if (selected == null) throw new IOException("No MPRIS media player found.");
        String method = switch (action) {
            case PREVIOUS -> "Previous";
            case PLAY_PAUSE -> "PlayPause";
            case NEXT -> "Next";
        };
        run(List.of("gdbus", "call", "--session", "--dest", selected, "--object-path", "/org/mpris/MediaPlayer2",
                "--method", "org.mpris.MediaPlayer2.Player." + method));
    }

    private static String run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        byte[] output = process.getInputStream().readAllBytes();
        if (!process.waitFor(4, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Media command timed out.");
        }
        if (process.exitValue() != 0) throw new IOException("Media command returned " + process.exitValue());
        return new String(output, StandardCharsets.UTF_8);
    }
}
