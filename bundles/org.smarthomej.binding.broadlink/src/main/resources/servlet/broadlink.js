window.onload = reload;

function reload() {
    fetch(document.documentURI + "/things").then(r => r.json()).then(things => setValues(things))
        .catch(error => alert("Failed to get things: " + error));
}

function setValues(things) {
    document.getElementById("things-table").innerHTML = '';
    Object.entries(things).forEach(thing => {
        document.getElementById("things-table")
            .appendChild(addThing(thing[0], thing[1])).appendChild(spacer());
    });
}

function spacer() {
    let row = document.createElement("div");
    row.classList.add("table-row");

    for (let i = 0; i < 3; i++) {
        let cell = document.createElement("div");
        cell.classList.add("table-cell-spacer");
        row.appendChild(cell)
    }

    return row;
}

function addThing(uid, label) {
    let row = document.createElement("div");
    row.classList.add("table-row");

    let labelDiv = document.createElement("div");
    labelDiv.classList.add("table-row-left")
    labelDiv.textContent = label == null ? uid : label;

    let commandDiv = document.createElement("div");
    commandDiv.classList.add("table-row-center");
    let command = document.createElement("input");
    command.type = "text";
    command.id = "command-" + uid;
    commandDiv.appendChild(command);

    let learnDiv = document.createElement("div");
    learnDiv.classList.add("table-row-center");
    let learn = document.createElement("input");
    learn.type = "button";
    learn.id = "learnir-" + uid;
    learn.value = "Learn IR";
    learn.onclick = doLearn;
    learnDiv.appendChild(learn);

    let clearDiv = document.createElement("div");
    clearDiv.classList.add("table-row-right");
    let clear = document.createElement("input");
    clear.type = "button";
    clear.id = "clear-" + uid;
    clear.value = "Clear";
    clearDiv.appendChild(clear);

    row.appendChild(labelDiv);
    row.appendChild(commandDiv)
    row.appendChild(learnDiv);
    row.appendChild(clearDiv);

    return row;
}

function doLearn(event) {
    let type = event.target.id.substr(5, 2);
    let id = event.target.id.substr(8);
    let command = document.getElementById("command-" + id).value;
    fetch(document.documentURI + "/learn?thing=" + id + "&command=" + command + "&type=" + type);
    alert("Use your remote to learn the new code within the next 30s!");
}
