package com.proyecto.pedidos.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayDeque;

/** Bounded global budget plus per-session budget. Never trusts client-supplied proxy IPs. */
public class WebChatRateLimitFilter extends OncePerRequestFilter {
    private final ArrayDeque<Long> global=new ArrayDeque<>();
    private synchronized boolean globalPermit(long now){while(!global.isEmpty() && now-global.peekFirst()>=60_000)global.removeFirst();if(global.size()>=40)return false;global.addLast(now);return true;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        if("POST".equals(req.getMethod()) && req.getRequestURI().startsWith(req.getContextPath()+"/api/chat/")) {
            if(req.getContentLengthLong()>12_000){res.setStatus(413);return;}
            HttpSession s=req.getSession();boolean permit;
            synchronized(s){Long since=(Long)s.getAttribute("chatLimitSince");Integer count=(Integer)s.getAttribute("chatLimitCount");long now=System.currentTimeMillis();
                if(since==null || now-since>=60_000){since=now;count=0;}
                permit=count<8 && globalPermit(now);if(permit)count++;s.setAttribute("chatLimitSince",since);s.setAttribute("chatLimitCount",count);}
            if(!permit){res.setStatus(429);res.setHeader("Retry-After","60");return;}
        }
        chain.doFilter(req,res);
    }
}
