package utils;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;

public class CharacterEncodingFilter implements Filter {
    
    private String encoding = "UTF-8";

    @Override
    public void init(FilterConfig config) throws ServletException {
        String configEncoding = config.getInitParameter("encoding");
        if (configEncoding != null && !configEncoding.isEmpty()) {
            this.encoding = configEncoding;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        request.setCharacterEncoding(encoding);
        response.setCharacterEncoding(encoding);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
