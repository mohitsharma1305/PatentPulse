package com.patentpulse.web;

import java.io.IOException;
import java.util.UUID;

import com.patentpulse.model.User;
import com.patentpulse.service.*;
import com.patentpulse.util.Problem;
import javax.servlet.*;
import javax.servlet.http.*;

public class AuthServlet extends HttpServlet {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    if (r.getServletPath().equals("/logout")) {
      s.sendError(405);
      return;
    }
    r.setAttribute("register", r.getServletPath().equals("/register"));
    r.getRequestDispatcher("/WEB-INF/views/auth.jsp").forward(r, s);
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    try {
      if (r.getServletPath().equals("/logout")) {
        r.getSession().invalidate();
        s.sendRedirect(r.getContextPath() + "/login?loggedOut=1");
        return;
      }
      AuthService auth = (AuthService) getServletContext().getAttribute("auth");
      if (r.getServletPath().equals("/register")) {
        auth.register(param(r, "name"), param(r, "email"), param(r, "password"));
        s.sendRedirect(r.getContextPath() + "/login?registered=1");
        return;
      }
      HttpSession session = r.getSession();
      Long blocked = (Long) session.getAttribute("blockedUntil");
      Problem.require(
          blocked == null || System.currentTimeMillis() > blocked,
          429,
          "Too many failed attempts in this session. Try again in one minute.");
      User user;
      try {
        user = auth.login(param(r, "email"), param(r, "password"));
      } catch (Problem e) {
        int failures =
            session.getAttribute("failures") == null
                ? 1
                : (Integer) session.getAttribute("failures") + 1;
        session.setAttribute("failures", failures);
        if (failures >= 5) {
          session.setAttribute("blockedUntil", System.currentTimeMillis() + 60000);
          session.setAttribute("failures", 0);
          throw new Problem(
              429, "Too many failed attempts in this session. Try again in one minute.");
        }
        throw e;
      }
      r.changeSessionId();
      session.setAttribute("user", user);
      session.setAttribute("csrf", UUID.randomUUID().toString());
      session.removeAttribute("failures");
      session.removeAttribute("blockedUntil");
      session.setMaxInactiveInterval(1800);
      s.sendRedirect(r.getContextPath() + "/app/dashboard");
    } catch (Problem e) {
      s.setStatus(e.status);
      r.setAttribute("error", e.getMessage());
      if (!r.getServletPath().equals("/register")) {
        r.setAttribute("loginEmail", param(r, "email"));
      }
      r.setAttribute("register", r.getServletPath().equals("/register"));
      r.getRequestDispatcher("/WEB-INF/views/auth.jsp").forward(r, s);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private String param(HttpServletRequest r, String n) {
    String value = r.getParameter(n);
    return value == null ? "" : value;
  }
}
