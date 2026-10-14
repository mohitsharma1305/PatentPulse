package com.patentpulse.web;

import com.patentpulse.model.User;
import com.patentpulse.service.AuthService;
import com.patentpulse.util.Problem;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class SecurityFilter implements Filter {
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest r = (HttpServletRequest) request;
    HttpServletResponse s = (HttpServletResponse) response;
    r.setCharacterEncoding("UTF-8");
    s.setCharacterEncoding("UTF-8");
    s.setHeader("X-Content-Type-Options", "nosniff");
    s.setHeader("X-Frame-Options", "DENY");
    s.setHeader("Referrer-Policy", "same-origin");
    s.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; form-action"
            + " 'self'; frame-ancestors 'none'; base-uri 'self'");
    String path = r.getRequestURI().substring(r.getContextPath().length());
    if (path.startsWith("/assets/")) {
      chain.doFilter(r, s);
      return;
    }
    s.setHeader("Cache-Control", "no-store");
    HttpSession session = r.getSession();
    if (session.getAttribute("csrf") == null)
      session.setAttribute("csrf", UUID.randomUUID().toString());
    try {
      User user = (User) session.getAttribute("user");
      if (user != null) {
        user = ((AuthService) r.getServletContext().getAttribute("auth")).current(user.getId());
        if (user == null) session.removeAttribute("user");
        else session.setAttribute("user", user);
      }
      if (path.startsWith("/app") || path.equals("/file")) {
        if (user == null) {
          s.sendRedirect(r.getContextPath() + "/login");
          return;
        }
        if (path.startsWith("/app/admin"))
          Problem.require(user.isAdmin(), 403, "Admin access required.");
        if (path.startsWith("/app/review"))
          Problem.require(user.isReviewer() || user.isAdmin(), 403, "Reviewer access required.");
      }
      if (r.getMethod().equals("POST")) {
        String token = r.getParameter("csrf");
        Problem.require(
            token != null
                && java.security.MessageDigest.isEqual(
                    token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    session
                        .getAttribute("csrf")
                        .toString()
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            403,
            "Your form expired. Refresh the page and try again.");
      }
      chain.doFilter(r, s);
    } catch (Problem e) {
      error(r, s, e.status, e.getMessage());
    } catch (IllegalStateException e) {
      error(r, s, 400, "Request could not be processed. Upload limit is 10 MB per file.");
    } catch (Exception e) {
      r.getServletContext().log("Request failed", e);
      error(r, s, 500, "Something went wrong. Please try again or contact the administrator.");
    }
  }

  private void error(HttpServletRequest r, HttpServletResponse s, int status, String message)
      throws ServletException, IOException {
    if (s.isCommitted()) return;
    s.setStatus(status);
    r.setAttribute("error", message);
    r.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(r, s);
  }
}
