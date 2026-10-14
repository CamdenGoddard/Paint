package com.mycompany.paint;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A small web server that shows the images the user chose to share.
 *
 * <ul>
 *   <li>{@code /} is a web page listing every shared image. When nothing is
 *       shared it says so instead.</li>
 *   <li>{@code /image/0}, {@code /image/1}, ... are the pictures themselves, as PNG.</li>
 * </ul>
 *
 * <p>The server only answers to this computer ("localhost"), so nobody else
 * on the network can look at the pictures. It asks an {@link ImageSource}
 * for the current images on every request, so the page always shows the
 * latest drawing. The page also reloads itself every few seconds.</p>
 */
final class ImageWebServer {

    /** Where the server gets the images it shows. */
    interface ImageSource {
        /** @return the names of the images currently shared, in display order */
        List<String> names();

        /**
         * @param index position in {@link #names()}
         * @return the image as PNG bytes, or null if there is no such image
         */
        byte[] png(int index);
    }

    private final ImageSource source;
    private HttpServer server;

    /** @param source where to get the shared images */
    ImageWebServer(ImageSource source) {
        this.source = source;
    }

    /**
     * Starts listening on this computer only.
     *
     * @param port the port to use; 0 picks any free port
     * @throws IOException if the port is already taken or cannot be opened
     */
    void start(int port) throws IOException {
        if (server != null) {
            return;
        }
        HttpServer created = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        created.createContext("/image/", this::handleImage);
        created.createContext("/", this::handlePage);
        created.start();
        server = created;
    }

    /** Stops the server. Safe to call when it is not running. */
    void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /** @return true while the server is listening */
    boolean isRunning() {
        return server != null;
    }

    /** @return the port actually in use, or -1 when stopped */
    int getPort() {
        return (server == null) ? -1 : server.getAddress().getPort();
    }

    /** Builds the home page: one picture per shared image, or a "nothing shared" note. */
    private void handlePage(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/")) {
            send(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8",
                buildPage(source.names()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builds the HTML for the home page.
     *
     * @param names the names of the shared images (may be empty)
     * @return a complete HTML page
     */
    static String buildPage(List<String> names) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
                .append("<meta http-equiv=\"refresh\" content=\"5\">")
                .append("<title>Camden's Pain(t) - shared images</title>")
                .append("<style>body{font-family:sans-serif;margin:24px}")
                .append("img{max-width:100%;border:1px solid #888;margin-bottom:6px}")
                .append("figure{margin:0 0 24px 0}</style></head><body>")
                .append("<h1>Camden's Pain(t)</h1>");
        if (names.isEmpty()) {
            html.append("<p>No images are being shared right now. ")
                    .append("In the program, open a tab and turn on \"Share this tab\".</p>");
        } else {
            for (int i = 0; i < names.size(); i++) {
                html.append("<figure><img src=\"/image/").append(i).append("\" alt=\"")
                        .append(escape(names.get(i))).append("\"><figcaption>")
                        .append(escape(names.get(i))).append("</figcaption></figure>");
            }
        }
        return html.append("</body></html>").toString();
    }

    /** Sends one picture as PNG, or a 404 if that number is not shared. */
    private void handleImage(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        byte[] bytes = null;
        try {
            int index = Integer.parseInt(path.substring("/image/".length()));
            bytes = source.png(index);
        } catch (NumberFormatException e) {
            // falls through to the 404 below
        }
        if (bytes == null) {
            send(exchange, 404, "text/plain; charset=utf-8", "No such image".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(exchange, 200, "image/png", bytes);
    }

    private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    /** Escapes the characters that mean something in HTML. */
    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
