package org.camunda.bpm.spring.boot.starter.webapp;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.camunda.bpm.admin.impl.web.AdminApplication;
import org.camunda.bpm.admin.impl.web.bootstrap.AdminContainerBootstrap;
import org.camunda.bpm.cockpit.impl.web.CockpitApplication;
import org.camunda.bpm.cockpit.impl.web.bootstrap.CockpitContainerBootstrap;
import org.camunda.bpm.engine.rest.filter.CacheControlFilter;
import org.camunda.bpm.spring.boot.starter.webapp.filter.LazyProcessEnginesFilter;
import org.camunda.bpm.spring.boot.starter.webapp.filter.LazySecurityFilter;
import org.camunda.bpm.tasklist.impl.web.TasklistApplication;
import org.camunda.bpm.tasklist.impl.web.bootstrap.TasklistContainerBootstrap;
import org.camunda.bpm.webapp.impl.engine.EngineRestApplication;
import org.camunda.bpm.webapp.impl.security.auth.AuthenticationFilter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.ServletContextInitializer;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.glassfish.jersey.servlet.ServletProperties.JAXRS_APPLICATION_CLASS;

/**
 * Inspired by:
 * https://groups.google.com/forum/#!msg/camunda-bpm-users/BQHdcLIivzs
 * /iNVix8GkhYAJ (Christoph Berg)
 */
public class CamundaBpmWebappInitializer implements ServletContextInitializer {

  public static String APP = "app";
  public static String API = "api";
  private final Logger logger = LoggerFactory.getLogger(CamundaBpmWebappInitializer.class);

  private static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.of(DispatcherType.REQUEST);

  private ServletContext servletContext;

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    this.servletContext = servletContext;

    servletContext.addListener(new CockpitContainerBootstrap());
    servletContext.addListener(new AdminContainerBootstrap());
    servletContext.addListener(new TasklistContainerBootstrap());

    registerFilter("Authentication Filter", AuthenticationFilter.class, "/*");

    registerFilter("Security Filter", LazySecurityFilter.class, singletonMap("configFile", "/securityFilterRules.json"), "/*");

    registerFilter("Engines Filter", LazyProcessEnginesFilter.class, format("/%s/*", APP));
    registerFilter("CacheControlFilter", CacheControlFilter.class, format("/%s/*", API));

    registerServlet("Cockpit Api", CockpitApplication.class, format("/%s/cockpit/*", API));
    registerServlet("Admin Api", AdminApplication.class, format("/%s/admin/*", API));
    registerServlet("Tasklist Api", TasklistApplication.class, format("/%s/tasklist/*", API));
    registerServlet("Engine Api", EngineRestApplication.class, format("/%s/engine/*", API));
  }

  private FilterRegistration registerFilter(final String filterName, final Class<? extends Filter> filterClass, final String... urlPatterns) {
    return registerFilter(filterName, filterClass, null, urlPatterns);
  }

  private FilterRegistration registerFilter(final String filterName, final Class<? extends Filter> filterClass, final Map<String, String> initParameters,
                                            final String... urlPatterns) {
    FilterRegistration filterRegistration = servletContext.getFilterRegistration(filterName);

    if (filterRegistration == null) {
      filterRegistration = servletContext.addFilter(filterName, filterClass);
      filterRegistration.addMappingForUrlPatterns(DISPATCHER_TYPES, true, urlPatterns);

      if (initParameters != null) {
        filterRegistration.setInitParameters(initParameters);
      }

      logger.debug("Filter {} for URL {} registered.", filterName, urlPatterns);
    }

    return filterRegistration;
  }

  private ServletRegistration registerServlet(final String servletName, final Class<?> applicationClass,
                                              final String... urlPatterns) {
    ServletRegistration servletRegistration = servletContext.getServletRegistration(servletName);

    if (servletRegistration == null) {
      servletRegistration = servletContext.addServlet(servletName, ServletContainer.class);
      servletRegistration.addMapping(urlPatterns);
      servletRegistration.setInitParameters(singletonMap(JAXRS_APPLICATION_CLASS, applicationClass.getName()));

      logger.debug("Servlet {} for URL {} registered.", servletName, urlPatterns);
    }

    return servletRegistration;
  }
}
