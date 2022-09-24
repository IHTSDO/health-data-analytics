package org.snomed.heathanalytics.server.rest;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class RootController {

	@RequestMapping(path = "/", method = RequestMethod.GET)
	@ApiOperation(value = "Root controller, redirects to swagger-ui.", hidden = true)
	public void getRoot(HttpServletResponse response) throws IOException {
		response.sendRedirect("swagger-ui.html");
	}

}
