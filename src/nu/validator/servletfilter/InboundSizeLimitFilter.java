/*
 * Copyright (c) 2007-2008 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package nu.validator.servletfilter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import nu.validator.io.BoundedInputStream;
import nu.validator.io.StreamBoundException;

public final class InboundSizeLimitFilter implements Filter {

    private long sizeLimit;
    
    /**
     * @param sizeLimit
     */
    public InboundSizeLimitFilter(final long sizeLimit) {
        this.sizeLimit = sizeLimit;
    }
    
    public InboundSizeLimitFilter() {
        this(Long.MAX_VALUE);
    }
    
    public void destroy() {
    }

    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        response.addHeader("Accept-Encoding", "gzip");
        String ce = request.getHeader("Content-Encoding");
        if (ce != null && "gzip".equalsIgnoreCase(ce.trim())) {
            chain.doFilter(new RequestWrapper(request), res);
        } else {
            chain.doFilter(req, res);
        }
    }

    public void init(FilterConfig config) throws ServletException {
        // XXX add configurability
    }

    private final class RequestWrapper extends HttpServletRequestWrapper {

        private ServletInputStream stream = null;

        public RequestWrapper(HttpServletRequest req) throws IOException {
            super(req);
        }

        /**
         * @see javax.servlet.ServletRequestWrapper#getInputStream()
         */
        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                if (super.getContentLength() > sizeLimit) {
                    throw new StreamBoundException("Resource size exceeds limit.");
                }
                stream = new DelegatingServletInputStream(new BoundedInputStream(super.getInputStream(), sizeLimit, super.getHeader("Content-Location")));
            }
            return stream;
        }
    }

}
