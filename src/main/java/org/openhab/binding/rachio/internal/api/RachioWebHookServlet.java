package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
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

@Component(service = HttpServlet.class, property = {"alias=/rachio/webhook", "servlet-name=RachioWebHookServlet"})
@NonNullByDefault
public class RachioWebHookServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);
    private RachioHandler rachioHandler;

    @Activate
    public RachioWebHookServlet() {
        logger.debug("RachioWebHookServlet activated");
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setRachioHandler(RachioHandler handler) {
        this.rachioHandler = handler;
        logger.debug("RachioHandler set in WebHookServlet");
    }

    public void unsetRachioHandler(RachioHandler handler) {
        if (this.rachioHandler == handler) {
            this.rachioHandler = null;
            logger.debug("RachioHandler unset from WebHookServlet");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (rachioHandler != null) {
            rachioHandler.handleWebhookCall(req);
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            logger.warn("RachioWebHookServlet received webhook but no handler available");
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }
}
