package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

import static org.openhab.binding.rachio.RachioBindingConstants.SERVLET_WEBHOOK_PATH;

/**
 * The {@link RachioWebHookServlet} handles webhook callbacks from Rachio cloud
 *
 * @author Markus Michels - Initial contribution
 */
@Component(service = RachioWebHookServlet.class, immediate = true)
@NonNullByDefault
public class RachioWebHookServlet extends HttpServlet {
    private static final long serialVersionUID = 468411145487382L;
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);

    private final HttpService httpService;
    private @Nullable RachioBridgeHandler bridgeHandler;

    @Activate
    public RachioWebHookServlet(@Reference HttpService httpService) {
        this.httpService = httpService;
        try {
            httpService.registerServlet(SERVLET_WEBHOOK_PATH, this, null, null);
            logger.debug("RachioWebHookServlet started at {}", SERVLET_WEBHOOK_PATH);
        } catch (ServletException | NamespaceException e) {
            logger.error("Error starting RachioWebHookServlet: {}", e.getMessage());
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            httpService.unregister(SERVLET_WEBHOOK_PATH);
            logger.debug("RachioWebHookServlet stopped");
        } catch (IllegalArgumentException e) {
            logger.debug("RachioWebHookServlet was not registered");
        }
    }

    public void injectBridgeHandler(RachioBridgeHandler handler) {
        this.bridgeHandler = handler;
        logger.debug("RachioWebHookServlet: Bridge handler injected");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Read the JSON payload
            StringBuilder payload = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    payload.append(line);
                }
            }

            String jsonPayload = payload.toString();
            logger.debug("RachioWebHookServlet: Received webhook payload: {}", jsonPayload);

            // Parse the event
            RachioEvent event = RachioEvent.fromJson(jsonPayload);
            if (event == null) {
                logger.warn("RachioWebHookServlet: Unable to parse webhook payload");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Process the event through the bridge handler
            RachioBridgeHandler localBridgeHandler = bridgeHandler;
            if (localBridgeHandler != null) {
                localBridgeHandler.webHookEvent(event);
                resp.setStatus(HttpServletResponse.SC_OK);
                logger.trace("RachioWebHookServlet: Webhook event processed successfully");
            } else {
                logger.warn("RachioWebHookServlet: No bridge handler available");
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }

        } catch (Exception e) {
            logger.error("RachioWebHookServlet: Error processing webhook: {}", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void destroy() {
        deactivate();
        super.destroy();
    }
}
