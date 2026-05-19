<template>
  <div class="container">
    <div class="header">
      <div>
        <h1>门店数据分析智能体</h1>
        <p class="subtitle">基于AI的门店经营数据智能分析助手</p>
      </div>
      <div class="status">
        <span :class="{ connected: isConnected, disconnected: !isConnected }">
          {{ isConnected ? '已连接' : '未连接' }}
        </span>
      </div>
    </div>

    <div class="chat-container">
      <div class="quick-actions">
        <button class="quick-action" @click="sendQuickQuery('北京门店上个月的财务情况怎么样')">
          北京门店财务分析
        </button>
        <button class="quick-action" @click="sendQuickQuery('对比北京和上海门店Q1业绩')">
          门店业绩对比
        </button>
        <button class="quick-action" @click="sendQuickQuery('分析北京门店近半年销售趋势')">
          销售趋势分析
        </button>
        <button class="quick-action" @click="sendQuickQuery('为什么北京门店上周客流突然下降')">
          异常诊断
        </button>
      </div>

      <div class="message-list" ref="messageList">
        <div v-if="messages.length === 0" class="welcome-message">
          <h2>欢迎使用门店数据分析智能体</h2>
          <p>请输入您的问题，或点击上方快捷按钮开始分析</p>
        </div>

        <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
          <div v-if="msg.role === 'agent'" class="avatar agent">AI</div>
          <div class="bubble">
            <div v-if="msg.loading" class="loading">
              <span></span><span></span><span></span>
            </div>
            <div v-else>
              <div v-html="renderMarkdown(msg.content)"></div>
              <div v-if="msg.charts && msg.charts.length > 0" class="chart-container">
                <ChartRenderer :config="msg.charts[0]" />
              </div>
            </div>
          </div>
          <div v-if="msg.role === 'user'" class="avatar user">U</div>
        </div>
      </div>

      <div class="input-area">
        <input
          v-model="inputQuery"
          placeholder="输入您的分析问题，如：北京门店上月营收情况如何？"
          @keyup.enter="sendQuery"
          :disabled="isLoading"
        />
        <button @click="sendQuery" :disabled="isLoading || !inputQuery">
          {{ isLoading ? '分析中...' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue'
import { marked } from 'marked'
import ChartRenderer from './components/ChartRenderer.vue'
import { analyze, analyzeStream } from './api/agentApi'

export default {
  name: 'App',
  components: { ChartRenderer },
  setup() {
    const messages = ref([])
    const inputQuery = ref('')
    const isLoading = ref(false)
    const isConnected = ref(false)
    const sessionId = ref('session-' + Date.now())
    const messageList = ref(null)

    onMounted(async () => {
      try {
        const response = await fetch('/api/agent/health')
        isConnected.value = response.ok
      } catch {
        isConnected.value = false
      }
    })

    const renderMarkdown = (content) => {
      if (!content) return ''
      return marked(content)
    }

    const scrollToBottom = () => {
      nextTick(() => {
        if (messageList.value) {
          messageList.value.scrollTop = messageList.value.scrollHeight
        }
      })
    }

    const sendQuery = async () => {
      if (!inputQuery.value.trim() || isLoading.value) return

      const query = inputQuery.value.trim()
      inputQuery.value = ''

      messages.value.push({
        id: Date.now(),
        role: 'user',
        content: query
      })

      const agentMsgId = Date.now() + 1
      messages.value.push({
        id: agentMsgId,
        role: 'agent',
        content: '',
        loading: true
      })

      isLoading.value = true
      scrollToBottom()

      try {
        let fullContent = ''

        await analyzeStream(sessionId.value, query, (chunk) => {
          fullContent += chunk
          const msg = messages.value.find(m => m.id === agentMsgId)
          if (msg) {
            msg.loading = false
            msg.content = fullContent
          }
          scrollToBottom()
        })

        const msg = messages.value.find(m => m.id === agentMsgId)
        if (msg) {
          msg.loading = false
        }
      } catch (error) {
        const msg = messages.value.find(m => m.id === agentMsgId)
        if (msg) {
          msg.loading = false
          msg.content = '分析过程中出现错误，请稍后重试。错误信息：' + error.message
        }
      }

      isLoading.value = false
      scrollToBottom()
    }

    const sendQuickQuery = (query) => {
      inputQuery.value = query
      sendQuery()
    }

    return {
      messages,
      inputQuery,
      isLoading,
      isConnected,
      messageList,
      renderMarkdown,
      sendQuery,
      sendQuickQuery
    }
  }
}
</script>

<style scoped>
.status {
  display: flex;
  align-items: center;
}

.status span {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.connected {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.disconnected {
  background: rgba(220, 53, 69, 0.2);
  color: #fff;
}
</style>