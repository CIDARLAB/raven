$(document).ready(function() {
    $.get("RavenServlet", {"command": "purge"});
    $('#loginForm').submit(function(e) {
        var self = this;
        e.preventDefault();
        if ($('#loginForm input[name="user"]').val() === "") {
            alert("Please enter your user name.");
        } else if ($('#loginForm input[name="password"]') === "") {
            alert("Please enter your password");
        } else {
            self.submit();
        }
    });
});