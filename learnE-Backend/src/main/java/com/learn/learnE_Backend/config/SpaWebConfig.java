package com.learn.learnE_Backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the built React app so the whole thing runs behind one origin and needs no CORS.
 *
 * <p>React Router owns paths like {@code /login}, which do not exist as files. Anything that is not
 * a real file and not an API call therefore falls back to {@code index.html} and is routed on the
 * client. Controller mappings are matched before resource handlers, so live API endpoints are never
 * shadowed; the explicit {@code api/} guard is what makes a <em>missing</em> API path return 404
 * instead of a page of HTML.
 *
 * <p>{@code app.frontend.location} decides where those files come from:
 * <ul>
 *   <li>{@code classpath:/static/} (default) — baked into the jar, so it is self-contained.</li>
 *   <li>{@code file:.../learnE-Frontend/dist/} — read from disk, so {@code npm run build} plus a
 *       browser refresh is enough and the backend never has to restart.</li>
 * </ul>
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private final String location;
    private final ResourceLoader resourceLoader;

    public SpaWebConfig(
            @Value("${app.frontend.location:classpath:/static/}") String location,
            ResourceLoader resourceLoader
    ) {
        this.location = location.endsWith("/") ? location : location + "/";
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Resource index = resourceLoader.getResource(location + "index.html");
        // Files inside the jar never change while it runs, so they are worth caching. Files on disk
        // are exactly the ones being edited, so caching them would serve the previous build.
        boolean immutable = location.startsWith("classpath:");

        registry.addResourceHandler("/**")
                .addResourceLocations(location)
                .setCachePeriod(immutable ? null : 0)
                .resourceChain(immutable)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource resourceLocation) throws IOException {
                        Resource requested = resourceLocation.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("v3/api-docs")) {
                            return null;
                        }
                        return index.exists() ? index : null;
                    }
                });
    }
}
