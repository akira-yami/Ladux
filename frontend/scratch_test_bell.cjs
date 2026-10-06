const { spawn } = require('child_process');

const chrome = spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', [
  '--headless=new',
  '--remote-debugging-port=9227',
  '--window-size=1440,900',
  'http://localhost:5173'
]);

setTimeout(async () => {
  try {
    const listRes = await fetch('http://127.0.0.1:9227/json');
    const tabs = await listRes.json();
    const targetTab = tabs.find(t => t.url.includes('5173') && t.type === 'page') || tabs[0];
    const ws = new WebSocket(targetTab.webSocketDebuggerUrl);
    ws.onopen = () => {
      const code = `(() => {
        const bellBtn = document.querySelector('button[aria-label="Thông báo"]');
        return {
          bellExists: !!bellBtn,
          bellAriaExpanded: bellBtn ? bellBtn.getAttribute('aria-expanded') : null,
          notifDropdownInDom: !!document.querySelector('.animate-in')
        };
      })()`;
      ws.send(JSON.stringify({
        id: 1,
        method: 'Runtime.evaluate',
        params: { expression: code, returnByValue: true }
      }));
    };
    ws.onmessage = (msg) => {
      const data = JSON.parse(msg.data);
      if (data.id === 1) {
        console.log('TEST RESULT:', data.result.result.value);
        chrome.kill();
        process.exit(0);
      }
    };
  } catch(e) {
    console.log('Error:', e.message);
    chrome.kill();
    process.exit(1);
  }
}, 2500);
