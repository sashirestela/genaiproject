package com.encora.genai.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.fileupload2.core.RequestContext;

import com.sun.net.httpserver.HttpExchange;

public class HttpHandlerRequestContext implements RequestContext {

    private HttpExchange httpExchange;

    public HttpHandlerRequestContext(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
    }

    @Override
    public String getCharacterEncoding() {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public String getContentType() {
        return httpExchange.getRequestHeaders().getFirst("Content-type");
    }

    @Override
    public long getContentLength() {
        return 0;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return httpExchange.getRequestBody();
    }

}
