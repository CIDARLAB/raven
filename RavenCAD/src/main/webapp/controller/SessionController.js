$(document).ready(function() {

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
        // Delete a cookie by setting the expiration date to yesterday
        date = new Date();
        date.setDate(date.getDate() - 1);
        document.cookie = escape(key) + '=;expires=' + date;
    }

    if (getCookie("raven") !== "authenticated") {
        deleteCookie("user");
    }

    if (getCookie("raven") === "authenticated") {
        $('#loginArea').html('<div id="loginArea" class="navbar-form pull-right">' +
                'You are logged in as <strong>' + getCookie("user") + '</strong>&nbsp;&nbsp;&nbsp;&nbsp;' +
                '<button id="btnLogout" class="btn btn-primary btn-warning">Logout</button>');
    } else if (getCookie("authenticate") === "failed") {
        window.location.replace("login.html");
    }

//-----------------------------------------------
// AUTHENTICATION
//-----------------------------------------------

    // SIGNUP Button
    $('#btnSignUp').click(function() {
        var username = $('#signup_username').val();
        var jsonRequest = {"command": "signup",
            "username": username,
            "password": $('#signup_password').val()};

        $.post("AuthenticationServlet", jsonRequest, function(response) {

            // if there was an error, then we display the error
            if (response['status'] === 'exception') {
                $('#signupError').html('<div class="alert alert-danger">' + response['result'] + '</div>');
            } else {
                $('#signupError').html('<div class="alert alert-success"> Success! </div>');

                // set the cookie
                setCookie("user", username, 1);
                setCookie("raven", "authenticated", 1);
                
                window.location.replace('import.html');
            }
        });
    });

    // LOGIN button
    $('#btnLogin').click(function() {
        var username = $('#login_username').val();
        var jsonRequest = {"command": "login",
            "username": username,
            "password": $('#login_password').val()};

        $.post("AuthenticationServlet", jsonRequest, function(response) {

            // if there was an error, then we display the error
            if (response['status'] === 'exception') {
                $('#loginError').html('<div class="alert alert-danger" style="margin-top:5px">' + response['result'] + '</div>');
//                window.location.replace("login.html");
            } else {
                $('#loginError').html('');

                // set the cookie
                setCookie("user", username, 1);
                setCookie("raven", "authenticated", 1);

                $('#loginArea').html('<div id="loginArea" class="navbar-form pull-right">' +
                        'You are logged in as <strong>' + getCookie("user") + '</strong>&nbsp;&nbsp;&nbsp;&nbsp;' +
                        '<button id="btnLogout" class="btn btn-primary btn-warning">Logout</button>');

                location.reload();
            }
        });
    });

    // LOGOUT Button
    $('#btnLogout').click(function() {

        var user = getCookie("user");
        var jsonRequest = {"command": "logout",
            "username": getCookie("user")};
        $.post("AuthenticationServlet", jsonRequest, function(response) {
            // if there was an error, then we display the error
            if (response['status'] === 'exception') {
                $('#loginError').html('<div class="alert alert-danger">' + response['result'] + '</div>');
            } else {
                deleteCookie("user");
                deleteCookie("raven");

                window.location.replace('index.html');
            }
        });
    });
});