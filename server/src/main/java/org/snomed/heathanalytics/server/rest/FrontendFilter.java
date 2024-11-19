package org.snomed.heathanalytics.server.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class FrontendFilter extends HttpFilter {

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		String servletPath = request.getServletPath();
		if (servletPath.startsWith("/api") || servletPath.contains(".") ||
				// Swagger page and resources
				servletPath.startsWith("/swagger") || servletPath.startsWith("/webjars") || servletPath.startsWith("/v3")) {
			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			// Frontend app URL
			servletRequest.getRequestDispatcher("/index.html").forward(servletRequest, servletResponse);
		}
	}

}
