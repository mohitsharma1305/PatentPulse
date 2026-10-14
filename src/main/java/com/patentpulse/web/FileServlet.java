package com.patentpulse.web;

import com.patentpulse.model.User;
import com.patentpulse.service.*;
import com.patentpulse.util.Problem;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class FileServlet extends HttpServlet {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    try {
      long id;
      try {
        id = Long.parseLong(r.getParameter("id"));
      } catch (NumberFormatException e) {
        throw new Problem(400, "Invalid record ID.");
      }
      Problem.require(id > 0, 400, "Invalid record ID.");
      var v =
          ((LedgerService) getServletContext().getAttribute("ledger"))
              .version((User) r.getSession().getAttribute("user"), id);
      Path path =
          ((UploadStore) getServletContext().getAttribute("store"))
              .path(v.get("stored_name").toString());
      Problem.require(
          Files.isRegularFile(path), 404, "Stored file is unavailable. Contact the administrator.");
      s.setContentType("application/octet-stream");
      s.setHeader(
          "Content-Disposition",
          "attachment; filename=\"research-file\"; filename*=UTF-8''"
              + URLEncoder.encode(v.get("original_name").toString(), StandardCharsets.UTF_8)
                  .replace("+", "%20"));
      s.setContentLengthLong(Files.size(path));
      Files.copy(path, s.getOutputStream());
    } catch (Problem e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
}
