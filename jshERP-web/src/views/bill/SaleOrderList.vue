<template>
  <a-card :bordered="false" class="card-area">
    <!-- 查询区域 -->
    <div class="table-page-search-wrapper">
      <!-- 搜索区域 -->
      <a-form layout="inline" @keyup.enter.native="searchQuery">
        <a-row :gutter="24">
          <a-col :md="6" :sm="8">
            <a-form-item label="单据编号" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
              <a-input placeholder="请输入单据编号查询" v-model="queryParam.number"></a-input>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="8">
            <a-form-item label="商品信息" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
              <a-input placeholder="请输入名称、规格、型号" v-model="queryParam.materialParam"></a-input>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="10">
            <a-form-item label="单据日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-range-picker
                style="width: 210px"
                v-model="queryParam.createTimeRange"
                format="YYYY-MM-DD"
                :placeholder="['开始时间', '结束时间']"
                @change="onDateChange"
                @ok="onDateOk"
              />
            </a-form-item>
          </a-col>
          <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
            <a-col :md="6" :sm="24">
              <a-button type="primary" @click="searchQuery">查询</a-button>
              <a-button style="margin-left: 8px" @click="searchReset">重置</a-button>
            </a-col>
          </span>
        </a-row>
      </a-form>
    </div>
    <!-- 操作按钮区域 -->
    <div class="table-operator"  style="margin-top: 5px">
      <a-button v-if="btnEnableList.indexOf(1)>-1" @click="myHandleAdd" type="primary" icon="plus">新增</a-button>
      <a-dropdown v-if="selectedRowKeys.length > 0">
        <a-menu slot="overlay">
          <a-menu-item key="1" v-if="btnEnableList.indexOf(1)>-1" @click="batchDel"><a-icon type="delete"/>删除</a-menu-item>
          <a-menu-item key="2" v-if="btnEnableList.indexOf(2)>-1" @click="batchSetStatus(1)"><a-icon type="check"/>审核</a-menu-item>
          <a-menu-item key="3" v-if="btnEnableList.indexOf(2)>-1" @click="batchSetStatus(0)"><a-icon type="stop"/>反审核</a-menu-item>
        </a-menu>
        <a-button style="margin-left: 8px">
          批量操作 <a-icon type="down" />
        </a-button>
      </a-dropdown>
    </div>
    <!-- table区域-begin -->
    <div>
      <a-table
        ref="table"
        size="middle"
        bordered
        rowKey="id"
        :columns="columns"
        :dataSource="dataSource"
        :pagination="ipagination"
        :loading="loading"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        @change="handleTableChange">
        <span slot="action" slot-scope="text, record">
          <a @click="myHandleDetail(record, '销售订单')">查看</a>
          <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
          <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleEdit(record)">编辑</a>
          <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
          <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" title="确定删除吗?" @confirm="() => handleDelete(record.id)">
            <a>删除</a>
          </a-popconfirm>
        </span>
        <template slot="customRenderStatus" slot-scope="status">
          <a-tag v-if="status == '0'" color="red">未审核</a-tag>
          <a-tag v-if="status == '1'" color="green">已审核</a-tag>
          <a-tag v-if="status == '2'" color="blue">已转采购</a-tag>
        </template>
      </a-table>
    </div>
    <!-- table区域-end -->
    <!-- 表单区域 -->
    <sale-order-modal ref="modalForm" @ok="modalFormOk"></sale-order-modal>
    <bill-detail ref="modalDetail"></bill-detail>
  </a-card>
</template>
<script>
  import SaleOrderModal from './modules/SaleOrderModal'
  import BillDetail from './dialog/BillDetail'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { BillListMixin } from './mixins/BillListMixin'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  export default {
    name: "SaleOrderList",
    mixins:[JeecgListMixin,BillListMixin],
    components: {
      SaleOrderModal,
      BillDetail,
      JDate
    },
    data () {
      return {
        // 查询条件
        queryParam: {
          number: "",
          searchMaterial: "",
          type: "其它",
          subType: "销售订单",
          roleType: Vue.ls.get('roleType')
        },
        labelCol: {
          xs: { span: 24 },
          sm: { span: 8 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        // 表头
        columns: [
          {
            title: '#',
            dataIndex: '',
            key:'rowIndex',
            width:40,
            align:"center",
            customRender:function (t,r,index) {
              return parseInt(index)+1;
            }
          },
          { title: '客户名称', dataIndex: 'organName',width:120},
          { title: '单据编号', dataIndex: 'number',width:160,
            customRender:function (text,record,index) {
              if(record.linkNumber) {
                return text + "[转]";
              } else {
                return text;
              }
            }
          },
          { title: '商品信息', dataIndex: 'materialsList',width:220, ellipsis:true,
            customRender:function (text,record,index) {
              if(text) {
                return text.replace(",","，");
              }
            }
          },
          { title: '单据日期', dataIndex: 'operTimeStr',width:145},
          { title: '操作员', dataIndex: 'userName',width:80},
          { title: '金额合计', dataIndex: 'totalPrice',width:80},
          {title: '状态', dataIndex: 'status', width: 70, align: "center",
            scopedSlots: { customRender: 'customRenderStatus' }
          },
          {
            title: '操作',
            dataIndex: 'action',
            align:"center", width: 150,
            scopedSlots: { customRender: 'action' },
          }
        ],
        url: {
          list: "/depotHead/list",
          delete: "/depotHead/delete",
          deleteBatch: "/depotHead/deleteBatch",
          batchSetStatusUrl: "/depotHead/batchSetStatus"
        }
      }
    },
    created() {
      this.removeStatusColumn()
    },
    computed: {

    },
    methods: {

    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>