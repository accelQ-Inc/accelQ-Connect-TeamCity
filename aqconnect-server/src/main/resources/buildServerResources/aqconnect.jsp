<%@ include file="/include.jsp" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<c:set var="title" value="accelQ Connect" scope="request"/>


<l:settingsGroup title="accelQ Connect Configuration">
    <tr>
        <td>
            <span>App URL:</span>
            <br>
            <props:textProperty name="appURL" className="longField"/>
            <br>
            <span>User:</span>
            <br>
            <props:textProperty name="username" />
            <br>
            <span>Password:</span>
            <br>
            <props:passwordProperty name="password"/>
            <br>
            <span>Project Name:</span>
            <br>
            <props:textProperty name="projectName" />
            <br>
            <span>JobPid:</span>
            <br>
            <props:textProperty name="jobPid" />
            <br>
            <span>Run Params:</span>
            <br>
            <props:textProperty name="runParamStr" className="longField"/>
        </td>
    </tr>
</l:settingsGroup>