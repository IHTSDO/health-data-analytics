package org.snomed.heathanalytics.server.rest;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;

@Component
@Order(1)
public class FrontendFilter implements Filter {

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		String servletPath = request.getServletPath();
		if (servletPath.startsWith("/api") || servletPath.contains(".") ||
				// Swagger page and resources
				servletPath.startsWith("/swagger") || servletPath.startsWith("/webjars") || servletPath.startsWith("/v2")) {
			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			// Frontend app URL
			servletRequest.getRequestDispatcher("/index.html").forward(servletRequest, servletResponse);
		}
	}

}
