import { SSE } from '../node_modules/sse.js/lib/sse.js';

document.addEventListener('DOMContentLoaded', () => {

  const hostUrl = "https://sashirestela-genaiproject-code-redirect-3.apps.sandbox-m3.1530.p1.openshiftapps.com"

  const messageInput = document.getElementById('message-input')
  const sendButton = document.getElementById('send-button')
  const chatBox = document.getElementById('chat-box')

  let messages = []
  let concatenatedData = ''

  sendButton.addEventListener('click', () => {
    const message = messageInput.value.trim()
    if (message) {
      const request = { question: message, messages: messages }
      sendMessage(message, 'user')
      messageInput.value = ''

      const source = new SSE(`${hostUrl}/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        payload: JSON.stringify(request)
      });

      source.addEventListener('delta', (event) => {
        const delta = event.data.replace(/(\r\n|\n|\r)/g, '<br>')
        concatenatedData += delta
        updateConcatenatedMessage(concatenatedData)
      })
      source.addEventListener('quote', (event) => {
        const quote = '<br>' + event.data
        concatenatedData += quote
        updateConcatenatedMessage(concatenatedData)
      })
      source.addEventListener('chatmessage', (event) => {
        const chatmessage = JSON.parse(event.data)
        messages.push(chatmessage)
      })
      source.addEventListener('done', (event) => {
        sendMessage(concatenatedData, 'bot')
        cleanConcatenatedMessage()
        concatenatedData = ''
      })

      source.stream()
    }
  });

  messageInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
      sendButton.click()
    }
  });

  const sendMessage = (message, sender) => {
    const messageElement = document.createElement('div')
    messageElement.classList.add('message', sender)
    messageElement.innerHTML = message
    chatBox.appendChild(messageElement)
    chatBox.scrollTop = chatBox.scrollHeight
  };

  const updateConcatenatedMessage = (message) => {
    const existingMessage = document.querySelector('.message.bot-concat')
    if (existingMessage) {
      existingMessage.innerHTML = message
    } else {
      const messageElement = document.createElement('div')
      messageElement.classList.add('message', 'bot-concat')
      messageElement.innerHTML = message
      chatBox.appendChild(messageElement)
    }
    chatBox.scrollTop = chatBox.scrollHeight
  };

  const addQuote = (quote) => {
    const concatenatedElement = document.querySelector('.message.bot-concat')
    const quoteElement = document.createElement('div')
  };

  const cleanConcatenatedMessage = () => {
    const element = document.querySelector('.message.bot-concat')
    chatBox.removeChild(element)
  }

});
