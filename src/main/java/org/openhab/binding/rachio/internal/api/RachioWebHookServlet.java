package org.openhab.binding.rachio.internal.api;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.security.auth.x500.X500Principal;

import org.openhab.binding.rachio.handler.RachioBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@Component(service = { Servlet.class, RachioWebHookServletService.class }, property = {
    "osgi.http.whiteboard.servlet.pattern=/rachio/webhook",
    "osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=default)"
})
public class RachioWebHookServlet extends HttpServlet implements RachioWebHookServletService {
    private static final long serialVersionUID = -4654253998990066051L;
    private final Logger webhookLogger = LoggerFactory.getLogger(RachioWebHookServlet.class);
    private final Gson gson = new Gson();

    private RachioBridgeHandler rachioBridgeHandler;

    @Activate
    protected void activate(Map<String, Object> config) {
        webhookLogger.debug("RachioWebHookServlet activated");
    }

    @Deactivate
    protected void deactivate() {
        webhookLogger.debug("RachioWebHookServlet deactivated");
    }

    public void injectBridgeHandler(RachioBridgeHandler handler) {
        this.rachioBridgeHandler = handler;
        webhookLogger.debug("RachioWebHook: BridgeHandler manually injected");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        String data = inputStreamToString(request);
        try {
            String ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            String path = request.getRequestURI();

            webhookLogger.trace("RachioWebHook: Request from {}:{}{} ({}:{}, {})", ipAddress, request.getRemotePort(), path,
                    request.getRemoteHost(), request.getServerPort(), request.getProtocol());

            if (!path.equalsIgnoreCase(SERVLET_WEBHOOK_PATH)) {
                webhookLogger.error("RachioWebHook: Invalid request received - path = {}", path);
                return;
            }

            X509Certificate cert = extractCertificate(request);
            if (cert != null) {
                X500Principal principal = cert.getIssuerX500Principal();
                webhookLogger.debug("RachioEvent: Certificate from '{}'", principal.getName());
            }

            if (data != null) {
                data = data.replace("\"{", "{").replace("}\"", "}").replace("\\", "")
                           .replace("\"?\"", "'?'");

                webhookLogger.trace("RachioWebHook: Data='{}'", data);
                RachioEvent event = gson.fromJson(data, RachioEvent.class);

                if ((event != null) && (rachioBridgeHandler != null)) {
                    webhookLogger.trace("RachioEvent {}.{} for device '{}': {}", event.category, event.type, event.deviceId,
                            event.summary);

                    event.apiResult.setRateLimit(
                        Integer.parseInt(request.getHeader(RACHIO_JSON_RATE_LIMIT)),
                        Integer.parseInt(request.getHeader(RACHIO_JSON_RATE_REMAINING)),
                        request.getHeader(RACHIO_JSON_RATE_RESET)
                    );

                    rachioBridgeHandler.webHookEvent(event);
                    return;
                }

                webhookLogger.debug("RachioWebHook: Unable to process inbound request, data='{}'", data);
            }
        } catch (Exception e) {
            if (data != null) {
                webhookLogger.error("RachioWebHook: Exception processing callback: {}, data='{}'", e.getMessage(), data);
            } else {
                webhookLogger.error("RachioWebHook: Exception processing callback: {}", e.getMessage());
            }
        } finally {
            setHeaders(resp);
            resp.getWriter().write("");
        }
    }

    private String inputStreamToString(HttpServletRequest request) throws IOException {
        Scanner scanner = new Scanner(request.getInputStream()).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private void setHeaders(HttpServletResponse response) {
        response.setCharacterEncoding(SERVLET_WEBHOOK_CHARSET);
        response.setContentType(SERVLET_WEBHOOK_APPLICATION_JSON);
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
    }

    private X509Certificate extractCertificate(HttpServletRequest req) {
        X509Certificate[] certs = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
        return (certs != null && certs.length > 0) ? certs[0] : null;
    }
}