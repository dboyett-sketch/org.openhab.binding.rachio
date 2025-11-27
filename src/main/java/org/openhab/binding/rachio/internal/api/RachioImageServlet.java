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

@Component(service = HttpServlet.class, property = {"alias=/rachio/image", "servlet-name=RachioImageServlet"})
@NonNullByDefault
public class RachioImageServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(RachioImageServlet.class);
    private RachioHandler rachioHandler;

    @Activate
    public RachioImageServlet() {
        logger.debug("RachioImageServlet activated");
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setRachioHandler(RachioHandler handler) {
        this.rachioHandler = handler;
        logger.debug("RachioHandler set in ImageServlet");
    }

    public void unsetRachioHandler(RachioHandler handler) {
        if (this.rachioHandler == handler) {
            this.rachioHandler = null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (rachioHandler != null) {
            rachioHandler.handleImageCall(req, resp);
        } else {
            logger.warn("Image request received but no handler available");
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }
}
