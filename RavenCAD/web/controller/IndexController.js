$(document).ready(function() {
    $('#loginForm').submit(function(e) {
        var self = this;
        e.preventDefault();
        if ($('#loginForm input[name="user"]').val() === "") {
            $('#loginModal').modal();
        } else if ($('#loginForm input[name="password"]').val() === "") {
            $('#loginModal').modal();
        } else {
            self.submit(function() {
                if (getCookie("authenticate") === "authenticated") {
                    $.get("RavenServlet", {"command": "purge"});
                }
            });
        }
    });
    function setCookie(c_name, value, exdays) {
        var exdate = new Date();
        exdate.setDate(exdate.getDate() + exdays);
        var c_value = escape(value) + ((exdays === null) ? "" : "; expires=" + exdate.toUTCString());
        document.cookie = c_name + "=" + c_value;
    }

    function getCookie(c_name) {
        var c_value = document.cookie;
        var c_start = c_value.indexOf(" " + c_name + "=");
        if (c_start === -1) {
            c_start = c_value.indexOf(c_name + "=");
        }
        if (c_start === -1) {
            c_value = null;
        }
        else {
            c_start = c_value.indexOf("=", c_start) + 1;
            var c_end = c_value.indexOf(";", c_start);
            if (c_end === -1) {
                c_end = c_value.length;
            }
            c_value = unescape(c_value.substring(c_start, c_end));
        }
        return c_value;
    }
    function deleteCookie(key) {
        // Delete a cookie by setting the date of expiry to yesterday
        date = new Date();
        date.setDate(date.getDate() - 1);
        document.cookie = escape(key) + '=;expires=' + date;
    }

    if (getCookie("authenticate") !== "authenticated") {
        deleteCookie("user");
    }

    if (getCookie("authenticate") === "authenticated") {
        $('#loginArea').html('<p class="pull-right">You are logged in as <strong>' + getCookie("user") + '</strong> <a id="logout">Log Out</a></p>');
        $('#startLink').attr("href", "documentation.html");
        $('#logout').click(function() {
            $.get("RavenServlet", {"command": "logout"}, function() {
                deleteCookie("authenticate");
                deleteCookie("user");
                window.location.replace("index.html");
            });
        });
    } else if (getCookie("authenticate") === "failed") {
        window.location.replace("login.html");
    }


    $.get("RavenServlet", {command: "getExamples"}, function(data) {
        var carousel = '';
        var jsonData = $.parseJSON(data);
        $.each(jsonData, function(index, value) {
            carousel = carousel+'<div class="item"><img src="examples/'+value+'" alt =""/></div>';
        });
        $('#submittedExamples div').prepend(carousel);
        $('#submittedExamples div div:first').addClass('active');
    });

});