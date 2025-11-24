package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class RachioBridgeConfiguration {
    public String apiKey = "";
    public String webhookId = "";
    public int refreshInterval = 60;
    public String webhookUrl = "";
}
