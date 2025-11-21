package org.openhab.binding.rachio.internal.api;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openhab.binding.rachio.handler.RachioBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { Servlet.class, RachioImageServletService.class }, property = {
    "osgi.http.whiteboard.servlet.pattern=/rachio/image",
    "osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=default)"
})
public class RachioImageServlet extends HttpServlet implements RachioImageServletService {
    private static final long serialVersionUID = 8706067059503685993L;
    private final Logger imageLogger = LoggerFactory.getLogger(RachioImageServlet.class);

    private RachioBridgeHandler rachioBridgeHandler;

    @Activate
    protected void activate(Map<String, Object> config) {
        imageLogger.debug("RachioImageServlet activated");
    }

    @Deactivate
    protected void deactivate() {
        imageLogger.debug("RachioImageServlet deactivated");
    }

    public void injectBridgeHandler(RachioBridgeHandler handler) {
        this.rachioBridgeHandler = handler;
        imageLogger.debug("RachioImage: BridgeHandler manually injected");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        InputStream reader = null;
        OutputStream writer = null;
        try {
            String ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            String path = request.getRequestURI().substring(0, SERVLET_IMAGE_PATH.length());
            imageLogger.trace("RachioImage: Request from {}:{}{} ({}:{}, {})", ipAddress, request.getRemotePort(), path,
                    request.getRemoteHost(), request.getServerPort(), request.getProtocol());

            if (!request.getMethod().equalsIgnoreCase(HTTP_METHOD_GET)) {
                imageLogger.error("RachioImage: Unexpected method='{}'", request.getMethod());
                return;
            }
            if (!path.equalsIgnoreCase(SERVLET_IMAGE_PATH)) {
                imageLogger.error("RachioImage: Invalid request received - path = {}", path);
                return;
            }

            String uri = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/") + 1);
            String imageUrl = SERVLET_IMAGE_URL_BASE + uri;
            imageLogger.debug("RachioImage: {} image '{}' from '{}'", request.getMethod(), uri, imageUrl);
            setHeaders(resp);

            URL url = new URL(imageUrl);
            URLConnection conn = url.openConnection();
            conn.setDoInput(true);
            reader = conn.getInputStream();
            writer = resp.getOutputStream();

            byte[] data = new byte[4096];
            int n;
            while ((n = reader.read(data)) != -1) {
                writer.write(data, 0, n);
            }

        } catch (Exception e) {
            imageLogger.error("RachioImage: Unable to process request: {}", e.getMessage());
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void setHeaders(HttpServletResponse response) {
        response.setContentType(SERVLET_IMAGE_MIME_TYPE);
        response.setHeader("Access-Control-Allow-Origin", "*");
    }
}