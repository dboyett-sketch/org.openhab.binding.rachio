package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import static org.openhab.binding.rachio.RachioBindingConstants.SERVLET_IMAGE_PATH;

/**
 * The {@link RachioImageServlet} serves zone images from Rachio CDN
 *
 * @author Markus Michels - Initial contribution
 */
@Component(service = RachioImageServlet.class, immediate = true)
@NonNullByDefault
public class RachioImageServlet extends HttpServlet {
    private static final long serialVersionUID = 468411145487381L;
    private final Logger logger = LoggerFactory.getLogger(RachioImageServlet.class);

    private final HttpService httpService;
    private @Nullable RachioBridgeHandler bridgeHandler;

    @Activate
    public RachioImageServlet(@Reference HttpService httpService) {
        this.httpService = httpService;
        try {
            httpService.registerServlet(SERVLET_IMAGE_PATH, this, null, null);
            logger.debug("RachioImageServlet started at {}", SERVLET_IMAGE_PATH);
        } catch (ServletException | NamespaceException e) {
            logger.error("Error starting RachioImageServlet: {}", e.getMessage());
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            httpService.unregister(SERVLET_IMAGE_PATH);
            logger.debug("RachioImageServlet stopped");
        } catch (IllegalArgumentException e) {
            logger.debug("RachioImageServlet was not registered");
        }
    }

    public void injectBridgeHandler(RachioBridgeHandler handler) {
        this.bridgeHandler = handler;
        logger.debug("RachioImageServlet: Bridge handler injected");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String imageUrl = req.getParameter("url");
            if (imageUrl == null || imageUrl.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image URL parameter");
                return;
            }

            logger.debug("RachioImageServlet: Serving image from {}", imageUrl);
            
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            resp.setContentType(connection.getContentType());
            resp.setContentLength(connection.getContentLength());
            
            try (var inputStream = connection.getInputStream();
                 var outputStream = resp.getOutputStream()) {
                inputStream.transferTo(outputStream);
            }
            
            logger.trace("RachioImageServlet: Image served successfully");

        } catch (Exception e) {
            logger.error("RachioImageServlet: Error serving image: {}", e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving image");
        }
    }

    @Override
    public void destroy() {
        deactivate();
        super.destroy();
    }
}
