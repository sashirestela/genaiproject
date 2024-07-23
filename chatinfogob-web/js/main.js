import { SSE } from '../node_modules/sse.js/lib/sse.js';

document.addEventListener('DOMContentLoaded', () => {

  const hostUrl = "https://sashirestela-genaiproject-code-redirect-3.apps.sandbox-m3.1530.p1.openshiftapps.com"

  const messageInput = document.getElementById('message-input');
  const sendButton = document.getElementById('send-button');
  const chatBox = document.getElementById('chat-box');
  const uploadButton = document.getElementById('upload-button');
  const pdfInput = document.getElementById('pdf-input');
  const leftPanel = document.getElementById('left-panel');

  const chatAction = 'chat';
  const uploadAction = 'upload';
  const MAX_NUM_RETRIES = 2;

  let messages = [];
  let concatenatedData = '';
  let concatenatedQuote = '';
  let numHits = 0;

  sendButton.addEventListener('click', handleMessageSend);

  messageInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleMessageSend();
    }
  });

  uploadButton.addEventListener('click', () => {
    pdfInput.click();
  });

  pdfInput.addEventListener('change', handlePDFUpload);

  function handleMessageSend() {
    let message = '';
    if (numHits == 0) {
      message = messageInput.value.trim();
      if (!message) {
        return;
      }
      addMessageToChat(message, 'user');
      messageInput.value = '';
    } else {
      message = getLastMessageFrom('user');
    }
    showLoader(chatAction);
    const request = { question: message, messages: messages };
    numHits++;

    const source = new SSE(`${hostUrl}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      payload: JSON.stringify(request)
    });

    source.addEventListener('open', onOpen);
    source.addEventListener('delta', onDelta);
    source.addEventListener('quote', onQuote);
    source.addEventListener('chatmessage', onChatMessage);
    source.addEventListener('done', onDone);
    source.addEventListener('error', onError);

    source.stream();
  }

  function handlePDFUpload(event) {
    const file = event.target.files[0];
    if (file) {
      showLoader(uploadAction);
      const formData = new FormData();
      formData.append('pdf', file);

      fetch(`${hostUrl}/upload`, {
        method: 'POST',
        body: formData
      })
        .then(data => {
          console.log('File uploaded successfully:', data);
          hideLoader(uploadAction);
        })
        .catch(error => {
          console.error('Error uploading file:', error);
          hideLoader(uploadAction);
        });
    }
  }

  function onOpen(event) {
    hideLoader(chatAction);
  }

  function onDelta(event) {
    concatenatedData += event.data;
    updateConcatenatedMessage(concatenatedData);
  }

  function onQuote(event) {
    concatenatedQuote = addQuote(event.data);
  }

  function onChatMessage(event) {
    const chatmessage = JSON.parse(event.data);
    messages.push(chatmessage);
  }

  function onDone(event) {
    addMessageToChat(concatenatedData + concatenatedQuote, 'bot');
    cleanConcatenatedMessage();
    concatenatedData = '';
    concatenatedQuote = '';
    numHits = 0;
  }

  function onError(event) {
    console.error('Error receiving response:', event);
    hideLoader(chatAction);
    if (numHits == MAX_NUM_RETRIES) {
      numHits = 0;
    } else {
      handleMessageSend();
    }
  }

  function addMessageToChat(message, sender) {
    const messageElement = document.createElement('div');
    messageElement.classList.add('message', sender);
    messageElement.innerHTML = message;
    chatBox.appendChild(messageElement);
    chatBox.scrollTop = chatBox.scrollHeight;
  }

  function getLastMessageFrom(sender) {
    const element = document.querySelectorAll('.message.' + sender + ':last-child');
    return element[0].innerText;
  }

  function updateConcatenatedMessage(message) {
    const existingMessage = document.querySelector('.message.bot-concat');
    if (existingMessage) {
      existingMessage.innerHTML = message;
    } else {
      const messageElement = document.createElement('div');
      messageElement.classList.add('message', 'bot-concat');
      messageElement.innerHTML = message;
      chatBox.appendChild(messageElement);
    }
    chatBox.scrollTop = chatBox.scrollHeight;
  }

  function addQuote(quote) {
    let quoteHtml = '';
    let existingQuote = document.querySelector('.message.bot-concat .quote');
    if (existingQuote) {
      existingQuote.innerHTML += '<br>' + quote;
      quoteHtml = existingQuote.outerHTML;
    } else {
      const concatenatedElement = document.querySelector('.message.bot-concat');
      const quoteElement = document.createElement('div');
      quoteElement.classList.add('quote');
      quoteElement.innerHTML = '<br><br>' + quote;
      concatenatedElement.appendChild(quoteElement);
      quoteHtml = quoteElement.outerHTML;
    }
    chatBox.scrollTop = chatBox.scrollHeight;
    return quoteHtml;
  }

  function cleanConcatenatedMessage() {
    const element = document.querySelector('.message.bot-concat');
    if (element) {
      chatBox.removeChild(element);
    }
  }

  function showLoader(action) {
    const loaderElement = document.createElement('div');
    if (action === 'chat') {
      loaderElement.classList.add('loader-chat');
      chatBox.appendChild(loaderElement);
      chatBox.scrollTop = chatBox.scrollHeight;
    } else if (action == 'upload') {
      loaderElement.classList.add('loader-upload');
      leftPanel.appendChild(loaderElement);
    }
  }

  function hideLoader(action) {
    const loaderElement = document.querySelector('.loader-chat, .loader-upload');
    if (loaderElement) {
      if (action === 'chat') {
        chatBox.removeChild(loaderElement);
      } else if (action == 'upload') {
        leftPanel.removeChild(loaderElement);
      }
    }
  }
});
