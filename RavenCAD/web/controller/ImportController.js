

$(document).ready(function() { //don't run javascript until page is loaded
// FIELDS
    var loaded = false;
    var ravenPart = null;
    var tabResized = true;
    var sequenceHash = {}; //key: composition/name; value: sequence
    var _parts = {}; //key: name, value: part JSON
    var _partIds = {}; //key: name, value: uuid
// FUNCTIONS

    function allAddRow(rowData) {
        $('#allTable').dataTable().fnAddData([
            rowData["uuid"],
            rowData["Name"],
            rowData["LO"],
            rowData["RO"],
            rowData["Type"],
            rowData["Composition"],
            rowData["Resistance"],
            rowData["Level"]
        ]);
    }
    function partAddRow(rowData) {
        $('#partTable').dataTable().fnAddData([
            rowData["uuid"],
            rowData["Name"],
            rowData["Sequence"],
            rowData["LO"],
            rowData["RO"],
            rowData["Type"],
            rowData["Composition"]
        ]);
    }
    function vectorAddRow(rowData) {
        $('#vectorTable').dataTable().fnAddData([
            rowData["uuid"],
            rowData["Name"],
            rowData["Sequence"],
            rowData["LO"],
            rowData["RO"],
            rowData["Type"],
            rowData["Resistance"],
            rowData["Level"]
        ]);
    }
    var refreshData = function() {
        $.get("RavenServlet", {"command": "dataStatus"}, function(data) {
            if (data === "loaded") {
                loaded = true;
                getData();
                $('#uploadArea').removeClass("in");
                $('#uploadArea').addClass("out");
                //TODO add some sort of popup as a guiding hint
            } else {
                loaded = false;
                $('#uploadArea').removeClass("out");
                $('#uploadArea').addClass("in");
                //TODO add some sort of popup as a guiding hint
            }
        });
    };
//draw table
    var drawTable = function() {
        //TODO draw parts and vectors into separate tabs
        var allTableBody = "<table id='allTable' class='table table-bordered table-hover'><thead><tr><th>uuid</th><th>Name</th><th>LO</th><th>RO</th><th>Type</th><th>Composition</th><th>Resistance</th><th>Level</th></tr></thead><tbody>";
        var partTableBody = "<table id='partTable' class='table table-bordered table-hover'><thead><tr><th>uuid</th><th>Name</th><th>LO</th><th>RO</th><th>Type</th><th>Composition</th></tr></thead><tbody>";
        var vectorTableBody = "<table id='vectorTable' class='table table-bordered table-hover'><thead><tr><th>uuid</th><th>Name</th><th>LO</th><th>RO</th><th>Type</th><th>Resistance</th><th>Level</th></tr></thead><tbody>";
        $.each(ravenPart, function() {
            allTableBody = allTableBody + "<tr><td>"
                    + this["uuid"] + "</td><td>"
                    + this["Name"] + "</td><td>"
                    + this["LO"] + "</td><td>"
                    + this["RO"] + "</td><td>"
                    + this["Type"] + "</td><td>"
                    + this["Composition"] + "</td><td>"
                    + this["Resistance"] + "</td><td>"
                    + this["Level"] + "</td></tr>";
            if (this["Type"] === "vector") {
                sequenceHash["vector_" + this["uuid"]] = this["sequence"];
                vectorTableBody = vectorTableBody + "<tr><td>"
                        + this["uuid"] + "</td><td>"
                        + this["Name"] + "</td><td>"
                        + this["LO"] + "</td><td>"
                        + this["RO"] + "</td><td>"
                        + this["Type"] + "</td><td>"
                        + this["Resistance"] + "</td><td>"
                        + this["Level"] + "</td></tr>";
            } else {
                sequenceHash["part_" + this["uuid"]] = this["sequence"];
                partTableBody = partTableBody + "<tr><td>"
                        + this["uuid"] + "</td><td>"
                        + this["Name"] + "</td><td>"
                        + this["LO"] + "</td><td>"
                        + this["RO"] + "</td><td>"
                        + this["Type"] + "</td><td>"
                        + this["Composition"] + "</td></tr>";
            }
        });
        allTableBody = allTableBody + "</tbody></table>";
        vectorTableBody = vectorTableBody + "</tbody></table>";
        partTableBody = partTableBody + "</tbody></table>";
        $("#allTableArea").html(allTableBody);
        $("#partTableArea").html(partTableBody);
        $("#vectorTableArea").html(vectorTableBody);

        $("#allTable").dataTable({
            "sScrollY": "300px",
            "bPaginate": false,
            "bScrollCollapse": true
        });
        $("#partTable").dataTable({
            "sScrollY": "300px",
            "bPaginate": false,
            "bScrollCollapse": true
        });
        $("#vectorTable").dataTable({
            "sScrollY": "300px",
            "bPaginate": false,
            "bScrollCollapse": true
        });
        $("tr").on("click", function() {
//        //TODO finish form generation
            var type = "Part";
            if ($(this).children('td:eq(1)').text() === "vector") {
                type = "Vector";
            }
            $('#editorArea').html('<form class="form-horizontal"><legend>Edit ' + type + '</legend><p>' + 'Name: ' + $(this).children('td:eq(0)').text() + '</p><p>Type: ' + $(this).children('td:eq(1)').text() + '</p></form>');
            $('#errorArea').addClass("hidden");
        });


    };


    var getData = function() {
        $.getJSON("RavenServlet", {"command": "fetch"}, function(json) {
            ravenPart = json["result"];
            if (json["status"] === "bad") {
                $('#error').html(json["message"]);
                $('#error').append('<hr/><p>We have uploaded the rest of your parts and vectors</p>');
                $('#errorArea').removeClass("hidden");
            }
            drawTable();
        });
    };
    //EVENT HANDLERS
    $('#clotho3Import').click(function() {
        refreshClothoParts();
    });

    $('#clotho3Export').click(function() {
        var basicParts = [];
        var compositeParts = [];
        var vectors = [];
        $.each(ravenPart, function(index, data) {
            if (_partIds[data["Name"]] === undefined) {
                if (data["Type"] === "composite") {
                    compositeParts.push(data);
                } else if (data["Type"] === "vector") {
                    vectors.push(data);
                } else {
                    basicParts.push(data);
                }
            }
        });
        $.each(basicParts, function(index, basicPart) {
            saveRavenPart(basicPart);
        });
        //refresh clotho parts
        refreshClothoParts();

        $.each(vectors, function(index, vector) {
        });
        $.each(compositeParts, function(index, compositePart) {
//            alert(JSON.stringify(compositePart));
            saveRavenPart(compositePart);
        });
    });

    $('.tablink').click(function() {
        tabResized = false;
    });
    $('#dismissButton').click(function() {
        $('#errorArea').addClass("hidden");
    });
    $('#resetButton').click(function() {
        $.get("RavenServlet", {"command": "purge"}, function() {
            window.location.replace("import.html");
        });
    });
    $('#designButton').click(function() {
        if (loaded) {
            window.location = "ravencad.html";
        } else {
            $('#uploadModal').modal();
        }
    });
    $('#allTableArea').mouseenter(function() {
        if (!tabResized) {
            allTable.fnAdjustColumnSizing();
            tabResized = true;
        }
    });
    $('#partTableArea').mouseenter(function() {
        if (!tabResized) {
            partTable.fnAdjustColumnSizing();
            tabResized = true;
        }
    });
    $('#vectorTableArea').mouseenter(function() {
        if (!tabResized) {
            vectorTable.fnAdjustColumnSizing();
            tabResized = true;
        }
    });
    $('#dataArea').mouseleave(function() {
        $('#editorArea').addClass("hidden");
        $('#editorArea').html("");
    });

    $('#dataArea').mouseenter(function() {
        $('#editorArea').removeClass("hidden");
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

    var saveRavenPart = function(part, partId) {
//        send("create", JSON.stringify(part));
        if (partId === undefined) {
            partId = new ObjectId().toString();
        }
        var seqId = new ObjectId().toString();
        if (part["Type"] === "vector") {
            //not sure I can save vectors yet...
        } else if (part["Type"] === "composite") {
            if (_partIds[part["Name"]] === undefined) {
                //create new part if it doesn't exist already

                //gather the composition and sequence of the composite part
                var partComposition = part["Composition"].substring(1, part["Composition"].length - 1).split(",");
                var composition = [];
                var compositeSequence = "";
                for (var i = 0; i < partComposition.length; i++) {
                    var basicPartName = partComposition[i].substring(0, partComposition[i].indexOf("|")).trim();
                    var basicPartId = _partIds[basicPartName];
                    var basicPart = _parts[basicPartName];
                    composition.push(basicPartId);
                    compositeSequence = compositeSequence + basicPart.sequence.sequence;
                }
                var newPart = {
                    schema: "CompositePart",
                    _id: partId,
                    author: "51e9579344ae846673a51b0f", //this probably shouldnt be hard coded later...
                    shortDescription: this["pigeon"],
                    sequence: {
                        _id: seqId,
                        isRNA: false,
                        isSingleStranded: false,
                        sequence: compositeSequence,
                        isDegenerate: false,
                        isLinear: false,
                        isCircular: false,
                        isLocked: false
                    },
                    name: part["Name"],
                    composition: composition,
                    format: "FreeForm",
                    type: "composite",
                    riskGroup: 0,
                };
                _parts[newPart["name"]] = newPart;
                _partIds[newPart["name"]] = partId;

                //create composite part
                send("create", JSON.stringify(newPart));

            }
        } else {
            //save basic part
            if (_partIds[part["Name"]] === undefined) {
                var newPart = {
                    schema: "BasicPart",
                    _id: partId,
                    author: "51e9579344ae846673a51b0f", //this probably shouldnt be hard coded later...
                    shortDescription: "",
                    sequence: {
                        _id: seqId,
                        isRNA: false,
                        isSingleStranded: false,
                        sequence: part["Sequence"],
                        isDegenerate: false,
                        isLinear: false,
                        isCircular: false,
                        isLocked: false
                    },
                    name: part["Name"],
                    format: "FreeForm",
                    type: part["Type"],
                    riskGroup: 0,
                };
                _parts[newPart["name"]] = newPart;
                _partIds[newPart["name"]] = partId;
                send("create", JSON.stringify(newPart));
            }
        }
    };



    /********Clotho Functions and Variables********/
    var _connection = new WebSocket('ws://localhost:8080/websocket');
    var canSend = false;
    var _requestCommand = {}; //key request id, value: callback function
    var _requestID = 0;
    var refreshClothoParts = function() {
        send("query", '{"schema":"BasicPart"}', function(data) {
            var newParts = {};
            var newPartsArray = [];
            $.each(data, function() {
                if (newParts[this["name"]] === undefined) {
                    newParts[this["name"]] = "added";
                    newPartsArray.push(this);
                }
                if (_parts[this["name"]] === undefined) {
                    _parts[this["name"]] = this;
                    _partIds[this["name"]] = this["id"];
                }
            });
//            alert(JSON.stringify(newPartsArray));
            $.post("RavenServlet", {command: "importClotho", data: JSON.stringify(newPartsArray)}, function(response) {
                //import composite parts
                send("query", '{"schema":"CompositePart"}', function(data) {
                    var newParts = {};
                    var newPartsArray = [];
                    $.each(data, function() {
                        if (newParts[this["name"]] === undefined) {
                            newParts[this["name"]] = "added";
                            newPartsArray.push(this);
                        }
                        if (_parts[this["name"]] === undefined) {
                            _parts[this["name"]] = this;
                            _partIds[this["name"]] = this["id"];
                        }
                    });
                    $.post("RavenServlet", {command: "importClotho", "data": JSON.stringify(newPartsArray)}, function(response) {
                        refreshData();
                    });
                });
            });
        });

    };

    var send = function(channel, data, callback) {
        if (canSend) {
            var message = '{"channel":"' + channel + '", "data":' + data + ',"requestId":"' + _requestID + '"}';
            _requestCommand[channel + _requestID] = callback;
            _connection.send(message);
            _requestID++;
        } else {
            _connection = new WebSocket('ws://localhost:8080/websocket');
        }
    };
    _connection.onmessage = function(e) {
        //parase message into JSON
        var dataJSON = $.parseJSON(e.data);
        //ignore say messages which have not requestId
        var channel = dataJSON["channel"];
        var requestId = dataJSON["requestId"];
        if (requestId !== null) {
            //if callback function exists, run it
            var callback = _requestCommand[channel + requestId];
            if (callback !== undefined) {
                callback(dataJSON["data"]);
                delete _requestCommand[channel + requestId];
            }
        }
    };

    _connection.onerror = function(e) {
//        alert("F**K");
    };

    _connection.onclose = function() {
//        alert('closing connection');
    };
    _connection.onopen = function(e) {
        canSend = true;
    };




    //functions to run
    if (getCookie("authenticate") !== "authenticated") {
        deleteCookie("user");
    }

    if (getCookie("authenticate") === "authenticated") {
        $('#loginArea').html('<p class="pull-right">You are logged in as <strong>' + getCookie("user") + '</strong> <a id="logout">Log Out</a></p>');
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

    refreshData();
    var allTable = $("#allTable").dataTable({
        "sScrollY": "300px",
        "bPaginate": false,
        "bScrollCollapse": true
    });
    var partTable = $("#partTable").dataTable({
        "sScrollY": "300px",
        "bPaginate": false,
        "bScrollCollapse": true
    });
    var vectorTable = $("#vectorTable").dataTable({
        "sScrollY": "300px",
        "bPaginate": false,
        "bScrollCollapse": true
    });

});
