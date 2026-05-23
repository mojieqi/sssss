<template>
  <div class="tool-result-card" :class="{ 'is-error': !cardData.success, 'is-pending': cardData.pending }">
    <div class="tool-header">
      <i v-if="cardData.pending" class="el-icon-loading"></i>
      <i v-else-if="cardData.success" class="el-icon-check" style="color:#67c23a"></i>
      <i v-else class="el-icon-close" style="color:#f56c6c"></i>
      <span class="tool-title">工具调用: {{ cardData.name || 'unknown' }}</span>
      <span v-if="cardData.pending" class="pending-tag">执行中...</span>
    </div>

    <!-- 参数区 -->
    <div v-if="cardData.arguments" class="tool-section">
      <div class="section-title">参数</div>
      <pre class="section-content">{{ formatJson(cardData.arguments) }}</pre>
    </div>

    <!-- 结果区 -->
    <div v-if="cardData.content" class="tool-section">
      <div class="section-title">结果</div>
      <div class="section-content result-content">{{ cardData.content }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ToolResultCard',
  props: {
    /** 工具调用数据对象 { id, name, arguments, success, content, pending } */
    cardData: { type: Object, default: () => ({}) },
    /** 兼容旧格式: toolCalls JSON */
    toolCalls: { type: [Object, String], default: null }
  },
  computed: {
    displayData() {
      if (this.cardData && this.cardData.name) return this.cardData
      // 兼容旧prop格式
      if (!this.toolCalls) return {}
      try {
        const data = typeof this.toolCalls === 'string'
          ? JSON.parse(this.toolCalls)
          : this.toolCalls
        if (Array.isArray(data) && data.length > 0) {
          const tc = data[0]
          const fn = tc.function || tc
          return {
            name: fn.name || 'unknown',
            arguments: fn.arguments,
            success: true,
            content: '工具已执行（详情见数据库）'
          }
        }
        return data
      } catch {
        return {}
      }
    }
  },
  methods: {
    formatJson(obj) {
      if (!obj) return ''
      if (typeof obj === 'string') {
        try { return JSON.stringify(JSON.parse(obj), null, 2) }
        catch { return obj }
      }
      return JSON.stringify(obj, null, 2)
    }
  }
}
</script>

<style scoped>
.tool-result-card {
  background: #f0f7ff;
  border: 1px solid #d0e8ff;
  border-radius: 6px;
  padding: 10px 12px;
  margin: 8px 0;
  font-size: 12px;
}
.tool-result-card.is-error {
  background: #fef0f0;
  border-color: #fbc4c4;
}
.tool-result-card.is-pending {
  background: #fdf6ec;
  border-color: #f5dab1;
}
.tool-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 6px;
}
.tool-header .tool-title { color: #303133; }
.pending-tag {
  font-size: 11px;
  color: #e6a23c;
  font-weight: normal;
}
.tool-section {
  margin-top: 6px;
}
.section-title {
  color: #909399;
  font-size: 11px;
  margin-bottom: 4px;
}
.section-content {
  background: #fff;
  border-radius: 4px;
  padding: 6px 8px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}
.section-content pre {
  margin: 0;
  font-size: 11px;
}
.result-content {
  max-height: 300px;
}
</style>
