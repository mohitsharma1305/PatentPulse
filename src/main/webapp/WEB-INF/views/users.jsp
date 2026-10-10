<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>People &amp; roles · PatentPulse</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin-users.css">
</head>
<body>
<main>
  <div class="eyebrow">ADMINISTRATION</div>
  <h1>People &amp; roles</h1>
  <p class="intro">Manage researcher and reviewer access. Administrator accounts cannot be changed here.</p>
  <section class="panel" aria-label="User accounts">
    <table>
      <thead>
      <tr>
        <th scope="col">PERSON</th>
        <th scope="col">CURRENT ROLE</th>
        <th scope="col">ACCOUNT</th>
        <th scope="col">MANAGE ACCESS</th>
      </tr>
      </thead>
      <tbody>
      <c:choose>
        <c:when test="${empty users}">
          <tr><td class="empty" colspan="4">No user accounts were found.</td></tr>
        </c:when>
        <c:otherwise>
          <c:forEach items="${users}" var="person">
            <tr>
              <td><strong><c:out value="${person.name}"/></strong><small><c:out value="${person.email}"/></small></td>
              <td><c:out value="${person.role}"/></td>
              <td><span class="badge ${person.active ? 'active' : 'disabled'}"><c:out value="${person.active ? 'ACTIVE' : 'DISABLED'}"/></span></td>
              <td>
                <c:choose>
                  <c:when test="${person.role == 'ADMIN'}">
                    <span class="muted">Protected administrator</span>
                  </c:when>
                  <c:otherwise>
                    <form class="controls" method="post" action="${pageContext.request.contextPath}/app/admin/user">
                      <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrf}'/>">
                      <input type="hidden" name="userId" value="<c:out value='${person.id}'/>">
                      <label>
                        <span class="muted">Role</span>
                        <select name="role" aria-label="Role for user ${person.id}">
                          <option value="RESEARCHER" ${person.role == 'RESEARCHER' ? 'selected' : ''}>Researcher</option>
                          <option value="REVIEWER" ${person.role == 'REVIEWER' ? 'selected' : ''}>Reviewer</option>
                        </select>
                      </label>
                      <label>
                        <span class="muted">Account</span>
                        <select name="active" aria-label="Account status for user ${person.id}">
                          <option value="true" ${person.active ? 'selected' : ''}>Active</option>
                          <option value="false" ${not person.active ? 'selected' : ''}>Disabled</option>
                        </select>
                      </label>
                      <button type="submit">Save</button>
                    </form>
                  </c:otherwise>
                </c:choose>
              </td>
            </tr>
          </c:forEach>
        </c:otherwise>
      </c:choose>
      </tbody>
    </table>
  </section>
</main>
</body>
</html>
