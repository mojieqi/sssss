<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="工具名称" prop="toolName">
        <el-input v-model="queryParams.toolName" placeholder="请输入工具名称" clearable style="width: 200px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="工具代码" prop="toolCode">
        <el-input v-model="queryParams.toolCode" placeholder="请输入工具代码" clearable style="width: 180px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini"
          @click="handleAdd" v-hasPermi="['ai:tool:add']">新增工具</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini"
          @click="handleDelete" v-hasPermi="['ai:tool:remove']" :disabled="ids.length === 0">批量删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="toolList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="工具ID" prop="toolId" width="80" align="center" />
      <el-table-column label="工具名称" prop="toolName" min-width="120" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.toolName }}</span>
          <el-tag v-if="scope.row.isBuiltin === '1'" size="mini" style="margin-left:6px">内置</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="工具代码" prop="toolCode" width="130" />
      <el-table-column label="描述" prop="toolDesc" min-width="200" show-overflow-tooltip />
      <el-table-column label="处理类" prop="handlerClass" min-width="200" show-overflow-tooltip />
      <el-table-column label="排序" prop="sort" width="60" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view"
            @click="handleView(scope.row)" v-hasPermi="['ai:tool:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit"
            @click="handleEdit(scope.row)" v-hasPermi="['ai:tool:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c"
            @click="handleSingleDelete(scope.row)" v-hasPermi="['ai:tool:remove']"
            :disabled="scope.row.isBuiltin === '1'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="680px" append-to-body
      :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="工具名称" prop="toolName">
          <el-input v-model="form.toolName" placeholder="请输入工具名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="工具代码" prop="toolCode">
          <el-input v-model="form.toolCode" placeholder="请输入工具代码，如 web_search" maxlength="50"
            :disabled="form.toolId != null" />
        </el-form-item>
        <el-form-item label="工具描述" prop="toolDesc">
          <el-input v-model="form.toolDesc" type="textarea" :rows="2" placeholder="请输入工具描述" maxlength="500" />
        </el-form-item>
        <el-form-item label="处理类路径" prop="handlerClass">
          <el-input v-model="form.handlerClass" placeholder="com.ruoyi.system.agent.tool.impl.XxxTool" maxlength="200" />
        </el-form-item>
        <el-form-item label="Function Schema" prop="functionSchema">
          <el-input v-model="form.functionSchema" type="textarea" :rows="10"
            placeholder='{"name":"tool_name","description":"...","parameters":{...}}' />
        </el-form-item>
        <el-form-item label="是否内置">
          <el-radio-group v-model="form.isBuiltin">
            <el-radio label="0">否</el-radio>
            <el-radio label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog title="工具详情" :visible.sync="viewDialogVisible" width="700px" append-to-body>
      <el-descriptions :column="2" border v-if="viewData">
        <el-descriptions-item label="工具ID">{{ viewData.toolId }}</el-descriptions-item>
        <el-descriptions-item label="工具名称">{{ viewData.toolName }}</el-descriptions-item>
        <el-descriptions-item label="工具代码">{{ viewData.toolCode }}</el-descriptions-item>
        <el-descriptions-item label="是否内置">
          <el-tag :type="viewData.isBuiltin === '1' ? 'warning' : 'info'" size="small">
            {{ viewData.isBuiltin === '1' ? '内置' : '自定义' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.status === '0' ? 'success' : 'info'" size="small">
            {{ viewData.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="排序">{{ viewData.sort }}</el-descriptions-item>
        <el-descriptions-item label="工具描述" :span="2">{{ viewData.toolDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理类" :span="2">{{ viewData.handlerClass }}</el-descriptions-item>
        <el-descriptions-item label="Function Schema" :span="2">
          <pre class="schema-pre">{{ formatJson(viewData.functionSchema) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ viewData.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { listTool, getTool, addTool, updateTool, delTool } from '@/api/ai/tool'

export default {
  name: 'AiTool',
  data() {
    return {
      showSearch: true,
      loading: false,
      ids: [],
      toolList: [],
      total: 0,
      dialogVisible: false,
      viewDialogVisible: false,
      dialogTitle: '',
      viewData: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        toolName: '',
        toolCode: '',
        status: ''
      },
      form: {
        toolId: null,
        toolName: '',
        toolCode: '',
        toolDesc: '',
        functionSchema: '',
        handlerClass: '',
        isBuiltin: '0',
        status: '0',
        sort: 0
      },
      rules: {
        toolName: [{ required: true, message: '工具名称不能为空', trigger: 'blur' }],
        toolCode: [{ required: true, message: '工具代码不能为空', trigger: 'blur' }],
        handlerClass: [{ required: true, message: '处理类路径不能为空', trigger: 'blur' }],
        functionSchema: [
          { required: true, message: 'Function Schema不能为空', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              try {
                JSON.parse(value)
                callback()
              } catch {
                callback(new Error('JSON格式不正确'))
              }
            }, trigger: 'blur'
          }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listTool(this.queryParams).then(res => {
        this.toolList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, toolName: '', toolCode: '', status: '' }
      this.getList()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.toolId)
    },
    handleAdd() {
      this.resetForm()
      this.dialogTitle = '新增工具'
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.resetForm()
      this.dialogTitle = '编辑工具'
      getTool(row.toolId).then(res => {
        this.form = res.data
        this.dialogVisible = true
      })
    },
    handleView(row) {
      getTool(row.toolId).then(res => {
        this.viewData = res.data
        this.viewDialogVisible = true
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (this.form.toolId != null) {
          updateTool(this.form).then(() => {
            this.$message.success('修改成功')
            this.dialogVisible = false
            this.getList()
          })
        } else {
          addTool(this.form).then(() => {
            this.$message.success('新增成功')
            this.dialogVisible = false
            this.getList()
          })
        }
      })
    },
    handleSingleDelete(row) {
      if (row.isBuiltin === '1') {
        this.$message.warning('内置工具不可删除')
        return
      }
      this.$confirm('确定删除该工具吗？', '提示', { type: 'warning' }).then(() => {
        delTool(row.toolId).then(() => {
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleDelete() {
      if (this.ids.length === 0) return
      const hasBuiltin = this.toolList
        .filter(item => this.ids.includes(item.toolId))
        .some(item => item.isBuiltin === '1')
      if (hasBuiltin) {
        this.$message.warning('选中的工具中包含内置工具，不可删除')
        return
      }
      this.$confirm('确定批量删除选中的工具吗？', '提示', { type: 'warning' }).then(() => {
        delTool(this.ids.join(',')).then(() => {
          this.$message.success('删除成功')
          this.getList()
          this.ids = []
        })
      }).catch(() => {})
    },
    resetForm() {
      this.form = {
        toolId: null,
        toolName: '',
        toolCode: '',
        toolDesc: '',
        functionSchema: '',
        handlerClass: '',
        isBuiltin: '0',
        status: '0',
        sort: 0
      }
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate()
      })
    },
    formatJson(jsonStr) {
      if (!jsonStr) return '-'
      try {
        return JSON.stringify(JSON.parse(jsonStr), null, 2)
      } catch {
        return jsonStr
      }
    }
  }
}
</script>

<style scoped>
.schema-pre {
  margin: 0;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
}
</style>
