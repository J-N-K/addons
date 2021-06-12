/**
 * Copyright (c) 2021 Contributors to the SmartHome/J project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.smarthomej.binding.broadlink.internal;

import static org.smarthomej.binding.broadlink.internal.BroadlinkBindingConstants.SUPPORTED_THING_TYPE_UIDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.http.servlet.OpenHABServlet;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.google.gson.Gson;

/**
 * The {@link BroadlinkServlet} is responsible for
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component
@NonNullByDefault
public class BroadlinkServlet extends OpenHABServlet {
    private static final String SERVLET_URL = "/broadlink";
    private final ThingRegistry thingRegistry;

    @Activate
    public BroadlinkServlet(@Reference HttpService httpService, @Reference HttpContext httpContext,
            @Reference ThingRegistry thingRegistry) {
        super(httpService, httpContext);

        try {
            httpService.registerServlet(SERVLET_URL, this, null, httpService.createDefaultHttpContext());
        } catch (NamespaceException | ServletException e) {
            logger.warn("Failed to register servlet '{}': {}", SERVLET_URL, e.getMessage());
        }

        this.thingRegistry = thingRegistry;
    }

    @SuppressWarnings("unused")
    @Deactivate
    public void deactivate() {
        httpService.unregister(SERVLET_URL);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String requestUri = Objects.requireNonNull(req.getRequestURI());

        try {
            if (requestUri.equals(SERVLET_URL + "/learnIr")) {
                String queryString = Objects.requireNonNullElse(req.getQueryString(), "");
                Map<String, String> params = Stream.of(queryString.split("&")).map(p -> p.split("="))
                        .collect(Collectors.toMap(a -> a[0], a -> URLDecoder.decode(a[1], StandardCharsets.UTF_8)));

                String thingUid = params.get("thing");
                String command = params.get("command");
                String type = params.getOrDefault("type", "ir");
                if (thingUid == null || command == null) {
                    resp.sendError(400, "Illegal parameter set");
                    return;
                }
                Thing thing = thingRegistry.get(new ThingUID(thingUid));
                if (thing == null) {
                    resp.sendError(400, "Thing not found");
                    return;
                }
                ThingHandler handler = thing.getHandler();
                if (!(handler instanceof RMThingHandler)) {
                    resp.sendError(400, "Thing has no handler or handler is not of correct type");
                    return;
                }
                RMThingHandler rmHandler = (RMThingHandler) handler;
                if ("ir".equals(type)) {
                    rmHandler.learnIr(command);
                }
                resp.setStatus(200);
            } else if (requestUri.equals(SERVLET_URL + "/things")) {
                Map<String, @Nullable String> things = thingRegistry.stream()
                        .filter(thing -> SUPPORTED_THING_TYPE_UIDS.contains(thing.getThingTypeUID()))
                        .collect(Collectors.toMap(t -> t.getUID().getAsString(), Thing::getLabel));
                Gson gson = new Gson();
                resp.addHeader("content-type", "application/json");
                resp.getWriter().write(gson.toJson(things));
            } else if (requestUri.startsWith(SERVLET_URL + "/resources")) {
                doGetResource(resp, requestUri);
            } else if (requestUri.equals(SERVLET_URL)) {
                String returnHtml = loadResource("servlet/broadlink.html");
                if (returnHtml == null) {
                    logger.warn("Could not load RepoManager");
                    resp.sendError(500);
                    return;
                }

                resp.addHeader("content-type", "text/html;charset=UTF-8");
                resp.getWriter().write(returnHtml);
            } else {
                resp.sendError(404);
            }
        } catch (IOException e) {
            logger.warn("Returning GET data for '{}' failed: {}", requestUri, e.getMessage());
        }
    }

    private void doGetResource(HttpServletResponse resp, String uri) throws IOException {
        String path = uri.replace(SERVLET_URL + "/resources", "");
        String resourceContent = loadResource(path);
        if (resourceContent == null) {
            resp.sendError(404);
            return;
        }
        resp.getWriter().write(resourceContent);

        // set content type
        if (path.endsWith(".css")) {
            resp.setContentType("text/css");
        } else {
            resp.setContentType("text/plain");
        }
    }

    /**
     * load a resource from the bundle
     *
     * @param path the path to the resource
     * @return a string containing the resource content
     */
    private @Nullable String loadResource(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            logger.warn("Could not get classloader.");
            return null;
        }

        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            if (inputStream == null) {
                logger.warn("Requested resource '{}' not found.", path);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (IOException e) {
            logger.warn("Requested resource '{}' could not be loaded: {}", path, e.getMessage());
            return null;
        }
    }
}
