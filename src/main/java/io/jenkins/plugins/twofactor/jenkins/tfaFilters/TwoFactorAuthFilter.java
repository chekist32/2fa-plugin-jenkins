/*
 * Copyright (c) 2023
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jenkins.plugins.twofactor.jenkins.tfaFilters;

import hudson.init.Initializer;
import hudson.model.User;
import hudson.util.PluginServletFilter;
import io.jenkins.plugins.twofactor.constants.PluginUrls;
import io.jenkins.plugins.twofactor.jenkins.CookieUtils;
import io.jenkins.plugins.twofactor.jenkins.globalConfig.GlobalConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.AbstractTfaConfig;
import io.jenkins.plugins.twofactor.jenkins.user.UserTfaCookieTokenService;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class TwoFactorAuthFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(TwoFactorAuthFilter.class.getName());
    public static final String REDIRECT_URL_NAME = "tfaRelayState";
    public static final String TFA_COOKIE_NAME = "tfa-session";

    private final UserTfaCookieTokenService userTfaCookieTokenService;

    private static final List<RequestMatcher> basicUrlsToAvoidRedirect = List.of(
            new AntPathRequestMatcher("/" + PluginUrls.USER_AUTH_PATH + "/**"),
            new AntPathRequestMatcher("/logout"),
            new AntPathRequestMatcher("/login"),

            // Resource uris
            new AntPathRequestMatcher("/static/**"),
            new AntPathRequestMatcher("/resourceBundle/**"),
            new AntPathRequestMatcher("/favicon.ico"),
            new AntPathRequestMatcher("/plugin/2fa-jenkins-plugin/**"),
            new AntPathRequestMatcher("/adjuncts/**"),

            // Rest API
            new AntPathRequestMatcher("/createItem/**"),
            new AntPathRequestMatcher("/createView/**"),
            new AntPathRequestMatcher("/queue/**"),
            new AntPathRequestMatcher("/overallLoad/**"),
            new AntPathRequestMatcher("/quietDown/**"),
            new AntPathRequestMatcher("/cancelQuietDown/**"),
            new AntPathRequestMatcher("/restart/**"),
            new AntPathRequestMatcher("/safeRestart/**"),
            new AntPathRequestMatcher("/api/json/**")
      );


    public TwoFactorAuthFilter() {
        this(UserTfaCookieTokenService.getInstance());
    }

    public TwoFactorAuthFilter(UserTfaCookieTokenService userTfaCookieTokenService) {
        this.userTfaCookieTokenService = userTfaCookieTokenService;
    }

    @SuppressWarnings("unused")
    @Initializer
    public static void setUpFilter() throws ServletException {
        LOGGER.fine("Setting up the filter for the two-factor plugin");
        PluginServletFilter.addFilter(new TwoFactorAuthFilter());
    }

    private boolean urlContainsInToAvoidRedirectUrls(HttpServletRequest req) {
        for (var requestMatcher : basicUrlsToAvoidRedirect) {
            if (requestMatcher.matches(req)) return true;
        }
        return false;
    }

    private boolean isTfaEnabledAndConfigured() {
        var globalConfig = GlobalConfig.get();
        return globalConfig.getEnableTfa() && !globalConfig.getEnabledTfaMethods().isEmpty();
    }

    private boolean isTfaVerifiedSession(HttpServletRequest req, User user) {
          Optional<Cookie> cookie = CookieUtils.findCookieByName(req, TFA_COOKIE_NAME);
          if (cookie.isEmpty()) return false;

          String value = cookie.get().getValue();

          try {
              var uuid = UUID.fromString(value);
              return userTfaCookieTokenService.contains(user.getId(), uuid);
          } catch (IllegalArgumentException e) { return false; }
    }

    private boolean byPass2FA(User user, HttpServletRequest req) {
        return user == null ||
               urlContainsInToAvoidRedirectUrls(req) ||
               !isTfaEnabledAndConfigured() ||
               isTfaVerifiedSession(req, user) ||
               !userHasAtLeastOneConfiguredTfaMethod(user);
    }

    private boolean userHasAtLeastOneConfiguredTfaMethod(User user) {
        Set<TfaMethodType> enabledTfaMethods = GlobalConfig.get().getEnabledTfaMethods();

        for (var userProperty : user.getAllProperties()) {
                if (userProperty instanceof AbstractTfaConfig) {
                    var tfaConfig = (AbstractTfaConfig) userProperty;
                    if (tfaConfig.isConfigured() &&
                        enabledTfaMethods.contains(tfaConfig.getMethodType())
                    ) { return true; }
                }
            }

        return false;
    }

    @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    try {
      var req = (HttpServletRequest) servletRequest;
      var user = User.current();
      var session = req.getSession();

      if (byPass2FA(user, req)) {
        filterChain.doFilter(servletRequest, servletResponse);
        return;
      }

      var rsp = (HttpServletResponse) servletResponse;
      var relayState = req.getRequestURI();

      if (session.getAttribute(REDIRECT_URL_NAME) == null) {
        session.setAttribute(REDIRECT_URL_NAME, relayState.trim());
      }

      LOGGER.fine(
          req.getRequestURI()
              + " is being redirecting for 2FA, saved relay state is "
              + relayState);

      rsp.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
      rsp.setHeader("Location", "/" + PluginUrls.USER_AUTH_PATH + "/");
    } catch (Exception e) {
      filterChain.doFilter(servletRequest, servletResponse);
      LOGGER.fine("Error in filter processing " + e.getMessage());
    }
  }

}
