package com.rbkmoney.skipper.endpoint;

import com.rbkmoney.woody.thrift.impl.http.THServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet("/v1/skipper")
public class SkipperEndpoint extends GenericServlet {

    private transient Servlet thriftServlet;

    //TODO: add
    //@Autowired
    //private transient SkipperSrv.Iface clearingServiceHandler;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        //thriftServlet = new THServiceBuilder()
        //        .build(SkipperSrv.Iface.class, clearingServiceHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }
}
