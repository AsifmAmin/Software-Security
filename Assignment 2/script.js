 
async function subscribe(id,vers) {
  let response = await fetch("/subscribe/" + id +"?version=" + vers);

  if (response.status == 502) {
    // Status 502 is a connection timeout error,
    // may happen when the connection was pending for too long,
    // and the remote server or a proxy closed it
    // let's reconnect
    await subscribe();
  } else if (response.status != 200) {
    // An error - let's show it
    alert(response.statusText);
    // Reconnect in one second
    await new Promise(resolve => setTimeout(resolve, 1000));
    await subscribe();
  } else {
    // Get and show the message
    let message = await response.text();
    let lineend = message.indexOf("\n")
    let newvers = message.substr(0, lineend);
    let html = message.substr(lineend+1);
    let chan = document.getElementById("channel");
    let chanevents = document.getElementById("chanevents");
    chan.replaceChild(htmlToElem(html),chanevents);
    // Call subscribe() again to get the next message
    await subscribe(id,newvers);
  }
}

function htmlToElem(html) {
  let temp = document.createElement('template');
  html = html.trim(); // Never return a space text node as a result
  temp.innerHTML = html;
  return temp.content.firstChild;
}

function submitOnEnter(event){
    if(event.which === 13 && !event.shiftKey){
        event.target.form.submit();
        //event.target.form.dispatchEvent(new Event("submit", {cancelable: true}));
        event.preventDefault();
    }
}


