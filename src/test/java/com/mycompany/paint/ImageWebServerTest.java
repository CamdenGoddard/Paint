package com.mycompany.paint;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for the web server, using a real server on a free local port. */
public class ImageWebServerTest {

    private final List<String> names = new ArrayList<>();
    private final byte[] fakePng = { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3 };
    private ImageWebServer server;

    @Before
    public void startServer() throws IOException {
        server = new ImageWebServer(new ImageWebServer.ImageSource() {
            @Override
            public List<String> names() {
                return new ArrayList<>(names);
            }

            @Override
            public byte[] png(int index) {
                return (index >= 0 && index < names.size()) ? fakePng : null;
            }
        });
        server.start(0);
    }

    @After
    public void stopServer() {
        server.stop();
    }

    private HttpURLConnection get(String path) throws IOException {
        URL url = URI.create("http://localhost:" + server.getPort() + path).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        return connection;
    }

    private static byte[] readAll(HttpURLConnection connection) throws IOException {
        try (InputStream in = (connection.getResponseCode() < 400)
                ? connection.getInputStream() : connection.getErrorStream()) {
            return in.readAllBytes();
        }
    }

    @Test
    public void saysSoWhenNoImagesAreShared() throws IOException {
        HttpURLConnection connection = get("/");
        assertEquals(200, connection.getResponseCode());
        String page = new String(readAll(connection), StandardCharsets.UTF_8);
        assertTrue(page.contains("No images are being shared"));
        assertFalse(page.contains("<img"));
    }

    @Test
    public void listsEverySharedImageAndServesItsBytes() throws IOException {
        names.addAll(Arrays.asList("cat.png", "dog <1>.png"));

        String page = new String(readAll(get("/")), StandardCharsets.UTF_8);
        assertTrue(page.contains("src=\"/image/0\""));
        assertTrue(page.contains("src=\"/image/1\""));
        assertTrue(page.contains("cat.png"));
        assertTrue(page.contains("dog &lt;1&gt;.png"));   // names are escaped
        assertFalse(page.contains("No images are being shared"));

        HttpURLConnection image = get("/image/1");
        assertEquals(200, image.getResponseCode());
        assertEquals("image/png", image.getContentType());
        assertArrayEquals(fakePng, readAll(image));
    }

    @Test
    public void unknownImagesAndPagesAreNotFound() throws IOException {
        names.add("only.png");
        assertEquals(404, get("/image/5").getResponseCode());
        assertEquals(404, get("/image/abc").getResponseCode());
        assertEquals(404, get("/nothing-here").getResponseCode());
    }

    @Test
    public void startsAndStopsCleanly() throws IOException {
        assertTrue(server.isRunning());
        int port = server.getPort();
        assertTrue(port > 0);
        server.stop();
        assertFalse(server.isRunning());
        assertEquals(-1, server.getPort());
        server.start(0);
        assertTrue(server.isRunning());
    }
}
