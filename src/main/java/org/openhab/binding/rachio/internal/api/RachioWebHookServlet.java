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
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Servlet for handling Rachio webhook calls
 */
@Component(service = HttpServlet.class, property = {"alias=/rachio/webhook", "servlet-name=RachioWebHookServlet"})
@NonNullByDefault
public class RachioWebHookServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);
    private final Gson gson = new Gson();
    private final Set<RachioHandler> handlers = ConcurrentHashMap.newKeySet();

    @Activate
    public RachioWebHookServlet() {
        logger.debug("RachioWebHookServlet activated");
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addRachioHandler(RachioHandler handler) {
        handlers.add(handler);
        logger.debug("RachioHandler registered: {}", handler.getThing().getUID());
    }

    public void removeRachioHandler(RachioHandler handler) {
        handlers.remove(handler);
        logger.debug("RachioHandler unregistered: {}", handler.getThing().getUID());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String payload = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
            logger.debug("Received webhook: {}", payload);
            
            // Extract device ID from webhook payload
            String deviceId = extractDeviceIdFromPayload(payload);
            
            if (deviceId != null) {
                boolean handled = false;
                for (RachioHandler handler : handlers) {
                    if (handler.handlesDevice(deviceId)) {
                        handler.handleWebhookCall(req);
                        handled = true;
                        logger.debug("Webhook routed to handler for device: {}", deviceId);
                        break;
                    }
                }
                
                if (handled) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    logger.warn("No handler found for webhook device: {}", deviceId);
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                logger.warn("Webhook received without device ID");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    private String extractDeviceIdFromPayload(String payload) {
        try {
            RachioWebhookEvent event = gson.fromJson(payload, RachioWebhookEvent.class);
            return event != null ? event.deviceId : null;
        } catch (JsonSyntaxException e) {
            logger.debug("Failed to parse webhook payload: {}", e.getMessage());
            return null;
        }
    }
}
