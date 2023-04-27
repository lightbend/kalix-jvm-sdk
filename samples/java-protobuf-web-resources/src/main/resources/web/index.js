console.log('Kalix Web Resources - Demo')

var xhttp = new XMLHttpRequest();
xhttp.onreadystatechange = function() {
    if (this.readyState == 4 && this.status == 200) {
        var response = JSON.parse(xhttp.responseText);
        var i = 0;
        var ul = document.getElementById("cart");
        while (i < response.items.length) {
            var li = document.createElement("li");
            li.appendChild(document.createTextNode(response.items[i]));
            ul.appendChild(li);
            i++;
        }
    }
};
xhttp.open("GET", "/get-cart", true);
xhttp.send();