package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.rachio.internal.handler.RachioHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for serving Rachio device and zone images
 */
@Component(service = HttpServlet.class, property = {"alias=/rachio/image", "servlet-name=RachioImageServlet"})
@NonNullByDefault
public class RachioImageServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(RachioImageServlet.class);
    private final Set<RachioHandler> handlers = ConcurrentHashMap.newKeySet();

    @Activate
    public RachioImageServlet() {
        logger.debug("RachioImageServlet activated");
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addRachioHandler(RachioHandler handler) {
        handlers.add(handler);
        logger.debug("RachioHandler registered for image serving: {}", handler.getThing().getUID());
    }

    public void removeRachioHandler(RachioHandler handler) {
        handlers.remove(handler);
        logger.debug("RachioHandler unregistered from image serving: {}", handler.getThing().getUID());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String pathInfo = req.getPathInfo();
            logger.debug("Image request received: {}", pathInfo);
            
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
                logger.warn("Invalid image request path: {}", pathInfo);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // Extract image ID from path (remove leading slash)
            String imageId = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
            
            if (imageId.isEmpty()) {
                logger.warn("Empty image ID in request");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // Try to find a handler that can serve this image
            boolean handled = false;
            for (RachioHandler handler : handlers) {
                try {
                    handler.handleImageCall(req, resp);
                    // If the response is committed, the handler served the image
                    if (resp.isCommitted()) {
                        handled = true;
                        logger.debug("Image served successfully by handler for image ID: {}", imageId);
                        break;
                    }
                } catch (Exception e) {
                    logger.debug("Handler failed to serve image {}, trying next handler: {}", imageId, e.getMessage());
                    // Continue to next handler
                }
            }
            
            if (!handled) {
                logger.warn("No handler could serve image: {}", imageId);
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            
        } catch (Exception e) {
            logger.error("Error serving image: {}", e.getMessage(), e);
            if (!resp.isCommitted()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
    
    /**
     * Get the number of registered handlers (for debugging)
     */
    public int getHandlerCount() {
        return handlers.size();
    }
}
